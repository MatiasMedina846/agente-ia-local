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
class GuardrailsServiceTest {

    @Autowired private GuardrailsService guardrailsService;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private ProductRepository productRepository;

    private Tenant tenant;

    @BeforeEach
    void setup() {
        tenant = tenantRepository.save(new Tenant("Guard Test", "guard-test"));
    }

    @AfterEach
    void cleanup() {
        productRepository.deleteAll();
        tenantRepository.deleteAll();
    }

    @Test
    @DisplayName("No causa handoff por solicitud de persona humana (el chatbot lo maneja)")
    void humanHandoffRequest() {
        var result = guardrailsService.evaluate("Quiero hablar con una persona", tenant.getId(), List.of());
        assertFalse(result.requiresAction(), "El chatbot maneja esta solicitud graceful");
    }

    @Test
    @DisplayName("Handoff por frustración")
    void frustrationDetected() {
        var result = guardrailsService.evaluate("Estoy furioso con el servicio", tenant.getId(), List.of());
        assertTrue(result.requiresAction());
        assertEquals("FRUSTRATION", result.actionType());
        assertTrue(result.warnings().contains("FRUSTRATION_DETECTED"));
    }

    @Test
    @DisplayName("Precio no verificado genera warning")
    void priceNotVerified() {
        var result = guardrailsService.evaluate("El producto cuesta $999.99", tenant.getId(), List.of());
        assertFalse(result.requiresAction());
        assertTrue(result.warnings().stream().anyMatch(w -> w.startsWith("PRICE_NOT_VERIFIED")));
    }

    @Test
    @DisplayName("Precio verificado (coincide con producto) no genera warning")
    void priceVerified() {
        Product product = new Product();
        product.setTenantId(tenant.getId());
        product.setSku("TEST-001");
        product.setName("Zapato Test");
        product.setPrice(new BigDecimal("999.99"));
        product.setStock(10);
        productRepository.save(product);

        var result = guardrailsService.evaluate("El producto cuesta $999.99", tenant.getId(), List.of());
        assertFalse(result.requiresAction());
        assertTrue(result.warnings().isEmpty());
    }

    @Test
    @DisplayName("Mensaje normal sin alertas")
    void normalMessage() {
        var result = guardrailsService.evaluate("Hola, ¿tienen zapatos?", tenant.getId(), List.of());
        assertFalse(result.requiresAction());
        assertNull(result.actionType());
    }

    @Test
    @DisplayName("Solicitud de agente humano no causa handoff automático (el chatbot lo maneja)")
    void agentRequest() {
        var result = guardrailsService.evaluate("Necesito un asesor humano", tenant.getId(), List.of());
        assertFalse(result.requiresAction(), "El chatbot maneja esto graceful, no necesita handoff automático");
    }

    @Test
    @DisplayName("Reclamo detectado")
    void complaintDetected() {
        var result = guardrailsService.evaluate("esto es una estafa, quiero mi dinero", tenant.getId(), List.of());
        assertTrue(result.requiresAction());
        assertEquals("FRUSTRATION", result.actionType());
    }
}
