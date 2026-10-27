package pe.utp.rinconcito.service;

import com.google.common.base.Preconditions;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.utp.rinconcito.dao.InventoryDao;
import pe.utp.rinconcito.dao.OperationDao;
import pe.utp.rinconcito.dao.ProductDao;
import pe.utp.rinconcito.domain.ReceiptLine;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** RF06: recibe lotes, conserva vencimientos y contabiliza una entrada por producto. */
@Service
public class ReceivingService {
    private static final Logger log=LoggerFactory.getLogger(ReceivingService.class);
    // detalle_ingreso.costo is NUMERIC(10,2): never silently round a received lot's historical cost.
    private static final BigDecimal MAX_COST=new BigDecimal("99999999.99");
    private final ProductDao products;
    private final InventoryDao inventory;
    private final OperationDao operations;
    public ReceivingService(ProductDao products,InventoryDao inventory,OperationDao operations){this.products=products;this.inventory=inventory;this.operations=operations;}
    @Transactional
    public long register(long supplierId,String guide,List<ReceiptLine> lines,String login){
        Preconditions.checkArgument(supplierId>0,"Proveedor inválido");
        Preconditions.checkArgument(StringUtils.isNotBlank(guide) && guide.trim().length()<=30,"Guía obligatoria (máximo 30 caracteres)");
        Preconditions.checkArgument(lines!=null && !lines.isEmpty(),"La recepción necesita al menos un lote");
        if(!operations.supplierActive(supplierId))throw new IllegalArgumentException("Proveedor no disponible");
        Map<Long,Integer> totals=new TreeMap<>();
        LocalDate receivedOn=LocalDate.now();
        for(ReceiptLine line:lines){
            Preconditions.checkArgument(line!=null,"Lote incompleto");
            Preconditions.checkArgument(line.productId()>0 && line.quantity()>0,"Producto y cantidad inválidos");
            Preconditions.checkArgument(line.cost()!=null && line.cost().signum()>=0,"Costo inválido");
            Preconditions.checkArgument(line.cost().compareTo(MAX_COST)<=0 && line.cost().stripTrailingZeros().scale()<=2,
                    "El costo debe estar entre 0 y 99999999.99 y tener como máximo dos decimales");
            Preconditions.checkArgument(line.expiration()==null || !line.expiration().isBefore(receivedOn),"Lote vencido; registrar incidencia por separado");
            totals.merge(line.productId(),line.quantity(),Math::addExact);
        }
        for(Long id:totals.keySet()){inventory.lockProduct(id);products.activeById(id);}
        long userId=operations.userId(login);
        long receiptId=operations.createReceipt(supplierId,guide.trim(),userId);
        for(ReceiptLine line:lines)operations.addReceiptDetail(receiptId,line.productId(),line.quantity(),line.cost(),line.expiration());
        for(Map.Entry<Long,Integer> e:totals.entrySet())inventory.incoming(e.getKey(),e.getValue(),receiptId,userId);
        log.info("Recepción confirmada: id={} productos={}",receiptId,totals.size());
        return receiptId;
    }
}
