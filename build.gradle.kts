import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import com.soywiz.korge.gradle.KorgeGradlePlugin
import com.soywiz.korge.gradle.korge

buildscript {
    repositories {
        mavenLocal()
        maven { url = uri("https://dl.bintray.com/korlibs/korlibs") }
        maven { url = uri("https://plugins.gradle.org/m2/") }
        mavenCentral()
        google()
    }
    dependencies {
        classpath("com.soywiz.korlibs.korge.plugins:korge-gradle-plugin:2.0.7.0")
    }
}

apply<KorgeGradlePlugin>()

korge {
    id = "com.soywiz.samples.korge3d"
    supportExperimental3d()
    targetDefault()
}
