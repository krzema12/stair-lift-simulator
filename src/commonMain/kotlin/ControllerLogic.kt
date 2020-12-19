import com.soywiz.klock.DateTime
import com.soywiz.klock.TimeProvider
import com.soywiz.klock.seconds

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
    var state: State = State.Parked
    private set

    sealed class State {
        object Parked : State()
        data class WaitingAfterTurningOn(val waitStart: DateTime) : State()
        object PlatformUnfolding : State()
        object UnfoldingBothFlaps : State()
        object WaitingForWheelchair : State()
        object PreparingForDrivingWithWheelchair : State()
        object Driving : State()
        object PreparingForWheelchairLeaving : State()
        object WaitingForWheelchairLeaving : State()
        object GoingDown : State()
    }

    fun run(sensorInputs: SensorInputs): ActuatorOutputs {
        state = getNewState(sensorInputs)
        return getActuatorOutputs(sensorInputs)
    }

    /**
     * Focuses on transitions between states.
     */
    private fun getNewState(sensorInputs: SensorInputs): State {
        return when (val currentState = state) {
            State.Parked -> if (!sensorInputs.isKeyEnabled) {
                currentState
            } else {
                State.WaitingAfterTurningOn(waitStart = TimeProvider.now())
            }
            is State.WaitingAfterTurningOn -> if (TimeProvider.now() - currentState.waitStart < 3.seconds) {
                currentState
            } else {
                State.PlatformUnfolding
            }
            State.PlatformUnfolding -> if (sensorInputs.foldablePlatformPositionNormalized < 1.0f) {
                currentState
            } else {
                State.UnfoldingBothFlaps
            }
            State.UnfoldingBothFlaps -> if (sensorInputs.lowerFlapPositionNormalized < 1.0f ||
                    sensorInputs.higherFlapPositionNormalized < 1.0f) {
                currentState
            } else {
                State.WaitingForWheelchair
            }
            State.WaitingForWheelchair -> if (!sensorInputs.isWheelchairPresent) {
                currentState
            } else {
                State.PreparingForDrivingWithWheelchair
            }
            State.PreparingForDrivingWithWheelchair -> if (sensorInputs.barriersPositionNormalized < 1.0f ||
                    sensorInputs.lowerFlapPositionNormalized > 0.5f ||
                    sensorInputs.higherFlapPositionNormalized > 0.5f) {
                currentState
            } else {
                State.Driving
            }
            State.Driving -> if (sensorInputs.mainMotorPositionNormalized < 1.0f) {
                currentState
            } else {
                State.PreparingForWheelchairLeaving
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
        return when (state) {
            State.Parked -> ActuatorOutputs()
            is State.WaitingAfterTurningOn -> ActuatorOutputs()
            State.PlatformUnfolding -> ActuatorOutputs(foldablePlatformUnfoldingSpeed = 0.2f)
            State.UnfoldingBothFlaps -> ActuatorOutputs(
                    lowerFlapUnfoldingSpeed = if (sensorInputs.lowerFlapPositionNormalized < 1.0f) 0.2f else 0.0f,
                    higherFlapUnfoldingSpeed = if (sensorInputs.higherFlapPositionNormalized < 1.0f) 0.2f else 0.0f,
            )
            State.WaitingForWheelchair -> ActuatorOutputs()
            State.PreparingForDrivingWithWheelchair -> ActuatorOutputs(
                    barriersUnfoldingSpeed = if (sensorInputs.barriersPositionNormalized < 1.0f) 0.2f else 0.0f,
                    lowerFlapUnfoldingSpeed = if (sensorInputs.lowerFlapPositionNormalized > 0.5f) -0.2f else 0.0f,
                    higherFlapUnfoldingSpeed = if (sensorInputs.higherFlapPositionNormalized > 0.5f) -0.2f else 0.0f,
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
