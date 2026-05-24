# QuickBite Backend

QuickBite is a production-shaped Spring Boot backend for a modern food delivery product. It is intentionally startup-like: the core order write path is small and synchronous, payment authorization triggers a Redis-backed async workflow, order status updates are exposed through SSE, and some operational debt is visible instead of hidden behind tutorial abstractions.

## Stack

- Java 17, Gradle, Spring Boot 4
- Spring MVC REST APIs, Bean Validation, Spring Security JWT resource server
- PostgreSQL with Flyway migrations and JPA repositories
- Redis list queue for paid-order workflow dispatch
- Docker Compose for API, PostgreSQL, and Redis
- Actuator, Prometheus registry, request trace logging

## Project structure

```text
src/main/java/com/codereferee/quickbite
├── admin         # privileged restaurant and order operations
├── auth          # signup, login, token issuing DTOs/services/controllers
├── common        # API error model and exception handling
├── config        # security, JWT, async executor, bootstrap admin, trace filter
├── delivery      # delivery tracking model and customer tracking API
├── order         # order aggregate, DTOs, SSE status stream, order API
├── payment       # payment request and authorization flow
├── queue         # Redis producer, polling consumer, async order workflow
├── restaurant    # restaurant/menu model, public read API, admin write service
└── user          # users, roles, repositories
```

Key files:

- `src/main/java/com/codereferee/quickbite/config/SecurityConfig.java`
- `src/main/java/com/codereferee/quickbite/order/OrderService.java`
- `src/main/java/com/codereferee/quickbite/payment/PaymentService.java`
- `src/main/java/com/codereferee/quickbite/queue/OrderQueueConsumer.java`
- `src/main/resources/db/migration/V1__quickbite_schema.sql`
- `docker-compose.yml`

## Local setup

```bash
cp .env.example .env
docker compose up --build
```

The API listens on `http://localhost:8080`. Local Swagger UI is exposed at `/swagger-ui/index.html`, health at `/actuator/health`, and Prometheus metrics at `/actuator/prometheus`.

A basic browser console is served from `/`. It supports customer signup/login, admin sample restaurant creation, restaurant browsing, order creation, payment authorization, SSE order events, and delivery tracking calls against the same API origin.

Compose bootstraps a local admin account when `BOOTSTRAP_ADMIN_EMAIL` and `BOOTSTRAP_ADMIN_PASSWORD` are set. Remove the password variable after first use in any shared environment.

For app-only development against local PostgreSQL and Redis:

```bash
./gradlew bootRun
```

Environment variables:

| Variable | Purpose |
| --- | --- |
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | PostgreSQL connection |
| `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD` | Redis connection |
| `JWT_SECRET`, `JWT_ISSUER`, `JWT_ACCESS_TOKEN_TTL` | HMAC token signing |
| `ORDER_QUEUE_KEY` | Redis list key for paid orders |
| `BOOTSTRAP_ADMIN_EMAIL`, `BOOTSTRAP_ADMIN_PASSWORD` | one-time local admin creation |

## API examples

Create a customer:

```bash
curl -s http://localhost:8080/api/auth/signup \
  -H 'Content-Type: application/json' \
  -d '{"email":"ryu@example.com","displayName":"Ryu","password":"delivery-demo-123"}'
```

Log in as the Compose admin, then create a restaurant:

```bash
TOKEN=$(curl -s http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"admin@quickbite.local","password":"quickbite-admin-local"}' \
  | jq -r '.accessToken')

curl -s http://localhost:8080/api/admin/restaurants \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"name":"Seoul Night Noodles","address":"12 Mapo-ro, Seoul","deliveryFee":3500}'

curl -s http://localhost:8080/api/admin/restaurants/1/menu-items \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"name":"Spicy Beef Udon","description":"broth, brisket, scallion","price":12900,"available":true}'
```

Place an order with a customer token. The response includes an order in `PAYMENT_PENDING` and a payment request:

```bash
curl -s http://localhost:8080/api/orders \
  -H "Authorization: Bearer $CUSTOMER_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "restaurantId": 1,
    "deliveryAddress": "21 Teheran-ro, Gangnam-gu",
    "customerNote": "Call on arrival",
    "items": [{"menuItemId": 1, "quantity": 2}]
  }'
```

Authorize payment and enqueue the async order workflow:

```bash
curl -s http://localhost:8080/api/payments/1/authorize \
  -H "Authorization: Bearer $CUSTOMER_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"providerReference":"sandbox-auth-20260522-001"}'
```

Track status changes through SSE:

```bash
curl -N http://localhost:8080/api/orders/1/events \
  -H "Authorization: Bearer $CUSTOMER_TOKEN"
```

The browser demo uses `EventSource`, which cannot attach custom `Authorization` headers. For that one UI path the backend also accepts `?access_token=...`; this is convenient for local demos but should be replaced with a cookie-backed session, short-lived one-time stream token, or WebSocket auth handshake before production.

Read delivery tracking after the Redis consumer has accepted the paid order:

```bash
curl -s http://localhost:8080/api/deliveries/orders/1 \
  -H "Authorization: Bearer $CUSTOMER_TOKEN"
```

## Redis queue flow

1. `POST /api/orders` writes `food_orders`, `order_items`, and `payment_requests` in PostgreSQL.
2. `POST /api/payments/{id}/authorize` marks the payment authorized and the order `PAID`.
3. `OrderQueueProducer` pushes JSON to Redis list `quickbite:orders:created`.
4. `OrderQueueConsumer` polls with `RPOP`, parses `ORDER_PAID`, and calls `OrderWorkflowService`.
5. The workflow moves the order to `RESTAURANT_CONFIRMED`, creates initial `delivery_tracking`, and publishes an SSE status event.

Example queue payload:

```json
{
  "orderId": 42,
  "eventType": "ORDER_PAID",
  "attempt": 0,
  "enqueuedAt": "2026-05-22T08:30:00Z"
}
```

This is intentionally a Redis list queue, not a full outbox/stream implementation. There is no DLQ or delivery retry journal yet, so it is a useful place to discuss reliability trade-offs.

## Schema sample

Flyway owns the source schema in `V1__quickbite_schema.sql`. The main relationships are:

```sql
app_users(id, email, role)
restaurants(id, owner_id, name, delivery_fee)
menu_items(id, restaurant_id, name, price)
food_orders(id, customer_id, restaurant_id, status, total)
order_items(id, order_id, menu_item_id, quantity, line_total)
payment_requests(id, order_id, idempotency_key, status, amount)
delivery_tracking(id, order_id, status, latitude, longitude)
```

The order item row keeps menu name and price snapshots so a later menu edit does not rewrite order history.

## Notes for review

- JWT is HMAC-signed for local simplicity. A multi-service deployment would normally move to asymmetric keys or an identity provider.
- Restaurant admin writes are centralized under `/api/admin`; owner-scoped mutation rules are a natural next slice.
- The Redis consumer is idempotent for already-moved orders, but queue durability is weaker than a transactional outbox.
- Actuator and request IDs help local observability; production would add structured logs, traces, rate limiting, secret management, and payment webhook verification.
