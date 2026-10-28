-- RINCONCITO APF3: 1,000 productos de DEMOSTRACIÓN, no datos reales.
-- USAR SOLO EN UNA BASE LOCAL DE PRUEBAS. Hacer copia de seguridad primero.
-- Requiere que schema.sql ya se haya aplicado. No borra ni altera ventas existentes.
-- Se puede repetir: no duplica productos con los mismos nombres DEMO.
-- Ejecutar en pgAdmin -> Query Tool de la base rinconcito de pruebas.

BEGIN;

INSERT INTO categoria(nombre, descripcion) VALUES
('Demo Abarrotes', 'Categoría ficticia para ensayos'),
('Demo Bebidas', 'Categoría ficticia para ensayos'),
('Demo Limpieza', 'Categoría ficticia para ensayos'),
('Demo Snacks', 'Categoría ficticia para ensayos')
ON CONFLICT(nombre) DO NOTHING;

CREATE TEMP TABLE rinconcito_demo_catalogo ON COMMIT DROP AS
SELECT n AS numero,
       'DEMO-' || LPAD(n::text, 4, '0') || ' - ' ||
          (ARRAY['Arroz', 'Agua', 'Detergente', 'Galletas',
                 'Leche', 'Azúcar', 'Jabón', 'Fideos'])[1 + ((n - 1) % 8)]
          AS nombre,
       (1.50 + (n % 151) * 0.25)::NUMERIC(10,2) AS precio,
       (ARRAY['Demo Abarrotes','Demo Bebidas','Demo Limpieza','Demo Snacks'])
          [1 + ((n - 1) % 4)] AS categoria,
       (3 + n % 12) AS stock_minimo
FROM generate_series(1,1000) AS serie(n);

-- Solo insertar si el nombre DEMO no existía en la base.
INSERT INTO producto(nombre, precio, id_categoria, descripcion)
SELECT d.nombre, d.precio, c.id_categoria, 'DATOS SINTÉTICOS - NO REALES'
FROM rinconcito_demo_catalogo d
JOIN categoria c ON c.nombre=d.categoria
WHERE NOT EXISTS (SELECT 1 FROM producto p WHERE p.nombre=d.nombre);

-- Configurar stock mínimo para los 1,000 productos DEMO, sin duplicar inventario.
INSERT INTO inventario(id_producto,stock_minimo)
SELECT p.id_producto,d.stock_minimo
FROM rinconcito_demo_catalogo d
JOIN producto p ON p.nombre=d.nombre
WHERE NOT EXISTS (SELECT 1 FROM inventario i WHERE i.id_producto=p.id_producto);

COMMIT;

-- Verificación: el resultado esperado es 1000 (si la base no tenía nombres DEMO repetidos).
SELECT COUNT(*) AS productos_demo
FROM producto WHERE nombre LIKE 'DEMO-%';

SELECT COUNT(*) AS productos_demo_con_inventario
FROM producto p JOIN inventario i ON i.id_producto=p.id_producto
WHERE p.nombre LIKE 'DEMO-%';

-- IMPORTANTE: el stock inicial de los productos creados es 0.
-- Para generar existencias, usar el módulo real de RECEPCIÓN del sistema.
