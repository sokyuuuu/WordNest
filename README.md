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

- **Quản lý Tổ (bộ từ):** tạo, sửa, xóa, xem tiến độ từng Tổ
- **Quản lý từ vựng:** thêm, sửa, xóa từ kèm nghĩa, phiên âm, ví dụ; tìm kiếm
- **Học flashcard:** lật thẻ, đánh dấu đã thuộc hoặc chưa thuộc, xem kết quả buổi học
- **Kiểm tra:** trắc nghiệm, điền từ, xem lại các từ làm sai
- **Theo dõi:** ngày ấp trứng (học liên tiếp), thống kê "Đàn chim của bạn"
- **Tiện ích:** nhắc học hằng ngày, chế độ tối

Hướng mở rộng: phát âm từ (Text-to-Speech), lặp lại ngắt quãng, đăng nhập và đồng bộ Firebase.

Danh sách đầy đủ xem tại [`doc/user_stories.md`](doc/user_stories.md).

## Công nghệ dự kiến

| Phần | Công nghệ |
|---|---|
| Nền tảng | Android |
| Ngôn ngữ | Kotlin |
| Kiến trúc | MVVM |
| Lưu trữ | Room (SQLite) |
| Thiết kế | Figma, Material Design 3 |

## Cấu trúc thư mục

```
WordNest/
├── app/                  # Mã nguồn Android (Kotlin, MVVM)
│   └── src/main/java/com/wordnest/
│       ├── data/         # Room: entity, DAO, repository
│       ├── viewmodel/    # ViewModel cho từng màn hình
│       ├── ui/           # Compose/View: màn hình, component, điều hướng
│       └── util/         # Hàm tiện ích (ngày ấp trứng, giờ nhắc...)
├── database/schema/      # Schema tham khảo (WN_DB.sql)
├── design/               # Style guide, ảnh màn hình
└── doc/                  # User story, danh sách chức năng, phân tích
```

## Cài đặt và chạy

Yêu cầu:
- Android Studio (bản mới nhất)
- JDK 17 (Android Studio đã kèm sẵn)
- Android SDK 35 trở lên

Các bước:

1. Clone repo:
   ```bash
   git clone https://github.com/sokyuuuu/WordNest.git
   cd WordNest
   ```
2. Mở thư mục `WordNest` bằng Android Studio.
3. Chờ Gradle sync xong.
4. Chạy app: chọn thiết bị hoặc emulator rồi bấm **Run** (`Shift + F10`).

Hoặc build bằng dòng lệnh:

```bash
./gradlew assembleDebug   # build APK debug
./gradlew test            # chạy unit test
```

APK debug nằm tại `app/build/outputs/apk/debug/`.

## Đóng góp

Đọc [`CONTRIBUTING.md`](CONTRIBUTING.md) trước khi tạo nhánh hoặc commit. Tóm tắt: không push trực tiếp vào `main`, mọi thay đổi đi qua Pull Request và cần 1 người khác duyệt.

## Thiết kế

Link Figma: https://www.figma.com/design/j4Ps8fU0fQH4WNHl7ZkTla/WordNest?node-id=0-1&p=f&t=mHVAcOWTHVvFk7V9-0

Ảnh các màn hình xem trong thư mục [`design/ui_screens/`](design/ui_screens/).
