package com.agente.agente_ia_local;

import com.agente.agente_ia_local.model.Tenant;
import com.agente.agente_ia_local.model.TenantCredentials;
import com.agente.agente_ia_local.repository.TenantRepository;
import com.agente.agente_ia_local.repository.TenantCredentialsRepository;
import com.agente.agente_ia_local.service.TenantService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TenantServiceTest {

    @Autowired private TenantService tenantService;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private TenantCredentialsRepository credentialsRepository;

    @AfterEach
    void cleanup() {
        credentialsRepository.deleteAll();
        tenantRepository.deleteAll();
    }

    @Test
    @Order(1)
    @DisplayName("Crear tenant exitosamente")
    void createTenant() {
        Tenant tenant = new Tenant("Zapatería López", "zapateria-lopez");
        tenant.setBusinessName("López S.A.");
        tenant.setToneOfVoice("FRIENDLY");
        tenant.setSystemPrompt("Eres un asistente de zapatería.");
        tenant.setWelcomeMessage("¡Hola! Bienvenido a Zapatería López");

        Tenant saved = tenantService.create(tenant);

        assertNotNull(saved.getId());
        assertEquals("Zapatería López", saved.getName());
        assertEquals("zapateria-lopez", saved.getSlug());
        assertTrue(saved.isActive());
    }

    @Test
    @Order(2)
    @DisplayName("Listar todos los tenants")
    void listTenants() {
        tenantService.create(new Tenant("Negocio A", "negocio-a"));
        tenantService.create(new Tenant("Negocio B", "negocio-b"));

        List<Tenant> tenants = tenantService.findAll();
        assertEquals(2, tenants.size());
    }

    @Test
    @Order(3)
    @DisplayName("Buscar tenant por ID")
    void findById() {
        Tenant saved = tenantService.create(new Tenant("Find Me", "find-me"));
        Optional<Tenant> found = tenantService.findById(saved.getId());

        assertTrue(found.isPresent());
        assertEquals("Find Me", found.get().getName());
    }

    @Test
    @Order(4)
    @DisplayName("Buscar tenant por slug")
    void findBySlug() {
        tenantService.create(new Tenant("Slug Test", "my-slug"));
        Optional<Tenant> found = tenantService.findBySlug("my-slug");

        assertTrue(found.isPresent());
        assertEquals("Slug Test", found.get().getName());
    }

    @Test
    @Order(5)
    @DisplayName("Actualizar tenant")
    void updateTenant() {
        Tenant saved = tenantService.create(new Tenant("Old Name", "update-test"));
        saved.setName("New Name");
        saved.setToneOfVoice("FORMAL");
        saved.setSystemPrompt("Eres formal y profesional.");

        Tenant updated = tenantService.update(saved.getId(), saved);

        assertEquals("New Name", updated.getName());
        assertEquals("FORMAL", updated.getToneOfVoice());
    }

    @Test
    @Order(6)
    @DisplayName("Guardar y recuperar credenciales encriptadas")
    void saveAndGetCredentials() {
        Tenant saved = tenantService.create(new Tenant("Creds", "creds-test"));

        TenantCredentials creds = new TenantCredentials(saved.getId());
        creds.setWhatsappApiKey("sk-test-api-key-12345");
        creds.setWhatsappPhoneNumberId("1234567890");
        creds.setWhisperApiKey("sk-whisper-key");

        tenantService.saveCredentials(creds);

        Optional<TenantCredentials> found = tenantService.getCredentials(saved.getId());
        assertTrue(found.isPresent());
        assertEquals("1234567890", found.get().getWhatsappPhoneNumberId());

        // Verificar que la API key se almacena y recupera correctamente (encriptación transparente)
        TenantCredentials rawFromDb = credentialsRepository.findByTenantId(saved.getId()).orElseThrow();
        assertEquals("sk-test-api-key-12345", rawFromDb.getWhatsappApiKey(),
                "El converter debe desencriptar al leer de la BD");
    }

    @Test
    @Order(7)
    @DisplayName("Buscar credenciales por phone number ID")
    void getCredentialsByPhoneId() {
        Tenant saved = tenantService.create(new Tenant("Phone", "phone-test"));
        TenantCredentials creds = new TenantCredentials(saved.getId());
        creds.setWhatsappPhoneNumberId("999999");
        tenantService.saveCredentials(creds);

        Optional<TenantCredentials> found = tenantService.getCredentialsByPhoneNumberId("999999");
        assertTrue(found.isPresent());
        assertEquals(saved.getId(), found.get().getTenantId());
    }

    @Test
    @Order(8)
    @DisplayName("Tenant inexistente retorna vacío")
    void nonExistent() {
        assertFalse(tenantService.findById(999L).isPresent());
        assertFalse(tenantService.findBySlug("nonexistent").isPresent());
    }

    @Test
    @Order(9)
    @DisplayName("Credenciales sobrescribir existentes")
    void overwriteCredentials() {
        Tenant saved = tenantService.create(new Tenant("Over", "over-test"));
        TenantCredentials creds = new TenantCredentials(saved.getId());
        creds.setWhatsappPhoneNumberId("111");
        tenantService.saveCredentials(creds);

        creds.setWhatsappPhoneNumberId("222");
        tenantService.saveCredentials(creds);

        Optional<TenantCredentials> found = tenantService.getCredentials(saved.getId());
        assertEquals("222", found.get().getWhatsappPhoneNumberId());
    }
}
