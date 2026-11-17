package com.supplymind.operations.inventory;

import com.supplymind.operations.common.InsufficientStockException;
import com.supplymind.operations.common.TenantContext;
import com.supplymind.operations.common.TenantRequiredException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * README §7: unique balance per (tenant, warehouse, product), movements as an immutable
 * ledger, never letting available stock go negative. This is the money path - an off-by-one
 * here either blocks a legitimate transfer/adjustment or lets stock go negative silently.
 */
class InventoryServiceTest {

    private static final String TENANT = "tenant-test";

    private InventoryBalanceRepository balanceRepository;
    private InventoryMovementRepository movementRepository;
    private InventoryService service;

    @BeforeEach
    void setUp() {
        TenantContext.set(TENANT);
        balanceRepository = mock(InventoryBalanceRepository.class);
        movementRepository = mock(InventoryMovementRepository.class);
        service = new InventoryService(balanceRepository, movementRepository);
        when(balanceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void transferMovesUnitsFromSourceToDestinationAndRecordsBothMovements() {
        InventoryBalance source = balanceWith("WH-C", "WID-1", 3800);
        InventoryBalance dest = balanceWith("WH-A", "WID-1", 2400);
        when(balanceRepository.findByTenantIdAndWarehouseCodeAndProductSku(TENANT, "WH-C", "WID-1"))
                .thenReturn(Optional.of(source));
        when(balanceRepository.findByTenantIdAndWarehouseCodeAndProductSku(TENANT, "WH-A", "WID-1"))
                .thenReturn(Optional.of(dest));

        service.transfer(new InventoryDtos.TransferRequest("WH-C", "WH-A", "WID-1", 1200, "TEST", "ref-1"));

        assertThat(source.getAvailableUnits()).isEqualTo(2600);
        assertThat(dest.getAvailableUnits()).isEqualTo(3600);

        verify(movementRepository).save(argThat(m ->
                m.getMovementType() == MovementType.TRANSFER_OUT && m.getWarehouseCode().equals("WH-C") && m.getQuantity() == 1200));
        verify(movementRepository).save(argThat(m ->
                m.getMovementType() == MovementType.TRANSFER_IN && m.getWarehouseCode().equals("WH-A") && m.getQuantity() == 1200));
    }

    @Test
    void transferRejectedWhenSourceHasInsufficientStock() {
        InventoryBalance source = balanceWith("WH-C", "WID-1", 100);
        when(balanceRepository.findByTenantIdAndWarehouseCodeAndProductSku(TENANT, "WH-C", "WID-1"))
                .thenReturn(Optional.of(source));

        assertThatThrownBy(() -> service.transfer(new InventoryDtos.TransferRequest("WH-C", "WH-A", "WID-1", 1200, "TEST", "ref-1")))
                .isInstanceOf(InsufficientStockException.class);

        // Must not have mutated the source balance or recorded any movement on a rejected transfer.
        assertThat(source.getAvailableUnits()).isEqualTo(100);
        verify(movementRepository, never()).save(any());
    }

    @Test
    void adjustmentIncreasesAvailableUnits() {
        InventoryBalance balance = balanceWith("WH-A", "WID-1", 2400);
        when(balanceRepository.findByTenantIdAndWarehouseCodeAndProductSku(TENANT, "WH-A", "WID-1"))
                .thenReturn(Optional.of(balance));

        service.adjust(new InventoryDtos.AdjustmentRequest("WH-A", "WID-1", 500, "seed"));

        assertThat(balance.getAvailableUnits()).isEqualTo(2900);
    }

    @Test
    void adjustmentRejectedWhenItWouldMakeAvailableUnitsNegative() {
        InventoryBalance balance = balanceWith("WH-A", "WID-1", 100);
        when(balanceRepository.findByTenantIdAndWarehouseCodeAndProductSku(TENANT, "WH-A", "WID-1"))
                .thenReturn(Optional.of(balance));

        assertThatThrownBy(() -> service.adjust(new InventoryDtos.AdjustmentRequest("WH-A", "WID-1", -500, "oops")))
                .isInstanceOf(InsufficientStockException.class);
        assertThat(balance.getAvailableUnits()).isEqualTo(100);
    }

    @Test
    void receiveIncrementsBalanceAndRecordsReceiptMovementAgainstThePurchaseOrder() {
        InventoryBalance balance = balanceWith("WH-F", "WID-1", 0);
        when(balanceRepository.findByTenantIdAndWarehouseCodeAndProductSku(TENANT, "WH-F", "WID-1"))
                .thenReturn(Optional.of(balance));

        service.receive(TENANT, "WH-F", "WID-1", 500, "po-123");

        assertThat(balance.getAvailableUnits()).isEqualTo(500);
        verify(movementRepository).save(argThat(m ->
                m.getMovementType() == MovementType.RECEIPT && m.getReferenceId().equals("po-123") && m.getQuantity() == 500));
    }

    @Test
    void getOrCreateMakesANewZeroBalanceWhenNoneExistsYet() {
        when(balanceRepository.findByTenantIdAndWarehouseCodeAndProductSku(TENANT, "WH-NEW", "WID-1"))
                .thenReturn(Optional.empty());

        service.adjust(new InventoryDtos.AdjustmentRequest("WH-NEW", "WID-1", 50, "first stock"));

        verify(balanceRepository, atLeastOnce()).save(argThat(b ->
                b.getWarehouseCode().equals("WH-NEW") && b.getTenantId().equals(TENANT)));
    }

    @Test
    void listRequiresTenantContext() {
        TenantContext.clear();
        assertThatThrownBy(service::list).isInstanceOf(TenantRequiredException.class);
    }

    @Test
    void lowStockOnlyReturnsBalancesBelowTheirOwnReorderPoint() {
        InventoryBalance belowReorder = balanceWith("WH-A", "WID-1", 100);
        belowReorder.setReorderPoint(1500);
        InventoryBalance aboveReorder = balanceWith("WH-A", "WID-2", 5000);
        aboveReorder.setReorderPoint(1500);
        InventoryBalance noReorderPointSet = balanceWith("WH-A", "WID-3", 1);
        when(balanceRepository.findAllByTenantId(TENANT)).thenReturn(java.util.List.of(belowReorder, aboveReorder, noReorderPointSet));

        var result = service.lowStock();

        assertThat(result).containsExactly(belowReorder);
    }

    private InventoryBalance balanceWith(String warehouseCode, String productSku, int availableUnits) {
        InventoryBalance balance = new InventoryBalance();
        balance.setTenantId(TENANT);
        balance.setWarehouseCode(warehouseCode);
        balance.setProductSku(productSku);
        balance.setAvailableUnits(availableUnits);
        return balance;
    }
}
