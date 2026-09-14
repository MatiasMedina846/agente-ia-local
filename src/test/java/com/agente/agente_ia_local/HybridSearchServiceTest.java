package com.agente.agente_ia_local;

import com.agente.agente_ia_local.model.*;
import com.agente.agente_ia_local.repository.*;
import com.agente.agente_ia_local.service.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class HybridSearchServiceTest {

    @Autowired private HybridSearchService searchService;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private ProductRepository productRepository;

    private Tenant tenant;

    @BeforeEach
    void setup() {
        tenant = tenantRepository.save(new Tenant("Search Test", "search-test"));
        createProduct("ZAP-001", "Zapato Nike Air", "Zapatilla deportiva Nike", new BigDecimal("15000"), "calzado,deportivo");
        createProduct("ZAP-002", "Bota Timberland", "Bota de cuero resistente", new BigDecimal("25000"), "calzado,bota");
        createProduct("ACC-001", "Cinturón de cuero", "Cinturón artesanal", new BigDecimal("8000"), "accesorio,cuero");
    }

    private void createProduct(String sku, String name, String desc, BigDecimal price, String tags) {
        Product p = new Product();
        p.setTenantId(tenant.getId());
        p.setSku(sku);
        p.setName(name);
        p.setDescription(desc);
        p.setPrice(price);
        p.setTags(tags);
        p.setStock(5);
        p.setCategories("calzado");
        productRepository.save(p);
    }

    @AfterEach
    void cleanup() {
        productRepository.deleteAll();
        tenantRepository.deleteAll();
    }

    @Test
    @DisplayName("Búsqueda por SKU exacto encuentra el producto")
    void searchBySku() {
        List<Product> results = searchService.search(tenant.getId(), "ZAP-001");
        assertFalse(results.isEmpty());
        assertEquals("Zapato Nike Air", results.get(0).getName());
    }

    @Test
    @DisplayName("Búsqueda por keyword en nombre")
    void searchByName() {
        List<Product> results = searchService.search(tenant.getId(), "Nike");
        assertFalse(results.isEmpty());
        assertTrue(results.stream().anyMatch(p -> p.getName().contains("Nike")));
    }

    @Test
    @DisplayName("Búsqueda por keyword en descripción")
    void searchByDescription() {
        List<Product> results = searchService.search(tenant.getId(), "cuero");
        assertFalse(results.isEmpty());
    }

    @Test
    @DisplayName("Búsqueda por keyword en tags")
    void searchByTags() {
        List<Product> results = searchService.search(tenant.getId(), "deportivo");
        assertFalse(results.isEmpty());
    }

    @Test
    @DisplayName("Búsqueda sin resultados")
    void searchNoResults() {
        List<Product> results = searchService.search(tenant.getId(), "televisorxyz123");
        assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("Formateo de contexto de productos")
    void formatContext() {
        List<Product> products = searchService.search(tenant.getId(), "Zapato");
        String context = searchService.formatProductContext(products);
        assertFalse(context.isBlank());
        assertTrue(context.contains("[Información de Productos Disponibles]"));
        assertTrue(context.contains("Zapato Nike Air"));
        assertTrue(context.contains("SKU: ZAP-001"));
    }

    @Test
    @DisplayName("Contexto vacío cuando no hay resultados")
    void formatEmptyContext() {
        String context = searchService.formatProductContext(List.of());
        assertTrue(context.isBlank());
    }
}
