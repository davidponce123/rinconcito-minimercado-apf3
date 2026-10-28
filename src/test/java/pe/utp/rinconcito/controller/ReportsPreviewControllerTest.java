package pe.utp.rinconcito.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pe.utp.rinconcito.config.SecurityConfig;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Comprueba paginación y protección del nuevo centro de reportes. */
@WebMvcTest(ReportsPreviewController.class)
@Import(SecurityConfig.class)
class ReportsPreviewControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean JdbcTemplate jdbc;

    @Test void inventoryShowsOnlyPageOf25AndTotalCount() throws Exception {
        when(jdbc.queryForObject("SELECT COUNT(*) FROM v_stock", Long.class))
                .thenReturn(1000L);
        when(jdbc.queryForList(anyString(),eq(25),eq(0)))
                .thenReturn(List.of(Map.of("id_producto",1L,"nombre","Arroz",
                        "stock_actual",5L,"stock_minimo",3L)));

        mvc.perform(get("/reports").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("reports-preview"))
                .andExpect(model().attribute("totalRows",1000L))
                .andExpect(model().attribute("pages",40L));
        verify(jdbc).queryForList(anyString(),eq(25),eq(0));
    }

    @Test void invalidDateRangeDoesNotTouchDatabase() throws Exception {
        mvc.perform(get("/reports").with(user("admin").roles("ADMIN"))
                .param("type","sales").param("from","2026-10-28").param("to","2026-10-01"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("error"));
        verifyNoInteractions(jdbc);
    }

    @Test void cashierCannotAccessPreview() throws Exception {
        mvc.perform(get("/reports").with(user("cashier").roles("CAJERO")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(jdbc);
    }
}
