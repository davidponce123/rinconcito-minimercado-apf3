package pe.utp.rinconcito.controller;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Vista previa paginada de los datos que se exportan a Excel.
 * Los resultados se obtienen de las vistas SQL utilizadas por el sistema;
 * no se generan archivos temporales ni se exponen datos sin autenticación.
 */
@Controller
public class ReportsPreviewController {
    private static final int PAGE_SIZE = 25;
    private static final Set<String> TYPES = Set.of("inventory", "sales", "slow");
    private final JdbcTemplate jdbc;

    // Misma regla de baja rotación empleada por los reportes XLSX actuales.
    private static final String SLOW_MOVING_SQL = """
        SELECT p.id_producto, p.nombre, s.stock_actual,
               (CURRENT_DATE - COALESCE(
                   MAX(CASE WHEN x.tipo='SALIDA' THEN x.fecha::date END),
                   MIN(CASE WHEN x.tipo='ENTRADA' THEN x.fecha::date END)
               )) AS dias,
               ROUND(s.stock_actual * p.precio, 2) AS valor_estimado
        FROM producto p
        JOIN v_stock s USING(id_producto)
        JOIN movimiento_inventario x USING(id_producto)
        WHERE p.estado = true AND s.stock_actual > 0
        GROUP BY p.id_producto, p.nombre, s.stock_actual, p.precio
        HAVING (CURRENT_DATE - COALESCE(
                   MAX(CASE WHEN x.tipo='SALIDA' THEN x.fecha::date END),
                   MIN(CASE WHEN x.tipo='ENTRADA' THEN x.fecha::date END)
               )) > 30
        """;

    public ReportsPreviewController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/reports")
    public String preview(
            @RequestParam(defaultValue = "inventory") String type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            Model model) {

        if (!TYPES.contains(type)) type = "inventory";
        int currentPage = Math.max(0, page);
        LocalDate begin = from == null ? LocalDate.now().minusDays(30) : from;
        LocalDate end = to == null ? LocalDate.now() : to;
        List<Map<String, Object>> rows = List.of();
        long total = 0;
        BigDecimal amount = BigDecimal.ZERO;
        String error = null;

        // No consultar ni exportar rangos inválidos.
        if ("sales".equals(type) &&
                (end.isBefore(begin) || end.isAfter(begin.plusYears(2)))) {
            error = "El rango de ventas no es válido (máximo dos años).";
        } else if ("inventory".equals(type)) {
            Long n = jdbc.queryForObject("SELECT COUNT(*) FROM v_stock", Long.class);
            total = n == null ? 0 : n;
            currentPage = normalizePage(currentPage, total);
            rows = jdbc.queryForList("""
                SELECT id_producto,nombre,stock_actual,stock_minimo
                FROM v_stock ORDER BY nombre LIMIT ? OFFSET ?
                """, PAGE_SIZE, currentPage * PAGE_SIZE);
        } else if ("sales".equals(type)) {
            Timestamp startAt = Timestamp.valueOf(begin.atStartOfDay());
            Timestamp endAt = Timestamp.valueOf(end.plusDays(1).atStartOfDay());
            Long n = jdbc.queryForObject("""
                SELECT COUNT(*) FROM v_venta_total
                WHERE fecha_pago >= ? AND fecha_pago < ?
                """, Long.class, startAt, endAt);
            total = n == null ? 0 : n;
            BigDecimal sum = jdbc.queryForObject("""
                SELECT COALESCE(SUM(total),0) FROM v_venta_total
                WHERE fecha_pago >= ? AND fecha_pago < ?
                """, BigDecimal.class, startAt, endAt);
            amount = sum == null ? BigDecimal.ZERO : sum;
            currentPage = normalizePage(currentPage, total);
            rows = jdbc.queryForList("""
                SELECT id_venta, fecha_pago, tipo_pago, total
                FROM v_venta_total WHERE fecha_pago >= ? AND fecha_pago < ?
                ORDER BY fecha_pago DESC, id_venta DESC LIMIT ? OFFSET ?
                """, startAt, endAt, PAGE_SIZE, currentPage * PAGE_SIZE);
        } else {
            Long n = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM (" + SLOW_MOVING_SQL + ") q", Long.class);
            total = n == null ? 0 : n;
            currentPage = normalizePage(currentPage, total);
            rows = jdbc.queryForList(
                    "SELECT * FROM (" + SLOW_MOVING_SQL + ") q " +
                    "ORDER BY dias DESC, nombre LIMIT ? OFFSET ?",
                    PAGE_SIZE, currentPage * PAGE_SIZE);
        }

        model.addAttribute("type", type);
        model.addAttribute("from", begin);
        model.addAttribute("to", end);
        model.addAttribute("page", currentPage);
        model.addAttribute("pages", Math.max(1L, (total + PAGE_SIZE - 1) / PAGE_SIZE));
        model.addAttribute("totalRows", total);
        model.addAttribute("totalAmount", amount);
        model.addAttribute("items", rows);
        model.addAttribute("error", error);
        return "reports-preview";
    }

    private static int normalizePage(int requested, long total) {
        long lastPage = Math.max(0, (total - 1) / PAGE_SIZE);
        return (int) Math.min(requested, Math.min(lastPage, Integer.MAX_VALUE / PAGE_SIZE));
    }
}
