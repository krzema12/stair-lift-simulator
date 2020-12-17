import com.soywiz.kds.get
import com.soywiz.klock.TimeSpan
import com.soywiz.korge.scene.Scene
import com.soywiz.korge.view.Container
import com.soywiz.korge.view.Text
import com.soywiz.korge.view.addUpdater
import com.soywiz.korge.view.position
import com.soywiz.korge3d.Camera3D
import com.soywiz.korge3d.Korge3DExperimental
import com.soywiz.korge3d.Library3D
import com.soywiz.korge3d.animation.Animation3D
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

            val drivingUpAndDownAnimation = library.getAnimation("Driving_up_and_down_Driving_up_and_downAction_transform")
            val drivingUpAndDownAnimator = RandomAccessAnimator(drivingUpAndDownAnimation, mainSceneView)
            val lowerFlapAnimation = library.getAnimation("Lower_flap_Lower_flapAction_transform")
            val lowerFlapAnimator = RandomAccessAnimator(lowerFlapAnimation, mainSceneView)
            val higherFlapAnimation = library.getAnimation("Higher_flap_Higher_flapAction_transform")
            val higherFlapAnimator = RandomAccessAnimator(higherFlapAnimation, mainSceneView)

            val cameraFromModel = mainSceneView.findByType<Camera3D>().firstOrNull()
                    ?: throw RuntimeException("Camera not found in the model")
            camera = cameraFromModel.clone()

            val movingParts = MovingParts(
                    drivingUpAndDownAnimator = drivingUpAndDownAnimator,
                    lowerFlapAnimator = lowerFlapAnimator,
                    higherFlapAnimator = higherFlapAnimator,
            )
            val controllerLogic = ControllerLogic()

            val sensorInputsText = Text(text = "").position(2, 2)
            this@sceneInit += sensorInputsText
            val actuatorOutputsText = Text(text = "").position(2, 52)
            this@sceneInit += actuatorOutputsText
            val controllerStateText = Text(text = "").position(2, 102)
            this@sceneInit += controllerStateText

            addUpdater { timeSpan ->
                val sensorInputs = movingParts.prepareSensorInputs()
                sensorInputsText.text = sensorInputs.toString().replace("(", "(\n")

                val actuatorOutputs = controllerLogic.run(sensorInputs)
                actuatorOutputsText.text = actuatorOutputs.toString().replace("(", "(\n")

                controllerStateText.text = controllerLogic.state.toString().replace("(", "(\n")

                movingParts.executeActuatorOutputs(actuatorOutputs, timeSpan)
            }
        }
    }
}

@Korge3DExperimental
private fun Library3D.getAnimation(name: String): Animation3D {
    return animationDefs[name] ?: throw IllegalArgumentException("Could not find animation with name '$name'")
}

@Korge3DExperimental
data class MovingParts(
        val drivingUpAndDownAnimator: RandomAccessAnimator,
        val lowerFlapAnimator: RandomAccessAnimator,
        val higherFlapAnimator: RandomAccessAnimator,
)

@Korge3DExperimental
private fun MovingParts.prepareSensorInputs(): SensorInputs {
    return SensorInputs(
            mainMotorPositionNormalized = drivingUpAndDownAnimator.progress,
    )
}

@Korge3DExperimental
private fun MovingParts.executeActuatorOutputs(actuatorOutputs: ActuatorOutputs, timeSpan: TimeSpan) {
    drivingUpAndDownAnimator.progress += actuatorOutputs.mainMotorSpeed * timeSpan.seconds.toFloat()
    lowerFlapAnimator.progress += actuatorOutputs.mainMotorSpeed * timeSpan.seconds.toFloat()
    higherFlapAnimator.progress += actuatorOutputs.mainMotorSpeed * timeSpan.seconds.toFloat()
}
