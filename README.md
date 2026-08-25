# WhereAmI

App ghi lại vị trí của chính bạn, gửi lên Firebase, xem lại trên bản đồ tại
<https://whereami.earth.io.vn>.

Tài liệu này giải thích **khi nào app ghi một bản ghi** — phần hay gây thắc mắc
nhất — và cho sẵn vài bộ cấu hình để bạn chép vào app mà dùng.

---

## Ba tầng quyết định

Mỗi bản ghi phải đi qua ba tầng. Hiểu được ba tầng này là hiểu hết.

```
Tầng 3  Stop detection   đứng yên đủ lâu  →  TẮT HẲN GPS
   ↓                                          (không có gì đi tiếp)
Tầng 1  Yêu cầu gửi Android   "khi nào thì giao fix cho tôi"
   ↓
Tầng 2  Bộ lọc trong app      "fix vừa nhận có đáng ghi không"
   ↓
        Hàng đợi  →  Gửi lên server
```

### Tầng 3 — Stop detection (đè lên tất cả)

Bật mặc định. Máy đứng yên đủ `60 giây` → app **ngắt hoàn toàn** GPS, dựng một
vòng geofence bán kính 100 m quanh chỗ đó. Từ lúc ấy **không có bản ghi vị trí
nào** cho tới khi bạn bước ra khỏi vòng đó.

Đây không phải lỗi — đây là thứ giữ cho máy bạn không hết pin. Trong tab Log
bạn sẽ thấy `StationaryEnter: pausing`.

Muốn vẫn có tín hiệu trong lúc này thì bật **Heartbeat**: nó gửi bản ghi
không toạ độ theo nhịp bạn đặt. Heartbeat **chỉ chạy khi Stop detection đang
bật**, vì nó sinh ra để lấp đúng khoảng lặng này.

### Tầng 1 — App yêu cầu gì ở Android

Đây là chỗ có một điều bất ngờ: **Android không nhận cả khoảng cách lẫn thời
gian theo kiểu "cái nào tới trước"**. Đặt cả hai thì nó hiểu là "đủ cả hai",
tức chặt hơn chứ không lỏng hơn. Nên app buộc phải chọn một:

| Bạn đặt | App gửi xuống Android | Hệ quả |
|---|---|---|
| Khoảng cách > 0 | *"chỉ giao khi đã dịch N mét"* | **Thời gian bị bỏ qua** |
| Khoảng cách = 0 | *"cứ N giây giao một fix"* | Thời gian có tác dụng thật |
| Độ chính xác = Cao nhất | *"giao mọi fix"* | Cả hai được xử ở tầng 2 |

Khi thông số của bạn bị bỏ qua, ô đó trong Settings hiện một dòng nhắc ngay bên
dưới, và tab Log ghi lại lý do.

### Tầng 2 — Bộ lọc trong app

Fix nào được Android giao lên thì lọc tiếp. Ở đây các điều kiện ghép bằng
**HOẶC** — thoả một cái là ghi:

- đi đủ **Khoảng cách** kể từ điểm ghi gần nhất, **hoặc**
- qua đủ **Interval** giây kể từ điểm ghi gần nhất, **hoặc**
- xoay đủ **Góc** độ (nâng cao)

Ba trường hợp **luôn được ghi**, không qua lọc: fix đầu tiên sau khi bật, fix
ngay lúc chuyển giữa đang-đi và đứng-yên, và các bản ghi không phải là fix
(heartbeat, sự kiện màn hình).

### Sự kiện màn hình lấy vị trí ở đâu

Mỗi lần bật/tắt màn hình, app ghi một bản ghi kèm vị trí — nhưng **không bật
GPS**. Nó lấy cái mới nhất trong ba thứ đã có sẵn: cache vị trí của hệ thống,
fix gần nhất nhận được, và fix gần nhất đã ghi. Chi phí bằng không.

Đổi lại, vị trí đó **có thể cũ**. Bản ghi luôn kèm tuổi của vị trí, và trang
web hiện nó ngay cạnh toạ độ — `10.762622, 106.660172 · đo 4 phút trước`.

Những bản ghi này **không nằm trên đường đi** và **không cộng vào quãng
đường**. Xem chúng ở tab Nhật ký của trang web.

---

## Bỏ qua nhiễu GPS

Bật mặc định. Một fix tự khai sai số ±60 m thì **không chứng minh được** bạn
đã đi 80 m — con số đó cũng có thể chỉ là bộ thu đang lang thang.

Không có nó, máy nằm yên trên bàn sẽ vẽ ra một đám điểm loạn xạ và cộng dồn
vài km "quãng đường" không có thật.

Đánh đổi: lúc sóng xấu mà bạn đang đi thật, điểm sẽ tới chậm hơn một nhịp. Khi
một fix bị chặn vì lý do này, tab Log ghi rõ:

```
Location skipped 10.762,106.660: moved 80 m of 75 m
but the fix is only accurate to 100 m
```

**Quy tắc đi kèm: Khoảng cách phải lớn hơn sai số của mức Độ chính xác bạn
chọn.** Nếu đặt nhỏ hơn, Settings sẽ cảnh báo màu đỏ.

| Độ chính xác | Nguồn định vị | Sai số thực tế | Khoảng cách tối thiểu nên đặt |
|---|---|---|---|
| Cao nhất | GPS, chạy hết công suất | 3–10 m | 20 m |
| Cao | GPS | 5–15 m | 20 m |
| **Trung bình** | Wi-Fi / trạm phát sóng | **20–100 m** | **75 m** |
| Thấp | Nhận ké từ app khác | 500 m – vài km | 1000 m |

---

## Bộ cấu hình pha sẵn

Chép nguyên một dòng vào Settings. Cột cuối là ước lượng số điểm.

### 🚶 Hằng ngày — *mặc định, dùng cho mọi việc*

| | |
|---|---|
| Độ chính xác | Trung bình |
| Khoảng cách | **75 m** |
| Interval | 300 (đang bị bỏ qua, xem tầng 1) |
| Stop detection | Bật |
| Heartbeat | 0 |

Ngồi yên thì im lặng, đi thì mỗi 75 m một điểm. Đi bộ ≈ 1 điểm/phút, xe máy
trong phố ≈ 1 điểm/11 giây. Pin gần như không cảm nhận được.

### 🏃 Đi bộ / chạy bộ — *cần đường mượt*

| | |
|---|---|
| Độ chính xác | **Cao** |
| Khoảng cách | **20 m** |
| Interval | 0 |
| Stop detection | Bật |
| Heartbeat | 0 |

Đi bộ ≈ 1 điểm/14 giây, một giờ ≈ 250 điểm. Phải dùng Cao chứ không phải Trung
bình — 20 m nhỏ hơn sai số của Trung bình.

### 🏍 Đi xe máy / ô tô — *quãng dài, không cần chi tiết*

| | |
|---|---|
| Độ chính xác | Trung bình |
| Khoảng cách | **150 m** |
| Interval | 0 |
| Stop detection | Bật |
| Heartbeat | 0 |

60 km/h ≈ 1 điểm/9 giây. Đặt 75 m mà chạy đường trường thì một chuyến 6 tiếng
có thể chạm trần ghi miễn phí của Firebase — 150 m chia đôi con số đó.

### 🔋 Tiết kiệm pin tối đa — *chỉ cần biết đại khái ở đâu*

| | |
|---|---|
| Độ chính xác | **Thấp** |
| Khoảng cách | **1000 m** |
| Interval | 0 |
| Stop detection | Bật |
| Heartbeat | 1800 |

Không tự bật GPS, chỉ nhận ké vị trí mà app khác đã xin. Có thể cả tiếng không
có điểm nào. Heartbeat 30 phút để biết máy còn sống.

### 📡 Nhịp đều — *quan tâm "máy còn sống" hơn là lộ trình*

| | |
|---|---|
| Độ chính xác | Trung bình |
| Khoảng cách | **0** |
| Interval | **300** |
| Stop detection | Bật |
| Heartbeat | 300 |

Đây là bộ duy nhất mà **Interval thật sự hoạt động**, vì Khoảng cách bằng 0.
Cứ 5 phút một bản ghi, đi hay đứng cũng vậy.

### 🎯 Theo dõi sát — *tốn pin, dùng từng lúc thôi*

| | |
|---|---|
| Độ chính xác | **Cao nhất** |
| Khoảng cách | **30 m** |
| Interval | **60** |
| Stop detection | **Tắt** |
| Heartbeat | 0 |

Bộ duy nhất có ngữ nghĩa *"30 m HOẶC 60 giây, cái nào tới trước"* đúng như trực
giác — vì Cao nhất đẩy cả hai điều kiện xuống tầng 2. Đổi lại GPS chạy liên tục
và không bao giờ tự ngủ. **Nhớ đổi lại khi xong việc.**

---

## Các thông số khác

| Thông số | Ý nghĩa |
|---|---|
| **Server URL** | Địa chỉ nhận dữ liệu. Có kèm token, nên **coi như mật khẩu** |
| **Device ID** | Tên định danh máy. Cài lại app là đổi, và lịch sử sẽ tách làm hai |
| **Góc** (nâng cao) | Ghi thêm một điểm khi hướng đi đổi quá N độ. 0 là tắt |
| **Buffer** (nâng cao) | Xếp hàng khi mất mạng rồi gửi bù. Nên để bật |
| **Wake lock** (nâng cao) | Giữ CPU thức. Chỉ bật khi nghi máy ngủ làm mất dữ liệu |
| **Log chi tiết mỗi (giây)** | Giãn cách giữa hai dòng log chi tiết cùng loại. 0 là ghi hết |
| **Sự kiện bật/tắt màn hình** | Gửi một bản ghi mỗi lần màn hình sáng/tắt, kèm vị trí gần đúng. Không bật GPS |
| **Ưu tiên nhà cung cấp nền tảng** | Dùng định vị hệ thống thay Google Play services |

---

## Đọc log khi thấy "sao không có điểm nào"

Tab **Log**, gạt sang chế độ **Chi tiết**. Vài dòng đáng chú ý:

| Dòng log | Nghĩa là |
|---|---|
| `StationaryEnter: pausing` | Stop detection đã tắt GPS. Bình thường |
| `Location skipped ...: moved 12 m of 75 m` | Fix có về, nhưng chưa đi đủ xa |
| `...but the fix is only accurate to 100 m` | Bị chặn vì nhiễu, không phải vì ngưỡng |
| `Interval 300s not requested from the platform` | Interval đang bị bỏ qua (tầng 1) |
| `Upload response 200` | Đã lên server |
| `Upload response 403` | Sai token trong Server URL |
| `Offline, waiting for network` | Đang xếp hàng, sẽ gửi bù |

Dữ liệu **không mất** khi upload lỗi — nó nằm trong hàng đợi trên máy và tự gửi
lại sau.
