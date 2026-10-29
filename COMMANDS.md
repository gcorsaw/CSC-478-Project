# Restaurant Database Commands

## Copy the project to your own directory

This copies the complete restaurant database project, including hidden files,
into a new folder in your home directory. It stops rather than merging with a
destination folder that already exists:

```bash
test ! -e "$HOME/CSC_478-copy" || {
  printf 'Destination already exists: %s\n' "$HOME/CSC_478-copy" >&2
  exit 1
}
cp -a /home/grace/CSC_478 "$HOME/CSC_478-copy"
```

To choose a different destination, replace both occurrences of
`$HOME/CSC_478-copy` with the directory you want. This is a full copy, including
the Git repository metadata and the generated `out` folder if present.

## Java source files ZIP

The [restaurant-java-sources.zip](./restaurant-java-sources.zip) archive in
this directory contains the Java source files from this project and the
provided `restaurantapptest.zip`. It includes `.java` source files only, not
compiled `.class` files.

## Start the database and web app

From the project directory, build and start both services:

```bash
docker compose up --build -d
```

Open the web app at <http://localhost:8080/>. PostgreSQL is exposed for database
clients on `localhost:5433`; it is not an HTTP website. The default local demo
credentials are database `app`, user `postgres`, and password `change-me`.
Set `POSTGRES_PASSWORD` in your environment before starting Compose to use a
different password. Do not use the demo password outside local development.

To start only PostgreSQL:

```bash
docker compose up -d postgres_db
```

Check running containers:

```bash
docker ps
```

## Test the SQL Script

Recommended command. It finds the running PostgreSQL 16 container, creates a
uniquely named temporary database, runs `Restaurant_Database.sql` twice to
check repeat initialization, and removes the temporary database:

```bash
./test_database.sh
```

To specify the container, PostgreSQL user, and database manually:

```bash
./test_database.sh my_postgres_db myuser mydatabase
```

For a container matching `docker-compose.yaml`:

```bash
./test_database.sh postgres_db postgres app
```

A successful test ends with:

```text
Database script passed twice. Temporary database removed.
```

The notice about the temporary database not existing is harmless:

```text
NOTICE: database "restaurant_schema_test" does not exist, skipping
```

## Open PostgreSQL

Connect to PostgreSQL from a terminal:

```bash
docker exec -it postgres_db psql -U postgres -d app
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

## Git and GitHub Commands

### Check status

```bash
git status
```

### Stage changes

```bash
git add .
```

### Commit changes

```bash
git commit -m "Describe your changes here"
```

### Push to GitHub

```bash
git push
```

### Set upstream for the current branch

This is required when Git says the current branch has no upstream branch:

```bash
git push --set-upstream origin main
```

If you are on a different branch, use:

```bash
git push --set-upstream origin HEAD
```

### Push a new branch to GitHub

```bash
git push -u origin <branch-name>
```

### When the remote has newer commits

This happens when Git rejects the push because the remote branch has changes you do not have locally:

```bash
git fetch origin
git pull --rebase origin main
git push --set-upstream origin main
```

If you are on a different branch, replace `main` with the current branch name or use `HEAD`:

```bash
git fetch origin
git pull --rebase origin HEAD
git push --set-upstream origin HEAD
```

### Commit and push in one flow

```bash
git add .
git commit -m "Describe your changes here"
git push
```

### Full safe workflow for a repo that is already connected to GitHub

```bash
git status
git add .
git commit -m "Describe your changes here"
git pull --rebase origin main
git push --set-upstream origin main
```

### If you are using VS Code Git UI

1. Open the Source Control panel in VS Code.
2. Review the changed files.
3. Click the plus button to stage files, or use the command palette for staging.
4. Enter a commit message.
5. Click Commit.
6. If Git asks for a rebase or shows remote changes, resolve the conflicts in VS Code.
7. Then click Sync Changes or run `git push` from the terminal.

### Helpful verification commands

Check tracking status:

```bash
git branch -vv
```

Check the remote and branch setup:

```bash
git remote -v
git status
```

## Run the web app without Docker

Keep PostgreSQL running, then run:

```bash
mvn spring-boot:run
```

The default local database connection matches the Compose settings. Override
`SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, and
`SPRING_DATASOURCE_PASSWORD` if your database uses different credentials.

## Stop the services

Stop the containers without deleting database data:

```bash
docker compose down
```

Do not use `docker compose down -v` unless you intentionally want to delete the PostgreSQL volume and all stored data.