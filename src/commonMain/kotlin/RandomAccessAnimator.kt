import com.soywiz.korge3d.Korge3DExperimental
import com.soywiz.korge3d.animation.Animator3D

@Korge3DExperimental
class RandomAccessAnimator(val animator3D: Animator3D, var progress: Float = 0.0f) {
    init {
        // To avoid excessive allocations, a single function is assigned to 'playbackPattern' through the whole
        // lifecycle of this animator, instead of setting { someNewDoubleValue } each time one wants to change
        // animation's progress.
        animator3D.playbackPattern = { progress.toDouble() }
    }
}
