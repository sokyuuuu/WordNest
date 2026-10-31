# WordNest – Ứng dụng học từ vựng Flashcard (Android / Jetpack Compose / Room)

## 1. Kiến trúc tổng thể (Architecture Overview)

Ứng dụng **WordNest** được xây dựng theo kiến trúc **MVVM + Clean Repository Layer** sử dụng:
- **UI Layer (Jetpack Compose + Material 3 + Navigation Compose)**:
  - `MainActivity` -> `WordNestTheme` -> `AppNavHost` (4 tab Bottom Navigation: `Trang chủ`, `Tổ từ`, `Thống kê`, `Cài đặt`).
  - Các màn hình nghiệp vụ F01–F07:
    - `DeckListScreen` (F01, F03): Danh sách Tổ từ vựng kèm tổng số từ, số từ đến hạn và thanh tiến độ `%`.
    - `DeckEditScreen` (F01, F02): Tạo mới / Chỉnh sửa Tổ từ (chọn 6 biểu tượng, 5 màu sắc pastel theo Figma) + Xóa Tổ kèm hộp thoại xác nhận.
    - `DeckDetailScreen` (F02, F03, F05, F06): Thống kê Tổ (Tổng số từ, Đã thuộc, Tiến độ `%`), 3 chế độ học tập, bộ lọc trạng thái (`Tất cả`, `Trứng`, `Chim non`, `Chim trưởng thành`), danh sách từ vựng.
    - `WordEditScreen` (F04, F05): Thêm / Sửa / Xóa từ vựng (`term`, `phonetic`, `meaning`, `example`, `note`) tích hợp nút **Dịch tự động** qua API.
    - `SearchScreen` (F07): Tìm kiếm toàn cục theo thời gian thực trên cả Tổ từ và Từ vựng.
- **ViewModel Layer (`ui/viewmodel/`)**:
  - `DeckViewModel`, `WordViewModel`, `SearchViewModel` quản lý `StateFlow` và phát sự kiện one-shot qua `SharedFlow`.
  - `ViewModelFactories.kt` cung cấp helper `vmFactory { ... }` gọn nhẹ không phụ thuộc Hilt.
- **Data & Repository Layer (`data/`)**:
  - `DeckRepository`, `WordRepository`, `UserRepository` bọc các thao tác bất đồng bộ trong `Result<T>` (không nuốt `CancellationException`) và kiểm tra đầu vào qua `Validation`.
  - `TranslationApi` & `TranslationRepository` (`data/remote/TranslationApi.kt`): Tích hợp **DeepL REST API v2** (`POST v2/translate` với header `Authorization: DeepL-Auth-Key <KEY>`, tự động nhận diện endpoint Free `api-free.deepl.com` hoặc Pro `api.deepl.com`).
  - Cấu hình `DEEPL_API_KEY`: Có thể đặt trong `local.properties` (`DEEPL_API_KEY=your_key:fx`) hoặc nhập trực tiếp tại màn hình **Cài đặt** trong ứng dụng.
  - `SampleDataSeeder` (`data/seed/SampleDataSeeder.kt`): Idempotent seeder tự động khởi tạo 4 Tổ mẫu (`Du lịch`, `IELTS`, `Công nghệ thông tin`, `Các loài chim`) với 36 từ vựng đa dạng trạng thái (`egg`, `chick`, `bird`).
- **Database Layer (Room SQLite v2 - `data/local/`)**:
  - Thực thể: `Admin`, `User`, `Setting`, `Deck` (có `icon`, `color`), `Word` (có `note`, `status`), `Study_Session`, `StudyItem`, `Quiz`, `QuizItem`, `QuizOption`.
  - Quan hệ khóa ngoại `ON DELETE CASCADE` từ `User -> Deck -> Word`.

---

## 2. Cấu trúc thư mục chính

```text
app/src/main/java/com/wordnest/
├── MainActivity.kt
├── WordNestApp.kt
├── di/
│   └── AppContainer.kt
├── data/
│   ├── local/
│   │   ├── AppDatabase.kt
│   │   ├── dao/        (DeckDao, WordDao, UserDao, SettingDao, Relation_POJOs, ...)
│   │   └── entity/     (Deck, DeckStyle, Word, WordStatus, User, Setting, ...)
│   ├── remote/
│   │   └── TranslationApi.kt
│   ├── repository/
│   │   ├── DeckRepository.kt
│   │   ├── WordRepository.kt
│   │   └── UserRepository.kt
│   └── seed/
│       └── SampleDataSeeder.kt
├── ui/
│   ├── components/     (DeckCard, WordRow, StatusChip, ConfirmDialog, EmptyState, PrimaryButton, ColorPicker, IconPicker)
│   ├── nav/
│   │   └── AppNavHost.kt
│   ├── screens/        (DeckListScreen, DeckEditScreen, DeckDetailScreen, WordEditScreen, SearchScreen, PlaceholderScreens)
│   ├── theme/          (Color, Theme, Type)
│   └── viewmodel/      (DeckViewModel, WordViewModel, SearchViewModel, ViewModelFactories)
└── util/
    ├── Constants.kt
    ├── DateTimeUtil.kt
    └── Validation.kt
```

---

## 3. Hướng dẫn chạy ứng dụng & kiểm thử

### Chạy Unit Test & Integration Test (JVM + Robolectric + Room In-Memory + MockK)
```bash
./gradlew testDebugUnitTest
```

### Build APK Debug
```bash
./gradlew assembleDebug
```
