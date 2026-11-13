# UserPulse

UserPulse is a Java and Spring Boot application for asynchronous user activity processing and analytics. It uses Apache Kafka for event streaming and PostgreSQL to store activity events and generate analytics.

## Features

- **Event Streaming:** Publishes activity events to Kafka using `userId` as the message key across three partitions, routing events for the same user to the same partition, where Kafka preserves their order.
- **Idempotent Processing:** Uses unique event IDs and atomic PostgreSQL inserts to prevent duplicate events from being stored or counted.
- **Activity Analytics:** Persists activity events in PostgreSQL and calculates event counts using database aggregation.
- **Failed Login Detection:** Tracks consecutive failed login attempts from persisted events, triggers an alert after three failures, and resets the streak after a successful login.
- **Error Handling:** Retries processing failures and routes unrecoverable messages to a dead-letter topic (`user-activity.DLT`). Invalid event IDs and deserialization failures are sent directly to the DLT.

## Tech Stack

Java 17 · Spring Boot · Apache Kafka · PostgreSQL · Docker Compose

## Quick Start

**Prerequisites:** Java 17 and Docker.

Configure environment variables:

```powershell
Copy-Item .env.example .env
```

Start Kafka and PostgreSQL:

```bash
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

**Example Request**

`POST /api/activities`

```json
{
  "userId": "user-101",
  "eventType": "LOGIN_SUCCESS"
}
```

An optional `eventId` can be supplied for idempotency. If omitted, the application generates one. Successful requests return `202 Accepted`.

## Kafka & Error Handling

- **Topic:** `user-activity` (3 partitions)
- **Consumer Group:** `userpulse-consumer`
- **Dead-letter Topic:** `user-activity.DLT`
- **Retry Policy:** 2 retries with a 1-second backoff.
- **Direct DLT Routing:** Invalid event IDs and deserialization failures bypass retries.

## Tests

PostgreSQL must be running locally. Kafka integration tests use Embedded Kafka.

Run the test suite:

```powershell
.\mvnw.cmd test
```
