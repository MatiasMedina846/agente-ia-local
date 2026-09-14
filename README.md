Enterprise WhatsApp AI Bot & Multi-Tenant Engine
Un motor de atención conversacional omnicanal, robusto y listo para producción, diseñado específicamente para comercios locales. Resuelve los problemas típicos de los chatbots genéricos mediante una arquitectura orientada a la resiliencia, RAG híbrido y aislamiento multi-tenant.

CARACTERISTICAS PRINCIPALES (Core Features)
Resiliencia contra el Usuario Caótico:

Procesamiento de Audios: Transcripción inteligente con limpieza automática de ruido y muletillas (vía Whisper API).

Buffer de Mensajes (Debounce): Agrupación temporal de mensajes múltiples para evitar respuestas fragmentadas o spam en el chat.

Visión por Computadora: Extracción automática de datos desde comprobantes de pago o imágenes de reclamos.

Gobernanza de Datos y Cero Alucinaciones:

RAG Híbrido: Búsqueda combinada por similitud semántica (Embeddings) e índices exactos por SKU/Código de producto.

Capa de Validación Determinista (Guardrails): Middleware programático que intercepta y bloquea salidas del LLM que contradigan los datos reales de inventario o precios.

Arquitectura Enterprise & Multi-Tenant:

Aislamiento estricto de bases de datos, prompts y credenciales por cada cliente comercial (tenant_id).

Panel de configuración White-Label con actualización en caliente (hot-reload).

Analítica y Automatización de Negocio:

Detección automática de fricciones y puntos de abandono conversacional (Drop-offs).

Reportes ejecutivos semanales automatizados directo al WhatsApp del dueño.

Handoff inteligente a operadores humanos con inyección de resumen de chat previo.

STACK TECNOLOGICO

Backend: Java, Spring Boot, Maven

IA & Procesamiento: OpenAI API (GPT / Whisper), Embeddings Vectoriales

Mensajería: Meta WhatsApp Business Cloud API (Webhooks)

Base de Datos: [Ej: PostgreSQL / H2 para desarrollo]

ARQUITECTURA DEL PROYECTO
Plaintext
src/
├── main/
│   ├── java/com/tuempresa/chatbot/
│   │   ├── controller/      # Webhooks de WhatsApp y Endpoints de API
│   │   ├── service/         # Lógica de negocio (Buffer, RAG, Transcripción)
│   │   ├── repository/      # Conexión multi-tenant a base de datos
│   │   └── guardrails/      # Filtros de seguridad post-LLM
│   └── resources/
│       └── application.yml  # Configuración y perfiles
CONFIGURACION E INSTALACION LOCAL
Clonar el repositorio:

Bash
git clone https://github.com/tu-usuario/tu-repositorio.git
cd tu-repositorio
Configurar las variables de entorno:
Crea un archivo .env o configura tu application.yml con las credenciales necesarias:

Properties
WHATSAPP_TOKEN=tu_token_de_meta
WHATSAPP_VERIFY_TOKEN=tu_token_secreto
OPENAI_API_KEY=tu_api_key_de_openai
Compilar y ejecutar con Maven:

Bash
mvn clean install
mvn spring-boot:run
Exponer el entorno local (para Webhooks):
Utiliza una herramienta como Ngrok para apuntar las peticiones de Meta a tu puerto local:

Bash
ngrok http 8080
TESTING
El proyecto incluye pruebas unitarias e de integración para asegurar la estabilidad del pipeline:

Bash
mvn test
AUTOR 
Matias German Medina - Full Stack Developer - Tu LinkedIn
