# Spring AOP Demo - Eventing with Outbox Pattern

Project nay minh hoa cach dung Spring AOP de theo doi thay doi domain, luu event vao outbox, sau do day len Kafka de service khac consume va luu activity log.

## Muc tieu

- Ghi nhan thay doi tren `Product` ma khong chen logic event vao business method.
- Dam bao "business data commit thi event cung duoc ghi" (transactional outbox).
- Cho phep consumer xu ly idempotent, an toan voi redelivery.

## Thanh phan chinh

- `service-a`: Product API, AOP diff state, outbox writer, outbox poller -> Kafka.
- `service-b`: Kafka consumer, persist `activity_logs`, API query log.
- `docker-compose.yml`: Kafka + Zookeeper cho local.

## Tai lieu chi tiet

- Kien truc tong the: `docs/ARCHITECTURE.md`
- Luong xu ly end-to-end: `docs/FLOW.md`
- Huong dan chay local + API mau: `docs/RUNBOOK.md`
- Approach va ky thuat su dung: `docs/TECHNIQUES.md`

## Nhanh de hinh dung

1. Client goi `PUT /api/products/{id}` vao `service-a`.
2. AOP doc state before/after, tao diff, ghi vao `outbox_events` cung transaction.
3. Scheduler poll outbox, gui message len topic `product.updates`, gui xong moi xoa outbox row.
4. `service-b` consume, persist vao `activity_logs` voi idempotency key `(topic, partition, offset)`.
5. Client goi `GET /api/activity-logs` de xem lich su thay doi.
