package com.serviceos.parts.service;

import com.serviceos.parts.entity.InventoryMovement;
import com.serviceos.parts.entity.SparePart;
import com.serviceos.parts.repository.InventoryMovementRepository;
import com.serviceos.parts.repository.SparePartRepository;
import com.serviceos.shared.exception.InsufficientStockException;
import com.serviceos.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
public class InventoryMovementService {

    private final SparePartRepository sparePartRepository;
    private final InventoryMovementRepository movementRepository;
    private final PartsEventPublisher eventPublisher;

    public InventoryMovementService(SparePartRepository sparePartRepository,
                                    InventoryMovementRepository movementRepository,
                                    PartsEventPublisher eventPublisher) {
        this.sparePartRepository = sparePartRepository;
        this.movementRepository = movementRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Records a stock movement and atomically updates current_stock.
     * Positive quantity = stock in; negative = stock out.
     * Throws {@link InsufficientStockException} if the result would go below zero.
     * Must always be called inside an existing transaction (Propagation.MANDATORY).
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public InventoryMovement recordMovement(UUID partId,
                                            String movementType,
                                            int quantity,
                                            BigDecimal unitPrice,
                                            UUID referenceId,
                                            String recordedBy,
                                            String notes) {
        SparePart part = sparePartRepository.findById(partId)
                .orElseThrow(() -> new ResourceNotFoundException("SparePart", partId));

        int newStock = part.getCurrentStock() + quantity;
        if (newStock < 0) {
            throw new InsufficientStockException(
                    partId, part.getSku(),
                    Math.abs(quantity), part.getCurrentStock()
            );
        }

        part.setCurrentStock(newStock);
        sparePartRepository.save(part);

        InventoryMovement movement = new InventoryMovement();
        movement.setPartId(partId);
        movement.setMovementType(movementType);
        movement.setQuantity(quantity);
        movement.setStockAfter(newStock);
        movement.setUnitPrice(unitPrice);
        if (unitPrice != null) {
            movement.setTotalValue(unitPrice.multiply(BigDecimal.valueOf(Math.abs(quantity))));
        }
        movement.setReferenceId(referenceId);
        movement.setRecordedBy(recordedBy);
        movement.setNotes(notes);
        movement.setCreatedAt(Instant.now());
        InventoryMovement saved = movementRepository.save(movement);

        // Fire reorder alert if stock has dropped to or below min_stock
        if (newStock <= part.getMinStock()) {
            eventPublisher.publishReorderAlert(part, newStock, 0, part.getMinStock() * 2 - newStock);
        }

        return saved;
    }
}
