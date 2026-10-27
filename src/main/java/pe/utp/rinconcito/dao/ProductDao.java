package pe.utp.rinconcito.dao;

import pe.utp.rinconcito.domain.Product;
import java.math.BigDecimal;
import java.util.List;

/** Contrato de persistencia del catálogo (DAO). */
public interface ProductDao {
    List<Product> all();
    Product activeById(long id);
    long create(String name, BigDecimal price, long categoryId);
    void update(long id, String name, BigDecimal price);
    void deactivate(long id);
    void createCategory(String name);
    List<Category> categories();
    record Category(long id, String name) {}
}
