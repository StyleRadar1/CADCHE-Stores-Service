[⬅ Volver al README Principal](../../README.md)

---

## 4. EVENTOS QUE PUBLICA Y CONSUME

| Dirección | Exchange / routing key | Evento | Destinatarios | Cuándo |
| :--- | :--- | :--- | :--- | :--- |
| **PUBLICA** | `styleradar.exchange` / `foundation.deactivated` | `FundacionDesactivadaEvent` | `ms-segunda-vida-donaciones` (:8520) | Al desactivar una fundación (SR-RF-65). |
| **PUBLICA** | `styleradar.exchange` / `foundation.reactivated` | `FundacionReactivadaEvent` | `ms-segunda-vida-donaciones` (:8520) | Al reactivarla. |
| **PUBLICA** | `styleradar.exchange` / `foundation.deleted` | `FundacionEliminadaEvent` | Donaciones y notificaciones | Al eliminar una fundación (SR-RF-120). |
| **PUBLICA** | `styleradar.exchange` / `store.deleted` | `AlmacenEliminadoEvent` | Catálogo, inventario, geolocalización y pagos | Al eliminar un almacén (SR-RF-119). |
| **PUBLICA** | `styleradar.exchange` / `foundation.status.changed` | `FundacionEstadoCambiadoEvent` | `ms-estadisticas` | +1 al registrar o reactivar una fundación; -1 al desactivarla o al eliminar una que estaba activa. |
| **PUBLICA** | `styleradar.exchange` / `store.verification.changed` | `VerificacionAlmacenCambiadaEvent` | `ms-estadisticas` | Al registrar un almacén (queda `PENDING`), al aprobarlo o rechazarlo (SR-RF-62) y al reabrir su revisión. |
| **CONSUME** | — | — | — | ms-tiendas no consume eventos. |

Cada evento lleva un `eventId` para que los consumidores sean idempotentes. Campos (análisis v2): `eventId`, `foundationId`, `foundationName`, `representativeId`, `deactivatedAt`/`reactivatedAt` y `reason`.

```json
{ "eventId": "4b1f...", "foundationId": 7, "foundationName": "Fundación Abrigar",
  "representativeId": 30, "deactivatedAt": "2026-11-13T15:04:05Z", "reason": "..." }
```

Eventos de eliminación (análisis v6), con fechas ISO-8601 en UTC:

```json
{ "eventId": "9c2e...", "foundationId": 7, "deletedAt": "2026-11-24T15:04:05Z" }
{ "eventId": "a71d...", "storeNit": "900123456-1", "deletedAt": "2026-11-24T15:04:05Z" }
```

Cambio en el conteo de fundaciones activas. `action` toma `REGISTERED`, `DEACTIVATED`, `REACTIVATED` o `DELETED`; eliminar una fundación ya inactiva no publica este evento:

```json
{ "eventId": "1e9a...", "eventType": "FOUNDATION_STATUS_CHANGED", "foundationId": 7,
  "action": "DEACTIVATED", "active": false, "delta": -1, "occurredAt": "2026-11-25T15:04:05Z" }
```

Cambio de verificación de un almacén. `status` y `previousStatus` toman `PENDING`, `APPROVED` o `REJECTED`; `previousStatus` es `null` cuando el almacén se acaba de registrar:

```json
{ "eventId": "5d0b...", "eventType": "STORE_VERIFICATION_CHANGED", "storeNit": "900123456-1",
  "status": "APPROVED", "previousStatus": "PENDING", "changedAt": "2026-11-24T15:04:05Z" }
```

---

[⬅ Volver al README Principal](../../README.md)
