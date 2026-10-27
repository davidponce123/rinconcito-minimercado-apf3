package pe.utp.rinconcito.dao;

import pe.utp.rinconcito.domain.Stock;
import java.util.List;

/** Operaciones de inventario. El bloqueo evita sobreventa concurrente. */
public interface InventoryDao {
    void lockProduct(long productId);
    long available(long productId);
    List<Stock> stock();
    void setup(long productId, long minStock);
    void incoming(long productId, int quantity, long receiptId, long userId);
    void outgoing(long productId, int quantity, long saleId, long userId);
}
