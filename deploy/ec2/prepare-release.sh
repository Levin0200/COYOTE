#!/usr/bin/env bash
set -Eeuo pipefail

cd /home/ubuntu/moyeoon
stamp="$(date +%Y%m%d-%H%M%S)"
mkdir -p backups

if [[ -f compose.yaml ]]; then
  cp -a compose.yaml "backups/compose-${stamp}.yaml"
fi
if [[ -d app ]]; then
  mv app "backups/app-${stamp}"
fi

sed -i 's/\r$//' app.env.upload

for key in JWT_SECRET MAIL_USERNAME MAIL_PASSWORD; do
  if ! grep -Eq "^${key}=.+" app.env.upload; then
    echo "MISSING_REQUIRED_ENV:${key}"
    exit 21
  fi
done

for key in MARIADB_ROOT_PASSWORD MARIADB_PASSWORD; do
  if ! grep -Eq "^${key}=.+" .env; then
    echo "MISSING_DB_ENV:${key}"
    exit 22
  fi
done

tar -xzf release-32e7166.tar.gz
mv app.env.upload app.env
chmod 600 .env app.env
sudo docker compose config --quiet

echo DEPLOY_FILES_READY
ls -ld app
ls -l compose.yaml app.env .env
