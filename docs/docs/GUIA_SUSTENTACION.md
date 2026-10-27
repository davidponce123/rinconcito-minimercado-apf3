# Guía para sustentación de diez minutos · cinco integrantes

Presentación de ejemplo; asignar los roles según participación real. Ensayar con cronómetro para respetar el máximo de 10 minutos y responder preguntas sin memorizar frases vacías.

**0:00–1:30 · Integrante 1 — Problema y continuidad APF2.** Describir registros manuales, diferencias de stock y demora en caja. Explicar por qué se eligió aplicación web MVC con Java y PostgreSQL. Enfatizar que las cifras del APF2 son metas/datos simulados cuando no se midieron.

**1:30–3:15 · Integrante 2 — Arquitectura y seguridad.** Mostrar el diagrama MVC → servicios → DAO → PostgreSQL, Spring Security, BCrypt, control de acceso y CSRF. Explicar SOLID usando interfaces DAO y responsabilidades separadas.

**3:15–5:15 · Integrante 3 — Base de datos.** Mostrar 14 tablas, vistas y relación de recepción con lotes. Explicar stock derivado de movimientos; transacciones y bloqueos para evitar sobreventa. Demostrar consultas sobre PostgreSQL *solo si se ejecutó realmente*.

**5:15–7:20 · Integrante 4 — Demostración de aplicación.** Iniciar sesión, mostrar producto, crear recepción y registrar una venta. Si el sistema no está ejecutándose, indicar claramente que las imágenes son solo referenciales: no simular un demo real.

**7:20–9:30 · Integrante 5 — Tests, librerías y GitHub.** Mostrar tests JUnit/Mockito y resultados reales, explicar Guava/Commons/POI/Logback, y abrir repositorio GitHub real con cambios. Distinguir pruebas pendientes de aprobadas.

**9:30–10:00 · Cierre.** Cobertura implementada, limitaciones y siguientes pasos: concurrencia, rendimiento, restauración, validación del negocio.

Preguntas habituales: ¿por qué usar DAO si hay JdbcTemplate? (abstraer persistencia); ¿por qué vencimiento en detalle_ingreso? (distintos lotes); ¿cómo evitan stock negativo? (bloqueos y una transacción por operación); ¿TDD aplicado de verdad? (mostrar commit/historial de pruebas o decir que solo existen tests actuales); ¿por qué Java? (alcance del curso y decisión de APF2).
