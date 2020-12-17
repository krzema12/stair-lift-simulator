data class SensorInputs(
        val mainMotorEncoder: Int,
)

data class ActuatorOutputs(
        val mainMotorSpeed: Float,
)

class ControllerLogic {
    var state = ControllerState(
            generalState = GeneralState.GoingUp,
    )
    private set

    companion object {
        const val EXTREME_MAIN_MOTOR_POSITION = 2000;
        const val MAIN_MOTOR_POSITION_SLOWDOWN_MARGIN = 200;
        const val MAIN_MOTOR_SPEED_FAST = 0.75f
        const val MAIN_MOTOR_SPEED_SLOW = 0.1f
    }

    enum class GeneralState {
        GoingUp,
        GoingUpApproachingTop,
        GoingDown,
        GoingDownApproachingBottom,
    }

    data class ControllerState(
            val generalState: GeneralState,
    )

    fun run(sensorInputs: SensorInputs): ActuatorOutputs {
        when (state.generalState) {
            GeneralState.GoingUp -> {
                return if (sensorInputs.mainMotorEncoder < EXTREME_MAIN_MOTOR_POSITION - MAIN_MOTOR_POSITION_SLOWDOWN_MARGIN) {
                    ActuatorOutputs(
                            mainMotorSpeed = MAIN_MOTOR_SPEED_FAST,
                    )
                } else {
                    state = state.copy(generalState = GeneralState.GoingUpApproachingTop)
                    run(sensorInputs)
                }
            }
            GeneralState.GoingUpApproachingTop -> {
                return if (sensorInputs.mainMotorEncoder < EXTREME_MAIN_MOTOR_POSITION) {
                    ActuatorOutputs(
                            mainMotorSpeed = MAIN_MOTOR_SPEED_SLOW,
                    )
                } else {
                    state = state.copy(generalState = GeneralState.GoingDown)
                    run(sensorInputs)
                }
            }
            GeneralState.GoingDown -> {
                return if (sensorInputs.mainMotorEncoder > -(EXTREME_MAIN_MOTOR_POSITION - MAIN_MOTOR_POSITION_SLOWDOWN_MARGIN)) {
                    ActuatorOutputs(
                            mainMotorSpeed = -MAIN_MOTOR_SPEED_FAST,
                    )
                } else {
                    state = state.copy(generalState = GeneralState.GoingDownApproachingBottom)
                    run(sensorInputs)
                }
            }
            GeneralState.GoingDownApproachingBottom -> {
                return if (sensorInputs.mainMotorEncoder > -EXTREME_MAIN_MOTOR_POSITION) {
                    ActuatorOutputs(
                            mainMotorSpeed = -MAIN_MOTOR_SPEED_SLOW,
                    )
                } else {
                    state = state.copy(generalState = GeneralState.GoingUp)
                    run(sensorInputs)
                }
            }
        }
    }
}

