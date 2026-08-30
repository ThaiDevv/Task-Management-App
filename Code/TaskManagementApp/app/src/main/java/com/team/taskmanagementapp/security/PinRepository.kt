package com.team.taskmanagementapp.security

/**
 * PinRepository — Contract (Interface) cho tất cả thao tác liên quan đến mã PIN.
 *
 * Tách thành interface để:
 * 1. Các ViewModel, Fragment dùng contract — không phụ thuộc trực tiếp vào implementation.
 * 2. Dễ dàng Mock khi viết Unit Test.
 * 3. Các thành viên khác (TASK-47, 48, 49) có thể dùng interface này ngay cả khi
 *    implementation chưa hoàn chỉnh.
 */
interface PinRepository {

    /**
     * Kiểm tra PIN đã được bật (thiết lập) chưa.
     * @return true nếu PIN đã được thiết lập và kích hoạt.
     */
    fun isPinEnabled(): Boolean

    /**
     * Lưu mã PIN mới với bảo mật (hash + salt). Tự động bật PIN khi gọi hàm này.
     * @param pin Chuỗi mã PIN (4–6 chữ số).
     */
    fun setPin(pin: String)

    /**
     * Xác thực mã PIN người dùng nhập.
     * @param inputPin Chuỗi PIN người dùng nhập vào.
     * @return true nếu PIN khớp.
     */
    fun verifyPin(inputPin: String): Boolean

    /**
     * Xóa mã PIN và tắt tính năng khóa PIN.
     */
    fun clearPin()

    /**
     * Bật/Tắt tính năng khóa PIN mà không xóa hash PIN đã lưu.
     * Hữu ích khi user muốn tạm tắt nhưng không muốn nhập lại PIN mới.
     * @param enabled true để bật, false để tắt.
     */
    fun setPinEnabled(enabled: Boolean)

    /**
     * Ghi nhận một lần nhập PIN sai.
     * @return Số lần nhập sai hiện tại.
     */
    fun recordFailedAttempt(): Int

    /**
     * Lấy số lần nhập PIN sai hiện tại.
     */
    fun getFailedAttempts(): Int

    /**
     * Kiểm tra xem tài khoản có đang bị khóa tạm thời (lockout) do nhập sai quá nhiều lần không.
     * @return true nếu đang trong thời gian lockout.
     */
    fun isLockedOut(): Boolean

    /**
     * Lấy thời điểm lockout kết thúc (epoch millis). 0L nếu không bị lockout.
     */
    fun getLockoutEndTime(): Long

    /**
     * Reset số lần nhập sai và xóa lockout.
     */
    fun resetFailedAttempts()
}
