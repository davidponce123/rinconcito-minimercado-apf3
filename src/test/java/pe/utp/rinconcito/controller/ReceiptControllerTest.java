package pe.utp.rinconcito.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pe.utp.rinconcito.config.SecurityConfig;
import pe.utp.rinconcito.dao.ProductDao;
import pe.utp.rinconcito.service.ReceivingService;
import pe.utp.rinconcito.service.SalesService;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OperationsController.class)
@Import(SecurityConfig.class)
class ReceiptControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean ProductDao products;
    @MockitoBean JdbcTemplate jdbc;
    @MockitoBean ReceivingService receiving;
    @MockitoBean SalesService sales;

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder receipt() {
        return post("/receipts").with(user("admin").roles("ADMIN")).with(csrf())
                .param("supplierId", "4").param("guide", "G-01")
                .param("productId", "8").param("quantity", "3").param("cost", "3.50");
    }

    @Test void rejectsMissingQuantityWithoutCallingService() throws Exception {
        mvc.perform(post("/receipts").with(user("admin").roles("ADMIN")).with(csrf())
                        .param("supplierId", "4").param("guide", "G-01")
                        .param("productId", "8", "9").param("quantity", "3", "")
                        .param("cost", "3.50", "2.00"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/receipts/new"))
                .andExpect(flash().attributeExists("error"));
        verifyNoInteractions(receiving);
    }

    @Test void rejectsDatesThatDoNotMatchTheLots() throws Exception {
        mvc.perform(receipt().param("expiry", "2030-01-01", "2031-01-01"))
                .andExpect(redirectedUrl("/receipts/new")).andExpect(flash().attributeExists("error"));
        verifyNoInteractions(receiving);
    }

    @Test void reportsDuplicateGuideWithoutDatabaseDetails() throws Exception {
        when(receiving.register(anyLong(), anyString(), anyList(), anyString()))
                .thenThrow(new DuplicateKeyException("private SQL details"));
        mvc.perform(receipt()).andExpect(redirectedUrl("/receipts/new"))
                .andExpect(flash().attribute("error", "La guía ya está registrada para este proveedor"));
    }

    @Test void reportsOtherIntegrityErrorsWithoutDatabaseDetails() throws Exception {
        when(receiving.register(anyLong(), anyString(), anyList(), anyString()))
                .thenThrow(new DataIntegrityViolationException("private SQL details"));
        mvc.perform(receipt()).andExpect(redirectedUrl("/receipts/new"))
                .andExpect(flash().attribute("error", "No se pudo registrar la recepción; revisa los datos de los lotes"));
    }

    @Test void acceptsAnOptionalExpiration() throws Exception {
        when(receiving.register(anyLong(), anyString(), anyList(), anyString())).thenReturn(18L);
        mvc.perform(receipt()).andExpect(redirectedUrl("/receipts"))
                .andExpect(flash().attributeExists("success"));
        verify(receiving).register(eq(4L), eq("G-01"), argThat(lines ->
                lines.size() == 1 && lines.get(0).quantity() == 3 && lines.get(0).expiration() == null), eq("admin"));
    }

    @Test void acceptsABlankExpirationField() throws Exception {
        mvc.perform(receipt().param("expiry", "")).andExpect(redirectedUrl("/receipts"));
    }

    @Test void rejectsAnInvalidDateWithoutCallingService() throws Exception {
        mvc.perform(receipt().param("expiry", "not-a-date"))
                .andExpect(redirectedUrl("/receipts/new")).andExpect(flash().attributeExists("error"));
        verifyNoInteractions(receiving);
    }

    @Test void cashierCannotRegisterReceipts() throws Exception {
        mvc.perform(receipt().with(user("cajero").roles("CAJERO"))).andExpect(status().isForbidden());
        verifyNoInteractions(receiving);
    }

    @Test void receiptRequiresCsrf() throws Exception {
        mvc.perform(post("/receipts").with(user("admin").roles("ADMIN")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(receiving);
    }

    @Test void anonymousCannotOpenReceiptForm() throws Exception {
        mvc.perform(get("/receipts/new")).andExpect(status().is3xxRedirection());
    }

    @Test void administratorCanRenderReceiptForm() throws Exception {
        mvc.perform(get("/receipts/new").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk()).andExpect(view().name("receipt-form"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Lotes y cantidades")));
    }
}
