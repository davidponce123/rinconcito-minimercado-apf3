package pe.utp.rinconcito.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pe.utp.rinconcito.dao.InventoryDao;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.LocalDate;

/** Exportación real .xlsx con Apache POI, no CSV renombrado. */
@Controller
public class ReportsController {
    private static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private final InventoryDao inventory; private final JdbcTemplate jdbc;
    public ReportsController(InventoryDao inventory, JdbcTemplate jdbc){this.inventory=inventory;this.jdbc=jdbc;}

    @GetMapping("/reports/inventory.xlsx") public void inventoryExcel(HttpServletResponse response) throws IOException {
        start(response,"rinconcito_inventario.xlsx");
        try(Workbook workbook=new XSSFWorkbook()){
            Sheet sheet=workbook.createSheet("Stock");
            header(workbook,sheet,"ID","Producto","Existencias","Stock mínimo","Reponer");
            int row=1;
            for(var s:inventory.stock()){
                Row r=sheet.createRow(row++);
                r.createCell(0).setCellValue(s.productId());r.createCell(1).setCellValue(s.name());
                r.createCell(2).setCellValue(s.quantity());r.createCell(3).setCellValue(s.minimum());
                r.createCell(4).setCellValue(s.needsReorder()?"SÍ":"NO");
            }
            finish(workbook,sheet,5,response);
        }
    }

    @GetMapping("/reports/sales.xlsx") public String salesExcel(
      @RequestParam(required=false) LocalDate from,@RequestParam(required=false) LocalDate to,
      HttpServletResponse response,RedirectAttributes flash) throws IOException {
        LocalDate begin=from==null?LocalDate.now().minusDays(30):from;
        LocalDate end=to==null?LocalDate.now():to;
        if(end.isBefore(begin) || end.isAfter(begin.plusYears(2))){
            flash.addFlashAttribute("error","Rango de fechas no válido: 'Hasta' debe ser posterior a 'Desde' y el rango no puede superar 2 años");
            return "redirect:/sales";
        }
        var records=jdbc.queryForList("SELECT id_venta,fecha_pago,tipo_pago,total FROM v_venta_total WHERE fecha_pago>=? AND fecha_pago<? ORDER BY fecha_pago",
              Timestamp.valueOf(begin.atStartOfDay()),Timestamp.valueOf(end.plusDays(1).atStartOfDay()));
        start(response,"rinconcito_ventas_"+begin+"_a_"+end+".xlsx");
        try(Workbook workbook=new XSSFWorkbook()){
            Sheet sheet=workbook.createSheet("Ventas pagadas");
            header(workbook,sheet,"Venta","Fecha de pago","Forma de pago","Total S/");
            CellStyle money=money(workbook);
            int idx=1;double sum=0;
            for(var v:records){Row row=sheet.createRow(idx++);
                double total=((Number)v.get("total")).doubleValue();sum+=total;
                row.createCell(0).setCellValue(((Number)v.get("id_venta")).doubleValue());
                row.createCell(1).setCellValue(String.valueOf(v.get("fecha_pago")));
                row.createCell(2).setCellValue(String.valueOf(v.get("tipo_pago")));
                Cell c=row.createCell(3);c.setCellValue(total);c.setCellStyle(money);
            }
            Row foot=sheet.createRow(idx);
            foot.createCell(2).setCellValue("TOTAL ("+records.size()+" ventas)");
            Cell t=foot.createCell(3);t.setCellValue(sum);t.setCellStyle(money);
            finish(workbook,sheet,4,response);
        }
        return null;
    }

    /** Mismo criterio que la pantalla de baja rotación: más de 30 días sin salidas. */
    @GetMapping("/reports/slow-moving.xlsx") public void slowMovingExcel(HttpServletResponse response) throws IOException {
        var records=jdbc.queryForList("""
          SELECT p.id_producto,p.nombre,s.stock_actual,
                 (CURRENT_DATE-COALESCE(MAX(CASE WHEN x.tipo='SALIDA' THEN x.fecha::date END),
                                           MIN(CASE WHEN x.tipo='ENTRADA' THEN x.fecha::date END))) AS dias,
                 ROUND(s.stock_actual*p.precio,2) AS valor_estimado
          FROM producto p JOIN v_stock s USING(id_producto)
          JOIN movimiento_inventario x USING(id_producto)
          WHERE p.estado=true AND s.stock_actual>0
          GROUP BY p.id_producto,p.nombre,s.stock_actual,p.precio
          HAVING (CURRENT_DATE-COALESCE(MAX(CASE WHEN x.tipo='SALIDA' THEN x.fecha::date END),
                                          MIN(CASE WHEN x.tipo='ENTRADA' THEN x.fecha::date END)))>30
          ORDER BY dias DESC,p.nombre
          """);
        start(response,"rinconcito_baja_rotacion.xlsx");
        try(Workbook workbook=new XSSFWorkbook()){
            Sheet sheet=workbook.createSheet("Baja rotación");
            header(workbook,sheet,"ID","Producto","Stock","Días sin salida","Valor estimado S/");
            CellStyle money=money(workbook);
            int idx=1;
            for(var v:records){Row row=sheet.createRow(idx++);
                row.createCell(0).setCellValue(((Number)v.get("id_producto")).doubleValue());
                row.createCell(1).setCellValue(String.valueOf(v.get("nombre")));
                row.createCell(2).setCellValue(((Number)v.get("stock_actual")).doubleValue());
                row.createCell(3).setCellValue(((Number)v.get("dias")).doubleValue());
                Cell c=row.createCell(4);c.setCellValue(((Number)v.get("valor_estimado")).doubleValue());c.setCellStyle(money);
            }
            finish(workbook,sheet,5,response);
        }
    }

    private static void start(HttpServletResponse response,String filename){
        response.setContentType(XLSX);
        response.setHeader("Content-Disposition","attachment; filename="+filename);
    }

    private static void header(Workbook workbook,Sheet sheet,String... names){
        Font bold=workbook.createFont();bold.setBold(true);
        CellStyle style=workbook.createCellStyle();style.setFont(bold);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Row header=sheet.createRow(0);
        for(int i=0;i<names.length;i++){Cell c=header.createCell(i);c.setCellValue(names[i]);c.setCellStyle(style);}
        sheet.createFreezePane(0,1);
    }

    private static CellStyle money(Workbook workbook){
        CellStyle style=workbook.createCellStyle();
        style.setDataFormat(workbook.createDataFormat().getFormat("#,##0.00"));
        return style;
    }

    private static void finish(Workbook workbook,Sheet sheet,int columns,HttpServletResponse response) throws IOException {
        for(int i=0;i<columns;i++)sheet.autoSizeColumn(i);
        workbook.write(response.getOutputStream());
    }
}
