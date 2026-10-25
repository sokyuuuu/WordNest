# Test case – WordNest

Tài liệu này được viết từ tiêu chí chấp nhận trong [`user_stories.md`](user_stories.md). Mỗi user story có các test case cho luồng chính, dữ liệu biên và trường hợp lỗi.

**Quy ước mã:** `TC<số US>-<số thứ tự>`, ví dụ `TC04-02` là test case thứ 2 của US04.

**Phạm vi:** các mục đánh dấu ⚠️ là mục chưa rõ trong tài liệu, cần nhóm xác nhận trước khi thực hiện. Xem phần [Câu hỏi cần xác nhận](#câu-hỏi-cần-xác-nhận) ở cuối.

---

## US01 – Tạo Tổ (F01)

| Mã | Kịch bản | Các bước | Kết quả mong đợi |
|---|---|---|---|
| TC01-01 | Tạo Tổ hợp lệ | 1. Bấm nút tạo Tổ<br>2. Nhập tên "Tiếng Anh cơ bản", mô tả "Từ vựng đời sống"<br>3. Bấm Lưu | Tổ mới hiện ngay trong danh sách với đúng tên |
| TC01-02 | Tạo Tổ không nhập mô tả | 1. Nhập tên, bỏ trống mô tả<br>2. Bấm Lưu | Tổ được tạo thành công |
| TC01-03 | Bỏ trống tên | 1. Bỏ trống tên, nhập mô tả<br>2. Bấm Lưu | Không lưu, hiển thị thông báo lỗi ở ô tên |
| TC01-04 | Tên chỉ có khoảng trắng | 1. Nhập tên toàn khoảng trắng<br>2. Bấm Lưu | Không lưu, hiển thị thông báo lỗi |

## US02 – Sửa và xóa Tổ (F02)

| Mã | Kịch bản | Các bước | Kết quả mong đợi |
|---|---|---|---|
| TC02-01 | Sửa tên Tổ | 1. Chọn Tổ, bấm Sửa<br>2. Đổi tên, bấm Lưu | Tên mới hiển thị ngay trong danh sách |
| TC02-02 | Hủy xóa Tổ | 1. Chọn Tổ, bấm Xóa<br>2. Ở hộp thoại xác nhận, bấm Hủy | Tổ vẫn còn, không thay đổi |
| TC02-03 | Xác nhận xóa Tổ | 1. Chọn Tổ, bấm Xóa<br>2. Bấm Xác nhận | Tổ biến mất khỏi danh sách |
| TC02-04 | Xóa Tổ thì xóa luôn các từ bên trong | 1. Ghi nhớ số từ trong Tổ A (ví dụ 5 từ)<br>2. Xóa Tổ A<br>3. Tìm một trong các từ đó bằng F07 | Không tìm thấy từ nào của Tổ A |

## US03 – Xem danh sách Tổ và tiến độ (F03)

| Mã | Kịch bản | Các bước | Kết quả mong đợi |
|---|---|---|---|
| TC03-01 | Hiển thị số từ | 1. Tạo Tổ có 10 từ<br>2. Mở danh sách Tổ | Tổ hiển thị "10 từ" |
| TC03-02 | Tỉ lệ đã thuộc đúng | 1. Tổ có 10 từ, 4 từ đã thuộc<br>2. Mở danh sách Tổ | Tổ hiển thị 40% đã thuộc |
| TC03-03 | Tổ rỗng | 1. Tạo Tổ mới, không thêm từ<br>2. Mở danh sách Tổ | Hiển thị 0 từ, 0%, không báo lỗi chia cho 0 |

## US04 – Thêm từ (F04)

| Mã | Kịch bản | Các bước | Kết quả mong đợi |
|---|---|---|---|
| TC04-01 | Thêm từ đầy đủ | 1. Mở Tổ, bấm thêm từ<br>2. Nhập từ "hello", nghĩa "xin chào", phiên âm "/həˈləʊ/", ví dụ "Hello, world"<br>3. Bấm Lưu | Từ được lưu và hiển thị trong danh sách Tổ với đủ 4 thông tin |
| TC04-02 | Thêm từ chỉ có từ và nghĩa | 1. Nhập từ và nghĩa, bỏ trống phiên âm và ví dụ<br>2. Bấm Lưu | Từ được lưu thành công |
| TC04-03 | Thiếu từ | 1. Bỏ trống ô từ, nhập nghĩa<br>2. Bấm Lưu | Không lưu, báo lỗi ở ô từ |
| TC04-04 | Thiếu nghĩa | 1. Nhập từ, bỏ trống nghĩa<br>2. Bấm Lưu | Không lưu, báo lỗi ở ô nghĩa |

## US05 – Sửa và xóa từ (F05)

| Mã | Kịch bản | Các bước | Kết quả mong đợi |
|---|---|---|---|
| TC05-01 | Sửa nghĩa của từ | 1. Chọn một từ, bấm Sửa<br>2. Đổi nghĩa, bấm Lưu | Nghĩa mới hiển thị ngay |
| TC05-02 | Xóa từ | 1. Chọn một từ, bấm Xóa<br>2. Xác nhận nếu có hộp thoại | Từ biến mất khỏi danh sách Tổ |
| TC05-03 | Sửa từ thành ô trống | 1. Xóa hết nội dung ô nghĩa<br>2. Bấm Lưu | Không lưu, báo lỗi |

## US06 – Xem danh sách từ trong Tổ (F06)

| Mã | Kịch bản | Các bước | Kết quả mong đợi |
|---|---|---|---|
| TC06-01 | Hiển thị trạng thái từng từ | 1. Mở Tổ có từ ở các trạng thái khác nhau | Mỗi từ hiện kèm một trong ba trạng thái: Trứng, Chim non, Chim bay |
| TC06-02 | Từ mới có trạng thái Trứng | 1. Thêm một từ mới<br>2. Mở danh sách từ của Tổ | Từ mới hiện trạng thái Trứng |

## US07 – Tìm kiếm từ (F07)

| Mã | Kịch bản | Các bước | Kết quả mong đợi |
|---|---|---|---|
| TC07-01 | Tìm bằng một phần của từ | 1. Có từ "hello" trong một Tổ<br>2. Gõ "hel" vào ô tìm kiếm | Từ "hello" hiện trong kết quả, kèm tên Tổ chứa nó |
| TC07-02 | Tìm trong nhiều Tổ | 1. Có từ "hello" ở Tổ A và "help" ở Tổ B<br>2. Gõ "hel" | Cả hai từ hiện trong kết quả |
| TC07-03 | Không phân biệt hoa thường | 1. Gõ "HELLO" | Vẫn ra kết quả "hello" |
| TC07-04 | Không có kết quả | 1. Gõ chuỗi không tồn tại | Hiển thị thông báo không có kết quả |

## US08 – Lật thẻ (F08)

| Mã | Kịch bản | Các bước | Kết quả mong đợi |
|---|---|---|---|
| TC08-01 | Lật thẻ | 1. Bắt đầu học flashcard<br>2. Chạm vào thẻ | Thẻ lật sang mặt nghĩa |
| TC08-02 | Lật lại | 1. Chạm thẻ lần nữa | Thẻ lật về mặt từ |
| TC08-03 | Hiển thị tiến độ | 1. Học Tổ có 20 từ, đang ở thẻ thứ 3 | Hiển thị "3/20" |

## US09 – Đánh dấu đã thuộc hoặc chưa thuộc (F09)

| Mã | Kịch bản | Các bước | Kết quả mong đợi |
|---|---|---|---|
| TC09-01 | Hai nút rõ ràng | 1. Mở thẻ ở mặt nghĩa | Có hai nút "Đã thuộc" và "Chưa thuộc", dễ phân biệt |
| TC09-02 | Chọn "Đã thuộc" | 1. Bấm "Đã thuộc" | Chuyển sang thẻ kế tiếp, từ trước được ghi nhận là đã thuộc |
| TC09-03 | Chọn "Chưa thuộc" | 1. Bấm "Chưa thuộc" | Chuyển sang thẻ kế tiếp, từ trước được ghi nhận là chưa thuộc |
| TC09-04 | Thẻ cuối cùng | 1. Chọn đáp án cho thẻ cuối | Chuyển sang màn hình kết quả buổi học |

## US10 – Kết quả buổi học (F10)

| Mã | Kịch bản | Các bước | Kết quả mong đợi |
|---|---|---|---|
| TC10-01 | Số liệu đúng | 1. Học 10 từ, đánh dấu thuộc 7, chưa thuộc 3<br>2. Xem kết quả | Hiển thị đúng 7 thuộc, 3 chưa thuộc |
| TC10-02 | Học lại từ chưa thuộc | 1. Ở màn hình kết quả, bấm "Học lại các từ chưa thuộc" | Chỉ học 3 từ chưa thuộc |
| TC10-03 | Không có từ chưa thuộc | 1. Đánh dấu thuộc hết 10 từ<br>2. Xem kết quả | Không có lỗi; nút học lại không hiển thị |

## US11 – Trạng thái từ: Trứng, Chim non, Chim bay (F11)

Việc lên hạng dựa trên **thời gian học đều đặn** (tương tự chuỗi ngày của Duolingo), không chỉ dựa trên số lần đúng. Ngưỡng số ngày cụ thể cần chốt ⚠️.

| Mã | Kịch bản | Các bước | Kết quả mong đợi |
|---|---|---|---|
| TC11-01 | Từ mới là Trứng | 1. Thêm từ mới | Trạng thái là Trứng |
| TC11-02 | Lên Chim non | 1. Học từ đều đặn đủ số ngày theo ngưỡng đã chốt | Trạng thái chuyển sang Chim non |
| TC11-03 | Lên Chim bay | 1. Tiếp tục học đều đặn đủ số ngày theo ngưỡng đã chốt | Trạng thái chuyển sang Chim bay |
| TC11-04 | Chưa thuộc không làm giảm hạng | 1. Từ đang là Chim non<br>2. Đánh dấu "Chưa thuộc" | Trạng thái giữ nguyên |
| TC11-05 | Không học lâu thì tự giảm hạng | 1. Từ đang là Chim bay<br>2. Không mở app trong khoảng thời gian quy định | Khi mở lại app, trạng thái giảm xuống một bậc ⚠️ (cần chốt số ngày và mức giảm) |

## US12 – Trắc nghiệm (F12)

| Mã | Kịch bản | Các bước | Kết quả mong đợi |
|---|---|---|---|
| TC12-01 | Mỗi câu có 4 đáp án | 1. Bắt đầu bài trắc nghiệm | Mỗi câu hiển thị đúng 4 đáp án |
| TC12-02 | Chọn đúng | 1. Chọn đáp án đúng | Báo "Đúng" ngay sau khi chọn |
| TC12-03 | Chọn sai | 1. Chọn đáp án sai | Báo "Sai" ngay, hiển thị đáp án đúng |
| TC12-04 | Không trùng đáp án | 1. Lặp qua nhiều câu | Không có 2 đáp án giống nhau trong cùng một câu |
| TC12-05 | Tổ có ít hơn 4 từ | 1. Tổ chỉ có 2 từ<br>2. Bắt đầu trắc nghiệm | Hiển thị gợi ý thêm từ vào Tổ |

## US13 – Xem kết quả và từ làm sai (F13)

| Mã | Kịch bản | Các bước | Kết quả mong đợi |
|---|---|---|---|
| TC13-01 | Điểm đúng | 1. Làm bài 10 câu, đúng 7 câu<br>2. Xem kết quả | Hiển thị 7/10 |
| TC13-02 | Danh sách từ sai đúng | 1. Sai 3 câu<br>2. Xem kết quả | Danh sách có đúng 3 từ đã sai |
| TC13-03 | Làm đúng hết | 1. Trả lời đúng tất cả | Danh sách từ sai rỗng, hiển thị thông báo phù hợp |

## US14 – Điền từ (F14)

| Mã | Kịch bản | Các bước | Kết quả mong đợi |
|---|---|---|---|
| TC14-01 | Gõ đúng | 1. Hiển thị nghĩa "xin chào"<br>2. Gõ "hello", gửi | Chấm đúng |
| TC14-02 | Gõ sai | 1. Gõ "helo", gửi | Chấm sai, hiển thị đáp án đúng "hello" |
| TC14-03 | Bỏ qua hoa thường và khoảng trắng | 1. Gõ " HELLO " | Chấm đúng |

## US15 – Trang chủ (F15)

| Mã | Kịch bản | Các bước | Kết quả mong đợi |
|---|---|---|---|
| TC15-01 | Hiển thị ngày ấp trứng | 1. Mở app | Trang chủ hiển thị ngày ấp trứng hiện tại |
| TC15-02 | Số từ cần ôn đúng | 1. Có 5 từ đến hạn ôn hôm nay, 2 từ chưa đến hạn<br>2. Mở app | Hiển thị 5 từ cần ôn |
| TC15-03 | Không có từ cần ôn | 1. Không có từ đến hạn | Hiển thị 0 và thông báo phù hợp |

## US16 – Đàn chim của bạn (F16)

| Mã | Kịch bản | Các bước | Kết quả mong đợi |
|---|---|---|---|
| TC16-01 | Số liệu từng trạng thái | 1. Có 4 Trứng, 3 Chim non, 2 Chim bay<br>2. Mở thống kê | Hiển thị đúng 4, 3, 2 |
| TC16-02 | Tổng khớp | 1. Đếm tổng số từ trong app<br>2. So với tổng ba số trên thống kê | Hai số bằng nhau |
| TC16-03 | App chưa có từ | 1. Dùng app mới, không có từ | Hiển thị 0, 0, 0 |

## US17 – Nhắc học hằng ngày (F17)

| Mã | Kịch bản | Các bước | Kết quả mong đợi |
|---|---|---|---|
| TC17-01 | Thông báo đúng giờ | 1. Bật nhắc, đặt 20:00<br>2. Chờ đến 20:00 | Thông báo xuất hiện đúng giờ |
| TC17-02 | Tắt nhắc | 1. Tắt nhắc<br>2. Chờ đến giờ đã đặt | Không có thông báo |
| TC17-03 | Lặp lại hằng ngày | 1. Nhận thông báo hôm nay<br>2. Chờ đến ngày hôm sau | Thông báo lại đúng giờ |
| TC17-04 | Từ chối quyền thông báo | 1. Từ chối quyền thông báo (Android 13 trở lên)<br>2. Bật nhắc | App không crash, hiển thị hướng dẫn cấp quyền |

## US18 – Cài đặt giờ nhắc và chế độ tối (F18)

| Mã | Kịch bản | Các bước | Kết quả mong đợi |
|---|---|---|---|
| TC18-01 | Đổi giờ nhắc được lưu | 1. Đổi giờ nhắc sang 07:30<br>2. Đóng hẳn app, mở lại | Giờ nhắc vẫn là 07:30 |
| TC18-02 | Bật chế độ tối | 1. Bật chế độ tối | Giao diện đổi ngay sang nền tối |
| TC18-03 | Chế độ tối được giữ | 1. Bật chế độ tối<br>2. Đóng hẳn app, mở lại | Vẫn ở chế độ tối |
| TC18-04 | Tắt chế độ tối | 1. Tắt chế độ tối | Giao diện trở về sáng |

## US19 – Phát âm từ (F19) ⚠️

Chức năng này thuộc mức ưu tiên Thấp, cần xác nhận có nằm trong v1.0.0 không.

| Mã | Kịch bản | Các bước | Kết quả mong đợi |
|---|---|---|---|
| TC19-01 | Có nút loa | 1. Mở thẻ flashcard | Thẻ có nút loa |
| TC19-02 | Đọc từ | 1. Chạm nút loa | Đọc to từ đó bằng tiếng Anh |

## US20 – Lặp lại ngắt quãng (F20) ⚠️

Chức năng này thuộc mức ưu tiên Thấp, cần xác nhận có nằm trong v1.0.0 không.

| Mã | Kịch bản | Các bước | Kết quả mong đợi |
|---|---|---|---|
| TC20-01 | Từ sai được ôn sớm hơn | 1. Một từ làm sai, một từ làm đúng, cùng thời điểm<br>2. Kiểm tra lịch ôn | Từ làm sai có ngày ôn lại sớm hơn |
| TC20-02 | Khoảng cách tăng dần | 1. Đánh dấu đúng một từ liên tiếp nhiều buổi | Khoảng cách đến lần ôn kế tiếp tăng dần |

## US21 – Đăng nhập và đồng bộ (F21) ⚠️

Chức năng này thuộc mức ưu tiên Thấp và đi ngược với README ("không cần tài khoản"). Cần xác nhận có nằm trong v1.0.0 không.

| Mã | Kịch bản | Các bước | Kết quả mong đợi |
|---|---|---|---|
| TC21-01 | Đăng nhập thành công | 1. Nhập tài khoản và mật khẩu đúng | Đăng nhập thành công |
| TC21-02 | Sai mật khẩu | 1. Nhập sai mật khẩu | Báo lỗi, không đăng nhập |
| TC21-03 | Khôi phục dữ liệu trên máy mới | 1. Đăng nhập trên máy có dữ liệu<br>2. Đăng nhập trên máy mới | Dữ liệu được khôi phục đầy đủ |

---

## Thống kê

| Nhóm | Số US | Số test case |
|---|---|---|
| Quản lý Tổ và từ (US01–US07) | 7 | 24 |
| Học tập và kiểm tra (US08–US14) | 7 | 26 |
| Theo dõi và tiện ích (US15–US18) | 4 | 14 |
| Nâng cao (US19–US21) | 3 | 7 |
| **Tổng** | **21** | **71** |

---

## Đã chốt

- **Mô tả Tổ là tùy chọn** (TC01-02).
- **Nút "học lại" ẩn** khi không có từ chưa thuộc (TC10-03).
- **Lên hạng dựa trên thời gian học đều đặn**, tương tự chuỗi ngày của Duolingo. "Chưa thuộc" không làm giảm hạng. Nếu không học trong một thời gian, hạng tự giảm (US11, TC11-02 đến TC11-05).
- **Tổ có ít từ:** gợi ý thêm từ (TC12-05).

## Câu hỏi còn lại

1. **Ngưỡng ngày học:** cần bao nhiêu ngày học đều đặn để lên Chim non và Chim bay? Không học bao lâu thì hạng giảm, và giảm một bậc hay nhiều hơn? (TC11-02, TC11-03, TC11-05)
2. **Phạm vi v1.0.0** cho US19, US20, US21 (xem đề xuất bên dưới).
