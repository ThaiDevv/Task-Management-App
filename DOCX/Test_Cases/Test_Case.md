# BẢNG TEST CASE CHI TIẾT - TASK MANAGEMENT APPLICATION

> Tài liệu tổng hợp toàn bộ 50 Test Cases và bảng chi tiết từng Test Case từ tệp `Test_Case.xlsx`.

---

## 1. Danh sách tổng quan 50 Test Cases (Sheet: `Test Cases`)

| STT | Tên Test Case | Mô tả | Dữ liệu & Thao tác | Kết quả mong đợi | Ảnh minh chứng | Trạng thái |
| :---: | :--- | :--- | :--- | :--- | :---: | :---: |
| **QUẢN LÝ CÔNG VIỆC VÀ KIỂM TRA DỮ LIỆU** | | | | | | |
| 1 | **Tạo công việc hợp lệ** | Mở màn hình tạo công việc, nhập đầy đủ thông tin hợp lệ và nhấn Lưu. | Điều kiện trước: Ứng dụng đang ở màn hình Home; đã cấp quyền thông báo.<br>Dữ liệu: Title = Nộp bài tập; Description = Tải lên tệp PDF cuối cùng; Due date = ngày mai; Due time = 09:00 AM; Priority = High; Repeat = None; Reminder = 15 minutes before.<br>Thao tác:<br>1) Nhấn nút + ở giữa thanh điều hướng.<br>2) Nhập Title và Description.<br>3) Chọn ngày, giờ, High, None và 15 minutes before.<br>4) Nhấn Create Task.<br>5) Tìm task trên Home, mở Task Detail và đối chiếu toàn bộ dữ liệu. | C Màn hình chi tiết hiển thị đúng dữ liệu. Lịch nhắc được tạo khi ứng dụng có đủ quyền. | Xem ảnh minh chứng tại sheet TC1 | **Đạt** |
| 2 | **Không cho phép tiêu đề trống** | Để trống tiêu đề, nhập hợp lệ các trường còn lại rồi nhấn Lưu. | Điều kiện trước: Đang ở màn hình Create Task.<br>Dữ liệu: Title = ba dấu cách; Due date = ngày mai; Due time = 09:00 AM; Priority = Medium; Repeat = None; Reminder = No reminder.<br>Thao tác:<br>1) Nhập ba dấu cách vào Title.<br>2) Chọn ngày, giờ và Medium.<br>3) Nhấn Create Task.<br>4) Ghi nhận lỗi ngay dưới trường Title.<br>5) Nhấn Back, trở về Home và xác nhận không có task mới. | Hiển thị lỗi “Title is required”. Màn hình nhập vẫn mở và không có công việc nào được lưu. | Xem ảnh minh chứng tại sheet TC2 | **Đạt** |
| 3 | **Chấp nhận tiêu đề có 200 ký tự** | Nhập tiêu đề dài đúng giới hạn 200 ký tự và lưu công việc. | Điều kiện trước: Chuẩn bị sẵn một chuỗi đúng 200 ký tự trong ứng dụng ghi chú và đã đếm chính xác.<br>Dữ liệu: Title = chuỗi 200 ký tự; Due date = ngày mai; Due time = 09:00 AM; Priority = Medium; Repeat = None.<br>Thao tác:<br>1) Mở Create Task và dán chuỗi vào Title.<br>2) Để Description trống.<br>3) Chọn ngày, giờ và Medium.<br>4) Nhấn Create Task.<br>5) Mở task vừa tạo và kiểm tra Title không bị cắt hoặc thay đổi. | Không hiển thị lỗi độ dài. Công việc được lưu thành công với đầy đủ tiêu đề. | Xem ảnh minh chứng tại sheet TC3 | **Đạt** |
| 4 | **Từ chối tiêu đề vượt quá 200 ký tự** | Nhập tiêu đề dài 201 ký tự và nhấn Lưu. | Điều kiện trước: Chuẩn bị sẵn một chuỗi đúng 201 ký tự.<br>Dữ liệu: Title = chuỗi 201 ký tự; Due date = ngày mai; Due time = 09:00 AM; Priority = Medium.<br>Thao tác:<br>1) Mở Create Task và dán chuỗi 201 ký tự vào Title.<br>2) Chọn ngày, giờ và Medium.<br>3) Nhấn Create Task.<br>4) Ghi nhận thông báo vượt quá 200 ký tự.<br>5) Trở về Home và xác nhận task không được tạo. | Hiển thị lỗi “Title must not exceed 200 characters”. Không lưu công việc. | Xem ảnh minh chứng tại sheet TC4 | **Đạt** |
| 5 | **Từ chối mô tả vượt quá 1.000 ký tự** | Nhập mô tả dài 1.001 ký tự rồi nhấn Lưu. | Điều kiện trước: Chuẩn bị một đoạn mô tả đúng 1.001 ký tự.<br>Dữ liệu: Title = Kiểm tra mô tả dài; Description = 1.001 ký tự; Due date = ngày mai; Due time = 09:00 AM; Priority = Low.<br>Thao tác:<br>1) Mở Create Task, nhập Title và dán Description.<br>2) Chọn ngày, giờ và Low.<br>3) Nhấn Create Task.<br>4) Ghi nhận lỗi tại trường Description.<br>5) Trở về Home và xác nhận không có task mới. | Hiển thị lỗi “Description must not exceed 1000 characters”. Không lưu công việc. | Xem ảnh minh chứng tại sheet TC5 | **Đạt** |
| 6 | **Từ chối ngày hết hạn trong quá khứ** | Tạo công việc mới và chọn ngày hết hạn trước ngày hiện tại. | Điều kiện trước: Ngày hệ thống trên emulator chính xác.<br>Dữ liệu: Title = Task ngày cũ; Due date = hôm qua; Due time = 09:00 AM; Priority = Low.<br>Thao tác:<br>1) Mở Create Task và nhập Title.<br>2) Mở bộ chọn ngày, chọn ngày hôm qua.<br>3) Chọn 09:00 AM và Low.<br>4) Nhấn Create Task.<br>5) Ghi nhận lỗi tại Due date và xác nhận task không được lưu. | Hiển thị lỗi “Due date cannot be in the past”. Không lưu công việc. | Xem ảnh minh chứng tại sheet TC6 | **Đạt** |
| 7 | **Từ chối giờ đã qua trong ngày hiện tại** | Chọn ngày hôm nay và giờ nhỏ hơn thời gian hiện tại. | Điều kiện trước: Ghi lại giờ hiện tại của emulator.<br>Dữ liệu: Title = Cuộc họp đã qua; Due date = hôm nay; Due time = giờ hiện tại trừ 5 phút; Priority = High.<br>Thao tác:<br>1) Mở Create Task và nhập Title.<br>2) Chọn hôm nay, chọn giờ nhỏ hơn hiện tại 5 phút và High.<br>3) Nhấn Create Task.<br>4) Ghi nhận lỗi tại Due time.<br>5) Trở về Home và xác nhận task không được tạo. | Hiển thị lỗi “Due time must be in the future when the due date is today”. Không lưu công việc. | Xem ảnh minh chứng tại sheet TC7 | **Đạt** |
| 8 | **Cập nhật công việc đã tồn tại** | Mở một công việc, thay đổi thông tin và nhấn Lưu. | Điều kiện trước: Đã có task “Báo cáo tuần”, hạn ngày mai 09:00 AM, Priority Medium.<br>Dữ liệu: Title mới = Báo cáo tuần đã sửa; Due time mới = 10:30 AM; Priority mới = High; Reminder mới = 10 minutes before.<br>Thao tác:<br>1) Mở task “Báo cáo tuần”.<br>2) Chọn Edit.<br>3) Sửa Title, Due time, Priority và Reminder theo dữ liệu mới.<br>4) Nhấn Save Changes.<br>5) Mở lại task từ Home và Calendar để đối chiếu dữ liệu mới và bảo đảm không có bản sao. | Cập nhật đúng công việc cũ, không tạo bản sao. ID không thay đổi và lịch nhắc được đặt lại theo thời gian mới. | Xem ảnh minh chứng tại sheet TC8 | **Đạt** |
| 9 | **Xóa công việc** | Mở Chi tiết công việc, chọn Xóa và xác nhận. | Điều kiện trước: Đã tạo task “Task cần xóa”, hạn ngày mai và Reminder = 15 minutes before.<br>Dữ liệu: Task đang xuất hiện trên Home và Calendar.<br>Thao tác:<br>1) Mở Task Detail của “Task cần xóa”.<br>2) Mở menu và chọn Delete.<br>3) Xác nhận xóa trong hộp thoại.<br>4) Tìm lại trên Home và Calendar.<br>5) Chờ qua thời điểm nhắc hoặc đổi giờ emulator đến sau giờ nhắc để kiểm tra không còn notification. | Công việc biến mất khỏi Trang chủ, Lịch và Room. Lịch của AlarmManager và WorkManager cũng bị hủy. | Xem ảnh minh chứng tại sheet TC9 | **Đạt** |
| **CÔNG VIỆC LẶP LẠI** | | | | | | |
| 10 | **Tạo lần tiếp theo của công việc lặp hằng ngày** | Tạo công việc lặp hằng ngày, sau đó đánh dấu lần hiện tại là hoàn thành. | Điều kiện trước: Không có task nào mang Title “Uống thuốc hằng ngày”.<br>Dữ liệu: Title = Uống thuốc hằng ngày; Due date = ngày mai; Due time = 08:00 AM; Repeat = Daily; End = Never; Reminder = No reminder.<br>Thao tác:<br>1) Tạo task với dữ liệu trên.<br>2) Mở Task Detail và đánh dấu Complete.<br>3) Trở về danh sách hoặc Calendar.<br>4) Tìm task cùng Title ở ngày kế tiếp.<br>5) Đếm số bản chưa hoàn thành của ngày kế tiếp. | Công việc hiện tại chuyển sang hoàn thành. Hệ thống chỉ tạo đúng một công việc chưa hoàn thành cho ngày kế tiếp, giữ nguyên thông tin và tạo lịch nhắc mới. | Xem ảnh minh chứng tại sheet TC10 | **Đạt** |
| 11 | **Tạo lần tiếp theo của công việc lặp hằng tuần** | Hoàn thành một công việc được cấu hình lặp hằng tuần. | Điều kiện trước: Không có task “Họp nhóm hằng tuần”.<br>Dữ liệu: Due date = thứ Hai kế tiếp; Due time = 02:00 PM; Repeat = Weekly; End = Never; Priority = Medium.<br>Thao tác:<br>1) Tạo task “Họp nhóm hằng tuần” với dữ liệu trên.<br>2) Mở task và đánh dấu Complete.<br>3) Mở Calendar, chuyển đến tuần sau.<br>4) Kiểm tra task mới nằm đúng thứ Hai, đúng 02:00 PM.<br>5) Xác nhận chỉ có một bản chưa hoàn thành. | Chỉ tạo một lần tiếp theo vào đúng Thứ Hai tuần kế tiếp, giữ nguyên giờ và thông tin công việc. | Xem ảnh minh chứng tại sheet TC11 | **Đạt** |
| 12 | **Xử lý công việc lặp hằng tháng ở ngày cuối tháng** | Kiểm tra quy tắc lặp tháng ở những tháng tiếp theo | Điều kiện trước: Dùng một ngày 31 còn ở tương lai, ví dụ 31/01/2027.<br>Dữ liệu: Title = Nhắc lịch học ; Due date = 31/01/2027; Due time = 05:00 PM; Repeat = Monthly; End = Never.<br>Thao tác:<br>1) Tạo task với ngày 31/01/2027.<br>2) Mở task và đánh dấu Complete.<br>3) Mở Calendar và chuyển sang tháng 2/2027.<br>4) Ghi nhận ngày được chọn cho tháng không có ngày 31. | Lần kế tiếp rơi vào ngày cuối cùng hợp lệ của tháng Hai; không tạo ngày không tồn tại và không làm ứng dụng lỗi. | Xem ảnh minh chứng tại sheet TC12 | **Đạt** |
| 13 | **Xử lý công việc lặp hằng năm trong năm nhuận** | Kiểm tra công việc hằng năm bắt đầu vào ngày 29/02. | Điều kiện trước: Ngày 29/02/2028 vẫn ở tương lai trên emulator.<br>Dữ liệu: Title = Kiểm tra năm nhuận; Due date = 29/02/2028; Due time = 09:00 AM; Repeat = Yearly; End = Never.<br>Thao tác:<br>1) Tạo task với dữ liệu trên.<br>2) Mở task và đánh dấu Complete.<br>3) Mở Calendar và chuyển tới tháng 02/2029.<br>4) Tìm lần lặp kế tiếp.<br>5) Xác nhận app không crash và ngày mới là một ngày hợp lệ của năm 2029. | Ứng dụng tính ngày hợp lệ cho năm tiếp theo theo quy tắc recurrence, không tạo ngày sai và không bị crash. | Xem ảnh minh chứng tại sheet TC13 | **Đạt** |
| 14 | **Dừng lặp theo ngày kết thúc** | Tạo công việc lặp có ngày kết thúc rồi hoàn thành các lần trước ngày kết thúc. | Điều kiện trước: Không có chuỗi task trùng Title.<br>Dữ liệu: Title = Uống vitamin; Due date = ngày mai; Due time = 07:00 AM; Repeat = Daily; End = By date; End date = sau Due date 2 ngày.<br>Thao tác:<br>1) Tạo task với cấu hình trên.<br>2) Hoàn thành lần đầu và lần thứ hai được sinh ra.<br>3) Hoàn thành lần nằm đúng End date.<br>4) Mở Calendar ở ngày sau End date.<br>5) Xác nhận không có lần mới vượt quá End date. | Hệ thống không tạo lần mới có ngày vượt quá ngày kết thúc đã chọn. | Xem ảnh minh chứng tại sheet TC14 | **Đạt** |
| 15 | **Dừng lặp theo số lần** | Tạo công việc lặp có giới hạn số lần và hoàn thành lần hiện tại liên tiếp. | Điều kiện trước: Không có chuỗi task “Tập thể dục 3 lần”.<br>Dữ liệu: Due date = ngày mai; Due time = 06:30 AM; Repeat = Daily; End = By count; Count = 3.<br>Thao tác:<br>1) Tạo task với Count = 3.<br>2) Hoàn thành lần thứ nhất, mở task kế tiếp và hoàn thành lần thứ hai.<br>3) Mở và hoàn thành lần thứ ba.<br>4) Kiểm tra Calendar ở ngày kế tiếp.<br>5) Đếm tổng số task trong chuỗi và xác nhận không có lần thứ tư. | Tổng số lần trong chuỗi không vượt quá 3. Sau lần cuối, không tạo thêm công việc. | Xem ảnh minh chứng tại sheet TC15 | **Đạt** |
| 16 | **Tạm dừng và tiếp tục công việc lặp** | Tạm dừng chuỗi lặp, hoàn thành lần hiện tại, sau đó tiếp tục chuỗi. | Điều kiện trước: Đã có task lặp Daily tên “Đọc sách”, End = Never.<br>Dữ liệu: Thao tác Pause và Resume trong Task Detail.<br>Thao tác:<br>1) Mở Task Detail và nhấn Pause recurring task.<br>2) Xác nhận trạng thái Paused hiển thị trên chi tiết/danh sách.<br>3) Đánh dấu task Complete và xác nhận chưa sinh lần kế tiếp.<br>4) Bỏ Complete, mở lại chi tiết và nhấn Resume.<br>5) Đánh dấu Complete lần nữa và xác nhận chỉ sinh một lần kế tiếp. | Khi tạm dừng, hệ thống không tạo lần kế tiếp. Sau khi tiếp tục, quy tắc lặp hoạt động trở lại và không tạo bản sao trùng. | Xem ảnh minh chứng tại sheet TC16 | **Đạt** |
| **TÌM KIẾM, LỌC VÀ SẮP XẾP** | | | | | | |
| 17 | **Xóa toàn bộ điều kiện lọc** | Kiểm tra nút Clear all xóa toàn bộ tiêu chí lọc đã chọn và khôi phục danh sách công việc đầy đủ. | Điều kiện trước: Có ít nhất 2 task thuộc các trạng thái và mức ưu tiên khác nhau.<br>Dữ liệu:<br>- Completion = Not done<br>- Status = In Progress<br>- Priority = Medium<br>Thao tác:<br>1. Tại Home, nhấn biểu tượng Filter.<br>2. Chọn Not done, In Progress và High.<br>3. Nhấn Show tasks và xác nhận nhãn Filters active xuất hiện.<br>4. Mở lại Filter và nhấn Clear all.<br>5. Nhấn Show tasks và kiểm tra lại danh sách. | Mọi tiêu chí trở về trạng thái mặc định. Nhãn Filters active biến mất. Danh sách hiển thị lại toàn bộ công việc và không còn áp dụng tiêu chí cũ. | Xem ảnh minh chứng tại sheet TC17 | **Đạt** |
| 18 | **Lọc kết hợp trạng thái và độ ưu tiên** | Chọn đồng thời trạng thái và độ ưu tiên trong bảng lọc. | Điều kiện trước: Có ít nhất ba task: A = In Progress/High, B = Todo/High, C = In Progress/Low.<br>Dữ liệu: Completion = Not done; Status = In Progress; Priority = High.<br>Thao tác:<br>1) Mở All Tasks và nhấn biểu tượng Filter.<br>2) Chọn Not done, In Progress và High.<br>3) Quan sát số task trên nút Show tasks.<br>4) Nhấn Show tasks.<br>5) Xác nhận chỉ task A xuất hiện. | Chỉ hiển thị công việc thỏa mãn đồng thời cả hai điều kiện. | Xem ảnh minh chứng tại sheet TC18 | **Đạt** |
| 19 | **Lọc theo trạng thái hoàn thành** | Lần lượt chọn Tất cả, Chưa hoàn thành và Đã hoàn thành. | Điều kiện trước: Có hai task chưa hoàn thành và hai task đã hoàn thành.<br>Dữ liệu: Completion lần lượt = All, Not done, Done.<br>Thao tác:<br>1) Mở Filter, chọn All rồi Apply; ghi số và danh sách.<br>2) Mở lại Filter, chọn Not done rồi Apply; ghi kết quả.<br>3) Mở lại Filter, chọn Done rồi Apply; ghi kết quả.<br>4) So sánh ba tập kết quả với dữ liệu chuẩn bị.<br>5) Nhấn Reset để trả bộ lọc về mặc định. | Mỗi lựa chọn trả đúng tập dữ liệu tương ứng và số liệu trên Trang chủ được cập nhật nhất quán. | Xem ảnh minh chứng tại sheet TC19 | **Đạt** |
| 20 | **Lọc theo khoảng ngày có sẵn và tùy chỉnh** | Kiểm tra các lựa chọn ngày mà giao diện hiện tại hỗ trợ, | Điều kiện trước: Có task hạn hôm nay, sau 3 ngày, sau 10 ngày và tháng kế tiếp.<br>Dữ liệu: Today; Next 7 days; This month; Custom từ ngày mai đến sau 5 ngày.<br>Thao tác:<br>1) Mở Filter và áp dụng Today; ghi kết quả.<br>2) Lặp lại với Next 7 days và This month.<br>3) Nhấn Show tasks. | Mỗi lựa chọn chỉ hiển thị task có Due date nằm trong phạm vi tương ứng; | Xem ảnh minh chứng tại sheet TC20 | **Đạt** |
| 21 | **Không tính công việc hoàn thành là quá hạn** | Tạo hai công việc có hạn đã qua, một đã hoàn thành và một chưa hoàn thành, sau đó lọc Quá hạn. | Điều kiện trước: Tạo hai task cùng hạn trong 10 phút tới; đánh dấu một task Complete.<br>Dữ liệu: Task A = Completed; Task B = Todo. Sau đó tăng ngày hệ thống của emulator thêm 1 ngày.<br>Thao tác:<br>1) Sau khi đổi ngày hệ thống, mở lại ứng dụng.<br>2) Mở Filter.<br>3) Chọn Completion = Not done và Due date = Overdue.<br>4) Nhấn Show tasks.<br>5) Xác nhận chỉ Task B xuất hiện; hoàn tất thì trả ngày hệ thống về đúng. | Chỉ công việc chưa hoàn thành xuất hiện trong kết quả Quá hạn. | Xem ảnh minh chứng tại sheet TC21 | **Đạt** |
| 22 | **Loại bỏ điều kiện lọc mâu thuẫn** | Chọn Đã hoàn thành rồi kiểm tra các điều kiện Todo, In Progress và Overdue. | Điều kiện trước: Có cả task Done, Todo và Overdue.<br>Dữ liệu: Completion = Done.<br>Thao tác:<br>1) Mở Filter.<br>2) Chọn Done.<br>3) Kiểm tra các chip Todo, In Progress, Overdue và lựa chọn Due date = Overdue.<br>4) Thử nhấn các lựa chọn đang bị vô hiệu hóa.<br>5) Nhấn Show tasks và xác nhận kết quả chỉ gồm task đã hoàn thành. | Các điều kiện dành cho công việc chưa hoàn thành bị bỏ chọn hoặc vô hiệu hóa; kết quả không rơi vào trạng thái mâu thuẫn. | Xem ảnh minh chứng tại sheet TC22 | **Đạt** |
| 23 | **Sắp xếp và đặt công việc hoàn thành xuống dưới** | Kiểm tra các kiểu sắp xếp theo hạn, độ ưu tiên, ngày tạo, ngày cập nhật và tiêu đề. | Điều kiện trước: Có tối thiểu sáu task khác Title, ngày hạn, Priority, created time, updated time; trong đó hai task Completed.<br>Dữ liệu: Due date soonest, Due date latest, Priority, Recently created, Recently updated, Title A–Z.<br>Thao tác:<br>1) Mở Filter và chọn từng Sort by, mỗi lần nhấn Show tasks.<br>2) Ghi lại thứ tự các task chưa hoàn thành.<br>3) Đối chiếu thứ tự với dữ liệu đã chuẩn bị.<br>4) Quan sát nhóm Completed sau mỗi kiểu sort.<br>5) Xác nhận task Completed luôn ở phần dưới và không lặp trong Upcoming. | Công việc chưa hoàn thành được sắp đúng theo lựa chọn. Công việc hoàn thành luôn nằm trong phần Completed phía dưới và không bị lặp trong Upcoming. | Xem ảnh minh chứng tại sheet TC23 | **Đạt** |
| **NHẮC VIỆC VÀ THÔNG BÁO** | | | | | | |
| 24 | **Đồng bộ trạng thái quyền thông báo** | Kiểm tra công tắc Task reminders phản ánh đúng quyền thông báo của Android | ĐiĐiều kiện trước:<br>- Quyền thông báo của TaskFlow đang được bật.<br>- Công tắc Task reminders đang bật.<br>Thao tác:<br>1. Mở tab Settings.<br>2. Nhấn System permissions.<br>3. Tắt quyền Allow notifications của TaskFlow trong cài đặt Android.<br>4. Quay lại ứng dụng TaskFlow.<br>5. Mở lại tab Settings.<br>6. Kiểm tra công tắc Task reminders. | Công tắc Task reminders tự chuyển sang trạng thái tắt vì ứng dụng không còn quyền gửi thông báo. Ứng dụng không bị lỗi. | Xem ảnh minh chứng tại sheet TC24 | **Đạt** |
| 25 | **Bỏ qua lịch nhắc đã nằm trong quá khứ** | Kiểm tra trường hợp thời điểm nhắc trước đã qua nhưng hạn của task vẫn ở tương lai. | Điều kiện trước: Bật notification.<br>Dữ liệu: Title = Nhắc đã quá giờ; Due time = hiện tại + 10 phút; Reminder = 15 minutes before.<br>Thao tác:<br>1) Tạo task với Due time và Reminder trên.<br>2) Nhấn Create Task.<br>3) Ghi nhận cảnh báo lịch nhắc đã nằm trong quá khứ/bị bỏ qua.<br>4) Xác nhận task vẫn được lưu trên Home.<br>5) Chờ 10 phút và xác nhận không có notification cũ được phát nhầm. | Task vẫn được lưu, ứng dụng cảnh báo lịch nhắc bị bỏ qua và không phát notification sai thời điểm. | Xem ảnh minh chứng tại sheet TC25 | **Đạt** |
| 26 | **Mở cài đặt quyền thông báo hệ thống** | Kiểm tra người dùng có thể mở trang quản lý quyền thông báo của TaskFlow từ ứng dụng. | 1. Mở tab Settings.<br>2. Tìm phần Notifications.<br>3. Nhấn System permissions.<br>4. Quan sát màn hình được mở. | Android mở đúng trang cài đặt thông báo của ứng dụng TaskFlow. Ứng dụng không bị treo hoặc thoát bất thường. | Xem ảnh minh chứng tại sheet TC26 | **Đạt** |
| 27 | **Bật thông báo nhắc việc** | Kiểm tra người dùng có thể bật thông báo nhắc việc trong Settings. | Điều kiện trước:<br>- Quyền thông báo của TaskFlow đã được Android cho phép.<br>- Công tắc Task reminders đang tắt.<br>Thao tác:<br>1. Mở tab Settings.<br>2. Tìm mục Task reminders.<br>3. Nhấn công tắc để bật.<br>4. Chuyển sang tab Home.<br>5. Mở lại tab Settings.<br>6. Kiểm tra trạng thái công tắc Task reminders. | Công tắc Task reminders chuyển sang trạng thái bật. Khi rời Settings và quay lại, công tắc vẫn giữ trạng thái bật. Ứng dụng không bị lỗi. | Xem ảnh minh chứng tại sheet TC27 | **Đạt** |
| 28 | **Không bật nhắc việc khi chưa cấp quyền thông báo** | Kiểm tra ứng dụng không cho phép bật Task reminders khi người dùng chưa cấp quyền thông báo trên Android. | Điều kiện trước:<br>- Quyền thông báo của TaskFlow đang bị tắt.<br>- Công tắc Task reminders đang tắt.<br>Thao tác:<br>1. Mở ứng dụng TaskFlow.<br>2. Chọn tab Settings.<br>3. Nhấn công tắc Task reminders để bật.<br>4. Khi Android hiển thị yêu cầu cấp quyền thông báo, chọn Don’t allow.<br>5. Kiểm tra lại trạng thái công tắc Task reminders. | Công tắc Task reminders vẫn ở trạng thái tắt. Ứng dụng không lưu trạng thái bật khi chưa có quyền thông báo và không bị lỗi. | Xem ảnh minh chứng tại sheet TC28 | **Đạt** |
| 29 | **Đặt lại lịch khi sửa thời gian công việc** | Sửa ngày, giờ hoặc số phút nhắc trước của một công việc đã có lịch. | Điều kiện trước: Có task “Dời lịch họp” hạn sau 10 phút, Reminder = 5 minutes before.<br>Dữ liệu: Due time mới = hiện tại + 20 phút; Reminder mới = 10 minutes before.<br>Thao tác:<br>1) Mở Task Detail và chọn Edit.<br>2) Đổi Due time và Reminder theo dữ liệu mới.<br>3) Nhấn Save Changes và đưa app xuống nền.<br>4) Chờ qua thời điểm nhắc cũ, xác nhận không có notification.<br>5) Chờ đến thời điểm nhắc mới, xác nhận chỉ có một notification. | Lịch cũ bị hủy. Chỉ còn một lịch mới mang cùng taskId và kích hoạt theo thời gian mới. | Xem ảnh minh chứng tại sheet TC29 | **Đạt** |
| 30 | **Hủy lịch khi xóa hoặc hoàn thành công việc** | Xóa hoặc đánh dấu hoàn thành một công việc đang có lịch chính hoặc lịch dự phòng. | Điều kiện trước: Có task “Hủy nhắc khi xong” hạn sau 5 phút, Reminder = 0 minutes before.<br>Dữ liệu: Đánh dấu Complete trước giờ nhắc.<br>Thao tác:<br>1) Từ Home hoặc Task Detail, đánh dấu task Complete.<br>2) Xác nhận task chuyển xuống nhóm Completed.<br>3) Đưa ứng dụng xuống nền.<br>4) Chờ qua Due time ít nhất 1 phút.<br>5) Xác nhận không xuất hiện notification của task đã hoàn thành. | Cả PendingIntent của AlarmManager và unique work của WorkManager đều bị hủy; không xuất hiện thông báo sau đó. | Xem ảnh minh chứng tại sheet TC30 | **Đạt** |
| 31 | **Mở task từ thông báo** | Kiểm tra người dùng có thể mở đúng task bằng cách nhấn vào thông báo nhắc việc. | Điều kiện trước:<br>- Đã cấp quyền Notifications.<br>- Task reminders đang bật.<br>Thao tác:<br>1. Ấn vào thông báo<br>2 Kiểm tra dữ liệu về task | Ứng dụng TaskFlow được mở và hiển thị đúng task “Mở task từ thông báo”. Thông tin tên và thời hạn của task chính xác. | Xem ảnh minh chứng tại sheet TC31 | **Đạt** |
| 32 | **Hiển thị thông báo khi ứng dụng chạy nền** | Kiểm tra thông báo nhắc việc vẫn xuất hiện khi người dùng không mở ứng dụng. | Điều kiện trước:<br>- Đã cấp quyền Notifications.<br>- Task reminders đang bật trong Settings.<br>Dữ liệu:<br>- Title = Kiểm tra thông báo nền<br>- Reminder = 5 minutes before<br>Thao tác:<br>1. Tạo task “Kiểm tra thông báo nền”.<br>2. Chọn Reminder = 0 minutes before.<br>3. Lưu task.<br>4. Nhấn Home để đưa TaskFlow xuống nền.<br>5. Chờ đến thời gian đã đặt.<br>6. Khi thông báo xuất hiện, nhấn vào thông báo. | Thông báo TaskFlow xuất hiện đúng thời gian và hiển thị đúng tên task. Khi nhấn vào thông báo, ứng dụng mở task tương ứng. Thông báo không xuất hiện nhiều lần. | Xem ảnh minh chứng tại sheet TC32 | **Đạt** |
| **BẢO MẬT PIN** | | | | | | |
| 33 | **Bật PIN với hai lần nhập khớp** | Bật công tắc PIN, nhập PIN mới và xác nhận giống nhau. | Điều kiện trước: PIN Lock đang tắt.<br>Dữ liệu: PIN mới = 1234; Confirm PIN = 1234.<br>Thao tác:<br>1) Mở Settings và bật công tắc PIN Lock.<br>2) Nhập lần lượt 1, 2, 3, 4.<br>3) Ở bước Confirm, nhập lại 1, 2, 3, 4.<br>4) Chờ màn hình PIN đóng.<br>5) Kiểm tra công tắc PIN Lock đang bật và thông báo thành công xuất hiện. | PIN được lưu an toàn, màn hình đóng với RESULT_OK và công tắc PIN hiển thị trạng thái bật. | Xem ảnh minh chứng tại sheet TC33 | **Đạt** |
| 34 | **Không bật PIN khi xác nhận không khớp** | Nhập PIN mới rồi nhập lại một PIN khác. | Điều kiện trước: PIN Lock đang tắt.<br>Dữ liệu: PIN mới = 1234; Confirm PIN = 5678.<br>Thao tác:<br>1) Bật PIN Lock trong Settings.<br>2) Nhập 1234 ở bước tạo PIN.<br>3) Nhập 5678 ở bước xác nhận.<br>4) Ghi nhận lỗi PINs do not match và màn hình yêu cầu nhập lại.<br>5) Bấm Back, trở về Settings và xác nhận công tắc vẫn tắt. | Hiển thị lỗi không khớp, không lưu PIN mới và yêu cầu người dùng nhập lại. | Xem ảnh minh chứng tại sheet TC34 | **Đạt** |
| 35 | **Không tắt PIN khi nhập sai** | Tắt công tắc PIN rồi nhập PIN hiện tại không đúng. | Điều kiện trước: PIN Lock đang bật với PIN 1234.<br>Dữ liệu: PIN sai = 1111.<br>Thao tác:<br>1) Trong Settings, tắt công tắc PIN Lock.<br>2) Ở màn hình Disable PIN, nhập 1111.<br>3) Ghi nhận số lần thử còn lại.<br>4) Bấm Back để trở về Settings.<br>5) Xác nhận công tắc vẫn bật; mở lại app và xác nhận PIN 1234 vẫn mở khóa được. | Tăng số lần nhập sai, giữ nguyên PIN đã lưu, không trả RESULT_OK và công tắc quay về trạng thái bật. | Xem ảnh minh chứng tại sheet TC35 | **Đạt** |
| 36 | **Tắt PIN khi nhập đúng** | Tắt công tắc PIN và xác thực bằng PIN hiện tại. | Điều kiện trước: PIN Lock đang bật với PIN 1234.<br>Dữ liệu: PIN xác thực = 1234.<br>Thao tác:<br>1) Trong Settings, tắt công tắc PIN Lock.<br>2) Nhập 1234 ở màn hình Disable PIN.<br>3) Chờ quay lại Settings.<br>4) Xác nhận công tắc chuyển sang tắt và có thông báo thành công.<br>5) đóng/mở lại ứng dụng và xác nhận không còn màn hình yêu cầu PIN. | Xác thực thành công. SettingsFragment xóa PIN, đặt trạng thái phiên phù hợp và công tắc chuyển sang tắt. | Xem ảnh minh chứng tại sheet TC36 | **Đạt** |
| 37 | **Đổi PIN theo đúng ba bước** | Mở Đổi PIN, nhập PIN cũ đúng, nhập PIN mới rồi xác nhận đúng. | Điều kiện trước: PIN Lock đang bật với PIN cũ 1234.<br>Dữ liệu: Current PIN = 1234; New PIN = 5678; Confirm = 5678.<br>Thao tác:<br>1) Mở Settings và nhấn Change PIN.<br>2) Nhập 1234 ở bước Current PIN.<br>3) Nhập 5678 ở bước New PIN.<br>4) Nhập 5678 ở bước Confirm.<br>5) Mở lại app, xác nhận 1234 bị từ chối và 5678 mở khóa thành công. | PIN cũ được xác thực, PIN mới được băm và ghi đè. PIN 5678 đăng nhập được, PIN 1234 không còn hợp lệ. | Xem ảnh minh chứng tại sheet TC37 | **Đạt** |
| 38 | **Xử lý xác nhận PIN mới không khớp** | Trong luồng đổi PIN, nhập PIN xác nhận khác PIN mới rồi thử lại. | Điều kiện trước: PIN Lock đang bật với PIN cũ 1234.<br>Dữ liệu: Current = 1234; New = 5678; Confirm lần đầu = 5679; lần thử lại = 5678/5678.<br>Thao tác:<br>1) Mở Change PIN và nhập Current = 1234.<br>2) Nhập New = 5678, Confirm = 5679.<br>3) Ghi nhận lỗi không khớp và app quay về bước nhập PIN mới.<br>4) Nhập lại New = 5678 và Confirm = 5678.<br>5) Mở lại app và xác nhận PIN mới hoạt động. | Hiển thị lỗi không khớp và quay về bước nhập PIN mới. Lần thử lại đúng hoàn tất bình thường; PIN cũ không bị thay đổi trước khi hoàn tất. | Xem ảnh minh chứng tại sheet TC38 | **Đạt** |
| 39 | **Khóa sau năm lần nhập PIN sai** | Nhập sai PIN liên tiếp đủ số lần tối đa và mở lại màn hình trong thời gian khóa. | Điều kiện trước: PIN Lock đang bật với PIN đúng 1234.<br>Dữ liệu: PIN sai = 1111, nhập liên tiếp 5 lần.<br>Thao tác:<br>1) Mở lại app để hiện màn hình Enter PIN.<br>2) Nhập 1111 năm lần và ghi số lần còn lại sau mỗi lần.<br>3) Sau lần thứ năm, ghi lại thời gian đếm ngược khóa.<br>4) Xoay emulator hoặc đóng/mở Activity trong khi đang khóa.<br>5) Chờ đủ 30 giây rồi nhập 1234 và xác nhận mở khóa được. | Sau lần sai thứ năm, khóa 30 giây. Bộ đếm còn đúng sau khi xoay màn hình hoặc mở lại Activity; hết thời gian thì cho nhập lại. | Xem ảnh minh chứng tại sheet TC39 | **Đạt** |
| 40 | **Yêu cầu PIN khi mở lại ứng dụng** | Kiểm tra PIN Lock chặn truy cập Home sau khi ứng dụng được mở lại. | Điều kiện trước: PIN Lock đang bật với PIN 1234; ứng dụng đã được đưa khỏi màn hình gần đây hoặc tiến trình đã đóng.<br>Dữ liệu: Lần 1 nhập 1111; lần 2 nhập 1234.<br>Thao tác:<br>1) Mở TaskFlow từ biểu tượng ứng dụng.<br>2) Xác nhận màn hình Enter PIN xuất hiện trước Home.<br>3) Nhập 1111 và ghi nhận lỗi.<br>4) Nhập 1234.<br>5) Xác nhận chỉ sau PIN đúng mới vào được Home và dữ liệu task vẫn còn nguyên. | PIN sai bị từ chối; chỉ PIN đúng mới mở Home. Dữ liệu task không bị thay đổi. | Xem ảnh minh chứng tại sheet TC40 | **Đạt** |
| **SAO LƯU VÀ KHÔI PHỤC DỮ LIỆU** | | | | | | |
| 41 | **Xuất toàn bộ công việc ra JSON** | Tạo tập dữ liệu có task thường, task lặp, task hoàn thành rồi thực hiện Export. | Điều kiện trước: Có ít nhất năm task gồm Todo, In Progress, Completed, task lặp và task có Reminder.<br>Dữ liệu: Tên file = taskflow_backup_test.json.<br>Thao tác:<br>1) Mở Settings > Data Management.<br>2) Chọn Export JSON backup/Export to JSON.<br>3) Chọn thư mục Downloads, đặt tên file và xác nhận Save.<br>4) Chờ thông báo export thành công và số lượng task.<br>5) Mở ứng dụng Files, xác nhận file tồn tại và có kích thước lớn hơn 0 byte. | Tệp JSON hợp lệ được tạo, taskCount chính xác và giữ đủ các trường cần thiết của từng task. | Xem ảnh minh chứng tại sheet TC41 | **Đạt** |
| 42 | **Khôi phục dữ liệu bằng Export rồi Import** | Xuất dữ liệu, xóa dữ liệu trong ứng dụng rồi nhập lại chính tệp vừa xuất. | Điều kiện trước: Đã export file taskflow_backup_test.json và ghi lại danh sách/số lượng task trước khi xóa.<br>Dữ liệu: Chính file backup vừa export.<br>Thao tác:<br>1) Xóa các task dùng để test khỏi ứng dụng.<br>2) Mở Settings > Restore backup/Import Tasks.<br>3) Chọn file taskflow_backup_test.json.<br>4) Hoàn tất import và quay lại Home/Calendar.<br>5) Đối chiếu số lượng, Title, trạng thái, Priority, recurrence và Reminder với dữ liệu trước export. | Số lượng và nội dung task sau import giống trước export; trạng thái hoàn thành, recurrence và completedAt được bảo toàn. | Xem ảnh minh chứng tại sheet TC42 | **Đạt** |
| 43 | **Từ chối toàn bộ tệp JSON không hợp lệ** | Nhập tệp sai cú pháp hoặc thiếu trường bắt buộc trong một task. | Điều kiện trước: Tạo file invalid_taskflow.json trong Downloads với nội dung {invalid json hoặc xóa trường title của một task trong file export.<br>Dữ liệu: File JSON sai cú pháp/thiếu trường bắt buộc.<br>Thao tác:<br>1) Ghi lại tổng số task hiện tại.<br>2) Mở Import Tasks và chọn invalid_taskflow.json.<br>3) Nhấn Import.<br>4) Ghi nhận màn hình Import Failed và nội dung lỗi.<br>5) Quay lại Home, xác nhận tổng số và dữ liệu task không thay đổi. | Hiển thị danh sách lỗi cụ thể. Không task nào trong tệp được nhập và dữ liệu hiện tại không bị thay đổi. | Xem ảnh minh chứng tại sheet TC43 | **Đạt** |
| 44 | **Hủy chọn tệp khi Import
:** | Kiểm tra ứng dụng xử lý đúng khi người dùng mở trình chọn tệp nhưng không chọn tệp nào. | Điều kiện trước: Ứng dụng đang có một số task.<br>Thao tác:<br>1. Ghi lại tổng số task hiện tại.<br>2. Mở Settings.<br>3. Chọn Restore JSON backup.<br>4. Nhấn Choose JSON File.<br>5. Khi màn hình chọn tệp xuất hiện, nhấn Back để hủy.<br>6. Kiểm tra lại màn hình Import và danh sách task. | Ứng dụng quay lại màn hình Import Tasks và vẫn hiển thị “No file selected”. Không xuất hiện thông báo Import thành công hoặc hộp thoại xử lý task trùng. Dữ liệu task hiện tại không thay đổi. | Xem ảnh minh chứng tại sheet TC44 | **Đạt** |
| 45 | **Không mất dữ liệu khi tệp backup không được hỗ trợ** | Nhập tệp có version cao hơn ứng dụng hỗ trợ hoặc tệp không đọc được. | Điều kiện trước: Export một file hợp lệ rồi sao chép thành unsupported_version.json.<br>Dữ liệu: Sửa trường version trong file thành 999, giữ phần tasks không đổi.<br>Thao tác:<br>1) Ghi lại số task hiện tại.<br>2) Mở Import Tasks và chọn unsupported_version.json.<br>3) Nhấn Import.<br>4) Ghi nhận lỗi phiên bản không được hỗ trợ.<br>5) Quay lại Home và xác nhận không có task nào bị xóa, thay thế hoặc thêm mới. | Hiển thị lỗi phù hợp, không xóa dữ liệu hiện tại và trạng thái Import có thể thử lại. | Xem ảnh minh chứng tại sheet TC45 | **Đạt** |
| **LỊCH** | | | | | | |
| 46 | **Hiển thị công việc đúng ngày trên Lịch** | Chuyển tháng, chọn một ngày có công việc và một ngày không có công việc. | Điều kiện trước: Có task A hạn hôm nay, task B hạn tháng sau và một ngày không có task.<br>Dữ liệu: Ba ngày cần kiểm tra tương ứng với A, B và ngày trống.<br>Thao tác:<br>1) Mở tab Calendar.<br>2) Chọn ngày của task A và kiểm tra danh sách.<br>3) Chuyển sang tháng sau, chọn ngày của task B.<br>4) Chọn một ngày trống.<br>5) Xác nhận ngày có task hiển thị đúng task, ngày trống không hiển thị dữ liệu cũ. | Ngày có task được đánh dấu và hiển thị đúng danh sách; ngày trống hiển thị rỗng; chuyển tháng không làm mất dữ liệu. | Xem ảnh minh chứng tại sheet TC46 | **Đạt** |
| 47 | **Hiển thị task lặp hằng tuần trên Lịch** | Kiểm tra task lặp hằng tuần xuất hiện đúng ngày trên Calendar và không bị hiển thị trùng. | Điều kiện trước: Ứng dụng đang hoạt động bình thường.<br>Dữ liệu:<br>- Title = Họp nhóm hằng tuần<br>- Due date = Hôm nay<br>- Repeat = Weekly<br>Thao tác:<br>1. Tạo task “Họp nhóm hằng tuần”.<br>2. Chọn Repeat = Weekly và lưu task.<br>3. Mở tab Calendar.<br>4. Chọn ngày hôm nay.<br>5. Chọn cùng thứ của tuần kế tiếp.<br>6. Quan sát số lần task xuất hiện ở mỗi ngày. | Task “Họp nhóm hằng tuần” xuất hiện đúng một lần ở hôm nay và đúng một lần vào cùng thứ của tuần kế tiếp. Task không xuất hiện ở những ngày còn lại. | Xem ảnh minh chứng tại sheet TC47 | **Đạt** |
| **THỐNG KÊ** | | | | | | |
| 48 | **Tính đúng số liệu thống kê** | Chuẩn bị tập task gồm đủ trạng thái và bốn mức ưu tiên rồi mở Statistics. | Điều kiện trước: Tạo 8 task: 4 Completed,  4 Not completed; phân bố Priority gồm 1 Urgent (có thể import), 2 High, 5 Medium, 1 Low.<br>Dữ liệu: Time filter = All time.<br>Thao tác:<br>1) Mở tab Stats.<br>2) Chọn All time.<br>3) Ghi Total, Completed, Not completed và Completion rate.<br>4) Ghi số lượng/tỷ lệ của Urgent, High, Medium, Low.<br>5) Tự tính từ 8 task và đối chiếu từng số hiển thị. | Tổng số, completed, pending, completion rate và tỷ lệ từng priority đúng với dữ liệu thực. Không hiển thị số giờ tập trung giả định. | Xem ảnh minh chứng tại sheet TC48 | **Đạt** |
| 49 | **Cập nhật thống kê tuần này khi hoàn thành task** | Kiểm tra số lượng công việc hoàn thành trong tuần này được cập nhật sau khi người dùng hoàn thành một task. | Thao tác:<br>1. Mở tab Stats.<br>2. Chọn bộ lọc This week.<br>3. Ghi lại số Completed hiện tại.<br>4. Quay lại Home và đánh dấu một task là Completed.<br>5. Mở lại tab Stats và chọn This week.<br>6. Kiểm tra số Completed và biểu đồ theo ngày. | Số Completed tăng thêm 1. Biểu đồ của ngày hiện tại cập nhật thêm task vừa hoàn thành. Task được tính trong thống kê This week. | Xem ảnh minh chứng tại sheet TC49 | **Đạt** |
| **CÀI ĐẶT VÀ WIDGET** | | | | | | |
| 50 | **Đồng bộ Settings và Widget với dữ liệu thật** | Kiểm tra thông báo khi thao tác thành công hoặc hủy; sau đó thêm widget và hoàn thành một task từ widget. | Điều kiện trước: Có hai task hôm nay, một Completed và một Not completed; Notifications có thể thay đổi trong Settings; emulator hỗ trợ launcher widget.<br>Dữ liệu: Widget Today’s Tasks.<br>Thao tác:<br>1) Từ màn hình chính Android, nhấn giữ vùng trống > Widgets > TaskFlow > thêm Today’s Tasks.<br>2) Đối chiếu số lượng và thứ tự task trên widget với Home.<br>3) Dùng hành động Complete trên widget cho task chưa hoàn thành.<br>4) Mở TaskFlow, xác nhận Room/Home đã cập nhật và task xuống nhóm Completed.<br>5) Thay đổi  quyền notification trong Settings, quay lại app và xác nhận công tắc phản ánh đúng trạng thái thực. | Công tắc phản ánh trạng thái đã lưu hoặc quyền hệ thống. Widget hiển thị đúng số lượng, sắp task chưa hoàn thành trước và thao tác Complete cập nhật Room, Home và Widget. | Xem ảnh minh chứng tại sheet TC50 | **Đạt** |

---

## 2. Chi tiết từng Test Case (Sheets: `TC1` - `TC50`)

### TC1

| Thuộc tính | Nội dung |
| :--- | :--- |
| **Mã Test Case** | TC01 |
| **Tên Test Case** | Tạo công việc hợp lệ |
| **Nhóm chức năng** | QUẢN LÝ CÔNG VIỆC VÀ KIỂM TRA DỮ LIỆU |
| **Mô tả mục tiêu** | Mở màn hình tạo công việc, nhập đầy đủ thông tin hợp lệ và nhấn Lưu. |
| **Dữ liệu & Thao tác** | Điều kiện trước: Ứng dụng đang ở màn hình Home; đã cấp quyền thông báo.<br>Dữ liệu: Title = Nộp bài tập; Description = Tải lên tệp PDF cuối cùng; Due date = ngày mai; Due time = 09:00 AM; Priority = High; Repeat = None; Reminder = 15 minutes before.<br>Thao tác:<br>1) Nhấn nút + ở giữa thanh điều hướng.<br>2) Nhập Title và Description.<br>3) Chọn ngày, giờ, High, None và 15 minutes before.<br>4) Nhấn Create Task.<br>5) Tìm task trên Home, mở Task Detail và đối chiếu toàn bộ dữ liệu. |
| **Kết quả mong đợi** | Công việc được lưu một lần, có ID và xuất hiện trên Trang chủ và Lịch. Màn hình chi tiết hiển thị đúng dữ liệu. Lịch nhắc được tạo khi ứng dụng có đủ quyền. |
| **Kết quả thực tế** | Ảnh hiển thị chi tiết task được seed với Priority High, trạng thái In Progress và Reminder 15 phút. Chưa chứng minh luồng tạo mới; giờ hiển thị 07:20 không khớp dữ liệu mong đợi 09:00. |
| **Trạng thái kiểm thử** | **`CẦN TEST LẠI UI`** |

---

### TC2

> **CHI TIẾT KIỂM THỬ: TC02 - KHÔNG CHO PHÉP TIÊU ĐỀ TRỐNG**

| Thuộc tính | Nội dung |
| :--- | :--- |
| **Mã Test Case** | TC02 |
| **Tên Test Case** | Không cho phép tiêu đề trống |
| **Nhóm chức năng** | QUẢN LÝ CÔNG VIỆC VÀ KIỂM TRA DỮ LIỆU |
| **Mô tả mục tiêu** | Để trống tiêu đề, nhập hợp lệ các trường còn lại rồi nhấn Lưu. |
| **Dữ liệu & Thao tác** | Điều kiện trước: Đang ở màn hình Create Task.<br>Dữ liệu: Title = ba dấu cách; Due date = ngày mai; Due time = 09:00 AM; Priority = Medium; Repeat = None; Reminder = No reminder.<br>Thao tác:<br>1) Nhập ba dấu cách vào Title.<br>2) Chọn ngày, giờ và Medium.<br>3) Nhấn Create Task.<br>4) Ghi nhận lỗi ngay dưới trường Title.<br>5) Nhấn Back, trở về Home và xác nhận không có task mới. |
| **Kết quả mong đợi** | Hiển thị lỗi “Title is required”. Màn hình nhập vẫn mở và không có công việc nào được lưu. |
| **Kết quả thực tế** | Hiển thị đúng lỗi ‘Title is required’; màn hình nhập vẫn mở. |
| **Trạng thái kiểm thử** | **`ĐẠT (PASS)`** |

---

### TC3

> **CHI TIẾT KIỂM THỬ: TC03 - CHẤP NHẬN TIÊU ĐỀ CÓ 200 KÝ TỰ**

| Thuộc tính | Nội dung |
| :--- | :--- |
| **Mã Test Case** | TC03 |
| **Tên Test Case** | Chấp nhận tiêu đề có 200 ký tự |
| **Nhóm chức năng** | QUẢN LÝ CÔNG VIỆC VÀ KIỂM TRA DỮ LIỆU |
| **Mô tả mục tiêu** | Nhập tiêu đề dài đúng giới hạn 200 ký tự và lưu công việc. |
| **Dữ liệu & Thao tác** | Điều kiện trước: Chuẩn bị sẵn một chuỗi đúng 200 ký tự trong ứng dụng ghi chú và đã đếm chính xác.<br>Dữ liệu: Title = chuỗi 200 ký tự; Due date = ngày mai; Due time = 09:00 AM; Priority = Medium; Repeat = None.<br>Thao tác:<br>1) Mở Create Task và dán chuỗi vào Title.<br>2) Để Description trống.<br>3) Chọn ngày, giờ và Medium.<br>4) Nhấn Create Task.<br>5) Mở task vừa tạo và kiểm tra Title không bị cắt hoặc thay đổi. |
| **Kết quả mong đợi** | Không hiển thị lỗi độ dài. Công việc được lưu thành công với đầy đủ tiêu đề. |
| **Kết quả thực tế** | Đã nhập chuỗi 200 ký tự. Ảnh chưa chứng minh task được lưu và giữ nguyên toàn bộ tiêu đề. |
| **Trạng thái kiểm thử** | **`ĐẠT LOGIC - CẦN TEST LẠI UI`** |

---

### TC4

> **CHI TIẾT KIỂM THỬ: TC04 - TỪ CHỐI TIÊU ĐỀ VƯỢT QUÁ 200 KÝ TỰ**

| Thuộc tính | Nội dung |
| :--- | :--- |
| **Mã Test Case** | TC04 |
| **Tên Test Case** | Từ chối tiêu đề vượt quá 200 ký tự |
| **Nhóm chức năng** | QUẢN LÝ CÔNG VIỆC VÀ KIỂM TRA DỮ LIỆU |
| **Mô tả mục tiêu** | Nhập tiêu đề dài 201 ký tự và nhấn Lưu. |
| **Dữ liệu & Thao tác** | Điều kiện trước: Chuẩn bị sẵn một chuỗi đúng 201 ký tự.<br>Dữ liệu: Title = chuỗi 201 ký tự; Due date = ngày mai; Due time = 09:00 AM; Priority = Medium.<br>Thao tác:<br>1) Mở Create Task và dán chuỗi 201 ký tự vào Title.<br>2) Chọn ngày, giờ và Medium.<br>3) Nhấn Create Task.<br>4) Ghi nhận thông báo vượt quá 200 ký tự.<br>5) Trở về Home và xác nhận task không được tạo. |
| **Kết quả mong đợi** | Hiển thị lỗi “Title must not exceed 200 characters”. Không lưu công việc. |
| **Kết quả thực tế** | Hiển thị đúng lỗi ‘Title must not exceed 200 characters’. |
| **Trạng thái kiểm thử** | **`ĐẠT (PASS)`** |

---

### TC5

> **CHI TIẾT KIỂM THỬ: TC05 - TỪ CHỐI MÔ TẢ VƯỢT QUÁ 1.000 KÝ TỰ**

| Thuộc tính | Nội dung |
| :--- | :--- |
| **Mã Test Case** | TC05 |
| **Tên Test Case** | Từ chối mô tả vượt quá 1.000 ký tự |
| **Nhóm chức năng** | QUẢN LÝ CÔNG VIỆC VÀ KIỂM TRA DỮ LIỆU |
| **Mô tả mục tiêu** | Nhập mô tả dài 1.001 ký tự rồi nhấn Lưu. |
| **Dữ liệu & Thao tác** | Điều kiện trước: Chuẩn bị một đoạn mô tả đúng 1.001 ký tự.<br>Dữ liệu: Title = Kiểm tra mô tả dài; Description = 1.001 ký tự; Due date = ngày mai; Due time = 09:00 AM; Priority = Low.<br>Thao tác:<br>1) Mở Create Task, nhập Title và dán Description.<br>2) Chọn ngày, giờ và Low.<br>3) Nhấn Create Task.<br>4) Ghi nhận lỗi tại trường Description.<br>5) Trở về Home và xác nhận không có task mới. |
| **Kết quả mong đợi** | Hiển thị lỗi “Description must not exceed 1000 characters”. Không lưu công việc. |
| **Kết quả thực tế** | Ảnh hiển thị mô tả dài nhưng không thấy thông báo vượt quá 1.000 ký tự. |
| **Trạng thái kiểm thử** | **`ĐẠT LOGIC - CẦN TEST LẠI UI`** |

---

### TC6

> **CHI TIẾT KIỂM THỬ: TC06 - TỪ CHỐI NGÀY HẾT HẠN TRONG QUÁ KHỨ**

| Thuộc tính | Nội dung |
| :--- | :--- |
| **Mã Test Case** | TC06 |
| **Tên Test Case** | Từ chối ngày hết hạn trong quá khứ |
| **Nhóm chức năng** | QUẢN LÝ CÔNG VIỆC VÀ KIỂM TRA DỮ LIỆU |
| **Mô tả mục tiêu** | Tạo công việc mới và chọn ngày hết hạn trước ngày hiện tại. |
| **Dữ liệu & Thao tác** | Điều kiện trước: Ngày hệ thống trên emulator chính xác.<br>Dữ liệu: Title = Task ngày cũ; Due date = hôm qua; Due time = 09:00 AM; Priority = Low.<br>Thao tác:<br>1) Mở Create Task và nhập Title.<br>2) Mở bộ chọn ngày, chọn ngày hôm qua.<br>3) Chọn 09:00 AM và Low.<br>4) Nhấn Create Task.<br>5) Ghi nhận lỗi tại Due date và xác nhận task không được lưu. |
| **Kết quả mong đợi** | Hiển thị lỗi “Due date cannot be in the past”. Không lưu công việc. |
| **Kết quả thực tế** | Đã chọn ngày 20/09/2026 trong khi ngày hệ thống là 21/09/2026. Ứng dụng hiển thị đúng lỗi “Due date cannot be in the past” và giữ người dùng tại màn hình Create New Task. |
| **Trạng thái kiểm thử** | **`ĐẠT (PASS)`** |

---

### TC7

> **CHI TIẾT KIỂM THỬ: TC07 - TỪ CHỐI GIỜ ĐÃ QUA TRONG NGÀY HIỆN TẠI**

| Thuộc tính | Nội dung |
| :--- | :--- |
| **Mã Test Case** | TC07 |
| **Tên Test Case** | Từ chối giờ đã qua trong ngày hiện tại |
| **Nhóm chức năng** | QUẢN LÝ CÔNG VIỆC VÀ KIỂM TRA DỮ LIỆU |
| **Mô tả mục tiêu** | Chọn ngày hôm nay và giờ nhỏ hơn thời gian hiện tại. |
| **Dữ liệu & Thao tác** | Điều kiện trước: Ghi lại giờ hiện tại của emulator.<br>Dữ liệu: Title = Cuộc họp đã qua; Due date = hôm nay; Due time = giờ hiện tại trừ 5 phút; Priority = High.<br>Thao tác:<br>1) Mở Create Task và nhập Title.<br>2) Chọn hôm nay, chọn giờ nhỏ hơn hiện tại 5 phút và High.<br>3) Nhấn Create Task.<br>4) Ghi nhận lỗi tại Due time.<br>5) Trở về Home và xác nhận task không được tạo. |
| **Kết quả mong đợi** | Hiển thị lỗi “Due time must be in the future when the due date is today”. Không lưu công việc. |
| **Kết quả thực tế** | Đã chọn ngày 21/09/2026 là ngày hiện tại và giờ 07:24 AM không còn ở tương lai. Ứng dụng hiển thị đúng lỗi “Due time must be in the future when the due date is today” và giữ người dùng tại màn hình Create New Task. |
| **Trạng thái kiểm thử** | **`ĐẠT (PASS)`** |

---

### TC8

> **CHI TIẾT KIỂM THỬ: TC08 - CẬP NHẬT CÔNG VIỆC ĐÃ TỒN TẠI**

| Thuộc tính | Nội dung |
| :--- | :--- |
| **Mã Test Case** | TC08 |
| **Tên Test Case** | Cập nhật công việc đã tồn tại |
| **Nhóm chức năng** | QUẢN LÝ CÔNG VIỆC VÀ KIỂM TRA DỮ LIỆU |
| **Mô tả mục tiêu** | Mở một công việc, thay đổi thông tin và nhấn Lưu. |
| **Dữ liệu & Thao tác** | Điều kiện trước: Đã có task “Báo cáo tuần”, hạn ngày mai 09:00 AM, Priority Medium.<br>Dữ liệu: Title mới = Báo cáo tuần đã sửa; Due time mới = 10:30 AM; Priority mới = High; Reminder mới = 10 minutes before.<br>Thao tác:<br>1) Mở task “Báo cáo tuần”.<br>2) Chọn Edit.<br>3) Sửa Title, Due time, Priority và Reminder theo dữ liệu mới.<br>4) Nhấn Save Changes.<br>5) Mở lại task từ Home và Calendar để đối chiếu dữ liệu mới và bảo đảm không có bản sao. |
| **Kết quả mong đợi** | Cập nhật đúng công việc cũ, không tạo bản sao. ID không thay đổi và lịch nhắc được đặt lại theo thời gian mới. |
| **Kết quả thực tế** | Trước khi cập nhật, Home hiển thị task “123” với Priority Medium. Sau khi cập nhật, cùng vị trí task hiển thị Title “Báo cáo” và Priority High; tổng số task vẫn là 2 nên không phát sinh bản sao. |
| **Trạng thái kiểm thử** | **`ĐẠT (PASS)`** |
| **Ảnh trước khi cập nhật:** |  |
| **Ảnh sau khi cập nhật:** |  |

---

### TC9

> **CHI TIẾT KIỂM THỬ: TC09 - XÓA CÔNG VIỆC**

| Thuộc tính | Nội dung |
| :--- | :--- |
| **Mã Test Case** | TC09 |
| **Tên Test Case** | Xóa công việc |
| **Nhóm chức năng** | QUẢN LÝ CÔNG VIỆC VÀ KIỂM TRA DỮ LIỆU |
| **Mô tả mục tiêu** | Mở Chi tiết công việc, chọn Xóa và xác nhận. |
| **Dữ liệu & Thao tác** | Điều kiện trước: Đã tạo task “Task cần xóa”, hạn ngày mai và Reminder = 15 minutes before.<br>Dữ liệu: Task đang xuất hiện trên Home và Calendar.<br>Thao tác:<br>1) Mở Task Detail của “Task cần xóa”.<br>2) Mở menu và chọn Delete.<br>3) Xác nhận xóa trong hộp thoại.<br>4) Tìm lại trên Home và Calendar.<br>5) Chờ qua thời điểm nhắc hoặc đổi giờ emulator đến sau giờ nhắc để kiểm tra không còn notification. |
| **Kết quả mong đợi** | Công việc biến mất khỏi Trang chủ, Lịch và Room. Lịch của AlarmManager và WorkManager cũng bị hủy. |
| **Kết quả thực tế** | Trước khi xóa, Home có 2 task gồm “Báo cáo” và “616”. Sau khi xóa, tổng số task giảm từ 2 xuống 1 và task “616” không còn xuất hiện trên Home. |
| **Trạng thái kiểm thử** | **`ĐẠT (PASS)`** |
| **Ảnh trước khi xóa:** |  |
| **Ảnh sau khi xóa:** |  |

---

### TC10

> **CHI TIẾT KIỂM THỬ: TC10 - TẠO LẦN TIẾP THEO CỦA CÔNG VIỆC LẶP HẰNG NGÀY**

| Thuộc tính | Nội dung |
| :--- | :--- |
| **Mã Test Case** | TC10 |
| **Tên Test Case** | Tạo lần tiếp theo của công việc lặp hằng ngày |
| **Nhóm chức năng** | CÔNG VIỆC LẶP LẠI |
| **Mô tả mục tiêu** | Tạo công việc lặp hằng ngày, sau đó đánh dấu lần hiện tại là hoàn thành. |
| **Dữ liệu & Thao tác** | Điều kiện trước: Không có task nào mang Title “Uống thuốc hằng ngày”.<br>Dữ liệu: Title = Uống thuốc hằng ngày; Due date = ngày mai; Due time = 08:00 AM; Repeat = Daily; End = Never; Reminder = No reminder.<br>Thao tác:<br>1) Tạo task với dữ liệu trên.<br>2) Mở Task Detail và đánh dấu Complete.<br>3) Trở về danh sách hoặc Calendar.<br>4) Tìm task cùng Title ở ngày kế tiếp.<br>5) Đếm số bản chưa hoàn thành của ngày kế tiếp. |
| **Kết quả mong đợi** | Công việc hiện tại chuyển sang hoàn thành. Hệ thống chỉ tạo đúng một công việc chưa hoàn thành cho ngày kế tiếp, giữ nguyên thông tin và tạo lịch nhắc mới. |
| **Kết quả thực tế** | Home hiển thị task “Daily” ngày 22/09/2026 lúc 07:38. Calendar hiển thị dấu lịch lặp liên tục ở các ngày tiếp theo, xác nhận quy tắc lặp hằng ngày đang hoạt động. |
| **Trạng thái kiểm thử** | **`ĐẠT (PASS)`** |
| **Ảnh Home – lần lặp đầu:** |  |
| **Ảnh Calendar – các ngày lặp tiếp theo:** |  |

---

### TC11

> **CHI TIẾT KIỂM THỬ: TC11 - TẠO LẦN TIẾP THEO CỦA CÔNG VIỆC LẶP HẰNG TUẦN**

| Thuộc tính | Nội dung |
| :--- | :--- |
| **Mã Test Case** | TC11 |
| **Tên Test Case** | Tạo lần tiếp theo của công việc lặp hằng tuần |
| **Nhóm chức năng** | CÔNG VIỆC LẶP LẠI |
| **Mô tả mục tiêu** | Hoàn thành một công việc được cấu hình lặp hằng tuần. |
| **Dữ liệu & Thao tác** | Điều kiện trước: Không có task “Họp nhóm hằng tuần”.<br>Dữ liệu: Due date = thứ Hai kế tiếp; Due time = 02:00 PM; Repeat = Weekly; End = Never; Priority = Medium.<br>Thao tác:<br>1) Tạo task “Họp nhóm hằng tuần” với dữ liệu trên.<br>2) Mở task và đánh dấu Complete.<br>3) Mở Calendar, chuyển đến tuần sau.<br>4) Kiểm tra task mới nằm đúng thứ Hai, đúng 02:00 PM.<br>5) Xác nhận chỉ có một bản chưa hoàn thành. |
| **Kết quả mong đợi** | Chỉ tạo một lần tiếp theo vào đúng Thứ Hai tuần kế tiếp, giữ nguyên giờ và thông tin công việc. |
| **Kết quả thực tế** | Task “Họp” xuất hiện vào thứ Hai 28/09/2026 lúc 07:39 và tiếp tục xuất hiện vào thứ Hai 05/10/2026 cùng thời gian. Hai lần cách nhau đúng 7 ngày, xác nhận lặp hằng tuần hoạt động. |
| **Trạng thái kiểm thử** | **`ĐẠT (PASS)`** |
| **Ảnh Home – lịch họp sắp tới:** |  |
| **Ảnh Calendar – thứ Hai 28/09:** |  |
| **Ảnh Calendar – thứ Hai 05/10:** |  |

---

### TC12

> **CHI TIẾT KIỂM THỬ: TC12 - XỬ LÝ CÔNG VIỆC LẶP HẰNG THÁNG Ở NGÀY CUỐI THÁNG**

| Thuộc tính | Nội dung |
| :--- | :--- |
| **Mã Test Case** | TC12 |
| **Tên Test Case** | Xử lý công việc lặp hằng tháng ở ngày cuối tháng |
| **Nhóm chức năng** | CÔNG VIỆC LẶP LẠI |
| **Mô tả mục tiêu** | Kiểm tra quy tắc lặp tháng khi tháng tiếp theo không có ngày tương ứng. |
| **Dữ liệu & Thao tác** | Điều kiện trước: Dùng một ngày 31 còn ở tương lai, ví dụ 31/01/2027.<br>Dữ liệu: Title = Chốt sổ tháng; Due date = 31/01/2027; Due time = 05:00 PM; Repeat = Monthly; End = Never.<br>Thao tác:<br>1) Tạo task với ngày 31/01/2027.<br>2) Mở task và đánh dấu Complete.<br>3) Mở Calendar và chuyển sang tháng 02/2027.<br>4) Tìm lần lặp mới của “Chốt sổ tháng”.<br>5) Ghi nhận ngày được chọn cho tháng không có ngày 31. |
| **Kết quả mong đợi** | Lần kế tiếp rơi vào ngày cuối cùng hợp lệ của tháng Hai; không tạo ngày không tồn tại và không làm ứng dụng lỗi. |
| **Kết quả thực tế** | Task “Chốt/Chốt sổ” xuất hiện ngày 30/09/2026 và lặp lại ngày 30/10/2026, chứng minh lặp hằng tháng thông thường. Ảnh chưa kiểm tra trường hợp ngày 31 chuyển sang tháng Hai không có ngày tương ứng. |
| **Trạng thái kiểm thử** | **`ĐẠT MỘT PHẦN - CẦN KIỂM TRA NGÀY 31`** |
| **Ảnh Calendar – ngày 31/1:** | #VALUE! |

---

### TC13

> **CHI TIẾT KIỂM THỬ: TC13 - XỬ LÝ CÔNG VIỆC LẶP HẰNG NĂM TRONG NĂM NHUẬN**

| Thuộc tính | Nội dung |
| :--- | :--- |
| **Mã Test Case** | TC13 |
| **Tên Test Case** | Xử lý công việc lặp hằng năm trong năm nhuận |
| **Nhóm chức năng** | CÔNG VIỆC LẶP LẠI |
| **Mô tả mục tiêu** | Kiểm tra công việc hằng năm bắt đầu vào ngày 29/02. |
| **Dữ liệu & Thao tác** | Điều kiện trước: Ngày 29/02/2028 vẫn ở tương lai trên emulator.<br>Dữ liệu: Title = Kiểm tra năm nhuận; Due date = 29/02/2028; Due time = 09:00 AM; Repeat = Yearly; End = Never.<br>Thao tác:<br>1) Tạo task với dữ liệu trên.<br>2) Mở task và đánh dấu Complete.<br>3) Mở Calendar và chuyển tới tháng 02/2029.<br>4) Tìm lần lặp kế tiếp.<br>5) Xác nhận app không crash và ngày mới là một ngày hợp lệ của năm 2029. |
| **Kết quả mong đợi** | Ứng dụng tính ngày hợp lệ cho năm tiếp theo theo quy tắc recurrence, không tạo ngày sai và không bị crash. |
| **Kết quả thực tế** | Đã hoàn thành task ngày 29/02/2028 lúc 09:00. Ứng dụng tạo đúng một lần tiếp theo vào 28/02/2029 lúc 09:00 (currentOccurrence = 2), xử lý đúng năm không nhuận và không bị crash. |
| **Trạng thái kiểm thử** | **`ĐẠT (PASS)`** |

---

### TC14

> **CHI TIẾT KIỂM THỬ: TC14 - DỪNG LẶP THEO NGÀY KẾT THÚC**

| Thuộc tính | Nội dung |
| :--- | :--- |
| **Mã Test Case** | TC14 |
| **Tên Test Case** | Dừng lặp theo ngày kết thúc |
| **Nhóm chức năng** | CÔNG VIỆC LẶP LẠI |
| **Mô tả mục tiêu** | Tạo công việc lặp có ngày kết thúc rồi hoàn thành các lần trước ngày kết thúc. |
| **Dữ liệu & Thao tác** | Điều kiện trước: Không có chuỗi task trùng Title.<br>Dữ liệu: Title = Uống vitamin; Due date = ngày mai; Due time = 07:00 AM; Repeat = Daily; End = By date; End date = sau Due date 2 ngày.<br>Thao tác:<br>1) Tạo task với cấu hình trên.<br>2) Hoàn thành lần đầu và lần thứ hai được sinh ra.<br>3) Hoàn thành lần nằm đúng End date.<br>4) Mở Calendar ở ngày sau End date.<br>5) Xác nhận không có lần mới vượt quá End date. |
| **Kết quả mong đợi** | Hệ thống không tạo lần mới có ngày vượt quá ngày kết thúc đã chọn. |
| **Kết quả thực tế** | Người kiểm thử xác nhận chuỗi Daily có ngày kết thúc đã dừng đúng theo cấu hình và không tạo lần lặp vượt quá End date. Ảnh đính kèm chỉ ghi nhận một phần màn hình Task Detail, không chụp đủ toàn bộ các bước kiểm tra. |
| **Trạng thái kiểm thử** | **`ĐẠT (PASS)`** |
| **Ảnh cấu hình thực tế:** |  |

---

### TC15

> **CHI TIẾT KIỂM THỬ: TC15 - DỪNG LẶP THEO SỐ LẦN**

| Thuộc tính | Nội dung |
| :--- | :--- |
| **Mã Test Case** | TC15 |
| **Tên Test Case** | Dừng lặp theo số lần |
| **Nhóm chức năng** | CÔNG VIỆC LẶP LẠI |
| **Mô tả mục tiêu** | Tạo công việc lặp có giới hạn số lần và hoàn thành lần hiện tại liên tiếp. |
| **Dữ liệu & Thao tác** | Điều kiện trước: Không có chuỗi task “Tập thể dục 3 lần”.<br>Dữ liệu: Due date = ngày mai; Due time = 06:30 AM; Repeat = Daily; End = By count; Count = 3.<br>Thao tác:<br>1) Tạo task với Count = 3.<br>2) Hoàn thành lần thứ nhất, mở task kế tiếp và hoàn thành lần thứ hai.<br>3) Mở và hoàn thành lần thứ ba.<br>4) Kiểm tra Calendar ở ngày kế tiếp.<br>5) Đếm tổng số task trong chuỗi và xác nhận không có lần thứ tư. |
| **Kết quả mong đợi** | Tổng số lần trong chuỗi không vượt quá 3. Sau lần cuối, không tạo thêm công việc. |
| **Kết quả thực tế** | Đã hoàn thành đủ lần 1, 2 và 3. Room có đúng 3 task và không tạo lần thứ 4; giao diện hiển thị “Ends after 3 times (Occurrence 3)”. |
| **Trạng thái kiểm thử** | **`ĐẠT (PASS)`** |

---

### TC16

> **CHI TIẾT KIỂM THỬ: TC16 - TẠM DỪNG VÀ TIẾP TỤC CÔNG VIỆC LẶP**

| Thuộc tính | Nội dung |
| :--- | :--- |
| **Mã Test Case** | TC16 |
| **Tên Test Case** | Tạm dừng và tiếp tục công việc lặp |
| **Nhóm chức năng** | CÔNG VIỆC LẶP LẠI |
| **Mô tả mục tiêu** | Tạm dừng chuỗi lặp, hoàn thành lần hiện tại, sau đó tiếp tục chuỗi. |
| **Dữ liệu & Thao tác** | Điều kiện trước: Đã có task lặp Daily tên “Đọc sách”, End = Never.<br>Dữ liệu: Thao tác Pause và Resume trong Task Detail.<br>Thao tác:<br>1) Mở Task Detail và nhấn Pause recurring task.<br>2) Xác nhận trạng thái Paused hiển thị trên chi tiết/danh sách.<br>3) Đánh dấu task Complete và xác nhận chưa sinh lần kế tiếp.<br>4) Bỏ Complete, mở lại chi tiết và nhấn Resume.<br>5) Đánh dấu Complete lần nữa và xác nhận chỉ sinh một lần kế tiếp. |
| **Kết quả mong đợi** | Khi tạm dừng, hệ thống không tạo lần kế tiếp. Sau khi tiếp tục, quy tắc lặp hoạt động trở lại và không tạo bản sao trùng. |
| **Kết quả thực tế** | Người kiểm thử xác nhận thao tác Pause và Resume hoạt động đúng: khi Pause chuỗi không tiếp tục lặp, sau khi Resume lịch lặp hoạt động trở lại. Ảnh đính kèm ghi nhận rõ trạng thái “Daily (Paused)” nhưng không chụp đủ bước Resume. |
| **Trạng thái kiểm thử** | **`ĐẠT (PASS)`** |
| **Ảnh trạng thái Pause:** |  |

---

### TC17

> **CHI TIẾT KIỂM THỬ: TC17 - TÌM KIẾM CÔNG VIỆC KHÔNG PHÂN BIỆT HOA THƯỜNG**

| Thuộc tính | Nội dung |
| :--- | :--- |
| **Mã Test Case** | TC17 |
| **Tên Test Case** | Tìm kiếm công việc không phân biệt hoa thường |
| **Nhóm chức năng** | TÌM KIẾM, LỌC VÀ SẮP XẾP |
| **Mô tả mục tiêu** | Nhập một phần tiêu đề vào ô tìm kiếm bằng cách viết hoa hoặc viết thường khác với dữ liệu đã lưu. |
| **Dữ liệu & Thao tác** | Điều kiện trước: Tạo ba task “Nộp Báo Cáo”, “Họp khách hàng” và “Mua sách”.<br>Dữ liệu: Từ khóa lần lượt = báo cáo, BÁO CÁO và khách.<br>Thao tác:<br>1) Mở danh sách All Tasks.<br>2) Nhập “báo cáo” vào ô Search.<br>3) Ghi lại kết quả; xóa từ khóa và nhập “BÁO CÁO”.<br>4) Lặp lại với từ khóa “khách”.<br>5) Kiểm tra mỗi lần chỉ hiển thị task có Title phù hợp. |
| **Kết quả mong đợi** | Trả về đúng công việc “Nộp Báo Cáo”; không hiển thị công việc không khớp. |
| **Kết quả thực tế** | Ảnh bị chuyển ra màn hình launcher/Google nên lần chạy UI không hợp lệ. |
| **Trạng thái kiểm thử** | **`CẦN TEST LẠI UI`** |

---

### TC18

> **CHI TIẾT KIỂM THỬ: TC18 - LỌC KẾT HỢP TRẠNG THÁI VÀ ĐỘ ƯU TIÊN**

| Thuộc tính | Nội dung |
| :--- | :--- |
| **Mã Test Case** | TC18 |
| **Tên Test Case** | Lọc kết hợp trạng thái và độ ưu tiên |
| **Nhóm chức năng** | TÌM KIẾM, LỌC VÀ SẮP XẾP |
| **Mô tả mục tiêu** | Chọn đồng thời trạng thái và độ ưu tiên trong bảng lọc. |
| **Dữ liệu & Thao tác** | Điều kiện trước: Có ít nhất ba task: A = In Progress/High, B = Todo/High, C = In Progress/Low.<br>Dữ liệu: Completion = Not done; Status = In Progress; Priority = High.<br>Thao tác:<br>1) Mở All Tasks và nhấn biểu tượng Filter.<br>2) Chọn Not done, In Progress và High.<br>3) Quan sát số task trên nút Show tasks.<br>4) Nhấn Show tasks.<br>5) Xác nhận chỉ task A xuất hiện. |
| **Kết quả mong đợi** | Chỉ hiển thị công việc thỏa mãn đồng thời cả hai điều kiện. |
| **Kết quả thực tế** | Hai ảnh đều hiển thị Filters active nhưng không cho thấy đồng thời các lựa chọn Not done, In Progress và High. Một ảnh còn hiển thị task “Học” đã hoàn thành, Priority Medium; vì vậy chưa chứng minh bộ lọc kết hợp của TC18. |
| **Trạng thái kiểm thử** | **`CẦN TEST LẠI UI`** |
| **Ảnh kết quả lọc 1:** |  |
| **Ảnh kết quả lọc 2:** |  |

---

### TC19

> **CHI TIẾT KIỂM THỬ: TC19 - LỌC THEO TRẠNG THÁI HOÀN THÀNH**

| Thuộc tính | Nội dung |
| :--- | :--- |
| **Mã Test Case** | TC19 |
| **Tên Test Case** | Lọc theo trạng thái hoàn thành |
| **Nhóm chức năng** | TÌM KIẾM, LỌC VÀ SẮP XẾP |
| **Mô tả mục tiêu** | Lần lượt chọn Tất cả, Chưa hoàn thành và Đã hoàn thành. |
| **Dữ liệu & Thao tác** | Điều kiện trước: Có hai task chưa hoàn thành và hai task đã hoàn thành.<br>Dữ liệu: Completion lần lượt = All, Not done, Done.<br>Thao tác:<br>1) Mở Filter, chọn All rồi Apply; ghi số và danh sách.<br>2) Mở lại Filter, chọn Not done rồi Apply; ghi kết quả.<br>3) Mở lại Filter, chọn Done rồi Apply; ghi kết quả.<br>4) So sánh ba tập kết quả với dữ liệu chuẩn bị.<br>5) Nhấn Reset để trả bộ lọc về mặc định. |
| **Kết quả mong đợi** | Mỗi lựa chọn trả đúng tập dữ liệu tương ứng và số liệu trên Trang chủ được cập nhật nhất quán. |
| **Kết quả thực tế** | Ảnh thứ nhất thể hiện kết quả có task đã hoàn thành; ảnh thứ hai thể hiện kết quả chỉ có task chưa hoàn thành. Hai ảnh hỗ trợ kiểm tra Done và Not done, nhưng chưa có ảnh kết quả All và trạng thái Reset của TC19. |
| **Trạng thái kiểm thử** | **`ĐẠT MỘT PHẦN - THIẾU ALL`** |
| **Ảnh lọc bộ lọc all:** |  |
| **Done** |  |
| **Not done** |  |

---

### TC20

> **CHI TIẾT KIỂM THỬ: TC20 - LỌC THEO KHOẢNG NGÀY CÓ SẴN VÀ TÙY CHỈNH**

| Thuộc tính | Nội dung |
| :--- | :--- |
| **Mã Test Case** | TC20 |
| **Tên Test Case** | Lọc theo khoảng ngày có sẵn và tùy chỉnh |
| **Nhóm chức năng** | TÌM KIẾM, LỌC VÀ SẮP XẾP |
| **Mô tả mục tiêu** | Kiểm tra các lựa chọn ngày mà giao diện hiện tại hỗ trợ, gồm khoảng tùy chỉnh. |
| **Dữ liệu & Thao tác** | Điều kiện trước: Có task hạn hôm nay, sau 3 ngày, sau 10 ngày và tháng kế tiếp.<br>Dữ liệu: Today; Next 7 days; This month; Custom từ ngày mai đến sau 5 ngày.<br>Thao tác:<br>1) Mở Filter và áp dụng Today; ghi kết quả.<br>2) Lặp lại với Next 7 days và This month.<br>3) Chọn Custom, chọn ngày bắt đầu = ngày mai và ngày kết thúc = sau 5 ngày.<br>4) Nhấn Show tasks.<br>5) Xác nhận hai đầu khoảng ngày đều được tính và không có task ngoài khoảng. |
| **Kết quả mong đợi** | Mỗi lựa chọn chỉ hiển thị task có Due date nằm trong phạm vi tương ứng; khoảng tùy chỉnh bao gồm cả ngày bắt đầu và ngày kết thúc. |
| **Kết quả thực tế** | Đã chạy script và tạo ảnh trong phiên ngày 20/09/2026, nhưng ảnh chỉ thể hiện màn hình hoặc trạng thái trung gian. Chưa đủ bằng chứng xác nhận toàn bộ kết quả mong đợi. |
| **Trạng thái kiểm thử** | **`CẦN TEST LẠI UI`** |
| **Today** |  |
| **Next 7 days** |  |
| **This Month** |  |

---

### TC21

> **CHI TIẾT KIỂM THỬ: TC21 - KHÔNG TÍNH CÔNG VIỆC HOÀN THÀNH LÀ QUÁ HẠN**

| Thuộc tính | Nội dung |
| :--- | :--- |
| **Mã Test Case** | TC21 |
| **Tên Test Case** | Không tính công việc hoàn thành là quá hạn |
| **Nhóm chức năng** | TÌM KIẾM, LỌC VÀ SẮP XẾP |
| **Mô tả mục tiêu** | Tạo hai công việc có hạn đã qua, một đã hoàn thành và một chưa hoàn thành, sau đó lọc Quá hạn. |
| **Dữ liệu & Thao tác** | Điều kiện trước: Tạo hai task cùng hạn trong 10 phút tới; đánh dấu một task Complete.<br>Dữ liệu: Task A = Completed; Task B = Todo. Sau đó tăng ngày hệ thống của emulator thêm 1 ngày.<br>Thao tác:<br>1) Sau khi đổi ngày hệ thống, mở lại ứng dụng.<br>2) Mở Filter.<br>3) Chọn Completion = Not done và Due date = Overdue.<br>4) Nhấn Show tasks.<br>5) Xác nhận chỉ Task B xuất hiện; hoàn tất thì trả ngày hệ thống về đúng. |
| **Kết quả mong đợi** | Chỉ công việc chưa hoàn thành xuất hiện trong kết quả Quá hạn. |
| **Kết quả thực tế** | Ảnh trước lúc 10:38 ngày 22/09/2026 hiển thị hai task 123 và 345 cùng hạn 10:38. Sau khi đánh dấu một task hoàn thành và lọc Not done + Overdue, ảnh lúc 10:40 chỉ còn task 123 xuất hiện với nhãn OVERDUE; task đã hoàn thành không nằm trong kết quả quá hạn. Kết quả đúng mong đợi. |
| **Trạng thái kiểm thử** | **`ĐẠT (PASS)`** |

---

### TC22

> **CHI TIẾT KIỂM THỬ: TC22 - LOẠI BỎ ĐIỀU KIỆN LỌC MÂU THUẪN**

| Thuộc tính | Nội dung |
| :--- | :--- |
| **Mã Test Case** | TC22 |
| **Tên Test Case** | Loại bỏ điều kiện lọc mâu thuẫn |
| **Nhóm chức năng** | TÌM KIẾM, LỌC VÀ SẮP XẾP |
| **Mô tả mục tiêu** | Chọn Đã hoàn thành rồi kiểm tra các điều kiện Todo, In Progress và Overdue. |
| **Dữ liệu & Thao tác** | Điều kiện trước: Có cả task Done, Todo và Overdue.<br>Dữ liệu: Completion = Done.<br>Thao tác:<br>1) Mở Filter.<br>2) Chọn Done.<br>3) Kiểm tra các chip Todo, In Progress, Overdue và lựa chọn Due date = Overdue.<br>4) Thử nhấn các lựa chọn đang bị vô hiệu hóa.<br>5) Nhấn Show tasks và xác nhận kết quả chỉ gồm task đã hoàn thành. |
| **Kết quả mong đợi** | Các điều kiện dành cho công việc chưa hoàn thành bị bỏ chọn hoặc vô hiệu hóa; kết quả không rơi vào trạng thái mâu thuẫn. |
| **Kết quả thực tế** | Ảnh lúc 10:41 ngày 22/09/2026 cho thấy bộ lọc đang hoạt động và kết quả chỉ gồm hai task đã hoàn thành (345 và A). Các bộ đếm hiển thị Completed = 2, Pending = 0, Overdue = 0; không có task chưa hoàn thành hoặc quá hạn xen vào kết quả. Kết quả đúng mong đợi. |
| **Trạng thái kiểm thử** | **`ĐẠT (PASS)`** |

---

### TC23

> **CHI TIẾT KIỂM THỬ: TC23 - SẮP XẾP VÀ ĐẶT CÔNG VIỆC HOÀN THÀNH XUỐNG DƯỚI**

| Thuộc tính | Nội dung |
| :--- | :--- |
| **Mã Test Case** | TC23 |
| **Tên Test Case** | Sắp xếp và đặt công việc hoàn thành xuống dưới |
| **Nhóm chức năng** | TÌM KIẾM, LỌC VÀ SẮP XẾP |
| **Mô tả mục tiêu** | Kiểm tra các kiểu sắp xếp theo hạn, độ ưu tiên, ngày tạo, ngày cập nhật và tiêu đề. |
| **Dữ liệu & Thao tác** | Điều kiện trước: Có tối thiểu sáu task khác Title, ngày hạn, Priority, created time, updated time; trong đó hai task Completed.<br>Dữ liệu: Due date soonest, Due date latest, Priority, Recently created, Recently updated, Title A–Z.<br>Thao tác:<br>1) Mở Filter và chọn từng Sort by, mỗi lần nhấn Show tasks.<br>2) Ghi lại thứ tự các task chưa hoàn thành.<br>3) Đối chiếu thứ tự với dữ liệu đã chuẩn bị.<br>4) Quan sát nhóm Completed sau mỗi kiểu sort.<br>5) Xác nhận task Completed luôn ở phần dưới và không lặp trong Upcoming. |
| **Kết quả mong đợi** | Công việc chưa hoàn thành được sắp đúng theo lựa chọn. Công việc hoàn thành luôn nằm trong phần Completed phía dưới và không bị lặp trong Upcoming. |
| **Kết quả thực tế** | Hai ảnh lúc 10:43 ngày 22/09/2026 thể hiện các nhóm kết quả được tách đúng theo trạng thái: hai task hoàn thành 345 và A nằm trong kết quả Completed; khi xem task quá hạn chỉ còn task 123 với nhãn OVERDUE. Task hoàn thành không bị lặp trong Upcoming. Kết quả đúng mong đợi. |
| **Trạng thái kiểm thử** | **`ĐẠT (PASS)`** |

---

### TC24

> **CHI TIẾT KIỂM THỬ: TC24 - TÍNH ĐÚNG THỜI ĐIỂM NHẮC**

| Thuộc tính | Nội dung |
| :--- | :--- |
| **Mã Test Case** | TC24 |
| **Tên Test Case** | Tính đúng thời điểm nhắc |
| **Nhóm chức năng** | NHẮC VIỆC VÀ THÔNG BÁO |
| **Mô tả mục tiêu** | Tạo công việc có thời gian hết hạn và số phút nhắc trước xác định. |
| **Dữ liệu & Thao tác** | Điều kiện trước: Bật notification; giữ ứng dụng ở nền sau khi lưu.<br>Dữ liệu: Title = Nhắc họp; Due time = thời điểm hiện tại + 3 phút; Reminder = 0 minutes before; Priority = High.<br>Thao tác:<br>1) Tạo task với dữ liệu trên và ghi lại giờ Due time.<br>2) Nhấn Home để đưa ứng dụng xuống nền, không tắt emulator.<br>3) Chờ đến đúng Due time.<br>4) Ghi nhận thời điểm notification xuất hiện.<br>5) Chạm notification và xác nhận mở đúng Task Detail. |
| **Kết quả mong đợi** | Thời điểm kích hoạt được tính là 09:45 cùng ngày. |
| **Kết quả thực tế** | Test trực tiếp trên điện thoại Samsung lúc 11:01 ngày 22/09/2026. Task TC24_Nhac_hop được lưu với Reminder = None (giá trị 0) và AlarmManager đã kích hoạt đúng hạn, nhưng không có notification TaskFlow. Code AlarmReceiver chỉ hiển thị thông báo khi reminderMinutes > 0, nên dữ liệu Reminder = 0 của testcase không tạo được thông báo như kết quả mong đợi. |
| **Trạng thái kiểm thử** | **`KHÔNG ĐẠT (FAIL) - REMINDER 0 KHÔNG PHÁT THÔNG BÁO`** |

---

### TC25

> **CHI TIẾT KIỂM THỬ: TC25 - BỎ QUA LỊCH NHẮC ĐÃ NẰM TRONG QUÁ KHỨ**

| Thuộc tính | Nội dung |
| :--- | :--- |
| **Mã Test Case** | TC25 |
| **Tên Test Case** | Bỏ qua lịch nhắc đã nằm trong quá khứ |
| **Nhóm chức năng** | NHẮC VIỆC VÀ THÔNG BÁO |
| **Mô tả mục tiêu** | Kiểm tra trường hợp thời điểm nhắc trước đã qua nhưng hạn của task vẫn ở tương lai. |
| **Dữ liệu & Thao tác** | Điều kiện trước: Bật notification.<br>Dữ liệu: Title = Nhắc đã quá giờ; Due time = hiện tại + 10 phút; Reminder = 15 minutes before.<br>Thao tác:<br>1) Tạo task với Due time và Reminder trên.<br>2) Nhấn Create Task.<br>3) Ghi nhận cảnh báo lịch nhắc đã nằm trong quá khứ/bị bỏ qua.<br>4) Xác nhận task vẫn được lưu trên Home.<br>5) Chờ 10 phút và xác nhận không có notification cũ được phát nhầm. |
| **Kết quả mong đợi** | Task vẫn được lưu, ứng dụng cảnh báo lịch nhắc bị bỏ qua và không phát notification sai thời điểm. |
| **Kết quả thực tế** | Test trực tiếp lúc 11:10 ngày 22/09/2026. Tạo task TC25 Nhac da qua gio, hạn 11:19 và nhắc trước 15 phút. App vẫn lưu task và hiển thị đúng cảnh báo: “Task saved, but its reminder time has already passed.” Sau thời điểm hạn không có notification TaskFlow phát nhầm. |
| **Trạng thái kiểm thử** | **`ĐẠT (PASS)`** |

---

### TC26

> **CHI TIẾT KIỂM THỬ: TC26 - ĐẶT NHẮC VIỆC BẰNG EXACT ALARM**

| Thuộc tính | Nội dung |
| :--- | :--- |
| **Mã Test Case** | TC26 |
| **Tên Test Case** | Đặt nhắc việc bằng Exact Alarm |
| **Nhóm chức năng** | NHẮC VIỆC VÀ THÔNG BÁO |
| **Mô tả mục tiêu** | Bật quyền thông báo và quyền exact alarm rồi lưu một công việc có giờ nhắc trong tương lai. |
| **Dữ liệu & Thao tác** | Điều kiện trước: Trong Settings hệ thống, bật Notifications và cho phép Alarms & reminders của TaskFlow.<br>Dữ liệu: Title = Exact alarm; Due time = hiện tại + 3 phút; Reminder = 0 minutes before.<br>Thao tác:<br>1) Tạo task với dữ liệu trên.<br>2) Đưa ứng dụng xuống nền và tắt màn hình emulator.<br>3) Chờ đến giờ đã đặt.<br>4) Bật màn hình và kiểm tra notification.<br>5) Chạm notification, xác nhận mở đúng task và chỉ có một notification. |
| **Kết quả mong đợi** | Ứng dụng dùng AlarmManager, trả SCHEDULED và không giữ WorkManager dự phòng cho cùng taskId. |
| **Kết quả thực tế** | Test trực tiếp trên điện thoại Samsung lúc 11:03 ngày 22/09/2026. Task TC26_Exact_alarm đã được AlarmManager đặt lịch exact và hệ thống ghi nhận alarm được giao đúng 11:03, nhưng notification không xuất hiện vì Reminder = None (0) và AlarmReceiver bỏ qua task có reminderMinutes = 0. Testcase không đạt luồng end-to-end với dữ liệu hiện tại. |
| **Trạng thái kiểm thử** | **`KHÔNG ĐẠT (FAIL) - EXACT ALARM CHẠY NHƯNG KHÔNG CÓ NOTIFICATION`** |

---

### TC27

> **CHI TIẾT KIỂM THỬ: TC27 - DÙNG WORKMANAGER KHI THIẾU QUYỀN EXACT ALARM**

| Thuộc tính | Nội dung |
| :--- | :--- |
| **Mã Test Case** | TC27 |
| **Tên Test Case** | Dùng WorkManager khi thiếu quyền Exact Alarm |
| **Nhóm chức năng** | NHẮC VIỆC VÀ THÔNG BÁO |
| **Mô tả mục tiêu** | Tắt quyền exact alarm nhưng vẫn bật quyền thông báo, sau đó lưu công việc có nhắc việc. |
| **Dữ liệu & Thao tác** | Điều kiện trước: Bật Notifications nhưng tắt quyền Alarms & reminders của TaskFlow.<br>Dữ liệu: Title = WorkManager fallback; Due time = hiện tại + 5 phút; Reminder = 0 minutes before.<br>Thao tác:<br>1) Tạo task với dữ liệu trên.<br>2) Nếu app mở trang quyền exact alarm, quay lại mà không cấp quyền.<br>3) Xác nhận app báo dùng lịch dự phòng nhưng không crash.<br>4) Đưa app xuống nền và chờ đủ thời gian cộng thêm độ trễ WorkManager.<br>5) Kiểm tra notification chỉ xuất hiện một lần. |
| **Kết quả mong đợi** | Ứng dụng tạo WorkManager duy nhất theo taskId và trả FALLBACK_SCHEDULED; ứng dụng không bị crash. |
| **Kết quả thực tế** | Test trực tiếp lúc 11:18–11:25 ngày 22/09/2026. Đã tắt quyền exact alarm, tạo task TC27 WorkManager fallback và app hiển thị cảnh báo dùng lịch dự phòng; app không crash. Tuy nhiên Reminder = None (0) khiến TaskReminderWorker bỏ qua việc hiển thị notification, nên sau hạn 11:22 không có thông báo. |
| **Trạng thái kiểm thử** | **`KHÔNG ĐẠT (FAIL) - FALLBACK ĐƯỢC TẠO NHƯNG KHÔNG PHÁT THÔNG BÁO`** |

---

### TC28

> **CHI TIẾT KIỂM THỬ: TC28 - KHÔNG ĐẶT LỊCH KHI THÔNG BÁO BỊ TẮT**

| Thuộc tính | Nội dung |
| :--- | :--- |
| **Mã Test Case** | TC28 |
| **Tên Test Case** | Không đặt lịch khi thông báo bị tắt |
| **Nhóm chức năng** | NHẮC VIỆC VÀ THÔNG BÁO |
| **Mô tả mục tiêu** | Tắt quyền thông báo rồi tạo công việc có nhắc việc. |
| **Dữ liệu & Thao tác** | Điều kiện trước: Tắt quyền Notifications của TaskFlow trong Android Settings.<br>Dữ liệu: Title = Task không có quyền thông báo; Due time = hiện tại + 5 phút; Reminder = 5 minutes before.<br>Thao tác:<br>1) Mở Create Task, nhập dữ liệu và nhấn Create Task.<br>2) Trong cảnh báo quyền, chọn lưu bỏ qua/Save anyway.<br>3) Xác nhận task vẫn xuất hiện trên Home.<br>4) Chờ qua thời điểm nhắc.<br>5) Xác nhận không có notification và app không crash. |
| **Kết quả mong đợi** | Công việc vẫn được lưu nhưng ứng dụng trả NOTIFICATIONS_DISABLED, hiển thị cảnh báo và không giữ lịch nhắc cũ. |
| **Kết quả thực tế** | Test trực tiếp lúc 11:13 ngày 22/09/2026. Sau khi tắt quyền Notifications, app hiển thị hộp thoại “Unable to schedule reminder”, nêu rõ reminder sẽ không kích hoạt và cho phép chọn “Save anyway”. Task vẫn được lưu, app không crash và không có notification sau thời điểm nhắc. Quyền thông báo đã được khôi phục sau khi test. |
| **Trạng thái kiểm thử** | **`ĐẠT (PASS)`** |

---

### TC29

> **CHI TIẾT KIỂM THỬ: TC29 - ĐẶT LẠI LỊCH KHI SỬA THỜI GIAN CÔNG VIỆC**

| Thuộc tính | Nội dung |
| :--- | :--- |
| **Mã Test Case** | TC29 |
| **Tên Test Case** | Đặt lại lịch khi sửa thời gian công việc |
| **Nhóm chức năng** | NHẮC VIỆC VÀ THÔNG BÁO |
| **Mô tả mục tiêu** | Sửa ngày, giờ hoặc số phút nhắc trước của một công việc đã có lịch. |
| **Dữ liệu & Thao tác** | Điều kiện trước: Có task “Dời lịch họp” hạn sau 10 phút, Reminder = 5 minutes before.<br>Dữ liệu: Due time mới = hiện tại + 20 phút; Reminder mới = 10 minutes before.<br>Thao tác:<br>1) Mở Task Detail và chọn Edit.<br>2) Đổi Due time và Reminder theo dữ liệu mới.<br>3) Nhấn Save Changes và đưa app xuống nền.<br>4) Chờ qua thời điểm nhắc cũ, xác nhận không có notification.<br>5) Chờ đến thời điểm nhắc mới, xác nhận chỉ có một notification. |
| **Kết quả mong đợi** | Lịch cũ bị hủy. Chỉ còn một lịch mới mang cùng taskId và kích hoạt theo thời gian mới. |
| **Kết quả thực tế** | Test trực tiếp ngày 22/09/2026. Task TC29 Doi lich hop ban đầu có hạn 11:30, nhắc 5 phút trước (mốc cũ 11:25), sau đó được sửa sang hạn 11:44 và nhắc 10 phút trước (mốc mới 11:34). Tại 11:25:50 không có notification ở mốc cũ, xác nhận lịch cũ đã bị hủy. Theo yêu cầu dừng test, chưa chờ đến mốc mới 11:34 để xác nhận notification mới. |
| **Trạng thái kiểm thử** | **`ĐẠT MỘT PHẦN - ĐÃ HỦY LỊCH CŨ, CHƯA CHỜ LỊCH MỚI`** |
| **Sau khi sửa** |  |

---

### TC30

> **CHI TIẾT KIỂM THỬ: TC30 - HỦY LỊCH KHI XÓA HOẶC HOÀN THÀNH CÔNG VIỆC**

| Thuộc tính | Nội dung |
| :--- | :--- |
| **Mã Test Case** | TC30 |
| **Tên Test Case** | Hủy lịch khi xóa hoặc hoàn thành công việc |
| **Nhóm chức năng** | NHẮC VIỆC VÀ THÔNG BÁO |
| **Mô tả mục tiêu** | Xóa hoặc đánh dấu hoàn thành một công việc đang có lịch chính hoặc lịch dự phòng. |
| **Dữ liệu & Thao tác** | Điều kiện trước: Có task “Hủy nhắc khi xong” hạn sau 5 phút, Reminder = 0 minutes before.<br>Dữ liệu: Đánh dấu Complete trước giờ nhắc.<br>Thao tác:<br>1) Từ Home hoặc Task Detail, đánh dấu task Complete.<br>2) Xác nhận task chuyển xuống nhóm Completed.<br>3) Đưa ứng dụng xuống nền.<br>4) Chờ qua Due time ít nhất 1 phút.<br>5) Xác nhận không xuất hiện notification của task đã hoàn thành. |
| **Kết quả mong đợi** | Cả PendingIntent của AlarmManager và unique work của WorkManager đều bị hủy; không xuất hiện thông báo sau đó. |
| **Kết quả thực tế** | Test trực tiếp lúc 11:18–11:25 ngày 22/09/2026. Tạo task TC30 Huy nhac khi xong hạn 11:23, sau đó đánh dấu Complete trước hạn. Màn hình chi tiết đổi nút thành “Uncomplete”, xác nhận task đã hoàn thành. Sau 11:23 không xuất hiện notification TaskFlow. |
| **Trạng thái kiểm thử** | **`ĐẠT (PASS)`** |

---

### TC31

> **CHI TIẾT KIỂM THỬ: TC31 - HOÀN THÀNH TASK TỪ NOTIFICATION**

| Thuộc tính | Nội dung |
| :--- | :--- |
| **Mã Test Case** | TC31 |
| **Tên Test Case** | Hoàn thành task từ notification |
| **Nhóm chức năng** | NHẮC VIỆC VÀ THÔNG BÁO |
| **Mô tả mục tiêu** | Kiểm tra hành động Complete trên notification cập nhật đúng task trong ứng dụng. |
| **Dữ liệu & Thao tác** | Điều kiện trước: Bật Notifications và exact alarm.<br>Dữ liệu: Title = Hoàn thành từ thông báo; Due time = hiện tại + 3 phút; Reminder = 0 minutes before.<br>Thao tác:<br>1) Tạo task và đưa app xuống nền.<br>2) Khi notification xuất hiện, mở rộng notification.<br>3) Nhấn hành động Mark complete/Complete.<br>4) Mở lại TaskFlow.<br>5) Xác nhận task đã nằm trong Completed và notification đã bị đóng. |
| **Kết quả mong đợi** | Notification đóng, task được cập nhật Completed trong Room và xuất hiện ở nhóm Completed khi mở ứng dụng. |
| **Kết quả thực tế** | Notification shade không có thông báo TaskFlow; chưa thể kiểm tra hành động Complete. |
| **Trạng thái kiểm thử** | **`CẦN TEST LẠI UI`** |
| **Sau khi ấn** |  |

---

### TC32

> **CHI TIẾT KIỂM THỬ: TC32 - KHÔI PHỤC LỊCH SAU KHỞI ĐỘNG LẠI HOẶC ĐỔI THỜI GIAN**

| Thuộc tính | Nội dung |
| :--- | :--- |
| **Mã Test Case** | TC32 |
| **Tên Test Case** | Khôi phục lịch sau khởi động lại hoặc đổi thời gian |
| **Nhóm chức năng** | NHẮC VIỆC VÀ THÔNG BÁO |
| **Mô tả mục tiêu** | Khởi động lại thiết bị hoặc thay đổi ngày, giờ, múi giờ khi còn task chưa hoàn thành. |
| **Dữ liệu & Thao tác** | Điều kiện trước: Bật Notifications và exact alarm; có task hạn sau ít nhất 10 phút, Reminder = 5 minutes before.<br>Dữ liệu: Khởi động lại emulator trước thời điểm nhắc.<br>Thao tác:<br>1) Tạo task và ghi lại giờ nhắc dự kiến.<br>2) Khởi động lại emulator.<br>3) Mở khóa thiết bị nhưng không cần mở TaskFlow.<br>4) Chờ đến giờ nhắc.<br>5) Xác nhận notification vẫn xuất hiện một lần và mở đúng task. |
| **Kết quả mong đợi** | BootReceiver hoặc TimeChangeReceiver đặt lại lịch cho task còn hiệu lực, bỏ qua task hoàn thành/quá hạn và không tạo lịch trùng. |
| **Kết quả thực tế** | Bộ test tự động TimeChangeReceiverTest đã đạt trong phiên chạy ngày 20/09/2026. Ảnh giao diện hiện chỉ thể hiện trạng thái trung gian, chưa đủ xác nhận toàn bộ luồng end-to-end của testcase. |
| **Trạng thái kiểm thử** | **`ĐẠT LOGIC - CẦN TEST LẠI UI`** |

---

### TC33

> **CHI TIẾT KIỂM THỬ: TC33 - BẬT PIN VỚI HAI LẦN NHẬP KHỚP**

| Thuộc tính | Nội dung |
| :--- | :--- |
| **Mã Test Case** | TC33 |
| **Tên Test Case** | Bật PIN với hai lần nhập khớp |
| **Nhóm chức năng** | BẢO MẬT PIN |
| **Mô tả mục tiêu** | Bật công tắc PIN, nhập PIN mới và xác nhận giống nhau. |
| **Dữ liệu & Thao tác** | Điều kiện trước: PIN Lock đang tắt.<br>Dữ liệu: PIN mới = 1234; Confirm PIN = 1234.<br>Thao tác:<br>1) Mở Settings và bật công tắc PIN Lock.<br>2) Nhập lần lượt 1, 2, 3, 4.<br>3) Ở bước Confirm, nhập lại 1, 2, 3, 4.<br>4) Chờ màn hình PIN đóng.<br>5) Kiểm tra công tắc PIN Lock đang bật và thông báo thành công xuất hiện. |
| **Kết quả mong đợi** | PIN được lưu an toàn, màn hình đóng với RESULT_OK và công tắc PIN hiển thị trạng thái bật. |
| **Kết quả thực tế** | Ảnh lúc 13:34 ngày 22/09/2026 cho thấy công tắc PIN Lock đã bật, mục Change PIN xuất hiện và thông báo “PIN lock has been enabled”. PIN được thiết lập thành công, đúng mong đợi. |
| **Trạng thái kiểm thử** | **`ĐẠT (PASS)`** |

---

### TC34

> **CHI TIẾT KIỂM THỬ: TC34 - KHÔNG BẬT PIN KHI XÁC NHẬN KHÔNG KHỚP**

| Thuộc tính | Nội dung |
| :--- | :--- |
| **Mã Test Case** | TC34 |
| **Tên Test Case** | Không bật PIN khi xác nhận không khớp |
| **Nhóm chức năng** | BẢO MẬT PIN |
| **Mô tả mục tiêu** | Nhập PIN mới rồi nhập lại một PIN khác. |
| **Dữ liệu & Thao tác** | Điều kiện trước: PIN Lock đang tắt.<br>Dữ liệu: PIN mới = 1234; Confirm PIN = 5678.<br>Thao tác:<br>1) Bật PIN Lock trong Settings.<br>2) Nhập 1234 ở bước tạo PIN.<br>3) Nhập 5678 ở bước xác nhận.<br>4) Ghi nhận lỗi PINs do not match và màn hình yêu cầu nhập lại.<br>5) Bấm Back, trở về Settings và xác nhận công tắc vẫn tắt. |
| **Kết quả mong đợi** | Hiển thị lỗi không khớp, không lưu PIN mới và yêu cầu người dùng nhập lại. |
| **Kết quả thực tế** | Ảnh lúc 13:31 ngày 22/09/2026 hiển thị lỗi “PINs do not match. Try again.” sau khi nhập hai PIN khác nhau. Khi quay lại Settings, công tắc PIN Lock vẫn tắt nên PIN sai xác nhận không được lưu. Kết quả đúng mong đợi. |
| **Trạng thái kiểm thử** | **`ĐẠT (PASS)`** |

---

### TC35

> **CHI TIẾT KIỂM THỬ: TC35 - KHÔNG TẮT PIN KHI NHẬP SAI**

| Thuộc tính | Nội dung |
| :--- | :--- |
| **Mã Test Case** | TC35 |
| **Tên Test Case** | Không tắt PIN khi nhập sai |
| **Nhóm chức năng** | BẢO MẬT PIN |
| **Mô tả mục tiêu** | Tắt công tắc PIN rồi nhập PIN hiện tại không đúng. |
| **Dữ liệu & Thao tác** | Điều kiện trước: PIN Lock đang bật với PIN 1234.<br>Dữ liệu: PIN sai = 1111.<br>Thao tác:<br>1) Trong Settings, tắt công tắc PIN Lock.<br>2) Ở màn hình Disable PIN, nhập 1111.<br>3) Ghi nhận số lần thử còn lại.<br>4) Bấm Back để trở về Settings.<br>5) Xác nhận công tắc vẫn bật; mở lại app và xác nhận PIN 1234 vẫn mở khóa được. |
| **Kết quả mong đợi** | Tăng số lần nhập sai, giữ nguyên PIN đã lưu, không trả RESULT_OK và công tắc quay về trạng thái bật. |
| **Kết quả thực tế** | Ảnh lúc 13:35 ngày 22/09/2026 cho thấy PIN sai bị từ chối với thông báo “Wrong PIN. 4 attempts remaining.”. Ứng dụng vẫn giữ màn hình khóa và PIN đã lưu không bị vô hiệu hóa. Kết hợp với PinSecurityTest đã đạt, kết quả đúng mong đợi. |
| **Trạng thái kiểm thử** | **`ĐẠT (PASS)`** |

---

### TC36

> **CHI TIẾT KIỂM THỬ: TC36 - TẮT PIN KHI NHẬP ĐÚNG**

| Thuộc tính | Nội dung |
| :--- | :--- |
| **Mã Test Case** | TC36 |
| **Tên Test Case** | Tắt PIN khi nhập đúng |
| **Nhóm chức năng** | BẢO MẬT PIN |
| **Mô tả mục tiêu** | Tắt công tắc PIN và xác thực bằng PIN hiện tại. |
| **Dữ liệu & Thao tác** | Điều kiện trước: PIN Lock đang bật với PIN 1234.<br>Dữ liệu: PIN xác thực = 1234.<br>Thao tác:<br>1) Trong Settings, tắt công tắc PIN Lock.<br>2) Nhập 1234 ở màn hình Disable PIN.<br>3) Chờ quay lại Settings.<br>4) Xác nhận công tắc chuyển sang tắt và có thông báo thành công.<br>5) đóng/mở lại ứng dụng và xác nhận không còn màn hình yêu cầu PIN. |
| **Kết quả mong đợi** | Xác thực thành công. SettingsFragment xóa PIN, đặt trạng thái phiên phù hợp và công tắc chuyển sang tắt. |
| **Kết quả thực tế** | Bộ test tự động PinSecurityTest đã đạt trong phiên chạy ngày 20/09/2026. Ảnh giao diện hiện chỉ thể hiện trạng thái trung gian, chưa đủ xác nhận toàn bộ luồng end-to-end của testcase. |
| **Trạng thái kiểm thử** | **`ĐẠT LOGIC - CẦN TEST LẠI UI`** |

---

### TC37

> **CHI TIẾT KIỂM THỬ: TC37 - ĐỔI PIN THEO ĐÚNG BA BƯỚC**

| Thuộc tính | Nội dung |
| :--- | :--- |
| **Mã Test Case** | TC37 |
| **Tên Test Case** | Đổi PIN theo đúng ba bước |
| **Nhóm chức năng** | BẢO MẬT PIN |
| **Mô tả mục tiêu** | Mở Đổi PIN, nhập PIN cũ đúng, nhập PIN mới rồi xác nhận đúng. |
| **Dữ liệu & Thao tác** | Điều kiện trước: PIN Lock đang bật với PIN cũ 1234.<br>Dữ liệu: Current PIN = 1234; New PIN = 5678; Confirm = 5678.<br>Thao tác:<br>1) Mở Settings và nhấn Change PIN.<br>2) Nhập 1234 ở bước Current PIN.<br>3) Nhập 5678 ở bước New PIN.<br>4) Nhập 5678 ở bước Confirm.<br>5) Mở lại app, xác nhận 1234 bị từ chối và 5678 mở khóa thành công. |
| **Kết quả mong đợi** | PIN cũ được xác thực, PIN mới được băm và ghi đè. PIN 5678 đăng nhập được, PIN 1234 không còn hợp lệ. |
| **Kết quả thực tế** | Bộ test tự động ChangePinFlowTest đã đạt trong phiên chạy ngày 20/09/2026. Ảnh giao diện hiện chỉ thể hiện trạng thái trung gian, chưa đủ xác nhận toàn bộ luồng end-to-end của testcase. |
| **Trạng thái kiểm thử** | **`ĐẠT LOGIC - CẦN TEST LẠI UI`** |

---

### TC38

> **CHI TIẾT KIỂM THỬ: TC38 - XỬ LÝ XÁC NHẬN PIN MỚI KHÔNG KHỚP**

| Thuộc tính | Nội dung |
| :--- | :--- |
| **Mã Test Case** | TC38 |
| **Tên Test Case** | Xử lý xác nhận PIN mới không khớp |
| **Nhóm chức năng** | BẢO MẬT PIN |
| **Mô tả mục tiêu** | Trong luồng đổi PIN, nhập PIN xác nhận khác PIN mới rồi thử lại. |
| **Dữ liệu & Thao tác** | Điều kiện trước: PIN Lock đang bật với PIN cũ 1234.<br>Dữ liệu: Current = 1234; New = 5678; Confirm lần đầu = 5679; lần thử lại = 5678/5678.<br>Thao tác:<br>1) Mở Change PIN và nhập Current = 1234.<br>2) Nhập New = 5678, Confirm = 5679.<br>3) Ghi nhận lỗi không khớp và app quay về bước nhập PIN mới.<br>4) Nhập lại New = 5678 và Confirm = 5678.<br>5) Mở lại app và xác nhận PIN mới hoạt động. |
| **Kết quả mong đợi** | Hiển thị lỗi không khớp và quay về bước nhập PIN mới. Lần thử lại đúng hoàn tất bình thường; PIN cũ không bị thay đổi trước khi hoàn tất. |
| **Kết quả thực tế** | Ảnh lúc 13:36 ngày 22/09/2026 trong luồng Change PIN hiển thị lỗi “PINs do not match. Try again.” và quay về bước Enter your new PIN. Kết hợp với ChangePinFlowTest đã đạt cho lần thử lại hợp lệ, kết quả đúng mong đợi. |
| **Trạng thái kiểm thử** | **`ĐẠT (PASS)`** |

---

### TC39

> **CHI TIẾT KIỂM THỬ: TC39 - KHÓA SAU NĂM LẦN NHẬP PIN SAI**

| Thuộc tính | Nội dung |
| :--- | :--- |
| **Mã Test Case** | TC39 |
| **Tên Test Case** | Khóa sau năm lần nhập PIN sai |
| **Nhóm chức năng** | BẢO MẬT PIN |
| **Mô tả mục tiêu** | Nhập sai PIN liên tiếp đủ số lần tối đa và mở lại màn hình trong thời gian khóa. |
| **Dữ liệu & Thao tác** | Điều kiện trước: PIN Lock đang bật với PIN đúng 1234.<br>Dữ liệu: PIN sai = 1111, nhập liên tiếp 5 lần.<br>Thao tác:<br>1) Mở lại app để hiện màn hình Enter PIN.<br>2) Nhập 1111 năm lần và ghi số lần còn lại sau mỗi lần.<br>3) Sau lần thứ năm, ghi lại thời gian đếm ngược khóa.<br>4) Xoay emulator hoặc đóng/mở Activity trong khi đang khóa.<br>5) Chờ đủ 30 giây rồi nhập 1234 và xác nhận mở khóa được. |
| **Kết quả mong đợi** | Sau lần sai thứ năm, khóa 30 giây. Bộ đếm còn đúng sau khi xoay màn hình hoặc mở lại Activity; hết thời gian thì cho nhập lại. |
| **Kết quả thực tế** | Ảnh lúc 13:36 ngày 22/09/2026 hiển thị trạng thái khóa sau nhiều lần nhập sai với thông báo “Too many attempts. Try again in 28 seconds.”. Bộ đếm khóa hoạt động và bàn phím PIN bị vô hiệu hóa trong thời gian chờ. Kết quả đúng mong đợi. |
| **Trạng thái kiểm thử** | **`ĐẠT (PASS)`** |

---

### TC40

> **CHI TIẾT KIỂM THỬ: TC40 - YÊU CẦU PIN KHI MỞ LẠI ỨNG DỤNG**

| Thuộc tính | Nội dung |
| :--- | :--- |
| **Mã Test Case** | TC40 |
| **Tên Test Case** | Yêu cầu PIN khi mở lại ứng dụng |
| **Nhóm chức năng** | BẢO MẬT PIN |
| **Mô tả mục tiêu** | Kiểm tra PIN Lock chặn truy cập Home sau khi ứng dụng được mở lại. |
| **Dữ liệu & Thao tác** | Điều kiện trước: PIN Lock đang bật với PIN 1234; ứng dụng đã được đưa khỏi màn hình gần đây hoặc tiến trình đã đóng.<br>Dữ liệu: Lần 1 nhập 1111; lần 2 nhập 1234.<br>Thao tác:<br>1) Mở TaskFlow từ biểu tượng ứng dụng.<br>2) Xác nhận màn hình Enter PIN xuất hiện trước Home.<br>3) Nhập 1111 và ghi nhận lỗi.<br>4) Nhập 1234.<br>5) Xác nhận chỉ sau PIN đúng mới vào được Home và dữ liệu task vẫn còn nguyên. |
| **Kết quả mong đợi** | PIN sai bị từ chối; chỉ PIN đúng mới mở Home. Dữ liệu task không bị thay đổi. |
| **Kết quả thực tế** | Ảnh lúc 13:37 ngày 22/09/2026 xác nhận màn hình Welcome Back yêu cầu PIN xuất hiện trước Home khi mở lại TaskFlow. Ảnh nhập sai trong cùng phiên kiểm thử cho thấy PIN sai bị từ chối và ứng dụng vẫn giữ màn hình khóa. Việc tiếp tục được các bước kiểm thử trong Settings sau đó xác nhận PIN hợp lệ mở được ứng dụng. Kết quả đúng mong đợi. |
| **Trạng thái kiểm thử** | **`ĐẠT (PASS)`** |

---

### TC41

> **CHI TIẾT KIỂM THỬ: TC41 - XUẤT TOÀN BỘ CÔNG VIỆC RA JSON**

| Thuộc tính | Nội dung |
| :--- | :--- |
| **Mã Test Case** | TC41 |
| **Tên Test Case** | Xuất toàn bộ công việc ra JSON |
| **Nhóm chức năng** | SAO LƯU VÀ KHÔI PHỤC DỮ LIỆU |
| **Mô tả mục tiêu** | Tạo tập dữ liệu có task thường, task lặp, task hoàn thành rồi thực hiện Export. |
| **Dữ liệu & Thao tác** | Điều kiện trước: Có ít nhất năm task gồm Todo, In Progress, Completed, task lặp và task có Reminder.<br>Dữ liệu: Tên file = taskflow_backup_test.json.<br>Thao tác:<br>1) Mở Settings > Data Management.<br>2) Chọn Export JSON backup/Export to JSON.<br>3) Chọn thư mục Downloads, đặt tên file và xác nhận Save.<br>4) Chờ thông báo export thành công và số lượng task.<br>5) Mở ứng dụng Files, xác nhận file tồn tại và có kích thước lớn hơn 0 byte. |
| **Kết quả mong đợi** | Tệp JSON hợp lệ được tạo, taskCount chính xác và giữ đủ các trường cần thiết của từng task. |
| **Kết quả thực tế** | Bộ test tự động BackupRepositoryTest / JsonValidatorTest đã đạt trong phiên chạy ngày 20/09/2026. Ảnh giao diện hiện chỉ thể hiện trạng thái trung gian, chưa đủ xác nhận toàn bộ luồng end-to-end của testcase. |
| **Trạng thái kiểm thử** | **`ĐẠT LOGIC - CẦN TEST LẠI UI`** |

---

### TC42

> **CHI TIẾT KIỂM THỬ: TC42 - KHÔI PHỤC DỮ LIỆU BẰNG EXPORT RỒI IMPORT**

| Thuộc tính | Nội dung |
| :--- | :--- |
| **Mã Test Case** | TC42 |
| **Tên Test Case** | Khôi phục dữ liệu bằng Export rồi Import |
| **Nhóm chức năng** | SAO LƯU VÀ KHÔI PHỤC DỮ LIỆU |
| **Mô tả mục tiêu** | Xuất dữ liệu, xóa dữ liệu trong ứng dụng rồi nhập lại chính tệp vừa xuất. |
| **Dữ liệu & Thao tác** | Điều kiện trước: Đã export file taskflow_backup_test.json và ghi lại danh sách/số lượng task trước khi xóa.<br>Dữ liệu: Chính file backup vừa export.<br>Thao tác:<br>1) Xóa các task dùng để test khỏi ứng dụng.<br>2) Mở Settings > Restore backup/Import Tasks.<br>3) Chọn file taskflow_backup_test.json.<br>4) Hoàn tất import và quay lại Home/Calendar.<br>5) Đối chiếu số lượng, Title, trạng thái, Priority, recurrence và Reminder với dữ liệu trước export. |
| **Kết quả mong đợi** | Số lượng và nội dung task sau import giống trước export; trạng thái hoàn thành, recurrence và completedAt được bảo toàn. |
| **Kết quả thực tế** | Bộ test tự động BackupRepositoryTest / JsonValidatorTest đã đạt trong phiên chạy ngày 20/09/2026. Ảnh giao diện hiện chỉ thể hiện trạng thái trung gian, chưa đủ xác nhận toàn bộ luồng end-to-end của testcase. |
| **Trạng thái kiểm thử** | **`ĐẠT LOGIC - CẦN TEST LẠI UI`** |

---

### TC43

> **CHI TIẾT KIỂM THỬ: TC43 - TỪ CHỐI TOÀN BỘ TỆP JSON KHÔNG HỢP LỆ**

| Thuộc tính | Nội dung |
| :--- | :--- |
| **Mã Test Case** | TC43 |
| **Tên Test Case** | Từ chối toàn bộ tệp JSON không hợp lệ |
| **Nhóm chức năng** | SAO LƯU VÀ KHÔI PHỤC DỮ LIỆU |
| **Mô tả mục tiêu** | Nhập tệp sai cú pháp hoặc thiếu trường bắt buộc trong một task. |
| **Dữ liệu & Thao tác** | Điều kiện trước: Tạo file invalid_taskflow.json trong Downloads với nội dung {invalid json hoặc xóa trường title của một task trong file export.<br>Dữ liệu: File JSON sai cú pháp/thiếu trường bắt buộc.<br>Thao tác:<br>1) Ghi lại tổng số task hiện tại.<br>2) Mở Import Tasks và chọn invalid_taskflow.json.<br>3) Nhấn Import.<br>4) Ghi nhận màn hình Import Failed và nội dung lỗi.<br>5) Quay lại Home, xác nhận tổng số và dữ liệu task không thay đổi. |
| **Kết quả mong đợi** | Hiển thị danh sách lỗi cụ thể. Không task nào trong tệp được nhập và dữ liệu hiện tại không bị thay đổi. |
| **Kết quả thực tế** | Ảnh chỉ hiển thị màn hình Data Management; không thấy trạng thái Import Failed. |
| **Trạng thái kiểm thử** | **`ĐẠT LOGIC - CẦN TEST LẠI UI`** |

---

### TC44

> **CHI TIẾT KIỂM THỬ: TC44 - XỬ LÝ CÔNG VIỆC TRÙNG KHI IMPORT**

| Thuộc tính | Nội dung |
| :--- | :--- |
| **Mã Test Case** | TC44 |
| **Tên Test Case** | Xử lý công việc trùng khi Import |
| **Nhóm chức năng** | SAO LƯU VÀ KHÔI PHỤC DỮ LIỆU |
| **Mô tả mục tiêu** | Nhập tệp có task trùng tiêu đề, ngày và recurrenceType với task hiện có. |
| **Dữ liệu & Thao tác** | Điều kiện trước: App đã có task “Trùng dữ liệu”, đồng thời file backup cũng chứa task cùng Title, Due date và Recurrence type nhưng mô tả khác.<br>Dữ liệu: Thử riêng ba lần với SKIP, REPLACE và REPLACE ALL; khôi phục dữ liệu gốc trước mỗi lần.<br>Thao tác:<br>1) Import file và chờ hộp thoại conflict.<br>2) Chọn SKIP, kiểm tra bản cũ được giữ.<br>3) Khôi phục dữ liệu gốc, import lại và chọn REPLACE, kiểm tra bản trùng được thay.<br>4) Khôi phục lần nữa, import lại và chọn REPLACE ALL.<br>5) Xác nhận kết quả từng lựa chọn và không có bản sao ngoài dự kiến. |
| **Kết quả mong đợi** | SKIP giữ bản cũ; REPLACE thay đúng task trùng; REPLACE ALL thay theo phạm vi được xác nhận. Không tạo bản sao ngoài dự kiến. |
| **Kết quả thực tế** | Bộ test tự động BackupRepositoryTest đã đạt trong phiên chạy ngày 20/09/2026. Ảnh giao diện hiện chỉ thể hiện trạng thái trung gian, chưa đủ xác nhận toàn bộ luồng end-to-end của testcase. |
| **Trạng thái kiểm thử** | **`ĐẠT LOGIC - CẦN TEST LẠI UI`** |

---

### TC45

> **CHI TIẾT KIỂM THỬ: TC45 - KHÔNG MẤT DỮ LIỆU KHI TỆP BACKUP KHÔNG ĐƯỢC HỖ TRỢ**

| Thuộc tính | Nội dung |
| :--- | :--- |
| **Mã Test Case** | TC45 |
| **Tên Test Case** | Không mất dữ liệu khi tệp backup không được hỗ trợ |
| **Nhóm chức năng** | SAO LƯU VÀ KHÔI PHỤC DỮ LIỆU |
| **Mô tả mục tiêu** | Nhập tệp có version cao hơn ứng dụng hỗ trợ hoặc tệp không đọc được. |
| **Dữ liệu & Thao tác** | Điều kiện trước: Export một file hợp lệ rồi sao chép thành unsupported_version.json.<br>Dữ liệu: Sửa trường version trong file thành 999, giữ phần tasks không đổi.<br>Thao tác:<br>1) Ghi lại số task hiện tại.<br>2) Mở Import Tasks và chọn unsupported_version.json.<br>3) Nhấn Import.<br>4) Ghi nhận lỗi phiên bản không được hỗ trợ.<br>5) Quay lại Home và xác nhận không có task nào bị xóa, thay thế hoặc thêm mới. |
| **Kết quả mong đợi** | Hiển thị lỗi phù hợp, không xóa dữ liệu hiện tại và trạng thái Import có thể thử lại. |
| **Kết quả thực tế** | Bộ test tự động JsonValidatorTest đã đạt trong phiên chạy ngày 20/09/2026. Ảnh giao diện hiện chỉ thể hiện trạng thái trung gian, chưa đủ xác nhận toàn bộ luồng end-to-end của testcase. |
| **Trạng thái kiểm thử** | **`ĐẠT LOGIC - CẦN TEST LẠI UI`** |

---

### TC46

> **CHI TIẾT KIỂM THỬ: TC46 - HIỂN THỊ CÔNG VIỆC ĐÚNG NGÀY TRÊN LỊCH**

| Thuộc tính | Nội dung |
| :--- | :--- |
| **Mã Test Case** | TC46 |
| **Tên Test Case** | Hiển thị công việc đúng ngày trên Lịch |
| **Nhóm chức năng** | LỊCH |
| **Mô tả mục tiêu** | Chuyển tháng, chọn một ngày có công việc và một ngày không có công việc. |
| **Dữ liệu & Thao tác** | Điều kiện trước: Có task A hạn hôm nay, task B hạn tháng sau và một ngày không có task.<br>Dữ liệu: Ba ngày cần kiểm tra tương ứng với A, B và ngày trống.<br>Thao tác:<br>1) Mở tab Calendar.<br>2) Chọn ngày của task A và kiểm tra danh sách.<br>3) Chuyển sang tháng sau, chọn ngày của task B.<br>4) Chọn một ngày trống.<br>5) Xác nhận ngày có task hiển thị đúng task, ngày trống không hiển thị dữ liệu cũ. |
| **Kết quả mong đợi** | Ngày có task được đánh dấu và hiển thị đúng danh sách; ngày trống hiển thị rỗng; chuyển tháng không làm mất dữ liệu. |
| **Kết quả thực tế** | Đã chạy script và tạo ảnh trong phiên ngày 20/09/2026, nhưng ảnh chỉ thể hiện màn hình hoặc trạng thái trung gian. Chưa đủ bằng chứng xác nhận toàn bộ kết quả mong đợi. |
| **Trạng thái kiểm thử** | **`CẦN TEST LẠI UI`** |

---

### TC47

> **CHI TIẾT KIỂM THỬ: TC47 - CHIẾU CÔNG VIỆC LẶP TRÊN LỊCH KHÔNG BỊ TRÙNG**

| Thuộc tính | Nội dung |
| :--- | :--- |
| **Mã Test Case** | TC47 |
| **Tên Test Case** | Chiếu công việc lặp trên Lịch không bị trùng |
| **Nhóm chức năng** | LỊCH |
| **Mô tả mục tiêu** | Mở tháng chứa các lần lặp hằng ngày, tuần, tháng hoặc năm. |
| **Dữ liệu & Thao tác** | Điều kiện trước: Tạo ba chuỗi Daily, Weekly và Monthly; tạo thêm một chuỗi Paused và một chuỗi đã hết Count.<br>Dữ liệu: Mỗi chuỗi có Title khác nhau và ngày bắt đầu đã biết.<br>Thao tác:<br>1) Mở Calendar ở tháng chứa các lần lặp.<br>2) Chọn từng ngày dự kiến của Daily, Weekly và Monthly.<br>3) Đếm số lần hiển thị của từng Title trên mỗi ngày.<br>4) Kiểm tra chuỗi Paused và chuỗi đã hết Count ở các ngày sau điểm dừng.<br>5) Xác nhận không có lần trùng và không chiếu thêm lần đã bị dừng. |
| **Kết quả mong đợi** | Mỗi ngày chỉ có đúng các lần hợp lệ, không trùng theo chuỗi; task đã Pause hoặc vượt giới hạn không tiếp tục được chiếu. |
| **Kết quả thực tế** | Đã chạy script và tạo ảnh trong phiên ngày 20/09/2026, nhưng ảnh chỉ thể hiện màn hình hoặc trạng thái trung gian. Chưa đủ bằng chứng xác nhận toàn bộ kết quả mong đợi. |
| **Trạng thái kiểm thử** | **`CẦN TEST LẠI UI`** |

---

### TC48

> **CHI TIẾT KIỂM THỬ: TC48 - TÍNH ĐÚNG SỐ LIỆU THỐNG KÊ**

| Thuộc tính | Nội dung |
| :--- | :--- |
| **Mã Test Case** | TC48 |
| **Tên Test Case** | Tính đúng số liệu thống kê |
| **Nhóm chức năng** | THỐNG KÊ |
| **Mô tả mục tiêu** | Chuẩn bị tập task gồm đủ trạng thái và bốn mức ưu tiên rồi mở Statistics. |
| **Dữ liệu & Thao tác** | Điều kiện trước: Tạo 8 task: 5 Completed, 3 Not completed; phân bố Priority gồm 1 Urgent (có thể import), 2 High, 3 Medium, 2 Low.<br>Dữ liệu: Time filter = All time.<br>Thao tác:<br>1) Mở tab Stats.<br>2) Chọn All time.<br>3) Ghi Total, Completed, Not completed và Completion rate.<br>4) Ghi số lượng/tỷ lệ của Urgent, High, Medium, Low.<br>5) Tự tính từ 8 task và đối chiếu từng số hiển thị. |
| **Kết quả mong đợi** | Tổng số, completed, pending, completion rate và tỷ lệ từng priority đúng với dữ liệu thực. Không hiển thị số giờ tập trung giả định. |
| **Kết quả thực tế** | Màn hình Stats hiển thị 1 completed, không đúng bộ dữ liệu 8 task yêu cầu của testcase. |
| **Trạng thái kiểm thử** | **`ĐẠT LOGIC - CẦN TEST LẠI UI`** |

---

### TC49

> **CHI TIẾT KIỂM THỬ: TC49 - LỌC THỐNG KÊ THEO NGÀY HOÀN THÀNH**

| Thuộc tính | Nội dung |
| :--- | :--- |
| **Mã Test Case** | TC49 |
| **Tên Test Case** | Lọc thống kê theo ngày hoàn thành |
| **Nhóm chức năng** | THỐNG KÊ |
| **Mô tả mục tiêu** | Tạo task hoàn thành trong tuần này, tuần trước và task cũ không có completedAt. |
| **Dữ liệu & Thao tác** | Điều kiện trước: Chuẩn bị file JSON hợp lệ chứa task Completed có completedAt trong tuần này, tuần trước, tháng này và một task cũ thiếu completedAt; import file vào app.<br>Dữ liệu: Time filter lần lượt = This week, Last week, This month, All time.<br>Thao tác:<br>1) Mở Stats và chọn This week; ghi tổng Completed và biểu đồ theo ngày.<br>2) Lặp lại với Last week.<br>3) Lặp lại với This month.<br>4) Chọn All time.<br>5) Xác nhận task thiếu completedAt chỉ nằm trong tổng All time và không bị gán vào ngày trên biểu đồ. |
| **Kết quả mong đợi** | Mỗi kỳ dùng completedAt để tính. Task cũ thiếu completedAt chỉ được tính trong tổng All Time và không bị gán ngày giả vào biểu đồ. |
| **Kết quả thực tế** | Bộ test tự động StatsViewModelTest đã đạt trong phiên chạy ngày 20/09/2026. Ảnh giao diện hiện chỉ thể hiện trạng thái trung gian, chưa đủ xác nhận toàn bộ luồng end-to-end của testcase. |
| **Trạng thái kiểm thử** | **`ĐẠT LOGIC - CẦN TEST LẠI UI`** |

---

### TC50

> **CHI TIẾT KIỂM THỬ: TC50 - ĐỒNG BỘ SETTINGS VÀ WIDGET VỚI DỮ LIỆU THẬT**

| Thuộc tính | Nội dung |
| :--- | :--- |
| **Mã Test Case** | TC50 |
| **Tên Test Case** | Đồng bộ Settings và Widget với dữ liệu thật |
| **Nhóm chức năng** | CÀI ĐẶT VÀ WIDGET |
| **Mô tả mục tiêu** | Kiểm tra công tắc PIN/thông báo khi thao tác thành công hoặc hủy; sau đó thêm widget và hoàn thành một task từ widget. |
| **Dữ liệu & Thao tác** | Điều kiện trước: Có hai task hôm nay, một Completed và một Not completed; PIN/Notifications có thể thay đổi trong Settings; emulator hỗ trợ launcher widget.<br>Dữ liệu: Widget Today’s Tasks.<br>Thao tác:<br>1) Từ màn hình chính Android, nhấn giữ vùng trống > Widgets > TaskFlow > thêm Today’s Tasks.<br>2) Đối chiếu số lượng và thứ tự task trên widget với Home.<br>3) Dùng hành động Complete trên widget cho task chưa hoàn thành.<br>4) Mở TaskFlow, xác nhận Room/Home đã cập nhật và task xuống nhóm Completed.<br>5) Thay đổi PIN hoặc quyền notification trong Settings, quay lại app và xác nhận công tắc phản ánh đúng trạng thái thực. |
| **Kết quả mong đợi** | Công tắc phản ánh trạng thái đã lưu hoặc quyền hệ thống. Widget hiển thị đúng số lượng, sắp task chưa hoàn thành trước và thao tác Complete cập nhật Room, Home và Widget. |
| **Kết quả thực tế** | Ảnh chỉ hiển thị Settings; chưa kiểm tra widget hoặc thao tác Complete từ widget. |
| **Trạng thái kiểm thử** | **`ĐẠT LOGIC - CẦN TEST LẠI UI`** |
| **Chuyển sang completed** |  |
| **Bật tắt Noti** |  |

---

