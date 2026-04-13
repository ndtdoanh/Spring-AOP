# Runbook local

## 1) Yeu cau

- Java 21
- Maven 3.9+
- PostgreSQL (2 database: `service_a`, `service_b`)
- Docker (de chay Kafka + Zookeeper)

## 2) Khoi dong ha tang

Tai thu muc goc project:

```bash
docker compose up -d
```

File compose hien tai chi khoi dong Kafka/Zookeeper.
Ban can tu tao va chay PostgreSQL rieng, sau do tao 2 DB:

- `service_a`
- `service_b`

## 3) Cau hinh ket noi DB

Project dang dung:

- user: `postgres`
- password: `ndtdoanh`
- host: `localhost`

Neu may cua ban khac, sua:

- `service-a/src/main/resources/application.yml`
- `service-b/src/main/resources/application.yml`

## 4) Chay service

Mo 2 terminal:

```bash
cd service-a
mvn spring-boot:run
```

```bash
cd service-b
mvn spring-boot:run
```

Flyway se tu dong tao schema khi app startup.

## 5) Thu nghiem nhanh

### 5.1 Update Product tai service-a

```bash
curl -X PUT "http://localhost:8080/api/products/1" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Demo keyboard v2",
    "description": "Mechanical, hot-swappable, RGB",
    "priceCents": 1399900
  }'
```

### 5.2 Query activity logs tai service-b

```bash
curl "http://localhost:8082/api/activity-logs?page=0&size=20&sort=createdAt,desc"
```

Neu luong thanh cong, ban se thay ban ghi moi trong `activity_logs` voi payload chua `changes`.

## 6) Cac endpoint chinh

- `PUT /api/products/{id}` - cap nhat product, trigger outbox event.
- `GET /api/activity-logs` - query log da ingest (co pagination).

## 7) Troubleshooting nhanh

- Khong thay log ben service-b:
  - kiem tra topic `product.updates`,
  - kiem tra `spring.kafka.bootstrap-servers`,
  - kiem tra outbox co row bi ton dong hay khong.
- Du lieu bi trung:
  - kiem tra unique constraint `uq_activity_log_kafka_position`,
  - kiem tra co instance nao bo qua acknowledge flow khong.
