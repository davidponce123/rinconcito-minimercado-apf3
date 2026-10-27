# Estado de validación (no confundir diseño con resultados)

Fecha de elaboración del paquete: 22 de octubre de 2026.

**Verificado en este entorno**
- Existencia de los archivos Java, HTML, CSS, JS, SQL, diagramas y pruebas escritas.
- Compilación Maven completa con Java 25 LTS y Spring Boot 3.5.16 (`mvn clean verify`).
- 7/7 pruebas unitarias JUnit/Mockito pasaron con Java 25; producción y clases de prueba compilaron.
- El bytecode de la aplicación se generó para Java 25 (versión mayor 69).
- Javadoc generado para las clases del dominio y contratos DAO.
- 14 declaraciones `CREATE TABLE` en el esquema; se proporcionan también dos vistas.
- Validación básica de estructura HTML y render visual de la presentación y el informe.

**No verificado aquí**
- Conexión a PostgreSQL y pruebas de integridad de SQL en servidor real.
- Apertura real de rutas y sesiones en navegador usando Spring Boot.
- Descargas Excel reales generadas por la aplicación.
- Concurrencia de dos cajas, rendimiento, backups y restauración.
- Publicación en GitHub, revisión de commits, participación individual y validación con propietario.

**Regla de honestidad académica:** incluir resultados de esas pruebas solo cuando el equipo las haya ejecutado y documentado.
