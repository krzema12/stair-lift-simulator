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
        when (state) {
            State.Parked -> {
                return if (sensorInputs.isKeyEnabled) {
                    state = State.PlatformUnfolding
                    run(sensorInputs)
                } else {
                    ActuatorOutputs()
                }
            }
            State.PlatformUnfolding -> {
                return if (sensorInputs.foldablePlatformPositionNormalized < 1.0f) {
                    ActuatorOutputs(foldablePlatformUnfoldingSpeed = 0.2f)
                } else {
                    state = State.UnfoldingBothFlaps
                    run(sensorInputs)
                }
            }
            State.UnfoldingBothFlaps -> {
                return if (sensorInputs.lowerFlapPositionNormalized < 1.0f || sensorInputs.higherFlapPositionNormalized < 1.0f) {
                    ActuatorOutputs(
                            lowerFlapUnfoldingSpeed = if (sensorInputs.lowerFlapPositionNormalized < 1.0f) 0.2f else 0.0f,
                            higherFlapUnfoldingSpeed = if (sensorInputs.higherFlapPositionNormalized < 1.0f) 0.2f else 0.0f,
                    )
                } else {
                    state = State.WaitingForWheelchair
                    run(sensorInputs)
                }
            }
            State.WaitingForWheelchair -> {
                return if (!sensorInputs.isWheelchairPresent) {
                    ActuatorOutputs()
                } else {
                    state = State.UnfoldingBarriers
                    run(sensorInputs)
                }
            }
            State.UnfoldingBarriers -> {
                return if (sensorInputs.barriersPositionNormalized < 1.0f) {
                    ActuatorOutputs(barriersUnfoldingSpeed = 0.2f)
                } else {
                    state = State.GoingUp
                    run(sensorInputs)
                }
            }
            State.GoingUp -> {
                return if (sensorInputs.mainMotorPositionNormalized < 1.0f) {
                    ActuatorOutputs(mainMotorSpeed = if (sensorInputs.goingUpButtonPressed) 0.2f else 0.0f)
                } else {
                    state = State.FoldingBarriers
                    run(sensorInputs)
                }
            }
            State.FoldingBarriers -> {
                return if (sensorInputs.barriersPositionNormalized > 0.0f) {
                    ActuatorOutputs(barriersUnfoldingSpeed = -0.2f)
                } else {
                    state = State.WaitingForWheelchairLeaving
                    run(sensorInputs)
                }
            }
            State.WaitingForWheelchairLeaving -> {
                return if (sensorInputs.isWheelchairPresent) {
                    ActuatorOutputs()
                } else {
                    state = State.GoingDown
                    run(sensorInputs)
                }
            }
            State.GoingDown -> {
                return if (sensorInputs.mainMotorPositionNormalized > 0.0f) {
                    ActuatorOutputs(mainMotorSpeed = -0.2f)
                } else {
                    state = State.GoingUp
                    run(sensorInputs)
                }
            }
        }
    }
}

