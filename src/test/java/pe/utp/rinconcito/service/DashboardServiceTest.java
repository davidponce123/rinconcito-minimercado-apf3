
package pe.utp.rinconcito.service;

import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import pe.utp.rinconcito.dao.DashboardDao;
import pe.utp.rinconcito.dao.InventoryDao;
import pe.utp.rinconcito.domain.Stock;

class DashboardServiceTest {

    @Test
    void muestraResumenYProductosParaReponer() {

        DashboardDao dashboardDao = mock(DashboardDao.class);
        InventoryDao inventoryDao = mock(InventoryDao.class);

        when(dashboardDao.activeProducts()).thenReturn(12);
        when(dashboardDao.todaySales()).thenReturn(4);

        Stock arroz = new Stock(1L, "Arroz", 3, 5);
        Stock leche = new Stock(2L, "Leche", 12, 4);

        when(inventoryDao.stock())
            .thenReturn(List.of(arroz, leche));

        DashboardService service =
            new DashboardService(dashboardDao, inventoryDao);

        assertEquals(12, service.activeProducts());
        assertEquals(4, service.todaySales());
        assertEquals(List.of(arroz), service.lowStock());
    }

    @Test
    void muestraComoMaximoOchoAlertas() {

        DashboardDao dashboardDao = mock(DashboardDao.class);
        InventoryDao inventoryDao = mock(InventoryDao.class);

        List<Stock> productos = IntStream.rangeClosed(1, 10)
            .mapToObj(i -> new Stock(i, "Producto " + i, 0, 5))
            .toList();

        when(inventoryDao.stock()).thenReturn(productos);

        DashboardService service =
            new DashboardService(dashboardDao, inventoryDao);

        assertEquals(8, service.lowStock().size());
    }
}
