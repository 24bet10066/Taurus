package com.serviceos.parts.service;

import com.serviceos.parts.entity.InventoryMovement;
import com.serviceos.parts.entity.SparePart;
import com.serviceos.parts.repository.InventoryMovementRepository;
import com.serviceos.parts.repository.SparePartRepository;
import com.serviceos.shared.exception.InsufficientStockException;
import com.serviceos.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InventoryMovementServiceTest {

    @Mock SparePartRepository sparePartRepository;
    @Mock InventoryMovementRepository movementRepository;
    @Mock PartsEventPublisher eventPublisher;

    @InjectMocks InventoryMovementService service;

    private UUID partId;
    private SparePart part;

    @BeforeEach
    void setUp() {
        partId = UUID.randomUUID();
        part = new SparePart();
        part.setId(partId);
        part.setSku("AC-001");
        part.setCurrentStock(10);
        part.setMinStock(3);
        part.setBuyPrice(BigDecimal.valueOf(200));

        given(sparePartRepository.findById(partId)).willReturn(Optional.of(part));
        given(sparePartRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
        given(movementRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("Stock-in movement increases currentStock and saves movement")
    void stockIn_increasesStock() {
        service.recordMovement(partId, "PURCHASE", 5, BigDecimal.valueOf(200), null, "admin", null);

        ArgumentCaptor<SparePart> partCaptor = ArgumentCaptor.forClass(SparePart.class);
        verify(sparePartRepository).save(partCaptor.capture());
        assertThat(partCaptor.getValue().getCurrentStock()).isEqualTo(15);
    }

    @Test
    @DisplayName("Stock-out movement decreases currentStock")
    void stockOut_decreasesStock() {
        service.recordMovement(partId, "FREELANCER_SALE", -4, BigDecimal.valueOf(500), null, "admin", null);

        ArgumentCaptor<SparePart> captor = ArgumentCaptor.forClass(SparePart.class);
        verify(sparePartRepository).save(captor.capture());
        assertThat(captor.getValue().getCurrentStock()).isEqualTo(6);
    }

    @Test
    @DisplayName("Movement record has correct stockAfter and totalValue")
    void movementRecord_hasCorrectFields() {
        service.recordMovement(partId, "PURCHASE", 5, BigDecimal.valueOf(200), null, "admin", "test");

        ArgumentCaptor<InventoryMovement> captor = ArgumentCaptor.forClass(InventoryMovement.class);
        verify(movementRepository).save(captor.capture());
        InventoryMovement saved = captor.getValue();
        assertThat(saved.getStockAfter()).isEqualTo(15);
        assertThat(saved.getTotalValue()).isEqualByComparingTo(BigDecimal.valueOf(1000));
        assertThat(saved.getNotes()).isEqualTo("test");
    }

    @Test
    @DisplayName("Negative stock throws InsufficientStockException, no DB writes")
    void negativeStock_throwsException() {
        assertThatThrownBy(() ->
                service.recordMovement(partId, "FREELANCER_SALE", -15, null, null, null, null))
                .isInstanceOf(InsufficientStockException.class);

        verify(sparePartRepository, never()).save(any());
        verify(movementRepository, never()).save(any());
    }

    @Test
    @DisplayName("Stock dropping to minStock triggers reorder alert event")
    void dropToMinStock_publishesReorderAlert() {
        // stock=10, take 7 → new stock=3 == minStock → should alert
        service.recordMovement(partId, "FREELANCER_SALE", -7, BigDecimal.valueOf(500), null, null, null);

        verify(eventPublisher).publishReorderAlert(any(), eq(3), anyInt(), anyInt());
    }

    @Test
    @DisplayName("Stock staying above minStock does not fire reorder alert")
    void stockAboveMin_noReorderAlert() {
        // stock=10, minStock=3, take 5 → new=5 > 3
        service.recordMovement(partId, "FREELANCER_SALE", -5, BigDecimal.valueOf(500), null, null, null);

        verify(eventPublisher, never()).publishReorderAlert(any(), anyInt(), anyInt(), anyInt());
    }

    @Test
    @DisplayName("Unknown partId throws ResourceNotFoundException")
    void unknownPart_throwsNotFound() {
        UUID unknown = UUID.randomUUID();
        given(sparePartRepository.findById(unknown)).willReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.recordMovement(unknown, "PURCHASE", 5, null, null, null, null))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Exact zero stock after movement is allowed")
    void exactZeroStock_allowed() {
        service.recordMovement(partId, "FREELANCER_SALE", -10, null, null, null, null);

        ArgumentCaptor<SparePart> captor = ArgumentCaptor.forClass(SparePart.class);
        verify(sparePartRepository).save(captor.capture());
        assertThat(captor.getValue().getCurrentStock()).isZero();
    }
}
