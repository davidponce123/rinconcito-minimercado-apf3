package pe.utp.rinconcito.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import pe.utp.rinconcito.dao.InventoryDao;
import pe.utp.rinconcito.dao.OperationDao;
import pe.utp.rinconcito.dao.ProductDao;
import pe.utp.rinconcito.domain.Product;
import pe.utp.rinconcito.domain.ReceiptLine;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReceivingServiceTest {
    private ProductDao products;
    private InventoryDao inventory;
    private OperationDao operations;
    private ReceivingService service;

    @BeforeEach void setup(){
        products=mock(ProductDao.class);inventory=mock(InventoryDao.class);operations=mock(OperationDao.class);
        service=new ReceivingService(products,inventory,operations);
    }

    @Test void conservaDosLotesYRegistraUnaEntradaConsolidada(){
        ProductDao products=mock(ProductDao.class);InventoryDao inventory=mock(InventoryDao.class);OperationDao operations=mock(OperationDao.class);
        when(operations.supplierActive(4)).thenReturn(true);
        when(products.activeById(8)).thenReturn(new Product(8,"Leche",new BigDecimal("5.60"),"Lácteos",true));
        when(operations.userId("admin")).thenReturn(1L);when(operations.createReceipt(4,"G-01",1)).thenReturn(18L);
        ReceivingService service=new ReceivingService(products,inventory,operations);
        var a=new ReceiptLine(8,3,new BigDecimal("3.50"),LocalDate.now().plusMonths(3));
        var b=new ReceiptLine(8,4,new BigDecimal("3.40"),LocalDate.now().plusMonths(6));
        assertEquals(18L,service.register(4,"G-01",List.of(a,b),"admin"));
        verify(operations,times(2)).addReceiptDetail(eq(18L),eq(8L),anyInt(),any(),any());
        verify(inventory,times(1)).incoming(8L,7,18L,1L);
    }
    @Test void rechazaProveedorDeshabilitado(){
        ProductDao p=mock(ProductDao.class);InventoryDao i=mock(InventoryDao.class);OperationDao o=mock(OperationDao.class);
        assertThrows(IllegalArgumentException.class,()->new ReceivingService(p,i,o).register(9,"G",List.of(new ReceiptLine(1,1,BigDecimal.ONE,null)),"admin"));
        verify(o,never()).createReceipt(anyLong(),anyString(),anyLong());
    }

    static java.util.stream.Stream<ReceiptLine> invalidLots(){
        return java.util.stream.Stream.of(null,
                new ReceiptLine(0,1,BigDecimal.ONE,null),
                new ReceiptLine(1,0,BigDecimal.ONE,null),
                new ReceiptLine(1,-1,BigDecimal.ONE,null),
                new ReceiptLine(1,1,null,null),
                new ReceiptLine(1,1,new BigDecimal("-0.01"),null),
                new ReceiptLine(1,1,BigDecimal.ONE,LocalDate.now().minusDays(1)));
    }

    @ParameterizedTest @MethodSource("invalidLots")
    void rechazaLotesInvalidosAntesDeBloquearOEscribir(ReceiptLine line){
        when(operations.supplierActive(4)).thenReturn(true);
        assertThrows(IllegalArgumentException.class,()->service.register(4,"G",java.util.Arrays.asList(line),"admin"));
        verifyNoInteractions(inventory,products);
        verify(operations,never()).createReceipt(anyLong(),anyString(),anyLong());
        verify(operations,never()).addReceiptDetail(anyLong(),anyLong(),anyInt(),any(),any());
    }

    @Test void rechazaCabecerasInvalidasSinConsultarLaBase(){
        var line=new ReceiptLine(1,1,BigDecimal.ONE,null);
        assertThrows(IllegalArgumentException.class,()->service.register(0,"G",List.of(line),"admin"));
        assertThrows(IllegalArgumentException.class,()->service.register(4,"  ",List.of(line),"admin"));
        assertThrows(IllegalArgumentException.class,()->service.register(4,"G".repeat(31),List.of(line),"admin"));
        assertThrows(IllegalArgumentException.class,()->service.register(4,"G",null,"admin"));
        assertThrows(IllegalArgumentException.class,()->service.register(4,"G",List.of(),"admin"));
        verifyNoInteractions(products,inventory,operations);
    }

    @Test void rechazaDesbordamientoSinEscribir(){
        when(operations.supplierActive(4)).thenReturn(true);
        var first=new ReceiptLine(1,Integer.MAX_VALUE,BigDecimal.ZERO,null);
        var second=new ReceiptLine(1,1,BigDecimal.ZERO,null);
        assertThrows(ArithmeticException.class,()->service.register(4,"G",List.of(first,second),"admin"));
        verifyNoInteractions(inventory,products);
        verify(operations,never()).createReceipt(anyLong(),anyString(),anyLong());
    }

    @Test void permiteVencimientoHoyOCampoOpcionalYBloqueaEnOrden(){
        when(operations.supplierActive(4)).thenReturn(true);
        when(operations.userId("admin")).thenReturn(1L);
        when(operations.createReceipt(4,"G",1)).thenReturn(18L);
        var today=new ReceiptLine(8,3,BigDecimal.ZERO,LocalDate.now());
        var optional=new ReceiptLine(2,4,BigDecimal.ONE,null);
        assertEquals(18,service.register(4," G ",List.of(today,optional),"admin"));
        var order=inOrder(inventory,operations);
        order.verify(inventory).lockProduct(2);
        order.verify(inventory).lockProduct(8);
        order.verify(operations).userId("admin");
        order.verify(operations).createReceipt(4,"G",1);
        verify(operations).addReceiptDetail(18,8,3,BigDecimal.ZERO,today.expiration());
        verify(operations).addReceiptDetail(18,2,4,BigDecimal.ONE,null);
        verify(inventory).incoming(2,4,18,1);
        verify(inventory).incoming(8,3,18,1);
    }

    @Test void productoNoDisponibleNoGeneraCabecera(){
        when(operations.supplierActive(4)).thenReturn(true);
        when(products.activeById(8)).thenThrow(new IllegalArgumentException("Producto no disponible"));
        assertThrows(IllegalArgumentException.class,()->service.register(4,"G",List.of(new ReceiptLine(8,1,BigDecimal.ONE,null)),"admin"));
        verify(operations,never()).createReceipt(anyLong(),anyString(),anyLong());
        verify(inventory,never()).incoming(anyLong(),anyInt(),anyLong(),anyLong());
    }
}
