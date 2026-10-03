# Style guide – WordNest

Tài liệu quy định giao diện chung để mọi màn hình của nhóm nhìn đồng bộ.

## 1. Nhận diện

| Mục | Nội dung |
|---|---|
| Tên | WordNest |
| Slogan | Nuôi lớn vốn từ mỗi ngày. |
| Phong cách | Dễ thương, ấm áp, mềm mại, gần gũi |
| Logo | Chiếc tổ chim nhỏ, bên trong có một quả trứng hoặc chú chim non. Có thể biến tấu chữ "W" thành hình tổ |

**Quy tắc logo:**
- Dùng trên nền kem hoặc nền trắng
- Chừa khoảng trống quanh logo bằng ít nhất một nửa chiều cao logo
- Không kéo giãn, không đổi màu tùy ý

## 2. Bảng màu

### Chế độ sáng

| Vai trò | Tên | Mã màu | Dùng cho |
|---|---|---|---|
| Chính | Xanh lá | `#4CAF7A` | Điểm nhấn, thanh tiến độ, biểu tượng đang chọn |
| Chính đậm | Xanh lá đậm | `#2E7D55` | Nền nút chính (chữ trắng), liên kết |
| Phụ | Nâu ấm | `#8D6E4B` | Tiêu đề phụ, đường viền, hình tổ chim |
| Nhấn | Vàng | `#FFC857` | Trứng, ngày ấp trứng, huy hiệu, làm nổi bật |
| Nền | Kem | `#FFF8EC` | Nền màn hình |
| Bề mặt | Trắng | `#FFFFFF` | Thẻ, hộp thoại, ô nhập liệu |
| Chữ chính | Nâu đen | `#3E2F1F` | Tiêu đề, nội dung |
| Chữ phụ | Nâu xám | `#7A6A58` | Chú thích, gợi ý |
| Đường kẻ | Be nhạt | `#EADFCB` | Viền thẻ, đường phân cách |
| Thành công | Xanh lá | `#4CAF7A` | Trả lời đúng |
| Lỗi | Đỏ nhạt | `#D9534F` | Trả lời sai, xóa |

> Chữ trắng trên nền `#4CAF7A` hơi khó đọc, nên nút chính dùng `#2E7D55`. Chữ trên nền vàng `#FFC857` dùng màu nâu đen `#3E2F1F`.

### Chế độ tối

| Vai trò | Mã màu |
|---|---|
| Nền | `#1E1B16` |
| Bề mặt (thẻ) | `#2A2620` |
| Chữ chính | `#F5EBDD` |
| Chữ phụ | `#B8A892` |
| Chính | `#6FCF97` |
| Nhấn | `#FFC857` |
| Đường kẻ | `#3A342B` |

## 3. Chữ

**Font:** Nunito (Google Fonts, bo tròn, hỗ trợ tiếng Việt). Font dự phòng: Roboto.

| Kiểu | Cỡ | Độ đậm | Dùng cho |
|---|---|---|---|
| Tiêu đề màn hình | 24 | Bold | Tiêu đề trên thanh trên cùng, màn hình chính |
| Tiêu đề phần | 18 | Bold | "Các Tổ của bạn", "Đàn chim" |
| Nội dung | 16 | Regular | Văn bản, danh sách |
| Nhấn mạnh | 16 | SemiBold | Tên từ, tên Tổ |
| Chú thích | 12 | Regular | Số từ, ngày, gợi ý |
| Từ trên flashcard | 32 | Bold | Mặt trước thẻ |
| Số lớn | 32 | ExtraBold | Số từ cần ôn, thống kê |

Nên kiểm tra các câu có dấu như "Ấp trứng, ngày học liên tiếp" để chắc font hiển thị đúng dấu tiếng Việt.

## 4. Khoảng cách và bố cục

| Mục | Quy ước |
|---|---|
| Kích thước khung | 360 x 800 (Android) |
| Lề trái, phải màn hình | 16 |
| Bội số khoảng cách | 4, 8, 12, 16, 24, 32 |
| Khoảng cách giữa các thẻ | 12 |
| Khoảng cách giữa các phần | 24 |
| Vùng chạm tối thiểu | 48 x 48 |

## 5. Hình khối

| Thành phần | Bo góc | Ghi chú |
|---|---|---|
| Thẻ (card) | 16 | Nền trắng, viền `#EADFCB`, đổ bóng nhẹ |
| Nút chính | 24 (bo tròn hoàn toàn) | Cao 48 |
| Ô nhập liệu | 12 | Viền `#EADFCB`, khi chọn viền xanh lá |
| Hộp thoại | 20 | Nền trắng |
| Huy hiệu | 999 (viên thuốc) | Cỡ chữ 12 |

**Đổ bóng:** chỉ dùng loại nhẹ, ví dụ Y 2, mờ 8, màu `#3E2F1F` độ trong suốt 8%.

## 6. Thành phần dùng chung

### Nút

| Loại | Nền | Chữ | Dùng khi |
|---|---|---|---|
| Chính | `#2E7D55` | Trắng | Hành động quan trọng nhất ("Bắt đầu học", "Lưu") |
| Phụ | Trắng, viền `#2E7D55` | `#2E7D55` | Hành động thứ hai ("Hủy", "Học lại") |
| Nguy hiểm | `#D9534F` | Trắng | Xóa |
| Vô hiệu hóa | `#EADFCB` | `#7A6A58` | Chưa thể bấm |

Mỗi màn hình chỉ nên có **một** nút chính.

### Huy hiệu trạng thái từ

| Trạng thái | Tên | Màu nền | Biểu tượng |
|---|---|---|---|
| Từ mới thêm | Trứng | `#FFC857` | Quả trứng |
| Đang học | Chim non | `#CFEBD9` (xanh nhạt) | Chim non |
| Đã thuộc | Chim bay | `#4CAF7A` | Chim trưởng thành |

### Các thành phần khác

- **Thanh trên cùng:** nút quay lại bên trái, tiêu đề, nền kem
- **Thanh điều hướng dưới:** 4 mục (Trang chủ, Các Tổ, Thống kê, Cài đặt); mục đang chọn dùng màu `#2E7D55` và chữ đậm
- **Thẻ Tổ:** tên Tổ, số từ, thanh tiến độ (nền `#EADFCB`, phần đã đạt `#4CAF7A`)
- **Dòng từ vựng:** từ, nghĩa, huy hiệu trạng thái
- **Flashcard:** thẻ lớn giữa màn hình, chạm để lật, mặt trước hiển thị từ, mặt sau hiển thị nghĩa, phiên âm, ví dụ
- **Hộp thoại xác nhận:** tiêu đề, một câu mô tả, nút "Hủy" và nút "Xóa"
- **Trạng thái trống:** hình tổ chim trống, một câu mời gọi, nút hành động

## 7. Biểu tượng và hình minh họa

| Mục | Quy ước |
|---|---|
| Bộ icon | Material Symbols (Rounded), nét 2px |
| Kích thước icon | 24 (mặc định), 20 (trong dòng chữ) |
| Màu icon | Chữ phụ `#7A6A58`, khi chọn dùng `#2E7D55` |
| Hình minh họa | Chọn **một** phong cách duy nhất (ví dụ Storyset, unDraw), nét mềm, bảng màu hợp với bảng màu app |
| Nguồn hình | Ghi nguồn trong README hoặc file này |

## 8. Cách viết nội dung trong app

- Viết ngắn gọn, thân thiện, nói như một người bạn
- Nút bắt đầu bằng động từ: "Tạo Tổ", "Bắt đầu học", "Lưu"
- Lỗi nói rõ chuyện gì xảy ra và nên làm gì: "Chưa nhập nghĩa của từ. Hãy nhập rồi lưu lại."
- Dùng đúng thuật ngữ của app:

| Khái niệm | Gọi là |
|---|---|
| Bộ từ | Tổ |
| Từ mới thêm | Trứng |
| Từ đang học | Chim non |
| Từ đã thuộc | Chim bay |
| Học liên tiếp | Ngày ấp trứng |
| Thống kê | Đàn chim của bạn |

## 9. Quy ước đặt tên trong Figma

| Mục | Quy ước | Ví dụ |
|---|---|---|
| Frame màn hình | Tên tiếng Anh, viết liền, nối bằng `_` khi có trạng thái | `Home`, `Home_Empty`, `FlashcardFront`, `FlashcardBack` |
| Component | `Loại/Biến thể` | `Button/Primary`, `Badge/Egg` |
| Color Style | `Nhóm/Tên` | `Brand/Primary`, `Text/Main` |
| Page | Theo người hoặc nhóm | `Khoa`, `Khang`, `Ngọc`, `Components` |

## 10. Tệp liên quan

- Logo và hình minh họa: `design/assets/`
- Ảnh các màn hình: `design/ui_screens/`
- Link file Figma: `design/link_figma.md`
