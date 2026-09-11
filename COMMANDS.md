# Restaurant Database Commands

## Start PostgreSQL

Start the database service in the background:

```bash
docker compose up -d postgres_db
```

Check running containers:

```bash
docker ps
```

## Test the SQL Script

Recommended command. It automatically finds the running PostgreSQL 16 container and reads its configured credentials:

```bash
./test_database.sh
```

The test creates a temporary database, runs `Restaurant_Database.sql`, and removes the temporary database when finished.

To specify the container, PostgreSQL user, and database manually:

```bash
./test_database.sh my_postgres_db myuser mydatabase
```

For a fresh container matching `docker_compose.yaml`:

```bash
./test_database.sh postgres_db postgres app
```

A successful test ends with:

```text
Database script passed. Temporary database removed.
```

The notice about the temporary database not existing is harmless:

```text
NOTICE: database "restaurant_schema_test" does not exist, skipping
```

## Open PostgreSQL

Automatically inspect the active container:

```bash
docker exec -it my_postgres_db psql -U myuser -d mydatabase
```

Inside `psql`, list tables:

```sql
\dt
```

View table structure:

```sql
\d worker
```

Run a query:

```sql
SELECT * FROM worker;
```

Exit `psql`:

```sql
\q
```

## Stop PostgreSQL

Stop the services without deleting database data:

```bash
docker compose down
```

Do not use `docker compose down -v` unless you intentionally want to delete the PostgreSQL volume and all stored data.
