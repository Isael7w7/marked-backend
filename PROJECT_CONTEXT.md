# Contexto del proyecto — `market.backend`

> Documento autocontenido para entregar a un asistente/IA que no conoce el proyecto.
> Última actualización: 2026-11-16.

---

## 1. Resumen

API REST de gestión de un mercado/tienda (productos, categorías, clientes y compras), escrita en **Java 17 + Spring Boot 4.1.1 + Gradle**, con persistencia en **PostgreSQL** vía **Spring Data JPA / Hibernate** y mapeo entre capas con **MapStruct**.

- Nombre del proyecto Gradle: `market.backend`
- Grupo: `com.merida.tecnm.mx` — versión `0.0.1-SNAPSHOT`
- Paquete base: `com.merida.tecnm.mx.market.backend`
- Puerto por defecto: `8080` (no se configura `server.port`, así que es el de Spring Boot)
- Perfil activo por defecto: `dev`
- No hay autenticación, autorización, CORS, validación (`jakarta.validation`), ni manejo global de errores.
- No hay migraciones (Flyway/Liquibase); el esquema se genera/actualiza con Hibernate (`ddl-auto=update`).

---

## 2. Stack y dependencias

Declaradas en `build.gradle`:

| Dependencia | Versión | Uso |
|---|---|---|
| `org.springframework.boot:spring-boot-starter-webmvc` | gestionada por Boot 4.1.1 | API REST |
| `org.springframework.boot:spring-boot-starter-data-jpa` | gestionada por Boot 4.1.1 | JPA / Hibernate |
| `org.postgresql:postgresql` | gestionada (`runtimeOnly`) | Driver PostgreSQL |
| `org.mapstruct:mapstruct` | 1.6.3 | Mapeo dominio ↔ entidad |
| `org.mapstruct:mapstruct-processor` | 1.6.3 | `annotationProcessor` |
| `testImplementation spring-boot-starter-webmvc-test` | — | Test de contexto |
| `testRuntimeOnly org.junit.platform:junit-platform-launcher` | — | JUnit 5 |

Plugins: `java`, `org.springframework.boot 4.1.1`, `io.spring.dependency-management 1.1.7`.
Java toolchain: **17**. Repositorio: Maven Central.

> Nota: Spring Boot 4 / Spring Framework 7 usan `spring-boot-starter-webmvc` (no `-web`). El proyecto es multi-módulo solo en apariencia: `settings.gradle` define un único `rootProject.name = 'market.backend'`.

---

## 3. Configuración

### `src/main/resources/application.properties`
```properties
spring.application.name=market.backend
spring.profiles.active=dev
```

### `src/main/resources/application-dev.properties`
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/market-backend
spring.datasource.username=postgres
spring.datasource.password=market123
spring.datasource.driver-class-name=org.postgresql.Driver

spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
```
> Credenciales de desarrollo en texto plano (dato sensible a tener en cuenta). La base debe llamarse `market-backend` en `localhost:5432`.

### `src/main/resources/application-pdn.properties`
Vacío (perfil de producción sin configurar).

### Ejecución
```bash
./gradlew bootRun          # arrancar en dev
./gradlew build            # compilar + test
./gradlew test             # solo tests
```
El test `ApplicationTests.contextLoads` es un `@SpringBootTest` completo: **requiere una base PostgreSQL accesible** para pasar (no usa H2 ni Testcontainers).

---

## 4. Arquitectura

Se sigue una **arquitectura hexagonal (puertos y adaptadores)** con tres capas dentro de `com.merida.tecnm.mx.market.backend`:

```
web (adaptador de entrada)
   └── domain.service (casos de uso)
          └── domain.repository (puertos / interfaces)
                 ▲
                 └── persistence (adaptador de salida: JPA + MapStruct)
```

Flujo de una petición:

```
HTTP → Controller (web) → Service (domain.service)
     → Repository interface (domain.repository)
     → RepositoryAdapter (persistence, @Repository)
     → CrudRepository (Spring Data) → PostgreSQL
```

Mapeos:

```
Producto (Entity) ⇄ Product (Domain)      ← ProductMapper (usa CategoryMapper)
Compra   (Entity) ⇄ Purchase (Domain)     ← PurchaseMapper (usa PurchaseItemMapper)
CompraProducto ⇄ PurchaseItem             ← PurchaseItemMapper (usa ProductMapper)
Categoria ⇄ Category                      ← CategoryMapper
```

Convenciones de nombres deliberadas:
- Las **entidades JPA** y las tablas/columnas están en **español** (`Producto`, `Compra`, `id_producto`, `precio_venta`).
- Los **objetos de dominio, servicios y controllers** están en **inglés** (`Product`, `PurchaseService`, `/products`).
- Los `CrudRepository` de Spring Data están en paquete `persistence.crud` y terminan en `CrudRepository` (`ProductoCrudRepository`, `CompraCrudRepository`).
- Los adaptadores terminan en `RepositoryAdapter` y se anotan `@Repository`.
- Todos los mappers son interfaces MapStruct con `componentModel = "spring"` (se inyectan como beans).

Inyección de dependencias: **por constructor** en todas las clases (sin `@Autowired` en campos).

---

## 5. Endpoints

Base URL local: `http://localhost:8080`. No hay prefijo global (`server.servlet.context-path` no está definido) ni versión de API.

### 5.1 Health / demo

| Método | Ruta | Respuesta | Notas |
|---|---|---|---|
| GET | `/saludar/saludo` | `200` `text/plain`: `hello Word!` | Controller de prueba (`helloWordController`). Mantiene la errata "Word". |

### 5.2 Productos — `ProductController` (`/products`)

| Método | Ruta | Body | Respuestas |
|---|---|---|---|
| GET | `/products` | — | `200` lista de productos (puede ser vacía) |
| GET | `/products/{id}` | — | `200` producto · `404` si no existe |
| GET | `/products/category/{categoryId}` | — | `200` lista de productos de esa categoría · `404` si la lista está vacía |
| POST | `/products` | `Product` JSON | `201` producto creado |
| DELETE | `/products/{id}` | — | `204` borrado · `404` si no existía |

Comportamientos relevantes:
- `GET /products/{id}` y `DELETE /products/{id}` usan el `id` como `int` en la ruta.
- `DELETE` es un **borrado físico** (`crudRepository.deleteById`), no lógico; el campo `estado`/`active` existe pero el borrado no lo usa.
- `GET /products/category/{categoryId}` devuelve `404` (no `200` con lista vacía) cuando no hay productos, porque el adaptador convierte lista vacía en `Optional.empty()`.
- `POST` no valida campos; un `id` enviado se respeta en el mapeo (riesgo de `merge`/sobrescritura).
- Existe en dominio un método `getScarceProducts(int quantity)` (productos con stock menor a `quantity` y activos), implementado en `ProductRepositoryAdapter` con `findByCantidadStockLessThanAndEstado`, **pero no está expuesto por ningún endpoint**.

JSON de producto (nombres reales del bean de dominio):

```json
{
  "productId": 1,
  "name": "Leche entera 1L",
  "barcode": "7501000000000",
  "categoryId": 2,
  "price": 24.5,
  "stock": 40,
  "active": true,
  "category": { "categoryid": 2, "category": "Lácteos", "active": true }
}
```
> `category` se rellena en las lecturas (mapeo `Producto.categoria → Product.category`) y se ignora en la escritura (`ProductMapper.toProducto` hace `target = "categoria", ignore = true`). En `POST`, lo que define la categoría es `categoryId`.
> Ojo con las mayúsculas: `productId` (camel) pero `categoryid` (todo minúsculas) dentro de `Category`.

### 5.3 Compras — `PurchaseController` (`/purchases`)

| Método | Ruta | Body | Respuestas |
|---|---|---|---|
| GET | `/purchases` | — | `200` lista de compras (con sus ítems) |
| GET | `/purchases/client/{clientId}` | — | `200` compras del cliente ordenadas por fecha desc · `404` si no tiene compras |
| POST | `/purchases` | `Purchase` JSON | `201` compra creada (con ítems) |

Comportamientos relevantes:
- `clientId` llega como `String` en el controller y se convierte con `Integer.parseInt` en `PurchaseRepositoryAdapter.getByClient`. Un valor no numérico produce `NumberFormatException` → **500**.
- No hay endpoint para actualizar ni borrar compras, ni para obtener una compra por id.
- `POST` es transaccional (`@Transactional` en el adaptador) y:
  1. Mapea `Purchase → Compra` (ignora `cliente`).
  2. Para cada ítem, exige `id.idProducto` (en JSON: `productId`); si falta lanza `IllegalArgumentException` → **500**.
  3. Busca el `Producto` y lanza `EntityNotFoundException("Product not found: X")` → **500** si no existe.
  4. Asigna la compra a cada ítem y guarda en cascada (`CascadeType.ALL` + `orphanRemoval`).
- **No** se descuenta stock, **no** se calcula `total`, y **no** se autocompleta `fecha`: si no se envían, quedan `null`.
- La PK de `compras_productos` es compuesta (`id_compra`, `id_producto`), por lo que **no puede haber dos líneas del mismo producto en una misma compra**.

JSON de compra (entrada aceptada por `POST`):

```json
{
  "clientId": 7,
  "date": "2026-11-16T10:30:00",
  "paymentMethod": "TARJETA",
  "comment": "Entrega en tienda",
  "state": "PAGADA",
  "items": [
    { "productId": 1, "quantity": 2, "total": 49.0, "active": true, "date": "2026-11-16T10:30:00" }
  ]
}
```

Respuesta de `GET`/`POST`:

```json
{
  "purchaseId": 15,
  "clientId": 7,
  "date": "2026-11-16T10:30:00",
  "paymentMethod": "TARJETA",
  "comment": "Entrega en tienda",
  "state": "PAGADA",
  "items": [
    {
      "productId": 1,
      "quantity": 2,
      "total": 49.0,
      "active": true,
      "date": "2026-11-16T10:30:00",
      "product": {
        "productId": 1, "name": "Leche entera 1L", "barcode": "7501000000000",
        "categoryId": 2, "price": 24.5, "stock": 40, "active": true,
        "category": { "categoryid": 2, "category": "Lácteos", "active": true }
      }
    }
  ]
}
```
> En la práctica `product` puede venir `null` si la relación no está cargada; no hay `@JsonIgnore` ni DTOs de salida, se serializan directamente los objetos de dominio.

---

## 6. Modelo de datos

No existe `schema.sql`; el esquema se infiere de las entidades con `ddl-auto=update`. Tablas y columnas:

### `categoria` — `Categoria`
| Columna | Tipo Java | Notas |
|---|---|---|
| `id_categoria` | `Integer` | PK, `IDENTITY` |
| `descripcion` | `String` | |
| `estado` | `Boolean` | activo/inactivo |

### `productos` — `Producto`
| Columna | Tipo Java | Notas |
|---|---|---|
| `id_producto` | `Integer` | PK, `IDENTITY` |
| `nombre` | `String` | |
| `id_categoria` | `Integer` | FK lógica + `@ManyToOne` a `Categoria` (`insertable=false, updatable=false`) |
| `codigo_barras` | `String` | |
| `precio_venta` | `Double` | |
| `cantidad_stock` | `Integer` | |
| `estado` | `Boolean` | |

### `clientes` — `Cliente`
| Columna | Tipo Java |
|---|---|
| `id` | `Integer` (PK, **sin** `@GeneratedValue`) |
| `nombre` | `String` |
| `apellidos` | `String` |
| `celular` | `String` |
| `direccion` | `String` |
| `correo_electronico` | `String` |

> `Cliente` solo se usa como relación de lectura dentro de `Compra`. No hay entidad/repositorio de clientes expuesto en dominio ni endpoints de clientes.

### `compras` — `Compra`
| Columna | Tipo Java | Notas |
|---|---|---|
| `id_compra` | `Integer` | PK, `IDENTITY` |
| `id_cliente` | `Integer` | FK lógica + `@ManyToOne` a `Cliente` (`insertable=false, updatable=false`) |
| `fecha` | `LocalDateTime` | no se autogenera |
| `medio_pago` | `String` | |
| `comentario` | `String` | |
| `estado` | `String` | en compras es texto; en productos/categorías/ítems es booleano |
| `productos` | `List<CompraProducto>` | `@OneToMany(mappedBy="compra")`, `CascadeType.ALL`, `orphanRemoval=true` |

### `compras_productos` — `CompraProducto` (PK compuesta `CompraProductoPK`)
| Columna | Tipo Java | Notas |
|---|---|---|
| `id_compra` | `Integer` | PK parte 1, `@MapsId("idCompra")` |
| `id_producto` | `Integer` | PK parte 2, `@MapsId("idProducto")` |
| `cantidad` | `Integer` | |
| `total` | `Double` | |
| `estado` | `Boolean` | |
| `fecha` | `LocalDateTime` | |

`CompraProductoPK` implementa `equals`/`hashCode` sobre ambas columnas y es `@Embeddable` + `Serializable`.

Relaciones: `Categoria 1—N Producto`, `Cliente 1—N Compra`, `Compra 1—N CompraProducto N—1 Producto`.

---

## 7. Inventario de archivos

```
market.backend/
├── build.gradle                  # plugins Spring Boot 4.1.1, Java 17, deps (JPA, WebMVC, Postgres, MapStruct)
├── settings.gradle               # rootProject.name = 'market.backend'
├── gradlew / gradlew.bat         # wrapper
├── gradle/wrapper/…              # gradle-wrapper.jar + .properties
├── HELP.md                       # guía autogenerada de Spring Boot (no relevante)
├── PROJECT_CONTEXT.md            # este documento
├── .gitignore / .gitattributes
├── .idea/…                       # configuración de IntelliJ (incluye data sources locales)
└── src/
    ├── main/
    │   ├── java/com/merida/tecnm/mx/market/backend/
    │   │   ├── Application.java                          # @SpringBootApplication / main
    │   │   ├── helloWordController.java                  # GET /saludar/saludo (demo)
    │   │   ├── domain/
    │   │   │   ├── Category.java                         # categoryid, category, active
    │   │   │   ├── Product.java                          # productId, name, barcode, categoryId, price, stock, active, category
    │   │   │   ├── Purchase.java                         # purchaseId, clientId, date, paymentMethod, comment, state, items
    │   │   │   ├── PurchaseItem.java                     # productId, quantity, total, active, date, product
    │   │   │   ├── repository/
    │   │   │   │   ├── ProductRepository.java            # puerto: getAll, getByCategory, getScarceProducts, getProduct, save, delete
    │   │   │   │   └── PurchaseRepository.java            # puerto: getAll, getByClient, save
    │   │   │   └── service/
    │   │   │       ├── ProductService.java               # @Service; CRUD + delete booleano
    │   │   │       └── PurchaseService.java              # @Service; listar, por cliente, guardar
    │   │   ├── persistence/
    │   │   │   ├── ProductRepositoryAdapter.java         # @Repository implements ProductRepository
    │   │   │   ├── PurchaseRepositoryAdapter.java        # @Repository implements PurchaseRepository, transaccional
    │   │   │   ├── crud/
    │   │   │   │   ├── ProductoCrudRepository.java        # CrudRepository<Producto,Integer> + queries derivadas
    │   │   │   │   └── CompraCrudRepository.java          # CrudRepository<Compra,Integer> + findByIdClienteOrderByFechaDesc
    │   │   │   ├── entity/
    │   │   │   │   ├── Categoria.java
    │   │   │   │   ├── Cliente.java
    │   │   │   │   ├── Compra.java
    │   │   │   │   ├── CompraProducto.java
    │   │   │   │   ├── CompraProductoPK.java
    │   │   │   │   └── Producto.java
    │   │   │   └── mapper/
    │   │   │       ├── CategoryMapper.java
    │   │   │       ├── ProductMapper.java
    │   │   │       ├── PurchaseMapper.java
    │   │   │       └── PurchaseItemMapper.java
    │   │   └── web/controller/
    │   │       ├── ProductController.java                # /products
    │   │       └── PurchaseController.java               # /purchases
    │   └── resources/
    │       ├── application.properties                    # perfil dev
    │       ├── application-dev.properties                # datasource PostgreSQL local
    │       └── application-pdn.properties                # vacío
    └── test/java/com/merida/tecnm/mx/market/backend/
        └── ApplicationTests.java                         # @SpringBootTest contextLoads
```

Solo existen **dos** controllers funcionales y **dos** servicios. No hay paquete `dto`, `exception`, `config`, `security` ni `util`.

---

## 8. Contratos internos clave

### `ProductRepository` (puerto)
```java
List<Product> getAll();
Optional<List<Product>> getByCategory(int categoryId);
Optional<List<Product>> getScarceProducts(int quantity);
Optional<Product> getProduct(int productId);
Product save(Product product);
void delete(int productId);
```

### `PurchaseRepository` (puerto)
```java
List<Purchase> getAll();
Optional<List<Purchase>> getByClient(String clienteId);   // String → parseInt dentro del adaptador
Purchase save(Purchase purchase);
```

### Queries derivadas de Spring Data
```java
// ProductoCrudRepository
List<Producto> findByIdCategoriaOrderByNombreAsc(Integer idCategoria);
Optional<List<Producto>> findByCantidadStockLessThanAndEstado(Integer cantidadStock, Boolean estado);

// CompraCrudRepository
List<Compra> findByIdClienteOrderByFechaDesc(Integer idCliente);
```

### Reglas de mapeo MapStruct (resumen)
- `CategoryMapper`: `idCategoria↔categoryid`, `descripcion↔category`, `estado↔active`.
- `ProductMapper`: `idProducto↔productId`, `nombre↔name`, `codigoBarras↔barcode`, `idCategoria↔categoryId`, `precioVenta↔price`, `cantidadStock↔stock`, `estado↔active`, `categoria→category` (solo lectura; ignorado al inverso).
- `PurchaseMapper`: `idCompra↔purchaseId`, `idCliente↔clientId`, `fecha↔date`, `medioPago↔paymentMethod`, `comentario↔comment`, `estado↔state`, `productos↔items`; `cliente` ignorado al inverso.
- `PurchaseItemMapper`: `id.idProducto↔productId`, `cantidad↔quantity`, `total↔total`, `estado↔active`, `fecha↔date`, `producto↔product`; al inverso ignora `id.idCompra`, `compra` y `producto`.

---

## 9. Estado actual, limitaciones y deuda técnica

Cosas que un asistente debe saber antes de tocar el código:

1. **Sin gestión de errores**: no hay `@ControllerAdvice`. `NumberFormatException`, `IllegalArgumentException`, `EntityNotFoundException` y errores de FK devuelven `500` con el formato por defecto de Spring.
2. **Sin validación**: no se usan `@Valid`, `@NotNull`, etc. Se aceptan payloads incompletos.
3. **Sin DTOs**: las entidades de dominio se exponen directamente; los nombres de JSON dependen de los getters de dominio.
4. **Sin paginación**: `getAll()` devuelve todo.
5. **Productos**: `getScarceProducts` está implementado pero no expuesto. El delete es físico y no idempotente en el sentido de `active`.
6. **Compras**: no descuenta stock, no calcula totales, no autogenera fecha, no valida que `clientId` exista (la FK lo hará fallar al guardar). PK compuesta impide repetir producto en una compra. No hay update/delete.
7. **`Cliente` / `Categoria`**: sin CRUD ni endpoints; `clientes.id` no es autogenerado.
8. **Categoría inexistente**: `POST /products` con `categoryId` inválido depende de la FK física de la BD; no se valida en código.
9. **Perfil `pdn` vacío**: no hay configuración de producción ni externalización de credenciales.
10. **Tests**: solo `contextLoads`, que necesita PostgreSQL real levantado. No hay tests de controllers, servicios ni mappers.
11. **`helloWordController`** rompe la convención (`helloWordController` en minúscula, ruta `/saludar/saludo`, texto con errata) — es código de ejemplo.
12. **Nombres de tablas con `id_` / `nombre` sin acentos**: los nombres de columnas inferidos por Hibernate son los de los campos; no hay `@Column` para todas, pero coinciden con el esquema esperado (`descripcion`, `estado`, etc.).

---

## 10. Cómo extender el proyecto (guía rápida de convenciones)

Para añadir un recurso nuevo (por ejemplo, categorías):

1. `domain/CategoriaX.java` — POJO de dominio en inglés, con getters/setters.
2. `domain/repository/XRepository.java` — interfaz puerto.
3. `domain/service/XService.java` — `@Service`, inyección por constructor.
4. `persistence/XRepositoryAdapter.java` — `@Repository`, implementa el puerto, usa `CrudRepository` + `Mapper`.
5. `persistence/crud/XC...CrudRepository.java` — `extends CrudRepository<Entidad, Integer>`.
6. `persistence/mapper/XMapper.java` — `@Mapper(componentModel = "spring", uses = {...})` con `@Mapping` explícito para cada campo cuyo nombre difiera.
7. `web/controller/XController.java` — `@RestController` + `@RequestMapping("/x")`, devolver `ResponseEntity`.

Mantener:
- Entidades y columnas en español; dominio/servicios/endpoints en inglés.
- Inyección por constructor y campos `final`.
- `Optional` en puertos para búsquedas que pueden no devolver nada; listas vacías se traducen a `Optional.empty()` (y por tanto a `404`) en los adaptadores existentes.
- `@Transactional` en el adaptador cuando se escriben varias entidades relacionadas.

---

## 11. Preguntas frecuentes de contexto

- **¿Dónde está la lógica de negocio?** En los `service`, pero es casi toda delegación; las reglas reales (existencia de producto, cascada de ítems) están en `PurchaseRepositoryAdapter.save`.
- **¿Dónde se define el esquema?** No hay script: Hibernate lo crea/actualiza desde las entidades (`ddl-auto=update`).
- **¿Cómo se conectan las capas?** Spring inyecta `ProductService`/`PurchaseService` en los controllers, y los adaptadores implementan las interfaces de `domain.repository`, que los servicios reciben.
- **¿Hay frontend?** No. Solo la API.
- **¿Qué idioma usa el proyecto?** Código de dominio/API en inglés; entidades, tablas y algunos comentarios/mensajes de error en español.
- **¿Se puede correr sin base de datos?** No tal cual: el arranque crea el `EntityManagerFactory` y falla si no hay PostgreSQL accesible en `localhost:5432/market-backend`.
