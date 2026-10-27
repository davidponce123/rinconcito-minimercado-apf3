package pe.utp.rinconcito.dao;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Persistencia de documentos; se ejecuta bajo transacciones Spring. */
public interface OperationDao {
    long userId(String login);
    long createSale(String method, long userId);
    void addSaleDetail(long saleId, long productId, int quantity, BigDecimal unitPrice);
    long createReceipt(long supplierId, String guide, long userId);
    void addReceiptDetail(long receiptId, long productId, int quantity, BigDecimal cost, LocalDate expiration);
    boolean supplierActive(long supplierId);
}
