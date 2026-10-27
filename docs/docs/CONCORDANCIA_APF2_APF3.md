# Conciliación APF2 mejorado (15 tablas) y código APF3

| Aspecto | APF2 mejorado | ZIP recibido | Paquete consolidado | Pendiente |
|---|---|---|---|---|
| Modelo de BD | 15 tablas incl. auditoria_cambio | schema.sql tenía 14 | schema.sql tiene 15 y hay migración 14→15 | Confirmar en PostgreSQL del equipo |
| Auditoría | Registrar cambios de catálogos, usuario, fecha | No había tabla ni registro de auditoría | Tabla incluida en SQL | Implementar/ejecutar operaciones de auditoría |
| Acceso | Dos perfiles | Spring Security y roles | Conservado | Prueba real rol cajero/admin |
| Ventas | Pago, detalle, salida y ticket interno | Servicio de venta y pantalla | Conservado | Ticket autónomo y prueba |
| Reportes | Exportar PDF o Excel | XLSX identificado | Conservado | PDF opcional comprometido en APF2 |
| Lotes | Vencimiento por lote y varios lotes en ingreso | Permite detalle_ingreso por lote | Conservado | Prueba en PostgreSQL |
| XP / TDD | Plan en iteraciones | Tests escritos | Tests conservados | Evidencia de ciclos real y retrospectivas |
| GitHub | Versionamiento equipo | No hay .git ni URL en ZIP | Instrucciones y plantilla | Commits reales de cinco autores |

**Advertencia:** El diccionario APF2 es diseño documental y algunos campos/tipos
no coinciden literalmente con el SQL de ejecución; para implementar manda
`schema.sql`. Deben actualizar el diccionario al cierre del proyecto y no
presentar dos modelos incompatibles como si fueran iguales.
