package com.serviceos.parts.service;

import com.serviceos.parts.dto.request.CreatePartRequest;
import com.serviceos.parts.dto.request.StockAdjustmentRequest;
import com.serviceos.parts.dto.request.UpdatePartRequest;
import com.serviceos.parts.dto.response.*;
import com.serviceos.parts.entity.SparePart;
import com.serviceos.parts.repository.InventoryMovementRepository;
import com.serviceos.parts.repository.SparePartRepository;
import com.serviceos.parts.service.trie.PartsTrieService;
import com.serviceos.shared.dto.PageResponse;
import com.serviceos.shared.exception.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class PartsService {

    private final SparePartRepository sparePartRepository;
    private final InventoryMovementRepository movementRepository;
    private final InventoryMovementService movementService;
    private final PartsTrieService trieService;

    public PartsService(SparePartRepository sparePartRepository,
                        InventoryMovementRepository movementRepository,
                        InventoryMovementService movementService,
                        PartsTrieService trieService) {
        this.sparePartRepository = sparePartRepository;
        this.movementRepository = movementRepository;
        this.movementService = movementService;
        this.trieService = trieService;
    }

    // -------------------------------------------------------------------------
    // Search
    // -------------------------------------------------------------------------

    public List<PartSearchResult> search(String query, int limit) {
        List<PartSearchResult> results = trieService.search(query, limit);
        if (!results.isEmpty()) return results;
        // Fallback to PostgreSQL full-text search
        return sparePartRepository.fullTextSearch(query, limit)
                .stream().map(this::toSearchResult).toList();
    }

    // -------------------------------------------------------------------------
    // CRUD
    // -------------------------------------------------------------------------

    @Transactional
    public PartResponse createPart(CreatePartRequest req, String createdBy) {
        SparePart part = new SparePart();
        applyCreate(part, req);
        part = sparePartRepository.save(part);

        // Record opening stock if provided
        if (req.currentStock() > 0) {
            movementService.recordMovement(part.getId(), "OPENING_STOCK",
                    req.currentStock(), req.buyPrice(), null, createdBy, "Opening stock");
        }

        trieService.insert(part);
        return toResponse(part);
    }

    @Transactional
    public PartResponse updatePart(UUID id, UpdatePartRequest req) {
        SparePart part = requirePart(id);
        applyUpdate(part, req);
        return toResponse(sparePartRepository.save(part));
    }

    @Transactional(readOnly = true)
    public PageResponse<PartResponse> listParts(String category, String applianceType,
                                                String brand, Boolean active,
                                                int page, int size) {
        Page<SparePart> p = sparePartRepository.findByFilters(
                category, applianceType, brand, active,
                PageRequest.of(page, size, Sort.by("name")));
        return PageResponse.of(p.getContent().stream().map(this::toResponse).toList(),
                page, size, p.getTotalElements());
    }

    @Transactional(readOnly = true)
    public List<LowStockResponse> getLowStock() {
        return sparePartRepository.findAllActive().stream()
                .filter(p -> p.getCurrentStock() <= p.getMinStock())
                .map(p -> new LowStockResponse(
                        p.getId(), p.getName(), p.getSku(), p.getLocation(),
                        p.getCurrentStock(), p.getMinStock(),
                        Math.max(0, p.getMinStock() - p.getCurrentStock())))
                .toList();
    }

    // -------------------------------------------------------------------------
    // Stock adjustment
    // -------------------------------------------------------------------------

    @Transactional
    public MovementResponse adjust(UUID partId, StockAdjustmentRequest req, String recordedBy) {
        var movement = movementService.recordMovement(
                partId, req.movementType(), req.quantity(), null, null, recordedBy, req.reason());
        return toMovementResponse(movement);
    }

    // -------------------------------------------------------------------------
    // Movements history
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public PageResponse<MovementResponse> getMovements(UUID partId, int page, int size) {
        requirePart(partId);
        var p = movementRepository.findByPartIdOrderByCreatedAtDesc(
                partId, PageRequest.of(page, size));
        return PageResponse.of(p.getContent().stream().map(this::toMovementResponse).toList(),
                page, size, p.getTotalElements());
    }

    // -------------------------------------------------------------------------
    // Internal price endpoint
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public InternalPriceResponse getInternalPrice(UUID partId) {
        SparePart p = requirePart(partId);
        return new InternalPriceResponse(p.getId(), p.getName(), p.getSku(),
                p.getSellPrice(), p.getInternalPrice(), p.getCurrentStock());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    public SparePart requirePart(UUID id) {
        return sparePartRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SparePart", id));
    }

    private void applyCreate(SparePart p, CreatePartRequest r) {
        p.setName(r.name()); p.setSku(r.sku()); p.setCategory(r.category());
        p.setApplianceType(r.applianceType()); p.setBrand(r.brand()); p.setOem(r.oem());
        p.setBuyPrice(r.buyPrice()); p.setSellPrice(r.sellPrice()); p.setInternalPrice(r.internalPrice());
        p.setCurrentStock(0);  // stock set via OPENING_STOCK movement
        p.setMinStock(r.minStock()); p.setLocation(r.location()); p.setFastMoving(r.fastMoving());
    }

    private void applyUpdate(SparePart p, UpdatePartRequest r) {
        if (r.name() != null)          p.setName(r.name());
        if (r.category() != null)      p.setCategory(r.category());
        if (r.applianceType() != null) p.setApplianceType(r.applianceType());
        if (r.brand() != null)         p.setBrand(r.brand());
        if (r.oem() != null)           p.setOem(r.oem());
        if (r.buyPrice() != null)      p.setBuyPrice(r.buyPrice());
        if (r.sellPrice() != null)     p.setSellPrice(r.sellPrice());
        if (r.internalPrice() != null) p.setInternalPrice(r.internalPrice());
        if (r.minStock() != null)      p.setMinStock(r.minStock());
        if (r.location() != null)      p.setLocation(r.location());
        if (r.fastMoving() != null)    p.setFastMoving(r.fastMoving());
        if (r.active() != null)        p.setActive(r.active());
    }

    PartResponse toResponse(SparePart p) {
        return new PartResponse(
                p.getId(), p.getName(), p.getSku(), p.getCategory(), p.getApplianceType(),
                p.getBrand(), p.isOem(), p.getBuyPrice(), p.getSellPrice(), p.getInternalPrice(),
                p.getCurrentStock(), p.getMinStock(), p.getLocation(), p.isFastMoving(), p.isActive(),
                p.getWeeklyForecast(), p.getReorderPoint(), p.getCreatedAt()
        );
    }

    private PartSearchResult toSearchResult(SparePart p) {
        return new PartSearchResult(p.getId(), p.getName(), p.getSku(), p.getBrand(),
                p.getCurrentStock(), p.getSellPrice(), p.getLocation());
    }

    MovementResponse toMovementResponse(com.serviceos.parts.entity.InventoryMovement m) {
        return new MovementResponse(m.getId(), m.getPartId(), m.getMovementType(),
                m.getQuantity(), m.getStockAfter(), m.getUnitPrice(), m.getTotalValue(),
                m.getReferenceId(), m.getRecordedBy(), m.getNotes(), m.getCreatedAt());
    }
}
