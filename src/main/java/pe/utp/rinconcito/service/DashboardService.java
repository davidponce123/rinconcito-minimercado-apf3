
package pe.utp.rinconcito.service;

import java.util.List;

import org.springframework.stereotype.Service;

import pe.utp.rinconcito.dao.DashboardDao;
import pe.utp.rinconcito.dao.InventoryDao;
import pe.utp.rinconcito.domain.Stock;

@Service
public class DashboardService {

    private final DashboardDao dashboardDao;
    private final InventoryDao inventoryDao;

    public DashboardService(
            DashboardDao dashboardDao,
            InventoryDao inventoryDao) {

        this.dashboardDao = dashboardDao;
        this.inventoryDao = inventoryDao;
    }

    public int activeProducts() {
        return dashboardDao.activeProducts();
    }

    public int todaySales() {
        return dashboardDao.todaySales();
    }

    public List<Stock> lowStock() {
        return inventoryDao.stock()
                .stream()
                .filter(Stock::needsReorder)
                .limit(8)
                .toList();
    }
}
