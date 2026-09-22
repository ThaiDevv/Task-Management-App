# Task Management App — Ứng dụng Quản lý Công việc (Kotlin)

> **Mã đề tài:** Android_UTH_01  
> **Ngôn ngữ & Công nghệ:** Kotlin, MVVM Architecture, Room Database, ViewModel, LiveData, Coroutines, AlarmManager, Material Design 3  
> **Môi trường phát triển:** Android Studio (minSdk 26, targetSdk 34, JDK 17)

---

## Danh sách thành viên nhóm

| STT | Họ và Tên | MSSV | Vai trò | Trách nhiệm chính |
| :---: | :--- | :---: | :---: | :--- |
| 1 | **Trần Văn Thái (Leader)** | 051206002315 | Leader (Core Architecture) | Kiến trúc MVVM, Database Room, Repository, Security (PIN Lock), Git Management |
| 2 | **Huỳnh Đình Chấn** | 077206002307 | UI Implementation | Code XML Layouts theo design, Activities/Fragments, Navigation, UI States |
| 3 | **Trần Văn Ngọc Thắng** | 046206001641 | Feature Developer 1 | CRUD Task, Task List, Filter & Sort, Recurring Tasks |
| 4 | **Nguyễn Lê Huy Tâm** | 056206011188 | Feature Developer 2 | Notifications, AlarmManager, Calendar View, Boot/Time Change Receivers |
| 5 | **Nguyễn Ngọc Gia Bảo** | 079206008279 | QA & Backup | Testing (Unit/DAO/Functional), Export/Import JSON, Báo cáo & Video Demo |

---

## Cấu trúc Repository (Directory Structure)

```
Task-Management-App/
├── Code/                  # Mã nguồn dự án Android Studio
│   └── TaskManagementApp/
│       ├── app/           # Android app module (data, ui, viewmodel, etc.)
│       ├── gradle/        # Gradle wrapper & version catalog (libs.versions.toml)
│       ├── gradlew        # Gradle wrapper executable script (Linux/Mac)
│       └── gradlew.bat    # Gradle wrapper executable script (Windows)
├── DOCX/                  # Tài liệu báo cáo, kế hoạch dự án & yêu cầu
│   ├── project require.xlsx
│   ├── project_plan.md
│   └── system_workflow.md
├── Extra/                 # Tài nguyên bổ sung, tài liệu tham khảo
├── PPTX/                  # Slide thuyết trình đồ án
└── README.md              # Tài liệu giới thiệu tổng quan dự án
```

---

## Kiến trúc ứng dụng (MVVM Architecture)

Ứng dụng được xây dựng theo kiến trúc **MVVM (Model - View - ViewModel)** chuẩn Google:

- **UI Layer (View):** `Activities`, `Fragments`, `XML Layouts` — chỉ đảm nhận hiển thị dữ liệu và nhận tương tác từ người dùng.
- **ViewModel Layer:** `TaskViewModel`, `CalendarViewModel`, `BackupViewModel` — quản lý UI state (`UiState.kt`), xử lý logic nghiệp vụ và survive qua configuration changes (xoay màn hình).
- **Repository Layer:** `TaskRepository`, `BackupRepository` — abstraction layer quản lý nguồn dữ liệu, chạy các tác vụ nặng trên background thread (`Dispatchers.IO`).
- **Data Layer:** `Room Database` (`AppDatabase`), `TaskDao`, `EncryptedSharedPreferences` (lưu PIN hash), `JSON File I/O` (Backup/Restore).

---

## Hướng dẫn Cài đặt & Chạy ứng dụng

### Yêu cầu môi trường:
1. **Android Studio:** Jellyfish / Hedgehog hoặc phiên bản mới hơn.
2. **JDK:** JDK 17.
3. **Android SDK:** minSdk = 26 (Android 8.0+), targetSdk = 34 (Android 14).

### Các bước thực hiện:
```bash
# 1. Clone repository về máy
git clone https://github.com/ThaiDevv/Task-Management-App.git

# 2. Mở Android Studio
# Chọn "Open" -> Trỏ đến thư mục: Code/TaskManagementApp

# 3. Sync Gradle và Run app
# Bấm nút "Sync Project with Gradle Files"
# Chọn Emulator (API 26+) hoặc Thiết bị thật và bấm "Run"
```

### Thiết lập trợ lý AI

Trợ lý AI sử dụng Firebase AI Logic. Trước khi Sync/Run, mỗi thành viên cần đăng ký
Android app với package `com.team.taskmanagementapp` trong cùng Firebase project,
tải `google-services.json` từ Firebase Console và đặt tại
`Code/TaskManagementApp/app/google-services.json`. Tệp này được `.gitignore` loại
khỏi Git; không đưa token App Check debug hoặc thông tin cấu hình riêng lên commit.

Trong Firebase Console, bật Gemini Developer API cho Firebase AI Logic và đăng ký
App Check cho Android app. Bản debug dùng App Check debug provider: lấy token riêng
của máy từ Logcat, sau đó đăng ký token đó trong Firebase Console. Bản release dùng
Play Integrity và cần cấu hình chứng thực phù hợp trước khi phát hành. Các thao tác
tạo hoặc sửa task do AI đề xuất chỉ được lưu sau khi người dùng xác nhận.

---

## Video Demo Sản Phẩm

*(Link video demo YouTube sẽ được cập nhật sau khi hoàn thành sản phẩm)*

---

## Giấy phép & Quy định
Đồ án thuộc môn học Lập trình Android — Trường Đại học Giao thông Vận tải TP.HCM (UTH).
