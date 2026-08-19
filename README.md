# Mentour Biz — core-server

Spring Boot backend for the Mentour Biz platform (CRM, groups & lessons, attendance,
payroll, payments, BigBlueButton lessons, Telegram/WhatsApp notifications).

## Stack

| | |
|---|---|
| Java | 17 |
| Spring Boot | 3.3.4 |
| Database | PostgreSQL |
| Build | Maven (`./mvnw`) |
| Auth | Spring Security + JWT |
| Integrations | BigBlueButton, Telegram, WhatsApp, Firebase, Octo, Uzum Checkout, OFB Pay, Sello |

## Setup

1. Clone and copy the config templates:

   ```bash
   cp src/main/resources/application.yml.example src/main/resources/application.yml
   cp src/main/resources/application-prod.yml.example src/main/resources/application-local.yml
   ```

2. Fill in every `CHANGE_ME` value (DB credentials, JWT secret, integration keys).
   Set the active profile in `application.yml` (`local` / `test` / `prod`).

   > Config files with real credentials are git-ignored on purpose — never commit them.

3. Run:

   ```bash
   ./mvnw spring-boot:run
   ```

Swagger UI: `http://localhost:8888/swagger-ui.html`

## Build

```bash
./mvnw clean package -DskipTests
```

## Docker

```bash
docker build -t mentour-biz .
docker run -p 8888:8888 -v /var/www/mentour:/var/www/mentour mentour-biz
```

## Branches

- `main` — production
- `dev` — development
