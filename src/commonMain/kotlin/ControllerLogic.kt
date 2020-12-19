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
    var state = ControllerState(
            generalState = GeneralState.Parked,
    )
    private set

    enum class GeneralState {
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

    data class ControllerState(
            val generalState: GeneralState,
    )

    fun run(sensorInputs: SensorInputs): ActuatorOutputs {
        when (state.generalState) {
            GeneralState.Parked -> {
                return if (sensorInputs.isKeyEnabled) {
                    state = state.copy(generalState = GeneralState.PlatformUnfolding)
                    run(sensorInputs)
                } else {
                    ActuatorOutputs()
                }
            }
            GeneralState.PlatformUnfolding -> {
                return if (sensorInputs.foldablePlatformPositionNormalized < 1.0f) {
                    ActuatorOutputs(foldablePlatformUnfoldingSpeed = 0.2f)
                } else {
                    state = state.copy(generalState = GeneralState.UnfoldingBothFlaps)
                    run(sensorInputs)
                }
            }
            GeneralState.UnfoldingBothFlaps -> {
                return if (sensorInputs.lowerFlapPositionNormalized < 1.0f || sensorInputs.higherFlapPositionNormalized < 1.0f) {
                    ActuatorOutputs(
                            lowerFlapUnfoldingSpeed = if (sensorInputs.lowerFlapPositionNormalized < 1.0f) 0.2f else 0.0f,
                            higherFlapUnfoldingSpeed = if (sensorInputs.higherFlapPositionNormalized < 1.0f) 0.2f else 0.0f,
                    )
                } else {
                    state = state.copy(generalState = GeneralState.WaitingForWheelchair)
                    run(sensorInputs)
                }
            }
            GeneralState.WaitingForWheelchair -> {
                return if (!sensorInputs.isWheelchairPresent) {
                    ActuatorOutputs()
                } else {
                    state = state.copy(generalState = GeneralState.UnfoldingBarriers)
                    run(sensorInputs)
                }
            }
            GeneralState.UnfoldingBarriers -> {
                return if (sensorInputs.barriersPositionNormalized < 1.0f) {
                    ActuatorOutputs(barriersUnfoldingSpeed = 0.2f)
                } else {
                    state = state.copy(generalState = GeneralState.GoingUp)
                    run(sensorInputs)
                }
            }
            GeneralState.GoingUp -> {
                return if (sensorInputs.mainMotorPositionNormalized < 1.0f) {
                    ActuatorOutputs(mainMotorSpeed = if (sensorInputs.goingUpButtonPressed) 0.2f else 0.0f)
                } else {
                    state = state.copy(generalState = GeneralState.FoldingBarriers)
                    run(sensorInputs)
                }
            }
            GeneralState.FoldingBarriers -> {
                return if (sensorInputs.barriersPositionNormalized > 0.0f) {
                    ActuatorOutputs(barriersUnfoldingSpeed = -0.2f)
                } else {
                    state = state.copy(generalState = GeneralState.WaitingForWheelchairLeaving)
                    run(sensorInputs)
                }
            }
            GeneralState.WaitingForWheelchairLeaving -> {
                return if (sensorInputs.isWheelchairPresent) {
                    ActuatorOutputs()
                } else {
                    state = state.copy(generalState = GeneralState.GoingDown)
                    run(sensorInputs)
                }
            }
            GeneralState.GoingDown -> {
                return if (sensorInputs.mainMotorPositionNormalized > 0.0f) {
                    ActuatorOutputs(mainMotorSpeed = -0.2f)
                } else {
                    state = state.copy(generalState = GeneralState.GoingUp)
                    run(sensorInputs)
                }
            }
        }
    }
}

