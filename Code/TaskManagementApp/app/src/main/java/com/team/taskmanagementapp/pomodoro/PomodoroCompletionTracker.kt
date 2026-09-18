package com.team.taskmanagementapp.pomodoro

/**
 * Đảm bảo mỗi phiên Pomodoro chỉ phát Sound / Vibration / Completion Notification **đúng một lần**.
 *
 * ## Vì sao cần lớp này
 * Việc phát hiện "phiên đã kết thúc" có thể đến từ nhiều nguồn cùng lúc:
 * - vòng lặp `tick()` mỗi giây của [PomodoroService];
 * - alarm đánh thức đúng mốc kết thúc khi thiết bị đang Doze (`ACTION_SESSION_END`);
 * - mỗi lệnh từ notification cũng gọi `tick()` một lần.
 *
 * Tất cả các nguồn đó đều dựa vào `PomodoroSnapshot.completionId` — bộ đếm tăng đúng một lần
 * cho mỗi phiên kết thúc (được tạo ở tầng state machine). Lớp này ghi nhớ `completionId` đã
 * xử lý, nên dù `tick()` bị gọi lặp lại thì cảnh báo vẫn chỉ phát một lần.
 *
 * ## Phiên bị skip
 * `completionId` cũng tăng khi người dùng bấm Skip, nhưng bản ghi có `isCompleted = false`.
 * Trường hợp đó **không** phát cảnh báo (người dùng đang chủ động thao tác) — nhưng vẫn được
 * đánh dấu đã xử lý để phiên kết thúc thật sau đó vẫn kích hoạt đúng.
 *
 * Class thuần Kotlin (không phụ thuộc Android) nên test được trên JVM.
 */
class PomodoroCompletionTracker {

    private var lastHandledCompletionId: Int = 0

    /**
     * Đồng bộ mốc `completionId` hiện tại mà **không** phát cảnh báo.
     *
     * Dùng khi Service được tạo lại: các phiên đã kết thúc trước đó không được phát lại
     * cảnh báo, chỉ những phiên kết thúc SAU thời điểm này mới kích hoạt.
     */
    fun syncTo(completionId: Int) {
        lastHandledCompletionId = completionId
    }

    /**
     * Trả về bản ghi nếu có một phiên MỚI vừa chạy hết giờ, ngược lại `null`.
     *
     * Hàm tự đánh dấu đã xử lý, nên gọi bao nhiêu lần với cùng một snapshot cũng chỉ
     * trả về bản ghi ở lần gọi đầu tiên.
     */
    fun consumeCompletedSession(snapshot: PomodoroSnapshot): CompletedSessionRecord? {
        if (snapshot.completionId == lastHandledCompletionId) return null

        // Đánh dấu đã xử lý kể cả khi phiên bị skip, để lần kết thúc thật sau đó vẫn được phát.
        lastHandledCompletionId = snapshot.completionId

        val record = snapshot.lastCompletedSession ?: return null
        return record.takeIf { it.isCompleted }
    }
}
