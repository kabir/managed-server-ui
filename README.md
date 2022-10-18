# Managed Server 

Start Postgres
```shell
docker run --rm -it \
  -e POSTGRES_USER=admin \
  -e POSTGRES_PASSWORD=admin \
  -e POSTGRES_DB=managed-server-db \
  -p 5432:5432 \
  postgres:latest
```