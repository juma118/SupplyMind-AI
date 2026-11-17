package com.supplymind.core.product;

import com.supplymind.core.common.NotFoundException;
import com.supplymind.core.common.TenantContext;
import com.supplymind.core.common.TenantRequiredException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * README §2: tenant isolation is enforced in application authorization - every repository
 * call here is scoped by tenant explicitly, never a bare findById. These tests exist to
 * catch the easiest mistake to make in this codebase: a new method that forgets to scope
 * by tenant, which would silently leak another tenant's data.
 */
class ProductServiceTest {

    private static final String TENANT = "tenant-a";

    private ProductRepository repository;
    private ProductService service;

    @BeforeEach
    void setUp() {
        TenantContext.set(TENANT);
        repository = mock(ProductRepository.class);
        service = new ProductService(repository);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void createRejectsADuplicateSkuWithinTheSameTenant() {
        when(repository.existsByTenantIdAndSku(TENANT, "WID-1")).thenReturn(true);

        assertThatThrownBy(() -> service.create(new ProductDtos.CreateProductRequest(
                "WID-1", "Widget", new BigDecimal("8.50"), 1500)))
                .isInstanceOf(IllegalArgumentException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void createStampsTheNewProductWithTheCurrentTenant() {
        when(repository.existsByTenantIdAndSku(any(), any())).thenReturn(false);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Product created = service.create(new ProductDtos.CreateProductRequest(
                "WID-1", "Widget", new BigDecimal("8.50"), 1500));

        assertThat(created.getTenantId()).isEqualTo(TENANT);
        assertThat(created.getSku()).isEqualTo("WID-1");
    }

    @Test
    void getNeverReturnsAnotherTenantsProductEvenWithTheRightId() {
        UUID id = UUID.randomUUID();
        // Repository itself is scoped by (id, tenant) - simulating the real repository
        // behavior of returning empty when the id exists but belongs to a different tenant.
        when(repository.findByIdAndTenantId(id, TENANT)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(id)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void everyOperationRequiresTenantContextToBeSet() {
        TenantContext.clear();

        assertThatThrownBy(service::list).isInstanceOf(TenantRequiredException.class);
        assertThatThrownBy(() -> service.get(UUID.randomUUID())).isInstanceOf(TenantRequiredException.class);
        assertThatThrownBy(() -> service.create(new ProductDtos.CreateProductRequest("W", "N", null, null)))
                .isInstanceOf(TenantRequiredException.class);
    }

    @Test
    void updateDoesNotLetTheClientChangeTheSkuOnceCreated() {
        UUID id = UUID.randomUUID();
        Product existing = new Product();
        existing.setTenantId(TENANT);
        existing.setSku("WID-1");
        existing.setName("Old name");
        when(repository.findByIdAndTenantId(id, TENANT)).thenReturn(Optional.of(existing));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Product updated = service.update(id, new ProductDtos.CreateProductRequest("WID-999-IGNORED", "New name", null, null));

        assertThat(updated.getSku()).isEqualTo("WID-1");
        assertThat(updated.getName()).isEqualTo("New name");
    }
}
