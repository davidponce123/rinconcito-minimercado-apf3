package pe.utp.rinconcito.dao;

import pe.utp.rinconcito.domain.Product;
import java.math.BigDecimal;
import java.util.List;

/** Contrato de persistencia del catálogo (DAO). */
public interface ProductDao {
    List<Product> all();
    List<Product> page(String search, int limit, int offset);
    long countMatching(String search);
    Product activeById(long id);
    long create(String name, BigDecimal price, long categoryId);
    void update(long id, String name, BigDecimal price);
    void deactivate(long id);
    void reactivate(long id);
    void createCategory(String name);
    List<Category> categories();

    /** @return true si existe una categoría con ese id. */
    boolean categoryExists(long categoryId);

    /** @return true si ya hay una categoría con ese nombre (sin distinguir mayúsculas). */
    boolean categoryNameExists(String name);

    /**
     * @param excludeId id del producto que se está editando (0 si es un alta nueva)
     * @return true si otro producto ACTIVO ya usa ese nombre (sin distinguir mayúsculas).
     */
    boolean activeProductNameExists(String name, long excludeId);

    record Category(long id, String name) {}
}
