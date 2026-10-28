# Quy ước làm việc – WordNest

Tài liệu này mô tả cách nhóm làm việc với Git và GitHub. Mọi thành viên cần đọc trước khi push code đầu tiên.

## 1. Nhánh

- `main`: nhánh ổn định, chỉ nhận code qua Pull Request. **Không push trực tiếp.**
- `backend`: nhánh riêng đã có sẵn trên repo, không thuộc quy ước dưới đây.
- Nhánh làm việc đặt theo mẫu `<loại>/<mô-tả-ngắn>`:
  - `feat/F15-trang-chu`
  - `fix/F09-danh-dau-the`
  - `chore/project-setup`
  - `docs/readme`
  - `test/us01-test-case`

Dùng chữ thường, nối bằng dấu gạch ngang, không dấu tiếng Việt.

## 2. Commit

Dùng [Conventional Commits](https://www.conventionalcommits.org/vi/):

```
<loại>(<phạm vi>): <mô tả ngắn>
```

| Loại | Dùng khi |
| --- | --- |
| `feat` | Thêm tính năng |
| `fix` | Sửa lỗi |
| `docs` | Chỉ thay đổi tài liệu |
| `style` | Format, không đổi logic |
| `refactor` | Đổi cấu trúc code, không đổi hành vi |
| `test` | Thêm hoặc sửa test |
| `build` | Gradle, dependency, cấu hình build |
| `chore` | Việc vặt khác (cấu hình repo, .gitignore) |

Ví dụ:

```
feat(home): hiển thị ngày ấp trứng và số từ cần ôn (F15)
fix(reminder): sửa giờ nhắc bị lệch múi giờ
test(egg): thêm unit test cho tính ngày ấp trứng
```

Quy tắc:
- Mô tả ngắn, viết thường, tối đa khoảng 72 ký tự.
- Mỗi commit làm một việc.
- Nếu liên quan đến chức năng, ghi mã F hoặc US trong ngoặc, ví dụ `(F15)`.

## 3. Pull Request

1. Tạo nhánh mới từ `main`, làm việc, push lên GitHub.
2. Mở PR vào `main`, điền đầy đủ mẫu PR.
3. Cần **ít nhất 1 thành viên khác duyệt** (Approve) trước khi merge.
4. Mọi thay đổi phải build được (`./gradlew assembleDebug`) và test pass.
5. Dùng **Squash and merge** để lịch sử `main` gọn.
6. Xóa nhánh sau khi merge.

## 4. Issue

- Mọi việc đều tạo Issue trước, rồi liên kết với PR bằng `Closes #<số>`.
- Dùng mẫu có sẵn: Bug report hoặc Feature request.
- Theo dõi tiến độ trên GitHub Projects.

## 5. Họp nhóm

- Họp nhanh 2 lần/tuần.
- Trước mỗi buổi họp, cập nhật trạng thái các Issue đang làm.
