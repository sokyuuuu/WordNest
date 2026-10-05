# Khảo sát và phân tích – WordNest

## 1. Giới thiệu đề tài

WordNest là ứng dụng Android giúp người học ghi nhớ từ vựng bằng flashcard. Ứng dụng lấy hình ảnh chú chim làm điểm nhấn mỗi từ là một quả trứng được ấp dần cho đến khi thành chim trưởng thành bay đi, giúp việc học có cảm giác chăm sóc và tiến bộ rõ ràng.

Slogan Nuôi lớn vốn từ mỗi ngày.

## 2. Mục tiêu

- Giúp người học tự tạo bộ từ riêng và ôn tập đều đặn mỗi ngày
- Tạo động lực học bằng cơ chế nuôi chim thay vì chỉ dùng điểm số
- Gọn nhẹ, dùng được không cần mạng và không cần tài khoản
- Hoàn toàn miễn phí, không khóa tính năng

## 3. Đối tượng người dùng

- Học sinh, sinh viên học ngoại ngữ (tiếng Anh, tiếng Nhật, tiếng Hàn...)
- Người đi làm muốn tự học từ vựng chuyên ngành hoặc từ vựng giao tiếp
- Người cần ôn từ vựng cho một kỳ thi cụ thể (IELTS, TOEIC, JLPT...)

## 4. Khảo sát ứng dụng tương tự

### 4.1. Quizlet

Nền tảng flashcard cho mọi môn học, người dùng tự tạo hoặc dùng bộ thẻ do cộng đồng chia sẻ.

Điểm mạnh
- Kho bộ thẻ cộng đồng lớn, thường đã có sẵn bộ thẻ cần học
- Nhiều chế độ học flashcard, ghép thẻ, luyện viết, kiểm tra
- Giao diện dễ dùng, có hỗ trợ học nhóm và lớp học

Điểm yếu
- Gói miễn phí bị giới hạn (số vòng Learn, số bài kiểm tra, có quảng cáo)
- Gói Plus có giá 7,99 USDtháng hoặc 35,99 USDnăm; Plus Unlimited là 9,99 USDtháng hoặc 44,99 USDnăm
- Nhiều người dùng phàn nàn vì các tính năng từng miễn phí nay phải trả tiền
- Cơ chế lặp lại chỉ ở mức cơ bản, không phải spaced repetition đầy đủ

### 4.2. Anki

Phần mềm flashcard mã nguồn mở, nổi tiếng với lặp lại ngắt quãng.

Điểm mạnh
- Bản desktop, AnkiWeb và AnkiDroid (Android) miễn phí
- Có thuật toán lặp lại FSRS tự điều chỉnh theo lịch sử ôn tập của người dùng
- Tùy biến sâu thêm hình, âm thanh, thẻ điền chỗ trống, plugin

Điểm yếu
- Bản iPhoneiPad có giá 24,99 USD (mua một lần)
- Giao diện kém thân thiện, mất thời gian làm quen
- Trải nghiệm thiên về chức năng, ít tạo cảm hứng, từ vựng học tách rời ngữ cảnh

### 4.3. Duolingo

Ứng dụng học ngôn ngữ theo bài học có cấu trúc, game hóa mạnh.

Điểm mạnh
- Giúp người học duy trì thói quen mỗi ngày nhờ streak và các yếu tố game
- Bài học có sẵn, không cần tự chuẩn bị nội dung

Điểm yếu
- Người học không kiểm soát được từ vựng cụ thể mình muốn luyện
- Không phù hợp để học theo danh sách từ riêng (đề thi, giáo trình, công việc)

## 5. Bảng so sánh

 Tiêu chí  Quizlet  Anki  Duolingo  WordNest 
---------------
 Tự tạo bộ từ  Có  Có  Không  Có 
 Lặp lại ngắt quãng  Cơ bản  Mạnh  —  Nâng cao (làm sau) 
 Tạo động lực  Điểm, game  Ít  Streak, game hóa  Nuôi chim, ngày ấp trứng 
 Giao diện  Dễ dùng  Khó làm quen  Thân thiện  Đơn giản, dễ thương 
 Dùng offline  Phải trả phí  Có  Hạn chế  Có, không cần tài khoản 
 Chi phí  Nhiều tính năng bị khóa  Miễn phí (trừ iOS)  Có gói trả phí  Miễn phí 

_Dấu — nhóm chưa kiểm chứng. Cột WordNest là định hướng thiết kế._

## 6. Điểm khác biệt của WordNest

1. Dễ dùng như Quizlet nhưng không khóa tính năng miễn phí, không quảng cáo.
2. Có động lực như Duolingo nhưng vẫn học từ vựng của riêng mình cơ chế nuôi chim thay cho điểm số.
3. Gọn nhẹ hơn Anki giao diện đơn giản, mở lên là học, không cần cấu hình.

## 7. Phạm vi đồ án

Trong phạm vi (tuần 1 – thiết kế)
- Quản lý Tổ và từ vựng
- Học flashcard, kiểm tra trắc nghiệm và điền từ
- Thống kê, nhắc học, cài đặt
- Dữ liệu lưu cục bộ

Ngoài phạm vi (hướng mở rộng)
- Phát âm từ (Text-to-Speech)
- Lặp lại ngắt quãng
- Đăng nhập và đồng bộ Firebase
- Chia sẻ bộ từ giữa người dùng

Chi tiết chức năng và user story xem tại [`user-stories.md`](user-stories.md).

## 8. Công nghệ dự kiến

 Phần  Công nghệ 
------
 Nền tảng  Android 
 Ngôn ngữ  Kotlin 
 Giao diện  Jetpack Compose hoặc XML (theo yêu cầu môn học) 
 Kiến trúc  MVVM 
 Lưu trữ  Room (SQLite) 
 Điều hướng  Navigation Component 
 Nhắc học  WorkManager + Notification 
 Thiết kế  Figma, Material Design 3 

## 9. Nguồn tham khảo

- Trang giá và tính năng của Quizlet (quizlet.com)
- Trang chủ Anki và tài liệu về FSRS (apps.ankiweb.net)
- Các bài so sánh Anki, Quizlet, Duolingo năm 2026
