# WordNest — Báo cáo test v1.0.0 (nhánh `feature/flashcard-statistics`)

**Ngày test:** 07/11/2026 · **Môi trường:** Emulator Medium_Phone_API_37.0 (`-gpu host`) + máy thật Xiaomi Redmi 9C M2006C3LG (Android 10), APK debug
**Kết quả build:** `gradlew test assembleDebug` OK — **48/48 unit test pass** (6 suites, 0 failures)
**Commit đã push:** `23eff5d` ← `895b2e9` → `cecd58b` → `b31bab4`
**Sửa chưa commit:** fix nhỏ F17 (SettingsScreen) + fix theme/splash F18 (4 file res + manifest) — xem mục 5, 8.

---

## 1. QC & sửa phần của Ngọc (F08–F16)

| Feature | Test case | Kết quả | Ghi chú |
|---|---|---|---|
| F08/F09 Flashcard | Lật thẻ, đánh dấu Đã/Chưa thuộc, tiến độ "Đã thuộc: x/8" | ✅ Đạt | — |
| F10 Kết quả buổi học | Kết quả 5/8, nút "Học lại từ chưa thuộc" hiện khi còn từ sai | ✅ Đạt | — |
| US10 Học lại | Phiên ôn chỉ chứa từ chưa thuộc, "Thẻ 1/3" đúng từ "pen"; ẩn nút khi 3/3 | ✅ Đạt sau fix `895b2e9` | Bug: trước đó hiện "Thẻ 1/8" từ sai + nút không ẩn |
| F12 Trắc nghiệm | Câu "Từ nào mang nghĩa X?", 4 đáp án là từ tiếng Anh | ✅ Đạt sau fix `cecd58b` | Bug: câu hỏi và đáp án trước đó đều là nghĩa — đáp án trùng câu hỏi |
| F13 Danh sách từ sai | Trả lời sai 8/8 → "Đúng: 0/8" + đủ 8 từ sai | ✅ Đạt | — |
| F14 Điền từ | Gõ sai 1 câu (`computerxx`) → 7/8, từ sai đúng "computer" | ✅ Đạt | — |
| F11/F16 Thống kê | Tổng 8 từ: Trứng 3 / Chim non 3 / Chim bay 2 | ✅ Đạt | Ngưỡng 3/7/7 ngày **chưa được chốt chính thức** — đổi cần sửa `StudyProgression` + test |
| F15 Ngày ấp trứng | 0 ngày khi có force-stop giữa buổi | ✅ Đạt theo logic hiện tại | Cần test liên tục đủ 1 ngày trên máy thật |

**Navigation (bug trên máy Xiaomi)** — fix `b31bab4`: bỏ `restoreState = true` khỏi `navigate()` tab (qua log xác nhận `navigate("home")` bị bỏ qua (không phát sinh destination mới) khi đang ở route `study`). Đã test đủ chuỗi: home→study→tab Trang chủ ✓, home→tab Cài đặt ✓, settings→tab Trang chủ ✓, study→tab Trang chủ ✓, settings→tab Trang chủ (lặp) ✓.

## 2. F17 — Nhắc học hằng ngày

| Test case | Kết quả | Ghi chú |
|---|---|---|
| TC17-01 Bật switch + mở dialog chọn giờ | ✅ | Material3 TimePicker render đúng |
| Đổi giờ thành 21:30, "Lưu" → "Giờ nhắc" cập nhật | ✅ | Persistence qua DataStore ✓ |
| TC17-05 Tắt switch → hàng giờ bị khóa | ✅ | Tap giờ khi tắt nhắc: không mở dialog |
| Tắt/bật lại → giữ nguyên 21:30 | ✅ | — |
| Quyền POST_NOTIFICATIONS | ✅ | Thu hồi quyền rồi bật lại switch → dialog hệ thống hiện; "Don't allow" → dòng cảnh báo đỏ "Chưa được cấp quyền thông báo…" hiện đúng (test 2 lần) |
| Đăng ký nhắc học thật | ✅ | Job `#ReminderWorker#` (periodic 24h) xuất hiện trong `dumpsys jobscheduler` |

## 3. F18 — Chế độ tối

- Bật switch → theme tối áp dụng tức thì ✓
- Duy trì qua khởi động lại app ✓ (check nhiều lần, kể cả sau khi process bị hệ thống giết do revoke quyền)
- Tắt → sáng ✓, bật lại → tối ✓

## 4. Bug đã sửa trong đợt này (3 commit)

1. **`895b2e9` fix(us10)** — flashcard hiển thị thẻ theo phiên (`currentCard`/`sessionSize`); thêm `unmasteredIds`, ẩn nút "Học lại" khi ôn xong.
2. **`cecd58b` fix(f12)** — `QuizBuilder` sinh distractor là **term**; câu hỏi "Từ nào mang nghĩa X?"; cập nhật `QuizBuilderTest`.
3. **`b31bab4` fix(nav)** — bỏ `restoreState` trong `WordNestApp.kt`, có comment giải thích.

## 5. Vấn đề nhỏ (trước đây là backlog — **đã sửa toàn bộ 4 mục, chưa commit**)

1. ✅ **Hàng "Giờ nhắc" mờ đi (alpha 0.5) khi nhắc tắt** — tap vào hàng vẫn khóa như cũ. Verify cả emulator lẫn máy thật (Redmi 9C).
2. ✅ **Cảnh báo quyền đúng trạng thái mọi thời điểm** — thay `notificationDenied` (chỉ set từ callback dialog) bằng `notificationGranted` tính lại từ `ContextCompat.checkSelfPermission`: lúc vào màn hình, mỗi ON_RESUME (cấp/từ chối từ Cài đặt hệ thống bắt được ngay), và từ kết quả dialog. Cảnh báo chỉ hiện khi `nhắc bật && thiếu quyền`. Verify: revoke từ ngoài → mở lại có cảnh báo ngay từ launch; grant từ ngoài → mở lại cảnh báo mất (kể cả khi process bị hệ thống restart).
3. ✅ **Layout nhảy khi cảnh báo xuất hiện** — cảnh báo bọc `AnimatedVisibility` (expand/shrink dọc) + `animateContentSize()` trên Column → chuyển tiếp trượt mượt.
4. ✅ **Theme/splash riêng** — tạo `res/values/colors.xml` (`#FDFBFF`), `values-night/colors.xml` (`#141218`), `values/themes.xml` + `values-v31/themes.xml` (`Theme.WordNest`: `windowBackground` + `windowSplashScreenBackground`), manifest đổi sang `@style/Theme.WordNest`. Emulator API 37 cold start: system dark → splash tối ngay frame đầu (hết chớp trắng); system light → splash sáng chuẩn; status bar icon tự đảo màu. **Caveat còn lại:** dark bật trong app khi system sáng → vẫn 1 pha chuyển sáng→tối ~300ms sau splash — chấp nhận cho v1.0. **Lưu ý máy thật MIUI (Android 10):** starting window do MIUI tự vẽ (đen bo góc, không dùng `windowBackground`) — không gây chớp màu vì snapshot theo trạng thái dark đã lưu.

## 6. Hạn chế môi trường & khuyến nghị test máy thật

- **Emulator SwiftShader trắng màn** khi tạo window mới (dialog/tab) — đã giải quyết bằng `-gpu host`; đây là quirk GPU phần mềm, **không phải bug app**.
- **Xiaomi `59WOJZZHBQ7L89FY`** (Redmi 9C, Android 10, UI 720x1449): `input tap` bị chặn `INJECT_EVENTS` (MIUI); `sendevent /dev/input/event4` bị SELinux chặn ghi; không có `su`. Chạm tự động không khả thi → **test theo chế độ kè**: hướng dẫn user chạm từng nút + verify bằng `screencap`. `uiautomator dump` trên máy này không trích được text Compose (chỉ status bar) — dùng screenshot thay thế.
- **F17 trên Android 10:** không có dialog POST_NOTIFICATIONS (API < 33) — app không xin quyền, `isNotificationGranted()` trả true cho SDK < 33 → không bao giờ hiện cảnh báo; đúng thiết kế.
- Còn lại: Notification nhắc có **đến thật** lúc 21:30 không — ưu tiên theo dõi tự nhiên (user dùng máy bình thường); đã xác nhận job được schedule đúng thời điểm.

## 8. Kết quả test máy thật (Xiaomi Redmi 9C, Android 10, "kè" theo chạm của user)

| Kịch bản | Kết quả | Ghi chú |
|---|---|---|
| Vào màn study từ Trang chủ | ✅ Đạt | "Thẻ 1/8", thẻ "airport" chưa lật + gợi ý "Chạm để lật thẻ" |
| Lật thẻ | ✅ Đạt | Hiện term + phiên âm + nghĩa ("clock" /klɒk/ "Đồng hồ") |
| Trả lời đủ phiên 8 thẻ (7 Đã thuộc + 1 Chưa thuộc) | ✅ Đạt | Counter "Đã thuộc: 7/8" đúng ngay trong session |
| Màn kết quả buổi học | ✅ Đạt | "Kết quả buổi học — Đã thuộc: 7/8"; "Học lại từ chưa thuộc" + "Làm trắc nghiệm"/"Điền từ"/"Xem thống kê" |
| Học lại từ chưa thuộc | ✅ Đạt | "Thẻ 1/1" đúng thẻ clock, counter riêng 0/1 |
| Hoàn tất học lại → nút "Học lại" ẩn | ✅ Đạt | Kết quả 1/1, chỉ còn trắc nghiệm/điền từ/thống kê — khớp `unmasteredIds` |
| **Tab "Trang chủ" từ màn study (bug gốc của máy này)** | ✅ **Đạt** — fix `b31bab4` xác nhận | Vào "Học flashcard" (Thẻ 1/8) → tab Trang chủ chuyển về nhà ngay |
| Tab "Trang chủ" từ màn kết quả | ✅ Đạt | — |
| F18 Bật chế độ tối trên máy thật | ✅ Đạt | Tối tức thì, cả màn Cài đặt |
| F17 alpha "Giờ nhắc" khi nhắc tắt | ✅ Đạt | Ảnh chụp rõ hàng bị mờ |
| F17 Bật nhắc trên Android 10 | ✅ Đạt | Không dialog quyền (đúng), hàng giờ sáng lên |
| TimePicker đặt 21:30 | ✅ Đạt | Material3 picker dark, "Lưu" hiển thị 21:30 |
| Đăng ký nhắc qua WorkManager | ✅ Đạt | `dumpsys jobscheduler`: job `SystemJobService` com.wordnest, `Minimum latency +16h25m` ≈ giờ máy 5:02 → 21:30 ✓ |
| Cold start + persist qua force-stop | ✅ Đạt | Home tối ngay; Cài đặt giữ Nhắc bật + 21:30 + dark |
- F15 chuỗi ngày + F11 lên hạng cần test thực tế nhiều ngày — hiện mới xác nhận qua unit test + 0 ngày (force-stop).

## 7. Phạm vi còn lại cho v1.0.0

- F01–F07 (Rooms/dictionary + phần Khang): đang chờ, `StudyWordSource` là stub 8 từ.
- F19 (làm nếu kịp). F21 Firebase → đẩy v1.1.
- Chốt ngưỡng F11 (hiện 3/7/7) trước khi tính hoàn thành — nếu đổi, sửa `StudyProgression` constants + test tham chiếu hằng số.
