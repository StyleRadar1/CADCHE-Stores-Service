[⬅ Volver al README Principal](../../README.md)

---

## 10. RAMAS Y CÓMO CONTRIBUIR

```text
main        <-- Producción (despliega PROD)
└── develop <-- Integración (despliega QA)
    └── feature/<tarea>   <-- Todo cambio nace aquí y entra a develop por Pull Request
```

* Nada de commits directos a `develop` ni a `main`.
* Mensajes en minúsculas y en imperativo: `tipo(alcance): descripción` con los tipos `feat`, `fix`, `refactor`, `test`, `docs`, `build`, `ci` y `chore`. Ejemplo: `fix(tiendas): validar jwt de ms-auth`.

### Checklist para Pull Requests

- [ ] La rama parte del `develop` más reciente.
- [ ] `mvn -B clean verify` pasa (pruebas y JaCoCo ≥ 85 %).
- [ ] SpotBugs sin hallazgos y Sonar en A.
- [ ] Las migraciones nuevas son `V<n>__descripcion.sql` y nunca se edita una ya aplicada.
- [ ] Swagger actualizado si cambió un contrato (y aviso a los equipos que lo consumen).
- [ ] Sin credenciales ni secretos en el código.

---

[⬅ Volver al README Principal](../../README.md)
