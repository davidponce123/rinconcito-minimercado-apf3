
package pe.utp.rinconcito.domain;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class StockTest {

    @Test
    void alertaCuandoStockEsMenorAlMinimo() {
        Stock stock = new Stock(1L, "Arroz", 3, 5);

        assertTrue(stock.needsReorder());
    }

    @Test
    void alertaCuandoStockEsIgualAlMinimo() {
        Stock stock = new Stock(2L, "Azucar", 5, 5);

        assertTrue(stock.needsReorder());
    }

    @Test
    void noAlertaCuandoStockEsMayorAlMinimo() {
        Stock stock = new Stock(3L, "Leche", 10, 5);

        assertFalse(stock.needsReorder());
    }
}
