-- Rinconcito APF3: verificación de modelo, consulta de solo lectura.
-- Ejecutar en pgAdmin > Query Tool, conectado a la base de datos rinconcito.
-- Resultado esperado: 15 tablas y 2 vistas, si se aplicó el esquema correcto.
SELECT count(*) AS total_tablas_rinconcito
FROM information_schema.tables
WHERE table_schema='public' AND table_type='BASE TABLE'
  AND table_name IN (
  'rol',
  'usuario',
  'administrador',
  'cajero',
  'categoria',
  'proveedor',
  'producto',
  'inventario',
  'venta',
  'detalle_venta',
  'ingreso_mercaderia',
  'detalle_ingreso',
  'movimiento_inventario',
  'accion_baja_rotacion',
  'auditoria_cambio');

SELECT table_name AS tabla
FROM information_schema.tables
WHERE table_schema='public' AND table_type='BASE TABLE'
  AND table_name IN ('rol', 'usuario', 'administrador', 'cajero', 'categoria', 'proveedor', 'producto', 'inventario', 'venta', 'detalle_venta', 'ingreso_mercaderia', 'detalle_ingreso', 'movimiento_inventario', 'accion_baja_rotacion', 'auditoria_cambio')
ORDER BY table_name;

SELECT table_name AS vista
FROM information_schema.views
WHERE table_schema='public' AND table_name IN ('v_stock','v_venta_total')
ORDER BY table_name;
