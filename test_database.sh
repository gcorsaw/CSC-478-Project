#!/usr/bin/env bash
set -euo pipefail

test_database="${4:-restaurant_schema_test_$$}"

if [[ $# -eq 0 ]]; then
    container_name="$(docker ps --format '{{.Names}} {{.Image}}' \
        | awk '$2 ~ /^postgres:16([.-]|$)/ { print $1; exit }')"
    if [[ -z "${container_name}" ]]; then
        echo "No running PostgreSQL 16 container was found. Start it with: docker compose up -d" >&2
        exit 1
    fi

    container_environment="$(docker inspect "${container_name}" --format '{{range .Config.Env}}{{println .}}{{end}}')"
    database_user="$(printf '%s\n' "${container_environment}" | sed -n 's/^POSTGRES_USER=//p')"
    database_name="$(printf '%s\n' "${container_environment}" | sed -n 's/^POSTGRES_DB=//p')"
else
    container_name="${1}"
    database_user="${2:-postgres}"
    database_name="${3:-app}"
fi

if [[ -z "${database_user}" || -z "${database_name}" ]]; then
    echo "Could not determine PostgreSQL user or database for ${container_name}." >&2
    exit 1
fi

echo "Testing Restaurant_Database.sql in temporary database: ${test_database}"

docker exec "${container_name}" dropdb \
    --if-exists --username="${database_user}" "${test_database}" >/dev/null
docker exec "${container_name}" createdb \
    --username="${database_user}" --owner="${database_user}" "${test_database}"

cleanup() {
    docker exec "${container_name}" dropdb \
        --if-exists --username="${database_user}" "${test_database}" >/dev/null
}
trap cleanup EXIT

for run in 1 2; do
    echo "Applying database script (run ${run}/2)"
    docker exec -i "${container_name}" psql \
        --username="${database_user}" \
        --dbname="${test_database}" \
        --set=ON_ERROR_STOP=1 \
        < src/main/resources/Restaurant_Database.sql
done

echo "Database script passed twice. Temporary database removed."