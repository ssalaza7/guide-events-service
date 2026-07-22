# guide-events-service

API REST que recibe un evento de cambio de estado de una guía (creada, en tránsito, entregada, novedad, etc.), valida que sea un caso real, evita procesarlo dos veces, y lo publica en RabbitMQ para que otros sistemas (tracking, notificaciones al cliente, BI) lo consuman casi en tiempo real.

Construido como ejercicio previo a una conversación técnica sobre arquitectura para TCC (logística/mensajería). Alcance deliberadamente acotado — no todas las decisiones de arquitectura de la conversación están implementadas en código, solo las que cambian el comportamiento de este servicio.

## Stack

- Java 21, Spring Boot 4.0.7, Gradle (Kotlin DSL)
- WebFlux (no bloqueante) + reactor-rabbitmq para publicar en RabbitMQ
- Redis (reactivo, vía Lettuce) para idempotencia
- JUnit 5, Mockito, reactor-test (`StepVerifier`), JaCoCo (gate 90%)

## Arquitectura: hexagonal

```
domain/
  model/           → GuideEvent, GuideEventStatus, ProcessResult
  eventos/         → EventPublisher, ProcessedEventStore, GuideValidador
  exception/       → InvalidGuideEventException, EventPublishingException
application/       → ProcessGuideEventUseCase, ProcessGuideEventService (el caso de uso)
infrastructure/
  controllers/     → GuideEventController, DTOs, manejo de errores
  adapters/        → RabbitMQAdapter, RabbitMQConfig, RedisAdapter, GuideValidadorAdapter
  config/          → AppConfig
```

Las dependencias apuntan hacia adentro: `domain` y `application` no conocen Spring Web, RabbitMQ ni Redis. Cada adaptador implementa una interfaz de `domain/eventos`; cambiar de tecnología es escribir un adaptador nuevo, sin tocar el caso de uso ni sus pruebas.

**Por qué las interfaces de salida (`EventPublisher`, `ProcessedEventStore`, `GuideValidador`) viven en `domain/eventos` y no en `application`:** siguen el mismo principio que un repositorio en DDD clásico — la interfaz expresa una necesidad del dominio ("necesito anunciar esto", "necesito saber si ya lo procesé", "necesito saber si esta guía es real"), y la implementación concreta (RabbitMQ, Redis, el futuro servicio de guías) es un detalle de infraestructura. `ProcessGuideEventUseCase`/`ProcessGuideEventService` sí se quedan en `application` porque son orquestación (qué hacer y en qué orden), no una necesidad del dominio en sí.

## Decisiones de diseño

**Mono de punta a punta, Flux solo donde la librería lo exige.** Cada request HTTP es un evento (cardinalidad 1) → `Mono` en controller/caso de uso/interfaces de dominio. `Flux` aparece únicamente en `RabbitMQAdapter`, porque `Sender.sendWithPublishConfirms` de reactor-rabbitmq está diseñado alrededor de `Flux` para lograr throughput.

**Por qué WebFlux + reactor-rabbitmq y no Spring MVC + RabbitTemplate.** Ningún hilo se bloquea esperando el ack del broker: un pool pequeño de hilos Netty puede tener miles de requests concurrentes en vuelo.

**Confirmaciones del broker (publisher confirms).** El `Mono` de `publish()` solo completa cuando RabbitMQ confirma haber aceptado el mensaje — si el broker no confirma o rechaza, la petición HTTP responde `503` para que el cliente reintente. Así se evita perder eventos silenciosamente.

**Idempotencia: patrón Idempotent Consumer con reserva atómica en Redis.** `ProcessedEventStore.reserve(eventId)` usa `SET NX EX`: si el mismo `eventId` llega dos veces casi al mismo tiempo —incluso a instancias distintas—, solo una gana la reserva y publica; la otra responde `200 DUPLICATE_IGNORED` sin republicar. Si el publish falla después de reservar, se libera la reserva (`release`) para no bloquear un reintento legítimo. Es el mismo patrón que usa Stripe con su header `Idempotency-Key`. El TTL (60 min por defecto, configurable) acota cuánto se recuerda un `eventId` — la deduplicación de largo plazo es responsabilidad del consumidor final, que de todas formas necesita su propia escritura idempotente.

**`GuideValidador`: la interfaz existe, la implementación (`GuideValidadorAdapter`) es un marcador de posición explícito.** Antes de aceptar un evento, el caso de uso pregunta `guideValidador.isValid(event)`. Hoy `GuideValidadorAdapter` siempre responde `true` — está documentado así en el propio código (ver comentario en la clase). En producción esto debe reemplazarse por una integración real con el servicio de consulta de guías (el TMS), que confirme que el `guideId` existe y que la operación no es fraudulenta, antes de publicar. No se simuló con una base de datos ficticia en este ejercicio porque no hay ningún otro sistema real con el que integrar en este alcance; la interfaz deja explícito dónde debe conectarse.

**Qué queda fuera de este código a propósito (temas para la sesión en vivo):**
- **Dead-letter queue** — la cola se declara simple; DLQ es una extensión de la topología en `RabbitMQConfig`, no un cambio de arquitectura.
- **Validación de transición de estado** (ej. no pasar de `ENTREGADA` a `CREADA`) — necesita el estado anterior de la guía, que vendría de la misma integración real que hoy cubre `GuideValidadorAdapter`.
- **Autenticación/autorización entre sistemas** — "cualquiera con el API" no debería poder llamar al endpoint sin credenciales; se resuelve con API key/mTLS/OAuth2 en el borde, no con validación de datos. Se explica como decisión, no implementada aquí.
- **Observabilidad (métricas Prometheus)** — se explica como decisión de arquitectura.

## Cómo correr y probar

```bash
docker compose up -d        # RabbitMQ (UI en :15672, guest/guest) + Redis
./gradlew bootRun            # arranca en :8080
```

```bash
curl -i -X POST http://localhost:8080/api/v1/guide-events \
  -H "Content-Type: application/json" \
  -d '{
    "eventId": "evt-demo-1",
    "guideId": "GUI-COL-000123",
    "status": "en_transito",
    "occurredAt": "2026-07-21T15:00:00Z",
    "location": "Bogota - CD Norte",
    "description": "Paquete en camino a destino",
    "metadata": {"carrier": "TCC", "channel": "app"}
  }'
# 202 Accepted

# Repetir el mismo eventId -> 200 OK, result DUPLICATE_IGNORED
```

Errores: `400` (payload inválido, estado no soportado, o guía no validada), `503` (el broker rechazó o no confirmó la publicación).

Ver el mensaje en RabbitMQ: UI en http://localhost:15672 → Queues → `tcc.guide-events.status.queue` → "Get messages".

Ver la clave de idempotencia en Redis: `docker exec -it guide-events-redis redis-cli KEYS "guide-events:processed:*"`.

## Pruebas y cobertura

```bash
./gradlew clean check
```

Pruebas unitarias (JUnit 5 + Mockito + `StepVerifier`) para dominio, caso de uso, controller, y los tres adaptadores (RabbitMQ, Redis, validador de guías). JaCoCo falla la tarea `check` si la cobertura de línea o de instrucciones cae debajo del 90% (`build.gradle.kts`, tarea `jacocoTestCoverageVerification`). Estado actual: **95.5% líneas / 97.4% instrucciones**.

## De la idea a producción

- **CI**: `./gradlew clean check` (tests + gate de cobertura) en cada PR.
- **CD**: imagen Docker (plugin `org.springframework.boot` de Gradle, `bootBuildImage`), despliegue a Kubernetes con `readinessProbe`/`livenessProbe` en `/actuator/health`.
- **Siguiente iteración natural, en orden de prioridad**: reemplazar `GuideValidadorAdapter` por la integración real con el TMS, DLQ, autenticación entre sistemas, circuit breaker hacia RabbitMQ, métricas Prometheus.
