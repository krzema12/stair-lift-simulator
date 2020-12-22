import com.soywiz.klock.DateTime
import com.soywiz.klock.TimeProvider
import com.soywiz.klock.seconds
import kotlin.math.sign

data class SensorInputs(
        val isUpperKeyEnabled: Boolean,
        val isLowerKeyEnabled: Boolean,
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
    var state: State = State.Parked
    private set

    sealed class State {
        abstract fun transitionToNextState(sensorInputs: SensorInputs): State

        object Parked : State() {
            override fun transitionToNextState(sensorInputs: SensorInputs) =
                    when {
                        sensorInputs.isUpperKeyEnabled -> WaitingAfterTurningOn(waitStart = TimeProvider.now(), enabledFrom = EnabledFrom.UpperKey)
                        sensorInputs.isLowerKeyEnabled -> WaitingAfterTurningOn(waitStart = TimeProvider.now(), enabledFrom = EnabledFrom.LowerKey)
                        else -> this
                    }
        }

        data class WaitingAfterTurningOn(val waitStart: DateTime,
                                         val enabledFrom: EnabledFrom) : State() {
            override fun transitionToNextState(sensorInputs: SensorInputs) =
                    if (TimeProvider.now() - waitStart < 3.seconds) {
                        this
                    } else {
                        when (enabledFrom) {
                            EnabledFrom.UpperKey -> GoingUpWithoutWheelchair
                            EnabledFrom.LowerKey -> PlatformUnfolding(enabledFrom = enabledFrom)
                        }
                    }
        }

        data class PlatformUnfolding(val enabledFrom: EnabledFrom) : State() {
            override fun transitionToNextState(sensorInputs: SensorInputs) =
                    if (sensorInputs.foldablePlatformPositionNormalized < 1.0f) {
                        this
                    } else {
                        PreparingBothFlapsForEnteringWheelchair(enabledFrom = enabledFrom)
                    }
        }

        data class PreparingBothFlapsForEnteringWheelchair(val enabledFrom: EnabledFrom) : State() {
            override fun transitionToNextState(sensorInputs: SensorInputs): State {
                val higherFlapTargetReached = when (enabledFrom) {
                    EnabledFrom.UpperKey -> sensorInputs.higherFlapPositionNormalized > 1.0f
                    EnabledFrom.LowerKey -> sensorInputs.higherFlapPositionNormalized in 0.45f..0.55f
                }
                val lowerFlapTargetReached = when (enabledFrom) {
                    EnabledFrom.UpperKey -> sensorInputs.lowerFlapPositionNormalized in 0.45f..0.55f
                    EnabledFrom.LowerKey -> sensorInputs.lowerFlapPositionNormalized > 1.0f
                }
                return if (!lowerFlapTargetReached || !higherFlapTargetReached) {
                    this
                } else {
                    WaitingForWheelchair
                }
            }
        }

        object GoingUpWithoutWheelchair : State() {
            override fun transitionToNextState(sensorInputs: SensorInputs) =
                    if (sensorInputs.mainMotorPositionNormalized < 1.0f) {
                        this
                    } else {
                        PlatformUnfolding(enabledFrom = EnabledFrom.UpperKey)
                    }
        }

        object WaitingForWheelchair : State() {
            override fun transitionToNextState(sensorInputs: SensorInputs) = if (!sensorInputs.isWheelchairPresent) {
                this
            } else {
                PreparingForDrivingWithWheelchair
            }
        }

        object PreparingForDrivingWithWheelchair : State() {
            override fun transitionToNextState(sensorInputs: SensorInputs) = if (sensorInputs.barriersPositionNormalized < 1.0f ||
                    sensorInputs.lowerFlapPositionNormalized !in 0.45f..0.55f ||
                    sensorInputs.higherFlapPositionNormalized !in 0.45f..0.55f) {
                this
            } else {
                DrivingWithWheelchair
            }
        }

        object DrivingWithWheelchair : State() {
            override fun transitionToNextState(sensorInputs: SensorInputs) = if (sensorInputs.mainMotorPositionNormalized < 0.0f && sensorInputs.goingDownButtonPressed ||
                    sensorInputs.mainMotorPositionNormalized > 1.0f && sensorInputs.goingUpButtonPressed) {
                PreparingForWheelchairLeaving
            } else {
                this
            }
        }

        object PreparingForWheelchairLeaving : State() {
            override fun transitionToNextState(sensorInputs: SensorInputs): State {
                val positionOfFlapDependingOnWheelchairPlace = if (sensorInputs.mainMotorPositionNormalized < 0.0f) {
                    sensorInputs.lowerFlapPositionNormalized
                } else {
                    sensorInputs.higherFlapPositionNormalized
                }
                return if (sensorInputs.barriersPositionNormalized > 0.0f || positionOfFlapDependingOnWheelchairPlace < 1.0f) {
                    this
                } else {
                    WaitingForWheelchairLeaving
                }
            }
        }

        object WaitingForWheelchairLeaving : State() {
            override fun transitionToNextState(sensorInputs: SensorInputs) = if (sensorInputs.isWheelchairPresent) {
                this
            } else {
                FoldingBothFlapsBeforeParking
            }
        }

        object FoldingBothFlapsBeforeParking : State() {
            override fun transitionToNextState(sensorInputs: SensorInputs) = if (sensorInputs.lowerFlapPositionNormalized > 0.0f || sensorInputs.higherFlapPositionNormalized > 0.0f) {
                this
            } else {
                FoldingPlatformBeforeParking
            }
        }

        object FoldingPlatformBeforeParking : State() {
            override fun transitionToNextState(sensorInputs: SensorInputs) = if (sensorInputs.foldablePlatformPositionNormalized > 0.0f) {
                this
            } else {
                if (sensorInputs.mainMotorPositionNormalized > 1.0f) {
                    GoingDownToPark
                } else {
                    Parked
                }
            }
        }

        object GoingDownToPark : State() {
            override fun transitionToNextState(sensorInputs: SensorInputs) = if (sensorInputs.mainMotorPositionNormalized > 0.0f) {
                this
            } else {
                Parked
            }
        }
    }

    enum class EnabledFrom {
        UpperKey,
        LowerKey,
    }

    fun run(sensorInputs: SensorInputs): ActuatorOutputs {
        state = state.transitionToNextState(sensorInputs)
        return getActuatorOutputs(sensorInputs)
    }

    /**
     * Focuses on what happens in a given state.
     */
    private fun getActuatorOutputs(sensorInputs: SensorInputs): ActuatorOutputs {
        return when (val currentState = state) {
            State.Parked -> ActuatorOutputs()
            is State.WaitingAfterTurningOn -> ActuatorOutputs()
            is State.PlatformUnfolding -> ActuatorOutputs(foldablePlatformUnfoldingSpeed = 0.2f)
            is State.PreparingBothFlapsForEnteringWheelchair -> ActuatorOutputs(
                    lowerFlapUnfoldingSpeed = when (currentState.enabledFrom) {
                        EnabledFrom.UpperKey -> (sensorInputs.lowerFlapPositionNormalized - 0.5f).sign * -0.2f
                        EnabledFrom.LowerKey -> if (sensorInputs.lowerFlapPositionNormalized < 1.0f) 0.2f else 0.0f
                    },
                    higherFlapUnfoldingSpeed = when (currentState.enabledFrom) {
                        EnabledFrom.UpperKey -> if (sensorInputs.higherFlapPositionNormalized < 1.0f) 0.2f else 0.0f
                        EnabledFrom.LowerKey -> (sensorInputs.higherFlapPositionNormalized - 0.5f).sign * -0.2f
                    }
            )
            State.GoingUpWithoutWheelchair -> ActuatorOutputs(mainMotorSpeed = 0.2f)
            State.WaitingForWheelchair -> ActuatorOutputs()
            State.PreparingForDrivingWithWheelchair -> ActuatorOutputs(
                    barriersUnfoldingSpeed = if (sensorInputs.barriersPositionNormalized < 1.0f) 0.2f else 0.0f,
                    lowerFlapUnfoldingSpeed = (sensorInputs.lowerFlapPositionNormalized - 0.5f).sign * -0.2f,
                    higherFlapUnfoldingSpeed = (sensorInputs.higherFlapPositionNormalized - 0.5f).sign * -0.2f,
            )
            State.DrivingWithWheelchair -> ActuatorOutputs(mainMotorSpeed = when {
                sensorInputs.goingUpButtonPressed -> 0.2f
                sensorInputs.goingDownButtonPressed -> -0.2f
                else -> 0.0f
            })
            State.PreparingForWheelchairLeaving -> {
                val actuatorOutputsWithoutFlaps = ActuatorOutputs(
                        barriersUnfoldingSpeed = if (sensorInputs.barriersPositionNormalized > 0.0f) -0.2f else 0.0f)
                if (sensorInputs.mainMotorPositionNormalized < 0.0f) {
                    actuatorOutputsWithoutFlaps.copy(
                            lowerFlapUnfoldingSpeed = if (sensorInputs.lowerFlapPositionNormalized < 1.0f) 0.2f else 0.0f)
                } else {
                    actuatorOutputsWithoutFlaps.copy(
                            higherFlapUnfoldingSpeed = if (sensorInputs.higherFlapPositionNormalized < 1.0f) 0.2f else 0.0f)
                }
            }
            State.WaitingForWheelchairLeaving -> ActuatorOutputs()
            State.FoldingBothFlapsBeforeParking -> ActuatorOutputs(
                    higherFlapUnfoldingSpeed = if (sensorInputs.higherFlapPositionNormalized > 0.0f) -0.2f else 0.0f,
                    lowerFlapUnfoldingSpeed = if (sensorInputs.lowerFlapPositionNormalized > 0.0f) -0.2f else 0.0f)
            State.FoldingPlatformBeforeParking -> ActuatorOutputs(
                    foldablePlatformUnfoldingSpeed = if (sensorInputs.foldablePlatformPositionNormalized > 0.0f) -0.2f else 0.0f)
            State.GoingDownToPark -> ActuatorOutputs(
                    mainMotorSpeed = if (sensorInputs.mainMotorPositionNormalized > 0.0f) -0.2f else 0.0f)
        }
    }
}
