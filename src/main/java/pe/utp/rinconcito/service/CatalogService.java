package pe.utp.rinconcito.service;

import com.google.common.base.Preconditions;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.utp.rinconcito.dao.InventoryDao;
import pe.utp.rinconcito.dao.ProductDao;
import java.math.BigDecimal;

/**
 * RF07: lógica de negocio de gestión de catálogo; no expone SQL a la interfaz.
 *
 * Reglas de validación del catálogo:
 *  - Nombre de producto: obligatorio, máx. 80 caracteres, sin espacios repetidos y único entre productos activos.
 *  - Precio: mayor que 0, máx. 2 decimales y no mayor a S/ 99,999,999.99 (límite de NUMERIC(10,2)).
 *  - Categoría: debe existir. Nombre obligatorio, máx. 50 caracteres y único sin distinguir mayúsculas.
 *  - Stock mínimo: entre 0 y 100,000.
 */
@Service
public class CatalogService {
    static final BigDecimal MAX_PRICE = new BigDecimal("99999999.99");
    static final long MAX_MIN_STOCK = 100_000;

    private final ProductDao products;
    private final InventoryDao inventory;

    public CatalogService(ProductDao products, InventoryDao inventory) {
        this.products = products;
        this.inventory = inventory;
    }

    @Transactional
    public long createProduct(String name, BigDecimal price, long category, long minStock) {
        String cleanName = validate(name, price);
        Preconditions.checkArgument(category > 0, "Categoría inválida");
        Preconditions.checkArgument(minStock >= 0 && minStock <= MAX_MIN_STOCK,
                "El stock mínimo debe estar entre 0 y " + MAX_MIN_STOCK);
        Preconditions.checkArgument(products.categoryExists(category), "La categoría seleccionada no existe");
        Preconditions.checkArgument(!products.activeProductNameExists(cleanName, 0),
                "Ya existe un producto activo con ese nombre");
        long id = products.create(cleanName, price, category);
        inventory.setup(id, minStock);
        return id;
    }

    @Transactional
    public void changeProduct(long id, String name, BigDecimal price) {
        Preconditions.checkArgument(id > 0, "Producto inválido");
        String cleanName = validate(name, price);
        Preconditions.checkArgument(!products.activeProductNameExists(cleanName, id),
                "Ya existe otro producto activo con ese nombre");
        products.update(id, cleanName, price);
    }

    @Transactional
    public void deactivate(long id) {
        Preconditions.checkArgument(id > 0, "Producto inválido");
        products.deactivate(id);
    }

    @Transactional
    public void addCategory(String name) {
        String cleanName = StringUtils.normalizeSpace(name);
        Preconditions.checkArgument(StringUtils.isNotBlank(cleanName) && cleanName.length() <= 50,
                "Nombre de categoría inválido (obligatorio, máx. 50 caracteres)");
        Preconditions.checkArgument(!products.categoryNameExists(cleanName), "Ya existe una categoría con ese nombre");
        products.createCategory(cleanName);
    }

    /** Valida nombre y precio; devuelve el nombre normalizado (sin espacios sobrantes). */
    private static String validate(String name, BigDecimal price) {
        String cleanName = StringUtils.normalizeSpace(name);
        Preconditions.checkArgument(StringUtils.isNotBlank(cleanName) && cleanName.length() <= 80,
                "Nombre inválido (obligatorio, máx. 80 caracteres)");
        Preconditions.checkArgument(price != null, "El precio es obligatorio");
        Preconditions.checkArgument(price.signum() > 0, "El precio debe ser mayor que cero");
        Preconditions.checkArgument(price.stripTrailingZeros().scale() <= 2, "El precio admite como máximo 2 decimales");
        Preconditions.checkArgument(price.compareTo(MAX_PRICE) <= 0, "El precio excede el máximo permitido");
        return cleanName;
    }
}
