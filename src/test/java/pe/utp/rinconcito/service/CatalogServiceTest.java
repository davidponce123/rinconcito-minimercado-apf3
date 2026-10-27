package pe.utp.rinconcito.service;

import org.junit.jupiter.api.Test;
import pe.utp.rinconcito.dao.InventoryDao;
import pe.utp.rinconcito.dao.ProductDao;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CatalogServiceTest {
    @Test void registraProductoYConfiguracionInventario(){
        ProductDao p=mock(ProductDao.class);InventoryDao i=mock(InventoryDao.class);
        when(p.create("Arroz",new BigDecimal("4.50"),2L)).thenReturn(17L);
        assertEquals(17L,new CatalogService(p,i).createProduct("Arroz",new BigDecimal("4.50"),2,5));
        verify(i).setup(17L,5L);
    }
    @Test void rechazaPrecioNegativo(){
        assertThrows(IllegalArgumentException.class,()->new CatalogService(mock(ProductDao.class),mock(InventoryDao.class)).createProduct("Arroz",new BigDecimal("-1"),2,0));
    }
}
