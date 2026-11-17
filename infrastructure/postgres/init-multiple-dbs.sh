#!/bin/bash
set -e

if [ -n "$POSTGRES_MULTIPLE_DATABASES" ]; then
  for db in $(echo "$POSTGRES_MULTIPLE_DATABASES" | tr ',' ' '); do
    psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_USER" <<-EOSQL
      SELECT 'CREATE DATABASE $db' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = '$db')\gexec
      GRANT ALL PRIVILEGES ON DATABASE $db TO $POSTGRES_USER;
EOSQL
    psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$db" -c "CREATE EXTENSION IF NOT EXISTS pgcrypto;"
  done
fi
