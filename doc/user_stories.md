# User story – WordNest

| Mã | User story | Tiêu chí chấp nhận | Chức năng |
|---|---|---|---|
| US01 | Là người học, tôi muốn tạo một Tổ riêng để học đúng chủ đề mình cần. | Nhập tên bắt buộc (≤ 100 ký tự, không rỗng) và mô tả tùy chọn (≤ 500 ký tự); Tổ mới hiện ngay đầu danh sách | F01 |
| US02 | Là người học, tôi muốn sửa hoặc xóa Tổ để giữ danh sách gọn gàng. | Sửa cập nhật ngay ở danh sách và chi tiết; xóa phải có xác nhận; xóa Tổ thì mọi từ bên trong cũng bị xóa khỏi tìm kiếm và thống kê | F02 |
| US03 | Là người học, tôi muốn xem tất cả Tổ cùng tiến độ để biết nên học Tổ nào. | Mỗi Tổ hiển thị số từ và tỉ lệ đã thuộc (%); Tổ 0 từ hiện 0%; chưa có Tổ thì hiện trạng thái trống mời tạo Tổ | F03 |
| US04 | Là người học, tôi muốn thêm từ kèm nghĩa, phiên âm và ví dụ để hiểu từ trong ngữ cảnh. | Bắt buộc có từ (≤ 100) và nghĩa (≤ 500); phiên âm, ví dụ là tùy chọn; từ mới mặc định ở trạng thái 🥚 Trứng | F04 |
| US05 | Là người học, tôi muốn sửa hoặc xóa từ để chỉnh lại khi nhập sai. | Sửa lưu và hiển thị ngay, giữ nguyên trạng thái chim và lịch sử ôn; xóa phải có hộp thoại xác nhận | F05 |
| US06 | Là người học, tôi muốn xem danh sách từ trong một Tổ để biết mình đã thêm những gì. | Mỗi từ hiện kèm huy hiệu 🥚 Trứng / 🐣 Chim non / 🕊️ Chim bay, tự cập nhật trực tiếp sau buổi học | F06 |
| US07 | Là người học, tôi muốn tìm nhanh một từ để không phải lướt qua nhiều Tổ. | Gõ một phần của từ vẫn ra kết quả trên mọi Tổ kèm tên Tổ chứa từ; không lỗi với ký tự đặc biệt (`%`, `_`, `\`) | F07 |
| US08 | Là người học, tôi muốn lật thẻ để tự kiểm tra xem mình có nhớ nghĩa không. | Chạm vào thẻ thì lật sang mặt sau; có hiển thị tiến độ (ví dụ 3/20) | F08 |
| US09 | Là người học, tôi muốn đánh dấu đã thuộc hoặc chưa thuộc để app biết từ nào tôi còn yếu. | Có hai nút rõ ràng; chuyển sang thẻ kế tiếp sau khi chọn | F09 |
| US10 | Là người học, tôi muốn xem kết quả sau mỗi buổi học để biết mình tiến bộ ra sao. | Hiển thị số từ thuộc, chưa thuộc, và nút học lại các từ chưa thuộc | F10 |
| US11 | Là người học, tôi muốn từ của mình "lớn lên" khi tôi học đều để thấy việc học có thành quả. | Từ mới là trứng; học đúng nhiều lần thì thành chim non rồi chim bay | F11 |
| US12 | Là người học, tôi muốn làm bài trắc nghiệm để kiểm tra lại những từ đã học. | Mỗi câu có 4 đáp án; báo đúng/sai ngay sau khi chọn | F12 |
| US13 | Là người học, tôi muốn xem lại các từ mình làm sai để ôn tập đúng chỗ yếu. | Kết quả hiện điểm và danh sách từ sai | F13 |
| US14 | Là người học, tôi muốn gõ lại từ thay vì chỉ chọn đáp án để nhớ sâu hơn. | Hiển thị nghĩa (kèm câu ví dụ thay từ bằng `___` nếu có); người dùng nhập từ, so khớp để chấm đúng/sai | F14 |
| US15 | Là người học, tôi muốn mở app là thấy ngay hôm nay cần ôn gì để không mất thời gian suy nghĩ. | Trang chủ có ngày ấp trứng, số từ cần ôn, tối đa 2 Tủ thật mới nhất và nút "Xem tất cả" sang tab Các Tủ | F15 |
| US16 | Là người học, tôi muốn xem thống kê "Đàn chim của bạn" để theo dõi tiến độ lâu dài. | Hiển thị số trứng, chim non, chim bay | F16 |
| US17 | Là người học, tôi muốn nhận nhắc nhở mỗi ngày để duy trì thói quen học. | Có thông báo vào giờ đã chọn | F17 |
| US18 | Là người học, tôi muốn đổi giờ nhắc và bật chế độ tối để dùng app thoải mái hơn. | Cài đặt được lưu, mở lại app vẫn giữ nguyên | F18 |
| US19 | Là người học, tôi muốn nghe phát âm của từ để đọc đúng. | Có nút loa ở flashcard và trắc nghiệm; chạm vào đọc to từ tiếng Anh; báo "Chưa cài dữ liệu phát âm" nếu thiếu giọng đọc | F19 |
| US20 | Là người học, tôi muốn từ khó xuất hiện lại sớm hơn để nhớ lâu. | Từ làm sai hoặc chưa thuộc được xếp ôn lại sớm hơn từ dễ | F20 |
| US21 | Là người học, tôi muốn đăng nhập để không mất dữ liệu khi đổi điện thoại. | Đăng nhập được bằng tài khoản; dữ liệu khôi phục trên thiết bị mới | F21 |
