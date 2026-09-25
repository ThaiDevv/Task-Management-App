# Task Management App — Ứng dụng Quản lý Công việc (Kotlin)

> **Mã đề tài:** Android_UTH_01  
> **Ngôn ngữ & Công nghệ:** Kotlin 100%, MVVM Architecture, Room Database (v4), ViewModel, LiveData / Flow, Coroutines, AlarmManager, Material Design 3, Firebase Vertex AI (Gemini 2.5 Flash), Biometric Authentication  
> **Môi trường phát triển:** Android Studio (minSdk 26, targetSdk 34, JDK 17)

---

## 👥 Danh sách thành viên nhóm

| STT | Họ và Tên | MSSV | Vai trò | Trách nhiệm chính |
| :---: | :--- | :---: | :---: | :--- |
| 1 | **Trần Văn Thái (Leader)** | 051206002315 | Leader (Core Architecture) | Làm slide thuyết trình, Kiến trúc MVVM, Database Room (v4), Repository, Security (PIN Lock), Git Management, UI Bento Grid |
| 2 | **Huỳnh Đình Chấn** | 077206002307 | UI Implementation | Tổng hợp nội dung tài liệu Báo Cáo, Code XML Layouts theo design, Filter & Overdue Task, Configuration Changes, Pomodoro Focus Timer |
| 3 | **Trần Văn Ngọc Thắng** | 046206001641 | Feature Developer 1 | Tạo Test Case, Calendar View, Edit Task, Export JSON (SAF), Sinh trắc học (Biometric Auth), App Shortcuts |
| 4 | **Nguyễn Lê Huy Tâm** | 056206011188 | Feature Developer 2 | Soạn nội dung tài liệu Báo Cáo, Task Detail Screen, UI States, Recurring Tasks, Import JSON & Validation, Hệ thống Streak & Badges |
| 5 | **Nguyễn Ngọc Gia Bảo** | 079206008279 | QA & Backup | Tạo Test Case, Material 3 Theme, Settings Screen, AlarmManager Notifications, Input Validation, Trợ lý AI Assistant |

---

## 📑 Tài liệu tham chiếu dự án (Documentation Index)

| Tài liệu | Định dạng | Mô tả nội dung |
| :--- | :---: | :--- |
| [Báo cáo đồ án hoàn chỉnh](DOCX/Bao_Cao_Do_An/bao_cao.md) | Markdown / DOCX | Báo cáo chi tiết toàn diện (>4.300 dòng) kèm mã nguồn, bảng đối chiếu và phân tích kỹ thuật |
| [Tóm tắt báo cáo toàn diện](DOCX/Bao_Cao_Do_An/tom_tat_bao_cao.md) | Markdown | Bản tóm tắt điều hành (Executive Summary) cô đọng từ Chương 1 đến Chương 4 |
| [Bộ 50 Test Cases kiểm thử](DOCX/Test_Cases/Test_Case.md) | Markdown / XLSX | Toàn bộ 50 ca kiểm thử hộp đen (TC01–TC50) phủ kín 8 nhóm chức năng |
| [Sơ đồ Luồng & Workflow](DOCX/system_workflow.md) | Markdown | Hệ thống Sequence Diagrams, Luồng dữ liệu và Thiết kế tương tác |
| [Kế hoạch triển khai dự án](DOCX/project_plan.md) | Markdown | Lộ trình phân chia công việc, tiến độ sprint và quản lý Jira |

---

## 📁 Cấu trúc Repository (Directory Structure)

```
Task-Management-App/
├── Code/                                      # Mã nguồn dự án Android Studio
│   └── TaskManagementApp/
│       ├── app/                               # Android App Module chính
│       │   ├── src/main/java/com/team/taskmanagementapp/
│       │   │   ├── ai/                        # Trợ lý AI (Gemini / Firebase AI Logic & Command Parser)
│       │   │   ├── data/                      # Tầng dữ liệu: Room Database v4, Entities, DAOs, Repositories, Validators
│       │   │   ├── pomodoro/                  # Quản lý & Service đếm giờ tập trung Pomodoro (Foreground Service)
│       │   │   ├── receiver/                  # BroadcastReceivers (Alarm, Boot, TimeChange, StreakReminder)
│       │   │   ├── security/                  # Bảo mật mã PIN (SHA-256 + Salt), Sinh trắc học & SharedPreferences
│       │   │   ├── ui/                        # Tầng hiển thị: Activities, Fragments, Adapters, BottomSheets
│       │   │   │   ├── activity/              # AddEditTaskActivity, ImportActivity...
│       │   │   │   ├── detail/                # TaskDetailActivity (Bento Grid) & DeleteDialog
│       │   │   │   ├── pin/                   # SetPinActivity, VerifyPinActivity...
│       │   │   │   └── viewmodel/             # AddEditTaskViewModel, StatsViewModel, PomodoroViewModel...
│       │   │   ├── util/                      # Tiện ích: RecurrenceHelper, StreakCalculator, SpecialBadgeCalculator...
│       │   │   ├── viewmodel/                 # TaskViewModel, CalendarViewModel, ImportViewModel...
│       │   │   ├── widget/                    # App Widget trên màn hình chính (HomeScreen Task Widget)
│       │   │   ├── worker/                    # WorkManager cho các tác vụ chạy ngầm định kỳ
│       │   │   ├── MainActivity.kt            # Activity chính điều hướng Navigation Component
│       │   │   └── TaskApplication.kt         # Khởi tạo toàn cục (Notification Channels, Logging)
│       │   ├── src/main/res/                  # Layouts XML, Drawables, Menus, NavGraph, Values đa ngôn ngữ
│       │   └── src/test/java/                 # 23 Test Suites (>235 Unit Tests) chạy trên JVM
│       ├── gradle/                            # Gradle Wrapper & Version Catalog (libs.versions.toml)
│       ├── build.gradle.kts                   # Project & Module build scripts
│       └── settings.gradle.kts                # Cấu hình Plugin Management & Repositories
├── DOCX/                                      # Tài liệu đồ án, kế hoạch & báo cáo kỹ thuật
│   ├── Bao_Cao_Do_An/                         # Báo cáo tổng hợp (bao_cao.md, tom_tat_bao_cao.md, file .docx)
│   ├── Test_Cases/                            # Danh mục 50 Test Cases (Test_Case.md, Test_Case.xlsx)
│   ├── UI-UX/                                 # Tài liệu thiết kế giao diện & bản mẫu Wireframe
│   ├── project_plan.md                        # Kế hoạch & lộ trình triển khai dự án
│   └── system_workflow.md                     # Tài liệu tổng hợp Workflow & Sequence Diagrams
├── Extra/                                     # Tài nguyên hình ảnh, biểu đồ tham khảo
├── PPTX/                                      # Slide thuyết trình đồ án
└── README.md                                  # Tài liệu giới thiệu tổng quan dự án
```

---

## ✨ Tính năng nổi bật (Key Features)

Ứng dụng được thiết kế theo định hướng **Local-first (Ưu tiên cục bộ)**: Hoạt động ngoại tuyến 100%, bảo mật tuyệt đối, tốc độ tức thì và tích hợp các tiện ích nâng cao:

### 1. 📌 Quản lý công việc toàn diện (Core Task Management)
* **Vòng đời CRUD đầy đủ:** Tạo mới, xem chi tiết, chỉnh sửa, xóa (kèm hộp thoại xác nhận) và đánh dấu hoàn thành.
* **Cấu trúc dữ liệu 7 trường nghiệp vụ:** Tiêu đề, Mô tả, Ngày hạn (DueDate chuẩn hóa `00:00:00`), Giờ nhắc (DueTime), Độ ưu tiên (`HIGH` / `MEDIUM` / `LOW`), Trạng thái (`PENDING` / `IN_PROGRESS` / `COMPLETED` / `OVERDUE`), Quy tắc lặp lại.
* **Lọc đa tiêu chí & Sắp xếp linh hoạt:** Lọc theo trạng thái và mức ưu tiên; Sắp xếp theo hạn chót (tăng/giảm dần) hoặc mức ưu tiên.
* **Danh ngôn động lực 5s (Quote Rotator):** Tự động luân chuyển danh ngôn truyền cảm hứng mỗi 5 giây tại màn hình chi tiết công việc.

### 2. 📅 Lịch biểu trực quan (Calendar View)
* Lưới lịch tháng 7 cột tùy biến hiển thị chỉ báo chấm màu (Event Dots) tại các ngày có công việc.
* Chọn ngày để lọc và xem danh sách công việc kèm giờ nhắc chi tiết tương ứng trong ngày.

### 3. 🔁 Công việc lặp lại định kỳ (Recurring Tasks)
* Hỗ trợ các chu kỳ lặp: **Hằng ngày (Daily)**, **Hằng tuần (Weekly)**, **Hằng tháng (Monthly)**, **Hằng năm (Yearly)**.
* Tùy chọn nâng cao: Đặt ngày kết thúc lặp (`repeatEndDate`) hoặc giới hạn số lần lặp (`repeatLimitCount`).
* Thuật toán `RecurrenceHelper` tự động tính toán kỳ hạn tiếp theo khi hoàn thành công việc.

### 4. 🔔 Nhắc nhở chính xác & Tự phục hồi (Exact Alarms & Recovery)
* Hẹn giờ chính xác theo thời gian thực (`AlarmManager.setExactAndAllowWhileIdle()`).
* Kênh thông báo `task_reminders_channel` độ ưu tiên cao kèm âm thanh chuông báo thức, rung và nút hành động nhanh (Hoàn thành / Báo lại 10 phút).
* Tự động đăng ký lại toàn bộ lịch nhắc sau khi thiết bị khởi động lại (`BootReceiver`) hoặc thay đổi múi giờ (`TimeChangeReceiver`).

### 5. 🔒 Bảo mật đa lớp (PIN Lock & Biometric Authentication)
* Khóa ứng dụng bằng mã PIN 4 số tùy biến; băm mật mã bằng thuật toán **SHA-256 kèm chuỗi Salt ngẫu nhiên**.
* Cơ chế chống dò mã (Anti-Brute Force): Tự động khóa tạm thời 30 giây khi nhập sai 5 lần liên tiếp.
* Tích hợp xác thực sinh trắc học vân tay (`AndroidX BiometricPrompt`); tự động vô hiệu hóa vân tay an toàn khi đổi hoặc xóa mã PIN.

### 6. 💾 Sao lưu & Phục hồi an toàn (JSON Backup / Restore via SAF)
* Xuất/nhập tệp sao lưu JSON qua chuẩn **Storage Access Framework (SAF)**, không yêu cầu quyền nguy hiểm truy cập bộ nhớ.
* Bộ thẩm định `BackupValidator` kiểm tra tính toàn vẹn 5 bước (cú pháp, phiên bản, cấu trúc mảng, kiểu dữ liệu, ràng buộc khóa ngoại).
* Cơ chế khôi phục dữ liệu nguyên tử trong một `@Transaction` duy nhất (*All-or-Nothing*).

### 7. ⏱️ Đồng hồ tập trung Pomodoro (Focus Timer)
* Chu kỳ kỹ thuật chuẩn: 25 phút tập trung, 5 phút nghỉ ngắn, 15 phút nghỉ dài.
* Quản lý qua `PomodoroService` (Foreground Service) chạy nền độc lập; thông báo thường trực hỗ trợ Tạm dừng / Bỏ qua / Dừng phiên.
* Máy trạng thái `PomodoroTimerEngine` dùng mốc thời gian đích đơn điệu (`elapsedRealtime()`), loại bỏ sai số đếm lùi.
* Ghi nhận lịch sử phiên vào Room DB và tích hợp thống kê thời gian tập trung theo từng công việc.

### 8. 📊 Thống kê năng suất Bento Grid Dashboard
* Thẻ tỷ lệ hoàn thành dạng cung tròn xoay sinh động (`CircularCompletionRateView`).
* Biểu đồ cột năng suất 7 ngày trong tuần (`WeeklyProductivityChartView`).
* Thống kê phân bổ công việc theo các mức độ ưu tiên.

### 9. 🏆 Hệ thống Gamification: Streak Tracker & 21 Huy hiệu thành tích
* `StreakCalculator` tính toán chuỗi ngày liên tục (`Current Streak`, `Best Streak`), tự động cập nhật khi qua 00:00.
* Thông báo nhắc nhở bảo vệ chuỗi phát lúc 20:00 hằng ngày nếu người dùng chưa hoàn thành công việc nào.
* **Bộ sưu tập 21 Huy hiệu chia làm 3 nhóm:**
  * 🌟 **7 Streak Milestones:** Starter (3 ngày), Sparkstarter (7 ngày), Streaker (30 ngày), Achiever (50 ngày), Champion (100 ngày), Legend (200 ngày), Master (365 ngày).
  * 🎯 **7 Task Done Milestones:** Task Novice (10 tasks), Task Doer (30 tasks), Task Achiever (50 tasks), Task Executor (100 tasks), Task Expert (250 tasks), Task Champion (500 tasks), Task Master (1000 tasks).
  * ⚡ **7 Special Habit Badges:** Early Bird (hoàn thành trước 8h), Night Owl (hoàn thành sau 22h), Weekend Warrior, Pomodoro Master, Century Club, Consistency King, Speed Demon.

### 10. 🤖 Trợ lý ảo AI thông minh & Phím tắt màn hình chính
* **Xử lý ngôn ngữ tự nhiên (NLP):** `TaskCommandParser` trích xuất câu lệnh bằng Regex (Tạo việc, Xem hôm nay, Đếm việc quá hạn).
* **Google Gemini 2.5 Flash:** Kết nối qua Firebase Vertex AI SDK, bảo vệ bảo mật qua Firebase App Check.
* **Launcher App Shortcuts:** Nhấn giữ icon ứng dụng trên màn hình chính để tạo nhanh công việc hoặc xem danh sách việc hôm nay.

### 11. 📱 Home Screen Task Widget
* Widget hiển thị danh sách công việc của ngày hôm nay kèm giờ nhắc.
* Tích hợp Checkbox tương tác trực tiếp trên màn hình chính để hoàn thành công việc mà không cần mở ứng dụng.
* Tự động chuyển sang ngày mới lúc 00:00 (`WidgetMidnightScheduler`) và tự làm mới ngay khi cơ sở dữ liệu thay đổi (`InvalidationTracker`).

---

## 🏛️ Kiến trúc ứng dụng (MVVM Architecture)

Ứng dụng tuân thủ nghiêm ngặt theo **Kiến trúc MVVM (Model - View - ViewModel)** chuẩn Google khuyến nghị, kết hợp luồng dữ liệu một chiều (**UDF — Unidirectional Data Flow**) và mô hình phản ứng **Reactive Programming (Kotlin Coroutines & Flow)**:

```mermaid
flowchart TD
    %% TIER 1: UI LAYER
    subgraph TIER_UI ["📱 TẦNG HIỂN THỊ — UI & PRESENTATION LAYER"]
        direction LR
        UI_MAIN["<b>Activities & Fragments</b><br/>MainActivity, TaskDetailActivity, AddEditTaskActivity<br/>TaskListFragment, CalendarFragment, RewardsFragment, SettingsFragment"]
        UI_COMP["<b>Components & BottomSheets</b><br/>TaskAdapter, StreakWeekAdapter, FilterBottomSheet<br/>StreakDetailsBottomSheet, AiAssistantBottomSheet"]
        UI_WIDGET["<b>HomeScreen Widget</b><br/>TaskWidgetProvider & Service"]
    end

    %% TIER 2: VIEWMODEL LAYER
    subgraph TIER_VM ["🧠 TẦNG QUẢN LÝ TRẠNG THÁI — VIEWMODEL LAYER (StateFlow / UDF)"]
        direction LR
        VM_CORE["<b>Task & Calendar ViewModels</b><br/>TaskViewModel, AddEditTaskViewModel, CalendarViewModel"]
        VM_FOCUS["<b>Focus & Analytics ViewModels</b><br/>PomodoroViewModel, StatsViewModel"]
        VM_DATA["<b>Backup & Import ViewModels</b><br/>BackupViewModel, ImportViewModel"]
    end

    %% TIER 3: DOMAIN ENGINES
    subgraph TIER_DOMAIN ["⚙️ TẦNG TÍNH TOÁN NGHIỆP VỤ — DOMAIN & COMPUTATION ENGINES"]
        direction LR
        ENG_GAMIFICATION["<b>Gamification & Recurrence</b><br/>StreakCalculator, SpecialBadgeCalculator, RecurrenceHelper"]
        ENG_SMART["<b>AI & Focus State Machine</b><br/>AiTaskAssistant, AiPromptManager, PomodoroTimerEngine"]
        ENG_VALIDATE["<b>Data Integrity Engine</b><br/>BackupValidator & Schema Validation"]
    end

    %% TIER 4: REPOSITORY LAYER
    subgraph TIER_REPO ["📦 TẦNG ĐIỀU PHỐI DỮ LIỆU — REPOSITORY LAYER (Single Source of Truth)"]
        direction LR
        REPO_TASK["<b>TaskRepository</b><br/>CRUD Tasks, Filters, Sorters"]
        REPO_POMO["<b>PomodoroRepository</b><br/>Focus Sessions & Stats History"]
        REPO_BACKUP["<b>BackupRepository</b><br/>JSON Export/Import via SAF"]
        REPO_SEC["<b>Security & Preferences</b><br/>PinRepository, PreferencesRepository"]
    end

    %% TIER 5: DATA PERSISTENCE LAYER
    subgraph TIER_DATA ["💾 TẦNG LƯU TRỮ DỮ LIỆU — DATA PERSISTENCE LAYER"]
        direction LR
        DB_ROOM["<b>Room Database v4 (AppDatabase)</b><br/>TaskDao, PomodoroDao (SQLite ORM)"]
        STORE_SEC["<b>Encrypted Storage</b><br/>SharedPreferences (SHA-256 + Salt PIN)"]
        STORE_SAF["<b>Document Storage</b><br/>Storage Access Framework (JSON Files)"]
    end

    %% TIER 6: BACKGROUND & SYSTEM
    subgraph TIER_SYS ["🔔 HỆ THỐNG NỀN & THÔNG BÁO — BACKGROUND SERVICES & SYSTEM INTEGRATION"]
        direction LR
        SYS_NOTIF["<b>Notification Engine</b><br/>NotificationHelper (Heads-up, Reminders, Pomodoro)"]
        SYS_ALARM["<b>Alarms & Services</b><br/>AlarmManager, StreakReminderScheduler, PomodoroService (Foreground)"]
        SYS_RCV["<b>Broadcast Receivers & Workers</b><br/>AlarmReceiver, BootReceiver, TimeChangeReceiver, WorkManager"]
    end

    %% CONNECTIONS (Strict Top-to-Bottom Flow)
    TIER_UI ==>|User Interactions / Collect UI State| TIER_VM
    TIER_VM -->|Execute Business Logic| TIER_DOMAIN
    TIER_DOMAIN -->|Query & Persist Data| TIER_REPO
    TIER_VM -->|Direct Data Requests| TIER_REPO
    TIER_REPO ==>|Room DB Operations / Storage Access| TIER_DATA
    
    TIER_VM -.->|Schedule Reminders / Trigger Alarms| TIER_SYS
    TIER_SYS -.->|Push Notifications & Re-sync State| TIER_REPO
```

### Chi tiết các tầng trong kiến trúc:

1. **UI Layer (View):**
   - Chỉ đảm nhận vai trò hiển thị giao diện và thu thập sự kiện tương tác của người dùng.
   - Hoàn toàn thụ động (Passive View), không chứa logic tính toán nghiệp vụ hay truy xuất trực tiếp cơ sở dữ liệu.
   - Lắng nghe luồng dữ liệu an toàn vòng đời thông qua `lifecycleScope.launch` kết hợp `repeatOnLifecycle(Lifecycle.State.STARTED)`.

2. **ViewModel Layer:**
   - Đóng vai trò cầu nối trung gian giữa View và Repository.
   - Nắm giữ và phát ra các `StateFlow<UiState>` đại diện cho trạng thái màn hình (`Loading`, `Success`, `Empty`, `Error`).
   - Xử lý các sự kiện đơn lẻ (Single Events) như thông báo `Snackbar`, điều hướng màn hình thông qua `SharedFlow` / `Channel`.

3. **Domain & Computation Layer (Engine tính toán):**
   - Độc lập với Android Framework, xử lý các thuật toán cốt lõi: tính toán chu kỳ lặp lại (`RecurrenceHelper`), thuật toán chuỗi ngày streak và mở khóa 21 danh hiệu (`StreakCalculator`, `SpecialBadgeCalculator`), kiểm tra định dạng tệp sao lưu (`BackupValidator`), máy trạng thái Pomodoro (`PomodoroTimerEngine`).

4. **Repository Layer:**
   - Trừu tượng hóa nguồn dữ liệu (*Single Source of Truth*), cung cấp API sạch cho ViewModel.
   - Chuyển đổi và xử lý các tác vụ I/O nặng trên luồng nền (`Dispatchers.IO`), đảm bảo giao diện luôn mượt mà.

5. **Data & Persistence Layer:**
   - Lưu trữ dữ liệu có cấu trúc với **Room Database Version 4** (hỗ trợ Coroutines Flow và cơ chế Defensive Migration v1 ➔ v2 ➔ v3 ➔ v4).
   - Bảo mật thông tin mã PIN bằng thuật toán SHA-256 kết hợp chuỗi Salt lưu trong `SharedPreferences`.
   - Tích hợp chuẩn Android SAF để người dùng sao lưu và phục hồi dữ liệu từ bộ nhớ trong hoặc Google Drive.

---

## 🧪 Kiểm thử & Đảm bảo chất lượng (Testing & QA)

Dự án áp dụng quy trình kiểm thử toàn diện kết hợp giữa kiểm thử tự động trên JVM và kiểm thử hộp đen trên thiết bị thực tế:

* **50 Ca kiểm thử hộp đen (Black-box Testing TC01–TC50):** Phủ kín 8 nhóm chức năng (CRUD, Recurring, Sắp xếp/Lọc, Notification & Exact Alarm, PIN Lock, Backup/Restore JSON, Calendar View, Widget & Stats). **Tỷ lệ đạt: 50/50 ca (100%)**.
* **23 Test Suites tự động trên JVM (>235 Unit Tests):** Chạy trong **< 5 giây** với tỷ lệ đỗ **100%**, bao phủ toàn bộ các module:
  * `StreakCalculatorTest`: Tính toán chuỗi ngày liên tục, reset chuỗi khi trôi qua 00:00.
  * `PomodoroTimerEngineTest`: Máy trạng thái Pomodoro, xử lý chuyển phiên và đếm thời gian đích.
  * `BackupValidatorTest`: Thẩm định cú pháp, phiên bản, cấu trúc schema và khóa ngoại tệp JSON.
  * `TaskCommandParserTest`: Phân tích câu lệnh ngôn ngữ tự nhiên tiếng Việt và tiếng Anh.
  * `PinSecurityTest`: Thuật toán băm SHA-256 + Salt và cơ chế chống dò mã.
  * `DateTimeUtilsTest`, `RecurrenceHelperTest`: Xử lý mốc thời gian và tính toán kỳ lặp.

---

## 🚀 Hướng dẫn Cài đặt & Chạy ứng dụng

### Yêu cầu môi trường:
1. **Android Studio:** Ladybug / Koala / Jellyfish hoặc phiên bản mới hơn.
2. **JDK:** JDK 17.
3. **Android SDK:** `minSdk = 26` (Android 8.0+), `targetSdk = 34` (Android 14+).

### Các bước thực hiện:
```bash
# 1. Clone repository về máy
git clone https://github.com/ThaiDevv/Task-Management-App.git

# 2. Mở Android Studio
# Chọn "Open" -> Trỏ đến thư mục: Code/TaskManagementApp

# 3. Sync Gradle và Run app
# Bấm nút "Sync Project with Gradle Files"
# Chọn Emulator (API 26+) hoặc Thiết bị thật và bấm "Run" (Shift + F10)
```

### Thiết lập Trợ lý AI (Google Gemini via Firebase)
1. Tạo một project trên [Firebase Console](https://console.firebase.google.com/) và kích hoạt tính năng **Gemini Developer API** (Firebase Vertex AI).
2. Thêm Android App với package name `com.team.taskmanagementapp`.
3. Tải tệp `google-services.json` và đặt vào thư mục `Code/TaskManagementApp/app/google-services.json` (tệp này đã được cấu hình trong `.gitignore`).
4. Kích hoạt Firebase App Check (bản debug sử dụng App Check Debug Provider lấy token từ Logcat).

---

## 🎥 Video Demo Sản Phẩm

*(Link video demo YouTube sẽ được cập nhật sau khi hoàn thành sản phẩm)*

---

## 📄 Giấy phép & Bản quyền
Đồ án thuộc môn học **Lập trình thiết bị di động** — Trường Đại học Giao thông Vận tải TP. Hồ Chí Minh (UTH).  
© 2026 Nhóm sinh viên thực hiện. Giữ toàn quyền phát triển và bảo lưu.

---

<p align="center">
  Đồ án môn học Lập trình Thiết bị di động <br>
  <b>Task Management App</b>
</p>