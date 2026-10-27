package pe.utp.rinconcito.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import pe.utp.rinconcito.dao.InventoryDao;
import java.io.IOException;
import org.springframework.jdbc.core.JdbcTemplate;
import java.time.LocalDate;
import org.springframework.web.bind.annotation.RequestParam;

/** Exportación real .xlsx con Apache POI, no CSV renombrado. */
@Controller
public class ReportsController {
    private final InventoryDao inventory; private final JdbcTemplate jdbc;
    public ReportsController(InventoryDao inventory, JdbcTemplate jdbc){this.inventory=inventory;this.jdbc=jdbc;}
    @GetMapping("/reports/inventory.xlsx") public void inventoryExcel(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition","attachment; filename=rinconcito_inventario.xlsx");
        try(Workbook workbook=new XSSFWorkbook()){
            Sheet sheet=workbook.createSheet("Stock");
            Row header=sheet.createRow(0);
            String[] names={"ID","Producto","Existencias","Stock mínimo","Reponer"};
            for(int i=0;i<names.length;i++)header.createCell(i).setCellValue(names[i]);
            int row=1;
            for(var s:inventory.stock()){
                Row r=sheet.createRow(row++);
                r.createCell(0).setCellValue(s.productId());r.createCell(1).setCellValue(s.name());
                r.createCell(2).setCellValue(s.quantity());r.createCell(3).setCellValue(s.minimum());
                r.createCell(4).setCellValue(s.needsReorder()?"SÍ":"NO");
            }
            for(int i=0;i<names.length;i++)sheet.autoSizeColumn(i);
            workbook.write(response.getOutputStream());
        }
    }
    @GetMapping("/reports/sales.xlsx") public void salesExcel(
      @RequestParam(required=false) LocalDate from,@RequestParam(required=false) LocalDate to,HttpServletResponse response) throws IOException {
        LocalDate begin=from==null?LocalDate.now().minusDays(30):from;
        LocalDate end=to==null?LocalDate.now():to;
        if(end.isBefore(begin) || end.isAfter(begin.plusYears(2)))throw new IllegalArgumentException("Rango no válido");
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition","attachment; filename=rinconcito_ventas.xlsx");
        try(Workbook workbook=new XSSFWorkbook()){
            Sheet sheet=workbook.createSheet("Ventas pagadas");
            Row header=sheet.createRow(0);
            String[] fields={"Venta","Fecha de pago","Forma de pago","Total S/"};
            for(int i=0;i<fields.length;i++)header.createCell(i).setCellValue(fields[i]);
            int idx=1;
            var records=jdbc.queryForList("SELECT id_venta,fecha_pago,tipo_pago,total FROM v_venta_total WHERE fecha_pago>=? AND fecha_pago<? ORDER BY fecha_pago",
                  java.sql.Timestamp.valueOf(begin.atStartOfDay()),java.sql.Timestamp.valueOf(end.plusDays(1).atStartOfDay()));
            for(var v:records){Row row=sheet.createRow(idx++);
                row.createCell(0).setCellValue(((Number)v.get("id_venta")).doubleValue());
                row.createCell(1).setCellValue(String.valueOf(v.get("fecha_pago")));
                row.createCell(2).setCellValue(String.valueOf(v.get("tipo_pago")));
                row.createCell(3).setCellValue(((Number)v.get("total")).doubleValue());
            }
            for(int i=0;i<fields.length;i++)sheet.autoSizeColumn(i);
            workbook.write(response.getOutputStream());
        }
    }

}
