# UserPulse

UserPulse is a Java and Spring Boot application for asynchronous user activity processing and analytics. It uses Apache Kafka for event streaming, PostgreSQL for persistent storage, and REST APIs for submitting activities and retrieving analytics.

## Features

- **Event Streaming:** Publishes validated activity events to Kafka using `userId` as the message key across three partitions.
- **Event Idempotency:** Uses unique event IDs and atomic PostgreSQL inserts to prevent duplicate events from being persisted or counted.
- **Activity Persistence & Analytics:** Stores activity events in PostgreSQL and calculates event counts through database aggregation.
- **Failed Login Detection:** Detects three consecutive failed login attempts and resets the counter after a successful login.
- **Error Handling:** Implements bounded retries with fixed backoff and routes malformed or unrecoverable messages to a dead-letter topic (`user-activity.DLT`).

## Tech Stack

Java 17 · Spring Boot · Apache Kafka · PostgreSQL · Spring Data JPA · Maven · Docker Compose · JUnit 5 · Mockito

## Quick Start

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
| `GET` | `/hello` | Check application availability |
| `GET` | `/actuator/health` | Check application health |

## Tests

PostgreSQL must be running locally. Kafka integration tests use Embedded Kafka.

Run the test suite:

```powershell
.\mvnw.cmd test
```