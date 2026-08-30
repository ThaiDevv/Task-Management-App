package com.team.taskmanagementapp.ui.pin

/**
 * Pure state machine for the three-step change-PIN flow.
 * Keeping transitions independent from Android UI makes every acceptance path unit-testable.
 */
internal class ChangePinFlow {

    enum class Step {
        VERIFY_CURRENT,
        ENTER_NEW,
        CONFIRM_NEW
    }

    sealed class Submission {
        data object CurrentPinRejected : Submission()
        data object AwaitingNewPin : Submission()
        data object AwaitingConfirmation : Submission()
        data object NewPinMismatch : Submission()
        data class Completed(val newPin: String) : Submission()
    }

    var step: Step = Step.VERIFY_CURRENT
        private set

    private var pendingNewPin: String? = null

    fun submit(pin: String, verifyCurrentPin: (String) -> Boolean): Submission = when (step) {
        Step.VERIFY_CURRENT -> {
            if (!verifyCurrentPin(pin)) {
                Submission.CurrentPinRejected
            } else {
                step = Step.ENTER_NEW
                Submission.AwaitingNewPin
            }
        }

        Step.ENTER_NEW -> {
            pendingNewPin = pin
            step = Step.CONFIRM_NEW
            Submission.AwaitingConfirmation
        }

        Step.CONFIRM_NEW -> {
            val newPin = requireNotNull(pendingNewPin)
            if (pin == newPin) {
                Submission.Completed(newPin)
            } else {
                pendingNewPin = null
                step = Step.ENTER_NEW
                Submission.NewPinMismatch
            }
        }
    }
}
