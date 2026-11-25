# WordNest — Báo cáo kiểm thử v1.1 (Gói B – Khang: `001-room-decks`)

**Ngày lập báo cáo:** 25/11/2026 · **Người phụ trách:** Nguyễn Hoàng Khang (Gói B)
**Nền mã nguồn:** `main` (`582ad20`) + toàn bộ thay đổi cục bộ gói `001-room-decks` (RD1, F01–F07, NOI1/F14, F19)
**Môi trường kiểm thử tự động:** Windows · JBR 25 (biên dịch JVM target 17) · AGP 9.2.1 · Kotlin 2.2.21 · Room 2.7.2 (KSP 2.3.6) · Robolectric 4.16.1 (SDK 34) · Turbine 1.2.0
**Kết quả biên dịch & Unit Test:** `.\gradlew.bat compileDebugKotlin testDebugUnitTest --no-daemon` → **BUILD SUCCESSFUL** (**27 suite, 175/175 unit test PASS, 0 failures, 0 errors, 0 cảnh báo biên dịch**)
**Thiết bị mục tiêu kiểm tay:** Xiaomi Redmi 9C M2006C3LG (Android 10, MIUI) + Android Emulator (thiếu gói giọng TTS tiếng Anh)

---

## 1. Tổng hợp kiểm thử tự động theo cụm chức năng (Gói B)

| Cụm / Mã chức năng | Hạng mục kiểm thử chính | Suite kiểm thử | Số test | Kết quả |
|---|---|---|---|---|
| **Hồi quy v1.0** (F08–F18) | Giữ nguyên luật học flashcard, trắc nghiệm, điền từ, ngày ấp trứng, lên/giảm hạng chim, giờ nhắc | `EggDayTest`, `LearningLogicTest`, `QuizBuilderTest`, `ReminderTimeTest`, `StudyProgressionTest`, `TypedAnswerTest` | 48 | ✅ 48/48 Đạt |
| **RD1 – Nền tảng Room v3 & Migration** | Schema v3 (`Deck`, `Word`, `StudyHistory`), `MIGRATION_2_3` bảo toàn dữ liệu v2 + hạ chữ thường `status` + kiểm FK, xoá mềm nguyên tử `deleted = 1`, `updated_at` ISO UTC | `MigrationTest`, `DeckDaoTest`, `WordDaoTest`, `StudyHistoryDaoTest`, `DeckRepositoryTest`, `WordRepositoryTest`, `StudyHistoryRoomRepositoryTest`, `WordStatusTest` | 45 | ✅ 45/45 Đạt |
| **RD1 – Seeder & Nguồn luồng học** | Nạp lũy đẳng 5 Tổ / 44 từ (gồm Tổ `"Từ vựng cơ bản"` chứa đúng 8 từ gốc `w1…w8`: 3 `egg`, 3 `chick`, 2 `bird`), không nạp lại sau khi xoá hết Tổ; `RoomStudyWordSource` (`Flow` + `suspend save`); ghi `StudyHistory` không trùng khi nhấn đúp | `SampleDataSeederTest`, `RoomStudyWordSourceTest`, `StudyViewModelTest` | 15 | ✅ 15/15 Đạt |
| **F01–F03 – Quản lý Tổ** | Danh sách Tổ kèm số từ & `%` tiến độ (0 từ → `0%`), tạo/sửa Tổ (validate tên rỗng, khoảng trắng, > 100 ký tự, mô tả > 500 ký tự, trùng tên), xoá mềm Tổ + tự thoát khi Tổ đang mở bị xoá | `ValidationTest`, `DeckViewModelTest` | 15 | ✅ 15/15 Đạt |
| **F04–F06 – Quản lý Từ, Huy hiệu & Gợi ý/Dịch Quizlet** | Chi tiết Tổ đếm trực tiếp 3 trạng thái 🥚🐣🕊️, thêm từ (`egg`), gợi ý autocomplete (`Datamuse`), tự điền phiên âm IPA & câu ví dụ (`Free Dictionary` ưu tiên `ExampleHole`), chip nghĩa tiếng Việt (`MyMemory` + dự phòng offline), sửa từ **giữ nguyên** `status`/`lastReviewedAt`, xoá mềm có xác nhận | `DeckDetailViewModelTest`, `WordFormViewModelTest`, `OnlineDictionaryRepositoryTest` | 20 | ✅ 20/20 Đạt |
| **F07 – Tìm kiếm toàn cục** | Tìm một phần từ không phân biệt hoa/thường trên mọi Tổ kèm tên Tổ (`WordWithDeck`), `debounce(200ms)`, thoát an toàn ký tự SQL `LIKE` (`%`, `_`, `\`), trạng thái trống & không có kết quả | `SearchViewModelTest` *(+ ca DAO/Repo trong `WordDaoTest`, `WordRepositoryTest`)* | 7 | ✅ 7/7 Đạt |
| **NOI1 + F14 – Trang chủ thật & Điền từ có ví dụ** | Trang chủ hiển thị tối đa 2 Tổ mới nhất thật (`topDecks`), trạng thái trống `hasDecks = false`, đồng bộ `dueCount`/`flockCounts`; `ExampleHole.replace` thay nguyên từ bằng `___` (10 ca biên) và cập nhật live trên `StudyViewModel.currentTypeExample` | `HomeViewModelTest`, `ExampleHoleTest` *(+ ca live trong `StudyViewModelTest`, `SampleDataSeederTest`)* | 13 | ✅ 13/13 Đạt |
| **F19 – Phát âm từ (TTS)** | Khởi tạo lười `Locale.ENGLISH`, `QUEUE_FLUSH`, giữ từ chờ khi cold-start, bảo vệ `generation` sau `shutdown`, tái khởi tạo sạch, phát sự kiện Toast `"Chưa cài dữ liệu phát âm"` khi `MissingData`/`Unsupported`, không ảnh hưởng giữa tab Thống kê & màn Học | `SpeakerTest` (`FakeSpeaker` + `FakeTtsEngine`) | 12 | ✅ 12/12 Đạt |
| **Tích hợp hợp đồng liên gói** (Khoa FB3 & Ngọc ST1/AI2) | LWW `updated_at` + xoá mềm ẩn khỏi toàn bộ luồng đọc; đồng bộ số liệu Trang chủ ↔ Các Tủ (`SC-004`); luồng AI2 `updateExample` → `currentTypeExample` (`B6`) & ST1 `observeAccuracy`/`observeWordsToReview`; xoá Tổ rồi tạo lại cùng tên không hồi sinh từ cũ | `CrossPackageIntegrationTest` | 3 | ✅ 3/3 Đạt |
| **Tổng cộng** | | **27 suite** | **175** | **✅ 175/175 Đạt** |

---

## 2. Đối chiếu Tiêu chí thành công (`SC-001` → `SC-007`)

| Mã | Tiêu chí thành công (`spec.md`) | Bằng chứng tự động (Code / Schema / Unit Test) | Trạng thái thiết bị thật |
|---|---|---|---|
| **SC-001** | 100% dữ liệu tạo trong app còn nguyên sau khi tắt hẳn app mở lại, cài lại APK mới (upgrade) trên Redmi 9C | `AppDatabase` v3 không có `fallbackToDestructiveMigration`; `DbMigrations.MIGRATION_2_3` + schema `2.json`/`3.json`; khoá bởi `MigrationTest` và `StudyViewModelTest.finishFlashcard_promotesAndPersistsToRoom_recordsHistory_andSupportsReviewUnknown` | ⏳ Chờ kiểm tay A3, A4 trên Redmi 9C |
| **SC-002** | Không màn hình nào còn đọc 8 từ mã cứng (`grep InMemoryStudyWordSource` rỗng) | Đã xoá `InMemoryStudyWordSource.kt`; truy vấn `InMemoryStudyWordSource`, `DeckSample`, `deckSamples` trên toàn bộ `app/src/` trả **0 kết quả** | ✅ Đạt 100% |
| **SC-003** | Tab Các Tủ không còn hiện `"Sắp ra mắt"` | Đã xoá `ComingSoonScreen.kt`, nối `DeckListScreen` vào route `Decks` trong `WordNestApp.kt`; truy vấn `"Sắp ra mắt"` và `"đang phát triển"` trên `app/src/` trả **0 kết quả** | ✅ Đạt 100% |
| **SC-004** | Số `"Hôm nay cần ôn"` và `"Đàn chim"` ở Trang chủ khớp với tổng từ thật ở tab Các Tủ tại mọi thời điểm | Cả `HomeViewModel`, `StudyViewModel`, `DeckViewModel` đọc chung Room `Flow`; khoá bởi `HomeViewModelTest` và `CrossPackageIntegrationTest` | ✅ Đạt tự động (⏳ kiểm trực quan B1–B2) |
| **SC-005** | `compileDebugKotlin` + `testDebugUnitTest` pass; có ≥ 1 test DAO cho mỗi bảng mới, ≥ 1 test khoá chuyển trạng thái từ | **27 suite / 175/175 PASS**, 0 cảnh báo; có `DeckDaoTest`, `WordDaoTest`, `StudyHistoryDaoTest`, `MigrationTest`, `StudyProgressionTest`, `StudyViewModelTest` | ✅ Đạt 100% |
| **SC-006** | Nút loa đọc to được từ trên Redmi 9C với dữ liệu phát âm có sẵn | `AndroidSpeaker` (`Locale.ENGLISH`, `QUEUE_FLUSH`) + `<queries>` `TTS_SERVICE` trong `AndroidManifest.xml` + `SpeakerTest` (12 ca PASS) | ⏳ Chờ kiểm âm thanh D1–D5 trên Redmi 9C & Emulator |
| **SC-007** | Từ có ví dụ hiện câu chứa `"___"` ở màn Điền từ (đếm ≥ 10 từ mẫu đều đúng) | **39/44** từ mẫu trong `SampleDataSeeder` sinh câu chứa `"___"` hợp lệ qua `ExampleHole.replace` (vượt ngưỡng ≥ 10); khoá bởi `SampleDataSeederTest.seedIfEmpty_providesAtLeastTenWordsWithReplaceableExamplesForTypeIn` và `ExampleHoleTest` (10 ca) | ✅ Đạt tự động (⏳ kiểm trực quan B4–B5) |

---

## 3. Bảng kiểm thử thiết bị thật & máy ảo (Quickstart A–D)

> **Hướng dẫn chạy nhanh cho Khang trước khi chốt nhánh PR:**
> 1. Nếu máy từng cài APK nhánh cũ (schema v2 cũ): `adb uninstall com.wordnest`
> 2. Cài bản debug mới: `.\gradlew.bat installDebug`
> 3. Thực hiện lần lượt 4 luồng dưới đây ở cả **Chế độ Sáng** và **Chế độ Tối**, lưu ảnh chụp màn hình vào PR.

### 3.1. Luồng A – Dữ liệu thật & bền vững (RD1)
| Mã | Kịch bản | Kiểm chứng tự động | Kết quả Redmi 9C | Ghi chú |
|---|---|---|---|---|
| A1 | Cài sạch lần đầu, mở app → Trang chủ & Thống kê hiện dữ liệu mẫu (5 Tổ / 44 từ, 8 từ gốc đầu phiên) | ✅ `SampleDataSeederTest`, `HomeViewModelTest` | ⏳ Chờ chạy tay | Gỡ APK cũ trước khi cài lần đầu |
| A2 | Học 1 buổi Flashcard, đánh dấu vài từ "Đã thuộc" | ✅ `StudyViewModelTest` | ⏳ Chờ chạy tay | — |
| A3 | Vuốt tắt hẳn app (Recent Apps) → mở lại → trạng thái chim giữ nguyên | ✅ `StudyViewModelTest` (mở lại VM trên cùng DB) | ⏳ Chờ chạy tay | — |
| A4 | Cài đè `adb install -r app-debug.apk` → dữ liệu Tổ/từ vừa học còn nguyên | ✅ `MigrationTest` (không destructive) | ⏳ Chờ chạy tay | — |
| A5 | Làm 1 buổi Trắc nghiệm 3 đúng / 2 sai → `study_history` ghi nhận độ chính xác 60% | ✅ `StudyViewModelTest.quizFlow_recordsHistoryOncePerQuestion_andComputes60PercentFor3Correct2Wrong` | ⏳ Chờ chạy tay | Sẵn sàng cho màn ST1 của Ngọc |

### 3.2. Luồng B – Trang chủ thật & Điền từ có ví dụ (NOI1 / F14)
| Mã | Kịch bản | Kiểm chứng tự động | Kết quả Redmi 9C | Ghi chú |
|---|---|---|---|---|
| B1 | Xoá hết 5 Tổ mẫu → Trang chủ hiện lời mời tạo Tổ, không tự nạp lại mẫu | ✅ `SampleDataSeederTest`, `HomeViewModelTest` | ⏳ Chờ chạy tay | — |
| B2 | Tạo 3 Tổ mới → Trang chủ hiện đúng 2 Tổ mới nhất kèm số từ | ✅ `HomeViewModelTest` | ⏳ Chờ chạy tay | — |
| B3 | Bấm `"Xem tất cả"` ở Trang chủ → chuyển sang tab Các Tủ | ✅ Đã nối `onSeeAllDecks` trong `WordNestApp.kt` | ⏳ Chờ chạy tay | — |
| B4 | Làm Điền từ gặp từ có câu ví dụ khớp → hiện thẻ `"Câu ví dụ"` có `___` | ✅ `ExampleHoleTest`, `StudyViewModelTest` | ⏳ Chờ chạy tay | — |
| B5 | Gặp từ không có câu ví dụ (`pen`, `clock`) → giao diện giữ nguyên như v1.0 | ✅ `StudyViewModelTest` | ⏳ Chờ chạy tay | — |
| B6 | Sau khi AI2 (Ngọc) lưu câu ví dụ qua `updateExample` → Điền từ hiện `___` ngay, không gọi lại AI | ✅ `StudyViewModelTest`, `CrossPackageIntegrationTest` | ⏳ Chờ ghép nhánh AI2 của Ngọc | Tầng dữ liệu & UI nhận ví dụ của Khang đã sẵn sàng |

### 3.3. Luồng C – Quản lý Tổ, Từ & Tìm kiếm (F01–F07)
| Mã | Kịch bản | Kiểm chứng tự động | Kết quả Redmi 9C | Ghi chú |
|---|---|---|---|---|
| C1 | Mở tab Các Tủ → danh sách Tổ theo Figma (Sáng/Tối), hết `"Sắp ra mắt"` | ✅ `DeckViewModelTest` + grep = 0 | ⏳ Chờ chạy tay | Chụp ảnh Sáng & Tối |
| C2 | Tạo Tổ để trống tên / toàn khoảng trắng → báo lỗi tại ô tên, không lưu | ✅ `ValidationTest`, `DeckViewModelTest` | ⏳ Chờ chạy tay | — |
| C3 | Tạo Tổ `"IELTS"` mới → hiện đầu danh sách, `0 từ`, tiến độ `0%` | ✅ `DeckViewModelTest` | ⏳ Chờ chạy tay | — |
| C4 | Sửa tên/mô tả Tổ → danh sách & chi tiết cập nhật tức thì | ✅ `DeckRepositoryTest`, `DeckViewModelTest` | ⏳ Chờ chạy tay | — |
| C5 | Xoá Tổ có từ bên trong → hiện hộp thoại xác nhận → xoá xong biến khỏi Tìm kiếm & Thống kê | ✅ `DeckRepositoryTest`, `CrossPackageIntegrationTest` | ⏳ Chờ chạy tay | — |
| C6 | Thêm từ bỏ trống nghĩa (hoặc từ) → báo lỗi đúng ô | ✅ `WordFormViewModelTest` | ⏳ Chờ chạy tay | — |
| C7 | Thêm từ hợp lệ → hiện đầu danh sách Tổ với huy hiệu 🥚 Trứng | ✅ `WordRepositoryTest`, `WordFormViewModelTest` | ⏳ Chờ chạy tay | — |
| C8 | Sửa nghĩa của từ đang là 🕊️ Chim bay → nội dung đổi, huy hiệu vẫn giữ 🕊️ | ✅ `WordRepositoryTest`, `WordFormViewModelTest` | ⏳ Chờ chạy tay | — |
| C9 | Xoá từ → hộp thoại xác nhận → biến mất khỏi danh sách | ✅ `DeckDetailViewModelTest`, `WordFormViewModelTest` | ⏳ Chờ chạy tay | — |
| C10 | Tìm `"air"` → ra từ khớp ở nhiều Tổ, mỗi dòng gắn nhãn `"Tổ: <tên>"` | ✅ `WordDaoTest`, `SearchViewModelTest` | ⏳ Chờ chạy tay | Kiểm thêm `imePadding` khi mở bàn phím |
| C11 | Tìm `"%"`, `"_"`, `"\"` → không crash, không trả toàn bộ DB | ✅ `WordDaoTest`, `WordRepositoryTest`, `SearchViewModelTest` | ⏳ Chờ chạy tay | — |
| C12 | Xoay màn hình khi đang nhập form Tạo Tổ / Thêm Từ → dữ liệu nhập còn nguyên | ✅ Dùng `rememberSaveable` ở `DeckFormScreen` & `WordFormScreen` | ⏳ Chờ chạy tay | — |

### 3.4. Luồng D – Phát âm TTS (F19)
| Mã | Kịch bản | Kiểm chứng tự động | Kết quả thiết bị | Ghi chú |
|---|---|---|---|---|
| D1 | Redmi 9C: mở Flashcard → bấm nút loa mặt trước → đọc tiếng Anh đúng từ, không lật thẻ | ✅ `SpeakerTest` | ⏳ Chờ chạy tay trên Redmi 9C | Lưu ý tăng âm lượng kênh Media |
| D2 | Redmi 9C: mở Trắc nghiệm → bấm nút loa → đọc từ tiếng Anh đang hỏi | ✅ `SpeakerTest` | ⏳ Chờ chạy tay trên Redmi 9C | Đã xoá nhãn `"đang phát triển"` |
| D3 | Emulator chưa cài dữ liệu giọng Anh → bấm loa → hiện Toast `"Chưa cài dữ liệu phát âm"`, không crash | ✅ `SpeakerTest` (`MissingData`/`Unsupported`) | ⏳ Chờ chạy tay trên Emulator | — |
| D4 | Bấm loa từ A rồi bấm loa từ B → từ A dừng ngay, đọc từ B (`QUEUE_FLUSH`) | ✅ `SpeakerTest` | ⏳ Chờ chạy tay trên Redmi 9C | — |
| D5 | Thoát khỏi màn học khi đang đọc/khởi tạo → dừng đọc & giải phóng engine, không đọc ngầm | ✅ `SpeakerTest` (`DisposableEffect` + `generation`) | ⏳ Chờ chạy tay trên Redmi 9C | — |

---

## 4. Trạng thái test chéo với các gói khác (`T067`)

1. **Với Khoa (Gói A – FB3 Đồng bộ Firestore):**
   - Hợp đồng tại [`specs/001-room-decks/contracts/room-firestore-contract.md`](../specs/001-room-decks/contracts/room-firestore-contract.md) đã được cài đặt đầy đủ ở tầng Room v3 (`updated_at` ISO-8601 UTC trên mọi thao tác ghi, `deleted = 0/1`, `status` chữ thường `egg|chick|bird`, `last_reviewed_at`, `getByIdIncludingDeleted` cho đồng bộ LWW).
   - Tự động kiểm chứng qua `MigrationTest` và `CrossPackageIntegrationTest`. Chờ ghép nhánh `FB3` của Khoa để chạy test đồng bộ hai chiều thực tế.
2. **Với Ngọc (Gói C – ST1 Thống kê lịch sử & AI2 Sinh câu ví dụ):**
   - Hợp đồng tại [`specs/001-room-decks/contracts/study-history-contract.md`](../specs/001-room-decks/contracts/study-history-contract.md) và [`example-hole.md`](../specs/001-room-decks/contracts/example-hole.md) đã hoạt động hoàn chỉnh (`StudyHistoryRoomRepository.observeAccuracy` / `observeRecentActivity` / `observeWordsToReview`, `WordRepository.updateExample`, `StudyViewModel.currentTypeExample`).
   - Tự động kiểm chứng qua `StudyHistoryRoomRepositoryTest`, `StudyViewModelTest` và `CrossPackageIntegrationTest`. Chờ ghép nhánh `ST1`/`AI2` của Ngọc để kiểm trực quan kịch bản B6 và màn Thống kê mở rộng.
