#!/usr/bin/env bash
set -Eeuo pipefail

cd /home/ubuntu/moyeoon

db_root_password="$(sed -n 's/^MARIADB_ROOT_PASSWORD=//p' .env | tail -n 1)"
if [[ -z "${db_root_password}" ]]; then
  echo MISSING_DB_ROOT_PASSWORD
  exit 31
fi

sudo docker compose up -d mariadb

for attempt in $(seq 1 30); do
  db_status="$(sudo docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' moyeoon-mariadb-1 2>/dev/null || true)"
  if [[ "${db_status}" == "healthy" ]]; then
    break
  fi
  if [[ "${attempt}" == "30" ]]; then
    echo "DATABASE_NOT_HEALTHY:${db_status}"
    sudo docker compose logs --tail=100 mariadb
    exit 32
  fi
  sleep 3
done

sudo docker exec -i -e MYSQL_PWD="${db_root_password}" moyeoon-mariadb-1 \
  mariadb --protocol=socket -uroot < app/db/coyote_backend_mj_schema.sql

printf "%s\n" \
  "GRANT ALL PRIVILEGES ON coyote_db_backend_mj.* TO 'moyeoon'@'%';" \
  "FLUSH PRIVILEGES;" \
  | sudo docker exec -i -e MYSQL_PWD="${db_root_password}" moyeoon-mariadb-1 \
      mariadb --protocol=socket -uroot

sudo docker compose build backend
sudo docker compose up -d backend

for attempt in $(seq 1 40); do
  if curl --fail --silent http://127.0.0.1:8080/api/health >/tmp/coyote-health.json; then
    cat /tmp/coyote-health.json
    echo
    sudo docker compose ps
    exit 0
  fi
  if [[ "${attempt}" == "40" ]]; then
    echo BACKEND_HEALTHCHECK_FAILED
    sudo docker compose ps
    sudo docker compose logs --tail=160 backend
    exit 33
  fi
  sleep 3
done
