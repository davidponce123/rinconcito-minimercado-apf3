package pe.utp.rinconcito.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import pe.utp.rinconcito.dao.InventoryDao;
import pe.utp.rinconcito.dao.OperationDao;
import pe.utp.rinconcito.dao.ProductDao;
import pe.utp.rinconcito.domain.ReceiptLine;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReceivingCostTest {
    private ProductDao products;
    private InventoryDao inventory;
    private OperationDao operations;
    private ReceivingService service;

    @BeforeEach void setup(){
        products=mock(ProductDao.class);
        inventory=mock(InventoryDao.class);
        operations=mock(OperationDao.class);
        when(operations.supplierActive(4)).thenReturn(true);
        service=new ReceivingService(products,inventory,operations);
    }

    @ParameterizedTest
    @ValueSource(strings={"0.001","3.505","100000000","1E+100"})
    void rejectsCostsThatCannotBeStoredExactly(String value){
        var lot=new ReceiptLine(8,3,new BigDecimal(value),null);
        var error=assertThrows(IllegalArgumentException.class,
                ()->service.register(4,"G-COST",List.of(lot),"admin"));
        assertEquals("El costo debe estar entre 0 y 99999999.99 y tener como máximo dos decimales",error.getMessage());
        verifyNoInteractions(inventory,products);
        verify(operations,never()).createReceipt(anyLong(),anyString(),anyLong());
    }

    @ParameterizedTest
    @ValueSource(strings={"0.0000","3.40","3.4000","99999999.99","1E+7"})
    void acceptsExactAmountsIncludingInsignificantZeros(String value){
        var cost=new BigDecimal(value);
        when(operations.userId("admin")).thenReturn(1L);
        when(operations.createReceipt(4,"G-COST",1)).thenReturn(18L);
        assertEquals(18L,service.register(4,"G-COST",List.of(new ReceiptLine(8,3,cost,null)),"admin"));
        verify(operations).addReceiptDetail(18,8,3,cost,null);
        verify(inventory).incoming(8,3,18,1);
    }

    @Test void invalidCostInLaterLotRejectsTheEntireReceiptBeforeWriting(){
        var first=new ReceiptLine(8,4,new BigDecimal("2.50"),null);
        var invalid=new ReceiptLine(8,6,new BigDecimal("2.505"),null);
        assertThrows(IllegalArgumentException.class,
                ()->service.register(4,"G-BATCH",List.of(first,invalid),"admin"));
        verifyNoInteractions(inventory,products);
        verify(operations,never()).createReceipt(anyLong(),anyString(),anyLong());
        verify(operations,never()).addReceiptDetail(anyLong(),anyLong(),anyInt(),any(),any());
    }
}
