package pe.utp.rinconcito.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pe.utp.rinconcito.dao.InventoryDao;
import pe.utp.rinconcito.dao.ProductDao;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/** Pruebas independientes para no modificar CatalogServiceTest del equipo. */
class CatalogReactivationTest {
    private ProductDao products;
    private CatalogService service;

    @BeforeEach
    void setup() {
        products = mock(ProductDao.class);
        service = new CatalogService(products, mock(InventoryDao.class));
    }

    @Test
    void reactivarProductoValidoLlamaAlDao() {
        service.reactivate(8L);
        verify(products).reactivate(8L);
    }

    @Test
    void noAceptaIdentificadoresInvalidos() {
        assertThrows(IllegalArgumentException.class, () -> service.reactivate(0L));
        assertThrows(IllegalArgumentException.class, () -> service.reactivate(-1L));
        verify(products, never()).reactivate(anyLong());
    }
}
