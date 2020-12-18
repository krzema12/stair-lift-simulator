data class SensorInputs(
        val isKeyEnabled: Boolean,
        val foldablePlatformPositionNormalized: Float,
        val lowerFlapPositionNormalized: Float,
        val mainMotorPositionNormalized: Float,
)

data class ActuatorOutputs(
        val foldablePlatformUnfoldingSpeed: Float,
        val lowerFlapUnfoldingSpeed: Float,
        val mainMotorSpeed: Float,
)

class ControllerLogic {
    var state = ControllerState(
            generalState = GeneralState.Parked,
    )
    private set

    enum class GeneralState {
        Parked,
        PlatformUnfolding,
        UnfoldingLowerFlap,
        WaitingForWheelchair,
        GoingUp,
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
                    ActuatorOutputs(
                            mainMotorSpeed = 0.0f,
                            foldablePlatformUnfoldingSpeed = 0.0f,
                            lowerFlapUnfoldingSpeed = 0.0f,
                    )
                }
            }
            GeneralState.PlatformUnfolding -> {
                return if (sensorInputs.foldablePlatformPositionNormalized < 1.0f) {
                    ActuatorOutputs(
                            mainMotorSpeed = 0.0f,
                            foldablePlatformUnfoldingSpeed = 0.2f,
                            lowerFlapUnfoldingSpeed = 0.0f,
                    )
                } else {
                    state = state.copy(generalState = GeneralState.UnfoldingLowerFlap)
                    run(sensorInputs)
                }
            }
            GeneralState.UnfoldingLowerFlap -> {
                return if (sensorInputs.lowerFlapPositionNormalized < 1.0f) {
                    ActuatorOutputs(
                            mainMotorSpeed = 0.0f,
                            foldablePlatformUnfoldingSpeed = 0.0f,
                            lowerFlapUnfoldingSpeed = 0.2f,
                    )
                } else {
                    state = state.copy(generalState = GeneralState.WaitingForWheelchair)
                    run(sensorInputs)
                }
            }
            GeneralState.WaitingForWheelchair -> {
                return ActuatorOutputs(
                        mainMotorSpeed = 0.0f,
                        foldablePlatformUnfoldingSpeed = 0.0f,
                        lowerFlapUnfoldingSpeed = 0.0f,
                )
            }
            GeneralState.GoingUp -> {
                return if (sensorInputs.mainMotorPositionNormalized < 1.0f) {
                    ActuatorOutputs(
                            mainMotorSpeed = 0.2f,
                            foldablePlatformUnfoldingSpeed = 0.0f,
                            lowerFlapUnfoldingSpeed = 0.0f,
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
                            foldablePlatformUnfoldingSpeed = 0.0f,
                            lowerFlapUnfoldingSpeed = 0.0f,
                    )
                } else {
                    state = state.copy(generalState = GeneralState.GoingUp)
                    run(sensorInputs)
                }
            }
        }
    }
}

