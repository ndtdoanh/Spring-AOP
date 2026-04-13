# Luong xu ly end-to-end

## 1) Luong update Product -> Activity Log

### B1. Nhan request mutation

- Client goi `PUT /api/products/{id}` vao `ProductController`.
- Controller goi `ProductService.updateProduct(...)`.

### B2. AOP intercept va doc before-state

- Method `updateProduct` duoc danh dau `@PublishDomainChanges`.
- `DomainChangePublicationAspect` chay `@Around`:
  - lay id aggregate tu argument (`idParameterIndex`),
  - dung `EntitySnapshotReader.find(...)` de doc state truoc khi mutate.

### B3. Chay business logic

- `ProductService` tim entity, cap nhat field `name/description/priceCents`, save lai.
- Method tra ve `Product` da cap nhat.

### B4. Doc after-state va tinh diff

- Aspect uu tien doc after-state tu return value.
- `EntityStateMapper` convert entity -> `Map<String, String>`.
- `DomainChangeDiff.buildIfChanged(...)` tao `DomainChangeEvent` neu co it nhat 1 field thay doi.

### B5. Ghi outbox trong cung transaction

- Aspect tao `OutboxEvent` va `outboxRepository.save(...)`.
- Neu luu outbox that bai: throw exception -> rollback transaction business.
- Ket qua: khong co truong hop business commit thanh cong nhung mat event.

### B6. Poll outbox va gui Kafka

- `OutboxPoller.process()` chay theo `app.outbox.poll-interval-ms` (mac dinh 5000ms).
- Poll theo thu tu `created_at ASC`, batch size 50.
- Moi record:
  - tao `ProducerRecord(topic, key=aggregateId, payload)`,
  - them headers (`messageId`, `aggregateType`, `aggregateId`, `occurredAt`),
  - `kafkaTemplate.send(...).get()` de cho broker ack.
- Gui thanh cong record nao thi xoa outbox record do.

### B7. Consume tai service-b

- `DomainChangeListener` nhan raw `String` payload.
- Deserialize bang `ObjectMapper` -> `DomainChangeEvent`.
- Dong goi cung Kafka metadata thanh `InboundActivityEnvelope`.
- Goi `ActivityLogCommandService.recordDomainChange(...)`.

### B8. Persist idempotent vao `activity_logs`

- Check fast-path: `existsByKafkaTopicAndKafkaPartitionAndKafkaOffset(...)`.
- Neu chua ton tai: insert.
- Neu race condition gay duplicate: DB unique constraint + catch `DataIntegrityViolationException`.
- Cuoi cung `ack.acknowledge()` commit offset.

### B9. Query ket qua

- Client goi `GET /api/activity-logs`.
- Service B tra ve `Page<ActivityLog>` (mac dinh sort `createdAt desc`).

## 2) Dam bao nhat quan va reliability

- Producer side: transactional outbox.
- Broker side: at-least-once delivery.
- Consumer side: idempotent write.
- Tong hop: chap nhan duplicate delivery tu Kafka, nhung dam bao du lieu log cuoi cung khong bi trung.

## 3) Cac case dang duoc xu ly

- Khong co thay doi field: khong tao outbox event.
- Payload loi / deserialize loi: log loi va ack (khong block partition).
- Null payload: skip + ack.
- Poller loi tren 1 record: log loi, tiep tuc record khac trong batch.

## 4) Gioi han hien tai

- Annotation hien tai bo qua case create khi `beforeEntity == null`.
- Chua co DLQ cho message loi.
- Chua co test tu dong cho luong integration end-to-end.
