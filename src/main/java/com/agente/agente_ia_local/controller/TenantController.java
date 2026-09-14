package com.agente.agente_ia_local.controller;

import com.agente.agente_ia_local.model.Tenant;
import com.agente.agente_ia_local.model.TenantCredentials;
import com.agente.agente_ia_local.service.TenantService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tenants")
@CrossOrigin(origins = "*")
public class TenantController {

    private final TenantService tenantService;

    public TenantController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    @GetMapping
    public ResponseEntity<List<Tenant>> listAll() {
        return ResponseEntity.ok(tenantService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Tenant> getById(@PathVariable Long id) {
        return tenantService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Tenant> create(@RequestBody Tenant tenant) {
        return ResponseEntity.ok(tenantService.create(tenant));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Tenant> update(@PathVariable Long id, @RequestBody Tenant tenant) {
        return ResponseEntity.ok(tenantService.update(id, tenant));
    }

    @GetMapping("/{id}/credentials")
    public ResponseEntity<TenantCredentials> getCredentials(@PathVariable Long id) {
        return tenantService.getCredentials(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/credentials")
    public ResponseEntity<TenantCredentials> saveCredentials(@PathVariable Long id, @RequestBody TenantCredentials creds) {
        creds.setTenantId(id);
        return ResponseEntity.ok(tenantService.saveCredentials(creds));
    }
}
