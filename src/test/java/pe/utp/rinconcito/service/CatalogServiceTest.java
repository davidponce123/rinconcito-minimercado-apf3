package pe.utp.rinconcito.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import pe.utp.rinconcito.dao.InventoryDao;
import pe.utp.rinconcito.dao.ProductDao;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CatalogServiceTest {
    private ProductDao products;
    private InventoryDao inventory;
    private CatalogService service;

    @BeforeEach
    void setUp() {
        products = mock(ProductDao.class);
        inventory = mock(InventoryDao.class);
        service = new CatalogService(products, inventory);
        when(products.categoryExists(2L)).thenReturn(true);
    }

    private IllegalArgumentException assertRejected(org.junit.jupiter.api.function.Executable action) {
        return assertThrows(IllegalArgumentException.class, action);
    }

    // ---------- Productos: casos válidos ----------

    @Test
    void registraProductoYConfiguracionInventario() {
        when(products.create("Arroz", new BigDecimal("4.50"), 2L)).thenReturn(17L);
        assertEquals(17L, service.createProduct("Arroz", new BigDecimal("4.50"), 2, 5));
        verify(inventory).setup(17L, 5L);
    }

    @Test
    void normalizaEspaciosEnElNombre() {
        when(products.create("Arroz extra 1 kg", new BigDecimal("4.50"), 2L)).thenReturn(8L);
        assertEquals(8L, service.createProduct("   Arroz   extra  1 kg ", new BigDecimal("4.50"), 2, 0));
    }

    @Test
    void aceptaPrecioConCerosSobrantes() {
        when(products.create(anyString(), any(), anyLong())).thenReturn(1L);
        assertDoesNotThrow(() -> service.createProduct("Sal", new BigDecimal("3.500"), 2, 0));
    }

    // ---------- Productos: validaciones ----------

    @ParameterizedTest
    @ValueSource(strings = {"-1", "0", "0.00", "4.505", "100000000"})
    void rechazaPreciosInvalidos(String precio) {
        assertRejected(() -> service.createProduct("Arroz", new BigDecimal(precio), 2, 0));
        verify(products, never()).create(anyString(), any(), anyLong());
    }

    @Test
    void rechazaPrecioNulo() {
        assertRejected(() -> service.createProduct("Arroz", null, 2, 0));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void rechazaNombreEnBlanco(String nombre) {
        assertRejected(() -> service.createProduct(nombre, new BigDecimal("4.50"), 2, 0));
    }

    @Test
    void rechazaNombreNulo() {
        assertRejected(() -> service.createProduct(null, new BigDecimal("4.50"), 2, 0));
    }

    @Test
    void rechazaNombreMayorA80Caracteres() {
        assertRejected(() -> service.createProduct("A".repeat(81), new BigDecimal("4.50"), 2, 0));
        assertDoesNotThrow(() -> service.createProduct("A".repeat(80), new BigDecimal("4.50"), 2, 0));
    }

    @Test
    void rechazaCategoriaInexistente() {
        IllegalArgumentException ex = assertRejected(() -> service.createProduct("Arroz", new BigDecimal("4.50"), 99, 0));
        assertEquals("La categoría seleccionada no existe", ex.getMessage());
        verify(products, never()).create(anyString(), any(), anyLong());
    }

    @Test
    void rechazaCategoriaCeroONegativa() {
        assertRejected(() -> service.createProduct("Arroz", new BigDecimal("4.50"), 0, 0));
        assertRejected(() -> service.createProduct("Arroz", new BigDecimal("4.50"), -3, 0));
    }

    @ParameterizedTest
    @ValueSource(longs = {-1, 100_001})
    void rechazaStockMinimoFueraDeRango(long minimo) {
        assertRejected(() -> service.createProduct("Arroz", new BigDecimal("4.50"), 2, minimo));
        verifyNoInteractions(inventory);
    }

    @Test
    void rechazaNombreDuplicadoSinCrearNiConfigurarInventario() {
        when(products.activeProductNameExists("Arroz", 0)).thenReturn(true);
        IllegalArgumentException ex = assertRejected(() -> service.createProduct("Arroz", new BigDecimal("4.50"), 2, 0));
        assertEquals("Ya existe un producto activo con ese nombre", ex.getMessage());
        verify(products, never()).create(anyString(), any(), anyLong());
        verifyNoInteractions(inventory);
    }

    // ---------- Edición y baja ----------

    @Test
    void actualizaProductoValido() {
        service.changeProduct(5, " Arroz   Superior ", new BigDecimal("5.20"));
        verify(products).update(5L, "Arroz Superior", new BigDecimal("5.20"));
    }

    @Test
    void rechazaEditarConNombreDeOtroProducto() {
        when(products.activeProductNameExists("Azúcar 1 kg", 5L)).thenReturn(true);
        assertRejected(() -> service.changeProduct(5, "Azúcar 1 kg", new BigDecimal("4.00")));
        verify(products, never()).update(anyLong(), anyString(), any());
    }

    @Test
    void rechazaEditarConPrecioInvalido() {
        assertRejected(() -> service.changeProduct(5, "Arroz", BigDecimal.ZERO));
        assertRejected(() -> service.changeProduct(0, "Arroz", new BigDecimal("4.50")));
    }

    @Test
    void deshabilitaProductoExistente() {
        service.deactivate(3);
        verify(products).deactivate(3L);
    }

    @Test
    void rechazaDeshabilitarIdInvalido() {
        assertRejected(() -> service.deactivate(0));
        verify(products, never()).deactivate(anyLong());
    }

    // ---------- Categorías ----------

    @Test
    void registraCategoriaNormalizandoEspacios() {
        service.addCategory("  Snacks   y   dulces ");
        verify(products).createCategory("Snacks y dulces");
    }

    @Test
    void rechazaCategoriaDuplicada() {
        when(products.categoryNameExists("Bebidas")).thenReturn(true);
        IllegalArgumentException ex = assertRejected(() -> service.addCategory("Bebidas"));
        assertEquals("Ya existe una categoría con ese nombre", ex.getMessage());
        verify(products, never()).createCategory(anyString());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void rechazaCategoriaEnBlanco(String nombre) {
        assertRejected(() -> service.addCategory(nombre));
    }

    @Test
    void rechazaCategoriaMayorA50Caracteres() {
        assertRejected(() -> service.addCategory("C".repeat(51)));
        assertDoesNotThrow(() -> service.addCategory("C".repeat(50)));
    }
}
