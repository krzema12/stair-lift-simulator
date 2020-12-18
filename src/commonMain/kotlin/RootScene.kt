import com.soywiz.kds.get
import com.soywiz.klock.TimeSpan
import com.soywiz.korev.Key
import com.soywiz.korge.input.keys
import com.soywiz.korge.scene.Scene
import com.soywiz.korge.view.Container
import com.soywiz.korge.view.Text
import com.soywiz.korge.view.addUpdater
import com.soywiz.korge.view.position
import com.soywiz.korge3d.Camera3D
import com.soywiz.korge3d.Korge3DExperimental
import com.soywiz.korge3d.Library3D
import com.soywiz.korge3d.View3D
import com.soywiz.korge3d.findByType
import com.soywiz.korge3d.format.readColladaLibrary
import com.soywiz.korge3d.instantiate
import com.soywiz.korge3d.scene3D
import com.soywiz.korio.file.std.resourcesVfs

@Korge3DExperimental
class RootScene : Scene() {
    override suspend fun Container.sceneInit() {

        scene3D {
            val library = resourcesVfs["StairLift.dae"].readColladaLibrary()
            val mainSceneView = library.mainScene.instantiate()
            this += mainSceneView

            val drivingUpAndDownAnimator = library.getAnimator("Driving_up_and_down_Driving_up_and_downAction_transform", mainSceneView)
            val foldablePlatformAnimator = library.getAnimator("Foldable_platform_Foldable_platformAction_transform", mainSceneView)
            val lowerFlapAnimator = library.getAnimator("Lower_flap_Lower_flapAction_transform", mainSceneView)
            val higherFlapAnimator = library.getAnimator("Higher_flap_Higher_flapAction_transform", mainSceneView)
            val barriersAnimator = library.getAnimator("Barriers_BarriersAction_transform", mainSceneView)

            val cameraFromModel = mainSceneView.findByType<Camera3D>().firstOrNull()
                    ?: throw RuntimeException("Camera not found in the model")
            camera = cameraFromModel.clone()

            val movingParts = MovingParts(
                    drivingUpAndDownAnimator = drivingUpAndDownAnimator,
                    foldablePlatformAnimator = foldablePlatformAnimator,
                    lowerFlapAnimator = lowerFlapAnimator,
                    higherFlapAnimator = higherFlapAnimator,
                    barriersAnimator = barriersAnimator,
            )
            val controllerLogic = ControllerLogic()

            val sensorInputsText = Text(text = "").position(2, 2)
            this@sceneInit += sensorInputsText
            val actuatorOutputsText = Text(text = "").position(2, 202)
            this@sceneInit += actuatorOutputsText
            val controllerStateText = Text(text = "").position(2, 402)
            this@sceneInit += controllerStateText

            var isKeyEnabled = false
            var isWheelchairPresent = false
            var goingUpButtonPressed = false
            var goingDownButtonPressed = false

            keys {
                down(Key.K) { isKeyEnabled = true }
                up(Key.K) { isKeyEnabled = false }

                down(Key.W) { isWheelchairPresent = true }
                down(Key.Q) { isWheelchairPresent = false }

                down(Key.PAGE_UP) { goingUpButtonPressed = true }
                up(Key.PAGE_UP) { goingUpButtonPressed = false }
                down(Key.PAGE_DOWN) { goingDownButtonPressed = true }
                up(Key.PAGE_DOWN) { goingDownButtonPressed = false }
            }

            addUpdater { timeSpan ->
                val sensorInputs = movingParts.prepareSensorInputs(
                        isKeyEnabled,
                        isWheelchairPresent,
                        goingUpButtonPressed,
                        goingDownButtonPressed)
                sensorInputsText.text = sensorInputs.toString().replace("(", "(\n").replace(",", ",\n")

                val actuatorOutputs = controllerLogic.run(sensorInputs)
                actuatorOutputsText.text = actuatorOutputs.toString().replace("(", "(\n").replace(",", ",\n")

                controllerStateText.text = controllerLogic.state.toString().replace("(", "(\n").replace(",", ",\n")

                movingParts.executeActuatorOutputs(actuatorOutputs, timeSpan)
            }
        }
    }
}

@Korge3DExperimental
private fun Library3D.getAnimator(name: String, mainSceneView: View3D): RandomAccessAnimator {
    val animation = animationDefs[name] ?: throw IllegalArgumentException("Could not find animation with name '$name'")
    return RandomAccessAnimator(animation, mainSceneView)
}

@Korge3DExperimental
data class MovingParts(
        val drivingUpAndDownAnimator: RandomAccessAnimator,
        val foldablePlatformAnimator: RandomAccessAnimator,
        val lowerFlapAnimator: RandomAccessAnimator,
        val higherFlapAnimator: RandomAccessAnimator,
        val barriersAnimator: RandomAccessAnimator,
)

@Korge3DExperimental
private fun MovingParts.prepareSensorInputs(isKeyEnabled: Boolean,
                                            isWheelchairPresent: Boolean,
                                            wantsToGoUp: Boolean,
                                            wantsToGoDown: Boolean): SensorInputs {
    return SensorInputs(
            isKeyEnabled = isKeyEnabled,
            goingUpButtonPressed = wantsToGoUp,
            goingDownButtonPressed = wantsToGoDown,
            foldablePlatformPositionNormalized = foldablePlatformAnimator.progress,
            lowerFlapPositionNormalized = lowerFlapAnimator.progress,
            higherFlapPositionNormalized = higherFlapAnimator.progress,
            isWheelchairPresent = isWheelchairPresent,
            barriersPositionNormalized = barriersAnimator.progress,
            mainMotorPositionNormalized = drivingUpAndDownAnimator.progress,
    )
}

@Korge3DExperimental
private fun MovingParts.executeActuatorOutputs(actuatorOutputs: ActuatorOutputs, timeSpan: TimeSpan) {
    drivingUpAndDownAnimator.progress += actuatorOutputs.mainMotorSpeed * timeSpan.seconds.toFloat()
    foldablePlatformAnimator.progress += actuatorOutputs.foldablePlatformUnfoldingSpeed * timeSpan.seconds.toFloat()
    lowerFlapAnimator.progress += actuatorOutputs.lowerFlapUnfoldingSpeed * timeSpan.seconds.toFloat()
    higherFlapAnimator.progress += actuatorOutputs.higherFlapUnfoldingSpeed * timeSpan.seconds.toFloat()
    barriersAnimator.progress += actuatorOutputs.barriersUnfoldingSpeed * timeSpan.seconds.toFloat()
}
