import com.soywiz.klock.seconds
import com.soywiz.korge.Korge
import com.soywiz.korge.scene.KorgeModule
import com.soywiz.korge.scene.Scene
import com.soywiz.korge.tween.get
import com.soywiz.korge.tween.tween
import com.soywiz.korge.view.Container
import com.soywiz.korge.view.addUpdater
import com.soywiz.korge3d.Korge3DExperimental
import com.soywiz.korge3d.format.readColladaLibrary
import com.soywiz.korge3d.get
import com.soywiz.korge3d.instantiate
import com.soywiz.korge3d.light
import com.soywiz.korge3d.position
import com.soywiz.korge3d.positionLookingAt
import com.soywiz.korge3d.scene3D
import com.soywiz.korim.color.Colors
import com.soywiz.korim.color.RGBA
import com.soywiz.korinject.AsyncInjector
import com.soywiz.korio.async.launchImmediately
import com.soywiz.korio.file.std.resourcesVfs
import com.soywiz.korma.geom.Angle
import com.soywiz.korma.geom.SizeInt
import com.soywiz.korma.geom.cos
import com.soywiz.korma.geom.degrees
import com.soywiz.korma.geom.sin
import com.soywiz.korma.geom.times
import com.soywiz.korma.interpolation.Easing

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
class RootScene : Scene() {
    override suspend fun Container.sceneInit() {
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
            val sideFlap = mainSceneView["Scene"]["Side flap"]
            this += mainSceneView

            var tick = 0
            addUpdater {
                val angle = (tick / 1.0).degrees
                camera.positionLookingAt(
                    cos(angle * 1) * 4, 2.0, -sin(angle * 1) * 4, // Orbiting camera
                    0.0, 0.0, 0.0
                )
                sideFlap?.rotationX = Angle.fromDegrees(tick)
                tick++
            }
        }
    }
}
