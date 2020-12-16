import com.soywiz.korge.Korge
import com.soywiz.korge.scene.KorgeModule
import com.soywiz.korge3d.Korge3DExperimental
import com.soywiz.korim.color.RGBA
import com.soywiz.korinject.AsyncInjector
import com.soywiz.korma.geom.SizeInt

@Korge3DExperimental
suspend fun main() = Korge(Korge.Config(module = StairLiftSimulatorModule()))

@Korge3DExperimental
class StairLiftSimulatorModule : KorgeModule(RootScene::class) {
    override val size: SizeInt = SizeInt(1280, 720)
    override val title: String = "Stair Lift Simulator"
    override val bgcolor: RGBA = RGBA.float(.25f, .25f, .25f, 1f)

    override suspend fun AsyncInjector.configure() {
        mapPrototype { RootScene() }
    }
}
