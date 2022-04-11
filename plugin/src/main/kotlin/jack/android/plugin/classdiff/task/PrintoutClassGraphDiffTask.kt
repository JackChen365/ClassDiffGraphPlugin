package jack.android.plugin.classdiff.task

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import jack.android.plugin.classdiff.diff.ClassNode
import jack.android.plugin.classdiff.diff.ClassDiffHelper
import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.lang.reflect.Type

/**
 * Output all the class diff. This task compare two commits and output the class diff.
 *
 * This task received three files.
 * 1. [diffClassFileProvider] the diff classes list. We use this diff file to reduce the class graph.
 * 2. [previousDiffFileProvider] the previous class diff configuration.
 * 3. [currentDiffFileProvider] the current class diff configuration.
 */
abstract class PrintoutClassGraphDiffTask : DefaultTask() {
    /**
     * The diff classes are output by the git diff.
     * Here is the shell command:
     *  git diff --name-only "$before_commit" "$current_commit" | grep '[.kt|.java]$' > $output_file
     */
    @get:Input
    abstract val diffClassFileProvider: Property<File>

    /**
     * the previous diff file.
     * We use ASM to build a class graph and use git diff to reduce the class graph.
     */
    @get:Input
    abstract val previousDiffFileProvider: Property<File>

    /**
     * the current diff file.
     * We use ASM to build a class graph and use git diff to reduce the class graph.
     */
    @get:Input
    abstract val currentDiffFileProvider: Property<File>

    @TaskAction
    fun outputClassGraphDiffTask() {
        if (!diffClassFileProvider.isPresent || !previousDiffFileProvider.isPresent || !currentDiffFileProvider.isPresent) return
        val diffClassFile = diffClassFileProvider.get()
        val previousDiffFile = previousDiffFileProvider.get()
        val currentDiffFile = currentDiffFileProvider.get()

        val gson = Gson()
        val type: Type = object : TypeToken<ArrayList<ClassNode>>() {}.type
        val previousClassList: List<ClassNode> = gson.fromJson(previousDiffFile.readText(), type)
        val currentClassList: List<ClassNode> = gson.fromJson(currentDiffFile.readText(), type)
        val diffClassList = diffClassFile.readLines()
        //Output the diff classes list.
        println("--------------------------------------------------")
        diffClassList.forEach(System.out::println)
        println("--------------------------------------------------")
        ClassDiffHelper.outputClassReferencesDiff(
            previousClassList,
            currentClassList,
            diffClassList
        )
    }
}