package pe.utp.rinconcito.controller;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pe.utp.rinconcito.config.SecurityConfig;
import pe.utp.rinconcito.dao.InventoryDao;
import pe.utp.rinconcito.domain.Stock;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReportsController.class)
@Import(SecurityConfig.class)
class ReportsControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean InventoryDao inventory;
    @MockitoBean JdbcTemplate jdbc;

    @Test void inventoryReportIsARealXlsxWithStockRows() throws Exception {
        when(inventory.stock()).thenReturn(List.of(new Stock(1,"Arroz",3,5),new Stock(2,"Azúcar",20,5)));
        var result=mvc.perform(get("/reports/inventory.xlsx").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andReturn();
        try(Workbook wb=new XSSFWorkbook(new ByteArrayInputStream(result.getResponse().getContentAsByteArray()))){
            Sheet sheet=wb.getSheet("Stock");
            assertEquals("Producto",sheet.getRow(0).getCell(1).getStringCellValue());
            assertEquals("Arroz",sheet.getRow(1).getCell(1).getStringCellValue());
            assertEquals("SÍ",sheet.getRow(1).getCell(4).getStringCellValue());
            assertEquals("NO",sheet.getRow(2).getCell(4).getStringCellValue());
        }
    }

    @Test void salesReportListsPaidSalesAndAddsATotalRow() throws Exception {
        when(jdbc.queryForList(anyString(),any(Timestamp.class),any(Timestamp.class))).thenReturn(List.of(
                Map.of("id_venta",1L,"fecha_pago",Timestamp.valueOf("2026-05-02 10:00:00"),"tipo_pago","EFECTIVO","total",new BigDecimal("10.50")),
                Map.of("id_venta",2L,"fecha_pago",Timestamp.valueOf("2026-05-03 11:00:00"),"tipo_pago","YAPE","total",new BigDecimal("4.50"))));
        var result=mvc.perform(get("/reports/sales.xlsx").param("from","2026-05-01").param("to","2026-05-31")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition","attachment; filename=rinconcito_ventas_2026-05-01_a_2026-05-31.xlsx"))
                .andReturn();
        try(Workbook wb=new XSSFWorkbook(new ByteArrayInputStream(result.getResponse().getContentAsByteArray()))){
            Sheet sheet=wb.getSheet("Ventas pagadas");
            assertEquals(10.50,sheet.getRow(1).getCell(3).getNumericCellValue());
            assertEquals("TOTAL (2 ventas)",sheet.getRow(3).getCell(2).getStringCellValue());
            assertEquals(15.00,sheet.getRow(3).getCell(3).getNumericCellValue());
        }
    }

    @Test void salesReportRejectsInvertedRangeWithoutQueryingTheDatabase() throws Exception {
        mvc.perform(get("/reports/sales.xlsx").param("from","2026-05-31").param("to","2026-05-01")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/sales"))
                .andExpect(flash().attributeExists("error"));
        verifyNoInteractions(jdbc);
    }

    @Test void salesReportRejectsRangesLongerThanTwoYears() throws Exception {
        mvc.perform(get("/reports/sales.xlsx").param("from","2020-01-01").param("to","2026-01-01")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(redirectedUrl("/sales")).andExpect(flash().attributeExists("error"));
        verifyNoInteractions(jdbc);
    }

    @Test void slowMovingReportListsProducts() throws Exception {
        when(jdbc.queryForList(anyString())).thenReturn(List.of(
                Map.of("id_producto",7,"nombre","Lentejas","stock_actual",12L,"dias",45,"valor_estimado",new BigDecimal("36.00"))));
        var result=mvc.perform(get("/reports/slow-moving.xlsx").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition","attachment; filename=rinconcito_baja_rotacion.xlsx"))
                .andReturn();
        try(Workbook wb=new XSSFWorkbook(new ByteArrayInputStream(result.getResponse().getContentAsByteArray()))){
            Sheet sheet=wb.getSheet("Baja rotación");
            assertEquals("Lentejas",sheet.getRow(1).getCell(1).getStringCellValue());
            assertEquals(45,sheet.getRow(1).getCell(3).getNumericCellValue());
            assertEquals(36.00,sheet.getRow(1).getCell(4).getNumericCellValue());
        }
    }

    @Test void cashierCannotDownloadReports() throws Exception {
        for(String url:List.of("/reports/inventory.xlsx","/reports/sales.xlsx","/reports/slow-moving.xlsx"))
            mvc.perform(get(url).with(user("cajero").roles("CAJERO"))).andExpect(status().isForbidden());
        verifyNoInteractions(inventory,jdbc);
    }

    @Test void anonymousUsersAreSentToLogin() throws Exception {
        mvc.perform(get("/reports/inventory.xlsx")).andExpect(status().is3xxRedirection());
    }
}
