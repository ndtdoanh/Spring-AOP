# Kien truc tong the

## 1) Big picture

He thong gom 2 service Spring Boot:

- `service-a` (port `8080`): quan ly `Product`, phat hien thay doi qua AOP, ghi outbox va publish Kafka.
- `service-b` (port `8082`): consume su kien thay doi, persist vao bang `activity_logs`, cung cap API tra cuu.

Ha tang phu thuoc:

- Kafka + Zookeeper (qua `docker-compose.yml`).
- PostgreSQL cho moi service (2 DB logic rieng: `service_a`, `service_b`).

## 2) Service A - Producer ben trong

### Layer chinh

- API layer: `ProductController`
- Business layer: `ProductService`
- Cross-cutting event layer: `PublishDomainChanges` + `DomainChangePublicationAspect`
- Reliability layer: `OutboxEvent`, `OutboxRepository`, `OutboxPoller`

### Cac bang DB

- `products`: du lieu domain goc.
- `outbox_events`: event tam (durable queue trong DB) truoc khi gui Kafka.
- `shedlock`: lock scheduler khi scale nhieu instance.

## 3) Service B - Consumer ben ngoai

### Layer chinh

- Ingest layer: `DomainChangeListener` (Kafka listener)
- Command layer: `ActivityLogCommandService` (persist + idempotency)
- Query layer: `ActivityLogQueryService` + `ActivityLogQueryController`
- AOP observability layer: `PersistActivityLog` + `ActivityLogPersistenceAspect`

### Bang DB

- `activity_logs`: luu ban ghi thay doi de truy van.
- Unique constraint `(kafka_topic, kafka_partition, kafka_offset)` lam idempotency key.

## 4) Contract su kien

Ca 2 service dung cung shape payload `DomainChangeEvent`:

- `aggregateType`: loai aggregate (vd `product`)
- `aggregateId`: id aggregate (vd `1`)
- `changes`: map field -> `{previous, current}`
- `occurredAt`: thoi diem tao su kien

Topic mac dinh: `product.updates`.

## 5) Data ownership

- `service-a` so huu va mutate `products`.
- `service-b` so huu projection/log `activity_logs`.
- Giao tiep giua 2 ben la asynchronous messaging (Kafka), khong goi truc tiep HTTP noi bo.
