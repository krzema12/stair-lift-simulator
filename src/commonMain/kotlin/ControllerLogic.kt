data class SensorInputs(
        val isKeyEnabled: Boolean,
        val goingUpButtonPressed: Boolean,
        val goingDownButtonPressed: Boolean,
        val foldablePlatformPositionNormalized: Float,
        val lowerFlapPositionNormalized: Float,
        val higherFlapPositionNormalized: Float,
        val isWheelchairPresent: Boolean,
        val barriersPositionNormalized: Float,
        val mainMotorPositionNormalized: Float,
)

data class ActuatorOutputs(
        val foldablePlatformUnfoldingSpeed: Float = 0.0f,
        val lowerFlapUnfoldingSpeed: Float = 0.0f,
        val barriersUnfoldingSpeed: Float = 0.0f,
        val mainMotorSpeed: Float = 0.0f,
        val higherFlapUnfoldingSpeed: Float = 0.0f,
)

class ControllerLogic {
    var state = State.Parked
    private set

    enum class State {
        Parked,
        PlatformUnfolding,
        UnfoldingBothFlaps,
        WaitingForWheelchair,
        UnfoldingBarriers,
        GoingUp,
        FoldingBarriers,
        WaitingForWheelchairLeaving,
        GoingDown,
    }

    fun run(sensorInputs: SensorInputs): ActuatorOutputs {
        state = getNewState(sensorInputs)
        return getActuatorOutputs(sensorInputs)
    }

    /**
     * Focuses on transitions between states.
     */
    private fun getNewState(sensorInputs: SensorInputs): State {
        return when (state) {
            State.Parked -> if (!sensorInputs.isKeyEnabled) {
                state
            } else {
                State.PlatformUnfolding
            }
            State.PlatformUnfolding -> if (sensorInputs.foldablePlatformPositionNormalized < 1.0f) {
                state
            } else {
                State.UnfoldingBothFlaps
            }
            State.UnfoldingBothFlaps -> if (sensorInputs.lowerFlapPositionNormalized < 1.0f || sensorInputs.higherFlapPositionNormalized < 1.0f) {
                state
            } else {
                State.WaitingForWheelchair
            }
            State.WaitingForWheelchair -> if (!sensorInputs.isWheelchairPresent) {
                state
            } else {
                State.UnfoldingBarriers
            }
            State.UnfoldingBarriers -> if (sensorInputs.barriersPositionNormalized < 1.0f) {
                state
            } else {
                State.GoingUp
            }
            State.GoingUp -> if (sensorInputs.mainMotorPositionNormalized < 1.0f) {
                state
            } else {
                State.FoldingBarriers
            }
            State.FoldingBarriers -> if (sensorInputs.barriersPositionNormalized > 0.0f) {
                state
            } else {
                State.WaitingForWheelchairLeaving
            }
            State.WaitingForWheelchairLeaving -> if (sensorInputs.isWheelchairPresent) {
                state
            } else {
                State.GoingDown
            }
            State.GoingDown -> if (sensorInputs.mainMotorPositionNormalized > 0.0f) {
                state
            } else {
                State.GoingUp
            }
        }
    }

    /**
     * Focuses on what happens in a given state.
     */
    private fun getActuatorOutputs(sensorInputs: SensorInputs): ActuatorOutputs {
        return when (state) {
            State.Parked -> ActuatorOutputs()
            State.PlatformUnfolding -> ActuatorOutputs(foldablePlatformUnfoldingSpeed = 0.2f)
            State.UnfoldingBothFlaps -> ActuatorOutputs(
                    lowerFlapUnfoldingSpeed = if (sensorInputs.lowerFlapPositionNormalized < 1.0f) 0.2f else 0.0f,
                    higherFlapUnfoldingSpeed = if (sensorInputs.higherFlapPositionNormalized < 1.0f) 0.2f else 0.0f,
            )
            State.WaitingForWheelchair -> ActuatorOutputs()
            State.UnfoldingBarriers -> ActuatorOutputs(barriersUnfoldingSpeed = 0.2f)
            State.GoingUp -> ActuatorOutputs(mainMotorSpeed = if (sensorInputs.goingUpButtonPressed) 0.2f else 0.0f)
            State.FoldingBarriers -> ActuatorOutputs(barriersUnfoldingSpeed = -0.2f)
            State.WaitingForWheelchairLeaving -> ActuatorOutputs()
            State.GoingDown -> ActuatorOutputs(mainMotorSpeed = -0.2f)
        }
    }
}

