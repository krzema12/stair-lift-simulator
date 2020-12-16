data class SensorInputs(
        val mainMotorEncoder: Int,
)

data class ActuatorOutputs(
        val mainMotorSpeed: Float,
)

class ControllerLogic() {
    private var state = State(
            generalState = GeneralState.GoingUp,
    )

    private enum class GeneralState { GoingUp, GoingDown }

    private data class State(
            val generalState: GeneralState,
    )

    fun run(sensorInputs: SensorInputs): ActuatorOutputs {
        println(sensorInputs)

        when (state.generalState) {
            GeneralState.GoingUp -> {
                return if (sensorInputs.mainMotorEncoder < 2000) {
                    ActuatorOutputs(
                            mainMotorSpeed = 0.5f,
                    )
                } else {
                    state = state.copy(generalState = GeneralState.GoingDown)
                    ActuatorOutputs(
                            mainMotorSpeed = -0.5f,
                    )
                }
            }
            GeneralState.GoingDown -> {
                return if (sensorInputs.mainMotorEncoder > -2000) {
                    ActuatorOutputs(
                            mainMotorSpeed = -0.5f,
                    )
                } else {
                    state = state.copy(generalState = GeneralState.GoingUp)
                    ActuatorOutputs(
                            mainMotorSpeed = 0.5f,
                    )
                }
            }
        }
    }
}

