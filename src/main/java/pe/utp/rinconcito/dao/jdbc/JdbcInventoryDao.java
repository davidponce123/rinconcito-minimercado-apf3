package pe.utp.rinconcito.dao.jdbc;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import pe.utp.rinconcito.dao.InventoryDao;
import pe.utp.rinconcito.domain.Stock;
import java.util.List;

@Repository
public class JdbcInventoryDao implements InventoryDao {
    private final JdbcTemplate jdbc;
    public JdbcInventoryDao(JdbcTemplate jdbc){this.jdbc=jdbc;}
    @Override public void lockProduct(long productId){
        Long locked=jdbc.query("SELECT id_producto FROM inventario WHERE id_producto=? FOR UPDATE",rs->rs.next()?rs.getLong(1):null,productId);
        if(locked==null)throw new IllegalArgumentException("Producto sin configuración de inventario: "+productId);
    }
    @Override public long available(long productId){
        Long result=jdbc.queryForObject("SELECT stock_actual FROM v_stock WHERE id_producto=?",Long.class,productId);
        if(result==null) throw new IllegalArgumentException("Stock inexistente");
        return result;
    }
    @Override public List<Stock> stock(){return jdbc.query("SELECT id_producto,nombre,stock_actual,stock_minimo FROM v_stock ORDER BY nombre",(rs,n)->new Stock(rs.getLong(1),rs.getString(2),rs.getLong(3),rs.getLong(4)));}
    @Override public void setup(long productId,long minimum){jdbc.update("INSERT INTO inventario(id_producto,stock_minimo) VALUES (?,?)",productId,minimum);}
    @Override public void incoming(long productId,int quantity,long receiptId,long userId){jdbc.update("INSERT INTO movimiento_inventario(tipo,cantidad,id_producto,id_usuario,id_ingreso) VALUES ('ENTRADA',?,?,?,?)",quantity,productId,userId,receiptId);}
    @Override public void outgoing(long productId,int quantity,long saleId,long userId){jdbc.update("INSERT INTO movimiento_inventario(tipo,cantidad,id_producto,id_usuario,id_venta) VALUES ('SALIDA',?,?,?,?)",quantity,productId,userId,saleId);}
}
