# Rinconcito Minimercado — APF3

Sistema web de gestión de ventas e inventario desarrollado para el Curso Integrador I.

El proyecto permite administrar productos, categorías, ventas, recepción de mercadería, inventario, usuarios y reportes.

## 1. Tecnologías utilizadas

- **Java 25**
- **Spring Boot 3.5.16**
- **Spring MVC y Thymeleaf**
- **Spring Security**
- **Spring JDBC / DAO**
- **PostgreSQL**
- **Maven**
- **JUnit 5 y Mockito**
- **Visual Studio Code**

El proyecto funciona localmente en Windows y no requiere Docker.

## 2. Requisitos previos

Antes de ejecutar el sistema, instalar:

1. JDK 25 o posterior.
2. Apache Maven.
3. PostgreSQL y pgAdmin 4.
4. Visual Studio Code con las extensiones para Java y Spring Boot.
5. Git.

Verificar Java y Maven desde PowerShell:

```powershell
java -version
mvn -version
git --version
```

Java debe utilizar la versión 25 o superior, de acuerdo con la configuración actual de `pom.xml`.

## 3. Clonar el repositorio

Abrir PowerShell en la carpeta donde se desea guardar el proyecto:

```powershell
cd $env:USERPROFILE\Downloads

git clone https://github.com/davidponce123/rinconcito-minimercado-apf3.git

cd rinconcito-minimercado-apf3

code .
```

## 4. Crear la base de datos

1. Abrir pgAdmin 4.
2. Conectarse al servidor PostgreSQL local.
3. Desplegar `Databases`.
4. Seleccionar `Create → Database`.
5. Crear una base llamada `rinconcito`.
6. Abrir `Query Tool` sobre esa base de datos.

En una base nueva y vacía, ejecutar el archivo:

`src/main/resources/db/schema.sql`

Este archivo crea las 15 tablas y las vistas del modelo de datos.

**Importante:** No volver a ejecutar `schema.sql` sobre una base ya configurada sin revisar previamente su estado, porque contiene instrucciones `CREATE TABLE` y no es un script de actualización repetible.

Para agregar productos de demostración en una base recién creada, puede ejecutarse opcionalmente:

`src/main/resources/db/demo_seed.sql`

Los productos de demostración comienzan con stock cero. Las existencias se generan mediante movimientos de recepción.

## 5. Configurar PostgreSQL

En la carpeta principal del proyecto, crear el archivo `.env` a partir de la plantilla:

```powershell
Copy-Item .env.example .env
```

Abrir el archivo `.env` y configurar los valores locales:

```properties
DB_URL=jdbc:postgresql://localhost:5432/rinconcito
DB_USER=postgres
DB_PASSWORD=TU_CLAVE_LOCAL_DE_POSTGRESQL
ADMIN_USER=administrador
ADMIN_PASSWORD=UNA_CLAVE_SEGURA_DE_AL_MENOS_12_CARACTERES
```

Cada integrante debe utilizar su propia contraseña de PostgreSQL.

El archivo `.env` contiene información privada y **no debe subirse a GitHub**. El repositorio únicamente comparte `.env.example`.

En una base recién preparada, el sistema puede crear el usuario administrador inicial utilizando las variables `ADMIN_USER` y `ADMIN_PASSWORD`.

## 6. Verificar PostgreSQL

Antes de iniciar el sistema, ejecutar:

```powershell
.\scripts\verificar-postgresql.ps1
```

Este script comprueba:

- Existencia del archivo `.env`.
- Variables obligatorias.
- Credenciales de ejemplo sin reemplazar.
- Formato básico de la URL JDBC.
- Accesibilidad TCP del servidor PostgreSQL.

La conexión TCP no demuestra por sí sola que las credenciales sean válidas. La autenticación y las consultas se comprueban al iniciar Spring Boot.

## 7. Ejecutar las pruebas

Ejecutar:

```powershell
mvn clean test
```

Si las pruebas configuradas terminan correctamente, Maven mostrará:

```text
BUILD SUCCESS
```

Las pruebas de integración de PostgreSQL que requieren una base de pruebas independiente están configuradas para ejecutarse únicamente cuando se habilita su entorno específico.

## 8. Iniciar el sistema

Desde la carpeta principal del proyecto:

```powershell
.\scripts\iniciar.ps1
```

El script verifica la configuración y ejecuta Spring Boot mediante Maven.

Después, abrir en el navegador:

`http://localhost:8080/login`

Para iniciar sesión en una instalación nueva, utilizar el usuario y la contraseña de administrador configurados previamente en `.env`.

También puede ejecutarse:

```powershell
mvn spring-boot:run
```

En este caso debe asegurarse de que Spring Boot pueda leer el archivo `.env` desde la raíz del proyecto.

## 9. Arquitectura del sistema

El proyecto está organizado en capas:

- **Controller:** recibe solicitudes HTTP y prepara las vistas.
- **Service:** implementa reglas y procesos de negocio.
- **DAO:** define las operaciones de acceso a datos.
- **JDBC:** ejecuta consultas SQL en PostgreSQL.
- **Domain:** contiene los modelos de dominio.
- **Templates:** contiene las páginas HTML renderizadas con Thymeleaf.

Un ejemplo del flujo del dashboard es:

`HomeController → DashboardService → DashboardDao / InventoryDao → PostgreSQL`

Esta separación facilita el mantenimiento y las pruebas automatizadas.

## 10. Verificación funcional

Con el sistema iniciado, comprobar las funciones disponibles según el rol del usuario:

1. Inicio y cierre de sesión.
2. Consulta del dashboard.
3. Gestión de productos y categorías.
4. Consulta de inventario.
5. Registro de una venta de prueba.
6. Actualización de existencias tras los movimientos.
7. Registro de recepción de mercadería.
8. Generación de reportes disponibles.

Estas comprobaciones deben realizarse con datos de prueba y documentarse con capturas de resultados reales.

## 11. Solución de problemas frecuentes

**Error de autenticación PostgreSQL:** revisar `DB_USER` y `DB_PASSWORD` en `.env`.

**Connection refused:** comprobar que PostgreSQL esté iniciado y que el puerto de `DB_URL` sea correcto.

**Base de datos inexistente:** crear `rinconcito` en pgAdmin y aplicar `schema.sql` sobre una base vacía.

**Java o Maven no reconocido:** revisar la instalación y las variables de entorno `JAVA_HOME` y `PATH`.

**Tablas no encontradas:** comprobar que el esquema de base de datos fue creado correctamente.

**Puerto 8080 ocupado:** cerrar la otra aplicación o configurar otro puerto mediante la variable `PORT`.

**Error al iniciar sesión:** verificar que el usuario administrador haya sido creado correctamente al arrancar el sistema.

## 12. Colaboración mediante GitHub

Cada integrante debe desarrollar sus tareas en una rama personal.

Ejemplo:

```powershell
git switch main
git pull --ff-only origin main
git switch -c aporte-integrante-funcionalidad
```

Después de implementar y probar los cambios:

```powershell
git add .
git commit -m "feat: descripcion del aporte"
git push -u origin aporte-integrante-funcionalidad
```

Antes de utilizar `git add .`, comprobar con `git status` que no existan archivos privados o accidentales.

Finalmente, crear un Pull Request hacia `main` y solicitar la revisión del equipo.

## 13. Evidencias de desarrollo

Para acreditar el trabajo realizado se pueden adjuntar:

- Capturas de pruebas JUnit aprobadas.
- Capturas de PostgreSQL con consultas ejecutadas.
- Capturas del sistema web funcionando.
- Enlaces a commits de cada integrante.
- Pull Requests con revisiones y cambios integrados.
- Registro de las funcionalidades demostradas.

No subir contraseñas, información privada ni archivos de configuración locales.

## Responsable de integración y documentación

**Ponce Sandoval, David Esaú**

Responsabilidades: integración MVC, configuración PostgreSQL, guía de instalación, verificación del sistema y revisión de Pull Requests del equipo.

**Proyecto:** Rinconcito Minimercado — Curso Integrador I — APF3.