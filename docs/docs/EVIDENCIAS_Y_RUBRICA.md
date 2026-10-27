# APF3 - Matriz honesta de entrega y comprobación

Este código continúa el APF2 presentado por el grupo: **web con Java/Spring Boot, Thymeleaf, PostgreSQL, usuarios Cajero/Administrador, stock por movimientos y lotes en recepciones**. No cambia arbitrariamente la tecnología del documento.

| Criterio de rúbrica | Archivo / evidencia que mostrar | Pendiente antes de entrega |
|---|---|---|
| MVC/DAO/SOLID/seguridad (3 pts) | `controller/`, `service/`, `dao/`, `config/SecurityConfig` | Demostrar navegación y acceso por roles |
| 4 librerías Java (2 pts) | `pom.xml`, POI en `ReportsController`, Commons, Guava y Logback | Mostrar exportación Excel y logs sin datos sensibles |
| Git/GitHub (3 pts) | `.gitignore`, README y repo del equipo | Publicar commits REALES de todos los integrantes y proporcionar URL |
| Interfaces (6 pts) | `templates/*.html`, `static/css/app.css` | Abrir y usar pantallas reales en Chrome, capturas propias |
| Producto (4 pts) | RF01–RF10 en código, `db/schema.sql`, `src/test/` | Ejecutar pruebas, validar integridad, documentar alcance comprobado |
| Sustentación (2 pts) | `docs/COMPRENDER_CODIGO.md` y presentación del APF3 | Cada integrante explica y demuestra su parte |

**Riesgo principal:** la rúbrica evalúa funcionamiento y autoría/dominio, no solo existencia de archivos. Ninguna instalación, ejecución, prueba en navegador o publicación GitHub del equipo está verificada por este archivo.

## Alcance definido en APF2 frente a implementación

| RF | Implementación de código disponible | Qué comprobar |
|---|---|---|
| RF01 | Sesión y roles, BCrypt | Login correcto/incorrecto; rutas solo ADMIN |
| RF02 | Formulario de venta y servicio transaccional | Compra pagada genera detalle y salida |
| RF03 | Stock calculado desde movimientos | 10 entradas menos 3 salidas son 7 |
| RF04 | Alertas por stock mínimo | Producto mínimo 5 con stock 3 alerta |
| RF05 | Reportes Excel (POI) | Abrir archivo descargado en Excel |
| RF06 | Recepción con guía y vencimiento de lote | Dos lotes, una entrada consolidada |
| RF07 | CRUD básico de catálogo | Alta, cambio y desactivación |
| RF08 | Cuentas por roles | ADMIN puede crear CAJERO; CAJERO no puede gestionar usuarios |
| RF09 | Proveedores básicos | Crear y deshabilitar proveedor |
| RF10 | Baja rotación >30 días | Datos de prueba con fechas y decisión |

## Evidencia obligatoria en carpeta `evidencias/`

- Capturas de `mvn clean test` terminando en `BUILD SUCCESS` y resultados JUnit.
- Captura real de VS Code con estructura por capas.
- Captura de pgAdmin mostrando las 15 tablas.
- Capturas de pantalla de recepción, venta e inventario con un caso identificado.
- Capturas reales de GitHub con commits de cada participante y URL.
- Nota de incidencias detectadas, correcciones y fecha de verificación.

Nunca crear capturas falsas o atribuir pruebas no ejecutadas.
