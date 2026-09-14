package com.agente.agente_ia_local.config;

import com.agente.agente_ia_local.model.Tenant;
import com.agente.agente_ia_local.repository.TenantRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class TenantInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(TenantInterceptor.class);
    private static final String HEADER_TENANT = "X-Tenant-Id";

    private final TenantRepository tenantRepository;

    public TenantInterceptor(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String tenantHeader = request.getHeader(HEADER_TENANT);
        if (tenantHeader != null && !tenantHeader.isBlank()) {
            try {
                Long tenantId = Long.parseLong(tenantHeader);
                Tenant tenant = tenantRepository.findById(tenantId).orElse(null);
                if (tenant != null && tenant.isActive()) {
                    TenantContext.setTenantId(tenantId);
                    return true;
                }
            } catch (NumberFormatException e) {
                log.warn("X-Tenant-Id inválido: {}", tenantHeader);
            }
        }
        String path = request.getRequestURI();
        if (path.startsWith("/api/admin") || path.startsWith("/api/tenants")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        TenantContext.clear();
    }
}
