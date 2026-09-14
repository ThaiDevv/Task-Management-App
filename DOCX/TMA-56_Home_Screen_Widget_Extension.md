# TMA-56 — Home Screen Widget Extension: Lý thuyết & Cơ chế hoạt động

> Tài liệu kỹ thuật cho widget "Việc hôm nay" (Today Tasks Widget) của TaskFlow.
> Branch: `feature/TMA-56-Implement-Home-Screen-Widget-Extension`

---

## 1. AppWidget là gì? Lý thuyết nền tảng

**App Widget** là một view thu nhỏ của ứng dụng được nhúng vào **màn hình chính (launcher)** của Android. Widget không phải là một Activity — nó **không chạy trong process của app**, mà được **render bởi process của launcher** (vd: Pixel Launcher, Nova Launcher).

Điều này tạo ra ràng buộc cốt lõi của toàn bộ hệ thống widget:

> App và launcher là 2 process khác nhau, không thể chia sẻ object hay gọi hàm trực tiếp.
> Android giải quyết bằng **RemoteViews** — một "bản mô tả tuần tự hóa (parcelable)" của cây view,
> launcher đọc mô tả này và dựng lại view theo đúng chỉ định.

### 1.1. RemoteViews — tập con hạn chế của View

Vì launcher tự inflate view, RemoteViews chỉ cho phép:

| Được phép | Không được phép |
|---|---|
| Các lớp framework: `FrameLayout`, `LinearLayout`, `RelativeLayout`, `GridLayout`, `ListView`, `TextView`, `ImageView`, `ProgressBar`, `Chronometer`... | Custom View, RecyclerView, Compose, mọi class thuộc app |
| Gọi hàm setter qua chuỗi: `setInt(id, "setTextColor", ...)`, `setTextViewText(...)`, `setViewVisibility(...)` | Gán listener (`onClickListener` không dùng được) |
| Tương tác qua **PendingIntent**: Activity / Broadcast / Service | Thao tác trực tiếp trên view object |

> ⚠️ **`<View>` trần (`android.view.View`) KHÔNG nằm trong whitelist.** Launcher sẽ báo
> `InflateException: Class not allowed to be inflated android.view.View` và hiển thị
> "Couldn't add widget" cho toàn bộ vùng danh sách. Muốn vẽ một ô/mảng chỉ có nền
> (như stripe màu priority) thì dùng **`ImageView`** — class này được phép và nhận
> `setBackgroundResource` như mọi View khác. (Bug đã gặp và fix trong TMA-56.)

Mọi thay đổi động (đổi text, đổi màu) phải đi qua API của RemoteViews, ví dụ trong dự án này:

```kotlin
views.setTextViewText(R.id.widget_item_title, item.title)
views.setInt(R.id.widget_item_title, "setPaintFlags", Paint.STRIKE_THRU_TEXT_FLAG) // gạch ngang task đã xong
views.setInt(R.id.widget_progress, "setProgress", percent) // ProgressBar không có setProgress riêng trên RemoteViews cũ
```

### 1.2. Ba thành phần bắt buộc của một widget

```
┌─────────────────────────────────────────────────────────────┐
│ 1. AppWidgetProvider (BroadcastReceiver)                     │
│    - Nhận APPWIDGET_UPDATE, APPWIDGET_OPTIONS_CHANGED        │
│    - Vẽ RemoteViews "khung" + gắn adapter cho ListView       │
├─────────────────────────────────────────────────────────────┤
│ 2. appwidget-provider metadata (res/xml/*.xml)               │
│    - Kích thước tối thiểu, resizable, updatePeriodMillis,    │
│      initialLayout, widgetCategory                           │
├─────────────────────────────────────────────────────────────┤
│ 3. Đăng ký trong AndroidManifest                             │
│    - <receiver> + intent-filter APPWIDGET_UPDATE             │
│    - <meta-data> trỏ tới file metadata ở trên                │
└─────────────────────────────────────────────────────────────┘
```

### 1.3. Kích thước widget → ô lưới màn hình chính

Quy ước chuẩn (từ tài liệu Android): `cell = 70dp × n − 30dp`.
Widget này khai báo `minWidth = 250dp` (~4 cột), `minHeight = 110dp` (~2 hàng),
và trên Android 12+ khai báo `targetCellWidth/Height = 4/2`. Người dùng có thể
resize để danh sách dài hơn (`maxResizeHeight = 400dp`).

---

## 2. Kiến trúc widget của TaskFlow

### 2.1. Sơ đồ tổng thể

```
                        ┌──────────────────────────────┐
                        │        Room DB (tasks)        │
                        └──────────────┬───────────────┘
                                       │ Room InvalidationTracker
                                       ▼ (bảng "tasks" bị đổi)
┌───────────────────── app process ────────────────────────────┐
│ TaskApplication → WidgetUpdater.updateAll()                  │
│   1. loadTodayTasks(): query đồng bộ [00:00 → 23:59 hôm nay] │
│   2. pushTodaySnapshot(): vẽ header (đếm việc, progress %)   │
│   3. notifyAppWidgetViewDataChanged() → launcher yêu cầu     │
│      RemoteViewsService dựng lại từng dòng                   │
└──────────────────────────────┬───────────────────────────────┘
                               │ RemoteViews (parcelable, qua Binder)
┌────────────────────── launcher process ──────────────────────┐
│ Launcher inflate widget_today_tasks.xml                      │
│ ListView ← TaskWidgetService (BIND_REMOTEVIEWS)              │
│            └─ TaskWidgetViewsFactory: dựng từng dòng task    │
└──────────────────────────────────────────────────────────────┘
```

### 2.2. Thành phần code và vai trò

| File | Vai trò |
|---|---|
| `widget/WidgetTaskListBuilder.kt` | Logic thuần (JVM-testable): biến `List<Task>` thành danh sách UI model đã sắp xếp + thống kê. Sắp xếp: **chưa xong trước → có giờ trước "Cả ngày" → theo giờ tăng dần; đã xong xuống cuối cùng.** |
| `widget/TaskWidgetProvider.kt` | `AppWidgetProvider`. `onUpdate` (launcher thêm widget / reboot): vẽ header + gắn adapter. `onReceive`: xử lý click mở chi tiết / hoàn thành task. |
| `widget/TaskWidgetService.kt` | `RemoteViewsService` + `TaskWidgetViewsFactory`: load snapshot mới nhất mỗi lần launcher cần vẽ lại danh sách; tạo RemoteViews từng dòng (stripe màu priority, giờ, checkbox tick). |
| `widget/WidgetUpdater.kt` | Điểm refresh trung tâm — mọi thay đổi dữ liệu gọi `updateAll()`. Widget **không giữ state riêng**, luôn vẽ lại từ Room. |
| `widget/WidgetMidnightScheduler.kt` | WorkManager periodic 24h, initial delay đến 00:00 → widget tự đổi sang "việc của ngày mới" ngay sau nửa đêm mà không cần mở app. |
| `util/TaskCompletionHelper.kt` | Đóng gói logic hoàn thành task (đảo trạng thái + hủy/đặt lại alarm + sinh instance kế tiếp cho task lặp lại) để **dùng chung** cho widget và notification action — mirror `TaskViewModel.toggleTaskComplete`. |
| `data/local/dao/TaskDao.kt` | Query mới `getTasksForDateRangeSync(start, end)` — snapshot đồng bộ các việc trong hôm nay. |

### 2.3. Cơ chế tương tác (click)

RemoteViews không gán listener được — toàn bộ click đi qua PendingIntent:

```
Header              → PendingIntent.getActivity(MainActivity)
Nút "+"             → PendingIntent.getActivity(AddEditTaskActivity)
Dòng task           → PendingIntent.getBroadcast(TASK template)
Checkbox từng dòng  → fill-in intent của dòng (cùng template, khác extras)
```

**Kỹ thuật PendingIntent template + fill-in intent:** vì ListView có hàng chục dòng
mà PendingIntent phải là một object duy nhất, ta đặt **một** PendingIntent "template"
lên toàn ListView bằng `setPendingIntentTemplate()`. Từng dòng chỉ đính kèm
`fill-in intent` chứa extras (`EXTRA_TASK_ID` + loại click). Khi người dùng chạm,
Android **hợp nhất** template với fill-in intent và phát broadcast.

Trong dự án, template chỉ có một action duy nhất, loại click được phân biệt bằng extra
`EXTRA_WIDGET_ACTION`:

| Chạm vào | Extra | Hành vi |
|---|---|---|
| Dòng task | `WIDGET_ACTION_OPEN_TASK` | Mở `TaskDetailActivity` |
| Checkbox | `WIDGET_ACTION_TOGGLE_TASK` | `TaskCompletionHelper.toggleById()` → cập nhật Room → refresh widget |

> Vì hoàn thành task từ widget **cần ghi DB**, phần xử lý nằm trong `BroadcastReceiver`
> với `goAsync()` + coroutine trên `Dispatchers.IO`: giữ receiver sống ~10s (giới hạn
> của goAsync) đủ để hoàn tất ghi Room rồi mới `finish()`.

> ⚠️ **Bắt buộc `FLAG_MUTABLE` cho PendingIntent template.** Cơ chế fill-in hoạt động
> bằng cách "điền thêm" extras vào PendingIntent lúc người dùng chạm. Với
> `FLAG_IMMUTABLE`, Android 12+ **chặn việc merge extras** → broadcast vẫn được gửi
> nhưng `intent.getIntExtra(EXTRA_TASK_ID, -1)` trả về `-1` và mọi cú chạm rơi vào
> nhánh mặc định (bug đã gặp và fix trong TMA-56). Template là broadcast nội bộ
> (explicit component, receiver `exported="false"`), nên `FLAG_MUTABLE` ở đây an toàn.

---

## 3. Cơ chế tự cập nhật (điểm mấu chốt của widget "luôn đúng")

Widget stale (hiển thị dữ liệu cũ) là lỗi phổ biến nhất của app có widget. TaskFlow
đóng mọi khe hở bằng 4 lớp refresh, **không có polling thủ công**:

| # | Kích hoạt | Cơ chế | Độ trễ |
|---|---|---|---|
| 1 | App thêm/sửa/xóa/hoàn thành task | Room **InvalidationTracker** lắng nghe bảng `tasks` (đăng ký trong `TaskApplication.onCreate`) → `WidgetUpdater.updateAll()` | ~0s (ngay khi DB commit) |
| 2 | Đổi giờ / múi giờ / đổi ngày trên máy | `TimeChangeReceiver` (có sẵn) thêm lời gọi `WidgetUpdater.updateAllSafe()` | ngay lập tức |
| 3 | Qua nửa đêm (ngày mới) | WorkManager periodic 24h (`WidgetMidnightWorker`) | ≤ vài phút sau 00:00 |
| 4 | Reboot máy / launcher thêm lại widget | Launcher tự phát `APPWIDGET_UPDATE` → `onUpdate()` vẽ lại từ Room | ngay khi launcher restore |

Lớp 1 là quan trọng nhất: `InvalidationTracker.Observer("tasks")` là **observer cấp bảng**
của Room — chỉ một dòng đăng ký trong `TaskApplication` là mọi ViewModel/repository
cập nhật task (kể cả qua đường import JSON, restore backup, notification action)
đều tự động kéo widget refresh, không phải chèn gọi tay vào từng điểm cập nhật.

> Lưu ý: `updatePeriodMillis` trong metadata chỉ đặt 30 phút như phương án dự phòng —
> đây là cơ chế polling tốn pin của Android và trong thực tế gần như không bao giờ
> cần thiết khi có lớp 1–3.

---

## 4. Thiết kế giao diện (bento card hiện đại)

### 4.1. Cấu trúc `widget_today_tasks.xml`

```
╭──────────────────────────────────────────╮  bo góc 22dp, nền widget_surface, viền 1dp
│  VIỆC HÔM NAY            (Còn 3 việc) [+] │  header nền tinted (widget_header_background)
│  Thứ hai, 14/09            ▁▁▁▁▁▂▂▂▂▂▂   │  thanh progress 4dp bo tròn
├──────────────────────────────────────────┤
│ ▎Đá lịch họp sprint          09:00   ✓○  │  stripe 4dp màu theo priority
│ ▎Học từ vựng               Cả ngày   ✓○  │  task đã xong: gạch ngang + mờ màu
│ ▎Gọi khách hàng A            15:30   ✓○  │
╰──────────────────────────────────────────╯
```

### 4.2. Nguyên tắc thiết kế

- **Bám design system TaskFlow:** màu primary `#105CDB`, success `#10B981`,
  bảng màu priority (`priority_low/medium/high/urgent`) y hệt màn hình danh sách —
  người dùng nhận diện được ngay dù không mở app.
- **Font Poppins** (app font gốc) — font resources hoạt động trong RemoteViews từ API 26.
- **Dark mode đầy đủ:** toàn bộ màu widget đặt trong `values/widget_colors.xml` và
  `values-night/widget_colors.xml` (nền `#191B22`, fill progress `#8FAEFF`...).
  Launcher tự resolve theo `uiMode` của nó — không cần code phân nhánh.
- **Khoảng trống hợp lý, chữ ≥ 11sp, vùng chạm checkbox 24dp** — tuân thủ
  hướng dẫn accessibility của Google cho widget.

### 4.3. Danh sách động trong RemoteViews

`ListView` trong widget KHÔNG dùng adapter thông thường. Nó dùng
**RemoteViewsService.RemoteViewsFactory** — bản tuần tự hóa của Adapter:

1. App khai báo `TaskWidgetService` với quyền `BIND_REMOTEVIEWS` trong Manifest
   (Android yêu cầu để launcher được bind vào service của app).
2. `setRemoteAdapter(android.R.id.list, intent)` trong Provider trỏ ListView tới service.
3. Khi cần vẽ (lần đầu, resize, hoặc `notifyAppWidgetViewDataChanged`),
   launcher **bind** tới service, gọi `onDataSetChanged()` — tại đây app load
   snapshot Room mới nhất — rồi launcher lấy từng dòng qua `getViewAt(position)`.
4. `setEmptyView(list, emptyTextView)`: launcher tự ẩn/hiện dòng chữ
   "Không có việc nào cho hôm nay" khi danh sách rỗng.

---

## 5. Quy ước & điểm cần nhớ khi bảo trì

1. **Widget không cache dữ liệu.** Mọi luồng ghi task mới đều không cần sửa code widget —
   chỉ cần đúng đường đi qua Room.
2. **Sửa logic hoàn thành task phải đổi cả 2 nơi** nếu `TaskViewModel.toggleTaskComplete`
   có hành vi mới (hiện tại `TaskCompletionHelper` đã mirror đầy đủ, gồm cả recurrence).
3. **Thêm trường hiển thị mới** (vd: tag): thêm vào `WidgetTaskUiModel` của
   `WidgetTaskListBuilder`, có unit test tương ứng, rồi mới sửa layout/factory.
4. **Không thêm custom View hay Material widget vào layout widget** — sẽ crash
   trong process launcher. Chỉ dùng view framework. **Đặc biệt không dùng `<View>` trần**
   — dùng `ImageView` thay thế (xem cảnh báo ở mục 1.1).
5. **Mọi `setInt` với màu phải truyền giá trị màu đã resolve** (`context.getColor(...)`)
   hoặc drawable resource (`setBackgroundResource`) — truyền `R.color.*` thẳng vào
   setter màu sẽ cho kết quả sai vì RemoteViews không resolve màu tự động.
6. **PendingIntent template cho collection luôn phải `FLAG_MUTABLE`** — nếu không,
   fill-in intent mất extras (xem cảnh báo ở mục 2.3).
7. **Widget hiển thị tiêu đề task kể cả khi app đang bật PIN lock.** Nếu cần bảo mật
   hơn, có thể ẩn tiêu đề (hiện "Việc riêng tư") khi PIN được bật — hiện chưa làm,
   đây là quyết định thiết kế cần cân nhắc.

---

## 6. Nhật ký lỗi đã gặp & cách xử lý (TMA-56)

Hai lỗi dưới đây đều **build thành công, không crash app** — chỉ biểu hiện trên UI widget,
nên rất dễ mất thời gian nếu không biết trước:

| Triệu chứng | Nguyên nhân | Fix |
|---|---|---|
| Widget hiện header + tiến độ đúng nhưng vùng danh sách trống, có chữ xám "Couldn't add widget" | `widget_task_item.xml` dùng `<View>` cho stripe priority — không có trong whitelist RemoteViews | Đổi `<View>` → `<ImageView>` |
| Không hiện task (lỗi ban đầu) | Launcher từ chối inflate **cả dòng** task do lỗi trên, nên ListView không có item nào | Như trên |
| Chạm checkbox không tick; mọi cú chạm đều mở chi tiết; log cho thấy `taskId=-1 widgetAction=-99` | PendingIntent template tạo bằng `FLAG_IMMUTABLE` → extras của fill-in intent không được merge | Đổi template sang `FLAG_MUTABLE` |

Cách chẩn đoán nhanh khi widget "im lặng": logcat của **launcher** (vd `AppWidgetHostView`)
chứa thông báo inflate lỗi, còn log do app tự thêm trong `onReceive` cho biết extras
thực tế nhận được.

---

## 7. Cách test

### 7.1. Unit test (đã có trong repo)

`app/src/test/.../widget/WidgetTaskListBuilderTest.kt` — 7 case:
danh sách rỗng, thứ tự chưa-xong/xong, "có giờ" trước "cả ngày", giờ sớm trước giờ muộn,
đã-xong luôn nằm dưới, số liệu đếm, định dạng giờ (`09:30` / `Cả ngày`).

```bash
./gradlew :app:testDebugUnitTest --tests "com.team.taskmanagementapp.widget.*"
```

### 7.2. Test trên thiết bị / emulator

1. Build: `./gradlew :app:assembleDebug` → cài APK.
2. Long-press màn hình chính → **Widgets** → tìm **TaskFlow** → kéo widget ra.
3. Kịch bản kiểm tra:
   - Thêm/sửa/xóa task trong app → widget cập nhật **ngay** (không cần mở lại widget).
   - Chạm checkbox trên widget → task chuyển gạch ngang, thanh progress tăng.
   - Chạm dòng task → mở đúng TaskDetailActivity.
   - Đặt máy sang ngày mai (đổi ngày hệ thống) → widget hiển thị danh sách của ngày mới.
   - Bật dark mode hệ thống → widget đổi palette tối.
   - Resize widget dọc → danh sách dài ra.

> Lưu ý build máy: dự án cần JDK 17. Trên máy này dùng
> `JAVA_HOME=~/.jdks/jbr-17.0.14 ./gradlew ...` (JDK 25 mặc định của hệ điều hành
> chưa được Gradle 8.2 hỗ trợ).
