
package pe.utp.rinconcito.dao.jdbc;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import pe.utp.rinconcito.dao.DashboardDao;

@Repository
public class JdbcDashboardDao implements DashboardDao {

    private final JdbcTemplate jdbc;

    public JdbcDashboardDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public int activeProducts() {
        Integer total = jdbc.queryForObject(
            "SELECT COUNT(*) FROM producto WHERE estado=true",
            Integer.class
        );

        return total != null ? total : 0;
    }

    @Override
    public int todaySales() {
        Integer total = jdbc.queryForObject(
            "SELECT COUNT(*) FROM venta WHERE fecha_pago::date=CURRENT_DATE",
            Integer.class
        );

        return total != null ? total : 0;
    }
}

