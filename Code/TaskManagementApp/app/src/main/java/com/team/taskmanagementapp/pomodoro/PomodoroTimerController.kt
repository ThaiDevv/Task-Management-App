package com.team.taskmanagementapp.pomodoro

import kotlinx.coroutines.flow.StateFlow

/**
 * Nơi giữ **một instance duy nhất** của [PomodoroTimerEngine] cho toàn bộ process.
 *
 * Vì sao cần lớp này:
 * - `PomodoroService` là nơi *điều khiển* timer (giữ foreground, cập nhật notification).
 * - `PomodoroViewModel` / UI chỉ cần *quan sát* [state] và gửi lệnh qua `PomodoroService`.
 * - Cả hai phải nói về **cùng một** timer: nếu engine nằm trong Service thì UI buộc phải
 *   bind service; nếu engine nằm trong ViewModel thì timer sẽ bị tạo lại mỗi lần
 *   Activity/Fragment recreate (rotate, đổi theme, process recreation…).
 *
 * Do đó engine sống ở đây, còn Service chỉ là "driver" (tick mỗi giây + notification).
 * Timer **không** phụ thuộc vòng đời của bất kỳ Activity/Fragment nào.
 *
 * ⚠️ [PomodoroTimerEngine] là state machine đơn luồng (không `synchronized`).
 * Mọi truy cập (đọc hoặc điều khiển) phải thực hiện trên **main thread** — `PomodoroService`
 * đã tick trên `Dispatchers.Main.immediate`; ViewModel cũng phải gọi từ main thread.
 *
 * Cách dùng từ ViewModel (task UI sau này):
 * ```kotlin
 * val state: StateFlow<PomodoroSnapshot> = PomodoroTimerController.state
 * PomodoroService.start(context, taskId = task.id.toLong())
 * ```
 *
 * ## Process restart
 * Class này không giả định timer sống mãi. Khi process bị kill, state ở đây mất theo.
 * Task recovery sau này sẽ: đọc timer state đã persist → dựng lại [PomodoroTimerEngine]
 * đã ở đúng phiên/`targetEnd` → gọi [install] để thay thế instance hiện tại trước khi
 * khởi động lại `PomodoroService`. Đó là lý do tồn tại của [install].
 */
object PomodoroTimerController {

    @Volatile
    private var engineInstance: PomodoroTimerEngine = PomodoroTimerEngine()

    /** Engine hiện hành — dùng bởi `PomodoroService` để tick và điều khiển. */
    val engine: PomodoroTimerEngine
        get() = engineInstance

    /** Luồng trạng thái để ViewModel/UI observe (thay thế cho việc bind service). */
    val state: StateFlow<PomodoroSnapshot>
        get() = engineInstance.snapshot

    /** Ảnh chụp trạng thái hiện tại (đọc đồng bộ, ví dụ khi build notification). */
    val snapshot: PomodoroSnapshot
        get() = engineInstance.currentSnapshot

    /**
     * Thay engine hiện tại bằng một instance đã được dựng lại từ persisted state.
     *
     * Chỉ dùng cho task recovery sau này. Trước khi gọi cần đảm bảo
     * `PomodoroService` đã dừng (engine cũ không còn được tick).
     */
    fun install(restoredEngine: PomodoroTimerEngine) {
        engineInstance = restoredEngine
    }
}
