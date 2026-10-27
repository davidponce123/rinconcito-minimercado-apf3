package pe.utp.rinconcito.service;

import com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.utp.rinconcito.dao.InventoryDao;
import pe.utp.rinconcito.dao.OperationDao;
import pe.utp.rinconcito.dao.ProductDao;
import pe.utp.rinconcito.domain.SaleLine;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/** RF02/RF03: registra venta pagada, detalles y movimientos en una transacción atómica. */
@Service
public class SalesService {
    private static final Logger log=LoggerFactory.getLogger(SalesService.class);
    private static final Set<String> METHODS=Set.of("EFECTIVO","YAPE","PLIN","TARJETA");
    private final ProductDao products;
    private final InventoryDao inventory;
    private final OperationDao operations;
    public SalesService(ProductDao products,InventoryDao inventory,OperationDao operations){this.products=products;this.inventory=inventory;this.operations=operations;}

    @Transactional
    public long confirm(List<SaleLine> items,String method,String login){
        Preconditions.checkArgument(items!=null && !items.isEmpty(),"La venta debe incluir productos");
        Preconditions.checkArgument(METHODS.contains(method),"Forma de pago no permitida");
        Map<Long,Integer> merged=new TreeMap<>();
        for(SaleLine item:items){
            Preconditions.checkArgument(item.productId()>0 && item.quantity()>0,"Productos y cantidades deben ser positivos");
            merged.merge(item.productId(),item.quantity(),Math::addExact);
        }
        // Orden fijo de bloqueos: previene sobreventa y reduce interbloqueos entre cajas.
        for(Long productId:merged.keySet())inventory.lockProduct(productId);
        for(Map.Entry<Long,Integer> e:merged.entrySet()){
            products.activeById(e.getKey());
            if(inventory.available(e.getKey())<e.getValue())throw new IllegalArgumentException("Stock insuficiente para el producto "+e.getKey());
        }
        long userId=operations.userId(login);
        long saleId=operations.createSale(method,userId);
        for(Map.Entry<Long,Integer> e:merged.entrySet()){
            var price=products.activeById(e.getKey()).price();
            operations.addSaleDetail(saleId,e.getKey(),e.getValue(),price);
            inventory.outgoing(e.getKey(),e.getValue(),saleId,userId);
        }
        log.info("Venta confirmada: id={} lineas={}",saleId,merged.size());
        return saleId;
    }
}
