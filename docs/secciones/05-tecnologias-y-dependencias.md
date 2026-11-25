[⬅ Volver al README Principal](../../README.md)

---

## 5. TECNOLOGÍAS Y DEPENDENCIAS

| Componente | Tecnología | Versión | Uso |
| :--- | :--- | :--- | :--- |
| Lenguaje | Java LTS | `21` | Estándar del curso. |
| Framework | Spring Boot | `3.2.12` | Web, Data JPA, AMQP, Validation y Actuator. |
| Base de datos | PostgreSQL | `16` | Todos los perfiles (dev con Docker Compose); **sin H2**. |
| Migraciones | Flyway | `10.20.1` | V1..V7 con `ddl-auto=validate`. |
| Mensajería | RabbitMQ (Spring AMQP) | `3.13` | Eventos de fundaciones. |
| Seguridad | JJWT | `0.12.6` | Validación de los JWT de ms-auth (misma librería y versión). |
| Almacenamiento | Azure Blob Storage SDK (`azure-storage-blob`) | `12.29.1` | Subida real de RUT, fotos y certificados (CADCHE-117); Azurite en `docker` y en `AzureBlobStorageAdapterIntegrationTest`. |
| Documentación | springdoc-openapi | `2.3.0` | Swagger UI y OpenAPI 3. |
| Pruebas | JUnit 5, Mockito, Testcontainers | `5.10 / 5.7 / 1.19.8` | Unitarias, WebMvc e integración con **PostgreSQL 16 real**. |
| Calidad | JaCoCo, SpotBugs, SonarCloud | `0.8.12 / 4.8.6 / –` | Cobertura ≥ 85 % de líneas y ≥ 80 % de ramas, y análisis estático. |
| Contenedor | Eclipse Temurin JRE Alpine | `21` | Imagen OCI no-root con el JAR validado en CI. |

### Análisis De Tecnologías Del Servicio (Plantilla Maestra Oficial)

Este apartado constituye la transcripción y aplicación integral de la **Plantilla Maestra Oficial de Análisis de Tecnologías** de StyleRadar para el microservicio `ms-tiendas` (`CADCHE-Stores-Service`), desarrollada conforme al estándar de entrega de arquitectura de software de StyleRadar 2026.

#### Ficha Técnica del Documento

* **Proyecto:** StyleRadar · 2026
* **Microservicio:** `CADCHE-Stores-Service` (`ms-tiendas`, Puerto `8140`)
* **Paquete Arquitectónico:** Paquete 1: Identidad
* **Alcance Arquitectónico:** Evaluación multicriterio de 13 Microservicios, API Gateway y Arquitectura Orientada a Eventos
* **Equipo Responsable (CADche):** Juan Diego Valderrama (Líder de Equipo), Julian Camilo Giral (`@jcobos548`), Juan Diego Gaitan (`@mamagege`)
* **Versión y Fecha:** v1.0 · Noviembre 2026

---

#### 00. Resumen Ejecutivo y Estructura del Documento

El presente documento define el marco formal para evaluar, justificar y documentar la selección del stack tecnológico del microservicio `ms-tiendas`, garantizando alineación entre los atributos de calidad definidos en la norma ISO/IEC 25010 y las restricciones presupuestales y operativas del proyecto StyleRadar.

* **Servicios del Ecosistema:** 13 Microservicios + 1 API Gateway Perimetral
* **Capas Evaluadas:** 8 capas tecnológicas de extremo a extremo
* **Estándar de Empaquetado:** 100% Contenedores compatibles con OCI (Docker)
* **Latencia Meta del Sistema:** Percentil P95 inferior a 200 ms en consultas operativas

---

#### 01. Contexto y Drivers Arquitectónicos (ISO/IEC 25010)

##### 1.1 Declaración del Problema y Justificación de Microservicios

El sistema StyleRadar requiere desacoplar el procesamiento intensivo de inteligencia artificial (inferencia del probador virtual) del catálogo de alta concurrencia de lectura y de la administración de comercios aliados. Específicamente, `ms-tiendas` debe aislar el registro comercial por NIT, la verificación documental del RUT en formato PDF, las coordenadas geográficas de sedes físicas, la galería de vitrinas y la gestión de fundaciones aliadas. Si la recepción y validación de documentos e imágenes se ejecutara en el microservicio de catálogo o en un monolito, la carga de archivos binarios de hasta 6 MB degradaría los tiempos de respuesta del catálogo público durante las horas pico de compra. Por tanto, se adopta una arquitectura de microservicios con límites de contexto basados en Domain-Driven Design (DDD), donde `ms-tiendas` gestiona su propia persistencia y delega las rutas de archivos binarios al puerto de almacenamiento de objetos.

##### 1.2 Atributos de Calidad Clave (ISO/IEC 25010)

| Atributo de Calidad | Métrica / Escenario Objetivo | Impacto en la Selección Tecnológica |
| :--- | :--- | :--- |
| **Rendimiento y Latencia** | P95 < 200 ms en consultas operativas; P99 < 500 ms | Exige índices relacionales en `almacenes` (`idx_almacenes_nombre`, `idx_almacenes_estado_verif`) y `almacen_fotos` (`idx_almacen_fotos_nit`). |
| **Escalabilidad Horizontal** | Soporte de 0 a 1,000 req/s en eventos de ferias de moda | Microservicios stateless en contenedores OCI; escalado elástico independiente por réplicas. |
| **Consistencia de Datos** | ACID estricto en NIT, administradores y fotos; eventual en eventos | PostgreSQL para transaccionalidad relacional estricta; RabbitMQ para propagación de eventos asíncronos. |
| **Disponibilidad y Resiliencia** | SLA 99.9%; tolerancia al fallo de servicios downstream | Circuit Breakers (Resilience4j), degradación elegante si el catálogo no responde al aprobar una tienda. |
| **Seguridad y Privacidad** | Control estricto BOLA/IDOR; validación de firma mágica PDF | Autorización RBAC granular para `STORE_ADMIN` sobre su propio NIT; validación de bytes `%PDF`. |
| **Eficiencia de Costos (FinOps)** | Costo mensual < $35 USD en fase MVP / académica | Almacenamiento de archivos binarios en Blob Storage Hot LRS de bajo costo sin saturar la base relacional. |
| **Mantenibilidad & Equipo** | Time-to-market de 8 semanas para el core MVP | Uso de tecnologías ampliamente dominadas por el equipo (Java 21 / Spring Boot) para mitigar riesgo de entrega. |

##### 1.3 Restricciones Técnicas y Organizacionales

El sistema debe empaquetarse en imágenes Docker compatibles con estándares OCI; las pruebas unitarias e integración deben alcanzar una cobertura mínima de JaCoCo igual o superior al 85% y superar el Quality Gate estricto de SonarQube con cero vulnerabilidades y cero code smells graves. La evaluación de tecnologías en esta plantilla sigue el método ATAM (Architecture Tradeoff Analysis Method), priorizando los drivers que generan mayor impacto en los riesgos del negocio y la viabilidad técnica del sistema.

---

#### 02. Metodología de Evaluación Multicriterio (MCDM / Matriz de Pugh)

Para evitar decisiones basadas en preferencias subjetivas, se utiliza una matriz de decisión cuantitativa multicriterio (MCDM / Matriz de Pugh), asignando ponderaciones porcentuales basadas en los drivers arquitectónicos de StyleRadar.

##### 2.1 Criterios de Selección y Ponderaciones

| Criterio | Peso (%) | Definición y Justificación | Métrica Objetiva de Medición |
| :---: | :---: | :--- | :--- |
| **C1: Rendimiento** | 25 % | Capacidad de atender alto throughput con latencia mínima P95 y bajo consumo de memoria RAM y CPU. | Benchmarks oficiales, tiempo de respuesta bajo carga (JMeter/k6), consumo de memoria por contenedor. |
| **C2: Madurez & LTS** | 20 % | Estabilidad de APIs, soporte extendido (LTS), documentación oficial y tamaño del ecosistema. | Años en el mercado, frecuencia de parches CVE, comunidad activa en GitHub y respaldo de fundaciones. |
| **C3: Velocidad Dev** | 20 % | Curva de aprendizaje del equipo, disponibilidad de librerías listas para producción y velocidad de prototipado. | Horas requeridas para implementar endpoints multipart con validación de dominio y persistencia relacional. |
| **C4: Operabilidad** | 15 % | Facilidad de contenedorización con Docker, compatibilidad CI/CD y observabilidad con métricas y logs. | Tamaño de la imagen Docker final, soporte nativo de métricas de salud (Actuator / Healthchecks). |
| **C5: Costo & TCO** | 20 % | Costo total de propiedad en nubes públicas, tipo de licenciamiento open-source y ausencia de vendor lock-in. | Licencia OSI (Apache 2.0 / MIT / PostgreSQL), costo de instancias administradas en Azure Calculator. |

##### 2.2 Escala de Calificación Objetiva (Rúbrica 1 a 5)

* **1 - 2 (Deficiente):** No cumple el driver. Curva prohibitiva, consumo desmedido de RAM o licenciamiento restrictivo.
* **3 (Aceptable):** Cumple parcialmente con trade-offs significativos que requieren mitigación técnica adicional.
* **4 - 5 (Sobresaliente):** Excelente desempeño, soporte LTS probado en la industria y alta productividad para el equipo.

##### 2.3 Estándar de Architectural Decision Records (ADR)

Cada decisión se documenta bajo el formato formal de ADR (Michael Nygard), sintetizando el contexto, las opciones evaluadas, la decisión final justificada y las consecuencias derivadas. La puntuación total ponderada se calcula mediante la fórmula oficial:

$$\text{Puntaje Total} = \sum_{i=1}^{5} (\text{Calificación}_{i} \times \text{Peso}_{i})$$

---

#### 03. Matrices de Decisión Técnicas Detalladas (ADRs de Capa)

##### 3.1 Lenguaje & Runtime de Ejecución Backend (ADR-001)

| Criterio (Peso) | Java 21 LTS (OpenJDK) | Node.js (TypeScript) | Go (Golang 1.22) | Python 3.12 (FastAPI) |
| :--- | :---: | :---: | :---: | :---: |
| C1: Rendimiento (25 %) | 5 | 4 | 5 | 3 |
| C2: Madurez & LTS (20 %) | 5 | 4 | 4 | 4 |
| C3: Velocidad Dev (20 %) | 4 | 4 | 3 | 5 |
| C4: Operabilidad (15 %) | 5 | 4 | 5 | 3 |
| C5: Costo & TCO (20 %) | 4 | 4 | 4 | 3 |
| **Total Ponderado** | **4.65 / 5.00 (Seleccionado)** | **3.95 / 5.00** | **4.30 / 5.00** | **3.55 / 5.00** |

* **Decisión ADR-001:** Se adopta Java 21 LTS como lenguaje estándar transversal para todos los microservicios backend de StyleRadar.
* **Por qué Java 21:** Precisión aritmética en cálculos trigonométricos de coma flotante de doble precisión para la fórmula de Haversine; Records inmutables para representar datos de comercios y DTOs de auditoría; excelente soporte de hilos virtuales para operaciones de I/O de archivos.
* **Por qué no los otros:** Node.js presenta menor precisión nativa y bloqueos en flujos de validación de streams binarios; Python tiene alto consumo de memoria y lentitud en operaciones matemáticas concurrentes debido al GIL.

##### 3.2 Framework Backend & Microservicios (ADR-002)

| Criterio (Peso) | Spring Boot 3.2.x | Quarkus 3.x | Micronaut 4.x | NestJS (Node/TS) |
| :--- | :---: | :---: | :---: | :---: |
| C1: Rendimiento (25 %) | 5 | 5 | 4 | 4 |
| C2: Madurez & LTS (20 %) | 5 | 4 | 3 | 4 |
| C3: Velocidad Dev (20 %) | 5 | 3 | 3 | 4 |
| C4: Operabilidad (15 %) | 5 | 5 | 4 | 4 |
| C5: Costo & TCO (20 %) | 4 | 5 | 5 | 4 |
| **Total Ponderado** | **4.80 / 5.00 (Seleccionado)** | **4.40 / 5.00** | **3.80 / 5.00** | **3.95 / 5.00** |

* **Decisión ADR-002:** Se adopta Spring Boot 3.2.x (`3.2.12`) como framework base para la construcción del microservicio `ms-tiendas`.
* **Por qué Spring Boot:** Soporte integrado para recepción de solicitudes `multipart/form-data` con límites estrictos configurables en `application.yml` (6 MB por archivo y 7 MB por petición); integración nativa con Spring Data JPA, Actuator y OpenAPI 3 (`springdoc 2.3.0`).
* **Por qué no los otros:** Quarkus y Micronaut requieren configuraciones adicionales complejas para el manejo de partes binarias multipart en contenedores nativos; NestJS introduce una pila tecnológica heterogénea frente al resto de microservicios Java.

##### 3.3 Persistencia Relacional Georreferenciada y Almacenamiento de Archivos (ADR-003)

| Criterio (Peso) | PostgreSQL 16 + Blob Storage | MySQL 8.0 | MongoDB 7.0 (NoSQL) | Amazon Aurora / Cloud DB |
| :--- | :---: | :---: | :---: | :---: |
| C1: Rendimiento (25 %) | 5 | 4 | 4 | 5 |
| C2: Madurez & LTS (20 %) | 5 | 5 | 4 | 5 |
| C3: Velocidad Dev (20 %) | 5 | 4 | 4 | 4 |
| C4: Operabilidad (15 %) | 5 | 4 | 4 | 5 |
| C5: Costo & TCO (20 %) | 5 | 4 | 3 | 2 |
| **Total Ponderado** | **4.95 / 5.00 (Seleccionado)** | **4.15 / 5.00** | **3.75 / 5.00** | **4.10 / 5.00** |

* **Decisión ADR-003:** Se adopta PostgreSQL 16 para los datos relacionales de tiendas junto con `AzureBlobStorageAdapter` (Azurite en local / Blob HTTPS en cloud) para desacoplar archivos binarios (PDFs de RUT y fotografías).
* **Por qué PostgreSQL + Blob Storage:** PostgreSQL garantiza integridad transaccional estricta sobre la clave primaria natural `nit`, unicidad en la asignación de administradores (`UNIQUE (almacen_nit, usuario_id)`) y control relacional en `almacen_fotos` (`ON DELETE CASCADE`) para la cuota de hasta 6 fotografías por sede (`Almacen.MAXIMO_FOTOGRAFIAS = 6`). Guardar únicamente URLs HTTPS en SQL mantiene las tablas ligeras y rápidas.
* **Por qué no los otros:** Almacenar binarios como `BYTEA` dentro de PostgreSQL o MySQL degradaría severamente la memoria del buffer pool; MongoDB no garantiza integridad referencial entre almacenes, administradores y fotografías.

##### 3.4 Contratos OpenAPI 3 & Seguridad Stateless sin Estado Compartido (ADR-004)

| Criterio (Peso) | JWT Stateless HS256 + PostgreSQL 16 | Sesiones Stateful / Redis en Stores | Sticky Sessions HTTP | JWT Opaco con Introspección Síncrona |
| :--- | :---: | :---: | :---: | :---: |
| C1: Rendimiento (25 %) | 5 | 4 | 3 | 3 |
| C2: Madurez & LTS (20 %) | 5 | 4 | 3 | 4 |
| C3: Velocidad Dev (20 %) | 5 | 4 | 4 | 3 |
| C4: Operabilidad (15 %) | 5 | 3 | 2 | 3 |
| C5: Costo & TCO (20 %) | 5 | 3 | 4 | 3 |
| **Total Ponderado** | **5.00 / 5.00 (Seleccionado)** | **3.65 / 5.00** | **3.25 / 5.00** | **3.20 / 5.00** |

* **Decisión ADR-004:** `ms-tiendas` valida autorización de tokens JWT localmente sin estado (`JwtAuthFilter` + `JwtSecurityValidator` HS256) y valida propiedad BOLA directamente contra `PostgreSQL 16` (`administradores_almacenes`), **sin requerir instancia Redis en `ms-tiendas`**.
* **Por qué validación stateless directa:** Reduce complejidad operativa y costos de infraestructura en el MVP manteniendo tiempos de respuesta predecibles y cero dependencias externas de caché en tiempo de arranque.
* **Por qué no Redis hoy en Stores:** Evita mantener una infraestructura de caché adicional cuando las consultas relacionales indexadas (`idx_almacenes_estado_verif`, `UNIQUE (almacen_nit, usuario_id)`) satisfacen la carga actual sin sobrecosto FinOps.

##### 3.5 Mensajería Asíncrona & Event-Driven Architecture (ADR-005)

| Criterio (Peso) | Spring AMQP / RabbitMQ | Apache Kafka 3.6 | AWS SQS / SNS | Polling HTTP Síncrono |
| :--- | :---: | :---: | :---: | :---: |
| C1: Rendimiento (25 %) | 5 | 5 | 4 | 3 |
| C2: Madurez & LTS (20 %) | 5 | 5 | 5 | 4 |
| C3: Velocidad Dev (20 %) | 5 | 3 | 4 | 5 |
| C4: Operabilidad (15 %) | 5 | 2 | 5 | 4 |
| C5: Costo & TCO (20 %) | 5 | 2 | 3 | 4 |
| **Total Ponderado** | **4.95 / 5.00 (Seleccionado)** | **3.40 / 5.00** | **4.15 / 5.00** | **3.95 / 5.00** |

* **Decisión ADR-005:** Se adopta Spring AMQP (`RabbitMQFundacionEventPublisherAdapter`) orientado a publicar `FundacionDesactivadaEvent` hacia `styleradar.exchange` (routing keys `foundation.deactivated` y `foundation.reactivated`) con captura resiliente de `AmqpException`. En QA, PROD y DEV la comprobación AMQP está deshabilitada (`management.health.rabbit.enabled=false`) hasta activar la integración de eventos con donaciones.
* **Por qué Spring AMQP:** Desacopla el puerto `FundacionEventPublisherPort` sin bloquear confirmaciones transaccionales en PostgreSQL cuando el broker no está conectado.
* **Por qué no los otros:** Kafka requiere clústeres pesados con KRaft/Zookeeper que superan el presupuesto MVP; el polling síncrono acopla disponibilidad entre microservicios.

---

#### 04. Análisis de Costos en la Nube (TCO Azure & FinOps)

Para garantizar la viabilidad financiera del proyecto se proyecta el Costo Total de Propiedad (TCO) comparando el escenario de desarrollo/MVP contra el escenario de producción comercial escalada en Microsoft Azure.

##### 4.1 Topología Cloud y Supuestos de Dimensionamiento

Los microservicios se ejecutan en Azure Container Apps (serverless sobre Kubernetes gestionado). La base de datos corre en Azure Database for PostgreSQL Flexible Server. El almacenamiento estático se hospeda en Azure Blob Storage (Hot LRS) y el broker de mensajería en CloudAMQP.

##### 4.2 Desglose Mensual Estimado de Costos (Azure Cloud Provider)

| Capa de Servicio | SKU / Tier Fase MVP | SKU / Tier Fase Producción | Costo MVP ($) | Costo Prod ($) |
| :--- | :--- | :--- | :---: | :---: |
| **Cómputo (`ms-tiendas` App Service)** | Azure App Service Linux (Java 21 LTS) | App Service Standard S1 | $0.00 (Tier Estudiante) | $58.00 / mes |
| **Base de Datos Relacional** | PostgreSQL 16 Flexible Server B1ms (1 vCPU, 2GB) | PostgreSQL Flexible Server B2s (2 vCPU, 4GB, HA) | $15.50 / mes | $42.00 / mes |
| **Caché Distribuido** | No aplica en `ms-tiendas` hoy (Stateless JWT) | Opcional futuro (Azure Cache for Redis Standard) | $0.00 | $38.00 / mes (Opcional) |
| **Broker de Mensajería** | Deshabilitado en QA/PROD hoy (`management.health.rabbit.enabled=false`) | Broker compartido AMQP TLS / CloudAMQP | $0.00 | $19.00 / mes |
| **Storage & Blobs** | Azurite Local / Azure Blob Storage Hot (10 GB) | Azure Blob Storage (100 GB) + Azure Front Door | $1.50 / mes | $12.00 / mes |
| **Logs y Monitoreo** | Log Analytics / App Insights (5 GB gratis/mes) | Application Insights Pay-as-you-go | $0.00 (Free) | $12.00 / mes |
| **TOTAL MENSUAL ESTIMADO (USD)** | | | **$17.00 / mes** | **$181.00 / mes** |

##### 4.3 Directrices FinOps para Optimización de Costos

1. **Escala a cero en valle:** Configurar reglas KEDA en Azure Container Apps para apagar réplicas inactivas entre 12:00 AM y 6:00 AM en entornos de desarrollo y staging.
2. **Ciclo de vida en Blob Storage:** Configurar políticas de almacenamiento para mover PDFs de RUT verificados hace más de 6 meses a nivel de acceso Cool, reduciendo costos de retención.
3. **Connection Pooling controlado:** HikariCP configurado a un máximo de 5 conexiones por pod en staging para evitar sobrepasar los límites de memoria RAM de la instancia PostgreSQL B1ms.

---

#### 05. Radar Tecnológico (Tech Radar)

El Technology Radar de StyleRadar categoriza las tecnologías según su nivel de adopción institucional, mitigando el riesgo de obsolescencia o dispersión técnica:

##### Cuadrante 1: Lenguajes & Frameworks
* **ADOPT:** Java 21 LTS, Spring Boot 3.2.x, React 18, Tailwind CSS, Vite.
* **TRIAL:** Spring Cloud Gateway Reactive, Spring Data Specifications.
* **ASSESS:** Virtual Threads en producción masiva, Keycloak OpenID Connect.
* **HOLD:** Monolito no modular, Node.js sin TypeScript, frameworks sin tipado.

##### Cuadrante 2: Almacenamiento & Datos
* **ADOPT:** PostgreSQL 16 (Database-per-Service), Flyway 10.20.1 (`V1..V7`), Azure Blob Storage (vía puerto `AlmacenamientoBlobPort`).
* **ADOPT:** SDK Azure Blob Storage v12 para la subida directa de binarios desde `AzureBlobStorageAdapter` (CADCHE-117).
* **ASSESS:** PostGIS y Redis en iteraciones futuras si aumenta la carga de lectura geoespacial.
* **HOLD:** MongoDB para transacciones relacionales de NIT o binarios `BYTEA` dentro de PostgreSQL.

##### Cuadrante 3: Plataforma & DevOps
* **ADOPT:** Docker, Docker Compose, Azure Container Apps, GitHub Actions.
* **TRIAL:** CloudAMQP RabbitMQ gestionado, Azure Front Door CDN.
* **ASSESS:** Kubernetes (AKS) bare-metal, Istio Service Mesh, OpenTelemetry.
* **HOLD:** Instancias GPU dedicadas sin auto-escalado, despliegues manuales SSH.

##### Cuadrante 4: Herramientas & Calidad
* **ADOPT:** JUnit 5, Mockito, Testcontainers, SonarQube, JaCoCo, Flyway.
* **TRIAL:** FASHN AI VTON API, SpringDoc OpenAPI (Swagger 3 UI).
* **ASSESS:** Playwright para pruebas E2E automatizadas en web móvil.
* **HOLD:** Apache Kafka para comunicación simple entre 13 microservicios.

*Criterio de transición:* Una tecnología pasa de TRIAL a ADOPT únicamente tras validar su desempeño en un sprint productivo, haber alcanzado cobertura de pruebas igual o superior al 85% y contar con documentación de arquitectura aprobada.

---

#### 06. Matriz de Riesgos Técnicos y Estrategias de Mitigación

| ID | Riesgo Técnico Identificado | Prob. / Imp. | Impacto Potencial | Estrategia de Mitigación Diseñada |
| :---: | :--- | :---: | :--- | :--- |
| **R1** | Vendor Lock-In de Servicios Cloud | Baja / Medio | Dificultad de migrar la infraestructura si los costos de Azure aumentan imprevistamente. | Empaquetado OCI estándar (Docker), uso de APIs abiertas (PostgreSQL, AMQP 0-9-1) y repositorios agnósticos de almacenamiento. |
| **R2** | Latencia Elevada en Inferencia IA | Alta / Alto | Tiempos de espera de 8 a 15 segundos en el probador virtual degradando la UX del usuario. | Procesamiento asíncrono con colas RabbitMQ; polling o WebSockets en frontend; compresión previa de fotos a 720p. |
| **R3** | Agotamiento de Pool de Conexiones DB | Media / Crítico | 13 microservicios x N réplicas saturando el límite de conexiones de PostgreSQL Flexible Server. | Configuración restrictiva de HikariCP (`maximum-pool-size: 5`) por pod; uso de PgBouncer en capas de persistencia compartidas. |
| **R4** | Carga en consultas BOLA bajo alta concurrencia | Baja / Medio | Latencia adicional al validar propiedad `(almacen_nit, usuario_id)` en PostgreSQL en cada mutación. | Índices relacionales y restricción `UNIQUE (almacen_nit, usuario_id)` en `administradores_almacenes`; caché distribuida evaluada en iteraciones futuras. |
| **R5** | Deuda Técnica por Curva de Aprendizaje | Baja / Medio | Errores de configuración o lentitud de entrega por adopción de stacks desconocidos. | Unificación del stack core en Java 21 / Spring Boot dominado por el equipo; Quality Gate obligatorio en CI/CD con SonarQube. |

##### Riesgo Crítico Específico de `ms-tiendas`: Carga de Archivos Maliciosos o Agotamiento de Disco por Imágenes
* **Riesgo:** Carga de scripts ejecutables camuflados con extensión `.pdf` en el formulario de registro de tienda, o saturación del ancho de banda y memoria por imágenes masivas que degraden el contenedor.
* **Mitigación:** Validación en dominio puro de la cabecera binaria del archivo (`%PDF` para documentos RUT); validación de tipo MIME real con Apache Tika / Java NIO; límite estricto de 6 MB por archivo; delegación inmediata del flujo binario a Azure Blob Storage conservando únicamente la URL segura y metadatos en PostgreSQL.

---

#### 07. Matriz de Asignación Tecnológica del Ecosistema (Segmentación Oficial de Puertos)

Consolidación técnica alineada con la segmentación oficial de StyleRadar (bloques `8100` a `8500`):

| Servicio / Puerto Oficial | Paquete de Dominio | Persistencia / Almacenamiento | Caché / Estado | Integración / Herramientas Clave |
| :--- | :--- | :--- | :--- | :--- |
| **`sr-api-gateway`** (`:8080`) | Perímetro | Sin estado (Stateless) | Límite de tasa perimetral | Enrutamiento HTTP centralizado hacia los paquetes P1–P5 |
| **`ms-tiendas`** (`:8140`) | **P1: Identidad (CADche)** | **PostgreSQL 16 (`styleradar_tiendas`) + `AzureBlobStorageAdapter`** | **Stateless (JwtAuthFilter HS256)** | **Flyway `V1..V7`, SpringDoc OpenAPI, AMQP deshabilitado hoy en QA/PROD** |
| **`ms-auth`** (`:8100`) | **P1: Identidad (CADche)** | **PostgreSQL 16 (`styleradar_auth`)** | **Stateless JWT HS256 / OTP** | **Emisión JWT, BCrypt, JavaMail** |
| **`ms-usuarios`** (`:8120`) | **P1: Identidad (CADche)** | **PostgreSQL 16 (`styleradar_usuarios`)** | **Stateless** | **Gestión de perfiles y usuarios** |
| **`ms-inventario`** (`:8200`) | P2: Catálogo | PostgreSQL 16 | En evolución por equipo RESTrepo | Control de stock y tallas |
| **`ms-item-catalogo`** (`:8220`) | P2: Catálogo | PostgreSQL 16 + Blob | En evolución por equipo RESTrepo | Gestión de prendas y moderación |
| **`ms-geolocalizacion`** (`:8240`) | P2: Catálogo | PostgreSQL + PostGIS | En evolución por equipo RESTrepo | Consultas de proximidad geográfica |
| **`ms-playlist`** (`:8260`) | P2: Catálogo | PostgreSQL 16 | En evolución por equipo RESTrepo | Colecciones y listas de estilo |
| **`ms-notificaciones`** (`:8300`) | P3: Notificaciones | PostgreSQL 16 | En evolución por equipo Guatoque | Envío de alertas y correos |
| **`ms-chat`** (`:8320`) | P3: Notificaciones | PostgreSQL 16 | En evolución por equipo Guatoque | Mensajería en tiempo real |
| **`ms-pagos-suscripciones`** (`:8400`) | P4: Pagos y Estadísticas | PostgreSQL 16 | En evolución por equipo NQS | Pasarela de pagos y planes |
| **`ms-estadisticas`** (`:8420`) | P4: Pagos y Estadísticas | PostgreSQL 16 | En evolución por equipo NQS | Analítica y reportes |
| **`ms-probador-virtual`** (`:8500`) | P5: Experiencia | PostgreSQL 16 + Blob | En evolución por equipo Portal 8080 | Procesamiento AI de probador virtual |
| **`ms-segunda-vida-donaciones`** (`:8520`) | P5: Experiencia | PostgreSQL 16 | Suscriptor futuro `foundation.deactivated` / `foundation.reactivated` | Gestión de donaciones y moda circular |

*Aislamiento por base de datos (Database-per-Service):* Cada microservicio posee su propio esquema o base de datos aislada en PostgreSQL 16 para garantizar bajo acoplamiento e independencia total de despliegue.

##### Justificación de las Cuatro Preguntas Clave para `ms-tiendas`:

1. **¿Requiere Base de Datos o no?**
   **Sí requiere base de datos propia.** El microservicio es la fuente de verdad de los almacenes aliados y fundaciones en StyleRadar: persiste `almacenes` (PK `nit`), `almacen_categorias`, `administradores_almacenes` (`(almacen_nit, usuario_id)` único), `almacen_fotos`, `fundaciones` y `fundacion_categorias_aceptadas`, además de estados de verificación (`PENDING`, `APPROVED`, `REJECTED`) y configuración de umbrales/periodos de catálogo.

2. **Tipo de Base de Datos y Motor Seleccionado:**
   **Persistencia Relacional en PostgreSQL 16 (`styleradar_tiendas`) + puerto `AlmacenamientoBlobPort` (`AzureBlobStorageAdapter`).** PostgreSQL garantiza atomicidad transaccional y reglas estructurales (`almacenes.nit` PK, `UNIQUE (almacen_nit, usuario_id)`, llaves foráneas con `ON DELETE CASCADE`), mientras que la regla de dominio `Almacen.MAXIMO_FOTOGRAFIAS = 6` controla la cuota de vitrina en `almacen_fotos`. Actualmente `ms-tiendas` **no utiliza Redis**.

3. **Frameworks, Lenguaje y Herramientas:**
   **Java 21 LTS, Spring Boot 3.2.12, Spring Data JPA, Jakarta Bean Validation 3, Flyway 10.20.1, SpringDoc OpenAPI 2.3.0, MapStruct 1.5.5.Final y Lombok 1.18.42.** Spring Boot gestiona el límite multipart (`6MB` por archivo, `7MB` por petición). Flyway versiona `V1..V7` de `styleradar_tiendas`, y los 9 casos de uso se cablean mediante `@Service` implementando `domain.port.in.*UseCase` sin acoplar Spring al dominio puro.

4. **Broker de Mensajería:**
   **Spring AMQP / RabbitMQ (`RabbitMQFundacionEventPublisherAdapter`).** El microservicio implementa la publicación de `FundacionDesactivadaEvent` hacia el exchange `styleradar.exchange` (routing keys `foundation.deactivated` y `foundation.reactivated`) capturando `AmqpException` para no bloquear las confirmaciones SQL. Actualmente **ni `ms-tiendas` ni `ms-auth` se conectan al broker RabbitMQ compartido** y `management.health.rabbit.enabled=false` está configurado en QA, PROD y DEV.

---

#### 08. Conclusiones Arquitectónicas y Validación Formal

##### 8.1 Resumen de Decisiones y Factores de Éxito

La adopción de Java 21 LTS y Spring Boot 3.2.12 garantiza un núcleo tipado, compilado con SpotBugs en 0 errores y cobertura JaCoCo de 94.46% instrucciones / 95.45% líneas sobre 408 pruebas. La persistencia relacional en PostgreSQL 16 con migraciones Flyway `V1..V7` e infraestructura stateless (`JwtAuthFilter` + `AutorizacionAlmacenValidator`) asegura integridad de datos y aislamiento multi-tenant sin dependencias externas innecesarias.

##### 8.2 Roadmap de Evolución Tecnológica

| Fase / Sprint | Objetivo Tecnológico Clave | Criterio de Aceptación |
| :--- | :--- | :--- |
| **Sprint 1 (Semanas 1-2)** | Core arquitectónico: Autenticación JWT, Flyway `V1..V7` y dominio hexagonal puro. | Tokens HS256 validados; migraciones ejecutadas en PostgreSQL 16. |
| **Sprint 2 (Semanas 3-4)** | Fotografías de vitrina (máx. 6), verificación RUT, fundaciones y control BOLA (`SR-RNF-01`). | Endpoints OpenAPI validados; ≥ 85% cobertura JaCoCo; 0 bugs SpotBugs. |
| **Sprint 3 (Semanas 5-6)** | Cableado persistente de los 8 endpoints de contrato DTO pendientes y SDK Azure Blob real. | Todos los endpoints persistidos en PostgreSQL 16 y blobs reales en Azurite/Blob. |
| **Sprint 4 (Semanas 7-8)** | Activación de mensajería AMQP TLS 5671 (cuando aplique) y pruebas E2E integradas. | Healthcheck limpio y smoke tests pasando en Azure QA y PROD. |

##### 8.3 Acta de Validación y Firmas del Equipo de Arquitectura (`CADche`)

| Rol en el Proyecto | Nombre del Ingeniero | Decisión / Estado | Firma / Aprobación |
| :--- | :--- | :---: | :---: |
| **Líder de Equipo (CADche)** | Juan Diego Valderrama | APPROVED | `J. D. Valderrama` |
| **Tech Lead / Arquitecto & Backend Developer** | Julian Camilo Giral (`@jcobos548`) | APPROVED | `J. C. Giral` |
| **Product Owner / Scrum Master & Backend Developer** | Juan Diego Gaitan (`@mamagege`) | APPROVED | `J. D. Gaitan` |

*Aprobación para el Desarrollo:* El presente documento constituye la línea base técnica (Technical Baseline) autorizada por el equipo CADche para el desarrollo de los microservicios de StyleRadar.

---

[⬅ Volver al README Principal](../../README.md)
