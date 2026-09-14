package com.agente.agente_ia_local.service;

import com.agente.agente_ia_local.model.ChatMessage;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class OllamaAiServiceImpl implements AiService {

    private static final Logger log = LoggerFactory.getLogger(OllamaAiServiceImpl.class);
    private static final Pattern PRODUCT_PATTERN = Pattern.compile(
            "\\[Información de Productos Disponibles\\]\\n(.+?)\\n\\n",
            Pattern.DOTALL);
    private static final Pattern SYSTEM_PROMPT_PATTERN = Pattern.compile(
            "\\[Instrucciones del sistema\\]\\n(.+?)\\n\\n",
            Pattern.DOTALL);

    private final RestTemplate restTemplate;
    private final Random random = new Random();

    @Value("${ai.ollama.model:llama3}")
    private String model;

    @Value("${ai.ollama.url:http://localhost:11434}")
    private String ollamaUrl;

    public OllamaAiServiceImpl() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(2));
        factory.setReadTimeout(Duration.ofSeconds(30));
        this.restTemplate = new RestTemplate(factory);
    }

    @Override
    public String generateResponse(String prompt, List<ChatMessage> history) {
        if (prompt == null || prompt.isBlank()) {
            return "";
        }
        try {
            Map<String, Object> request = Map.of("model", model, "prompt", prompt, "stream", false);
            ResponseEntity<JsonNode> response = restTemplate.postForEntity(
                    ollamaUrl + "/api/generate", request, JsonNode.class);
            JsonNode body = response.getBody();
            if (body != null && body.hasNonNull("response")) {
                return body.get("response").asText().trim();
            }
        } catch (Exception e) {
            log.warn("No se pudo conectar con Ollama ({}). Usando respuesta simulada.", e.getMessage());
        }
        return simulatedResponse(prompt, history);
    }

    private String simulatedResponse(String enrichedPrompt, List<ChatMessage> history) {
        String question = currentQuestion(enrichedPrompt).trim();
        String lower = question.toLowerCase(Locale.ROOT).strip();
        List<ProductInfo> products = extractProducts(enrichedPrompt);

        // ===== DESPEDIDAS =====
        if (kw(lower, "chau", "adiós", "adios", "nos vemos", "hasta luego", "hasta pronto",
                "bye", "me voy", "me tengo que ir")) {
            return pick(
                    "¡Hasta luego! Fue un gusto atenderte. ¡Que tengas un excelente día! 😊",
                    "¡Nos vemos! Cualquier cosa que necesites, escribinos. ¡Éxitos! 😄",
                    "¡Chau! Que te vaya muy bien. ¡Volvé cuando quieras! 😊"
            );
        }

        // ===== AGRADECIMIENTOS =====
        if (kw(lower, "gracias", "muchas gracias", "te agradezco", "genial", "perfecto",
                "excelente", "buenísimo", "buenisimo", "copado", "dale gracias")) {
            return pick(
                    "¡De nada! Para eso estamos. 😊",
                    "¡Nada que agradecer! Estoy acá si necesitás algo más.",
                    "¡Con gusto! Si se te ocurre otra cosa, avisame.",
                    "¡Tranquilo! Para eso existo. 😄"
            );
        }

        // ===== SALUDOS =====
        if (kw(lower, "hola", "buenas", "buenos días", "buenas tardes", "buenas noches",
                "saludos", "qué tal", "que tal", "holis", "holi", "hey")) {
            if (history == null || history.isEmpty()) {
                return pick(
                        "¡Hola! Bienvenido/a. ¿En qué te puedo ayudar?",
                        "¡Hola! ¿Cómo andás? Contame en qué te puedo servir.",
                        "¡Buenas! Qué gusto que nos escribas. ¿Qué necesitás?",
                        "¡Hola! Soy tu asistente virtual. ¿En qué te puedo ayudar hoy?"
                );
            }
            return pick(
                    "¡Hola de nuevo! ¿En qué te puedo ayudar?",
                    "¡Hola! ¿Qué más necesitás?",
                    "¡Hey! Vos de nuevo. ¿Qué te traigo por acá esta vez?"
            );
        }

        // ===== PREGUNTAS GENERALES DE BIENESTAR =====
        if (kw(lower, "cómo estás", "como estas", "qué onda", "que onda", "todo bien",
                "cómo andás", "como andas", "cómo va", "como va")) {
            return pick(
                    "¡Todo bien, acá a tu servicio! ¿Y vos? ¿En qué te puedo ayudar?",
                    "¡Muy bien, gracias! Listo para lo que necesites. ¿Qué tal?",
                    "¡Acá funcionando al 100%! Contame, ¿qué necesitás?"
            );
        }

        // ===== QUIÉN SOS =====
        if (kw(lower, "quién sos", "quien sos", "qué sos", "que sos", "cómo te llamás",
                "como te llamas", "tu nombre", "tu marca")) {
            return pick(
                    "Soy el asistente virtual de la negocio. Estoy acá para ayudarte con lo que necesites: productos, precios, horarios, pedidos, y más. ¿Qué te gustaría saber?",
                    "¡Me llamo como quieras! 😄 Lo importante es que estoy acá para ayudarte. ¿Qué necesitás?",
                    "Soy tu asistente virtual. Puedo ayudarte con info de productos, precios, horarios y mucho más. ¿Por dónde empezamos?"
            );
        }

        // ===== RECOMENDACIONES =====
        if (kw(lower, "recomend", "sugerí", "sugeri", "que me conviene", "que me sirve",
                "ayudame a elegir", "no sé qué", "no se que", "opciones", "alternativas")) {
            if (!products.isEmpty()) return buildRecommendation(products);
            return pick(
                    "¡Con gusto te ayudo a elegir! Contame: ¿qué tipo de producto buscás? ¿Para qué lo necesitás? Así te puedo guiar mejor.",
                    "¡Dale! Para recomendarte algo copado, contame un poco: ¿qué estás buscando y para qué lo querés?",
                    "¡Buena idea! Contame qué tenés en mente y te ayudo a encontrar algo ideal."
            );
        }

        // ===== STOCK / DISPONIBILIDAD =====
        if (kw(lower, "stock", "disponible", "disponibilidad", "tenés", "tienes", "hay",
                "quedan", "queda", "consigo", "conseguir", "conseguís")) {
            if (!products.isEmpty()) return buildStockResponse(products);
            return pick(
                    "¿De qué producto querés saber la disponibilidad? Si me decís el nombre o código, lo verifico al toque.",
                    "¿Cuál es el producto que buscás? Así reviso si lo tenemos.",
                    "Contame qué producto necesitás y te digo si tenemos stock."
            );
        }

        // ===== SKU ESPECÍFICO =====
        Matcher skuM = Pattern.compile("\\b([A-Z]{2,4}-?\\d{3,6})\\b", Pattern.CASE_INSENSITIVE).matcher(question);
        if (skuM.find()) {
            String sku = skuM.group(1).toUpperCase();
            Optional<ProductInfo> match = products.stream().filter(p -> p.sku.equalsIgnoreCase(sku)).findFirst();
            if (match.isPresent()) {
                ProductInfo p = match.get();
                return String.format("Encontré el **%s** (código %s):\n\nPrecio: $%s\nStock: %s\n\n%s",
                        p.name, p.sku, p.price,
                        p.stock > 0 ? "✅ Disponible (" + p.stock + " unidades)" : "❌ Agotado",
                        p.description != null ? p.description + "\n\n" : "") +
                        "¿Te interesa? Puedo darte más info o ayudarte con la compra.";
            }
            return "No encontré un producto con el código " + sku + ". ¿Podrías verificar el código o decirme el nombre del producto?";
        }

        // ===== PRODUCTOS / CATÁLOGO =====
        if (kw(lower, "zapato", "zapatilla", "bota", "calzado", "billetera", "cinturón", "cinturon",
                "cartera", "mochila", "reloj", "anillo", "collar", "pulsera", "aros",
                "camisa", "remera", "pantalón", "pantalon", "pollera", "vestido", "campera",
                "producto", "catálogo", "catalogo", "qué tienen", "que tienen",
                "qué venden", "que venden", "qué hay", "que hay", "mostrame", "ver", "ver productos")) {
            if (!products.isEmpty()) return buildCatalog(products);
            return pick(
                    "Contamos con productos muy variados. ¿Hay algo específico que estés buscando? Puedo ayudarte a encontrar exactamente lo que necesitás.",
                    "Tenemos cosas muy copadas. Contame qué tipo de producto te interesa y te ayudo a elegir.",
                    "¡Buena pregunta! ¿Qué tipo de producto tenés en mente?"
            );
        }

        // ===== PRECIOS =====
        if (kw(lower, "precio", "cuánto", "cuanto", "cuesta", "costo", "valor", "barato",
                "caro", "económico", "economico", "descuento", "oferta", "promo", "promociones")) {
            if (!products.isEmpty()) return buildPriceResponse(products, lower);
            return pick(
                    "¿De qué producto querés saber el precio? Así te doy la info exacta.",
                    "Nuestros precios son muy competitivos. ¿Qué producto te interesa?",
                    "Contame qué estás buscando y te digo el precio."
            );
        }

        // ===== PAGO Y ENVÍO =====
        if (kw(lower, "pago", "pagar", "transferencia", "tarjeta", "efectivo", "cuotas",
                "envío", "envio", "entrega", "delivery", "llevar", "mandar", "despacho",
                "cuándo llega", "cuando llega", "plazo", "tardanza")) {
            return pick(
                    "Respecto a la entrega y el pago, trabajamos con varias opciones:\n\n" +
                    "💳 Tarjeta de crédito hasta en 12 cuotas\n" +
                    "💵 Efectivo con descuento\n" +
                    "🏦 Transferencia bancaria\n" +
                    "🚚 Envío a domicilio (consultá por zona)\n\n" +
                    "¿Qué opción te interesa? Así te doy más detalles.",

                    "Tenemos varias formas de pago:\n\n" +
                    "• Tarjeta de crédito (hasta 12 cuotas)\n" +
                    "• Débito\n" +
                    "• Efectivo\n" +
                    "• Transferencia\n\n" +
                    "Y el envío lo coordinamos según tu zona. ¿Qué necesitás saber?"
            );
        }

        // ===== UBICACIÓN =====
        if (kw(lower, "dónde", "donde", "ubicación", "ubicacion", "dirección", "direccion",
                "están", "estan", "sucursal", "local", "visitar", "ir", "llegar",
                "cómo llego", "como llego", "mapa")) {
            return "📍 Nos podés encontrar en:\n\n" +
                    "Av. Principal 1234, Local 5\n" +
                    "🕐 Lunes a sábado de 9:00 a 20:00 hs\n" +
                    "🅿️ Estacionamiento gratuito\n\n" +
                    "¿Querés que te pase la ubicación por WhatsApp para que la tengas en el mapa?";
        }

        // ===== HORARIOS =====
        if (kw(lower, "horario", "abierto", "hora", "abren", "cierran", "atención", "atencion",
                "cuándo", "cuando", "días", "dias", "domingo", "feriado", "feriados")) {
            return "📅 Nuestro horario de atención es:\n\n" +
                    "• Lunes a viernes: 9:00 a 20:00 hs\n" +
                    "• Sábados: 9:00 a 18:00 hs\n" +
                    "• Domingos y feriados: Cerrado\n\n" +
                    "¿Hay algo más que quieras saber?";
        }

        // ===== PEDIDOS =====
        if (kw(lower, "pedido", "orden", "compra", "enviado", "llegó", "llego",
                "seguimiento", "tracking", "dónde está mi", "donde esta mi")) {
            return "Para consultar el estado de tu pedido necesito algunos datos:\n\n" +
                    "1️⃣ Nombre con el que hiciste la compra\n" +
                    "2️⃣ Número de pedido (si lo tenés)\n" +
                    "3️⃣ Fecha de la compra\n\n" +
                    "Con eso puedo buscar los detalles y decirte dónde está tu pedido.";
        }

        // ===== QUEJA / PROBLEMA =====
        if (kw(lower, "problema", "reclamo", "queja", "roto", "defectuoso",
                "no me gusta", "no funciona", "error", "equivocado", "malísimo", "malisimo",
                "terrible", "pésimo", "pesimo", "furioso", "molesto", "enojado", "indignado",
                "una estafa", "estafa", "no sirve")) {
            return "Lamento mucho que hayas tenido esa experiencia. Tu satisfacción es nuestra prioridad.\n\n" +
                    "Contame qué pasó con detalle y voy a hacer todo lo posible para resolverlo.\n\n" +
                    "¿Qué es lo que sucedió?";
        }

        // ===== PEDIR ASESOR / HUMANO =====
        if (kw(lower, "hablar con", "asesor", "representante", "humano", "operador",
                "empleado", "alguien", "supervisor", "gerente", "dueño", "persona humana")) {
            return "¡Claro! Antes de conectarte, ¿podrías contarme brevemente tu consulta? Así el asesor ya llega con contexto y te atiende mucho más rápido. No querés repetir todo de cero, ¿no? 😊\n\n¿Qué necesitás?";
        }

        // ===== SÍ / AFIERMATIVO =====
        if (kw(lower, "sí", "si", "dale", "ok", "bueno", "claro", "obvio", "por favor",
                "porfa", "dale", "buenísimo", "buenisimo", "compralo", "lo llevo", "me llevo",
                "me interesa", "quiero", "tomalo", "sí por favor")) {
            return pick(
                    "¡Perfecto! ¿En qué más te puedo ayudar?",
                    "¡Genial! ¿Qué otra cosa necesitás?",
                    "¡Dale! Seguimos con algo más?"
            );
        }

        // ===== NO / NEGATIVO =====
        if (kw(lower, "no", "nah", "nope", "para nada", "nada", "no gracias", "no问题",
                "después", "despues", "luego", "otro día", "otro dia")) {
            return pick(
                    "¡Sin problema! Si cambiás de opinión, estoy acá. 😊",
                    "¡Tranquilo! Cuando necesites algo, escribinos.",
                    "¡Dale! Estoy acá cuando me necesites."
            );
        }

        // ===== SEGUIR / PROFUNDIZAR =====
        if (kw(lower, "y?", "y más", "y mas", "cuéntame", "cuentame", "explícame",
                "explicame", "más info", "mas info", "detalles", "contame",
                "cómo funciona", "como funciona", "contame más", "cuentame mas")) {
            if (!products.isEmpty()) {
                ProductInfo p = products.get(0);
                return "Sobre " + p.name + ": " +
                        (p.description != null ? p.description + ". " : "") +
                        "Precio: $" + p.price + ". " +
                        (p.stock > 0 ? "Tenemos stock disponible." : "Está agotado por el momento.") +
                        "\n\n¿Qué te gustaría saber específicamente?";
            }
            return pick(
                    "¡Claro! Contame sobre qué tema querés que profundice.",
                    "¡Dale! Sobre qué querés que te cuente más.",
                    "¡Por supuesto! ¿Qué te gustaría saber?"
            );
        }

        // ===== COMPARAR =====
        if (kw(lower, "comparar", "diferencia", "diferente", "mejor", "peor",
                "cuál es mejor", "cual es mejor", "vs", "versus", "comparación")) {
            if (products.size() >= 2) {
                ProductInfo a = products.get(0);
                ProductInfo b = products.get(1);
                return "Comparando nuestros productos:\n\n" +
                        "**" + a.name + "** - $" + a.price + "\n" +
                        (a.description != null ? a.description : "") + "\n\n" +
                        "**" + b.name + "** - $" + b.price + "\n" +
                        (b.description != null ? b.description : "") + "\n\n" +
                        "¿Cuál te interesa más? Puedo darte más detalles de cualquiera.";
            }
            return "¿Qué productos te gustaría comparar? Así te doy las diferencias.";
        }

        // ===== GUARDAR / RESERVAR =====
        if (kw(lower, "guardar", "reservar", "reserva", "guardame", "reservame",
                "lo quiero", "lo llevo", "me lo quedo", "comprar", "compra")) {
            return "¡Excelente elección! Para proceder con la compra/reserva, necesito que me confirmes:\n\n" +
                    "1️⃣ ¿Qué producto querés?\n" +
                    "2️⃣ ¿Cantidad?\n" +
                    "3️⃣ Nombre completo\n" +
                    "4️⃣ Teléfono de contacto\n\n" +
                    "Con eso te armo el pedido al toque.";
        }

        // ===== DEFAULT INTELIGENTE =====
        return smartFallback(question, lower, products, history);
    }

    private String smartFallback(String question, String lower, List<ProductInfo> products, List<ChatMessage> history) {
        // Si hay contexto de productos, usarlo
        if (!products.isEmpty()) {
            ProductInfo p = products.get(0);
            return "Buena pregunta. De nuestros productos, el **" + p.name + "** es una excelente opción. " +
                    (p.description != null ? p.description + ". " : "") +
                    "Precio: $" + p.price + ". " +
                    (p.stock > 0 ? "Tenemos stock." : "Está agotado por ahora.") +
                    "\n\n¿Te interesa? Puedo darte más info o mostrarte otros productos.";
        }

        // Si hay historial, referenciarlo
        if (history != null && !history.isEmpty()) {
            String lastTopic = history.get(history.size() - 1).getPrompt();
            return "Entiendo. Vi que antes preguntaste sobre \"" + lastTopic + "\". " +
                    "¿Querés que sigamos con eso o hay algo nuevo en lo que te pueda ayudar?";
        }

        // Respuesta genérica amigable
        return pick(
                "Interesante consulta. Aunque no tengo esa info específica, puedo ayudarte con:\n\n" +
                "🔍 Productos y precios\n" +
                "📦 Pedidos y envíos\n" +
                "💳 Formas de pago\n" +
                "📍 Ubicación y horarios\n\n" +
                "¿Alguna de esas te sirve?",

                "Buena pregunta. Contame un poco más sobre qué necesitás y te ayudo a encontrar lo que buscás.",

                "Me interesa tu consulta. ¿Podrías contarme un poco más? Así te puedo ayudar mejor."
        );
    }

    // ===== HELPERS =====

    private String buildRecommendation(List<ProductInfo> products) {
        StringBuilder sb = new StringBuilder();
        sb.append("¡Con gusto te recomiendo! Estos son nuestros productos destacados:\n\n");
        int limit = Math.min(5, products.size());
        for (int i = 0; i < limit; i++) {
            ProductInfo p = products.get(i);
            sb.append("• **").append(p.name).append("**");
            sb.append(" - $").append(p.price);
            sb.append(p.stock > 0 ? " ✅" : " ❌");
            sb.append("\n");
        }
        sb.append("\n¿Alguno te llama la atención? Contame para qué lo necesitás y te ayudo a elegir.");
        return sb.toString();
    }

    private String buildCatalog(List<ProductInfo> products) {
        StringBuilder sb = new StringBuilder();
        sb.append("Nuestro catálogo:\n\n");
        int limit = Math.min(8, products.size());
        for (int i = 0; i < limit; i++) {
            ProductInfo p = products.get(i);
            sb.append("🔹 ").append(p.name).append(" (").append(p.sku).append(")\n");
            sb.append("   $").append(p.price);
            if (p.stock > 0) sb.append(" | ").append(p.stock).append(" disponibles");
            else sb.append(" | Agotado");
            sb.append("\n");
        }
        if (products.size() > 8) sb.append("\n... y más productos.\n");
        sb.append("\n¿Cuál te interesa? Puedo darte todos los detalles.");
        return sb.toString();
    }

    private String buildPriceResponse(List<ProductInfo> products, String lower) {
        for (ProductInfo p : products) {
            String firstName = p.name.toLowerCase(Locale.ROOT).split(" ")[0];
            if (lower.contains(firstName)) {
                return String.format("**%s**: $%s\n\n%s\n\n¿Te interesa? Puedo ayudarte con la compra.",
                        p.name, p.price,
                        p.stock > 0 ? "✅ Tenemos stock disponible." : "❌ Agotado por el momento.");
            }
        }
        ProductInfo cheapest = products.stream().min(Comparator.comparing(ProductInfo::getPrice)).orElse(null);
        ProductInfo priciest = products.stream().max(Comparator.comparing(ProductInfo::getPrice)).orElse(null);
        if (cheapest != null && priciest != null) {
            return "Nuestros precios van desde **$" + cheapest.price + "** hasta **$" + priciest.price + "**.\n\n" +
                    "¿Qué tipo de producto te interesa? Así te doy el precio exacto.";
        }
        return "¿De qué producto querés saber el precio? Así te doy la info.";
    }

    private String buildStockResponse(List<ProductInfo> products) {
        StringBuilder sb = new StringBuilder();
        sb.append("Estado de stock:\n\n");
        for (ProductInfo p : products) {
            sb.append("• ").append(p.name).append(": ");
            sb.append(p.stock > 0 ? "✅ " + p.stock + " unidades" : "❌ Agotado");
            sb.append("\n");
        }
        sb.append("\n¿Querés que te reserve alguno?");
        return sb.toString();
    }

    private List<ProductInfo> extractProducts(String enrichedPrompt) {
        List<ProductInfo> products = new ArrayList<>();
        Matcher m = PRODUCT_PATTERN.matcher(enrichedPrompt);
        if (m.find()) {
            String block = m.group(1);
            for (String line : block.split("\n")) {
                line = line.trim();
                if (line.startsWith("- ")) {
                    String name = "", sku = "", price = "", desc = "";
                    Matcher nameM = Pattern.compile("^- (.+?) \\(SKU: (.+?)\\)").matcher(line);
                    if (nameM.find()) {
                        name = nameM.group(1);
                        sku = nameM.group(2);
                    }
                    Matcher priceM = Pattern.compile("Precio: \\$(.+)").matcher(line);
                    if (priceM.find()) price = priceM.group(1).trim();
                    String stockStr = "";
                    Matcher stockM = Pattern.compile("Stock: (.+?)(?:\\||$)").matcher(line);
                    if (stockM.find()) stockStr = stockM.group(1).trim();
                    int stock = stockStr.contains("Disponible") ? 5 : 0;
                    String[] parts = line.split("\\|");
                    if (parts.length > 3) desc = parts[parts.length - 1].trim();
                    products.add(new ProductInfo(name, sku, price, stock, desc));
                }
            }
        }
        return products;
    }

    private String currentQuestion(String enrichedPrompt) {
        int idx = enrichedPrompt.lastIndexOf("Usuario: ");
        if (idx >= 0) return enrichedPrompt.substring(idx + "Usuario: ".length());
        return enrichedPrompt;
    }

    private boolean kw(String text, String... keywords) {
        for (String k : keywords) if (text.contains(k)) return true;
        return false;
    }

    private String pick(String... options) {
        return options[random.nextInt(options.length)];
    }

    private record ProductInfo(String name, String sku, String price, int stock, String description) {
        public BigDecimal getPrice() {
            try { return new BigDecimal(price.replace(",", ".")); }
            catch (Exception e) { return BigDecimal.ZERO; }
        }
    }
}
