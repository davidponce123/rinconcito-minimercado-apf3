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
    /** Busca productos en el catÃ¡logo sin descargar mil filas en el navegador. */
    @Override public List<Product> page(String search,int limit,int offset){
        String pattern = pattern(search);
        return jdbc.query("SELECT p.id_producto,p.nombre,p.precio,c.nombre AS categoria,p.estado " +
                  "FROM producto p JOIN categoria c USING(id_categoria) " +
                  "WHERE p.nombre ILIKE ? ESCAPE '!' ORDER BY p.nombre LIMIT ? OFFSET ?",
                (rs,n)->new Product(rs.getLong(1),rs.getString(2),rs.getBigDecimal(3),rs.getString(4),rs.getBoolean(5)),
                pattern,limit,offset);
    }
    @Override public long countMatching(String search){
        Long n=jdbc.queryForObject("SELECT COUNT(*) FROM producto WHERE nombre ILIKE ? ESCAPE '!'",
                Long.class,pattern(search));
        return n==null?0:n;
    }
    private static String pattern(String search){
        String q=search==null?"":search.trim();
        return "%"+q.replace("!","!!").replace("%","!%").replace("_","!_")+"%";
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
    @Override public void deactivate(long id) {
        if(jdbc.update("UPDATE producto SET estado=false WHERE id_producto=?",id)!=1) throw new IllegalArgumentException("Producto no encontrado");
    }
    @Override public void reactivate(long id) {
        int changed=jdbc.update("""
            UPDATE producto p SET estado=true
            WHERE p.id_producto=? AND p.estado=false
              AND NOT EXISTS (
                SELECT 1 FROM producto otro
                WHERE otro.estado=true AND otro.id_producto<>p.id_producto
                  AND lower(otro.nombre)=lower(p.nombre)
              )
            """,id);
        if(changed!=1)throw new IllegalArgumentException(
                "No se puede reactivar: producto inexistente, ya activo o nombre duplicado con otro activo");
    }
    @Override public void createCategory(String name) {jdbc.update("INSERT INTO categoria(nombre) VALUES (?)",name);}
    @Override public List<Category> categories(){return jdbc.query("SELECT id_categoria,nombre FROM categoria ORDER BY nombre",(rs,n)->new Category(rs.getLong(1),rs.getString(2)));}
    @Override public boolean categoryExists(long categoryId){
        return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM categoria WHERE id_categoria=?)",Boolean.class,categoryId));
    }
    @Override public boolean categoryNameExists(String name){
        return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM categoria WHERE lower(nombre)=lower(?))",Boolean.class,name));
    }
    @Override public boolean activeProductNameExists(String name,long excludeId){
        return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM producto WHERE lower(nombre)=lower(?) AND estado=true AND id_producto<>?)",Boolean.class,name,excludeId));
    }
}
