package pe.utp.rinconcito.dao.jdbc;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import pe.utp.rinconcito.dao.OperationDao;
import java.math.BigDecimal;
import java.time.LocalDate;

@Repository
public class JdbcOperationDao implements OperationDao {
    private final JdbcTemplate jdbc;
    public JdbcOperationDao(JdbcTemplate jdbc){this.jdbc=jdbc;}
    @Override public long userId(String login){Long x=jdbc.queryForObject("SELECT id_usuario FROM usuario WHERE nombre_usuario=? AND estado=true",Long.class,login);if(x==null)throw new IllegalArgumentException("Cuenta deshabilitada");return x;}
    @Override public long createSale(String method,long userId){Long x=jdbc.queryForObject("INSERT INTO venta(tipo_pago,fecha_pago,id_usuario) VALUES (?,now(),?) RETURNING id_venta",Long.class,method,userId);return x;}
    @Override public void addSaleDetail(long saleId,long productId,int quantity,BigDecimal unitPrice){jdbc.update("INSERT INTO detalle_venta(id_venta,id_producto,cantidad,precio) VALUES (?,?,?,?)",saleId,productId,quantity,unitPrice);}
    @Override public long createReceipt(long supplierId,String guide,long userId){Long x=jdbc.queryForObject("INSERT INTO ingreso_mercaderia(id_proveedor,guia,id_usuario,estado) VALUES (?,?,?,'INGRESADO') RETURNING id_ingreso",Long.class,supplierId,guide,userId);return x;}
    @Override public void addReceiptDetail(long receiptId,long productId,int quantity,BigDecimal cost,LocalDate expiration){jdbc.update("INSERT INTO detalle_ingreso(id_ingreso,id_producto,cantidad,costo,fecha_vencimiento) VALUES (?,?,?,?,?)",receiptId,productId,quantity,cost,expiration);}
    @Override public boolean supplierActive(long supplierId){Boolean x=jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM proveedor WHERE id_proveedor=? AND estado=true)",Boolean.class,supplierId);return Boolean.TRUE.equals(x);}
}
