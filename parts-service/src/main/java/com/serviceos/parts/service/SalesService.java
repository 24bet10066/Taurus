package com.serviceos.parts.service;

import com.serviceos.parts.dto.request.SellPartsRequest;
import com.serviceos.parts.dto.response.SaleReceiptResponse;
import com.serviceos.parts.entity.PartsSale;
import com.serviceos.parts.entity.SaleItem;
import com.serviceos.parts.entity.SparePart;
import com.serviceos.parts.entity.TechnicianCredit;
import com.serviceos.parts.repository.PartsSaleRepository;
import com.serviceos.parts.repository.SparePartRepository;
import com.serviceos.parts.repository.TechnicianCreditRepository;
import com.serviceos.shared.dto.PageResponse;
import com.serviceos.shared.enums.MovementType;
import com.serviceos.shared.exception.CreditLimitExceededException;
import com.serviceos.shared.exception.InsufficientStockException;
import com.serviceos.shared.exception.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class SalesService {

    private final SparePartRepository sparePartRepository;
    private final PartsSaleRepository saleRepository;
    private final TechnicianCreditRepository creditRepository;
    private final InventoryMovementService movementService;
    private final CreditService creditService;
    private final PartsEventPublisher eventPublisher;

    public SalesService(SparePartRepository sparePartRepository,
                        PartsSaleRepository saleRepository,
                        TechnicianCreditRepository creditRepository,
                        InventoryMovementService movementService,
                        CreditService creditService,
                        PartsEventPublisher eventPublisher) {
        this.sparePartRepository = sparePartRepository;
        this.saleRepository = saleRepository;
        this.creditRepository = creditRepository;
        this.movementService = movementService;
        this.creditService = creditService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public SaleReceiptResponse sell(SellPartsRequest req, String soldBy) {
        // Step 1: resolve all parts and validate stock
        List<LineResolved> lines = resolveLinesAndValidate(req);

        BigDecimal total = lines.stream()
                .map(LineResolved::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Step 2: handle credit payment
        BigDecimal creditUsed = BigDecimal.ZERO;
        BigDecimal newBalance = null;
        BigDecimal availableCredit = null;

        if ("CREDIT".equalsIgnoreCase(req.paymentMethod())) {
            TechnicianCredit credit = creditRepository.findById(req.technicianId())
                    .orElseThrow(() -> new ResourceNotFoundException("TechnicianCredit", req.technicianId()));

            BigDecimal available = credit.getCreditLimit().subtract(credit.getCurrentBalance());
            if (total.compareTo(available) > 0) {
                throw new CreditLimitExceededException(
                        req.technicianId(), total, available, credit.getCreditLimit());
            }
            creditUsed = total;
        }

        // Step 3: create sale record
        PartsSale sale = new PartsSale();
        sale.setTechnicianId(req.technicianId());
        sale.setTechnicianName(req.technicianName());
        sale.setPaymentMethod(req.paymentMethod().toUpperCase());
        sale.setTotalAmount(total);
        sale.setCreditUsed(creditUsed);
        sale.setItems(lines.stream()
                .map(l -> new SaleItem(l.partId(), l.partName(), l.qty(), l.unitPrice(), l.lineTotal()))
                .toList());
        sale = saleRepository.save(sale);

        // Step 4: record inventory movements (within same transaction)
        for (LineResolved line : lines) {
            movementService.recordMovement(
                    line.partId(), "FREELANCER_SALE", -line.qty(),
                    line.unitPrice(), sale.getId(), soldBy, null);
        }

        // Step 5: if CREDIT, record the debit ledger entry
        if ("CREDIT".equalsIgnoreCase(req.paymentMethod())) {
            TechnicianCredit updatedCredit = creditService.debitCredit(
                    req.technicianId(), total, sale.getId(), soldBy);
            newBalance = updatedCredit.getCurrentBalance();
            availableCredit = updatedCredit.getCreditLimit().subtract(newBalance);
        }

        // Step 6: update total_purchased
        creditRepository.findById(req.technicianId()).ifPresent(c -> {
            c.setTotalPurchased(c.getTotalPurchased().add(total));
            c.setLastPurchaseAt(Instant.now());
            creditRepository.save(c);
        });

        // Step 7: publish Kafka events per item
        for (LineResolved line : lines) {
            eventPublisher.publishPartsSold(sale.getId(), line.partId(), line.sku(),
                    req.technicianId(), line.qty(), line.unitPrice(), line.lineTotal(),
                    MovementType.OUTWARD_B2B);
        }

        List<SaleReceiptResponse.LineItem> receiptLines = lines.stream()
                .map(l -> new SaleReceiptResponse.LineItem(l.partId(), l.partName(),
                        l.qty(), l.unitPrice(), l.lineTotal()))
                .toList();

        return new SaleReceiptResponse(
                sale.getId(), req.technicianId(), req.technicianName(),
                receiptLines, total, req.paymentMethod().toUpperCase(),
                creditUsed, newBalance, availableCredit, sale.getCreatedAt()
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<SaleReceiptResponse> listSales(UUID techId, LocalDate date, int page, int size) {
        ZoneId tz = ZoneId.of("Asia/Kolkata");
        Instant from = date != null ? date.atStartOfDay(tz).toInstant() : null;
        Instant to = date != null ? date.plusDays(1).atStartOfDay(tz).toInstant() : null;

        Page<PartsSale> p = saleRepository.findByFilters(
                techId, from, to, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        List<SaleReceiptResponse> content = p.getContent().stream().map(this::toReceipt).toList();
        return PageResponse.of(content, page, size, p.getTotalElements());
    }

    // -------------------------------------------------------------------------

    private List<LineResolved> resolveLinesAndValidate(SellPartsRequest req) {
        List<LineResolved> lines = new ArrayList<>();
        for (SellPartsRequest.SaleItemRequest item : req.items()) {
            SparePart part = sparePartRepository.findById(item.partId())
                    .orElseThrow(() -> new ResourceNotFoundException("SparePart", item.partId()));
            if (!part.isActive()) {
                throw new ResourceNotFoundException("SparePart (inactive)", item.partId());
            }
            if (part.getCurrentStock() < item.quantity()) {
                throw new InsufficientStockException(
                        part.getId(), part.getSku(), item.quantity(), part.getCurrentStock());
            }
            BigDecimal lineTotal = part.getSellPrice()
                    .multiply(BigDecimal.valueOf(item.quantity()));
            lines.add(new LineResolved(part.getId(), part.getName(), part.getSku(),
                    item.quantity(), part.getSellPrice(), lineTotal));
        }
        return lines;
    }

    private SaleReceiptResponse toReceipt(PartsSale s) {
        List<SaleReceiptResponse.LineItem> items = s.getItems().stream()
                .map(i -> new SaleReceiptResponse.LineItem(
                        i.getPartId(), i.getPartName(), i.getQty(), i.getUnitPrice(), i.getLineTotal()))
                .toList();
        return new SaleReceiptResponse(s.getId(), s.getTechnicianId(), s.getTechnicianName(),
                items, s.getTotalAmount(), s.getPaymentMethod(),
                s.getCreditUsed(), null, null, s.getCreatedAt());
    }

    private record LineResolved(UUID partId, String partName, String sku,
                                int qty, BigDecimal unitPrice, BigDecimal lineTotal) {}
}
