import com.soywiz.klock.TimeSpan
import com.soywiz.korge.Korge
import com.soywiz.korge.scene.KorgeModule
import com.soywiz.korge.scene.Scene
import com.soywiz.korge.view.Container
import com.soywiz.korge.view.addUpdater
import com.soywiz.korge3d.Korge3DExperimental
import com.soywiz.korge3d.View3D
import com.soywiz.korge3d.format.readColladaLibrary
import com.soywiz.korge3d.get
import com.soywiz.korge3d.instantiate
import com.soywiz.korge3d.scene3D
import com.soywiz.korim.color.RGBA
import com.soywiz.korinject.AsyncInjector
import com.soywiz.korio.file.std.resourcesVfs
import com.soywiz.korma.geom.SizeInt

@Korge3DExperimental
suspend fun main() = Korge(Korge.Config(module = StairLiftSimulatorModule()))

@Korge3DExperimental
class StairLiftSimulatorModule : KorgeModule(RootScene::class) {
    override val size: SizeInt = SizeInt(1280, 720)
    override val title: String = "KorGE 3D"
    override val bgcolor: RGBA = RGBA.float(.25f, .25f, .25f, 1f)

    override suspend fun AsyncInjector.configure() {
        mapPrototype { RootScene() }
    }
}

@Korge3DExperimental
data class MovingParts(
        val wholeStairLift: View3D,
)

@Korge3DExperimental
class RootScene : Scene() {
    override suspend fun Container.sceneInit() {
        scene3D {
            val library = resourcesVfs["StairLift.dae"].readColladaLibrary()
            val mainSceneView = library.mainScene.instantiate()
            this += mainSceneView

            val movingParts = MovingParts(
                    wholeStairLift = mainSceneView["Scene"] ?: throw RuntimeException("Model part not found!"),
            )
            val controllerLogic = ControllerLogic()

            addUpdater { timeSpan ->
                val sensorInputs = movingParts.prepareSensorInputs()
                val actuatorOutputs = controllerLogic.run(sensorInputs)
                movingParts.executeActuatorOutputs(actuatorOutputs, timeSpan)
            }
        }
    }
}

@Korge3DExperimental
private fun MovingParts.prepareSensorInputs(): SensorInputs {
    return SensorInputs(
            mainMotorEncoder = (wholeStairLift.x * 1000.0).toInt(),
    )
}

@Korge3DExperimental
private fun MovingParts.executeActuatorOutputs(actuatorOutputs: ActuatorOutputs, timeSpan: TimeSpan) {
    wholeStairLift.x += actuatorOutputs.mainMotorSpeed * timeSpan.seconds
}
