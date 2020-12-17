import com.soywiz.kds.get
import com.soywiz.klock.TimeSpan
import com.soywiz.korge.scene.Scene
import com.soywiz.korge.view.Container
import com.soywiz.korge.view.Text
import com.soywiz.korge.view.addUpdater
import com.soywiz.korge.view.position
import com.soywiz.korge3d.Camera3D
import com.soywiz.korge3d.Korge3DExperimental
import com.soywiz.korge3d.View3D
import com.soywiz.korge3d.findByType
import com.soywiz.korge3d.format.readColladaLibrary
import com.soywiz.korge3d.get
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

            val sideFlapAnimation = library.animationDefs["Side_flap_Side_flapAction_transform"]
                    ?: throw RuntimeException("Side flap animation not found!")
            val sideFlapAnimator = RandomAccessAnimator(sideFlapAnimation, mainSceneView)

            val cameraFromModel = mainSceneView.findByType<Camera3D>().firstOrNull()
                    ?: throw RuntimeException("Camera not found in the model")
            camera = cameraFromModel.clone()

            val movingParts = MovingParts(
                    wholeStairLift = mainSceneView["Scene"] ?: throw RuntimeException("Model part not found!"),
                    sideFlapAnimator = sideFlapAnimator,
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
data class MovingParts(
        val wholeStairLift: View3D,
        val sideFlapAnimator: RandomAccessAnimator,
)

@Korge3DExperimental
private fun MovingParts.prepareSensorInputs(): SensorInputs {
    return SensorInputs(
            mainMotorEncoder = (wholeStairLift.x * 1000.0).toInt(),
    )
}

@Korge3DExperimental
private fun MovingParts.executeActuatorOutputs(actuatorOutputs: ActuatorOutputs, timeSpan: TimeSpan) {
    wholeStairLift.x += actuatorOutputs.mainMotorSpeed * timeSpan.seconds
    wholeStairLift.y += 0.3 * actuatorOutputs.mainMotorSpeed * timeSpan.seconds

    sideFlapAnimator.setProgress(((wholeStairLift.x + 2.0)/4.0).toFloat())
}
