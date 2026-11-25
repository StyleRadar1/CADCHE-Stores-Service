[⬅ Volver al README Principal](../../README.md)

---

## 8. PRUEBAS Y COBERTURA (≥ 85%)

```bash
mvn -B clean verify
mvn -B com.github.spotbugs:spotbugs-maven-plugin:4.8.6.6:check -Dspotbugs.excludeFilterFile=spotbugs-exclude.xml
```

| Métrica (`mvn verify`, rama `feature/arreglos`) | Resultado | Umbral |
| :--- | :---: | :---: |
| Pruebas | **408**, 0 fallos | 100 % en verde |
| Cobertura de instrucciones | **94.5 %** | ≥ 85 % (rompe el build) |
| Cobertura de ramas | **76.4 %** | — |
| SpotBugs | 0 hallazgos | 0 |

| Nivel | Qué cubre |
| :--- | :--- |
| Dominio y validadores | Reglas de `Almacen`, `Fundacion`, umbrales, periodos y roles exactos. |
| Casos de uso (Mockito) | Los 13 casos de uso con sus flujos alternos. |
| WebMvc | Los 5 controladores con el contrato de errores. |
| Seguridad | `JwtAuthFilterTest` (cabeceras falsificadas, token vencido o ajeno), `JwtSecurityValidatorTest` y `OwaspTop10SecurityTest`. |
| **Trazabilidad Word y contrato** | `TrazabilidadRequerimientosTiendasTest`: un caso por cada flujo alterno y regla de negocio de SR-RF-59 a SR-RF-74 (análisis v2), por cada punto del contrato Front ↔ ms-tiendas y por los nombres en inglés del v2, contra PostgreSQL 16 real y con JWT como los de ms-auth. |
| **Integración PostgreSQL 16** | `PersistenciaPostgres16IntegrationTest`: Flyway V1..V7 sobre PostgreSQL 16 real con `ddl-auto=validate`, paginación, propiedad (BOLA) y fundaciones. |

> Testcontainers se omite si no hay Docker. Con Docker 29+ en local exporte `DOCKER_API_VERSION=1.44`.

<!-- evidencias -->
### Evidencia: reporte de cobertura JaCoCo

<div align="center">
  <img src="../evidencias/jacoco-cobertura.png" alt="JaCoCo ms-tiendas" width="90%" />
  <p><sub><b>Figura 8.1:</b> reporte JaCoCo de `mvn verify` (con Docker): líneas 96.0 %, instrucciones 95.2 %, ramas 78.2 %.</sub></p>
</div>

<!-- /evidencias -->

---

[⬅ Volver al README Principal](../../README.md)
