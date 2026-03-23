# Habit Tracker API

Backend REST API for tracking habits, logs, streaks, and completion metrics.

Persistence model:

- `User` is stored in PostgreSQL (JPA)
- `Habit` and `HabitLog` are stored in MongoDB
- Relationship is maintained by `userId` as a String UUID in Habit documents

## Tech stack

- Java 21
- Spring Boot
- Spring Data MongoDB
- Spring Data JPA
- PostgreSQL
- Lombok
- Jakarta Validation

## Base URL

`http://localhost:8080`

## API examples

### 1) Create user

**Request**

`POST /users`

```json
{
	"username": "manu_dev",
	"email": "manu.dev@example.com"
}
```

**Response (201 Created)**

```json
{
	"id": "7d1bf359-e8cf-4812-9e50-106800f52bb6",
	"username": "manu_dev",
	"email": "manu.dev@example.com"
}
```

### 2) Create habit

**Request**

`POST /habits`

```json
{
	"userId": "7d1bf359-e8cf-4812-9e50-106800f52bb6",
	"name": "Drink 2L water",
	"type": "BOOLEAN",
	"frequency": "DAILY"
}
```

**Response (201 Created)**

```json
{
	"id": "65f8cc7b3e4f9d2bc3ab1202",
	"userId": "7d1bf359-e8cf-4812-9e50-106800f52bb6",
	"name": "Drink 2L water",
	"type": "BOOLEAN",
	"frequency": "DAILY",
	"createdAt": "2026-03-20T14:32:51.248Z"
}
```

### 3) Add log

**Request**

`POST /logs`

```json
{
	"habitId": "65f8cc7b3e4f9d2bc3ab1202",
	"date": "2026-03-20",
	"value": true
}
```

**Response (201 Created)**

```json
{
	"id": "65f8ccd53e4f9d2bc3ab1203",
	"habitId": "65f8cc7b3e4f9d2bc3ab1202",
	"date": "2026-03-20",
	"value": true
}
```

### 4) Get logs

**Request**

`GET /logs/habit/65f8cc7b3e4f9d2bc3ab1202?from=2026-03-14&to=2026-03-20`

**Response (200 OK)**

```json
[
	{
		"id": "65f8cb113e4f9d2bc3ab11f8",
		"habitId": "65f8cc7b3e4f9d2bc3ab1202",
		"date": "2026-03-14",
		"value": true
	},
	{
		"id": "65f8cbc13e4f9d2bc3ab11fd",
		"habitId": "65f8cc7b3e4f9d2bc3ab1202",
		"date": "2026-03-18",
		"value": true
	},
	{
		"id": "65f8ccd53e4f9d2bc3ab1203",
		"habitId": "65f8cc7b3e4f9d2bc3ab1202",
		"date": "2026-03-20",
		"value": true
	}
]
```

### 5) Get streak

**Request**

`GET /habits/65f8cc7b3e4f9d2bc3ab1202/streak`

**Response (200 OK)**

```json
4
```

### 6) Get completion %

**Request**

`GET /habits/65f8cc7b3e4f9d2bc3ab1202/completion`

**Response (200 OK)**

```json
71.43
```

## Running the project

### Prerequisites

- Java 21
- Maven 3.9+
- Docker Desktop

### 1) Start Docker services

```bash
docker compose up -d mongo postgres mongo-express
```

MongoDB will be available at `localhost:27017`.
PostgreSQL will be available at `localhost:5432`.
Mongo Express UI will be available at `http://localhost:8081`.

### 2) Configure `.env`

Create local environment file from the template:

PowerShell:

```powershell
Copy-Item .env.example .env
```

Bash:

```bash
cp .env.example .env
```

Current required variable:

```env
SPRING_DATA_MONGODB_URI=mongodb://root:rootpassword@localhost:27017/habitdb?authSource=admin
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/habit_tracker
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=postgres
SPRING_JPA_HIBERNATE_DDL_AUTO=update
SPRING_JPA_SHOW_SQL=true
```

### 3) Run the API

```bash
mvn spring-boot:run
```

The API starts at `http://localhost:8080`.

### Startup seed data

On startup, the app inserts demo data automatically if the database is empty:

- 1 user in PostgreSQL (`demo_user`)
- 3 habits in MongoDB (BOOLEAN, NUMBER, TEXT)
- 8 habit logs in MongoDB

This seed runs only when there are no users, habits, or logs yet.

To disable the seed:

```env
APP_SEED_ENABLED=false
```

### Stop local services

```bash
docker compose down
```
