# Запуск сервера Midas Digital

Короткая инструкция: поднять БД в Docker и запустить сервер. Конфигурация БД во всех
местах согласована (`docker-compose.yml`, `server/.env`, дефолты `AppConfig.kt`), поэтому
**переменные окружения задавать не нужно** — всё работает «из коробки».

## Требования

- Docker и Docker Compose
- JDK 17

## Где выполнять команды

Все команды — из каталога `Midas_Digital_Server` (там лежат `gradlew` и `docker-compose.yml`):

```bash
cd Midas_Digital_Server
```

## 1. Поднять PostgreSQL

```bash
docker compose up -d postgres
```

Поднимется контейнер `midas-digital-postgres` (PostgreSQL 16) на порту **5433**.
Проверить готовность:

```bash
docker compose ps
```

В колонке STATUS должно быть `healthy`.

## 2. Запустить сервер

```bash
./gradlew :server:run
```

Готово, когда в логах появится:

```
Responding at http://0.0.0.0:8080
```

При первом запуске Flyway сам создаст схему (миграции `V1`…`V4`).

## 3. Проверить, что работает

В другом терминале:

```bash
# Регистрация — вернёт токен
curl -s -X POST http://localhost:8080/api/v1/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"fullName":"Иван Петров","phone":"+79990000000","pin":"1234"}'
```

## 4. Остановить

```bash
# Остановить сервер — Ctrl+C в его терминале, затем:
docker compose down       # остановить БД, данные сохранятся
docker compose down -v     # остановить БД и УДАЛИТЬ все данные (чистый старт)
```

## Параметры подключения

| Параметр   | Значение                                       |
|------------|------------------------------------------------|
| Хост:порт  | `localhost:5433`                               |
| База       | `midas_digital`                                |
| Пользователь / пароль | `midas_digital` / `midas_digital`   |
| Порт API   | `8080`                                          |

Значения по умолчанию заданы в `server/src/main/kotlin/com/midasdigital/server/infrastructure/config/AppConfig.kt`.
Их можно переопределить переменными окружения (нужно только если меняете БД/порт):

```bash
export MIDAS_DIGITAL_PORT=8080
export MIDAS_DIGITAL_DB_URL=jdbc:postgresql://localhost:5433/midas_digital
export MIDAS_DIGITAL_DB_USER=midas_digital
export MIDAS_DIGITAL_DB_PASSWORD=midas_digital
export MIDAS_DIGITAL_SESSION_TTL_DAYS=30
export MIDAS_DIGITAL_INITIAL_BALANCE=1000.00
```

## Частые проблемы

**`java.net.BindException: Address already in use`** — порт 8080 занят прошлым запуском сервера.
Найти и завершить процесс:

```bash
lsof -nP -iTCP:8080 -sTCP:LISTEN -t | xargs kill
```

**`Connection refused` / `localhost:5433`** — не поднят PostgreSQL. Запустите `docker compose up -d postgres`
и дождитесь статуса `healthy` (`docker compose ps`).

**`FATAL: database "..." does not exist` или ошибка аутентификации** — в вашем шелле остались
экспортированные ранее переменные с другой БД (например `nopay`). Сбросьте их:

```bash
unset MIDAS_DIGITAL_DB_URL MIDAS_DIGITAL_DB_USER MIDAS_DIGITAL_DB_PASSWORD
```

## Клиентское приложение

Android-приложение обращается к серверу по адресу `http://10.0.2.2:8080/` (это `localhost`
хост-машины с точки зрения Android-эмулятора). Отдельная настройка не требуется — достаточно
запущенного сервера.
