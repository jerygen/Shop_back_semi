# Customer Product Query Work Summary

## Scope

Customer product query functionality was implemented and adjusted around the following rules.

- Product list and product detail queries are public APIs.
- Request-side lookup data uses `ProductReq`.
- Response data uses `ProductRes`.
- Internal product code `productId` is used for lookup but is not exposed in `ProductRes`.

## API Endpoints

### Product List

- Method: `GET`
- Path: `/api/products`
- Auth: public
- Response body type: `ApiResponse<List<ProductRes>>`

Flow:

```text
CustomerController.getAllProducts()
-> CustomerService.findProductAll()
-> ProductRepository.findAll()
-> Product entity list converted to ProductRes list
```

### Product Detail

- Method: `GET`
- Path: `/api/products/{productId}`
- Auth: public
- Request lookup DTO: `ProductReq`
- Response body type: `ApiResponse<ProductRes>`

Flow:

```text
CustomerController.getProduct(productId)
-> ProductReq created with productId
-> CustomerService.findProduct(ProductReq)
-> ProductRepository.findByProductId(productId)
-> Product entity converted to ProductRes
```

## Main Code Changes

### `CustomerController`

File: `src/main/java/web/mvc/controller/CustomerController.java`

- Changed list response from `List<ProductReq>` to `List<ProductRes>`.
- Added detail query endpoint: `/api/products/{productId}`.
- Detail query creates a `ProductReq` and sets the path variable `productId`.

### `CustomerService`

File: `src/main/java/web/mvc/service/CustomerService.java`

- Changed `findProductAll()` return type to `List<ProductRes>`.
- Added `findProduct(ProductReq productReq)` for detail lookup.

### `CustomerServiceImpl`

File: `src/main/java/web/mvc/service/CustomerServiceImpl.java`

- Converts `Product` entities to `ProductRes` using `ProductRes::new`.
- Throws `ProductException(ErrorCode.PRODUCT_NOT_FOUND)` when no products exist or a detail product cannot be found.

### `ProductRepository`

File: `src/main/java/web/mvc/repository/ProductRepository.java`

- Changed `findByProductId(String productId)` return type from `List<Product>` to `Optional<Product>`.
- This matches the unique `productId` column and simplifies not-found handling.

### `ProductReq`

File: `src/main/java/web/mvc/dto/request/ProductReq.java`

- Kept as the request-side DTO.
- `toProduct()` now converts the current DTO instance instead of receiving another `ProductReq` parameter.
- Lombok annotations currently used: `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@Builder`.

### `ProductRes`

File: `src/main/java/web/mvc/dto/response/ProductRes.java`

- Used as the response-side DTO.
- `productId` was intentionally removed because codes like `A10` are internal lookup values and do not need to be shown to customers.
- Current response fields:
  - `productNo`
  - `productName`
  - `stock`
  - `price`
  - `description`

### `SecurityConfig`

File: `src/main/java/web/mvc/config/SecurityConfig.java`

- Added public access for customer product query APIs.

```java
.requestMatchers(HttpMethod.GET, "/api/products", "/api/products/**").permitAll()
```

## Test Changes

### `CustomerTests`

File: `src/test/java/web/mvc/CustomerTests.java`

Added test data setup and query tests.

#### `createProducts()`

- Clears existing product data with `productRepository.deleteAll()`.
- Inserts two products:
  - `A10`, `Keyboard`, `30000`, stock `10`
  - `B20`, `Mouse`, `15000`, stock `20`
- Verifies that two products were saved.

#### `getAllProducts()`

- Calls `createProducts()` first.
- Calls `customerService.findProductAll()`.
- Verifies two products are returned.
- Verifies product names contain `Keyboard` and `Mouse`.

#### `getProduct()`

- Calls `createProducts()` first.
- Builds `ProductReq` with `productId = "A10"`.
- Calls `customerService.findProduct(productReq)`.
- Verifies returned product fields.

## Verification

The following commands were run with a temporary `JAVA_HOME` value.

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-21.0.10'
.\mvnw.cmd -q -DskipTests compile
.\mvnw.cmd -q test-compile
```

Results:

- Main source compile: passed
- Test source compile: passed

Running `CustomerTests` failed because the test MySQL database was not reachable.

Observed failure:

```text
Communications link failure
```

Required before running integration tests:

```powershell
docker compose -f docker-compose.test.yml up -d
$env:JAVA_HOME='C:\Program Files\Java\jdk-21.0.10'
.\mvnw.cmd -Dtest=CustomerTests test
```

## Notes

- `ProductRes` intentionally does not expose `productId`.
- `productId` remains in `ProductReq`, `Product`, and `ProductRepository` because it is still used as the detail lookup key.
- Current tests are integration-style tests because they load the Spring context and require MySQL.
