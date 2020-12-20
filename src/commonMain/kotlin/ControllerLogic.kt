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
        object Parked : State()
        data class WaitingAfterTurningOn(val waitStart: DateTime,
                                         val enabledFrom: EnabledFrom) : State()
        data class PlatformUnfolding(val enabledFrom: EnabledFrom) : State()
        data class PreparingBothFlapsForEnteringWheelchair(val enabledFrom: EnabledFrom) : State()
        object GoingUpWithoutWheelchair : State()
        object WaitingForWheelchair : State()
        object PreparingForDrivingWithWheelchair : State()
        object Driving : State()
        object PreparingForWheelchairLeaving : State()
        object WaitingForWheelchairLeaving : State()
        object GoingDown : State()
    }

    enum class EnabledFrom {
        UpperKey,
        LowerKey,
    }

    fun run(sensorInputs: SensorInputs): ActuatorOutputs {
        state = transitionToNextState(sensorInputs)
        return getActuatorOutputs(sensorInputs)
    }

    /**
     * Focuses on transitions between states.
     */
    private fun transitionToNextState(sensorInputs: SensorInputs): State {
        return when (val currentState = state) {
            State.Parked -> when {
                sensorInputs.isUpperKeyEnabled -> State.WaitingAfterTurningOn(waitStart = TimeProvider.now(), enabledFrom = EnabledFrom.UpperKey)
                sensorInputs.isLowerKeyEnabled -> State.WaitingAfterTurningOn(waitStart = TimeProvider.now(), enabledFrom = EnabledFrom.LowerKey)
                else -> currentState
            }
            is State.WaitingAfterTurningOn -> if (TimeProvider.now() - currentState.waitStart < 3.seconds) {
                currentState
            } else {
                when (currentState.enabledFrom) {
                    EnabledFrom.UpperKey -> State.GoingUpWithoutWheelchair
                    EnabledFrom.LowerKey -> State.PlatformUnfolding(enabledFrom = currentState.enabledFrom)
                }
            }
            is State.PlatformUnfolding -> if (sensorInputs.foldablePlatformPositionNormalized < 1.0f) {
                currentState
            } else {
                State.PreparingBothFlapsForEnteringWheelchair(enabledFrom = currentState.enabledFrom)
            }
            is State.PreparingBothFlapsForEnteringWheelchair -> {
                val higherFlapTargetReached = when (currentState.enabledFrom) {
                    EnabledFrom.UpperKey -> sensorInputs.higherFlapPositionNormalized > 1.0f
                    EnabledFrom.LowerKey -> sensorInputs.higherFlapPositionNormalized in 0.45f..0.55f
                }
                val lowerFlapTargetReached = when (currentState.enabledFrom) {
                    EnabledFrom.UpperKey -> sensorInputs.lowerFlapPositionNormalized in 0.45f..0.55f
                    EnabledFrom.LowerKey -> sensorInputs.lowerFlapPositionNormalized > 1.0f
                }
                if (!lowerFlapTargetReached || !higherFlapTargetReached) {
                    currentState
                } else {
                    State.WaitingForWheelchair
                }
            }
            State.GoingUpWithoutWheelchair -> if (sensorInputs.mainMotorPositionNormalized < 1.0f) {
                currentState
            } else {
                State.PlatformUnfolding(enabledFrom = EnabledFrom.UpperKey)
            }
            State.WaitingForWheelchair -> if (!sensorInputs.isWheelchairPresent) {
                currentState
            } else {
                State.PreparingForDrivingWithWheelchair
            }
            State.PreparingForDrivingWithWheelchair -> if (sensorInputs.barriersPositionNormalized < 1.0f ||
                    sensorInputs.lowerFlapPositionNormalized !in 0.45f..0.55f ||
                    sensorInputs.higherFlapPositionNormalized !in 0.45f..0.55f) {
                currentState
            } else {
                State.Driving
            }
            State.Driving -> if (sensorInputs.mainMotorPositionNormalized < 0.0f && sensorInputs.goingDownButtonPressed ||
                    sensorInputs.mainMotorPositionNormalized > 1.0f && sensorInputs.goingUpButtonPressed) {
                State.PreparingForWheelchairLeaving
            } else {
                currentState
            }
            State.PreparingForWheelchairLeaving -> if (sensorInputs.barriersPositionNormalized > 0.0f ||
                    sensorInputs.higherFlapPositionNormalized < 1.0f) {
                currentState
            } else {
                State.WaitingForWheelchairLeaving
            }
            State.WaitingForWheelchairLeaving -> if (sensorInputs.isWheelchairPresent) {
                currentState
            } else {
                State.GoingDown
            }
            State.GoingDown -> if (sensorInputs.mainMotorPositionNormalized > 0.0f) {
                currentState
            } else {
                State.Driving
            }
        }
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
            State.Driving -> ActuatorOutputs(mainMotorSpeed = when {
                sensorInputs.goingUpButtonPressed -> 0.2f
                sensorInputs.goingDownButtonPressed -> -0.2f
                else -> 0.0f
            })
            State.PreparingForWheelchairLeaving -> ActuatorOutputs(
                    barriersUnfoldingSpeed = if (sensorInputs.barriersPositionNormalized > 0.0f) -0.2f else 0.0f,
                    higherFlapUnfoldingSpeed = if (sensorInputs.higherFlapPositionNormalized < 1.0f) 0.2f else 0.0f,
            )
            State.WaitingForWheelchairLeaving -> ActuatorOutputs()
            State.GoingDown -> ActuatorOutputs(mainMotorSpeed = -0.2f)
        }
    }
}
