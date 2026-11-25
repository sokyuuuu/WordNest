# WordNest

**Nuôi lớn vốn từ mỗi ngày.**

WordNest là ứng dụng Android giúp ghi nhớ từ vựng bằng flashcard. Mỗi từ như một chú chim: mới thêm vào là **trứng**, đang học là **chim non**, đã thuộc là **chim trưởng thành** và bay đi. Dữ liệu lưu ngay trên điện thoại, không cần tài khoản hay kết nối mạng.

## Thành viên

| Họ tên | Vai trò |
|---|---|
| Nguyễn Huỳnh Đăng Khoa | Trưởng nhóm |
| Nguyễn Hoàng Khang | Thành viên |
| Hà Vũ Như Ngọc | Thành viên |

## Chức năng chính

- **Quản lý Tổ (bộ từ):** tạo, sửa, xóa (có hộp thoại xác nhận), xem tiến độ từng Tổ
- **Quản lý từ vựng:** thêm, sửa, xóa từ kèm gợi ý từ điển & dịch nghĩa tự động kiểu Quizlet (autocomplete từ, tự điền phiên âm IPA, chọn chip nghĩa tiếng Việt & câu ví dụ gợi ý); tìm kiếm toàn cục qua mọi Tổ
- **Học flashcard:** lật thẻ, đánh dấu đã thuộc hoặc chưa thuộc, xem kết quả buổi học và học lại từ chưa thuộc
- **Kiểm tra:** trắc nghiệm 4 đáp án, điền từ (hiển thị câu ví dụ khuyết `___`), xem lại các từ làm sai
- **Theo dõi:** ngày ấp trứng (chuỗi ngày học liên tiếp), thống kê "Đàn chim của bạn", lưu lịch sử từng lần trả lời
- **Tiện ích:** phát âm từ tiếng Anh (Text-to-Speech ở flashcard & trắc nghiệm), nhắc học hằng ngày, chế độ sáng/tối

Hướng mở rộng: trợ lý AI sinh câu ví dụ, lặp lại ngắt quãng nâng cao, đăng nhập và đồng bộ Firebase.

Danh sách đầy đủ xem tại [`doc/user_stories.md`](doc/user_stories.md).

## Công nghệ sử dụng

| Phần | Công nghệ |
|---|---|
| Nền tảng | Android (`minSdk 26`, `targetSdk 36`, `compileSdk 37`) |
| Ngôn ngữ | Kotlin 2.2 (JVM 17) |
| Giao diện | Jetpack Compose, Material Design 3, Navigation Compose |
| Kiến trúc | MVVM + Repository + `StateFlow`, DI thủ công (`AppContainer`) |
| Lưu trữ cục bộ | Room 2.7.2 (SQLite, KSP) + Preferences DataStore |
| Từ điển & Dịch gợi ý | Datamuse API + Free Dictionary API + MyMemory API (kèm kho dự phòng offline, không cần API Key) |
| Tiện ích hệ thống | Android `TextToSpeech` (TTS), WorkManager |
| Kiểm thử | JUnit 4, Robolectric, Room Testing (`MigrationTestHelper`), Coroutines Test, Turbine |
| Thiết kế | Figma, hệ màu & component WordNest (`WnComponents`) |

## Cơ sở dữ liệu & dữ liệu mẫu

- **Cơ sở dữ liệu Room (`wordnest.db`, version 3):**
  - Toàn bộ Tổ (`Deck`), từ vựng (`Word`) và lịch sử trả lời (`study_history`) được lưu bền vững trên thiết bị thông qua Room.
  - Schema JSON được xuất tự động tại [`app/schemas/com.wordnest.data.local.AppDatabase/`](app/schemas/com.wordnest.data.local.AppDatabase/) (`2.json`, `3.json`). Ứng dụng **không** dùng `fallbackToDestructiveMigration`; mọi nâng cấp schema đi qua migration tường minh (`DbMigrations.MIGRATION_2_3`) nhằm bảo toàn dữ liệu người dùng.
  - Các thao tác xóa Tổ/từ sử dụng cơ chế **xóa mềm** (`deleted = 0/1`) kèm mốc thời gian `updated_at` chuẩn ISO-8601 UTC và trạng thái chữ thường (`egg | chick | bird`), sẵn sàng đồng bộ hai chiều với Firestore.
  - Chuỗi ngày ấp trứng (`study_days`) và cài đặt giờ nhắc học / chế độ tối được lưu riêng trong `DataStore`.
- **Dữ liệu mẫu lần đầu mở app (`SampleDataSeeder`):**
  - Khi cơ sở dữ liệu hoàn toàn trống lần đầu cài đặt, ứng dụng tự động nạp một lần duy nhất **5 Tổ / 44 từ vựng** (lũy đẳng — nếu người dùng chủ động xóa hết Tổ về sau, dữ liệu mẫu không tự nạp lại):
    1. **Từ vựng cơ bản** (8 từ gốc v1.0 `w1…w8`: 3 Trứng 🥚, 3 Chim non 🐣, 2 Chim bay 🕊️)
    2. **Du lịch** (10 từ)
    3. **IELTS** (10 từ)
    4. **Công nghệ thông tin** (8 từ)
    5. **Các loài chim** (8 từ)

## Cấu trúc thư mục

```
WordNest/
├── app/                  # Mã nguồn Android (Kotlin, Jetpack Compose, MVVM)
│   ├── schemas/          # Room schema JSON theo từng phiên bản DB (v2, v3)
│   └── src/main/java/com/wordnest/
│       ├── data/         # Room (entity, DAO, migration), repository, seeder, nguồn học
│       ├── di/           # AppContainer (cấp phát singleton DB, repository, TTS Speaker)
│       ├── reminder/     # Nhắc học hằng ngày bằng WorkManager
│       ├── ui/           # Màn hình Compose (home, decks, study, settings), theme, component
│       ├── util/         # Tiện ích (trạng thái từ, đục lỗ câu ví dụ, TTS, validate, ngày giờ)
│       └── viewmodel/    # ViewModel quản lý StateFlow cho luồng học & Trang chủ
├── database/schema/      # Schema SQL tham khảo ban đầu (WN_DB.sql)
├── design/               # Style guide, tài nguyên thiết kế
├── doc/                  # Phân tích, chức năng, user stories, test cases, báo cáo test
└── specs/                # Đặc tả kỹ thuật, hợp đồng dữ liệu & báo cáo tiến độ v1.1
```

## Cài đặt và chạy

Yêu cầu:
- Android Studio (bản mới nhất, đi kèm JBR/JDK 17+)
- Android SDK 36/37

Các bước:

1. Clone repo:
   ```bash
   git clone https://github.com/sokyuuuu/WordNest.git
   cd WordNest
   ```
2. Mở thư mục `WordNest` bằng Android Studio.
3. Chờ Gradle sync xong.
4. Chạy app: chọn thiết bị hoặc emulator rồi bấm **Run** (`Shift + F10`).

Hoặc build và chạy kiểm thử bằng dòng lệnh:

```bash
./gradlew assembleDebug                          # build APK debug
./gradlew compileDebugKotlin testDebugUnitTest   # biên dịch Kotlin & chạy toàn bộ unit test
```

APK debug nằm tại `app/build/outputs/apk/debug/`.

## Đóng góp

Đọc [`CONTRIBUTING.md`](CONTRIBUTING.md) trước khi tạo nhánh hoặc commit. Tóm tắt: không push trực tiếp vào `main`, mọi thay đổi đi qua Pull Request và cần 1 người khác duyệt.

## Thiết kế

Link Figma: https://www.figma.com/design/j4Ps8fU0fQH4WNHl7ZkTla/WordNest?node-id=0-1&p=f&t=mHVAcOWTHVvFk7V9-0

Ảnh các màn hình xem trong thư mục [`design/ui_screens/`](design/ui_screens/).
