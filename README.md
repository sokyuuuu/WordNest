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

Danh sách đầy đủ xem tại [`docs/user_stories.md`](docs/user_stories.md).

## Công nghệ dự kiến

| Phần | Công nghệ |
|---|---|
| Nền tảng | Android |
| Ngôn ngữ | Kotlin |
| Kiến trúc | MVVM |
| Lưu trữ | Room (SQLite) |
| Thiết kế | Figma, Material Design 3 |


## Thiết kế

Link Figma: https://www.figma.com/design/j4Ps8fU0fQH4WNHl7ZkTla/WordNest?node-id=0-1&p=f&t=mHVAcOWTHVvFk7V9-0

Ảnh các màn hình xem trong thư mục [`design/ui_screens/`](design/ui_screens/).

## Cấu trúc repo

```
wordnest-android/
├── README.md
├── docs/
│   ├── analysis.md
│   ├── user_stories.md
│   ├── usecase.png
│   ├── erd.png
│   └── navigation_flow.png
├── design/
│   ├── style_guide.md
│   ├── ui_screens/
└── app/
