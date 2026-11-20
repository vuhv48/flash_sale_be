# Chức năng sự kiện và loại vé

Ứng dụng Spring Boot, package `io.github.vuhv48.flashsale`. Phần này là **BRD 04**: ban tổ chức soạn sự kiện, Admin duyệt, khách xem danh sách công khai. Mã sự kiện tự sinh `EV-000001`, `EV-000002`, ...

Các API công khai (không cần token): `GET /api/provinces`, `GET /api/event-categories`, `GET /api/events`, `GET /api/events/{id}`. Mọi API còn lại phải gửi `Authorization: Bearer {{accessToken}}`.

Swagger UI: [http://localhost:8088/swagger-ui.html](http://localhost:8088/swagger-ui.html).

Các lệnh dùng biến Postman `base_url` = `http://localhost:8088`. Trong Postman chọn **Import → Raw text**, dán cả khối `curl`, rồi bấm Import. Không dán vào ô URL.

Sau khi gọi API tạo sự kiện, loại vé hoặc ảnh, copy `id` trả về vào biến Postman `eventId`, `ticketTypeId`, `imageId`, `provinceId`.

Tài khoản gọi API ban tổ chức phải có vai trò `ORGANIZER` (quyền `CREATE_DRAFT_EVENT`, `UPDATE_OWN_EVENT`, `SUBMIT_EVENT`, `READ_UNPUBLISHED_EVENT`, `PAUSE_SALE`). Admin duyệt bằng `READ_UNPUBLISHED_EVENT`, `REVIEW_EVENT`, `PAUSE_SALE`, `CANCEL_EVENT`. Sự kiện của người khác trả `404 EVENT_NOT_FOUND`. Thiếu quyền trả `403 FORBIDDEN`.

## Trạng thái

| Mã | Nhãn | Chuyển tiếp |
| --- | --- | --- |
| `DRAFT` | Nháp | Ban tổ chức gửi duyệt |
| `PENDING_REVIEW` | Chờ duyệt | Admin duyệt hoặc từ chối |
| `REJECTED` | Bị từ chối | Ban tổ chức sửa thì về Nháp, rồi gửi lại |
| `APPROVED` | Đã duyệt, sắp mở bán | Job, khi tới giờ mở bán |
| `ON_SALE` | Đang bán | Tạm dừng, hoặc job khi hết giờ bán / hết vé |
| `PAUSED` | Tạm dừng | Tiếp tục, hoặc quá 30 phút thì job tự tiếp tục |
| `SALE_ENDED` | Kết thúc bán | Job, sau giờ sự kiện kết thúc |
| `CANCELLED` | Đã hủy | Trạng thái cuối. Admin hủy từ các trạng thái trước Đã diễn ra |
| `COMPLETED` | Đã diễn ra | Trạng thái cuối |

Danh sách công khai gồm `APPROVED`, `ON_SALE`, `PAUSED`, `SALE_ENDED`.

Khách không thấy số vé còn lại. Mỗi loại vé chỉ có `AVAILABLE` (Còn, trên 20%), `LOW` (Sắp hết, 1–20%), `SOLD_OUT` (Hết).

## API

### Danh sách tỉnh, thành

`GET /api/provinces` — không cần token. Trả `200`, 34 tỉnh thành.

```bash
curl --location '{{base_url}}/api/provinces'
```

Mỗi dòng có `id`, `gsoCode`, `name`, `shortName`, `type` (`CITY` hoặc `PROVINCE`). Dùng `id` cho `provinceId` khi tạo sự kiện.

### Thể loại sự kiện

`GET /api/event-categories` — không cần token. Trả `200`.

```bash
curl --location '{{base_url}}/api/event-categories'
```

| Mã | Nhãn |
| --- | --- |
| `MUSIC` | Âm nhạc |
| `SPORT` | Thể thao |
| `CONFERENCE` | Hội thảo |
| `THEATER` | Sân khấu |
| `OTHER` | Khác |

### Danh sách sự kiện công khai

`GET /api/events` — không cần token. Trả `200`.

Query: `category`, `provinceId`, `date` (`yyyy-MM-dd`, lọc theo ngày bắt đầu diễn ra ở giờ Việt Nam), `page` (mặc định 0), `size` (mặc định 20, tối đa 50). Sắp theo giờ mở bán tăng dần.

```bash
curl --location '{{base_url}}/api/events?category=MUSIC&provinceId={{provinceId}}&date=2026-12-20&page=0&size=20'
```

Mỗi phần tử có `id`, `code`, `name`, `category`, `categoryLabel`, `provinceName`, `venueName`, `startsAt`, `saleStartsAt`, `saleEndsAt`, `status`, `statusLabel`, `coverDocumentId`, `minPrice`. `items`, `total`, `page`, `size` nằm ở ngoài.

Danh sách và trang chi tiết được cache Redis 30 giây, xóa cache khi sự kiện đổi.

### Chi tiết sự kiện công khai

`GET /api/events/{id}` — không cần token. Trả `200`. Sự kiện chưa công khai hoặc đã hủy: `404 EVENT_NOT_FOUND`.

```bash
curl --location '{{base_url}}/api/events/{{eventId}}'
```

Thêm so với danh sách: `description`, `venueAddress`, `endsAt`, `waitingRoomOpensAt`, `ticketTypes`, `serverNow`, `countdownTarget`, `countdownSeconds`.

`countdownTarget` là `SALE_START` khi sắp mở bán, `SALE_END` khi đang bán hoặc tạm dừng, còn lại là `NONE`. `countdownSeconds` là số giây còn lại tới mốc đó.

Mỗi loại vé công khai: `id`, `name`, `price`, `availability`, `availabilityLabel`, `sortOrder`. Không có `totalQuantity`, `heldQuantity`, `soldQuantity`.

### Sự kiện của tôi

`GET /api/organizer/events` — cần `READ_UNPUBLISHED_EVENT`. Trả `200`. Mới tạo hiện trước.

```bash
curl --location '{{base_url}}/api/organizer/events?page=0&size=20' \
--header 'Authorization: Bearer {{accessToken}}'
```

Mỗi dòng là thẻ rút gọn: `id`, `code`, `name`, `category`, `categoryLabel`, `provinceName`, `status`, `statusLabel`, `saleStartsAt`, `startsAt`, `submittedAt`, `rejectReason`, `pauseCount`, `ticketCount`, `organizerId`.

### Chi tiết sự kiện của tôi

`GET /api/organizer/events/{id}` — cần `READ_UNPUBLISHED_EVENT`. Trả `200`.

```bash
curl --location '{{base_url}}/api/organizer/events/{{eventId}}' \
--header 'Authorization: Bearer {{accessToken}}'
```

Có đủ giờ bán, `maxTicketsPerUser`, `holdMinutes` (mặc định 10), `admissionRate` (mặc định 200), lý do từ chối hoặc hủy, `ticketTypes` kèm số lượng, và `images`.

### Tạo sự kiện nháp

`POST /api/organizer/events` — cần `CREATE_DRAFT_EVENT`. Trả `201`, trạng thái `DRAFT`.

Chỉ `name` là bắt buộc lúc tạo, 5–200 ký tự. Các trường khác có thể để trống và điền trước khi gửi duyệt.

```bash
curl --location '{{base_url}}/api/organizer/events' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer {{accessToken}}' \
--data '{"name":"Đêm nhạc Mùa Đông 2026","description":"Đêm nhạc ngoài trời tại sân vận động, mở cửa từ 17:00.","category":"MUSIC","venueName":"Sân vận động Mỹ Đình","venueAddress":"Đường Lê Đức Thọ, Mỹ Đình","provinceId":"{{provinceId}}","startsAt":"2026-12-20T12:00:00Z","endsAt":"2026-12-20T15:00:00Z","waitingRoomOpensAt":"2026-12-19T02:30:00Z","saleStartsAt":"2026-12-19T03:00:00Z","saleEndsAt":"2026-12-20T11:00:00Z","maxTicketsPerUser":4}'
```

`category` được chuyển thành chữ hoa. `maxTicketsPerUser` từ 1 đến 10, bỏ trống thì là 4. Tỉnh không tồn tại: `404 PROVINCE_NOT_FOUND`. Thể loại không có trong danh mục: `400 CATEGORY_INVALID`. Giờ sai thứ tự: `400 VALIDATION_ERROR`.

Thứ tự giờ: mở phòng chờ trước mở bán, mở bán trước đóng bán, đóng bán trước hoặc bằng giờ bắt đầu, giờ bắt đầu trước giờ kết thúc.

### Sửa sự kiện

`PUT /api/organizer/events/{id}` — cần `UPDATE_OWN_EVENT`. Trả `200`.

Chỉ sửa được khi `DRAFT` hoặc `REJECTED`. Sự kiện `REJECTED` sau lần sửa này về `DRAFT` và xóa lý do từ chối. Gửi lại toàn bộ form. Trường chữ bỏ trống sẽ bị xóa. `maxTicketsPerUser` bỏ trống thì giữ số cũ.

```bash
curl --location --request PUT '{{base_url}}/api/organizer/events/{{eventId}}' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer {{accessToken}}' \
--data '{"name":"Đêm nhạc Mùa Đông 2026","description":"Đêm nhạc ngoài trời tại sân vận động, mở cửa từ 17:00.","category":"MUSIC","venueName":"Sân vận động Mỹ Đình","venueAddress":"Đường Lê Đức Thọ, Mỹ Đình","provinceId":"{{provinceId}}","startsAt":"2026-12-20T12:00:00Z","endsAt":"2026-12-20T15:00:00Z","waitingRoomOpensAt":"2026-12-19T02:30:00Z","saleStartsAt":"2026-12-19T03:00:00Z","saleEndsAt":"2026-12-20T11:00:00Z","maxTicketsPerUser":4}'
```

Đã gửi duyệt hoặc đã duyệt: `409 EVENT_NOT_EDITABLE`. Sau khi duyệt không đổi giá, số lượng, giới hạn vé hoặc giờ mở bán.

### Thêm loại vé

`POST /api/organizer/events/{id}/ticket-types` — cần `CREATE_DRAFT_EVENT`. Trả `201`. Chỉ khi nháp hoặc bị từ chối.

```bash
curl --location '{{base_url}}/api/organizer/events/{{eventId}}/ticket-types' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer {{accessToken}}' \
--data '{"name":"VIP","price":500000,"totalQuantity":1000,"sortOrder":1}'
```

- `name`: 2–50 ký tự, không trùng trong cùng sự kiện, không phân biệt hoa thường. Trùng: `409 TICKET_TYPE_DUPLICATE`.
- `price`: 0 đến 50.000.000, bội số của 1.000. `0` là vé miễn phí. Sai bước giá: `400 VALIDATION_ERROR`, message `Giá phải là bội số của 1.000`.
- `totalQuantity`: 1 đến 100.000.
- Mỗi sự kiện tối đa 10 loại vé. Vượt: `409 TICKET_TYPE_LIMIT`.

### Sửa loại vé

`PUT /api/organizer/events/{id}/ticket-types/{ticketTypeId}` — cần `UPDATE_OWN_EVENT`. Trả `200`.

```bash
curl --location --request PUT '{{base_url}}/api/organizer/events/{{eventId}}/ticket-types/{{ticketTypeId}}' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer {{accessToken}}' \
--data '{"name":"VIP","price":600000,"totalQuantity":1000,"sortOrder":1}'
```

Tổng số lượng không được nhỏ hơn vé đang giữ cộng vé đã bán.

### Xóa loại vé

`DELETE /api/organizer/events/{id}/ticket-types/{ticketTypeId}` — cần `UPDATE_OWN_EVENT`. Trả `200`, xóa mềm. Không xóa loại vé đã có vé đang giữ hoặc đã bán.

```bash
curl --location --request DELETE '{{base_url}}/api/organizer/events/{{eventId}}/ticket-types/{{ticketTypeId}}' \
--header 'Authorization: Bearer {{accessToken}}'
```

Không thấy loại vé: `404 TICKET_TYPE_NOT_FOUND`.

### Gắn ảnh

`POST /api/organizer/events/{id}/images` — cần `UPDATE_OWN_EVENT`. Trả `201`.

API ghi nhận tên tệp, định dạng và kích thước để sự kiện đủ điều kiện gửi duyệt. Byte của file chưa được đưa vào kho lưu trữ. Phần tải file thuộc chức năng tệp.

```bash
curl --location '{{base_url}}/api/organizer/events/{{eventId}}/images' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer {{accessToken}}' \
--data '{"type":"EVENT_COVER","fileName":"cover.jpg","mimeType":"image/jpeg","sizeBytes":350000,"sortOrder":0}'
```

- `type`: `EVENT_COVER` (ảnh bìa, tối đa 1; gắn ảnh mới thì ảnh bìa cũ bị gỡ) hoặc `EVENT_IMAGE` (ảnh phụ, tối đa 10).
- `mimeType`: `image/jpeg`, `image/png`, `image/webp`.
- `sizeBytes`: lớn hơn 0, tối đa 5 MB.
- `id` trong `images` là id ảnh của sự kiện. `documentId` là id tệp.

### Gỡ ảnh

`DELETE /api/organizer/events/{id}/images/{imageId}` — cần `UPDATE_OWN_EVENT`. `{imageId}` là `images[].id`, không phải `documentId`.

```bash
curl --location --request DELETE '{{base_url}}/api/organizer/events/{{eventId}}/images/{{imageId}}' \
--header 'Authorization: Bearer {{accessToken}}'
```

Không thấy ảnh: `404 IMAGE_NOT_FOUND`.

### Gửi duyệt

`POST /api/organizer/events/{id}/submit` — cần `SUBMIT_EVENT`. Trả `200`, trạng thái `PENDING_REVIEW`. Chỉ gửi từ `DRAFT`. Sự kiện bị từ chối phải sửa trước, lần sửa đưa về nháp.

```bash
curl --location '{{base_url}}/api/organizer/events/{{eventId}}/submit' \
--header 'Authorization: Bearer {{accessToken}}'
```

Thiếu dữ liệu: `400 VALIDATION_ERROR`. Các điều kiện lúc gửi:

- Mô tả 20–10.000 ký tự.
- Có thể loại, tên địa điểm, địa chỉ, tỉnh thành.
- Đủ năm mốc giờ, đúng thứ tự ở mục tạo sự kiện.
- Giờ mở phòng chờ trước giờ mở bán từ 5 đến 60 phút.
- Giờ mở bán sau thời điểm gửi ít nhất 24 giờ. Gửi khi giờ mở bán chỉ còn 10 giờ bị từ chối, message `Giờ mở bán phải sau thời điểm gửi duyệt ít nhất 24 giờ`.
- Có 1–10 loại vé và đúng 1 ảnh bìa.

Hệ thống ghi `event.submitted` vào `integration_outbox`. Chưa gửi Kafka.

### Danh sách cho Admin

`GET /api/admin/events` — cần `READ_UNPUBLISHED_EVENT`. Trả `200`. Query `status` không bắt buộc. Bỏ trống thì lấy mọi trạng thái. Mới tạo hiện trước.

```bash
curl --location '{{base_url}}/api/admin/events?status=PENDING_REVIEW&page=0&size=20' \
--header 'Authorization: Bearer {{accessToken}}'
```

`status` sai: `400 VALIDATION_ERROR`, message `Trạng thái không hợp lệ`.

### Chi tiết cho Admin

`GET /api/admin/events/{id}` — cần `READ_UNPUBLISHED_EVENT`. Trả `200`, cùng dạng chi tiết của ban tổ chức, kể cả sự kiện chưa công khai.

```bash
curl --location '{{base_url}}/api/admin/events/{{eventId}}' \
--header 'Authorization: Bearer {{accessToken}}'
```

### Duyệt

`POST /api/admin/events/{id}/approve` — cần `REVIEW_EVENT`. Trả `200`, trạng thái `APPROVED`. Chỉ duyệt sự kiện đang chờ.

```bash
curl --location '{{base_url}}/api/admin/events/{{eventId}}/approve' \
--header 'Authorization: Bearer {{accessToken}}'
```

Sai trạng thái: `409 EVENT_STATUS_INVALID`. Mail `EVENT_REVIEWED` được xếp vào `mail_queue` cho email của ban tổ chức. Chưa có SMTP thì dòng mail nằm ở trạng thái `PENDING`.

### Từ chối

`POST /api/admin/events/{id}/reject` — cần `REVIEW_EVENT`. Trả `200`, trạng thái `REJECTED`. Lý do 10–500 ký tự.

```bash
curl --location '{{base_url}}/api/admin/events/{{eventId}}/reject' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer {{accessToken}}' \
--data '{"reason":"Thiếu giấy phép tổ chức sự kiện"}'
```

### Tạm dừng

`POST /api/organizer/events/{id}/pause` — cần `PAUSE_SALE`. Chỉ khi `ON_SALE`.

```bash
curl --location '{{base_url}}/api/organizer/events/{{eventId}}/pause' \
--header 'Authorization: Bearer {{accessToken}}'
```

Ban tổ chức tạm dừng tối đa 3 lần trong một đợt bán. Lần thứ 4: `409 PAUSE_LIMIT`. Mỗi lần tối đa 30 phút. Hết 30 phút, job đưa về `ON_SALE`. Nếu đã hết giờ bán hoặc hết vé thì sang `SALE_ENDED`.

Admin tạm dừng không tính vào 3 lần đó:

```bash
curl --location '{{base_url}}/api/admin/events/{{eventId}}/pause' \
--header 'Authorization: Bearer {{accessToken}}'
```

Giữ chỗ đang có vẫn thanh toán được đến khi hết hạn. API giữ chỗ thuộc BRD 05.

### Tiếp tục bán

`POST /api/organizer/events/{id}/resume` và `POST /api/admin/events/{id}/resume` — cần `PAUSE_SALE`. Chỉ khi `PAUSED`.

```bash
curl --location '{{base_url}}/api/organizer/events/{{eventId}}/resume' \
--header 'Authorization: Bearer {{accessToken}}'
```

Không đang tạm dừng: `409 EVENT_STATUS_INVALID`.

### Hủy sự kiện

`POST /api/admin/events/{id}/cancel` — cần `CANCEL_EVENT`. Trả `200`, trạng thái `CANCELLED`. Lý do 10–500 ký tự. Không hủy sự kiện đã diễn ra.

```bash
curl --location '{{base_url}}/api/admin/events/{{eventId}}/cancel' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer {{accessToken}}' \
--data '{"reason":"Ban tổ chức đổi địa điểm tổ chức"}'
```

Hệ thống hủy giữ chỗ đang `HELD`, tạo hoàn tiền `EVENT_CANCELLED` cho đơn `PAID` hoặc `ISSUED` có số tiền lớn hơn 0, hủy đơn chờ thanh toán và đơn miễn phí, vô hiệu hóa vé `VALID`, rồi xếp mail `EVENT_CANCELLED` cho từng đơn. `event.cancelled` được ghi vào `integration_outbox`.

## Job theo giờ

Ứng dụng tự kiểm tra mỗi giây:

- `APPROVED` tới giờ mở bán thì sang `ON_SALE`. Nếu giờ đóng bán cũng đã qua, hoặc không còn vé, thì sang `SALE_ENDED`.
- `ON_SALE` tới giờ đóng bán, hoặc mọi loại vé hết, thì sang `SALE_ENDED`.
- `PAUSED` quá 30 phút thì tiếp tục bán, hoặc kết thúc bán nếu đã hết giờ hoặc hết vé.
- `SALE_ENDED` sau giờ sự kiện kết thúc thì sang `COMPLETED`.

Trước giờ mở phòng chờ 2 phút, tồn kho từng loại vé (`totalQuantity - heldQuantity - soldQuantity`) được nạp vào Redis, khóa `flash-sale:inventory:{ticketTypeId}`, chỉ nạp nếu khóa chưa có.

## Lỗi

Body lỗi:

```json
{
  "success": false,
  "data": null,
  "error": {
    "timestamp": "2026-11-18T08:03:08.998Z",
    "status": 400,
    "code": "VALIDATION_ERROR",
    "message": "Giờ mở bán phải sau thời điểm gửi duyệt ít nhất 24 giờ",
    "path": "/api/organizer/events/00000000-0000-0000-0000-000000000000/submit",
    "details": {}
  }
}
```

API thành công trả thẳng JSON của sự kiện, không bọc `success`.

| Mã | HTTP | Khi nào |
| --- | --- | --- |
| `VALIDATION_ERROR` | 400 | Form sai, giờ sai, thiếu ảnh bìa, giá không chia hết 1.000 |
| `CATEGORY_INVALID` | 400 | Thể loại không có trong danh mục |
| `FORBIDDEN` | 403 | Token không có quyền của API |
| `EVENT_NOT_FOUND` | 404 | Không có sự kiện, đã xóa, chưa công khai, hoặc không phải của mình |
| `TICKET_TYPE_NOT_FOUND` | 404 | Không có loại vé trong sự kiện |
| `IMAGE_NOT_FOUND` | 404 | Không có ảnh trong sự kiện |
| `PROVINCE_NOT_FOUND` | 404 | Không có tỉnh, thành |
| `EVENT_NOT_EDITABLE` | 409 | Sửa khi không còn nháp hoặc bị từ chối |
| `EVENT_STATUS_INVALID` | 409 | Duyệt, tạm dừng, tiếp tục hoặc hủy sai trạng thái |
| `TICKET_TYPE_DUPLICATE` | 409 | Trùng tên loại vé |
| `TICKET_TYPE_LIMIT` | 409 | Quá 10 loại vé |
| `PAUSE_LIMIT` | 409 | Ban tổ chức tạm dừng lần thứ 4 |

## Dữ liệu

| Bảng | Nội dung |
| --- | --- |
| `events` | Sự kiện, mã, ban tổ chức, giờ, trạng thái, quy trình |
| `ticket_types` | Loại vé. Số còn lại = tổng − đang giữ − đã bán |
| `event_documents` | Ảnh bìa và ảnh phụ của sự kiện |
| `documents` | Tệp được ghi nhận khi gắn ảnh |
| `event_flows` | Lịch sử từng lần đổi trạng thái |
| `flows`, `flow_steps` | Quy trình `EVENT_LIFECYCLE` |
| `provinces` | 34 tỉnh, thành |
| `system_configs` | Thể loại nhóm `EVENT_CATEGORY` |
| `integration_outbox` | Sự kiện nghiệp vụ chờ gửi đi |
| `mail_queue` | Mail duyệt, từ chối và hủy sự kiện |
| `orders`, `refunds`, `tickets`, `reservations` | Được cập nhật khi Admin hủy sự kiện |

Chưa làm trong BRD 04: tải file thật lên kho, gửi Kafka, giữ chỗ, thanh toán, và sửa mô tả sau khi sự kiện đã công khai.
