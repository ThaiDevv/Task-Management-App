# 🗺️ WORKFLOW TOÀN BỘ HỆ THỐNG — Task Management App

---

## 1. 🏗️ SYSTEM OVERVIEW — Kiến trúc tổng quan

```mermaid
graph TB
    subgraph "📱 UI Layer"
        MA["MainActivity<br/>BottomNavigationView"]
        TLF["TaskListFragment"]
        CF["CalendarFragment"]
        SF["SettingsFragment"]
        AETA["AddEditTaskActivity"]
        TDA["TaskDetailActivity"]
        PLA["PinLockActivity"]
        BRA["BackupRestoreActivity"]
    end

    subgraph "🧠 ViewModel Layer"
        TVM["TaskViewModel"]
        CVM["CalendarViewModel"]
        BVM["BackupViewModel"]
    end

    subgraph "📦 Repository Layer"
        TR["TaskRepository"]
        BR["BackupRepository"]
    end

    subgraph "💾 Data Layer"
        RDB["Room Database<br/>(AppDatabase)"]
        DAO["TaskDao"]
        ESP["EncryptedSharedPreferences<br/>(PIN Hash + Salt)"]
        SP["SharedPreferences<br/>(Settings, Sort Pref)"]
        JSON["JSON File I/O<br/>(Backup/Restore)"]
    end

    subgraph "⚙️ Services & Receivers"
        NH["NotificationHelper"]
        AS["AlarmScheduler"]
        AR["AlarmReceiver"]
        BOOT["BootReceiver"]
        TCR["TimeChangeReceiver"]
        PM["PinManager"]
    end

    MA --> TLF & CF & SF
    TLF --> TVM
    CF --> CVM
    SF -->|PIN Toggle| PM
    SF -->|Backup| BRA
    AETA --> TVM
    TDA --> TVM
    PLA --> PM
    BRA --> BVM

    TVM --> TR
    CVM --> TR
    BVM --> BR

    TR --> DAO --> RDB
    BR --> JSON
    PM --> ESP
    TVM -.->|Sort Pref| SP

    TR -->|Schedule/Cancel| AS
    AS --> AR
    AR --> NH
    BOOT -->|BOOT_COMPLETED| AS
    TCR -->|TIME/TZ CHANGED| AS
```

---

## 2. 🚀 APP LAUNCH FLOW — Luồng khởi động ứng dụng

```mermaid
flowchart TD
    START(["👤 User mở App"]) --> CHECK_PIN{"🔐 PIN Lock<br/>đang bật?"}

    CHECK_PIN -->|Không| MAIN["📱 MainActivity<br/>(Task List Tab)"]
    CHECK_PIN -->|Có| PIN_SCREEN["🔢 PinLockActivity<br/>Nhập PIN"]

    PIN_SCREEN --> VERIFY{"✅ PIN<br/>đúng?"}
    VERIFY -->|Đúng| MAIN
    VERIFY -->|Sai| COUNT{"❌ Đã sai<br/>≥ 5 lần?"}
    COUNT -->|Chưa| PIN_SCREEN
    COUNT -->|Rồi| LOCK["⏳ Khóa 30 giây<br/>(Countdown)"]
    LOCK -->|Hết 30s| PIN_SCREEN

    MAIN --> INIT["⚙️ Khởi tạo hệ thống"]
    INIT --> CREATE_CHANNEL["🔔 Tạo Notification Channel"]
    INIT --> CHECK_OVERDUE["⏰ Check & Update<br/>Overdue Tasks"]
    INIT --> CHECK_NOTI_PERM{"📋 Android 13+?"}
    CHECK_NOTI_PERM -->|Có| REQ_PERM["Yêu cầu quyền<br/>POST_NOTIFICATIONS"]
    CHECK_NOTI_PERM -->|Không| READY
    REQ_PERM --> READY["✅ App sẵn sàng"]
    CREATE_CHANNEL --> READY
    CHECK_OVERDUE --> READY

    style START fill:#4CAF50,color:white
    style MAIN fill:#2196F3,color:white
    style LOCK fill:#F44336,color:white
    style READY fill:#4CAF50,color:white
```

---

## 3. 🧭 MAIN NAVIGATION — Điều hướng chính

```mermaid
flowchart LR
    subgraph "📱 MainActivity"
        BN["BottomNavigationView"]
    end

    BN -->|"Tab 1: 📋"| TL["TaskListFragment<br/>Danh sách Task"]
    BN -->|"Tab 2: 📅"| CAL["CalendarFragment<br/>Xem Lịch"]
    BN -->|"Tab 3: ⚙️"| SET["SettingsFragment<br/>Cài đặt"]

    TL -->|"FAB +"| ADD["AddEditTaskActivity<br/>(Mode: ADD)"]
    TL -->|"Click item"| DET["TaskDetailActivity"]
    TL -->|"Filter icon"| FIL["Filter/Sort<br/>Bottom Sheet"]

    DET -->|"Nút Edit"| EDIT["AddEditTaskActivity<br/>(Mode: EDIT)"]
    DET -->|"Nút Delete"| DEL["AlertDialog<br/>Confirm Delete"]
    DET -->|"Nút Complete"| COMP["Toggle<br/>Complete/Uncomplete"]

    CAL -->|"Tap ngày"| DAYLIST["Task List<br/>của ngày selected"]
    DAYLIST -->|"Click task"| DET

    SET -->|"PIN Toggle"| PINSEC["PinLockActivity<br/>(SET/VERIFY)"]
    SET -->|"Backup"| BACKUP["BackupRestoreActivity"]
    SET -->|"Notification Toggle"| NOTI["Enable/Disable<br/>Notifications"]

    style BN fill:#1565C0,color:white
    style TL fill:#0288D1,color:white
    style CAL fill:#0288D1,color:white
    style SET fill:#0288D1,color:white
```

---

## 4. ✏️ TASK CRUD FLOW — Luồng Tạo / Sửa / Xóa / Hoàn thành

```mermaid
flowchart TD
    subgraph "📝 CREATE TASK"
        C1["Nhấn FAB (+)"] --> C2["Mở AddEditTaskActivity<br/>(Mode: ADD)"]
        C2 --> C3["Điền Form:<br/>Title*, Description,<br/>Due Date*, Due Time,<br/>Priority*, Recurrence"]
        C3 --> C4{"🔍 Validate<br/>form?"}
        C4 -->|"❌ Fail"| C5["Hiển thị error<br/>inline trên TextInputLayout"]
        C5 --> C3
        C4 -->|"✅ Pass"| C6["viewModel.insertTask(task)"]
        C6 --> C7["Repository.insert()<br/>→ DAO.insert() (Background Thread)"]
        C7 --> C8["📅 AlarmScheduler<br/>.scheduleAlarm()"]
        C8 --> C9["✅ Snackbar: Task created!<br/>→ Navigate back"]
    end

    subgraph "✏️ EDIT TASK"
        E1["Nhấn Edit<br/>từ TaskDetail"] --> E2["Mở AddEditTaskActivity<br/>(Mode: EDIT, taskId)"]
        E2 --> E3["Load task by ID<br/>→ Populate form"]
        E3 --> E4["Sửa fields"]
        E4 --> E5{"🔍 Validate?"}
        E5 -->|"❌ Fail"| E4
        E5 -->|"✅ Pass"| E6["viewModel.updateTask(task)<br/>updatedAt = now"]
        E6 --> E7{"📅 Date/Time<br/>thay đổi?"}
        E7 -->|Có| E8["AlarmScheduler<br/>.rescheduleAlarm()"]
        E7 -->|Không| E9["✅ Snackbar: Task updated!"]
        E8 --> E9
    end

    subgraph "🗑️ DELETE TASK"
        D1["Nhấn Delete<br/>từ TaskDetail"] --> D2["AlertDialog:<br/>Xác nhận xóa?"]
        D2 -->|Cancel| D3["Đóng dialog"]
        D2 -->|Confirm| D4["viewModel.deleteTask(task)"]
        D4 --> D5["AlarmScheduler<br/>.cancelAlarm(taskId)"]
        D5 --> D6["NotificationHelper<br/>.cancelNotification(taskId)"]
        D6 --> D7["✅ Snackbar: Task deleted!<br/>→ Navigate back"]
    end

    subgraph "✅ MARK COMPLETE"
        M1["Checkbox (List)<br/>hoặc Button (Detail)"] --> M2["viewModel.toggleComplete(task)"]
        M2 --> M3{"🔄 Task có<br/>Recurrence?"}
        M3 -->|Không| M4["status = COMPLETED<br/>isCompleted = true"]
        M3 -->|Có| M5["RecurrenceHelper<br/>.calculateNextDueDate()"]
        M5 --> M6["Tạo Task mới<br/>(kế thừa title, desc, priority,<br/>recurrence)"]
        M6 --> M7["Schedule alarm<br/>cho task mới"]
        M7 --> M4
        M4 --> M8["Cancel alarm<br/>task hiện tại"]
        M8 --> M9["✅ UI Update:<br/>Strikethrough, dim, checkmark"]
    end

    style C1 fill:#4CAF50,color:white
    style E1 fill:#FF9800,color:white
    style D1 fill:#F44336,color:white
    style M1 fill:#2196F3,color:white
```

---

## 5. 🔍 FILTER & SORT FLOW — Luồng Lọc và Sắp xếp

```mermaid
flowchart TD
    TL["📋 TaskListFragment"] --> OPEN["Nhấn Filter/Sort icon"]
    OPEN --> BS["Bottom Sheet / Menu"]

    BS --> FILTER["🔍 FILTER"]
    BS --> SORT["📊 SORT"]

    FILTER --> F1["By Status:<br/>All | TODO | IN_PROGRESS<br/>| COMPLETED | OVERDUE"]
    FILTER --> F2["By Priority:<br/>All | LOW | MEDIUM<br/>| HIGH | URGENT"]
    FILTER --> F3["By Due Date:<br/>Today | This Week<br/>| This Month | Overdue"]

    F1 & F2 & F3 --> FC["FilterCriteria.java<br/>(combine filters)"]
    FC --> VM["viewModel.applyFilter<br/>(FilterCriteria)"]

    SORT --> S1["By Due Date"]
    SORT --> S2["By Priority"]
    SORT --> S3["By Created Date"]
    SORT --> S4["By Title"]
    S1 & S2 & S3 & S4 --> SD["ASC ↑ / DESC ↓"]
    SD --> VMSort["viewModel.applySort<br/>(SortType, SortOrder)"]
    VMSort --> SAVE["💾 Save sort pref<br/>→ SharedPreferences"]

    VM --> RESULT["📋 Danh sách task<br/>cập nhật theo filter"]
    SAVE --> RESULT

    style TL fill:#1565C0,color:white
    style RESULT fill:#4CAF50,color:white
```

---

## 6. 🔔 NOTIFICATION & ALARM SYSTEM — Hệ thống thông báo nhắc nhở

```mermaid
flowchart TD
    subgraph "📅 Lập lịch Alarm"
        TRIGGER["Task tạo/sửa<br/>có Due Date/Time"] --> SCHED["AlarmScheduler<br/>.scheduleAlarm(task)"]
        SCHED --> CHECK_PERM{"🔐 canSchedule<br/>ExactAlarms()?"}
        CHECK_PERM -->|Có| EXACT["AlarmManager<br/>.setExactAndAllowWhileIdle()"]
        CHECK_PERM -->|Không| FALLBACK["Fallback:<br/>WorkManager"]
        EXACT --> PI["PendingIntent<br/>FLAG_IMMUTABLE<br/>requestCode = taskId"]
        FALLBACK --> PI
    end

    subgraph "⏰ Alarm Fire"
        PI --> AR["AlarmReceiver<br/>.onReceive()"]
        AR --> NH["NotificationHelper<br/>.showTaskReminder()"]
        NH --> NOTIF["📲 Notification:<br/>Title = task.title<br/>Body = task.description"]
        NOTIF --> ACT1["Action: ✅ Mark Complete"]
        NOTIF --> ACT2["Action: ⏰ Snooze 15min"]
    end

    subgraph "🔇 Hủy Alarm"
        DEL_TASK["Task bị xóa"] --> CANCEL["AlarmScheduler<br/>.cancelAlarm(taskId)"]
        COMP_TASK["Task hoàn thành"] --> CANCEL
        CANCEL --> DISMISS["NotificationHelper<br/>.cancelNotification(taskId)"]
    end

    subgraph "📋 Quản lý Permission"
        APP_START["App khởi động"] --> CHECK_API{"API ≥ 33?<br/>(Android 13+)"}
        CHECK_API -->|Có| REQ["ActivityResultLauncher<br/>POST_NOTIFICATIONS"]
        REQ --> GRANTED{"Được cấp?"}
        GRANTED -->|Có| OK["✅ Gửi notification bình thường"]
        GRANTED -->|Không| WARN["⚠️ Hiển thị warning:<br/>Notification bị tắt"]
        WARN --> SETTINGS["🔗 Deep link<br/>→ System Settings"]
        CHECK_API -->|Không| OK
    end

    style TRIGGER fill:#FF9800,color:white
    style NOTIF fill:#4CAF50,color:white
    style CANCEL fill:#F44336,color:white
```

---

## 7. 📅 CALENDAR VIEW FLOW — Luồng Xem Lịch

```mermaid
flowchart TD
    CAL["📅 CalendarFragment"] --> LOAD["CalendarViewModel<br/>.loadTasksForMonth(year, month)"]
    LOAD --> QUERY["TaskRepository<br/>.getTasksByDateRange(startOfMonth, endOfMonth)"]
    QUERY --> DAO["TaskDao<br/>@Query SELECT ... WHERE dueDate BETWEEN ? AND ?"]
    DAO --> CACHE["Cache:<br/>Map&lt;Long, List&lt;Task&gt;&gt;"]
    CACHE --> MARK["🔵 Đánh dấu dot indicator<br/>cho ngày có task"]
    MARK --> DISPLAY["Hiển thị Calendar<br/>với dots"]

    DISPLAY -->|"👆 Tap ngày"| SELECT["CalendarViewModel<br/>.selectDate(date)"]
    SELECT --> FILTER_DAY["Lọc tasks<br/>theo ngày selected"]
    FILTER_DAY --> LIST["📋 RecyclerView<br/>Tasks của ngày đó"]
    LIST -->|"Click task"| DETAIL["TaskDetailActivity"]

    DISPLAY -->|"◀ ▶ Swipe tháng"| NAV["CalendarViewModel<br/>.navigateMonth(+1 / -1)"]
    NAV --> LOAD

    style CAL fill:#1565C0,color:white
    style DISPLAY fill:#0288D1,color:white
    style LIST fill:#4CAF50,color:white
```

---

## 8. 🔄 RECURRING TASKS FLOW — Luồng Công việc lặp lại

```mermaid
flowchart TD
    CREATE["Tạo Task với<br/>Recurrence ≠ None"] --> SAVE["Lưu vào Room DB<br/>isRecurring = true<br/>recurrenceType = DAILY/WEEKLY/MONTHLY"]
    SAVE --> ALARM["Schedule Alarm<br/>cho due date gốc"]

    ALARM --> WAIT(["⏳ Chờ user<br/>hoàn thành task"])

    WAIT --> COMPLETE["✅ Mark Complete"]
    COMPLETE --> CALC["RecurrenceHelper<br/>.calculateNextDueDate()"]

    CALC --> DAILY{"recurrenceType?"}
    DAILY -->|"DAILY"| D["+1 ngày"]
    DAILY -->|"WEEKLY"| W["+7 ngày"]
    DAILY -->|"MONTHLY"| M["+1 tháng<br/>(xử lý edge case<br/>tháng 28/29/30/31)"]

    D & W & M --> NEW_TASK["Tạo Task mới:<br/>• Kế thừa title, desc, priority<br/>• Due date = next calculated date<br/>• status = TODO<br/>• isRecurring = true"]

    NEW_TASK --> NEW_ALARM["Schedule Alarm<br/>cho task mới"]
    NEW_ALARM --> OLD_DONE["Task cũ:<br/>status = COMPLETED<br/>Cancel alarm cũ"]

    OLD_DONE --> UI_UPDATE["📋 UI cập nhật:<br/>Task cũ ✅ strikethrough<br/>Task mới xuất hiện trong list"]

    style CREATE fill:#FF9800,color:white
    style COMPLETE fill:#4CAF50,color:white
    style UI_UPDATE fill:#2196F3,color:white
```

---

## 9. 🔐 PIN SECURITY FLOW — Luồng Bảo mật PIN

```mermaid
flowchart TD
    subgraph "🔓 ENABLE PIN"
        EN1["Settings: Toggle PIN ON"] --> EN2["PinLockActivity<br/>(Mode: SET)"]
        EN2 --> EN3["Nhập PIN mới<br/>(4-6 số)"]
        EN3 --> EN4["Xác nhận PIN<br/>(nhập lại)"]
        EN4 --> EN5{"Match?"}
        EN5 -->|Không| EN3
        EN5 -->|Có| EN6["PinManager.setPin():<br/>Salt = random bytes<br/>Hash = SHA-256(PIN + Salt)"]
        EN6 --> EN7["Lưu Hash + Salt<br/>→ EncryptedSharedPreferences"]
        EN7 --> EN8["✅ PIN enabled"]
    end

    subgraph "🔑 VERIFY PIN (Mở app)"
        V1["App mở / Resume<br/>sau > 1 phút"] --> V2["PinLockActivity<br/>(Mode: ENTER)"]
        V2 --> V3["Nhập PIN"]
        V3 --> V4["PinManager.verifyPin():<br/>Hash(input + stored Salt)<br/>== stored Hash?"]
        V4 -->|"✅ Đúng"| V5["Mở app"]
        V4 -->|"❌ Sai"| V6["Đếm lần sai"]
        V6 --> V7{"≥ 5 lần?"}
        V7 -->|Chưa| V3
        V7 -->|Rồi| V8["⏳ Lock 30s<br/>(Countdown timer)"]
        V8 --> V3
    end

    subgraph "🔄 CHANGE PIN"
        CH1["Settings: Change PIN"] --> CH2["Step 1: Nhập PIN cũ"]
        CH2 --> CH3{"Verify?"}
        CH3 -->|Sai| CH2
        CH3 -->|Đúng| CH4["Step 2: Nhập PIN mới"]
        CH4 --> CH5["Step 3: Xác nhận PIN mới"]
        CH5 --> CH6{"Match?"}
        CH6 -->|Không| CH4
        CH6 -->|Có| CH7["PinManager.setPin()<br/>(New Hash + New Salt)"]
        CH7 --> CH8["✅ PIN changed!"]
    end

    subgraph "🔓 DISABLE PIN"
        DIS1["Settings: Toggle PIN OFF"] --> DIS2["Verify PIN hiện tại"]
        DIS2 --> DIS3{"Đúng?"}
        DIS3 -->|Không| DIS2
        DIS3 -->|Có| DIS4["PinManager.removePin()"]
        DIS4 --> DIS5["✅ PIN disabled"]
    end

    style EN8 fill:#4CAF50,color:white
    style V5 fill:#4CAF50,color:white
    style CH8 fill:#4CAF50,color:white
    style DIS5 fill:#4CAF50,color:white
    style V8 fill:#F44336,color:white
```

---

## 10. 💾 BACKUP & RESTORE FLOW — Luồng Sao lưu / Khôi phục

```mermaid
flowchart TD
    subgraph "📤 EXPORT (Backup)"
        EX1["Nhấn Export<br/>trong BackupRestoreActivity"] --> EX2["SAF: ACTION_CREATE_DOCUMENT<br/>filename: task_backup_20260725.json"]
        EX2 --> EX3["User chọn vị trí lưu"]
        EX3 --> EX4["BackupRepository.exportTasks(uri)"]
        EX4 --> EX5["Query tất cả tasks<br/>từ Room DB"]
        EX5 --> EX6["Serialize → JSON (Gson)<br/>{version, exportDate, tasks: [...]}"]
        EX6 --> EX7["Ghi file qua<br/>ContentResolver.openOutputStream()"]
        EX7 --> EX8["Lưu last backup timestamp"]
        EX8 --> EX9["✅ Snackbar:<br/>Exported N tasks successfully!"]
    end

    subgraph "📥 IMPORT (Restore)"
        IM1["Nhấn Import<br/>trong BackupRestoreActivity"] --> IM2["SAF: ACTION_OPEN_DOCUMENT<br/>MIME: application/json"]
        IM2 --> IM3["User chọn file JSON"]
        IM3 --> IM4["Đọc file qua<br/>ContentResolver.openInputStream()"]
        IM4 --> IM5["JsonValidator.validate(json)"]
        IM5 --> IM6{"Valid?"}
        IM6 -->|"❌ Invalid"| IM7["Error: Hiển thị lỗi cụ thể<br/>(file corrupt, missing fields...)"]
        IM6 -->|"✅ Valid"| IM8["Parse JSON → List&lt;Task&gt;"]
        IM8 --> IM9{"Conflict?<br/>(Task trùng ID)"}
        IM9 -->|"Có"| IM10["Dialog:<br/>Replace / Skip / Replace All"]
        IM9 -->|"Không"| IM11["Room @Transaction<br/>Atomic insert all tasks"]
        IM10 --> IM11
        IM11 --> IM12["✅ Snackbar:<br/>Imported N tasks successfully!"]
    end

    style EX9 fill:#4CAF50,color:white
    style IM12 fill:#4CAF50,color:white
    style IM7 fill:#F44336,color:white
```

---

## 11. 📡 SYSTEM EVENTS FLOW — Xử lý sự kiện hệ thống

```mermaid
flowchart TD
    subgraph "🔄 BOOT_COMPLETED"
        BOOT["📱 Device khởi động lại"] --> BR["BootReceiver.onReceive()"]
        BR --> ASYNC["goAsync() — Background thread"]
        ASYNC --> QUERY_PENDING["Query tasks:<br/>dueDate ≥ now<br/>AND status ≠ COMPLETED"]
        QUERY_PENDING --> LOOP["Với mỗi task →<br/>AlarmScheduler.scheduleAlarm()"]
        LOOP --> DONE1["✅ Tất cả alarms<br/>đã được khôi phục"]
    end

    subgraph "🕐 TIME / TIMEZONE / DATE CHANGED"
        TIME["⏰ User đổi giờ<br/>hoặc timezone"] --> TCR["TimeChangeReceiver.onReceive()<br/>ACTION_TIME_CHANGED<br/>ACTION_TIMEZONE_CHANGED<br/>ACTION_DATE_CHANGED"]
        TCR --> RESCHED["Reschedule<br/>TẤT CẢ active alarms"]
        RESCHED --> RECHECK["checkAndUpdateOverdueTasks()<br/>(re-check overdue)"]
        RECHECK --> DONE2["✅ Alarms & Overdue<br/>đã cập nhật"]
    end

    subgraph "⏰ OVERDUE DETECTION"
        APP_RESUME["App resume /<br/>WorkManager periodic"] --> CHECK["checkAndUpdateOverdueTasks()"]
        CHECK --> SQL["UPDATE tasks<br/>SET status = 'OVERDUE'<br/>WHERE dueDate < now<br/>AND status ≠ 'COMPLETED'"]
        SQL --> UI["🔴 UI update:<br/>Overdue visual indicator"]
    end

    style DONE1 fill:#4CAF50,color:white
    style DONE2 fill:#4CAF50,color:white
    style UI fill:#FF5722,color:white
```

---

## 12. 🎨 UI STATES FLOW — 5 Trạng thái giao diện

```mermaid
flowchart TD
    VM["ViewModel emit<br/>UiState&lt;List&lt;Task&gt;&gt;"] --> STATE{"UiState?"}

    STATE -->|"⏳ LOADING"| LOADING["ProgressBar / Shimmer<br/>layout_loading_state.xml"]
    STATE -->|"📭 EMPTY"| EMPTY["Icon + Message:<br/>'Chưa có task nào'<br/>+ Button 'Tạo task đầu tiên'<br/>layout_empty_state.xml"]
    STATE -->|"✅ SUCCESS"| SUCCESS["RecyclerView hiển thị<br/>danh sách tasks<br/>+ Snackbar cho actions"]
    STATE -->|"❌ ERROR"| ERROR["Icon lỗi + Message<br/>+ Button 'Thử lại'<br/>layout_error_state.xml"]

    SUCCESS --> OVERDUE_CHECK{"Có task<br/>quá hạn?"}
    OVERDUE_CHECK -->|"Có"| OVERDUE["🔴 OVERDUE Visual:<br/>• Background đỏ nhạt<br/>• Icon cảnh báo<br/>• Text 'Overdue'<br/>trong item_task.xml"]
    OVERDUE_CHECK -->|"Không"| NORMAL["Hiển thị bình thường"]

    ERROR -->|"Nhấn Retry"| VM

    style LOADING fill:#FFC107,color:black
    style EMPTY fill:#9E9E9E,color:white
    style SUCCESS fill:#4CAF50,color:white
    style ERROR fill:#F44336,color:white
    style OVERDUE fill:#FF5722,color:white
```

---

## 13. 🔀 DATA FLOW ARCHITECTURE — Luồng dữ liệu MVVM End-to-End

```mermaid
flowchart LR
    subgraph "👆 User Action"
        UA["Tap / Input / Scroll"]
    end

    subgraph "📱 View (Activity/Fragment)"
        VIEW["Observe LiveData<br/>→ Update UI"]
    end

    subgraph "🧠 ViewModel"
        VMLD["MutableLiveData<br/>(internal)"]
        VMEX["LiveData<br/>(exposed to UI)"]
    end

    subgraph "📦 Repository"
        REPO["Business Logic<br/>+ Background Thread"]
    end

    subgraph "💾 Data Sources"
        ROOM["Room Database<br/>(DAO)"]
        PREFS["SharedPreferences<br/>EncryptedSharedPrefs"]
        FILES["JSON File I/O"]
    end

    UA -->|"1. User event"| VIEW
    VIEW -->|"2. Call method"| VMLD
    VMLD -->|"3. Delegate to"| REPO
    REPO -->|"4. CRUD on<br/>background thread"| ROOM & PREFS & FILES
    ROOM -->|"5. LiveData<br/>auto-notify"| REPO
    REPO -->|"6. Forward<br/>LiveData"| VMEX
    VMEX -->|"7. Observe &<br/>re-render UI"| VIEW

    style UA fill:#FF9800,color:white
    style VIEW fill:#2196F3,color:white
    style VMLD fill:#9C27B0,color:white
    style REPO fill:#009688,color:white
    style ROOM fill:#795548,color:white
```

---

## 📊 TỔNG HỢP CÁC CHỨC NĂNG THEO MODULE

| Module | Chức năng | Screens liên quan |
|--------|-----------|-------------------|
| **🏠 Home & Navigation** | Bottom Navigation 3 tabs, Dashboard | `MainActivity`, `TaskListFragment`, `CalendarFragment`, `SettingsFragment` |
| **✏️ Task CRUD** | Tạo / Sửa / Xóa / Hoàn thành task | `AddEditTaskActivity`, `TaskDetailActivity` |
| **🔍 Filter & Sort** | Lọc (Status, Priority, Date) + Sắp xếp (4 tiêu chí × 2 hướng) | `TaskListFragment` (Bottom Sheet) |
| **🔄 Recurring Tasks** | Lặp lại Daily / Weekly / Monthly, tự tạo task mới khi hoàn thành | `RecurrenceHelper`, `AddEditTaskActivity` |
| **📅 Calendar View** | Xem lịch tháng, đánh dấu ngày có task, xem task theo ngày | `CalendarFragment`, `CalendarViewModel` |
| **🔔 Notifications** | Nhắc nhở đúng giờ, action Mark Complete & Snooze 15min | `NotificationHelper`, `AlarmScheduler`, `AlarmReceiver` |
| **📡 System Events** | Khôi phục alarms sau reboot / timezone / date change | `BootReceiver`, `TimeChangeReceiver` |
| **🔐 PIN Security** | Bật/Tắt/Đổi PIN, SHA-256 hash, auto-lock sau 1 phút background | `PinLockActivity`, `PinManager` |
| **💾 Backup & Restore** | Xuất/Nhập file JSON qua SAF, validate data, atomic transaction | `BackupRestoreActivity`, `BackupRepository`, `JsonValidator` |
| **🎨 UI States** | Loading, Empty, Error, Success, Overdue — 5 trạng thái giao diện | Tất cả Fragments |
| **✅ Validation** | Title required, date valid, max chars, real-time TextWatcher | `ValidationHelper`, `AddEditTaskActivity` |
| **⏰ Overdue Detection** | Tự động phát hiện & đánh dấu task quá hạn | `TaskRepository`, `TaskDao` |
