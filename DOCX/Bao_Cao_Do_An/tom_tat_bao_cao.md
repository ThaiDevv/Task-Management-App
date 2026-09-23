# TỔNG HỢP & TÓM TẮT TOÀN DIỆN BÁO CÁO ĐỒ ÁN
# ĐỀ TÀI: ỨNG DỤNG QUẢN LÝ CÔNG VIỆC CÁ NHÂN (TASKFLOW APP)

---

## 🏛️ THÔNG TIN CHUNG
* **Tên đề tài:** Xây dựng Ứng dụng Quản lý công việc cá nhân trên nền tảng Android (*Task Management Application — TaskFlow*)
* **Học phần:** Lập trình thiết bị di động (Mã lớp học phần: `012012103402`)
* **Đơn vị đào tạo:** Khoa Công nghệ Thông tin — Trường Đại học Giao thông Vận tải TP. Hồ Chí Minh
* **Giảng viên hướng dẫn:** ThS. Mai Ngọc Châu
* **Nhóm sinh viên thực hiện:**
  1. **Trần Văn Thái** — MSSV: `051206002315` *(Trưởng nhóm)*
  2. **Nguyễn Lê Huy Tâm** — MSSV: `056206011188`
  3. **Nguyễn Ngọc Gia Bảo** — MSSV: `079206008279`
  4. **Trần Văn Ngọc Thắng** — MSSV: `046206001641`
  5. **Huỳnh Đình Chấn** — MSSV: `077206002307`
* **Môi trường & Nền tảng:** Android Studio Ladybug/Koala, Kotlin 100%, Min SDK 26 (Android 8.0), Target SDK 34 (Android 14+), Gradle Version Catalog (`libs.versions.toml`).

---

## 📑 MỤC LỤC TÓM TẮT CHI TIẾT
1. [Phần Mở đầu: Đặt vấn đề, Tính cấp thiết, Mục tiêu & Phạm vi](#1-phần-mở-đầu)
2. [Chương I: Tổng quan bài toán, Khảo sát & Định hướng giải pháp](#2-chương-i-tổng-quan-và-khảo-sát-bài-toán)
3. [Chương II: Phân tích yêu cầu & Thiết kế hệ thống](#3-chương-ii-phân-tích-và-thiết-kế-hệ-thống)
4. [Chương III: Hiện thực hệ thống & Các giải pháp kỹ thuật cốt lõi](#4-chương-iii-hiện-thực-hệ-thống-và-các-tính-năng)
5. [Chương IV: Kiểm thử, Đánh giá chất lượng & Mức độ hoàn thành](#5-chương-iv-kiểm-thử-và-đánh-giá-hệ-thống)
6. [Kết luận, Đánh giá Ưu/Nhược điểm & Hướng phát triển](#6-kết-luận-và-hướng-phát-triển)
7. [Tóm lược toàn bộ Phụ lục (Phụ lục A – F)](#7-tóm-lược-các-phụ-lục)

---

## 1. PHẦN MỞ ĐẦU

### 1.1. Tính cấp thiết của đề tài
* Trong thời đại số, khối lượng công việc, bài tập và dự án cá nhân gia tăng nhanh chóng. Con người dễ rơi vào trạng thái quá tải, quên hạn chót (deadline) và suy giảm năng suất làm việc.
* **Hạn chế của các ứng dụng thương mại hiện nay (Todoist, TickTick, Microsoft To Do):**
  * **Phụ thuộc kết nối Internet & Tài khoản:** Bắt buộc đăng nhập tài khoản đám mây; khi mất mạng dữ liệu bị hạn chế hoặc không thể truy cập.
  * **Rủi ro quyền riêng tư:** Toàn bộ dữ liệu kế hoạch, thông tin công việc, ghi chú nhạy cảm đều tải lên máy chủ của bên thứ ba.
  * **Khóa tính năng trong gói trả phí:** Nhắc nhở nâng cao, thống kê năng suất, widget tương tác, khóa bảo mật mã PIN thường bị tính phí.
* **Giải pháp của đề tài:** Xây dựng ứng dụng theo hướng **Local-first (Ưu tiên cục bộ)**: Dữ liệu lưu trữ 100% trên thiết bị, hoạt động hoàn hảo khi không có mạng, bảo mật bằng mã PIN & sinh trắc học vân tay, người dùng hoàn toàn làm chủ dữ liệu qua sao lưu JSON, kèm các tiện ích nâng cao hoàn toàn miễn phí.

### 1.2. Mục tiêu nghiên cứu
* **Mục tiêu tổng quát:** Xây dựng một ứng dụng Android hoàn chỉnh, ổn định, hoạt động độc lập, có giá trị thực tiễn cao giúp người dùng lập kế hoạch và duy trì năng suất cá nhân.
* **Mục tiêu cụ thể:**
  * Hoàn thành xuất sắc **11 yêu cầu chức năng bắt buộc** của đề tài (CRUD, Deadline, Priority, Alarm, Room DB, PIN Lock, Validation, Recurring, Calendar, Boot Recovery, Backup/Restore JSON qua SAF).
  * Phát triển **6 phân hệ mở rộng cao cấp:** Home Screen Widget, Bento Grid Statistics, Pomodoro Timer (Foreground Service), Gamification (Streak Tracker & 7 Huy hiệu), Trợ lý ảo AI (Google Gemini 2.5 Flash), Phím tắt màn hình chính (App Shortcuts).
* **Phạm vi nền tảng:** Hỗ trợ từ Android 8.0 (API 26) đến Android 14 (API 34) và mới hơn; hoạt động thuần ngoại tuyến (offline) trên một thiết bị cá nhân.

---

## 2. CHƯƠNG I: TỔNG QUAN VÀ KHẢO SÁT BÀI TOÁN

### 2.1. Cấu trúc thông tin của một công việc (Task Model)
* Một công việc trong hệ thống được chuẩn hóa thành một thực thể thông tin gồm 7 trường nghiệp vụ:
  1. **Title (Tiêu đề):** Tên định danh ngắn gọn (trường bắt buộc).
  2. **Description (Mô tả):** Ghi chú chi tiết, hướng dẫn thực hiện, ngữ cảnh công việc.
  3. **DueDate (Ngày hạn):** Mốc ngày hoàn thành (chuẩn hóa về đầu ngày `00:00:00.000` để phục vụ tra cứu lịch và lọc ngày).
  4. **DueTime (Giờ nhắc):** Mốc thời gian chính xác đến từng phút để kích hoạt chuông báo thức.
  5. **Priority (Độ ưu tiên):** 3 mức độ rõ ràng: `HIGH` (Cao - Đỏ), `MEDIUM` (Trung bình - Vàng), `LOW` (Thấp - Xanh).
  6. **Status (Trạng thái):** `PENDING` (Chờ xử lý), `IN_PROGRESS` (Đang làm), `COMPLETED` (Đã xong), `OVERDUE` (Quá hạn).
  7. **Recurrence (Quy tắc lặp):** `NONE`, `DAILY` (Hằng ngày), `WEEKLY` (Hằng tuần), `MONTHLY` (Hằng tháng).

### 2.2. Khảo sát các ứng dụng tương tự
* **Khảo sát 3 sản phẩm lớn:**
  * *Todoist:* Mạnh về phân cấp dự án và nhãn, nhưng không thể khóa PIN cục bộ và tính phí tính năng sao lưu/nhắc nhở.
  * *TickTick:* Tích hợp Pomodoro và Habit tracker nhưng phụ thuộc tài khoản, widget và thống kê nâng cao phải trả phí.
  * *Microsoft To Do:* Đơn giản, miễn phí nhưng phụ thuộc hoàn toàn vào hệ sinh thái tài khoản Microsoft, không hỗ trợ xuất tệp sao lưu cục bộ và không có khóa PIN.
* **Khoảng trống thị trường:** Một ứng dụng Local-first hoàn toàn miễn phí, độc lập, bảo mật cao, có widget tương tác trực tiếp, thống kê năng suất và tích hợp AI hỗ trợ người dùng.

### 2.3. Định hướng giải pháp & Sơ đồ phân rã chức năng (12 nhóm nghiệp vụ)
* **4 Trụ cột giải pháp:** **Độc lập** (Không cần mạng/server) — **An toàn** (Bảo mật PIN/Vân tay) — **Nhanh chóng** (Phản ứng thời gian thực với Room Flow) — **Đầy đủ tiện ích** (Widget, Bento Stats, Pomodoro, AI, Streak).
* **Sơ đồ phân rã 12 nhóm chức năng:**
  1. *Quản lý công việc:* Tạo mới, Xem chi tiết, Chỉnh sửa, Xóa (hộp thoại xác nhận), Đánh dấu hoàn thành, Quick Actions (3-dot menu), Quote Rotator 5s.
  2. *Lọc và sắp xếp:* Lọc theo trạng thái, độ ưu tiên; Sắp xếp theo hạn chót (tăng/giảm dần) và mức độ ưu tiên.
  3. *Lịch biểu:* Lịch tháng 7 cột, event dots chỉ báo ngày có việc, danh sách việc theo ngày chọn.
  4. *Nhắc nhở:* Lập lịch báo chính xác (Exact Alarm), xin quyền Android 13+, khôi phục lịch nhắc khi khởi động lại hoặc đổi múi giờ, thao tác nhanh trên Notification (Hoàn thành / Báo lại).
  5. *Công việc lặp lại:* Chu kỳ hằng ngày, hằng tuần, hằng tháng; tự động tính hạn tiếp theo khi hoàn thành.
  6. *Bảo mật:* Bật/tắt PIN 4 số (SHA-256 + Salt), chống brute-force khóa 30s sau 5 lần sai, mở khóa vân tay (Biometric), tự động hủy vân tay khi đổi/xóa PIN.
  7. *Sao lưu & Phục hồi:* Xuất/nhập JSON qua SAF, kiểm tra toàn vẹn đa lớp, khôi phục nguyên tử trong 1 Transaction.
  8. *Widget màn hình chính:* Xem danh sách việc hôm nay, checkbox hoàn thành trực tiếp trên widget, tự làm mới lúc 00:00.
  9. *Thống kê Bento Grid:* Tỷ lệ hoàn thành tròn, biểu đồ cột năng suất 7 ngày trong tuần, thống kê theo mức ưu tiên.
  10. *Pomodoro Timer:* 25p làm việc / 5p nghỉ, Foreground Service chạy nền độc lập, chuông rung thông báo, thống kê phiên.
  11. *Gamification:* Streak Tracker đếm chuỗi liên tục, tiến độ 7 ngày trong tuần, mở khóa 7 huy hiệu đặc biệt, thông báo bảo vệ chuỗi lúc 20:00.
  12. *Trợ lý ảo AI & Phím tắt:* Xử lý lệnh tự nhiên NLP (`TaskCommandParser`), Chatbot Google Gemini 2.5 Flash, Firebase App Check, Phím tắt Launcher Shortcuts (`shortcuts.xml`).

---

## 3. CHƯƠNG II: PHÂN TÍCH VÀ THIẾT KẾ HỆ THỐNG

### 3.1. Đặc tả yêu cầu chức năng (FR) & Phi chức năng (NFR)
* **Yêu cầu chức năng (FR-01 đến FR-13):**
  * `FR-01`: Quản lý công việc CRUD đầy đủ.
  * `FR-02`: Cấu trúc dữ liệu chi tiết đầy đủ 7 thuộc tính.
  * `FR-03`: Lọc đa tiêu chí và sắp xếp linh hoạt.
  * `FR-04`: Thông báo nhắc nhở chính xác theo thời gian thực (Exact Alarm).
  * `FR-05`: Lưu trữ bền vững với Room Database.
  * `FR-06`: Bảo mật ứng dụng bằng mã PIN băm SHA-256 + Salt.
  * `FR-07`: Kiểm tra tính hợp lệ dữ liệu nhập (Validation) & Xử lý quá hạn.
  * `FR-08`: Quản lý công việc lặp lại định kỳ (Recurring).
  * `FR-09`: Xem công việc theo Lịch biểu trực quan (Calendar).
  * `FR-10`: Trợ lý ảo AI đàm thoại và phân tích câu lệnh tự nhiên (NLP).
  * `FR-11`: Gamification: Đếm chuỗi ngày (Streak) và mở khóa 7 huy hiệu thành tích.
  * `FR-12`: Xác thực sinh trắc học vân tay (AndroidX Biometric).
  * `FR-13`: Phím tắt truy cập nhanh từ màn hình chính (Launcher App Shortcuts).
* **Yêu cầu phi chức năng (NFR-01 đến NFR-08):** Hiệu năng phản hồi <100ms; hoạt động offline 100%; an toàn dữ liệu tuyệt đối; tối ưu hóa pin; giao diện chuẩn Material Design 3; tương thích Android 8.0 – 14+.

### 3.2. Thiết kế Kiến trúc phần mềm (MVVM + Repository Pattern)
* **Tầng View (UI):** Các `Activity`, `Fragment`, `Custom Views`, `Adapters` và `Dialogs`. Sử dụng **ViewBinding** triệt để (loại bỏ `findViewById`). Lắng nghe dữ liệu bất biến từ ViewModel thông qua `StateFlow` / `Flow`.
* **Tầng ViewModel:** `TaskViewModel`, `CalendarViewModel`, `StatsViewModel`, `PomodoroViewModel`, `TaskAssistantViewModel`, `AddEditTaskViewModel`, `BackupViewModel`. Nắm giữ trạng thái giao diện (`UiState`), điều phối các tác vụ bất đồng bộ bằng `viewModelScope` (Coroutines), độc lập hoàn toàn với vòng đời của View.
* **Tầng Data (Single Source of Truth):**
  * `TaskRepository`, `PomodoroRepository`, `BackupRepository`, `PinRepository`.
  * `TaskDao`, `PomodoroDao` định nghĩa các truy vấn SQL.
  * `Room Database` (SQLite), `SharedPreferences` (Lưu trữ PIN Hash & Salt).

### 3.3. Thiết kế Cơ sở dữ liệu Room (Phiên bản 4)
* **Bảng `tasks` (17 cột):** `id` (PK), `title`, `description`, `dueDate` (Long, chuẩn hóa đầu ngày), `dueTime` (Long, mốc giờ nhắc), `priority`, `status`, `recurrenceType`, `createdAt`, `updatedAt`, `completedAt`, `totalFocusTimeMinutes`, `completedPomodoros`, `repeatEndDate`, `repeatLimitCount`, `currentOccurrence`, `isPaused`.
* **Bảng `pomodoro_sessions` (8 cột):** `id` (PK), `taskId` (FK trỏ tới `tasks.id`), `sessionType` (`FOCUS`/`SHORT_BREAK`/`LONG_BREAK`), `durationMinutes`, `startTime`, `endTime`, `isCompleted`, `createdAt`.
  * Ràng buộc: `ON DELETE CASCADE` (xóa task tự động xóa toàn bộ phiên Pomodoro liên quan, không để lại bản ghi rác).
  * Chỉ mục: `INDEX on startTime` giúp tăng tốc độ truy vấn thống kê theo ngày/tuần.
* **Quy tắc Nâng cấp lược đồ (Defensive Migration):** Migration từ v1/v2/v3 lên v4 sử dụng câu lệnh kiểm tra `PRAGMA table_info` trước mỗi lệnh `ALTER TABLE ADD COLUMN`, đảm bảo ứng dụng không bao giờ bị crash do lỗi "duplicate column" và không mất dữ liệu người dùng.

### 3.4. 10 Luồng xử lý chính trong hệ thống (Sequence Flows)
1. **Luồng CRUD công việc:** View gửi dữ liệu -> ViewModel xác thực qua `ValidationHelper` -> Repository thực thi `suspend fun` -> Room DB cập nhật -> Room Flow tự động phát tín hiệu làm mới UI tức thì.
2. **Luồng thông báo nhắc việc:** Tính thời điểm nhắc -> `AlarmScheduler` gọi `AlarmManager.setExactAndAllowWhileIdle()` -> Báo thức kích hoạt -> `AlarmReceiver` -> `NotificationHelper` phát Notification kèm âm thanh, rung, heads-up banner và các nút hành động (Hoàn thành / Snooze 10 phút).
3. **Luồng khôi phục nhắc nhở sau Reboot/Đổi giờ:** Hệ điều hành phát broadcast `ACTION_BOOT_COMPLETED` hoặc `ACTION_TIME_CHANGED` -> `BootReceiver` / `TimeChangeReceiver` chạy nền bất đồng bộ -> Đọc danh sách task chưa hoàn thành -> Lập lịch lại toàn bộ Alarms tự động.
4. **Luồng xác thực mã PIN:** Người dùng nhập PIN -> Hệ thống lấy chuỗi Salt đã lưu -> Tính toán `hash = SHA-256(PIN + Salt)` -> So sánh với chuỗi Hash trong SharedPreferences -> Đúng: mở ứng dụng; Sai 5 lần: khóa tạm thời 30 giây chống dò mã.
5. **Luồng Backup & Restore JSON:** 
   * *Backup:* Đọc tasks + pomodoro_sessions -> Chuyển thành JSON định dạng chuẩn -> Ghi vào URI qua SAF.
   * *Restore:* Đọc JSON -> Bộ thẩm định `BackupValidator` kiểm tra 5 bước (cú pháp, phiên bản, cấu trúc mảng, kiểu dữ liệu, toàn vẹn khóa ngoại) -> Mở Transaction -> Xóa dữ liệu cũ -> Ghi dữ liệu mới -> Commit (*All-or-Nothing*).
6. **Luồng Home Screen Widget:** `AppWidgetProvider` nhận sự kiện cập nhật -> `WidgetTaskListBuilder` truy vấn task hôm nay -> `RemoteViews` hiển thị danh sách; đăng ký `InvalidationTracker.Observer` tự động cập nhật widget ngay khi DB thay đổi; `WidgetMidnightScheduler` tự đổi ngày lúc `00:00`.
7. **Luồng Pomodoro Timer:** Tính thời điểm kết thúc đơn điệu `targetEndElapsedRealtime` -> Khởi động `Foreground Service` -> Duy trì thông báo Notification thường trực -> Mỗi nhịp đồng hồ so sánh với mốc đích (loại bỏ sai số đếm lùi) -> Hết giờ: rung chuông, ghi phiên vào DB trong một transaction.
8. **Luồng Trợ lý ảo AI:** Người dùng nhập câu lệnh -> `TaskCommandParser` bóc tách cấu trúc câu lệnh bằng Regex (Tạo task, Xem hôm nay, Đếm quá hạn) -> Nếu câu hỏi mở, gọi Google Gemini 2.5 Flash qua Firebase Vertex AI -> Trả về câu trả lời và tự động thêm task vào Room DB.
9. **Luồng Gamification (Streak & Badges):** Đánh dấu hoàn thành task -> `StreakCalculator` tính toán chuỗi liên tục dựa trên các mốc ngày UTC -> `SpecialBadgeCalculator` kiểm tra điều kiện 7 huy hiệu -> `StreakReminderScheduler` hẹn giờ phát thông báo nhắc nhở bảo vệ chuỗi lúc 20:00 hằng ngày.
10. **Luồng Sinh trắc học & App Shortcuts:** Khi mở app, `BiometricAuthHelper` gọi `BiometricPrompt` quét vân tay; khi người dùng nhấn shortcut từ màn hình chính, `BaseActivity` chặn hiển thị và kiểm tra trạng thái khóa PIN trước khi điều hướng vào nội dung.

---

## 4. CHƯƠNG III: HIỆN THỰC HỆ THỐNG VÀ CÁC TÍNH NĂNG

### 4.1. Môi trường phát triển & Thư viện sử dụng
* **Android Jetpack:** `androidx.room:room-ktx:2.6.1`, `androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0`, `androidx.navigation:navigation-fragment-ktx:2.7.7`, `androidx.biometric:biometric:1.2.0-alpha05`.
* **Kotlin Coroutines:** `kotlinx-coroutines-android:1.7.3`, `kotlinx-coroutines-core:1.7.3`.
* **Google Firebase AI:** `com.google.firebase:firebase-ai:16.0.0-beta01` (Gemini 2.5 Flash), `com.google.firebase:firebase-appcheck-playintegrity` & `debug`.
* **Giao diện & Tiện ích:** `com.google.android.material:material:1.11.0` (Material 3), `com.google.code.gson:gson:2.10.1`, `com.github.PhilJay:MPAndroidChart:v3.1.0`.

### 4.2. Hiện thực chi tiết từng phân hệ chức năng
* **3.4. Quản lý công việc:** 
  * `TaskListFragment`: Bố cục Bento Card, chia nhóm "Hôm nay", "Sắp tới" và "Đã hoàn thành".
  * `AddEditTaskActivity`: Form nhập liệu thông minh, tích hợp DatePicker, TimePicker, bộ chọn Priority/Recurrence dạng chip.
  * `TaskDetailActivity`: Hiển thị chi tiết toàn diện, tích hợp Quick Actions (Hoàn thành xanh lá / Xóa đỏ) và **Bộ quay danh ngôn động lực 5s (Quote Rotator)**.
* **3.5. Lọc và sắp xếp:** Modal Bottom Sheet cho phép lọc đa tiêu chí (Tất cả, Chưa xong, Đã xong; Ưu tiên Cao/TB/Thấp) và sắp xếp tăng/giảm theo hạn chót hoặc mức ưu tiên.
* **3.6. Lịch biểu (Calendar View):** Lưới lịch tháng tùy biến hiển thị chỉ báo chấm xanh ở các ngày có công việc, danh sách chi tiết các công việc trong ngày được chọn.
* **3.7. Nhắc nhở (Notification):** 
  * Kênh thông báo `task_reminders_channel` có mức độ ưu tiên `IMPORTANCE_HIGH`, âm thanh chuông báo thức và rung.
  * Tương thích hoàn toàn với cơ chế phân quyền runtime `POST_NOTIFICATIONS` trên Android 13+.
* **3.8. Công việc lặp lại (Recurring Tasks):** Thuật toán `RecurrenceHelper` tự động tính toán kỳ hạn kế tiếp khi hoàn thành task lặp (cộng 1 ngày, 1 tuần hoặc 1 tháng) mà không làm mất lịch sử công việc cũ.
* **3.9. Bảo mật PIN Lock & Sinh trắc học (Biometric):**
  * Bàn phím số PIN tùy biến (4 ô tròn), băm mật mã SHA-256 kèm Salt, chống brute-force khóa 30 giây.
  * Tích hợp AndroidX Biometric quét vân tay phần cứng; quy tắc an toàn tự động vô hiệu hóa vân tay khi thay đổi hoặc xóa mã PIN.
* **3.10. Sao lưu và Khôi phục (Backup/Restore):**
  * Xuất tệp JSON qua Storage Access Framework (SAF), tên tệp dạng `taskflow_backup_YYYYMMDD_HHmmss.json`.
  * Bộ thẩm định `BackupValidator` kiểm tra tính hợp lệ toàn diện trước khi ghi đè; khôi phục an toàn trong 1 `@Transaction` duy nhất.
* **3.11. Home Screen Widget:**
  * Widget kích thước linh hoạt hiển thị danh sách công việc hôm nay kèm giờ nhắc.
  * Tương tác trực tiếp: Chạm vào checkbox để đánh dấu hoàn thành ngay trên màn hình chính mà không cần mở ứng dụng; tự động chuyển sang ngày mới lúc 00:00.
* **3.12. Thống kê Bento Grid:**
  * Thẻ tỷ lệ hoàn thành dạng cung tròn xoay (`CircularCompletionRateView`).
  * Biểu đồ cột năng suất 7 ngày trong tuần (`WeeklyProductivityChartView`).
  * Thống kê phân bổ công việc theo mức độ ưu tiên.
* **3.13. Quản lý thời gian tập trung (Pomodoro Timer):**
  * Kỹ thuật Pomodoro chuẩn (25 phút tập trung, 5 phút nghỉ ngắn, 15 phút nghỉ dài).
  * `PomodoroService` (Foreground Service) quản lý đồng hồ chạy nền độc lập; thông báo thường trực có nút Tạm dừng / Bỏ qua / Dừng phiên.
  * Ghi nhận lịch sử phiên vào cơ sở dữ liệu và tích hợp thống kê thời gian tập trung.
* **3.14. Chuỗi ngày & Hệ thống Huy hiệu (Streak & Milestone Badges):**
  * `StreakCalculator` tính chuỗi ngày hoàn thành liên tục (`Current Streak`, `Best Streak`).
  * `SpecialBadgeCalculator` đánh giá mở khóa 7 huy hiệu độc đáo: Early Bird, Night Owl, Weekend Warrior, Pomodoro Master, Century Club, Consistency King, Speed Demon.
  * `StreakReminderReceiver` phát thông báo nhắc nhở bảo vệ chuỗi lúc 20:00 hằng ngày nếu người dùng chưa hoàn thành việc nào.
* **3.15. Trợ lý ảo AI thông minh (AI Task Assistant):**
  * Nút nổi Launcher với tooltip chào mừng thông minh.
  * Khung chat `AiAssistantBottomSheet` với các chip gợi ý thao tác nhanh.
  * `TaskCommandParser` phân tích lệnh tự nhiên tiếng Việt/Anh; kết nối Google Gemini 2.5 Flash API qua Firebase Vertex AI SDK; bảo vệ qua Firebase App Check.
* **3.16. Phím tắt màn hình chính (Launcher App Shortcuts):**
  * Tệp cấu hình `shortcuts.xml` cung cấp các lối tắt: Tạo việc mới, Xem việc hôm nay, Bật Pomodoro khi nhấn giữ biểu tượng ứng dụng.
  * `BaseActivity` đảm bảo người dùng phải mở khóa PIN/Vân tay trước khi truy cập nội dung từ Shortcut.

### 4.3. 8 Giải pháp kỹ thuật nổi bật
1. **Bảo mật mã PIN bằng SHA-256 kèm Salt ngẫu nhiên:** Đảm bảo không thể suy ngược mã PIN kể cả khi trích xuất được cơ sở dữ liệu lưu trữ.
2. **Quản lý tệp qua Storage Access Framework (SAF):** Tuân thủ chính sách bảo mật của Google, không yêu cầu quyền nguy hiểm truy cập toàn bộ bộ nhớ.
3. **Lập lịch nhắc chính xác (Exact Alarm) & Tự phục hồi sau khởi động lại:** Kết hợp `AlarmManager`, `BootReceiver` và `TimeChangeReceiver`.
4. **Kiến trúc MVVM kết hợp Repository Pattern:** Đảm bảo nguyên lý Single Source of Truth, phân tách rõ ràng trách nhiệm giữa các tầng.
5. **Cơ chế phản ứng Reactive với Room Database & Kotlin Flow:** Dữ liệu tự động phát luồng cập nhật toàn bộ màn hình và widget mà không cần tải lại thủ công.
6. **Nâng cấp cơ sở dữ liệu phòng thủ (Defensive Migration):** Kiểm tra sự tồn tại của cột trước khi thêm mới, bảo vệ 100% dữ liệu người dùng khi nâng cấp app.
7. **Khôi phục dữ liệu nguyên tử theo mô hình Transaction:** Toàn bộ quá trình phục hồi từ JSON được bọc trong một transaction duy nhất (*All-or-Nothing*).
8. **Máy trạng thái Pomodoro thuần Kotlin:** Tách biệt hoàn toàn khỏi Android Framework, sử dụng mốc thời gian đích đơn điệu (`elapsedRealtime()`) triệt tiêu sai số đếm lùi và cho phép kiểm thử tự động trên JVM.

### 4.4. Bảng đối chiếu 11 yêu cầu bàn giao của đề tài
* **Tất cả 11/11 yêu cầu cốt lõi và 6 phân hệ mở rộng đều ĐẠT 100% tiêu chí kỹ thuật và nghiệp vụ.**

---

## 5. CHƯƠNG IV: KIỂM THỬ VÀ ĐÁNH GIÁ HỆ THỐNG

### 5.1. Môi trường & Phương pháp kiểm thử
* **Môi trường:** Thiết bị thật (Xiaomi Redmi Note 11 - Android 13, Samsung Galaxy S21 - Android 14) và Máy ảo Android Emulator (Pixel 6 Pro - Android 14 API 34).
* **Phương pháp:** Kết hợp Kiểm thử hộp đen (Black-box Testing) trên thiết bị và Kiểm thử tự động (Unit Testing) trên JVM.

### 5.2. Kết quả kiểm thử chi tiết
* **Kiểm thử hộp đen (Black-box Testing):**
  * Nhóm 1: Quản lý công việc (TC01–TC06) -> 6/6 Đạt (100%).
  * Nhóm 2: Thông báo và khôi phục nhắc nhở (TC07–TC10) -> 4/4 Đạt (100%).
  * Nhóm 3: Bảo mật mã PIN và Sinh trắc học (TC11–TC13) -> 3/3 Đạt (100%).
  * Nhóm 4: Sao lưu và khôi phục dữ liệu JSON (TC14–TC18) -> 5/5 Đạt (100%).
  * Nhóm 5: Widget, Thống kê và Pomodoro (TC19–TC21) -> 3/3 Đạt (100%).
  * Nhóm kiểm thử biên và chịu lỗi (13 ca kiểm thử bổ sung) -> 13/13 Đạt (100%).
  * **Tổng kết: 34/34 ca kiểm thử ĐẠT yêu cầu (Tỷ lệ thành công 100%).**
* **Kiểm thử tự động trên JVM (Unit Tests):**
  * Xây dựng **15 lớp kiểm thử** với hơn **90 test cases** bao phủ: `PomodoroTimerEngineTest`, `StatsCalculationTest`, `BackupValidatorTest`, `TaskCommandParserTest`, `PinSecurityTest`, `StreakCalculatorTest`, `DateTimeUtilsTest`,...
  * Toàn bộ test case chạy hoàn tất trong **< 5 giây trên JVM** với tỷ lệ đỗ **100%**.
* **Kiểm chứng ngoài thiết bị:** Kiểm chứng tính toàn vẹn cơ sở dữ liệu SQLite, kiểm chứng cơ chế Migration v1->v4, kiểm tra tính đúng đắn của thuật toán băm SHA-256 + Salt.

---

## 6. KẾT LUẬN VÀ HƯỚNG PHÁT TRIỂN

### 6.1. Kết quả đạt được
* **Về chức năng:** Hoàn thành trọn vẹn 100% yêu cầu đề bài và phát triển thêm các phân hệ tiện ích mở rộng cao cấp (Widget, Pomodoro, Bento Stats, Gamification Streak/Badges, AI Assistant, App Shortcuts).
* **Về kiến trúc & Chất lượng mã nguồn:** Áp dụng chuẩn mực kiến trúc MVVM, Clean Architecture, Single Source of Truth, Room Flow reactive, quản lý mã nguồn phân nhánh Git bài bản.
* **Về giá trị thực tiễn:** Ứng dụng mang lại trải nghiệm mượt mà, độc lập, an toàn, hỗ trợ người dùng xây dựng thói quen và quản lý thời gian hiệu quả.

### 6.2. Hạn chế hiện tại
1. Chưa hỗ trợ đồng bộ dữ liệu tự động qua đám mây giữa nhiều thiết bị.
2. Chưa có tính năng làm việc nhóm, chia sẻ công việc hoặc gán việc cho nhiều người.

### 6.3. Hướng phát triển trong tương lai
* **Ngắn hạn:** Đính kèm tệp đa phương tiện (hình ảnh, ghi âm giọng nói); lọc theo thẻ tùy chỉnh (Tags/Labels).
* **Trung hạn:** Đồng bộ đám mây tùy chọn qua Google Drive / Firebase Firestore với thuật toán giải quyết xung đột dữ liệu (*Conflict Resolution*).
* **Dài hạn:** Phát triển ứng dụng đồng hành trên đồng hồ thông minh (Wear OS Companion App).

---

## 7. TÓM LƯỢC CÁC PHỤ LỤC (PHỤ LỤC A – F)

* **Phụ lục A (Cấu trúc Source Code):** Danh sách đầy đủ hơn 55 tệp mã nguồn Kotlin được tổ chức khoa học theo các package nghiệp vụ (`ai/`, `data/`, `pomodoro/`, `receiver/`, `security/`, `ui/`, `util/`, `viewmodel/`, `widget/`) và cấu trúc thư mục tài nguyên `res/`.
* **Phụ lục B (Cấu trúc Database chi tiết):** Lược đồ tạo bảng Room version 4 (`tasks`, `pomodoro_sessions`), các câu lệnh truy vấn DAO tiêu biểu, mã nguồn các bước migration v1 -> v4 có kiểm tra cấu trúc cột, quy tắc toàn vẹn dữ liệu.
* **Phụ lục C (Bộ Test Case chi tiết):** Bảng chi tiết 34 ca kiểm thử hộp đen hoàn chỉnh (Mã TC, Mục tiêu, Tiền điều kiện, Các bước thực hiện, Kết quả mong đợi, Kết quả thực tế, Đánh giá).
* **Phụ lục D (Các đoạn Source Code quan trọng):** Trích lục 12 đoạn mã nguồn cốt lõi của dự án:
  * `D.1`: Lập lịch nhắc việc với cơ chế dự phòng (`AlarmScheduler.kt`).
  * `D.2`: Defensive Migration kiểm tra sự tồn tại của cột trước khi thêm (`AppDatabase.kt`).
  * `D.3`: Ghi phiên tập trung Pomodoro và cộng dồn số liệu trong một transaction.
  * `D.4`: Máy trạng thái Pomodoro — nguồn thời gian đích đơn điệu (`PomodoroTimerEngine.kt`).
  * `D.5`: Xuất tệp sao lưu và khôi phục trong một `@Transaction` (`BackupRepository.kt`).
  * `D.6`: Bộ kiểm tra tính hợp lệ đa tầng của tệp sao lưu (`BackupValidator.kt`).
  * `D.7`: Thuật toán băm mã PIN bằng SHA-256 kèm Salt ngẫu nhiên (`PinRepositoryImpl.kt`).
  * `D.8`: Foreground Service quản lý đồng hồ đếm ngược Pomodoro (`PomodoroService.kt`).
  * `D.9`: Bộ phân tích cú pháp biểu thức chính quy xử lý lệnh tự nhiên (`TaskCommandParser.kt`).
  * `D.10`: Thuật toán kiểm tra điều kiện mở khóa 7 huy hiệu đặc biệt (`SpecialBadgeCalculator.kt`).
  * `D.11`: Lớp tiện ích quản lý xác thực sinh trắc học vân tay an toàn (`BiometricAuthHelper.kt`).
  * `D.12`: Cấu hình phím tắt ứng dụng tĩnh trên màn hình chính (`shortcuts.xml`).
* **Phụ lục E (Hướng dẫn sử dụng ứng dụng):** Cẩm nang hướng dẫn người dùng gồm 12 mục chi tiết từ cài đặt, tạo việc, quản lý lịch, khóa PIN, sao lưu JSON, kéo widget, chạy Pomodoro, trò chuyện với Trợ lý AI, theo dõi Chuỗi ngày và bảng xử lý 6 sự cố thường gặp.
* **Phụ lục F (Đường dẫn dự án):** Kho lưu trữ mã nguồn GitHub và tài liệu liên quan của đề tài.
