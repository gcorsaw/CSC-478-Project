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

## Stop PostgreSQL

Stop the services without deleting database data:

```bash
docker compose down
```

Do not use `docker compose down -v` unless you intentionally want to delete the PostgreSQL volume and all stored data.
