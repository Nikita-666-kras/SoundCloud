# Что нужно для выкладки в прод

## 1. URL API и фронтенд (обязательно)

Сейчас везде зашит `http://localhost:8080` и `http://localhost:5173`. В проде фронт и бэкенд должны ходить по одному домену или по продовым URL.

### Фронтенд
- **`frontend/src/api.ts`** — `BASE_URL` должен быть относительным `/api` (чтобы запросы шли на тот же хост, что и SPA), либо переменная окружения при сборке (например `VITE_API_URL`).
- **Все картинки и стримы** — вместо `http://localhost:8080${...}` использовать один базовый URL (относительный `/api` или `import.meta.env.VITE_API_URL`).
- Файлы, где жёстко прописан localhost:
  - `api.ts`, `stores/auth.ts`, `stores/player.ts`
  - `App.vue`, `Home.vue`, `Artist.vue`, `Album.vue`, `Albums.vue`, `Favorites.vue`, `FavoriteArtists.vue`, `Friends.vue`, `MyReleases.vue`, `PlaylistDetail.vue`, `Profile.vue`, `ProfileEdit.vue`, `FavoritesTracks.vue`

**Рекомендация:** ввести в проекте константу (например `import.meta.env.VITE_APP_URL` или относительный путь) и везде подставлять её; при сборке прокидывать значение через `.env.production`.

### Бэкенд
- **CORS** — в `SecurityConfig.java`, `WebConfig.java` и во всех `@CrossOrigin` заменить `http://localhost:5173` на продовый origin (например `https://yourdomain.com`) или читать из переменной окружения (например `CORS_ALLOWED_ORIGINS`).
- Контроллеры с `@CrossOrigin(origins = "http://localhost:5173")`: AuthController, UserController, AlbumController, TrackController, NotificationController, FriendController, AdminController, PlaylistController.

---

## 2. Секреты и конфиг (обязательно)

- **JWT** — в `application.properties` стоит дефолтный `jwt.secret=your-256-bit-secret-...`. В проде **обязательно** задать свой длинный секрет через переменную окружения (например `JWT_SECRET`), не хранить в репозитории.
- **MinIO** — сейчас `minioadmin/minioadmin`. В проде сменить логин/пароль и задавать через переменные (`MINIO_ACCESS_KEY`, `MINIO_SECRET_KEY` и т.д.).
- **БД** — H2 с файлом `./data/soundcloud`. Для прода лучше PostgreSQL/MySQL: прописать URL, логин и пароль через переменные окружения, не коммитить пароли.
- **Redis** — пароль при необходимости задать и передать через конфиг/окружение.

---

## 3. Инфраструктура и Docker

- **Порты** — в `docker-compose.yml` порты 8080 и 5173. В проде обычно стоит reverse proxy (Nginx/Traefik) перед контейнерами; наружу открывают 80/443, а не 8080/5173.
- **Nginx (frontend)** — в `frontend/nginx.conf` уже есть `proxy_pass http://soundcloud-backend:8080` для `/api/`. Важно: в проде фронт должен обращаться к API по тому же домену (относительный путь `/api`), чтобы запросы шли через этот proxy, а не на localhost.
- **Том для БД** — для H2 имеет смысл завести volume для `./backend/data`, чтобы данные не терялись при пересоздании контейнера (в текущем compose уже есть volume для backend).
- **HTTPS** — в проде терминация SSL на reverse proxy (Nginx/Traefik/облачный LB). В приложении можно включить опцию "trust proxy" и при необходимости ограничить cookies/redirect на `secure`.

---

## 4. Безопасность и режим работы

- **H2 Console** — в `application.properties`: `spring.h2.console.enabled=true`. В проде отключить: `spring.h2.console.enabled=false`.
- **Логирование** — `spring.jpa.show-sql=true` в проде лучше выключить или оставить только для профиля dev.
- **CSRF** — сейчас отключён (`csrf.disable()`). Для API с JWT это часто приемлемо, но если позже появятся cookie-based сессии — нужно будет включить защиту.
- **Заголовки безопасности** — при желании добавить в Nginx/Spring (X-Content-Type-Options, HSTS и т.д.).

---

## 5. Резюме действий

| Задача | Где |
|--------|-----|
| Базовый URL API на фронте | `api.ts`, константа/`VITE_APP_URL`, все шаблоны с localhost |
| CORS на продовый домен | SecurityConfig, WebConfig, все контроллеры с @CrossOrigin |
| JWT секрет из переменной окружения | application.properties / env |
| Пароли MinIO и БД из переменных | application.properties / env, docker-compose |
| Отключить H2 console и show-sql в проде | application.properties или application-prod.properties |
| Сборка фронта с продовым URL | `.env.production` + `VITE_APP_URL` (или относительный `/api`) |
| Раздача фронта и проксирование /api на бэкенд | Уже есть в nginx.conf; в проде — один домен и HTTPS |

После этого можно выкладывать в прод за reverse proxy с HTTPS и заданными секретами.
