package jack.android.plugin.classdiff.task

import jack.android.plugin.classdiff.util.GradleUtils
import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import java.io.File


abstract class OutputDiffClassListTask : DefaultTask() {
    @get:Input
    abstract val diffFileProvider: Property<File>

    @get:Input
    abstract val diffClassOutputFileProvider: Property<File>

    @TaskAction
    fun outputClassGraphDiffTask() {
        if (!diffFileProvider.isPresent || !diffFileProvider.isPresent) return
        val diffFile = diffFileProvider.get()
        val diffClassFile = diffClassOutputFileProvider.get()
        val sourceSets = GradleUtils.getSourceSets(project)
        val diffClasses =
            diffFile.readLines().filter { line -> line.endsWith(".java") || line.endsWith(".kt") }
                .mapNotNull { filePath ->
                    val classPath = filePath.substringBeforeLast(".").replace('/', '.')
                    val sourceSet = sourceSets.firstOrNull { classPath.startsWith(it) }
                    if (null == sourceSet) null else classPath.substring(sourceSet.length + 1)
                }
        println("--------------------------------------------------")
        diffClasses.forEach(System.out::println)
        println("--------------------------------------------------")
        diffClassFile.writeText(diffClasses.joinToString("\n"))
    }
}