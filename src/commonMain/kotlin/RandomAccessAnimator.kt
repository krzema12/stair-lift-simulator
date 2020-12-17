import com.soywiz.kds.getCyclic
import com.soywiz.klock.milliseconds
import com.soywiz.klock.seconds
import com.soywiz.korge3d.Korge3DExperimental
import com.soywiz.korge3d.View3D
import com.soywiz.korge3d.animation.Animation3D
import com.soywiz.korge3d.get
import com.soywiz.korma.geom.degrees
import com.soywiz.korma.interpolation.interpolate

@Korge3DExperimental
class RandomAccessAnimator(val animation: Animation3D, val rootView: View3D) {
    fun setProgress(progress: Float) {
        println(progress)

        val keyFrames = animation.keyFrames
        val fseconds = keyFrames.seconds
        val ftransforms = keyFrames.transforms
        val ffloats = keyFrames.floats
        val aproperty = animation.property
        val elapsedTimeInAnimation = animation.totalTime * progress.toDouble()
        //genericBinarySearch(0, animation.keys.size) { animation.keys[it] }

        val n = keyFrames.findIndex(elapsedTimeInAnimation)
        if (n < 0) return

        val startTime = fseconds[n].toDouble().seconds
        val endTime = fseconds.getOrNull(n + 1)?.let { it.toDouble().seconds } ?: startTime
        val fragmentTime = (endTime - startTime)
        if (fragmentTime <= 0.milliseconds) return

        val ratio = (elapsedTimeInAnimation - startTime) / fragmentTime
        val aview = rootView[animation.target]
        //println("ratio: $ratio, startTime=$startTime, endTime=$endTime, elapsedTimeInAnimation=$elapsedTimeInAnimation")
        if (aview != null) {
            when (aproperty) {
                "transform" -> {
                    if (ftransforms != null) {
                        if (n >= ftransforms.size) {
                            error("Unexpected")
                        }
                        aview.transform.setToInterpolated(
                                ftransforms[n],
                                ftransforms.getCyclic(n + 1),
                                ratio.toDouble()
                        )
                    }
                }
                "location.X", "location.Y", "location.Z", "scale.X", "scale.Y", "scale.Z", "rotationX.ANGLE", "rotationY.ANGLE", "rotationZ.ANGLE" -> {
                    if (ffloats != null) {
                        val value = ratio.interpolate(ffloats[n], ffloats[n % ffloats.size]).toDouble()
                        when (aproperty) {
                            "location.X" -> aview.x = value
                            "location.Y" -> aview.y = value
                            "location.Z" -> aview.z = value
                            "scale.X" -> aview.scaleX = value
                            "scale.Y" -> aview.scaleY = value
                            "scale.Z" -> aview.scaleZ = value
                            "rotationX.ANGLE" -> aview.rotationX = value.degrees
                            "rotationY.ANGLE" -> aview.rotationY = value.degrees
                            "rotationZ.ANGLE" -> aview.rotationZ = value.degrees
                        }
                    }
                }
                else -> {
                    println("WARNING: animation.property=${animation.property} not implemented")
                }
            }
        }

        //animation.keyFrames.binarySearch { it.time.millisecondsInt }
        //println(animation)
    }
}