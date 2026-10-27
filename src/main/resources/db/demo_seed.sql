-- DATOS DE PRUEBA, NO DATOS REALES DEL MINIMERCADO.
-- Ejecutar sobre una base nueva con schema.sql ya aplicado.
BEGIN;
INSERT INTO categoria(nombre,descripcion) VALUES
 ('Abarrotes','Productos de consumo general'),('Bebidas','Bebidas sin alcohol'),
 ('Lácteos','Productos refrigerados'),('Limpieza','Productos de higiene')
ON CONFLICT(nombre) DO NOTHING;
INSERT INTO proveedor(nombre) VALUES
 ('Distribuidora Andina [SIMULADO]'),('Mayorista Norte [SIMULADO]');
INSERT INTO producto(nombre,precio,id_categoria) SELECT 'Arroz 1 kg',4.50,id_categoria FROM categoria WHERE nombre='Abarrotes';
INSERT INTO producto(nombre,precio,id_categoria) SELECT 'Azúcar 1 kg',4.00,id_categoria FROM categoria WHERE nombre='Abarrotes';
INSERT INTO producto(nombre,precio,id_categoria) SELECT 'Leche evaporada',5.80,id_categoria FROM categoria WHERE nombre='Lácteos';
INSERT INTO producto(nombre,precio,id_categoria) SELECT 'Agua mineral',2.00,id_categoria FROM categoria WHERE nombre='Bebidas';
INSERT INTO producto(nombre,precio,id_categoria) SELECT 'Detergente 500 g',8.20,id_categoria FROM categoria WHERE nombre='Limpieza';
INSERT INTO inventario(id_producto,stock_minimo)
 SELECT id_producto,5 FROM producto p WHERE NOT EXISTS(SELECT 1 FROM inventario i WHERE i.id_producto=p.id_producto);
COMMIT;
-- Todas las existencias inician en 0. Registra RECEPCIONES para crear entradas reales.
