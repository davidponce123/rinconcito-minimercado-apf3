# Rinconcito Minimercado · APF3

Proyecto académico de **sistema web de ventas e inventario** basado en el APF2 entregado por el equipo UTP y la rúbrica del Avance de Proyecto Final 3. Código Java 25 (LTS) / Spring Boot MVC / Thymeleaf / PostgreSQL / Spring Security / DAO JDBC.

> **Estado transparente:** `mvn clean verify` pasó con Java 25 LTS y Spring Boot 3.5.16; las 7 pruebas unitarias JUnit/Mockito pasaron. No se verificaron aquí PostgreSQL, pruebas de integración contra una base real, ni la interfaz en navegador. El equipo debe realizar y documentar esas comprobaciones antes de presentar resultados como verificados. El repositorio todavía no está publicado en GitHub.

## Funciones y rutas diseñadas e implementadas en código

| RF | Función | Rutas principales | Estado de evidencia |
|---|---|---|---|
| RF01 | Inicio de sesión, BCrypt, permisos y estado de cuenta | `/login` | Código; ejecución pendiente |
| RF02 | Venta, detalles, precio histórico y forma de pago | `/sales`, `/sales/new` | Código y unit tests; ejecución pendiente |
| RF03 | Salidas y stock derivado de movimientos | `/inventory`, `v_stock` | Código y SQL; integración pendiente |
| RF04 | Alertas de stock mínimo | `/dashboard`, `/inventory` | Código; interfaz pendiente de verificación |
| RF05 | Exportación de ventas e inventario a Excel | `/reports/sales.xlsx`, `/reports/inventory.xlsx` | Código Apache POI; archivo pendiente de probar |
| RF06 | Recepción, lotes, proveedor, guía y entradas | `/receipts`, `/receipts/new` | Código y unit tests; integración pendiente |
| RF07 | Gestión básica de categorías y productos | `/products` | Código y unit tests; integración pendiente |
| RF08 | Alta y deshabilitación de cuentas con roles | `/users` | Código; integración pendiente |
| RF09 | Alta, consulta y habilitación de proveedores | `/suppliers` | Código; integración pendiente |
| RF10 | Baja rotación >30 días y decisión | `/slow-moving` | Código; validación de reglas pendiente |

La existencia de controladores y pantallas **no equivale a una aprobación funcional del 100 %**. El informe distingue explícitamente cobertura en código y validación ejecutada.

## Preparación de entorno

**Requisitos:** JDK 25 (LTS), Maven 3.9+, Docker Desktop con Docker Compose (o PostgreSQL 16), Git y un navegador Chrome/Edge.

1. Copiar `.env.example` a `.env` y asignar una contraseña local fuerte de base de datos y una clave inicial de al menos 12 caracteres para `ADMIN_PASSWORD`. No publicar `.env`.
2. En la carpeta raíz, iniciar PostgreSQL: `docker compose up -d`.
3. Esperar estado saludable: `docker compose ps`.
4. Crear esquema en PostgreSQL (Linux/macOS; sustituyendo `DB_PASSWORD`):
   ```bash
   docker compose exec -T database psql -U rinconcito_app -d rinconcito < src/main/resources/db/schema.sql
   docker compose exec -T database psql -U rinconcito_app -d rinconcito < src/main/resources/db/demo_seed.sql
   ```
5. Exportar las variables de entorno (Linux/macOS):
   ```bash
   set -a; source .env; set +a
   mvn clean test
   mvn spring-boot:run
   ```
   En PowerShell configurar variables con `$env:DB_PASSWORD="..."`, `$env:ADMIN_USER="..."`, `$env:ADMIN_PASSWORD="..."` antes de ejecutar Maven. Las variables `DB_URL` y `DB_USER` tienen valores por defecto locales.
6. Abrir `http://localhost:8080/login`. La primera cuenta `ADMIN_USER` se crea al iniciar si las variables existen y la base contiene las tablas.
7. Para probar ventas primero registrar una recepción con stock positivo. Los datos en `demo_seed.sql` son **simulados** y empiezan con existencias en cero.

**Importante:** `schema.sql` está pensado para **una base vacía**, no debe ejecutarse dos veces. `demo_seed.sql` tampoco es idempotente en todas sus filas. Antes de migrar una base con datos se debe elaborar una migración controlada. Se usa HTTP únicamente para desarrollo local; en producción implementar HTTPS, cookies `Secure`, rotación de claves, copias/restauración verificadas y controles de acceso a la red.

## Pruebas y verificación

- `mvn clean test` — ejecuta JUnit 5 + Mockito (requiere dependencias descargables).
- `mvn spring-boot:run` — requiere PostgreSQL accesible y esquema aplicado.
- `mvn javadoc:javadoc` — documentación Java con dependencias resueltas.
- `docs/javadoc/index.html` — Javadoc generado en este entorno para dominio y contratos DAO.
- `docs/PLAN_PRUEBAS.md` — casos manuales y de integración; **no están marcados aprobados**.

**Caso crítico de concurrencia:** abrir dos sesiones de caja, cargar el último artículo disponible, confirmar a la vez. Se espera una única venta exitosa y una segunda rechazada, sin stock negativo. El diseño usa `SELECT ... FOR UPDATE` sobre `inventario` en orden creciente de producto y transacciones `@Transactional`; este caso **debe probarse con PostgreSQL real**.

## Arquitectura y principios

- **MVC:** `controller/` recibe formularios HTTP; `service/` concentra reglas de negocio; `templates/` presenta HTML.
- **DAO:** `dao/` contiene interfaces; `dao/jdbc/` implementaciones SQL parametrizadas sobre JdbcTemplate.
- **SOLID:** responsabilidad única en servicios, inversión de dependencias mediante contratos DAO; composición sobre herencia en reglas de aplicación.
- **TDD:** tests de casos normales, errores de inventario y lotes múltiples con JUnit/Mockito. El ciclo rojo-verde-refactor debe demostrarse mediante el historial real de Git cuando se ejecute; **no se finge un ciclo previo**.
- **Seguridad:** BCrypt, sesiones, CSRF, autorización en servidor, usuarios activos y sin contraseñas fijas.
- **Librerías de rúbrica:** Guava (validación), Apache Commons Lang (texto), Apache POI (Excel), Logback (registro).

### Modelo de base de datos

14 tablas: `rol`, `usuario`, `administrador`, `cajero`, `categoria`, `proveedor`, `producto`, `inventario`, `venta`, `detalle_venta`, `ingreso_mercaderia`, `detalle_ingreso`, `movimiento_inventario` y `accion_baja_rotacion`. Las vistas `v_stock` y `v_venta_total` derivan existencias y totales. `detalle_ingreso.fecha_vencimiento` identifica el lote recibido, no el producto genérico. Un ingreso puede tener varios lotes del mismo producto; **una entrada agregada** por producto preserva el conteo y cumple la unicidad de movimientos.

**Cambio respecto al APF2:** se eliminó la referencia textual de origen de movimientos y se usan claves foráneas a `venta` e `ingreso_mercaderia`; se evita una tabla adicional de auditoría que hubiera elevado el esquema a 15 tablas. Cambios operativos se registran también en Logback; no equivale a una auditoría de datos íntegra.

## Versionamiento real en Git y GitHub

El equipo debe crear su propio repositorio (no se ha publicado en su nombre). Comandos iniciales, desde la carpeta raíz:

```bash
git init
git branch -M main
git add .
git commit -m "feat: arquitectura MVC y esquema PostgreSQL APF3"
git remote add origin https://github.com/USUARIO/REPOSITORIO.git
git push -u origin main
```

Al iterar, realizar commits con evidencia real como `test: casos de stock insuficiente`, `feat: formulario de ventas`, `fix: recepción con lotes múltiples`, `docs: informe APF3`, usando ramas y revisiones cuando corresponda. No fabricar commits retroactivos. Documentar URL del repo, fecha de publicación, colaboradores, código revisado, resultado de `mvn test` y capturas del historial real. Evitar `.env`, secretos, bases de datos reales y datos personales.

## Estructura del paquete

- `src/main/java/...`: código Java documentado.
- `src/main/resources/templates/`: once pantallas Thymeleaf.
- `src/main/resources/static/`: estilos e interacción básica.
- `src/main/resources/db/`: esquema de 14 tablas y datos sintéticos.
- `src/test/`: casos de prueba JUnit/Mockito escritos.
- `diagrams/`: diagramas de arquitectura y modelo de datos (DOT + PNG).
- `docs/`: plan de pruebas, guía de demostración y Javadoc parcial.

## Alcance de autoría y validación

Paquete elaborado como base asistida para trabajo académico. El equipo es responsable de revisar, comprender, ejecutar, adaptar, mantener y defender cada bloque de código; no declarar pruebas, publicaciones, entrevistas ni validación con usuarios que no hayan tenido lugar.
