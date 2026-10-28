-- OPCIONAL: ingreso simulado de 60 productos DEMO para ver stock en reportes.
-- Ejecutar SOLO DESPUÉS de crear un ADMIN desde Spring Boot y cargar los 1000 DEMO.
-- No altera registros de venta ni modifica existencias directamente:
-- registra ingreso + detalle + movimientos legítimos.
-- Esta guía simulada solo puede insertarse una vez por proveedor.
-- SOLO BASE LOCAL DE PRUEBAS, nunca datos de la tienda.

BEGIN;
DO $$
DECLARE
    v_usuario INTEGER;
    v_proveedor INTEGER;
    v_ingreso BIGINT;
BEGIN
    SELECT id_usuario INTO v_usuario FROM usuario
    WHERE estado=true AND id_rol IN (SELECT id_rol FROM rol WHERE nombre='ADMIN')
    ORDER BY id_usuario LIMIT 1;
    IF v_usuario IS NULL THEN
        RAISE EXCEPTION 'Primero inicia Spring Boot y crea una cuenta ADMIN activa.';
    END IF;

    SELECT id_proveedor INTO v_proveedor FROM proveedor
    WHERE nombre='PROVEEDOR DEMOSTRACION - NO REAL' ORDER BY id_proveedor LIMIT 1;
    IF v_proveedor IS NULL THEN
        INSERT INTO proveedor(nombre) VALUES('PROVEEDOR DEMOSTRACION - NO REAL')
        RETURNING id_proveedor INTO v_proveedor;
    END IF;

    IF EXISTS(SELECT 1 FROM ingreso_mercaderia
              WHERE id_proveedor=v_proveedor AND guia='GUIA-DEMO-1000') THEN
        RAISE NOTICE 'Ya existe la recepción de prueba: no se duplicaron movimientos.';
        RETURN;
    END IF;

    INSERT INTO ingreso_mercaderia(guia,id_proveedor,id_usuario,estado)
    VALUES('GUIA-DEMO-1000',v_proveedor,v_usuario,'INGRESADO')
    RETURNING id_ingreso INTO v_ingreso;

    INSERT INTO detalle_ingreso(id_ingreso,id_producto,cantidad,costo,fecha_vencimiento)
    SELECT v_ingreso,p.id_producto,10+(n.numero%20),2.50,NULL
    FROM producto p
    JOIN (SELECT id_producto, ROW_NUMBER() OVER(ORDER BY nombre) AS numero
          FROM producto WHERE nombre LIKE 'DEMO-%') n USING(id_producto)
    WHERE n.numero <= 60;

    INSERT INTO movimiento_inventario(tipo,cantidad,id_producto,id_usuario,id_ingreso)
    SELECT 'ENTRADA',di.cantidad,di.id_producto,v_usuario,v_ingreso
    FROM detalle_ingreso di WHERE di.id_ingreso=v_ingreso;
END $$;
COMMIT;

SELECT tipo, COUNT(*) AS movimientos_demo
FROM movimiento_inventario WHERE id_ingreso IN
 (SELECT id_ingreso FROM ingreso_mercaderia WHERE guia='GUIA-DEMO-1000')
GROUP BY tipo;
