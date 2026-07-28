# Cấu hình Gradle với các dependencies

Dự án cần được cấu hình để hỗ trợ Room (KSP), Lifecycle (LiveData/ViewModel), Material Design, RecyclerView và Kotlin Coroutines.

## User Review Required

> [!IMPORTANT]
> Tôi sẽ thêm plugin Kotlin và KSP vào dự án. Phiên bản Kotlin được chọn là `2.1.0` và KSP là `2.1.0-1.0.29` để đảm bảo tương thích.

## Proposed Changes

### [Gradle Configuration]

#### [MODIFY] [libs.versions.toml](file:///D:/Document/K%C3%AC%20h%C3%A8/Android/Task-Management-App/Code/TaskManagementApp/gradle/libs.versions.toml)
- Thêm các version cho: Kotlin, KSP, Room, Lifecycle, RecyclerView, Coroutines.
- Thêm các định nghĩa library và plugin tương ứng.

#### [MODIFY] [build.gradle.kts (root)](file:///D:/Document/K%C3%AC%20h%C3%A8/Android/Task-Management-App/Code/TaskManagementApp/build.gradle.kts)
- Khai báo các plugin Kotlin và KSP.

#### [MODIFY] [app/build.gradle.kts](file:///D:/Document/K%C3%AC%20h%C3%A8/Android/Task-Management-App/Code/TaskManagementApp/app/build.gradle.kts)
- Áp dụng plugin Kotlin và KSP.
- Thêm các dependencies vào block `dependencies`.

## Verification Plan

### Automated Tests
- Chạy `gradle sync` để đảm bảo tất cả các dependencies được tải về và cấu hình thành công.
- Build dự án để kiểm tra lỗi compilation.
