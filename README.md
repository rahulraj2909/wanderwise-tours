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

## Deploy a public URL (Render + GitHub)

GitHub stores code and runs CI; it does **not** host Spring Boot apps. Use [Render](https://render.com) (free tier) connected to this repo.

1. Sign up at [render.com](https://render.com) and link your GitHub account.
2. **New → Blueprint** → select repo `rahulraj2909/wanderwise-tours` → apply `render.yaml`.
3. Wait for both services to build (first deploy ~5–10 min).
4. Open the **wanderwise-booking** service URL — that is your public customer site (`https://wanderwise-booking-xxxx.onrender.com/`).
5. Admin portal: **wanderwise-catalog** URL + `/admin/login.html` (secret: `wanderwise-admin`).

| Service | Public role |
|---------|-------------|
| `wanderwise-booking` | Customer UI + checkout (main link to share) |
| `wanderwise-catalog` | Catalog API + admin |

**Notes:** Free tier sleeps after ~15 min idle (cold start ~1 min). Data uses in-memory H2 and re-seeds on restart. Demo logins are the same as local.

If Blueprint fails on first try, deploy **wanderwise-catalog** first, copy its URL, then deploy **wanderwise-booking** with env `WANDERWISE_CATALOG_BASE_URL=https://your-catalog-url`.

## Author

Rahul Kumar — [rahulraj2909@gmail.com](mailto:rahulraj2909@gmail.com)
