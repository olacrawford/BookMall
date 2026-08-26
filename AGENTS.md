# Repository Guidelines

## Project Structure & Module Organization

The Git root is this directory.

- `BookMall/`: Spring Cloud Alibaba backend:
  - `bookmall-common`: shared `Result`, `PageResult`, errors, and exceptions.
  - `bookmall-auth` (8060): registration, login, JWT, and user address management.
  - `bookmall-book` (8070): books, categories, pagination, Redis, and Sentinel.
  - `bookmall-cart` (8083): cart items with quantity/selection and Feign validation against the book service.
  - `bookmall-order` (8050): direct/cart orders and OpenFeign calls to the book and cart services.
  - `bookmall-gateway` (8080): routing, JWT validation, and `X-User-Id`.
- `front/`: Vue 3 + Vite app under `front/src/`, including the cart page at `front/src/views/CartView.vue`.
- `sql/sql.txt`: MySQL schema and seed data.
- `sql/updates/`: incremental SQL scripts for existing environments.
- `nacos-config/`: per-service config and `publish.sh`.
- `说明文档/`: detailed module and deployment docs.

## Build, Test, and Development Commands

Use `mvn -f BookMall/pom.xml -q clean package` to compile all backend modules, and `mvn -f BookMall/pom.xml -q test` to run backend tests. Start one service with `mvn -f BookMall/pom.xml -pl bookmall-auth -am spring-boot:run`; replace the module name as needed. Start auth, book, cart, and order before the gateway.

For frontend, run `cd front && npm install && npm run dev` to start Vue at `http://localhost:5173`, or `npm run build` to produce the build. Apply database changes by running `sql/sql.txt` and `sql/updates/*.sql` against MySQL. Publish config changes with `cd nacos-config && bash publish.sh`.

## Coding Style & Naming Conventions

Backend: use Java 17, UTF-8, and 4-space indentation. Follow existing packages: `controller`, `service`, `service.impl`, `mapper`, `entity`, `dto`, `vo`, `config`, `client`, `filter`, `util`. Name classes by role, like `BookCreateRequest` or `AuthGlobalFilter`. Keep controllers thin, validate DTOs, return `Result<T>` / `PageResult<T>`, and throw `BusinessException` with `ErrorCode`. No linter is configured; match the surrounding code.

Frontend: keep requests in `front/src/api/`, views in `front/src/views/`, and use the existing Vue 3 SFC style. New database changes go in `sql/updates/` and align with backend entities and mappers.

## Testing Guidelines

Backend tests are not present yet. Add JUnit 5 under `BookMall/<module>/src/test/java/com/bookmall/<module>`, plus `spring-boot-starter-test` in that module's `pom.xml`. Name tests `method_expectedBehavior_whenCondition`, e.g. `createOrder_snapshotPrice_whenBookExists`. Run targeted tests with `mvn -f BookMall/pom.xml -pl <module> -am test`. No frontend test runner is configured; verify with `npm run dev`.

## Commit & Pull Request Guidelines

Use scoped Conventional Commits like `feat(auth)`, `docs`, or `chore`. Keep subjects lowercase and focused. In PRs, describe changes, show verification steps, and reference related issues. Include related backend, frontend, SQL, and config changes for cross-layer features.

## Documentation Sync

Update `README.md`, `说明文档/`, `sql/sql.txt`, `nacos-config/*.yaml`, and this file in the same change as related code, schema, or config modifications. Keep implementation and documentation in one PR.

## Security & Configuration

Keep real credentials out of Git. Keep local ports and service names in `application.yml`; DB, Redis, and JWT values belong in `nacos-config/*.yaml`. Never let downstream services trust client-supplied `X-User-Id`; only the gateway filter should set it.



