package pe.utp.rinconcito.service;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.context.annotation.*;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import pe.utp.rinconcito.dao.InventoryDao;
import pe.utp.rinconcito.dao.OperationDao;
import pe.utp.rinconcito.dao.ProductDao;
import pe.utp.rinconcito.dao.jdbc.*;
import pe.utp.rinconcito.domain.ReceiptLine;
import pe.utp.rinconcito.domain.SaleLine;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

/** Opt-in PostgreSQL tests: a localhost test database and a fresh random schema, never application data. */
@EnabledIfEnvironmentVariable(named="RINCONCITO_TEST_DB_URL",
        matches="jdbc:postgresql://(?:127\\.0\\.0\\.1|localhost):[0-9]+/rinconcito_test_[a-zA-Z0-9_]+")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class InventoryMovementsIntegrationTest {
    private AnnotationConfigApplicationContext context;
    private JdbcTemplate admin;
    private JdbcTemplate jdbc;
    private ReceivingService receiving;
    private SalesService sales;
    private InventoryDao inventory;
    private final String schema="rinconcito_test_"+UUID.randomUUID().toString().replace("-", "");

    @TestConfiguration @EnableTransactionManagement
    static class Services {
        @Bean JdbcTemplate jdbc(DataSource source){return new JdbcTemplate(source);}
        @Bean PlatformTransactionManager transactions(DataSource source){return new DataSourceTransactionManager(source);}
        @Bean ProductDao products(JdbcTemplate jdbc){return new JdbcProductDao(jdbc);}
        @Bean InventoryDao inventory(JdbcTemplate jdbc){return new JdbcInventoryDao(jdbc);}
        @Bean OperationDao operations(JdbcTemplate jdbc){return new JdbcOperationDao(jdbc);}
        @Bean ReceivingService receiving(ProductDao p,InventoryDao i,OperationDao o){return new ReceivingService(p,i,o);}
        @Bean SalesService sales(ProductDao p,InventoryDao i,OperationDao o){return new SalesService(p,i,o);}
    }

    @BeforeAll void connect(){
        var source=new DriverManagerDataSource();
        source.setUrl(System.getenv("RINCONCITO_TEST_DB_URL"));
        source.setUsername(System.getenv().getOrDefault("RINCONCITO_TEST_DB_USER","rinconcito_test"));
        source.setPassword(System.getenv().getOrDefault("RINCONCITO_TEST_DB_PASSWORD",""));
        admin=new JdbcTemplate(source);
        assertTrue(schema.matches("rinconcito_test_[a-f0-9]+"));
        admin.execute("CREATE SCHEMA "+schema);
        var properties=new Properties();
        properties.setProperty("currentSchema",schema);
        properties.setProperty("options","-c statement_timeout=10000 -c lock_timeout=5000");
        source.setConnectionProperties(properties);
        context=new AnnotationConfigApplicationContext();
        context.registerBean(DataSource.class,()->source);
        context.register(Services.class);
        context.refresh();
        jdbc=context.getBean(JdbcTemplate.class);
        new ResourceDatabasePopulator(new ClassPathResource("db/schema.sql")).execute(source);
        receiving=context.getBean(ReceivingService.class);
        sales=context.getBean(SalesService.class);
        inventory=context.getBean(InventoryDao.class);
    }

    @AfterAll void disconnect(){
        if(context!=null)context.close();
        if(admin!=null && schema.matches("rinconcito_test_[a-f0-9]+"))admin.execute("DROP SCHEMA IF EXISTS "+schema+" CASCADE");
    }

    @BeforeEach void seed(){
        jdbc.execute("ALTER TABLE movimiento_inventario DROP CONSTRAINT IF EXISTS test_reject_movement");
        jdbc.execute("TRUNCATE rol,categoria,proveedor RESTART IDENTITY CASCADE");
        jdbc.update("INSERT INTO rol(id_rol,nombre) VALUES (1,'ADMIN')");
        jdbc.update("INSERT INTO usuario(id_usuario,nombre_usuario,password_hash,id_rol) VALUES (1,'admin','TEST_ONLY_NOT_A_LOGIN_HASH',1)");
        jdbc.update("INSERT INTO categoria(id_categoria,nombre) VALUES (1,'Prueba')");
        jdbc.update("INSERT INTO proveedor(id_proveedor,nombre) VALUES (1,'Simulado A'),(2,'Simulado B')");
        jdbc.update("INSERT INTO producto(id_producto,nombre,precio,id_categoria) VALUES (1,'Prueba A',4.50,1),(2,'Prueba B',2.00,1)");
        jdbc.update("INSERT INTO inventario(id_producto,stock_minimo) VALUES (1,5),(2,2)");
    }

    private ReceiptLine lot(long id,int quantity,LocalDate expiration){
        return new ReceiptLine(id,quantity,new BigDecimal("2.50"),expiration);
    }

    private int count(String table){
        return jdbc.queryForObject("SELECT COUNT(*) FROM "+table,Integer.class);
    }

    @Test void subcentCostCannotBeRoundedIntoARecordedLot(){
        var valid=lot(1,4,null);
        var subcent=new ReceiptLine(1,6,new BigDecimal("1.005"),null);
        assertThrows(IllegalArgumentException.class,
                ()->receiving.register(1,"G-SUBCENT",List.of(valid,subcent),"admin"));
        assertEquals(0,count("ingreso_mercaderia"));
        assertEquals(0,count("detalle_ingreso"));
        assertEquals(0,count("movimiento_inventario"));
        assertEquals(0,inventory.available(1));
    }

    @Test void oversizedCostIsRejectedAsAValidationError(){
        var oversized=new ReceiptLine(1,3,new BigDecimal("100000000"),null);
        assertThrows(IllegalArgumentException.class,
                ()->receiving.register(1,"G-RANGE",List.of(oversized),"admin"));
        assertEquals(0,count("ingreso_mercaderia"));
        assertEquals(0,count("detalle_ingreso"));
        assertEquals(0,count("movimiento_inventario"));
        assertEquals(0,inventory.available(1));
    }

    @Test void exactCostsAndOptionalExpirationsSurviveConsolidation(){
        var dated=new ReceiptLine(1,4,new BigDecimal("3.4000"),LocalDate.now().plusMonths(2));
        var undated=new ReceiptLine(1,6,new BigDecimal("1E+1"),null);
        long id=receiving.register(1,"G-EXACT",List.of(dated,undated),"admin");
        assertEquals(List.of(new BigDecimal("3.40"),new BigDecimal("10.00")),jdbc.query(
                "SELECT costo FROM detalle_ingreso WHERE id_ingreso=? ORDER BY id_detalle_ingreso",
                (rs,n)->rs.getBigDecimal(1),id));
        assertNull(jdbc.queryForObject(
                "SELECT fecha_vencimiento FROM detalle_ingreso WHERE id_ingreso=? ORDER BY id_detalle_ingreso DESC LIMIT 1",
                java.sql.Date.class,id));
        assertEquals(2,count("detalle_ingreso"));
        assertEquals(1,count("movimiento_inventario"));
        assertEquals(10,inventory.available(1));
    }

    @Test void preservesLotsAndConsolidatesEntryThenSaleLeavesSeven(){
        var date=LocalDate.now().plusMonths(3);
        long receipt=receiving.register(1," G-01 ",List.of(lot(1,4,date),lot(1,6,date.plusMonths(2))),"admin");
        assertEquals(2,count("detalle_ingreso"));
        assertEquals(1,count("movimiento_inventario"));
        assertEquals(10,inventory.available(1));
        assertEquals(List.of(date,date.plusMonths(2)),jdbc.query(
                "SELECT fecha_vencimiento FROM detalle_ingreso WHERE id_ingreso=? ORDER BY id_detalle_ingreso",
                (rs,n)->rs.getDate(1).toLocalDate(),receipt));
        assertEquals("G-01",jdbc.queryForObject("SELECT guia FROM ingreso_mercaderia WHERE id_ingreso=?",String.class,receipt));
        sales.confirm(List.of(new SaleLine(1,3)),"EFECTIVO","admin");
        assertEquals(7,inventory.available(1));
        var stock=inventory.stock().stream().filter(s->s.productId()==1).findFirst().orElseThrow();
        assertEquals(7,stock.quantity());
        assertEquals(5,stock.minimum());
        assertFalse(stock.needsReorder());
        assertEquals(2,count("movimiento_inventario"));
        assertThrows(IllegalArgumentException.class,()->sales.confirm(List.of(new SaleLine(1,8)),"EFECTIVO","admin"));
        assertEquals(1,count("venta"));
        assertEquals(7,inventory.available(1));
    }

    @Test void duplicateGuideCannotCreateAnotherReceiptOrMovement(){
        receiving.register(1,"G-01",List.of(lot(1,5,null)),"admin");
        assertThrows(DuplicateKeyException.class,()->receiving.register(1," G-01 ",List.of(lot(1,9,null)),"admin"));
        assertEquals(1,count("ingreso_mercaderia"));
        assertEquals(1,count("detalle_ingreso"));
        assertEquals(1,count("movimiento_inventario"));
        assertEquals(5,inventory.available(1));
        receiving.register(2,"G-01",List.of(lot(1,2,null)),"admin");
        assertEquals(7,inventory.available(1));
    }

    @Test void expiredLotDoesNotWriteAnything(){
        assertThrows(IllegalArgumentException.class,()->receiving.register(1,"G",List.of(lot(1,3,LocalDate.now().minusDays(1))),"admin"));
        assertEquals(0,count("ingreso_mercaderia"));
        assertEquals(0,count("detalle_ingreso"));
        assertEquals(0,count("movimiento_inventario"));
        assertEquals(0,inventory.available(1));
    }

    @Test void movementFailureRollsBackHeaderAllLotsAndEarlierMovement(){
        // Failure after header, both details and the first product's entry were already inserted.
        jdbc.execute("ALTER TABLE movimiento_inventario ADD CONSTRAINT test_reject_movement CHECK(cantidad<>13)");
        assertThrows(DataIntegrityViolationException.class,()->receiving.register(1,"G",List.of(lot(1,2,null),lot(2,13,null)),"admin"));
        assertEquals(0,count("ingreso_mercaderia"));
        assertEquals(0,count("detalle_ingreso"));
        assertEquals(0,count("movimiento_inventario"));
        assertEquals(0,inventory.available(1));
        assertEquals(0,inventory.available(2));
    }

    private List<Boolean> concurrently(Supplier<Boolean> first,Supplier<Boolean> second) throws Exception {
        var start=new CountDownLatch(1);
        var executor=Executors.newFixedThreadPool(2);
        try{
            var a=executor.submit(()->{start.await();return first.get();});
            var c=executor.submit(()->{start.await();return second.get();});
            start.countDown();
            return List.of(a.get(15,TimeUnit.SECONDS),c.get(15,TimeUnit.SECONDS));
        }finally{
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(15,TimeUnit.SECONDS));
        }
    }

    @Test void concurrentReceiptsKeepEveryEntry() throws Exception {
        concurrently(()->{receiving.register(1,"G-A",List.of(lot(1,4,null)),"admin");return true;},
                ()->{receiving.register(1,"G-B",List.of(lot(1,6,null)),"admin");return true;});
        assertEquals(10,inventory.available(1));
        assertEquals(2,count("ingreso_mercaderia"));
        assertEquals(2,count("movimiento_inventario"));
    }

    @Test void inventoryIncludesZeroStockAndWarnsAtMinimum(){
        var initial=inventory.stock().stream().filter(s->s.productId()==1).findFirst().orElseThrow();
        assertEquals(0,initial.quantity());
        assertTrue(initial.needsReorder());
        receiving.register(1,"G-MIN",List.of(lot(1,5,null)),"admin");
        var atMinimum=inventory.stock().stream().filter(s->s.productId()==1).findFirst().orElseThrow();
        assertEquals(5,atMinimum.quantity());
        assertTrue(atMinimum.needsReorder());
    }

    @Test void concurrentSameGuideRecordsOnlyOneReceipt() throws Exception {
        Supplier<Boolean> receive=()->{
            try{receiving.register(1,"G-SAME",List.of(lot(1,3,null)),"admin");return true;}
            catch(DuplicateKeyException expected){return false;}
        };
        assertEquals(1,concurrently(receive,receive).stream().filter(Boolean::booleanValue).count());
        assertEquals(1,count("ingreso_mercaderia"));
        assertEquals(1,count("detalle_ingreso"));
        assertEquals(1,count("movimiento_inventario"));
        assertEquals(3,inventory.available(1));
    }

    @Test void twoCashiersCannotSellTheSameLastUnit() throws Exception {
        receiving.register(1,"G-LAST",List.of(lot(1,1,null)),"admin");
        Supplier<Boolean> sell=()->{
            try{sales.confirm(List.of(new SaleLine(1,1)),"EFECTIVO","admin");return true;}
            catch(IllegalArgumentException expected){return false;}
        };
        assertEquals(1,concurrently(sell,sell).stream().filter(Boolean::booleanValue).count());
        assertEquals(0,inventory.available(1));
        assertEquals(1,count("venta"));
        assertEquals(1,count("detalle_venta"));
        assertEquals(2,count("movimiento_inventario"));
    }
}
