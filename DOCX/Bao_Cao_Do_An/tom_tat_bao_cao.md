# TỔNG HỢP & TÓM TẮT TOÀN DIỆN BÁO CÁO ĐỒ ÁN
# ĐỀ TÀI: ỨNG DỤNG QUẢN LÝ CÔNG VIỆC CÁ NHÂN (TASK MANAGEMENT APPLICATION - TASKFLOW)

---

> [!NOTE]
> **Tài liệu tham chiếu chi tiết:** Toàn bộ nội dung báo cáo hoàn chỉnh (hơn 4.300 dòng) kèm 100% mã nguồn mẫu, bảng biểu, quy trình thiết kế, danh mục ca kiểm thử và phụ lục hướng dẫn được lưu trữ tại: [bao_cao.md](bao_cao.md).

---

## 🏛️ THÔNG TIN CHUNG
* **Tên đề tài:** Báo cáo đồ án kết thúc học phần — Ứng dụng Quản lý công việc (*Task Management Application*)
* **Môn học:** Lập trình thiết bị di động (Mã lớp học phần: `012012103402`)
* **Đơn vị đào tạo:** Khoa Công nghệ Thông tin — Trường Đại học Giao thông Vận tải TP. Hồ Chí Minh
* **Giảng viên hướng dẫn:** ThS. Mai Ngọc Châu
* **Danh sách sinh viên tham gia:**
  1. **Trần Văn Thái** — MSSV: `051206002315` *(Trưởng nhóm)*
  2. **Nguyễn Lê Huy Tâm** — MSSV: `056206011188`
  3. **Nguyễn Ngọc Gia Bảo** — MSSV: `079206008279`
  4. **Trần Văn Ngọc Thắng** — MSSV: `046206001641`
  5. **Huỳnh Đình Chấn** — MSSV: `077206002307`
* **Môi trường & Nền tảng:** Android Studio Ladybug/Koala, Kotlin 100%, Min SDK 26 (Android 8.0), Target SDK 34 (Android 14+), Gradle Version Catalog (`libs.versions.toml`).

---

## 📑 MỤC LỤC TÓM TẮT CHI TIẾT
1. [Phần Mở đầu: Tính cấp thiết, Mục tiêu, Đối tượng & Phạm vi nghiên cứu](#1-phần-mở-đầu)
2. [Chương I: Giới thiệu và Tổng quan về đề tài](#2-chương-i-giới-thiệu-và-tổng-quan-về-đề-tài)
3. [Chương II: Phân tích và Thiết kế hệ thống](#3-chương-ii-phân-tích-và-thiết-kế-hệ-thống)
4. [Chương III: Hiện thực hệ thống và Các tính năng](#4-chương-iii-hiện-thực-hệ-thống-và-các-tính-năng)
5. [Chương IV: Kiểm thử và Đánh giá hệ thống](#5-chương-iv-kiểm-thử-và-đánh-giá-hệ-thống)
6. [Kết luận và Hướng phát triển](#6-kết-luận-và-hướng-phát-triển)
7. [Tóm lược toàn bộ Phụ lục (Phụ lục A – F)](#7-tóm-lược-toàn-bộ-phụ-lục-phụ-lục-a--f)

---

## 1. PHẦN MỞ ĐẦU

### 1.1. Tính cấp thiết của đề tài
* Trong kỷ nguyên số và nhịp sống hiện đại, khối lượng công việc, học tập cá nhân ngày càng gia tăng. Người dùng dễ rơi vào tình trạng quá tải, quên hạn chót (deadline) và giảm sút năng suất.
* **Hạn chế của các ứng dụng thương mại hiện nay (Todoist, TickTick, Microsoft To Do):**
  * **Phụ thuộc kết nối Internet & Tài khoản đám mây:** Bắt buộc đăng nhập; khi mất mạng dữ liệu bị hạn chế hoặc không truy cập được.
  * **Rủi ro về quyền riêng tư:** Dữ liệu cá nhân, kế hoạch bảo mật và ghi chú công việc bị tải lên máy chủ bên thứ ba.
  * **Khóa tính năng trong gói trả phí (Paywall):** Nhắc nhở nâng cao, widget tương tác, thống kê chuyên sâu, khóa mã PIN thường bị tính phí.
* **Giải pháp của đề tài:** Xây dựng ứng dụng **Local-first (Ưu tiên cục bộ)**: Hoạt động ngoại tuyến 100%, bảo mật tuyệt đối với mã PIN băm SHA-256 + Salt & Sinh trắc học vân tay, làm chủ dữ liệu qua sao lưu JSON, kèm đầy đủ tiện ích nâng cao miễn phí.

### 1.2. Mục tiêu nghiên cứu
* **Mục tiêu tổng quát:** Xây dựng ứng dụng Android quản lý công việc cá nhân hoàn chỉnh, ổn định, độc lập, bảo mật cao và tối ưu trải nghiệm người dùng.
* **Mục tiêu cụ thể:**
  * Hoàn thành xuất sắc **11 yêu cầu chức năng bắt buộc** (CRUD, Deadline, Priority, Alarm, Room DB, PIN Lock, Validation, Recurring, Calendar, Boot Recovery, Backup/Restore JSON qua SAF).
  * Phát triển **4 phân hệ tiện ích mở rộng cao cấp:** Home Screen Widget, Bento Grid Statistics Dashboard, Pomodoro Timer (Foreground Service), Gamification (Streak Tracker & Milestone Badges), Trợ lý ảo AI và Phím tắt màn hình chính.
* **Phạm vi nền tảng:** Android 8.0 (API 26) trở lên đến Android 14+ (API 34); hoạt động hoàn toàn ngoại tuyến trên thiết bị người dùng.

---

## 2. CHƯƠNG I: GIỚI THIỆU VÀ TỔNG QUAN VỀ ĐỀ TÀI

### 2.1. Cấu trúc thông tin của một công việc (Task Model)
* Mô hình công việc gồm 7 trường nghiệp vụ cốt lõi:
  1. **Title (Tiêu đề):** Tên định danh công việc (bắt buộc).
  2. **Description (Mô tả):** Ghi chú chi tiết, hướng dẫn thực hiện.
  3. **DueDate (Ngày hạn):** Mốc ngày hoàn thành (chuẩn hóa về `00:00:00.000` phục vụ lọc và hiển thị lịch).
  4. **DueTime (Giờ nhắc):** Mốc thời gian chính xác đến từng phút để kích hoạt thông báo chuông nhắc việc.
  5. **Priority (Độ ưu tiên):** 3 mức độ rõ ràng: `HIGH` (Cao - Đỏ), `MEDIUM` (Trung bình - Vàng), `LOW` (Thấp - Xanh).
  6. **Status (Trạng thái):** `PENDING` (Chờ xử lý), `IN_PROGRESS` (Đang làm), `COMPLETED` (Đã xong), `OVERDUE` (Quá hạn).
  7. **Recurrence (Quy tắc lặp):** `NONE`, `DAILY` (Hằng ngày), `WEEKLY` (Hằng tuần), `MONTHLY` (Hằng tháng), `YEARLY` (Hằng năm).

### 2.2. Khảo sát các ứng dụng tương tự
* **So sánh với Todoist, TickTick, Microsoft To Do:**
  * *Todoist:* Quản lý dự án tốt nhưng không hỗ trợ khóa PIN cục bộ, tính phí nhắc nhở/sao lưu.
  * *TickTick:* Có Pomodoro và Habit tracker nhưng phụ thuộc tài khoản, tính năng widget/thống kê nâng cao phải trả phí.
  * *Microsoft To Do:* Miễn phí nhưng gắn chặt với hệ sinh thái tài khoản Microsoft, không hỗ trợ sao lưu tệp JSON cục bộ, không có khóa PIN.
* **Định vị của TaskFlow:** Ứng dụng Local-first độc lập, tốc độ phản hồi tức thì, bảo vệ quyền riêng tư 100%, tích hợp sẵn Widget, Pomodoro, Thống kê Bento Grid và Trợ lý ảo AI.

### 2.3. Định hướng giải pháp & Sơ đồ phân rã chức năng (12 nhóm nghiệp vụ)
* **4 Trụ cột giải pháp:** **Độc lập** (Không cần mạng/server) — **An toàn** (Bảo mật PIN/Vân tay) — **Nhanh chóng** (Room reactive Flow) — **Đầy đủ tiện ích** (Widget, Stats, Pomodoro, AI).
* **12 Nhóm chức năng phân rã:**
  1. *Quản lý công việc:* Thêm, xem chi tiết, sửa, xóa (hộp thoại xác nhận), đánh dấu hoàn thành, Quick Actions, Quote Rotator 5s.
  2. *Lọc & Sắp xếp:* Lọc theo trạng thái, độ ưu tiên; Sắp xếp theo hạn chót (tăng/giảm dần) và mức ưu tiên.
  3. *Lịch biểu:* Lưới lịch tháng 7 cột, chấm màu chỉ báo ngày có việc, danh sách việc theo ngày chọn.
  4. *Nhắc nhở:* Exact Alarm, phân quyền Android 13+, khôi phục tự động sau Reboot hoặc đổi múi giờ.
  5. *Công việc lặp:* Chu kỳ ngày/tuần/tháng/năm, tự động tính hạn tiếp theo khi hoàn thành.
  6. *Bảo mật:* Mã PIN 4 số (SHA-256 + Salt), chống brute-force khóa 30s sau 5 lần sai, mở khóa vân tay (Biometric).
  7. *Sao lưu & Khôi phục:* Xuất/nhập JSON qua SAF, thẩm định toàn vẹn 5 bước, khôi phục nguyên tử trong 1 Transaction.
  8. *Home Screen Widget:* Xem danh sách việc hôm nay, checkbox hoàn thành trực tiếp trên widget, tự làm mới lúc 00:00.
  9. *Thống kê Bento Grid:* Tỷ lệ hoàn thành xoay vòng, biểu đồ cột năng suất 7 ngày, phân bổ độ ưu tiên.
  10. *Pomodoro Timer:* 25p làm việc / 5p nghỉ, Foreground Service chạy nền độc lập, chuông rung thông báo, thống kê phiên.
  11. *Gamification:* Streak Tracker đếm chuỗi ngày liên tục, mở khóa bộ huy hiệu danh hiệu, nhắc nhở bảo vệ chuỗi lúc 20:00.
  12. *Trợ lý ảo AI & Phím tắt:* Xử lý câu lệnh tự nhiên (NLP Regex), gọi Gemini 2.5 Flash qua Firebase, Launcher App Shortcuts.

---

## 3. CHƯƠNG II: PHÂN TÍCH VÀ THIẾT KẾ HỆ THỐNG

### 3.1. Đặc tả yêu cầu hệ thống
* **Yêu cầu chức năng (FR-01 đến FR-13):**
  * `FR-01`: Quản lý công việc CRUD đầy đủ vòng đời.
  * `FR-02`: Lưu trữ đầy đủ 7 thuộc tính dữ liệu nghiệp vụ.
  * `FR-03`: Lọc đa tiêu chí kết hợp và sắp xếp linh hoạt.
  * `FR-04`: Thông báo nhắc nhở chính xác theo thời gian thực (Exact Alarm).
  * `FR-05`: Lưu trữ cục bộ bền vững với Room Database.
  * `FR-06`: Bảo mật ứng dụng bằng mã PIN băm SHA-256 + Salt.
  * `FR-07`: Kiểm tra dữ liệu hợp lệ (Validation) & Xử lý trạng thái rỗng, quá hạn.
  * `FR-08`: Quản lý công việc lặp lại định kỳ (Recurring Tasks).
  * `FR-09`: Hiển thị công việc theo Lịch biểu trực quan (Calendar View).
  * `FR-10`: Khôi phục tự động lịch nhắc sau Reboot hoặc đổi múi giờ.
  * `FR-11`: Sao lưu và Khôi phục dữ liệu qua tệp JSON an toàn.
  * `FR-12`: Tiện ích mở rộng: Widget màn hình chính, Bento Statistics, Pomodoro Timer.
  * `FR-13`: Tiện ích nâng cao: Gamification Streak, Sinh trắc học vân tay, AI Assistant, App Shortcuts.
* **Yêu cầu phi chức năng (NFR-01 đến NFR-08):** Tốc độ phản hồi UI < 100ms; hoạt động ngoại tuyến 100%; bảo mật dữ liệu an toàn; tối ưu pin; giao diện Material Design 3; tương thích Android 8.0 đến Android 14+.

### 3.2. Thiết kế Kiến trúc phần mềm (MVVM + Repository Pattern)
* **Tầng View (UI):** `Activity`, `Fragment`, `Custom Views`, `Adapters` và `Dialogs`. Sử dụng **ViewBinding** triệt để (loại bỏ `findViewById`). Lắng nghe luồng dữ liệu bất biến từ ViewModel thông qua `StateFlow` / `Flow`.
* **Tầng ViewModel:** `TaskViewModel`, `CalendarViewModel`, `StatsViewModel`, `PomodoroViewModel`, `TaskAssistantViewModel`, `AddEditTaskViewModel`, `BackupViewModel`. Quản lý `UiState`, thực thi tác vụ nền qua `viewModelScope` (Coroutines), hoàn toàn độc lập với vòng đời View.
* **Tầng Data (Single Source of Truth):**
  * `TaskRepository`, `PomodoroRepository`, `BackupRepository`, `PinRepository`.
  * `TaskDao`, `PomodoroDao` định nghĩa các câu lệnh truy vấn SQLite tối ưu.
  * `Room Database` lưu trữ dữ liệu chính, `SharedPreferences` lưu trữ chuỗi PIN Hash & Salt.

### 3.3. Thiết kế Cơ sở dữ liệu Room (Phiên bản 4)
* **Bảng `tasks` (17 cột):** `id` (PK, AUTOINCREMENT), `title`, `description`, `dueDate` (Long, chuẩn hóa đầu ngày), `dueTime` (Long, mốc giờ nhắc), `priority`, `status`, `recurrenceType`, `createdAt`, `updatedAt`, `completedAt`, `totalFocusTimeMinutes`, `completedPomodoros`, `repeatEndDate`, `repeatLimitCount`, `currentOccurrence`, `isPaused`.
* **Bảng `pomodoro_sessions` (8 cột):** `id` (PK), `taskId` (FK trỏ tới `tasks.id`), `sessionType` (`FOCUS`/`SHORT_BREAK`/`LONG_BREAK`), `durationMinutes`, `startTime`, `endTime`, `isCompleted`, `createdAt`.
  * *Ràng buộc:* `ON DELETE CASCADE` (xóa task tự động xóa sạch các phiên Pomodoro liên quan, loại bỏ bản ghi rác).
  * *Chỉ mục:* `INDEX on startTime` giúp tăng tốc truy vấn thống kê theo ngày/tuần.
* **Quy tắc Nâng cấp lược đồ (Defensive Migration):** Migration từ v1/v2/v3 lên v4 sử dụng câu lệnh kiểm tra `PRAGMA table_info` trước mỗi lệnh `ALTER TABLE ADD COLUMN`, đảm bảo không bao giờ crash do "duplicate column" và giữ nguyên 100% dữ liệu người dùng.

### 3.4. 10 Luồng xử lý chính trong hệ thống (Sequence Flows)
1. **Luồng CRUD công việc:** UI $\rightarrow$ ViewModel xác thực $\rightarrow$ Repository thực thi $\rightarrow$ Room DB cập nhật $\rightarrow$ Room Flow phát tín hiệu làm mới UI tức thì.
2. **Luồng thông báo nhắc việc:** `AlarmScheduler` đặt lịch `AlarmManager.setExactAndAllowWhileIdle()` $\rightarrow$ `AlarmReceiver` $\rightarrow$ `NotificationHelper` phát Notification kèm chuông, rung và Action buttons (Hoàn thành / Báo lại 10 phút).
3. **Luồng khôi phục nhắc nhở sau Reboot/Đổi giờ:** Hệ thống phát broadcast `ACTION_BOOT_COMPLETED` / `ACTION_TIME_CHANGED` $\rightarrow$ `BootReceiver` / `TimeChangeReceiver` chạy nền $\rightarrow$ Đọc task chưa xong $\rightarrow$ Đặt lại toàn bộ Alarms tự động.
4. **Luồng xác thực mã PIN:** Người dùng nhập PIN $\rightarrow$ Lấy Salt $\rightarrow$ Tính `SHA-256(PIN + Salt)` $\rightarrow$ So sánh Hash đã lưu $\rightarrow$ Khớp: mở app; Sai 5 lần: khóa 30 giây chống brute-force.
5. **Luồng Backup & Restore JSON:**
   * *Backup:* Đọc tasks + pomodoro_sessions $\rightarrow$ Chuyển thành JSON $\rightarrow$ Ghi vào URI qua SAF.
   * *Restore:* Đọc JSON $\rightarrow$ `BackupValidator` thẩm định 5 bước $\rightarrow$ Mở `@Transaction` $\rightarrow$ Xóa cũ, ghi mới $\rightarrow$ Commit (*All-or-Nothing*).
6. **Luồng Home Screen Widget:** `AppWidgetProvider` nhận cập nhật $\rightarrow$ `WidgetTaskListBuilder` truy vấn task hôm nay $\rightarrow$ `RemoteViews` hiển thị; `InvalidationTracker.Observer` tự động cập nhật khi DB thay đổi; `WidgetMidnightScheduler` tự đổi ngày lúc 00:00.
7. **Luồng Pomodoro Timer:** Tính thời điểm kết thúc đơn điệu `elapsedRealtime()` $\rightarrow$ Khởi động `Foreground Service` $\rightarrow$ Duy trì Notification thường trực $\rightarrow$ Hết giờ: rung chuông, ghi nhận phiên vào DB trong một transaction.
8. **Luồng Trợ lý ảo AI:** Người dùng nhập lệnh $\rightarrow$ `TaskCommandParser` trích xuất thông tin bằng Regex $\rightarrow$ Nếu câu hỏi mở, gọi Gemini 2.5 Flash qua Firebase $\rightarrow$ Trả kết quả và tự thêm task vào DB.
9. **Luồng Gamification (Streak & Badges):** Hoàn thành task $\rightarrow$ `StreakCalculator` tính toán chuỗi ngày liên tục $\rightarrow$ `SpecialBadgeCalculator` kiểm tra điều kiện mở khóa huy hiệu $\rightarrow$ `StreakReminderScheduler` nhắc bảo vệ chuỗi lúc 20:00 hằng ngày.
10. **Luồng Sinh trắc học & App Shortcuts:** Khi mở app, `BiometricAuthHelper` gọi `BiometricPrompt` quét vân tay; khi mở từ Shortcut màn hình chính, `BaseActivity` chặn và kiểm tra mã PIN trước khi hiển thị nội dung.

---

## 4. CHƯƠNG III: HIỆN THỰC HỆ THỐNG VÀ CÁC TÍNH NĂNG

### 4.1. Môi trường phát triển & Thư viện sử dụng
* **Android Jetpack:** `androidx.room:room-ktx:2.6.1`, `androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0`, `androidx.navigation:navigation-fragment-ktx:2.7.7`, `androidx.biometric:biometric:1.2.0-alpha05`.
* **Kotlin Coroutines:** `kotlinx-coroutines-android:1.7.3`, `kotlinx-coroutines-core:1.7.3`.
* **Google Firebase AI:** `com.google.firebase:firebase-ai:16.0.0-beta01` (Gemini 2.5 Flash), `com.google.firebase:firebase-appcheck-playintegrity` & `debug`.
* **Giao diện & Tiện ích:** `com.google.android.material:material:1.11.0` (Material 3), `com.google.code.gson:gson:2.10.1`, `com.github.PhilJay:MPAndroidChart:v3.1.0`.

### 4.2. Hiện thực chi tiết từng phân hệ chức năng
* **Quản lý công việc:** `TaskListFragment` bố cục Bento Card (Hôm nay, Sắp tới, Đã xong); `AddEditTaskActivity` form nhập liệu thông minh (DatePicker, TimePicker, Chips); `TaskDetailActivity` chi tiết toàn diện kèm Quick Actions và Quote Rotator 5s.
* **Lọc và sắp xếp:** Modal Bottom Sheet lọc đa chiều (Trạng thái, Độ ưu tiên) và sắp xếp tăng/giảm theo hạn chót hoặc mức ưu tiên.
* **Lịch biểu (Calendar View):** Lưới lịch tháng 7 cột tùy biến với chấm màu chỉ báo ngày có công việc, danh sách việc theo ngày chọn.
* **Nhắc nhở (Notification):** Kênh `task_reminders_channel` độ ưu tiên cao, âm thanh và rung, tương thích quyền runtime `POST_NOTIFICATIONS` trên Android 13+.
* **Công việc lặp lại:** Thuật toán `RecurrenceHelper` tự tính toán kỳ hạn kế tiếp (ngày/tuần/tháng/năm) khi hoàn thành công việc.
* **Bảo mật PIN Lock & Sinh trắc học:** Bàn phím số 4 ô tròn, băm SHA-256 + Salt, khóa tạm thời 30 giây khi nhập sai 5 lần, tích hợp vân tay `BiometricPrompt`.
* **Sao lưu và Khôi phục:** Xuất tệp JSON qua Storage Access Framework (SAF), thẩm định tính hợp lệ toàn diện với `BackupValidator`, khôi phục an toàn trong 1 transaction duy nhất.
* **Home Screen Widget:** Danh sách công việc hôm nay, checkbox hoàn thành trực tiếp trên màn hình chính, tự chuyển ngày lúc 00:00.
* **Thống kê Bento Grid:** Thẻ tỷ lệ hoàn thành dạng cung tròn xoay (`CircularCompletionRateView`), biểu đồ cột năng suất 7 ngày (`WeeklyProductivityChartView`), phân bổ mức độ ưu tiên.
* **Pomodoro Timer:** Chu kỳ chuẩn 25p làm việc / 5p nghỉ, `PomodoroService` Foreground Service chạy nền độc lập, lưu lịch sử phiên vào DB và tích hợp thống kê thời gian tập trung.
* **Chuỗi ngày & Hệ thống Huy hiệu:** Đếm chuỗi ngày (`Current Streak`, `Best Streak`), mở khóa bộ huy hiệu danh hiệu (Early Bird, Night Owl, Weekend Warrior, Pomodoro Master, Century Club, Consistency King, Speed Demon), nhắc nhở bảo vệ chuỗi lúc 20:00.
* **Trợ lý ảo AI & Phím tắt:** Phân tích lệnh tự nhiên qua Regex, kết nối Gemini 2.5 Flash qua Firebase Vertex AI SDK; Launcher App Shortcuts truy cập nhanh Tạo việc / Xem hôm nay từ màn hình chính.

### 4.3. 8 Giải pháp kỹ thuật cốt lõi
1. **Bảo mật PIN bằng SHA-256 kèm Salt ngẫu nhiên:** Không thể suy ngược mật mã kể cả khi trích xuất cơ sở dữ liệu.
2. **Quản lý tệp qua Storage Access Framework (SAF):** Tuân thủ chính sách bảo mật Google, không xin quyền nguy hiểm truy cập toàn bộ bộ nhớ.
3. **Hẹn giờ chính xác (Exact Alarm) & Tự khôi phục:** Kết hợp `AlarmManager`, `BootReceiver` và `TimeChangeReceiver`.
4. **Kiến trúc MVVM kết hợp Repository:** Phân tách rõ ràng trách nhiệm, đảm bảo Single Source of Truth.
5. **Cơ chế phản ứng Reactive với Room & Flow:** Dữ liệu tự động phát luồng cập nhật toàn bộ UI và Widget ngay khi thay đổi.
6. **Nâng cấp cơ sở dữ liệu phòng thủ (Defensive Migration):** Kiểm tra cấu trúc cột trước khi thêm mới, bảo vệ 100% dữ liệu khi nâng cấp ứng dụng.
7. **Khôi phục dữ liệu nguyên tử theo mô hình Transaction:** Toàn bộ quá trình phục hồi từ JSON được bọc trong một transaction duy nhất (*All-or-Nothing*).
8. **Máy trạng thái Pomodoro thuần Kotlin:** Tách biệt khỏi Android SDK, sử dụng mốc thời gian đích đơn điệu `elapsedRealtime()`, triệt tiêu sai số đếm lùi và cho phép kiểm thử tự động trên JVM.

### 4.4. Đối chiếu yêu cầu bàn giao
* **11/11 yêu cầu cốt lõi và toàn bộ các hạng mục mở rộng đều ĐẠT 100% tiêu chí kỹ thuật và nghiệp vụ.**

---

## 5. CHƯƠNG IV: KIỂM THỬ VÀ ĐÁNH GIÁ HỆ THỐNG

### 5.1. Môi trường & Phương pháp kiểm thử
* **Môi trường:** Thiết bị thật (Xiaomi Redmi Note 11 - Android 13, Samsung Galaxy S21 - Android 14) và Máy ảo Android Emulator (Pixel 6 Pro - Android 14 API 34).
* **Phương pháp:** Kết hợp Kiểm thử hộp đen (Black-box Testing) trên thiết bị và Kiểm thử tự động (Unit Testing) trên JVM.

### 5.2. Kết quả kiểm thử chi tiết
* **Kiểm thử hộp đen (Black-box Testing):**
  * Thiết kế và thực thi **50 ca kiểm thử hoàn chỉnh (TC01 đến TC50)** phân chia thành 8 nhóm chức năng:
    1. *Nhóm 1 — Quản lý công việc và kiểm tra dữ liệu:* TC01 – TC09 (9/9 Đạt).
    2. *Nhóm 2 — Công việc lặp lại:* TC10 – TC16 (7/7 Đạt).
    3. *Nhóm 3 — Tìm kiếm, lọc và sắp xếp:* TC17 – TC23 (7/7 Đạt).
    4. *Nhóm 4 — Nhắc việc và thông báo:* TC24 – TC32 (9/9 Đạt).
    5. *Nhóm 5 — Bảo mật PIN:* TC33 – TC40 (8/8 Đạt).
    6. *Nhóm 6 — Sao lưu và khôi phục dữ liệu:* TC41 – TC45 (5/5 Đạt).
    7. *Nhóm 7 — Hiển thị lịch:* TC46 – TC47 (2/2 Đạt).
    8. *Nhóm 8 — Thống kê, Cài đặt và Widget:* TC48 – TC50 (3/3 Đạt).
  * **Tổng kết kiểm thử hộp đen: 50/50 ca kiểm thử ĐẠT yêu cầu (Tỷ lệ thành công 100%).**
* **Kiểm thử tự động trên JVM (Unit Testing):**
  * Xây dựng **23 test suite** với **208+ test cases** tự động bao phủ toàn diện logic nghiệp vụ thuần Kotlin (`PomodoroTimerEngineTest`, `StatsCalculationTest`, `BackupValidatorTest`, `TaskCommandParserTest`, `PinSecurityTest`, `StreakCalculatorTest`, `DateTimeUtilsTest`,...).
  * Thời gian thực thi toàn bộ test suite: **< 5 giây trên JVM** với tỷ lệ đỗ **100%**.
* **Kiểm chứng ngoài thiết bị:** Kiểm chứng tính toàn vẹn cơ sở dữ liệu SQLite, kiểm chứng cơ chế Migration v1 $\rightarrow$ v4 thành công không lỗi trùng cột, kiểm tra tính đúng đắn của thuật toán băm SHA-256 + Salt.

---

## 6. KẾT LUẬN VÀ HƯỚNG PHÁT TRIỂN

### 6.1. Kết quả đạt được
* **Về chức năng:** Hoàn thành 100% yêu cầu đề bài (11/11 nhóm chức năng cốt lõi) và phát triển thêm các phân hệ mở rộng cao cấp (Widget, Pomodoro, Bento Stats, Gamification Streak/Badges, AI Assistant, App Shortcuts).
* **Về kiến trúc & Chất lượng mã nguồn:** Áp dụng chuẩn mực kiến trúc MVVM, Clean Architecture, Single Source of Truth, Room Flow reactive, quản lý mã nguồn phân nhánh Git bài bản.
* **Về giá trị thực tiễn:** Ứng dụng hoạt động độc lập, không cần mạng, không cần tài khoản, bảo mật tuyệt đối, mang lại trải nghiệm mượt mà và hỗ trợ người dùng nâng cao năng suất cá nhân.

### 6.2. Hạn chế hiện tại
1. Chưa hỗ trợ đồng bộ đám mây tự động thời gian thực giữa nhiều thiết bị.
2. Chưa có tính năng làm việc nhóm, chia sẻ công việc cho nhiều người dùng.
3. Một số hành vi dịch vụ nền phụ thuộc vào chính sách tiết kiệm pin riêng của từng hãng sản xuất thiết bị Android.
4. Chưa có bộ kiểm thử giao diện tự động ở mức UI/Espresso chạy thường xuyên.

### 6.3. Hướng phát triển trong tương lai
* **Ngắn hạn:** Bổ sung bộ kiểm thử UI tự động; hoàn thiện đa ngôn ngữ toàn diện (Vi/En); bổ sung tính năng sao lưu tự động theo lịch qua WorkManager; phục hồi phiên Pomodoro khi tiến trình bị hủy.
* **Trung hạn:** Đồng bộ đám mây đa thiết bị qua Firebase Firestore có giải quyết xung đột dữ liệu (*Conflict Resolution*); tính năng cộng tác nhóm (Collaborative Tasks); hỗ trợ thêm định dạng xuất CSV, XML, iCalendar.
* **Dài hạn:** Tích hợp trí tuệ nhân tạo chuyên sâu phân tích thói quen làm việc và dự đoán khả năng trễ hạn để đề xuất khung giờ tập trung tối ưu; phát triển ứng dụng đồng hành trên đồng hồ thông minh (Wear OS).

---

## 7. TÓM LƯỢC TOÀN BỘ PHỤ LỤC (PHỤ LỤC A – F)

* **Phụ lục A (Cấu trúc Source Code):** Danh mục chi tiết hơn 55 tệp mã nguồn Kotlin được tổ chức theo các package nghiệp vụ (`ai/`, `data/`, `pomodoro/`, `receiver/`, `security/`, `ui/`, `util/`, `viewmodel/`, `widget/`) và cấu trúc tài nguyên `res/`.
* **Phụ lục B (Cấu trúc Database chi tiết):** Lược đồ tạo bảng Room version 4 (`tasks`, `pomodoro_sessions`), các truy vấn DAO tiêu biểu, mã nguồn các bước migration v1 $\rightarrow$ v4 có kiểm tra cấu trúc cột, quy tắc toàn vẹn dữ liệu.
* **Phụ lục C (Bộ Test Case chi tiết):** Bảng chi tiết 50 ca kiểm thử hộp đen hoàn chỉnh (TC01 đến TC50) bao gồm Mã TC, Mục tiêu, Tiền điều kiện, Các bước thực hiện, Kết quả mong đợi, Kết quả thực tế và Đánh giá.
* **Phụ lục D (Các đoạn Source Code quan trọng):** Trích lục 12 đoạn mã nguồn cốt lõi:
  * `D.1`: Lập lịch nhắc việc với cơ chế dự phòng (`AlarmScheduler.kt`).
  * `D.2`: Defensive Migration kiểm tra sự tồn tại của cột trước khi thêm (`AppDatabase.kt`).
  * `D.3`: Ghi phiên tập trung Pomodoro và cộng dồn số liệu trong một transaction.
  * `D.4`: Máy trạng thái Pomodoro — nguồn thời gian đích đơn điệu (`PomodoroTimerEngine.kt`).
  * `D.5`: Xuất tệp sao lưu và khôi phục trong một `@Transaction` (`BackupRepository.kt`).
  * `D.6`: Bộ kiểm tra tính hợp lệ đa tầng của tệp sao lưu (`BackupValidator.kt`).
  * `D.7`: Thuật toán băm mã PIN bằng SHA-256 kèm Salt ngẫu nhiên (`PinRepositoryImpl.kt`).
  * `D.8`: Foreground Service quản lý đồng hồ đếm ngược Pomodoro (`PomodoroService.kt`).
  * `D.9`: Bộ phân tích cú pháp biểu thức chính quy xử lý lệnh tự nhiên (`TaskCommandParser.kt`).
  * `D.10`: Thuật toán kiểm tra điều kiện mở khóa huy hiệu (`SpecialBadgeCalculator.kt`).
  * `D.11`: Lớp tiện ích quản lý xác thực sinh trắc học vân tay an toàn (`BiometricAuthHelper.kt`).
  * `D.12`: Cấu hình phím tắt ứng dụng tĩnh trên màn hình chính (`shortcuts.xml`).
* **Phụ lục E (Hướng dẫn sử dụng ứng dụng):** Cẩm nang hướng dẫn người dùng gồm 12 mục chi tiết từ cài đặt, tạo việc, quản lý lịch, khóa PIN, sao lưu JSON, kéo widget, chạy Pomodoro, trò chuyện với Trợ lý AI, theo dõi Chuỗi ngày và bảng xử lý sự cố thường gặp.
* **Phụ lục F (Đường dẫn dự án):** Kho lưu trữ mã nguồn GitHub chính thức của đề tài: `https://github.com/ThaiDevv/Task-Management-App`.
