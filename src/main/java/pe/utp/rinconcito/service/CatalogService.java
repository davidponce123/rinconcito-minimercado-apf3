package pe.utp.rinconcito.service;

import com.google.common.base.Preconditions;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.utp.rinconcito.dao.InventoryDao;
import pe.utp.rinconcito.dao.ProductDao;
import java.math.BigDecimal;

/** RF07: lógica de negocio de gestión de catálogo; no expone SQL a la interfaz. */
@Service
public class CatalogService {
    private final ProductDao products;
    private final InventoryDao inventory;
    public CatalogService(ProductDao products,InventoryDao inventory){this.products=products;this.inventory=inventory;}
    @Transactional public long createProduct(String name,BigDecimal price,long category,long minStock){
        validate(name,price);Preconditions.checkArgument(category>0 && minStock>=0,"Categoría o mínimo inválido");
        long id=products.create(name.trim(),price,category);
        inventory.setup(id,minStock);
        return id;
    }
    @Transactional public void changeProduct(long id,String name,BigDecimal price){validate(name,price);products.update(id,name.trim(),price);}
    @Transactional public void deactivate(long id){Preconditions.checkArgument(id>0,"Producto inválido");products.deactivate(id);}
    @Transactional public void addCategory(String name){Preconditions.checkArgument(StringUtils.isNotBlank(name) && name.trim().length()<=50,"Nombre de categoría inválido");products.createCategory(name.trim());}
    private static void validate(String name,BigDecimal price){
        Preconditions.checkArgument(StringUtils.isNotBlank(name) && name.trim().length()<=80,"Nombre inválido");
        Preconditions.checkArgument(price!=null && price.signum()>=0 && price.scale()<=2,"Precio inválido");
    }
}
