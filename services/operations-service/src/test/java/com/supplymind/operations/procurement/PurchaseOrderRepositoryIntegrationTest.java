package com.supplymind.operations.procurement;

import org.hibernate.Hibernate;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * This is the exact bug class that broke PurchaseOrderController.submit() during manual
 * testing this session: items is a LAZY collection, open-in-view is disabled, and a DTO
 * built from the repository's result outside the original transaction would throw
 * LazyInitializationException unless the query itself fetch-joins items (see
 * PurchaseOrderRepository). A Mockito unit test can't catch this - it needs a real
 * Hibernate session against a real database to prove the collection comes back already
 * initialized, not just "happens not to crash because the test kept the session open."
 *
 * Disabled in this environment: Testcontainers can't start a container here - Docker
 * Desktop's raw API returns an empty/stub 400 response regardless of which named pipe
 * endpoint is targeted (tried both the default and desktop-linux context endpoints, plus
 * an explicit DOCKER_API_VERSION pin), which is a docker-java/Testcontainers compatibility
 * issue with this Docker Desktop version, not a problem with the test or the code under
 * test. The fix this test documents was already verified against the real fetch-join
 * query in this session, live, against the actual running containers (PurchaseOrderController
 * submit()/list() returned correctly without LazyInitializationException after the fix).
 * Remove @Disabled once Testcontainers works in this environment.
 */
@Disabled("Testcontainers cannot reach Docker Desktop's API in this environment - see class Javadoc")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class PurchaseOrderRepositoryIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @org.springframework.beans.factory.annotation.Autowired
    private PurchaseOrderRepository repository;

    @org.springframework.beans.factory.annotation.Autowired
    private TestEntityManager entityManager;

    @Test
    void findAllByTenantIdReturnsItemsAlreadyInitializedEvenAfterClearingTheSession() {
        String tenantId = "tenant-test";
        PurchaseOrder po = new PurchaseOrder();
        po.setTenantId(tenantId);
        po.setSupplierId("supplier-1");
        po.setWarehouseCode("WH-A");
        po.setTotalValue(new BigDecimal("850.00"));
        PurchaseOrderItem item = new PurchaseOrderItem();
        item.setTenantId(tenantId);
        item.setProductSku("WID-1");
        item.setQuantity(100);
        item.setUnitPrice(new BigDecimal("8.50"));
        po.addItem(item);
        entityManager.persistAndFlush(po);

        // Clearing the persistence context is what makes this a real test: without it,
        // the repository call below would just hand back the same Java object already
        // sitting in memory from the persist above, proving nothing about the query itself.
        entityManager.clear();

        var results = repository.findAllByTenantId(tenantId);

        assertThat(results).hasSize(1);
        assertThat(Hibernate.isInitialized(results.get(0).getItems()))
                .as("items must be fetch-joined, not a lazy proxy, since callers map to a DTO outside this transaction")
                .isTrue();
        assertThat(results.get(0).getItems()).hasSize(1);
        assertThat(results.get(0).getItems().get(0).getProductSku()).isEqualTo("WID-1");
    }

    @Test
    void findByIdAndTenantIdAlsoReturnsItemsAlreadyInitialized() {
        String tenantId = "tenant-test";
        PurchaseOrder po = new PurchaseOrder();
        po.setTenantId(tenantId);
        po.setSupplierId("supplier-1");
        po.setWarehouseCode("WH-A");
        PurchaseOrderItem item = new PurchaseOrderItem();
        item.setTenantId(tenantId);
        item.setProductSku("WID-2");
        item.setQuantity(10);
        item.setUnitPrice(new BigDecimal("1.00"));
        po.addItem(item);
        entityManager.persistAndFlush(po);
        UUID id = po.getId();
        entityManager.clear();

        var result = repository.findByIdAndTenantId(id, tenantId);

        assertThat(result).isPresent();
        assertThat(Hibernate.isInitialized(result.get().getItems())).isTrue();
    }

    @Test
    void findAllByTenantIdNeverReturnsAnotherTenantsPurchaseOrders() {
        PurchaseOrder mine = new PurchaseOrder();
        mine.setTenantId("tenant-a");
        mine.setSupplierId("supplier-1");
        mine.setWarehouseCode("WH-A");
        entityManager.persistAndFlush(mine);

        PurchaseOrder theirs = new PurchaseOrder();
        theirs.setTenantId("tenant-b");
        theirs.setSupplierId("supplier-1");
        theirs.setWarehouseCode("WH-A");
        entityManager.persistAndFlush(theirs);
        entityManager.clear();

        var results = repository.findAllByTenantId("tenant-a");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getTenantId()).isEqualTo("tenant-a");
    }
}
