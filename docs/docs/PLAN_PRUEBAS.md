# Plan de pruebas y acta de evidencias · APF3

**Estado actual de ejecución:** NO EJECUTADAS en PostgreSQL ni Maven en este entorno. Los casos se suministran para que el equipo los realice, registre capturas y señale resultado real.

| ID | Prueba | Procedimiento resumido | Resultado esperado | Evidencia |
|---|---|---|---|---|
| PT01 | Login válido | Credenciales creadas desde variables | Acceso a panel y sesión | Captura de panel |
| PT02 | Login inválido | Clave incorrecta | Denegación sin exponer secretos | Captura mensaje |
| PT03 | Autorización por rol | Cajero abre `/users` | HTTP 403 | Captura/respuesta |
| PT04 | Crear producto | Alta válida y mínimo 5 | Producto e inventario configurados | Pantalla + SELECT |
| PT05 | Ingreso de dos lotes | Producto repetido con dos vencimientos | 2 detalles y 1 entrada sumada | SELECT tablas |
| PT06 | Venta normal | Recibir 10 y vender 3 | Stock resultante 7 | Pantalla + SELECT |
| PT07 | Stock insuficiente | Intentar vender 8 unidades si quedan 7 | Rechazo; sin venta ni salida | Antes/después SQL |
| PT08 | Concurrencia | Dos cajas venden última unidad a la vez | Una venta confirmada y otra denegada | Logs y consultas |
| PT09 | Guía duplicada | Repetir guía del mismo proveedor | Error y transacción revertida | Log + SELECT |
| PT10 | Excel inventario | Pulsar reporte | Descarga XLSX legible y saldos correctos | Archivo + Excel |
| PT11 | Excel ventas | Elegir periodo y descargar | Totales desde detalle y ventas pagadas | Archivo + SELECT |
| PT12 | Baja rotación | Datos con salida de >30 días | Producto y urgencia correctos | SQL + captura |
| PT13 | Pruebas automáticas | `mvn clean test` | Pruebas JUnit5 aprobadas | Salida y reporte Surefire |
| PT14 | Seguridad | Revisar sesión, csrf y hash | 403, 403 CSRF y hash no reversible | Evidencia técnica |
| PT15 | Javadoc | `mvn javadoc:javadoc` | Documentación generada | Página `index.html` |
| PT16 | GitHub | Repositorio del equipo versionado | Historial y enlaces verificables | URL/commits |

**Formato de registro por caso**: fecha real | responsable | entorno (SO, Java, PG) | entradas | salida | aprobado/rechazado | captura o enlace | incidencia y corrección.

## Consultas de evidencia (ejecutar únicamente en base de pruebas)

```sql
SELECT id_producto,nombre,stock_actual,stock_minimo FROM v_stock ORDER BY id_producto;
SELECT v.id_venta,v.fecha_pago,t.total FROM venta v JOIN v_venta_total t USING(id_venta) ORDER BY v.id_venta DESC;
SELECT id_ingreso,id_producto,cantidad,fecha_vencimiento FROM detalle_ingreso ORDER BY id_ingreso DESC LIMIT 20;
SELECT id_ingreso,id_producto,SUM(cantidad) AS suma_movimientos FROM movimiento_inventario WHERE tipo='ENTRADA' GROUP BY id_ingreso,id_producto;
```

**Metas del APF2 que no se deben inventar:** p95 de venta ≤2 s, venta ≤60 s, disponibilidad 99 %, respaldo diario y recuperación ≤4 h. Solo reportar resultados medidos con identificación de hardware, fechas, muestra, método y capturas/logs.
