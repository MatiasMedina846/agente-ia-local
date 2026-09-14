package com.agente.agente_ia_local.service;

import com.agente.agente_ia_local.model.Product;
import com.agente.agente_ia_local.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class HybridSearchService {

    private static final Logger log = LoggerFactory.getLogger(HybridSearchService.class);
    private static final Pattern SKU_PATTERN = Pattern.compile("\\b([A-Z]{2,4}-?\\d{3,6})\\b", Pattern.CASE_INSENSITIVE);

    private final ProductRepository productRepository;

    public HybridSearchService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<Product> search(Long tenantId, String query) {
        Set<Long> seenIds = new LinkedHashSet<>();
        List<Product> results = new ArrayList<>();

        Matcher skuMatcher = SKU_PATTERN.matcher(query);
        if (skuMatcher.find()) {
            String sku = skuMatcher.group(1).toUpperCase();
            productRepository.findByTenantIdAndSku(tenantId, sku).ifPresent(product -> {
                results.add(product);
                seenIds.add(product.getId());
            });
        }

        List<Product> keywordResults = productRepository.searchByKeyword(tenantId, query);
        for (Product p : keywordResults) {
            if (seenIds.add(p.getId())) {
                results.add(p);
            }
        }

        return results;
    }

    public String formatProductContext(List<Product> products) {
        if (products == null || products.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("[Información de Productos Disponibles]").append(System.lineSeparator());
        for (Product p : products) {
            sb.append("- ").append(p.getName())
              .append(" (SKU: ").append(p.getSku()).append(")")
              .append(" | Precio: $").append(p.getPrice())
              .append(" | Stock: ").append(p.getStock() > 0 ? "Disponible" : "Agotado");
            if (p.getDescription() != null && !p.getDescription().isBlank()) {
                sb.append(" | ").append(p.getDescription());
            }
            sb.append(System.lineSeparator());
        }
        return sb.toString();
    }
}
