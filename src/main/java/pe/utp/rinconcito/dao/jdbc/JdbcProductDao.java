package pe.utp.rinconcito.dao.jdbc;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import pe.utp.rinconcito.dao.ProductDao;
import pe.utp.rinconcito.domain.Product;
import java.math.BigDecimal;
import java.util.List;

@Repository
public class JdbcProductDao implements ProductDao {
    private final JdbcTemplate jdbc;
    public JdbcProductDao(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    @Override public List<Product> all() {
        return jdbc.query("SELECT p.id_producto,p.nombre,p.precio,c.nombre AS categoria,p.estado FROM producto p JOIN categoria c USING(id_categoria) ORDER BY p.nombre", (rs,n)->new Product(rs.getLong(1),rs.getString(2),rs.getBigDecimal(3),rs.getString(4),rs.getBoolean(5)));
    }
    @Override public Product activeById(long id) {
        return jdbc.query("SELECT p.id_producto,p.nombre,p.precio,c.nombre AS categoria,p.estado FROM producto p JOIN categoria c USING(id_categoria) WHERE p.id_producto=? AND p.estado=true",rs->{
            if(!rs.next()) throw new IllegalArgumentException("Producto no disponible: " + id);
            return new Product(rs.getLong(1),rs.getString(2),rs.getBigDecimal(3),rs.getString(4),rs.getBoolean(5));
        },id);
    }
    @Override public long create(String name,BigDecimal price,long categoryId) {
        Long id=jdbc.queryForObject("INSERT INTO producto(nombre,precio,id_categoria) VALUES (?,?,?) RETURNING id_producto",Long.class,name,price,categoryId);
        if(id==null)throw new IllegalStateException("No se generó ID de producto");
        return id;
    }
    @Override public void update(long id,String name,BigDecimal price) {
        if(jdbc.update("UPDATE producto SET nombre=?,precio=? WHERE id_producto=? AND estado=true",name,price,id)!=1) throw new IllegalArgumentException("Producto no encontrado");
    }
    @Override public void deactivate(long id) {jdbc.update("UPDATE producto SET estado=false WHERE id_producto=?",id);}
    @Override public void createCategory(String name) {jdbc.update("INSERT INTO categoria(nombre) VALUES (?)",name);}
    @Override public List<Category> categories(){return jdbc.query("SELECT id_categoria,nombre FROM categoria ORDER BY nombre",(rs,n)->new Category(rs.getLong(1),rs.getString(2)));}
}
