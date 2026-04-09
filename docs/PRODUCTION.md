# Продакшен: что уже сделано в коде и что нужно от тебя

## Домен slapshous.ru (VPS)

1. DNS: A-записи `@` и `www` на IP сервера (у тебя уже настроено).
2. На сервере: `docker compose up -d` из корня репозитория, файл `.env` из `.env.example` (пароль БД и `JWT_SECRET`).
3. Nginx + TLS: пример [`deploy/nginx-slapshous.conf.example`](../deploy/nginx-slapshous.conf.example) → `sites-available`, затем `certbot --nginx -d slapshous.ru -d www.slapshous.ru`. Проверка только по IP (без TLS): [`deploy/nginx-by-ip.conf.example`](../deploy/nginx-by-ip.conf.example).
4. Фронт в Docker слушает **127.0.0.1:5173**; наружу только 80/443 через системный Nginx. При **ufw**: `sudo ufw allow 80,443/tcp`.

## Уже в репозитории

- **PostgreSQL** в `docker-compose.yml`, данные в volume `postgres-data`; бэкенд с `SPRING_PROFILES_ACTIVE=prod` подключается к `postgres:5432`.
- CORS в compose: `https://slapshous.ru`, `https://www.slapshous.ru` и localhost для разработки.
- Фронт: `VITE_APP_ORIGIN` пустой в сборке — один origin с nginx, прокси `/api` на backend.
- Vite dev: proxy `/api` → `localhost:8080`.
- JWT: `jwt.secret=${JWT_SECRET:...}`.
- Профиль `prod`: PostgreSQL, отключены лишние логи и H2-консоль; `server.forward-headers-strategy` для reverse-proxy.
- Порты сервисов на хосте привязаны к **127.0.0.1** (кроме внутренней сети compose).

## Что ещё усилить по желанию

- **`.env` на сервере:** задать свой `POSTGRES_PASSWORD` и `JWT_SECRET` (см. `.env.example`).
- **MinIO:** сменить `MINIO_ROOT_*` и ключи в env бэкенда, если консоль/ API смотрят в интернет.
- **Миграции:** сейчас схема создаётся Hibernate `ddl-auto=update`; для эволюции схемы позже — Flyway/Liquibase.
- **Первый деплой с H2:** данные из старой файловой БД в Postgres не переносятся автоматически — либо экспорт/импорт вручную, либо чистая база.

Проверка после выката: логин, загрузка трека, стрим, лайки на `https://slapshous.ru`.
