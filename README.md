# 🔗 URL Shortener

Микросервисный сервис для сокращения ссылок. Учебный backend-проект с REST API, асинхронной аналитикой и мониторингом.

## 🌐 Live Demo

- **Frontend:** http://91.186.199.219/
- **Swagger:** http://91.186.199.219:8080/swagger-ui/index.html
- **Grafana:** http://91.186.199.219:3000

## 🛠 Стек

- Java 21
- Spring Boot 3
- PostgreSQL + Liquibase
- Redis
- Apache Kafka + DLQ
- Docker + Docker Compose
- Prometheus + Grafana
- Nginx
- Kubernetes / Minikube

## 🏗 Архитектура

Проект состоит из двух сервисов:

- **shortener-service** — создание коротких ссылок, редиректы, rate limiting, Redis-кэш и публикация событий в Kafka.
- **analytics-service** — асинхронная обработка Kafka-событий и сбор статистики переходов.

```text
Frontend
   │
   ▼
Nginx
   │
   ▼
shortener-service :8080
   │
   ├── PostgreSQL
   ├── Redis
   │
   └── Kafka
        │
        ▼
analytics-service :8081

🔌 API
Метод	Endpoint	Описание
POST	/api/links	Создать короткую ссылку
GET	/{shortCode}	Редирект по короткой ссылке
GET	/api/links/{shortCode}/stats	Статистика переходов
GET	/api/links/{shortCode}/analytics	Детальная аналитика
Создание ссылки
curl -X POST http://localhost:8080/api/links \
  -H "Content-Type: application/json" \
  -d '{"originalUrl":"https://google.com"}'
Ответ:
{
  "id": 1,
  "originalUrl": "https://google.com",
  "shortCode": "8hfHM0",
  "clicks": 0
}

🚀 Запуск
git clone <YOUR_REPOSITORY_URL>
cd url-shortener-service

docker compose up -d --build
Проверить контейнеры:
docker compose ps
Health check:
curl http://localhost:8080/actuator/health
📊 Monitoring
Prometheus собирает метрики приложения, а Grafana используется для их визуализации.
Также реализованы:
* Redis caching
* Rate limiting — 10 ссылок в минуту с одного IP
* Kafka Retry Topics
* Dead Letter Queue
* Correlation ID
* Liquibase migrations
* Истечение срока действия ссылок
* HTTP 410 Gone для истёкших ссылок
* Docker deployment на VPS
☁️ Deployment
Проект развернут на VPS с использованием Docker Compose и Nginx.
Публичный frontend:
http://91.186.199.219/
Backend:
http://91.186.199.219:8080
