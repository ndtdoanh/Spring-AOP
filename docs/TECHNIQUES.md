# Approach va ky thuat su dung

## 1) Vi sao dung AOP o producer

Project dat logic publish event vao annotation `@PublishDomainChanges` + aspect thay vi viet trong service method.

Loi ich:

- Tach biet business logic va integration logic.
- Co the tai su dung cho nhieu aggregate khac nhau.
- Bat buoc theo convention: method mutation return aggregate root de aspect lay after-state nhanh.

## 2) Vi sao dung Transactional Outbox

Neu gui Kafka truc tiep trong transaction business, co 2 rui ro:

- DB commit roi nhung send Kafka fail -> mat event.
- Kafka gui thanh cong nhung DB rollback -> event "ma".

Outbox giai quyet bang cach:

1. Ghi event vao bang `outbox_events` cung transaction domain.
2. Scheduler doc outbox va gui Kafka bat dong bo.

He thong dat duoc "eventual consistency" an toan hon.

## 3) Reliability model

- Delivery tu `service-a` sang Kafka: at-least-once (do poller co the retry).
- Delivery Kafka -> `service-b`: at-least-once (redelivery co the xay ra).
- Persist ben `service-b`: idempotent write (constraint + exists check).

Tong the: uu tien khong mat du lieu, chap nhan duplicate va loai duplicate o consumer.

## 4) Ky thuat trong service-a

- Reflection mapper co cache `PropertyDescriptor` (`EntityStateMapper`) de giam overhead.
- Diff duoc tinh tren tap hop key union, chi tao event khi gia tri thay doi.
- Outbox poller:
  - batch pull (`LIMIT`),
  - gui dong bo `.get()` truoc khi xoa outbox row,
  - gan metadata vao Kafka headers.
- ShedLock ngan nhieu instance cung poll mot luc.

## 5) Ky thuat trong service-b

- Listener dung raw `String` + `ObjectMapper` thay vi JsonDeserializer:
  - giam coupling package/class giua producer-consumer,
  - de debug payload loi.
- Ack thu cong (`enable-auto-commit=false`, `ack-mode=RECORD`) de kiem soat offset commit sau persist.
- CQRS nhe: tach command service va query service.

## 6) Trade-off dang chap nhan

- Ack ca record loi deserialize de tranh poison-pill block partition (doi lai co the mat kha nang retry tu dong).
- Poller gui block tung record -> throughput thap hon async send, nhung de bao toan an toan delete-outbox.
- Chua co DLQ, chua co retry policy cap app.

## 7) Huong nang cap de xuat

- Them DLQ topic cho record parse fail.
- Them metrics + tracing (Micrometer/OpenTelemetry) cho outbox lag, consume lag, error ratio.
- Bo sung integration test cho full flow update -> outbox -> kafka -> activity log.
- Mo rong annotation de handle create/delete event ro rang hon.
