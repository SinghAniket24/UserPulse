# UserPulse

UserPulse is a Java and Spring Boot application for asynchronous user activity processing and analytics. It uses Apache Kafka for event streaming, PostgreSQL for persistent storage, and REST APIs for submitting activities and retrieving analytics.

## Features

- **Event Streaming:** Publishes activity events to Kafka using `userId` as the message key across three partitions, preserving per-user event ordering.
- **Event Idempotency:** Uses unique event IDs and atomic PostgreSQL inserts to prevent duplicate events from being stored or counted.
- **Activity Persistence & Analytics:** Stores activity events in PostgreSQL and calculates event counts using database aggregation.
- **Failed Login Detection:** Tracks consecutive failed login attempts in memory, logs an alert after three failures, and resets the counter after a successful login.
- **Error Handling:** Retries processing failures and routes unrecoverable messages to a dead-letter topic (`user-activity.DLT`). Invalid event IDs are sent directly to the DLT without retries.

## Tech Stack

Java · Spring Boot · Apache Kafka · PostgreSQL · Docker Compose

## Quick Start

**Prerequisites:** Java 17 and Docker.

Configure your environment:

```powershell
Copy-Item .env.example .env
```

Start Kafka and PostgreSQL:

```powershell
docker compose up -d
```

Run the application:

```powershell
.\mvnw.cmd spring-boot:run
```

The API is available at `http://localhost:8080`.

## API Endpoints

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/activities` | Submit a user activity event |
| `GET` | `/api/analytics` | Retrieve aggregated event counts |
| `GET` | `/actuator/health` | Check application health |

### Example Request

`POST /api/activities`

```json
{
  "userId": "user-101",
  "eventType": "LOGIN_SUCCESS"
}
```

An optional `eventId` can be supplied for idempotency. If omitted, the application generates one. Successful requests return `202 Accepted`.

## Kafka & Error Handling

- Main topic: `user-activity` (3 partitions).
- Consumer group: `userpulse-consumer`.
- Dead-letter topic: `user-activity.DLT`.
- Retry policy: 2 retries with a 1-second backoff.
- Events with missing or blank event IDs, along with deserialization failures, are routed directly to the DLT without retries.

## Tests

PostgreSQL must be running locally. Kafka integration tests use Embedded Kafka.

Run the test suite:

```powershell
.\mvnw.cmd test
```