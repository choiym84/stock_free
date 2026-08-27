# Stock Free Backend

## User session authentication

The user API uses a server-side session stored in the `JSESSIONID` cookie. See
[`docs/user-session-auth.md`](docs/user-session-auth.md) for the endpoints and browser CSRF flow.

## Local PostgreSQL with Docker

The local database is defined in `compose.yaml`. Docker Compose gives every developer the same
PostgreSQL version and settings without installing PostgreSQL directly on macOS.

### Mental model

- **Image**: the immutable PostgreSQL 17 template downloaded from Docker Hub.
- **Container**: a running PostgreSQL process created from that image.
- **Port**: `127.0.0.1:5432` on macOS forwards to PostgreSQL port `5432` in the container.
- **Volume**: `stock-free_postgres-data` stores database files independently of the container.
- **Compose**: `compose.yaml` describes and operates the image, container, port, volume, and health check together.

The port binds only to localhost, so the development database is not exposed to the local network.

### Start and inspect PostgreSQL

From the `backend` directory:

```bash
docker compose up -d
docker compose ps
docker compose logs -f postgres
```

`up -d` starts PostgreSQL in the background. Wait until `docker compose ps` reports `healthy`.
Press `Ctrl+C` to stop following logs; this does not stop PostgreSQL.

Default local connection values:

```text
Host:     127.0.0.1
Port:     5432
Database: stock_free
Username: stock_free
Password: stock_free_local
```

These defaults are for local development only. To override them, copy `.env.example` to `.env`
and edit the ignored `.env` file:

```bash
cp .env.example .env
```

Environment values initialize users and databases only when the data volume is empty. Changing
`.env` later does not rewrite credentials already stored in an existing volume.

### Run the Spring application

```bash
./gradlew bootRun
```

Spring Boot detects `compose.yaml`, starts PostgreSQL if necessary, waits for readiness, and obtains
the JDBC connection details from the running service. The configured `start-and-stop` lifecycle
stops the PostgreSQL container when the application exits while preserving its named volume.
If PostgreSQL was already running before Spring Boot started, Spring reuses that service and leaves
it running when the application exits.

### Connect with psql inside the container

```bash
docker compose exec postgres sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB"'
```

Useful psql commands:

```text
\l                 list databases
\dt                list tables
\d table_name      describe a table
\q                 exit
```

### Stop, restart, and remove

```bash
docker compose stop       # Stop PostgreSQL; preserve container and data
docker compose start      # Start the stopped container
docker compose restart    # Restart PostgreSQL
docker compose down       # Remove container/network; preserve data volume
docker compose up -d      # Recreate the container using the preserved data
```

To intentionally delete all local database data and start over:

```bash
docker compose down -v
```

`down -v` is destructive because it removes the named data volume.

### Flyway schema management

Flyway migrations live in:

```text
src/main/resources/db/migration
```

The initial PostgreSQL schema is created by `V1__create_stock_free_schema.sql`. Once a migration has
been applied, do not edit it. Add a new version such as `V2__add_market_calendar.sql` instead.

### Development database versus test database

- `./gradlew bootRun` uses the persistent PostgreSQL database from `compose.yaml`.
- `./gradlew test` uses Testcontainers to create a separate, temporary PostgreSQL 17 container.

Tests therefore do not modify development data. Testcontainers normally removes its temporary
container after the test process exits, while Docker may cache the PostgreSQL image for faster reuse.

### Troubleshooting

Check Docker and service status:

```bash
docker info
docker compose ps
docker compose logs postgres
```

Check whether another process already uses port 5432:

```bash
lsof -nP -iTCP:5432 -sTCP:LISTEN
```

Validate the resolved Compose configuration:

```bash
docker compose config
```
