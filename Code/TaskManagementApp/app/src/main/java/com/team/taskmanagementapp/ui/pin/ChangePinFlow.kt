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

    /**
     * The new PIN entered in [Step.ENTER_NEW], held until confirmed in [Step.CONFIRM_NEW].
     * Exposed as `internal` so [PinLockActivity] can persist and restore it across
     * configuration changes via [restoreState]. Must NOT be shared outside the ui.pin package.
     */
    internal var pendingNewPin: String? = null
        private set

    /**
     * Atomically restores saved state after a configuration change.
     * Called from [PinLockActivity.onRestoreInstanceState] – never from normal flow paths.
     */
    internal fun restoreState(step: Step, pendingNewPin: String?) {
        this.step = step
        this.pendingNewPin = pendingNewPin
    }

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
