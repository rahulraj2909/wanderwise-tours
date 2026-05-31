# WanderWise — Tours & Attractions Platform

Multi-service marketplace for tours and attractions: catalog with vendor ingestion, customer booking UI, and payments.

**Stack:** Java 17 · Spring Boot 3.3.5 · Maven · JPA/H2 (local) · Redis (optional, `full` profile) · Stripe (optional; mock checkout by default)

## Modules

| Module | Port | Role |
|--------|------|------|
| `wanderwise-catalog-ingestion-service` | **8081** | Catalog DB, vendor feeds, reviews, admin portal |
| `wanderwise-booking-service` | **8080** | Customer UI, auth, bookings, payments |
| `wanderwise-common` | — | Shared DTOs and internal API contracts |

Booking proxies public catalog APIs to 8081; services communicate via an internal API (`X-Internal-Secret`).

## Quick start (H2, local)

**Requirements:** JDK 17+, Maven 3.9+

```bash
git clone https://github.com/rahulraj2909/wanderwise-tours.git
cd wanderwise-tours

# Terminal 1 — start catalog first
mvn -pl wanderwise-catalog-ingestion-service spring-boot:run "-Dspring-boot.run.profiles=h2"

# Terminal 2 — booking + customer UI
mvn -pl wanderwise-booking-service spring-boot:run "-Dspring-boot.run.profiles=h2"
```

| URL | Purpose |
|-----|---------|
| http://localhost:8080/ | Customer site |
| http://localhost:8081/admin/login.html | Admin portal |

**Demo login (booking):**

| Email | Password |
|-------|----------|
| customer@tours.demo | demo123 |
| admin@tours.demo | demo123 |

Admin portal secret: `wanderwise-admin`

H2 data is stored under `./data/catalog` and `./data/booking` (gitignored). If listings are empty, ensure catalog is running on 8081 before opening 8080.

## Tests & build

```bash
mvn test
mvn clean package -DskipTests
```

## Author

Rahul Kumar — [rahulraj2909@gmail.com](mailto:rahulraj2909@gmail.com)
