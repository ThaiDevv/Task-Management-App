package com.team.taskmanagementapp.ui.pin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChangePinFlowTest {

    @Test
    fun `wrong current PIN does not advance the flow`() {
        val flow = ChangePinFlow()

        val result = flow.submit("0000") { false }

        assertTrue(result is ChangePinFlow.Submission.CurrentPinRejected)
        assertEquals(ChangePinFlow.Step.VERIFY_CURRENT, flow.step)
    }

    @Test
    fun `correct current PIN followed by matching new PIN completes the flow`() {
        val flow = ChangePinFlow()

        val verified = flow.submit("1234") { it == "1234" }
        assertTrue(verified is ChangePinFlow.Submission.AwaitingNewPin)
        assertEquals(ChangePinFlow.Step.ENTER_NEW, flow.step)

        val entered = flow.submit("5678") { false }
        assertTrue(entered is ChangePinFlow.Submission.AwaitingConfirmation)
        assertEquals(ChangePinFlow.Step.CONFIRM_NEW, flow.step)

        val completed = flow.submit("5678") { false }
        assertTrue(completed is ChangePinFlow.Submission.Completed)
        assertEquals("5678", (completed as ChangePinFlow.Submission.Completed).newPin)
    }

    @Test
    fun `mismatched confirmation returns to new PIN step and supports retry`() {
        val flow = ChangePinFlow()
        flow.submit("1234") { true }
        flow.submit("5678") { false }

        val mismatch = flow.submit("9999") { false }

        assertTrue(mismatch is ChangePinFlow.Submission.NewPinMismatch)
        assertEquals(ChangePinFlow.Step.ENTER_NEW, flow.step)

        flow.submit("2468") { false }
        val completed = flow.submit("2468") { false }
        assertEquals("2468", (completed as ChangePinFlow.Submission.Completed).newPin)
    }
}
