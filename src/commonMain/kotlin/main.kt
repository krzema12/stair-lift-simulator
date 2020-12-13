import com.soywiz.kds.*
import com.soywiz.klock.*
import com.soywiz.korge.*
import com.soywiz.korge.scene.*
import com.soywiz.korge.tween.*
import com.soywiz.korge.view.*
import com.soywiz.korge3d.*
import com.soywiz.korge3d.format.*
import com.soywiz.korge3d.tween.moveBy
import com.soywiz.korim.color.*
import com.soywiz.korinject.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.std.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.interpolation.*

//suspend fun main() = Demo3.main(args)
suspend fun main() = Korge(Korge.Config(module = Korge3DSampleModule()))

@Korge3DExperimental
class Korge3DSampleModule : KorgeModule(RootScene::class) {
    override val size: SizeInt = SizeInt(1280, 720)
    override val title: String = "KorGE 3D"
    override val bgcolor: RGBA = RGBA.float(.25f, .25f, .25f, 1f)

    override suspend fun AsyncInjector.configure() {
        mapPrototype { RootScene() }
        mapPrototype { MonkeyScene() }
    }
}

class RootScene : Scene() {
    lateinit var contentSceneContainer: SceneContainer

    override suspend fun Container.sceneInit() {
        contentSceneContainer = sceneContainer(views)

        contentSceneContainer.changeTo<MonkeyScene>()
    }
}

@Korge3DExperimental
class MonkeyScene : Scene() {
    override suspend fun Container.sceneInit() {
        //delay(10.seconds)
        //println("delay")
        scene3D {
            val light1 = light().position(0, 10, +10).setTo(Colors.RED)
            val light2 = light().position(10, 0, +10).setTo(Colors.BLUE)

            launchImmediately {
                while (true) {
                    tween(light1::y[-20], light2::x[-20], time = 1.seconds, easing = Easing.SMOOTH)
                    tween(light1::y[+20], light2::x[+20], time = 1.seconds, easing = Easing.SMOOTH)
                }
            }

            val library = resourcesVfs["StairLift.dae"].readColladaLibrary()
            val mainSceneView = library.mainScene.instantiate()
            this += mainSceneView

            var tick = 0
            addUpdater {
                val angle = (tick / 1.0).degrees
                camera.positionLookingAt(
                    cos(angle * 1) * 4, 0.0, -sin(angle * 1) * 4, // Orbiting camera
                    0.0, 0.0, 0.0
                )
                tick++
            }
        }
    }

}
