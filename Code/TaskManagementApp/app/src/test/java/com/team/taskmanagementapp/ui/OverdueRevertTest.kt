package com.team.taskmanagementapp.ui

import com.team.taskmanagementapp.data.model.enums.TaskStatus
import com.team.taskmanagementapp.ui.viewmodel.AddEditTaskViewModel
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Unit tests cho logic overdue detection & revert (Task 4+5 – TMA-36).
 *
 * Kiểm tra hàm [AddEditTaskViewModel.resolveStatusOnUpdate] với 6 cases
 * theo Acceptance Criteria.
 *
 * Không cần DB, coroutines hay mock vì hàm là pure function (internal).
 */
class OverdueRevertTest {

    private var now: Long = 0L
    // Timestamps tương đối để test luôn deterministic
    private val past: Long   get() = now - 3_600_000L   // 1 giờ trước
    private val future: Long get() = now + 86_400_000L  // 24 giờ sau

    // Stub trực tiếp AddEditTaskViewModel – resolveStatusOnUpdate không
    // sử dụng repository nên có thể truyền stub.
    private lateinit var viewModel: AddEditTaskViewModel

    @Before
    fun setup() {
        now = System.currentTimeMillis()
        viewModel = AddEditTaskViewModel(FakeTaskRepository())
    }

    // ─── Case 1 ────────────────────────────────────────────────────────────────
    /**
     * TODO + deadline quá khứ → giữ nguyên TODO (batch job DAO lo việc OVERDUE).
     * resolveStatusOnUpdate không tự đánh OVERDUE; đó là trách nhiệm của DAO batch.
     */
    @Test
    fun `case1 - TODO task with past due date keeps status from UI`() {
        val result = viewModel.resolveStatusOnUpdate(
            currentStatus = TaskStatus.TODO,
            newStatus     = TaskStatus.TODO,
            combinedDueTimestamp = past
        )
        assertEquals("TODO + past dueDate → giữ nguyên TODO", TaskStatus.TODO, result)
    }

    // ─── Case 2 ────────────────────────────────────────────────────────────────
    /** IN_PROGRESS + deadline quá khứ → giữ nguyên IN_PROGRESS. */
    @Test
    fun `case2 - IN_PROGRESS task with past due date keeps status from UI`() {
        val result = viewModel.resolveStatusOnUpdate(
            currentStatus = TaskStatus.IN_PROGRESS,
            newStatus     = TaskStatus.IN_PROGRESS,
            combinedDueTimestamp = past
        )
        assertEquals("IN_PROGRESS + past dueDate → giữ nguyên", TaskStatus.IN_PROGRESS, result)
    }

    // ─── Case 3 ────────────────────────────────────────────────────────────────
    /** COMPLETED + deadline quá khứ → vẫn COMPLETED. Rule 1 bảo vệ tuyệt đối. */
    @Test
    fun `case3 - COMPLETED task with past due date stays COMPLETED`() {
        val result = viewModel.resolveStatusOnUpdate(
            currentStatus = TaskStatus.COMPLETED,
            newStatus     = TaskStatus.OVERDUE,   // UI truyền sai → phải bị bỏ qua
            combinedDueTimestamp = past
        )
        assertEquals("COMPLETED không bao giờ bị override", TaskStatus.COMPLETED, result)
    }

    // ─── Case 4 ────────────────────────────────────────────────────────────────
    /** OVERDUE + user đổi dueDate về tương lai → revert về TODO. Core logic Task 4. */
    @Test
    fun `case4 - OVERDUE task updated to future due date reverts to TODO`() {
        val result = viewModel.resolveStatusOnUpdate(
            currentStatus = TaskStatus.OVERDUE,
            newStatus     = TaskStatus.OVERDUE,
            combinedDueTimestamp = future
        )
        assertEquals("OVERDUE + future dueDate → revert TODO", TaskStatus.TODO, result)
    }

    // ─── Case 5 ────────────────────────────────────────────────────────────────
    /** COMPLETED + dueDate đổi về tương lai → vẫn COMPLETED. Rule 1. */
    @Test
    fun `case5 - COMPLETED task updated to future due date stays COMPLETED`() {
        val result = viewModel.resolveStatusOnUpdate(
            currentStatus = TaskStatus.COMPLETED,
            newStatus     = TaskStatus.COMPLETED,
            combinedDueTimestamp = future
        )
        assertEquals("COMPLETED + future dueDate → vẫn COMPLETED", TaskStatus.COMPLETED, result)
    }

    // ─── Case 6 ────────────────────────────────────────────────────────────────
    /** TODO + dueDate tương lai → không tự đổi status. */
    @Test
    fun `case6 - TODO task with future due date is not changed`() {
        val result = viewModel.resolveStatusOnUpdate(
            currentStatus = TaskStatus.TODO,
            newStatus     = TaskStatus.TODO,
            combinedDueTimestamp = future
        )
        assertEquals("TODO + future dueDate → giữ nguyên TODO", TaskStatus.TODO, result)
    }

    // ─── Bonus ─────────────────────────────────────────────────────────────────
    /** OVERDUE + dueDate vẫn quá khứ → giữ nguyên OVERDUE (không revert). */
    @Test
    fun `bonus - OVERDUE task with still-past due date stays OVERDUE`() {
        val result = viewModel.resolveStatusOnUpdate(
            currentStatus = TaskStatus.OVERDUE,
            newStatus     = TaskStatus.OVERDUE,
            combinedDueTimestamp = past
        )
        assertEquals("OVERDUE + past dueDate → không revert", TaskStatus.OVERDUE, result)
    }
}
