# UserPulse

UserPulse is a Java and Spring Boot application that uses Apache Kafka for asynchronous user activity processing and provides analytics through REST APIs.

## Features

- **Event Streaming:** Publishes validated activity events to Kafka using `userId` as the message key, with three topic partitions.
- **Activity Analytics:** Aggregates event counts and detects three consecutive failed login attempts, resetting the counter after a successful login.
- **Error Handling:** Implements bounded retries with backoff and routes malformed or unrecoverable messages to a dead-letter topic (`user-activity.DLT`).

## Tech Stack

Java 17 · Spring Boot · Apache Kafka · Maven · Docker Compose · JUnit 5

## Quick Start

Start Kafka:

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

## Tests

With Kafka running locally, execute:

```powershell
.\mvnw.cmd test
```
