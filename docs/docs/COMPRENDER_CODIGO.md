# Comprende Spring Boot con lo que ya viste en las clases

## Traducción del taller NetBeans a VS Code
No estás cambiando el lenguaje: **sigue siendo Java**. Cambia la vista Swing por HTML/Thymeleaf y cambia MySQL por PostgreSQL, porque eso es lo que indica el APF2. Spring Boot crea las instancias y conecta las capas mediante anotaciones.

| Taller | Proyecto web |
|---|---|
| `Main` con `main()` | `RinconcitoApplication.java` con `SpringApplication.run` |
| `VentanaPrincipal` (Swing) | `templates/products.html`, `sale-form.html`, `inventory.html` |
| `InventarioController` | `CatalogController`, `OperationsController` |
| `InventarioService` | `CatalogService`, `SalesService`, `ReceivingService` |
| `ProductoDAO` | `ProductDao` / `InventoryDao` |
| `ProductoDAOMemoria` | `JdbcProductDao` / `JdbcInventoryDao` (persistencia real) |
| `Producto` | `domain/Product.java` |
| MySQL Workbench | PostgreSQL + pgAdmin |

## Sigamos una venta paso a paso
1. Tú abres `/sales/new`. Spring envía la plantilla `templates/sale-form.html`.
2. Seleccionas producto, cantidad y medio de pago y das clic en **Confirmar**.
3. `OperationsController.sell()` recibe los campos del formulario.
4. El controlador llama a `SalesService.confirm()`. **El controlador no escribe SQL.**
5. `SalesService` comprueba cantidades, método de pago y stock disponible. Utiliza interfaces `ProductDao`, `InventoryDao` y `OperationDao`.
6. Los DAO implementados en `dao/jdbc` hablan con PostgreSQL. La base guarda cabecera, detalle y movimiento de salida dentro de una transacción.
7. Spring redirige a `/sales` y muestra un aviso; el cálculo `v_stock` refleja la salida.

### ¿Por qué servicio separado del controlador?
Si mañana cambias la forma de calcular stock, modificas el servicio o la consulta, sin editar los botones del HTML. Esto ilustra **responsabilidad única (SOLID)**.

### ¿Qué es DAO?
Una interfaz que define operaciones como `available(productId)` y `outgoing(productId, quantity, saleId, userId)`. El servicio conoce **qué necesita**, no el SQL exacto: la implementación `JdbcInventoryDao` define cómo hacerlo.

### ¿Qué es `@Transactional`?
Agrupa los cambios de una venta. Si una regla falla, no se debe registrar solo una parte de la operación. Las pruebas de concurrencia y recuperación deben confirmarse con PostgreSQL real.

### ¿Qué es TDD?
Test Driven Development significa escribir primero una prueba que falle, luego código que la haga pasar y refactorizar. Aquí se incluyen pruebas JUnit/Mockito, pero el historial **rojo-verde-refactor no ha sido demostrado**. Deben reproducir el ciclo de forma auténtica para defender TDD.

## Las cuatro librerías en una frase
- **Guava**: `Preconditions.checkArgument` rechaza una cantidad negativa.
- **Commons Lang3**: `StringUtils` limpia y verifica nombres.
- **Apache POI**: genera un `.xlsx` desde `/reports/inventory.xlsx`.
- **Logback + SLF4J**: registra eventos sin imprimir contraseñas.

## Preguntas que puede hacer el docente
- ¿Dónde se valida que no se venda sin stock? En `SalesService.confirm` y en `InventoryDao`.
- ¿Dónde queda el precio de la venta? En `detalle_venta.precio`, distinto del precio vigente del catálogo.
- ¿Dónde queda la fecha de vencimiento? En `detalle_ingreso.fecha_vencimiento`, por lote.
- ¿Por qué no guardar stock directamente en producto? Se deriva de entradas y salidas mediante `v_stock`.
- ¿Por qué PostgreSQL y no MySQL? Porque PostgreSQL fue seleccionado en el APF2. Los mismos conceptos de DAO/JDBC aplican a ambos.
- ¿Qué hago en VS Code? Abro la carpeta con `pom.xml`, ejecuto Maven y abro la web en Chrome.

## Tu tarea inicial
Abre solamente estos 5 archivos, en orden: `RinconcitoApplication.java`, `domain/Product.java`, `dao/InventoryDao.java`, `service/SalesService.java` y `controller/OperationsController.java`. No necesitas memorizar el proyecto entero.
