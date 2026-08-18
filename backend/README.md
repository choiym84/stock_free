# Stock Free Backend

## Local MySQL with Docker

The local database is defined in `compose.yaml`. Docker Compose gives every developer the same
MySQL version and settings without installing MySQL directly on macOS.

### Mental model

- **Image**: the immutable MySQL 8.4 template downloaded from Docker Hub.
- **Container**: a running MySQL process created from that image.
- **Port**: `127.0.0.1:3306` on macOS forwards to MySQL port `3306` in the container.
- **Volume**: `stock-free_mysql-data` stores database files independently of the container.
- **Compose**: `compose.yaml` describes and operates the image, container, port, volume, and health check together.

The port binds only to localhost, so the development database is not exposed to the local network.

### Start and inspect MySQL

From the `backend` directory:

```bash
docker compose up -d
docker compose ps
docker compose logs -f mysql
```

`up -d` starts MySQL in the background. Wait until `docker compose ps` reports `healthy`.
Press `Ctrl+C` to stop following logs; this does not stop MySQL.

Default local connection values:

```text
Host:     127.0.0.1
Port:     3306
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

Spring Boot detects `compose.yaml`, starts MySQL if necessary, waits for readiness, and obtains the
JDBC connection details from the running service. The configured `start-only` lifecycle leaves the
shared database running when the application exits, which is useful across Git worktrees.

### Connect with the MySQL client inside the container

```bash
docker compose exec mysql sh -c 'mysql -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DATABASE"'
```

Useful SQL commands:

```sql
SHOW DATABASES;
SHOW TABLES;
SELECT CURRENT_USER();
exit
```

### Stop, restart, and remove

```bash
docker compose stop       # Stop MySQL; preserve container and data
docker compose start      # Start the stopped container
docker compose restart    # Restart MySQL
docker compose down       # Remove container/network; preserve data volume
docker compose up -d      # Recreate the container using the preserved data
```

To intentionally delete all local database data and start over:

```bash
docker compose down -v
```

`down -v` is destructive because it removes the named data volume.

### Development database versus test database

- `docker compose up -d` provides the persistent database used while running the application.
- `./gradlew test` uses Testcontainers to create a separate, temporary MySQL 8.4 container.

Tests therefore do not modify development data. Testcontainers normally removes its temporary
container after the test process exits, while Docker may cache the MySQL image for faster reuse.

### Troubleshooting

Check Docker and service status:

```bash
docker info
docker compose ps
docker compose logs mysql
```

Check whether another process already uses port 3306:

```bash
lsof -nP -iTCP:3306 -sTCP:LISTEN
```

Validate the resolved Compose configuration:

```bash
docker compose config
```
