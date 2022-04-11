package jack.android.plugin.classdiff.util

import com.android.build.gradle.AppExtension
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.LibraryExtension
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginWrapper
import java.io.File

/**
 * The Gradle utils
 */
object GradleUtils {
    /**
     * Return all the sourceSet from this project.
     * Tips:
     *  Including all the android app and library
     */
    fun getSourceSets(project: Project): List<String> {
        val sourceSetFiles = mutableSetOf<File>()
        project.rootProject.allprojects { subProject ->
            when {
                subProject.plugins.hasPlugin("com.android.application") -> { //android app
                    val appExtension: BaseExtension =
                        project.extensions.getByType(AppExtension::class.java);
                    appExtension.sourceSets.forEach { sourceSet ->
                        if (sourceSet.name != "test" && sourceSet.name != "androidTest") {
                            sourceSetFiles.addAll(sourceSet.java.srcDirs)
                        }
                    }
                }
                subProject.plugins.hasPlugin("com.android.library") -> { //android app/library
                    val appExtension: BaseExtension =
                        project.extensions.getByType(LibraryExtension::class.java);
                    appExtension.sourceSets.forEach { sourceSet ->
                        //ignore the test folder.
                        if (sourceSet.name != "test" && sourceSet.name != "androidTest") {
                            sourceSetFiles.addAll(sourceSet.java.srcDirs)
                        }
                    }
                }
                subProject.plugins.hasPlugin(JavaPlugin::class.java) -> { //java module
                    val javaConvention =
                        project.convention.getPlugin(JavaPluginConvention::class.java)
                    javaConvention.sourceSets.forEach { sourceSet ->
                        //ignore the test folder.
                        if (sourceSet.name != "test" && sourceSet.name != "androidTest") {
                            sourceSetFiles.addAll(sourceSet.java.srcDirs)
                        }
                    }
                }
            }
        }
        val rootDir = project.rootDir.absolutePath
        return sourceSetFiles.map { file ->
            file.absolutePath.substring(rootDir.length + 1).replace('/', '.')
        }.filterNot { it.isEmpty() }
    }
}