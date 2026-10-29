# UserPulse

UserPulse is a Java and Spring Boot application that uses Apache Kafka for asynchronous user activity processing and provides analytics through REST APIs.

## Features

- **Event Streaming:** Publishes validated activity events to Kafka using `userId` as the message key, with three topic partitions.
- **Activity Analytics:** Aggregates event counts and detects three consecutive failed login attempts, resetting the counter after a successful login.
- **Error Handling:** Implements bounded retries with backoff and routes malformed or unrecoverable messages to a dead-letter topic (`user-activity.DLT`).
- **Persistence & Configuration:** PostgreSQL database configured via Docker Compose and environment variables loaded via `.env`.

## Tech Stack

Java 17 · Spring Boot · Apache Kafka · PostgreSQL · Spring Data JPA · Maven · Docker Compose · JUnit 5

## Quick Start

### 1. Prerequisites

- Java 17 or higher
- Docker & Docker Compose

### 2. Environment Configuration

Copy the sample environment file and configure your database credentials:

```powershell
Copy-Item .env.example .env
```

Ensure `.env` contains:

```env
POSTGRES_DB=userpulse
POSTGRES_USER=userpulse
POSTGRES_PASSWORD=your_password_here
POSTGRES_HOST=localhost
POSTGRES_PORT=5432
```

### 3. Start Infrastructure Services

Start Apache Kafka and PostgreSQL in the background:

```powershell
docker compose up -d
```

### 4. Build and Run the Application

Run the Spring Boot application using Maven:

```powershell
.\mvnw.cmd spring-boot:run
```

*(On Linux / macOS: `./mvnw spring-boot:run`)*

### 5. Run Tests

Execute the automated test suite:

```powershell
.\mvnw.cmd test
```

## API Reference

### Record User Activity

- **URL:** `/api/activities`
- **Method:** `POST`
- **Content-Type:** `application/json`
- **Request Body:**
  ```json
  {
    "userId": "user-101",
    "eventType": "LOGIN_SUCCESS"
  }
  ```
- **Response:**
  - `202 Accepted` - `"Activity accepted for processing"`
  - `400 Bad Request` - `"Invalid activity request"` (if `eventType` is missing or blank)

### Retrieve Analytics

- **URL:** `/api/analytics`
- **Method:** `GET`
- **Response:**
  - `200 OK`
  ```json
  {
    "LOGIN_SUCCESS": 12,
    "LOGIN_FAILED": 2
  }
  ```

### Health Check

- **URL:** `/hello`
- **Method:** `GET`
- **Response:** `200 OK` - `"Welcome to UserPulse!"`

## Architecture & Error Handling

- **Kafka Topics:**
  - `user-activity`: Primary event topic configured with 3 partitions and partition key `userId`.
  - `user-activity.DLT`: Dead-letter topic with 3 partitions.
- **Retry Policy:**
  - Configurable backoff (default `1000ms`) and maximum retry attempts (default `2`).
  - Unrecoverable deserialization errors route immediately to the DLT as raw bytes.
  - Listener execution failures retry up to the configured limit before routing to the DLT with exception headers.
