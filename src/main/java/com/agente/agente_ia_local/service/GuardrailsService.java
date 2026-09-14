package com.agente.agente_ia_local.service;

import com.agente.agente_ia_local.model.ConversationMessage;
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
public class GuardrailsService {

    private static final Logger log = LoggerFactory.getLogger(GuardrailsService.class);

    private static final Pattern PRICE_PATTERN = Pattern.compile("\\$?\\s*(\\d+[.,]?\\d*)");
    private static final Pattern SKU_PATTERN = Pattern.compile("\\b([A-Z]{2,4}-?\\d{3,6})\\b", Pattern.CASE_INSENSITIVE);
    private static final Set<String> FRUSTRATION_KEYWORDS = Set.of(
            "estoy furioso", "esto es una estafa", "no sirve", "quiero mi dinero",
            "demanda", "abogado", "queja formal", "haberme estafado",
            "un asco", "pésimo servicio", "pesimo servicio", "nunca más"
    );

    private final ProductRepository productRepository;

    public GuardrailsService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public GuardrailsResult evaluate(String message, Long tenantId, List<ConversationMessage> history) {
        String lower = message.toLowerCase(Locale.ROOT);
        List<String> warnings = new ArrayList<>();

        for (String keyword : FRUSTRATION_KEYWORDS) {
            if (lower.contains(keyword)) {
                warnings.add("FRUSTRATION_DETECTED");
                return new GuardrailsResult(true, "FRUSTRATION", "Frustración detectada en el usuario", warnings);
            }
        }

        Matcher priceMatcher = PRICE_PATTERN.matcher(message);
        while (priceMatcher.find()) {
            String priceStr = priceMatcher.group(1).replace(",", ".");
            try {
                BigDecimal claimedPrice = new BigDecimal(priceStr);
                List<Product> products = productRepository.findByTenantIdAndActiveTrue(tenantId);
                boolean foundMatchingProduct = products.stream()
                        .anyMatch(p -> p.getPrice().compareTo(claimedPrice) == 0);
                if (!foundMatchingProduct && claimedPrice.compareTo(BigDecimal.ZERO) > 0) {
                    warnings.add("PRICE_NOT_VERIFIED: $" + priceStr);
                }
            } catch (NumberFormatException ignored) {}
        }

        return new GuardrailsResult(false, null, null, warnings);
    }

    public record GuardrailsResult(boolean requiresAction, String actionType, String reason, List<String> warnings) {}
}
