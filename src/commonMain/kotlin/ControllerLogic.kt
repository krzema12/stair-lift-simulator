data class SensorInputs(
        val mainMotorPositionNormalized: Float,
)

data class ActuatorOutputs(
        val mainMotorSpeed: Float,
)

class ControllerLogic {
    var state = ControllerState(
            generalState = GeneralState.GoingUp,
    )
    private set

    enum class GeneralState {
        GoingUp,
        GoingDown,
    }

    data class ControllerState(
            val generalState: GeneralState,
    )

    fun run(sensorInputs: SensorInputs): ActuatorOutputs {
        when (state.generalState) {
            GeneralState.GoingUp -> {
                return if (sensorInputs.mainMotorPositionNormalized < 1.0f) {
                    ActuatorOutputs(
                            mainMotorSpeed = 0.2f,
                    )
                } else {
                    state = state.copy(generalState = GeneralState.GoingDown)
                    run(sensorInputs)
                }
            }
            GeneralState.GoingDown -> {
                return if (sensorInputs.mainMotorPositionNormalized > 0.0f) {
                    ActuatorOutputs(
                            mainMotorSpeed = -0.2f,
                    )
                } else {
                    state = state.copy(generalState = GeneralState.GoingUp)
                    run(sensorInputs)
                }
            }
        }
    }
}

