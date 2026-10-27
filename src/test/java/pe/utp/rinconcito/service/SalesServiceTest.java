package pe.utp.rinconcito.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pe.utp.rinconcito.dao.InventoryDao;
import pe.utp.rinconcito.dao.OperationDao;
import pe.utp.rinconcito.dao.ProductDao;
import pe.utp.rinconcito.domain.Product;
import pe.utp.rinconcito.domain.SaleLine;
import java.math.BigDecimal;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Pruebas unitarias TDD del flujo de venta; no sustituyen integración con PostgreSQL. */
class SalesServiceTest {
    private ProductDao products; private InventoryDao inventory; private OperationDao operations; private SalesService service;
    @BeforeEach void setup(){products=mock(ProductDao.class);inventory=mock(InventoryDao.class);operations=mock(OperationDao.class);service=new SalesService(products,inventory,operations);}
    @Test void cancelaSinStockYNoRegistraMovimientos(){
        when(products.activeById(7)).thenReturn(new Product(7,"Arroz",new BigDecimal("4.50"),"Abarrotes",true));
        when(inventory.available(7)).thenReturn(2L);
        assertThrows(IllegalArgumentException.class,()->service.confirm(List.of(new SaleLine(7,3)),"EFECTIVO","cajero"));
        verify(operations,never()).createSale(anyString(),anyLong());
        verify(inventory,never()).outgoing(anyLong(),anyInt(),anyLong(),anyLong());
    }
    @Test void registraVentaYUnaSalidaPorProducto(){
        when(products.activeById(7)).thenReturn(new Product(7,"Arroz",new BigDecimal("4.50"),"Abarrotes",true));
        when(inventory.available(7)).thenReturn(10L);
        when(operations.userId("cajero")).thenReturn(5L);when(operations.createSale("YAPE",5)).thenReturn(100L);
        long id=service.confirm(List.of(new SaleLine(7,2),new SaleLine(7,1)),"YAPE","cajero");
        assertEquals(100L,id);
        verify(inventory).lockProduct(7);
        verify(operations).addSaleDetail(100L,7L,3,new BigDecimal("4.50"));
        verify(inventory).outgoing(7L,3,100L,5L);
    }
    @Test void rechazaCantidadesYMediosInvalidos(){
        assertThrows(IllegalArgumentException.class,()->service.confirm(List.of(new SaleLine(1,0)),"EFECTIVO","x"));
        assertThrows(IllegalArgumentException.class,()->service.confirm(List.of(new SaleLine(1,1)),"BITCOIN","x"));
    }
}
