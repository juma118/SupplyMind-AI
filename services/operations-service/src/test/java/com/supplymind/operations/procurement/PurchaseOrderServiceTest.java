package com.supplymind.operations.procurement;

import com.supplymind.operations.common.InvalidStateTransitionException;
import com.supplymind.operations.common.TenantContext;
import com.supplymind.operations.inventory.InventoryService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * README §5: "[the Procurement Agent] cannot independently authorize or submit [purchase
 * orders]" - submit()/receive() are the only path from DRAFT to something that actually
 * moves inventory, so the state machine here is the real enforcement of that rule, not
 * just documentation of intent.
 */
class PurchaseOrderServiceTest {

    private static final String TENANT = "tenant-test";
    private static final UUID PO_ID = UUID.randomUUID();

    private PurchaseOrderRepository repository;
    private InventoryService inventoryService;
    private PurchaseOrderService service;

    @BeforeEach
    void setUp() {
        TenantContext.set(TENANT);
        repository = mock(PurchaseOrderRepository.class);
        inventoryService = mock(InventoryService.class);
        service = new PurchaseOrderService(repository, inventoryService);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void submitMovesDraftToSubmitted() {
        PurchaseOrder po = poWithStatus(PurchaseOrderStatus.DRAFT);
        when(repository.findByIdAndTenantId(PO_ID, TENANT)).thenReturn(Optional.of(po));

        PurchaseOrder result = service.submit(PO_ID);

        assertThat(result.getStatus()).isEqualTo(PurchaseOrderStatus.SUBMITTED);
    }

    @Test
    void submitRejectedWhenNotInDraft() {
        PurchaseOrder po = poWithStatus(PurchaseOrderStatus.SUBMITTED);
        when(repository.findByIdAndTenantId(PO_ID, TENANT)).thenReturn(Optional.of(po));

        assertThatThrownBy(() -> service.submit(PO_ID)).isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    void receiveMovesSubmittedToReceivedAndCreditsInventoryForEveryItem() {
        PurchaseOrder po = poWithStatus(PurchaseOrderStatus.SUBMITTED);
        po.setWarehouseCode("WH-F");
        PurchaseOrderItem item1 = item("WID-1", 500, "8.50");
        PurchaseOrderItem item2 = item("WID-2", 200, "3.00");
        po.addItem(item1);
        po.addItem(item2);
        when(repository.findByIdAndTenantId(PO_ID, TENANT)).thenReturn(Optional.of(po));

        PurchaseOrder result = service.receive(PO_ID);

        assertThat(result.getStatus()).isEqualTo(PurchaseOrderStatus.RECEIVED);
        verify(inventoryService).receive(eq(TENANT), eq("WH-F"), eq("WID-1"), eq(500), any());
        verify(inventoryService).receive(eq(TENANT), eq("WH-F"), eq("WID-2"), eq(200), any());
    }

    @Test
    void receiveRejectedWhenNotSubmittedYet() {
        PurchaseOrder po = poWithStatus(PurchaseOrderStatus.DRAFT);
        when(repository.findByIdAndTenantId(PO_ID, TENANT)).thenReturn(Optional.of(po));

        assertThatThrownBy(() -> service.receive(PO_ID)).isInstanceOf(InvalidStateTransitionException.class);
        verifyNoInteractions(inventoryService);
    }

    @Test
    void receiveRejectedWhenAlreadyReceivedCannotDoubleCreditInventory() {
        PurchaseOrder po = poWithStatus(PurchaseOrderStatus.RECEIVED);
        when(repository.findByIdAndTenantId(PO_ID, TENANT)).thenReturn(Optional.of(po));

        assertThatThrownBy(() -> service.receive(PO_ID)).isInstanceOf(InvalidStateTransitionException.class);
        verifyNoInteractions(inventoryService);
    }

    @Test
    void createComputesTotalValueFromAllItems() {
        var request = new PurchaseOrderDtos.CreatePurchaseOrderRequest(
                "supplier-1", "WH-A",
                java.util.List.of(
                        new PurchaseOrderDtos.CreateItemRequest("WID-1", 100, new BigDecimal("8.50")),
                        new PurchaseOrderDtos.CreateItemRequest("WID-2", 50, new BigDecimal("3.00"))),
                "MANUAL", null);

        PurchaseOrder result = service.create(request);

        // 100*8.50 + 50*3.00 = 850.00 + 150.00
        assertThat(result.getTotalValue()).isEqualByComparingTo("1000.00");
        assertThat(result.getItems()).hasSize(2);
    }

    private PurchaseOrder poWithStatus(PurchaseOrderStatus status) {
        PurchaseOrder po = new PurchaseOrder();
        po.setTenantId(TENANT);
        po.setStatus(status);
        // id has no setter (JPA-generated only) - receive() calls po.getId().toString(),
        // so a manually-constructed PO needs this set via reflection or that call NPEs.
        org.springframework.test.util.ReflectionTestUtils.setField(po, "id", PO_ID);
        return po;
    }

    private PurchaseOrderItem item(String sku, int quantity, String unitPrice) {
        PurchaseOrderItem item = new PurchaseOrderItem();
        item.setTenantId(TENANT);
        item.setProductSku(sku);
        item.setQuantity(quantity);
        item.setUnitPrice(new BigDecimal(unitPrice));
        return item;
    }
}
