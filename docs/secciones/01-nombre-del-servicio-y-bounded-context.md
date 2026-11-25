[⬅ Volver al README Principal](../../README.md)

---

## 1. NOMBRE DEL SERVICIO Y SU BOUNDED CONTEXT

* **Identificador de Servicio:** `ms-tiendas` (`CADCHE-Stores-Service`)
* **Paquete Arquitectónico:** `Paquete 1: Identidad` · Equipo `CADche`
* **Puerto de Escucha Oficial:** `8140` (bloque `8100` del equipo: ms-auth `8100`, ms-usuarios `8120`, ms-tiendas `8140`)
* **Base de Datos Asignada:** `PostgreSQL 16` (`styleradar_tiendas`), una por ambiente (QA y PROD separadas), con migraciones **Flyway V1..V7**. Los binarios (RUT en PDF, fotos y certificados) van a **Azure Blob Storage**; la base solo guarda la URL.
* **Bounded Context:** `Gestión de Tiendas Aliadas y Fundaciones`

### Responsabilidades del Dominio (historias del Sprint 2)

| Historia | Qué hace |
| :--- | :--- |
| **SR-RF-59** · Últimas unidades | Umbral configurable por almacén (`0..50`) y distintivo «Últimas unidades» por prenda. |
| **SR-RF-60** · Nueva llegada | Periodo configurable (`1..60` días) y distintivo «Nueva llegada». |
| **SR-RF-62-A** · Registrar almacén | La tienda registra su almacén y sube el RUT (PDF ≤ 5 MB); queda `PENDING`. |
| **SR-RF-62** · Verificar almacén | El admin aprueba (RUT obligatorio) o rechaza (motivo de 10 a 1000 caracteres) y puede reabrir la revisión. |
| **SR-RF-63 / 64 / 65** · Fundaciones | Registrar, editar, desactivar y reactivar fundaciones aliadas; publica eventos al desactivar o reactivar. |
| **SR-RF-74** · Fotografías | Hasta `6` fotos por tienda (JPG, PNG o WEBP ≤ 6 MB), orden y portada. |
| **SR-RF-116** · Consultar almacenes | Lista pública paginada de almacenes aprobados (filtro por categoría) y detalle por NIT. |
| **SR-RF-117** · Consultar fundaciones | Lista pública de fundaciones activas, por cercanía o por nombre, sin datos internos de registro. |
| **SR-RF-118** · Asociar administrador | El admin asocia cuentas como administradores de un almacén; el administrador ve los de su almacén. |
| **SR-RF-119** · Eliminar almacén | El admin elimina un almacén con sus fotos, su RUT en PDF, configuración y administradores (los archivos también se borran del Blob); publica `store.deleted`. |
| **SR-RF-120** · Eliminar fundación | El admin elimina una fundación y su certificado; publica `foundation.deleted`. |

**Qué NO hace:** no autentica (los JWT los emite `ms-auth`), no guarda usuarios (`ms-usuarios`), ni inventario o catálogo de prendas (`ms-inventario`, `ms-item-catalogo`).

---

[⬅ Volver al README Principal](../../README.md)
