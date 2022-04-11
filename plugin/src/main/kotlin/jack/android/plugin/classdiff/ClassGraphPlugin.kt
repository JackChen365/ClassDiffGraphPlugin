package jack.android.plugin.classdiff

import com.android.build.gradle.AppExtension
import jack.android.plugin.classdiff.extension.ClassGraphExtension
import jack.android.plugin.classdiff.task.PrintoutClassGraphDiffTask
import jack.android.plugin.classdiff.task.OutputClassGraphDotTask
import jack.android.plugin.classdiff.task.OutputDiffClassListTask
import jack.android.plugin.classdiff.transform.ClassGraphBuildTransform
import jack.android.plugin.classdiff.util.GradleUtils
import org.gradle.api.Plugin
import org.gradle.api.Project
import java.io.File

/**
 * Gradle plugin class for 'classdiff', applied on the base application module
 *
 * Here is the extension for this plugin.
 *
 * <p>
 * classdiff {
 *  componentListFiles project.file("../component_list.txt"), project.file("../component_list1.txt")
 *  filterPackages "boolean", "int", "short", "double", "float", "char"
 *  library {
 *      packageName = "android"
 *      desc = "android"
 *  }
 *  library {
 *      packageName = "androidx"
 *      desc = "androidx"
 *  }
 *  library {
 *      packageName = "java"
 *      includes = ["boolean", "int", "short", "double", "float", "char"]
 *      desc = "java"
 *  }
 * }
 * </p>
 * Check more detail at [ClassGraphExtension]
 *
 * The related tasks
 * 1. [OutputDiffClassListTask] Output the class diff to a json file in current commit.
 * 2. [PrintoutClassGraphDiffTask] Output all the class diff message between two commits.
 * 3. [OutputClassGraphDotTask] Output the class diff to a dot file between two commits.
 *
 * We have registered a transform called: [ClassGraphBuildTransform] to help us build the class graph.
 *
 */
class ClassGraphPlugin : Plugin<Project> {
    /**
     * The custom extension for this plugin.
     */
    private lateinit var extension: ClassGraphExtension

    override fun apply(target: Project) {
        //only apply this plugin for android app.
        if (!isAndroidProject(target)) return
        extension =
            target.extensions.create(ClassGraphConstants.PLUGIN_ID, ClassGraphExtension::class.java)
        if (target.hasProperty(ClassGraphConstants.PROPERTY_DIFF_FILE)) {
            val diffFile = File(target.property(ClassGraphConstants.PROPERTY_DIFF_FILE).toString())
            println(diffFile.absolutePath)
            if (!diffFile.exists()) {
                System.err.println("The diff file not exist.");
                return
            }
            registerOutputDiffClassTask(target, diffFile)
            //Register the gradle transform.
            registerClassGraphTransform(target, diffFile)
        }
        if (target.hasProperty(ClassGraphConstants.PROPERTY_PREVIOUS_DIFF_FILE) && target.hasProperty(
                ClassGraphConstants.PROPERTY_PREVIOUS_DIFF_FILE
            ) && target.hasProperty(
                ClassGraphConstants.PROPERTY_CURRENT_DIFF_FILE
            )
        ) {
            val diffClassFilePath = target.property(ClassGraphConstants.PROPERTY_DIFF_CLASS_FILE)
            val previousDiffFilePath =
                target.property(ClassGraphConstants.PROPERTY_PREVIOUS_DIFF_FILE)
            val currentDiffFilePath =
                target.property(ClassGraphConstants.PROPERTY_CURRENT_DIFF_FILE)
            val diffClassFile = File(diffClassFilePath.toString())
            val previousDiffFile = File(previousDiffFilePath.toString())
            val currentDiffFile = File(currentDiffFilePath.toString())
            registerOutputClassGraphDiffTask(
                target,
                diffClassFile,
                previousDiffFile,
                currentDiffFile
            )
            registerOutputClassGraphDotTask(
                target,
                diffClassFile,
                previousDiffFile,
                currentDiffFile
            )
        }
    }

    /**
     * Register the [OutputDiffClassListTask], It requires the diff file and diff output file.
     */
    private fun registerOutputDiffClassTask(target: Project, diffFile: File) {
        //Register the outputDiffClassesTask
        target.tasks.register(
            ClassGraphConstants.TASK_OUTPUT_DIFF_CLASSES, OutputDiffClassListTask::class.java
        ) { task ->
            task.group = ClassGraphConstants.TASK_GROUP_NAME
            task.description = "Process all the diff files as class references"
            if (target.hasProperty(ClassGraphConstants.PROPERTY_DIFF_OUTPUT_CLASS_FILE)) {
                val diffOutputClassFilePath =
                    target.property(ClassGraphConstants.PROPERTY_DIFF_OUTPUT_CLASS_FILE).toString()
                val diffOutputClassFile = File(diffOutputClassFilePath)
                task.diffFileProvider.set(diffFile)
                task.diffClassOutputFileProvider.set(diffOutputClassFile)
            }
        }
    }

    /**
     * Register the [PrintoutClassGraphDiffTask] task
     */
    private fun registerOutputClassGraphDiffTask(
        target: Project,
        diffClassFile: File,
        previousDiffFile: File,
        currentDiffFile: File
    ) {
        target.tasks.register(
            ClassGraphConstants.TASK_PRINT_OUT_CLASS_GRAPH_DIFF,
            PrintoutClassGraphDiffTask::class.java
        ) { task ->
            task.group = ClassGraphConstants.TASK_GROUP_NAME
            task.description = "Printout the class diff between two versions"
            if (diffClassFile.exists() && previousDiffFile.exists() && currentDiffFile.exists()) {
                task.diffClassFileProvider.set(diffClassFile)
                task.previousDiffFileProvider.set(previousDiffFile)
                task.currentDiffFileProvider.set(currentDiffFile)
            }
        }
    }

    /**
     * Register the [OutputClassGraphDotTask]
     */
    private fun registerOutputClassGraphDotTask(
        target: Project, diffClassFile: File, previousDiffFile: File, currentDiffFile: File
    ) {
        target.tasks.register(
            ClassGraphConstants.TASK_OUTPUT_CLASS_GRAPH_DIFF_DOT,
            OutputClassGraphDotTask::class.java
        ) { task ->
            task.group = ClassGraphConstants.TASK_GROUP_NAME
            task.description = "Generate the class graph dot file"
            if (target.hasProperty(ClassGraphConstants.PROPERTY_CLASS_CHANGE_TITLE)) {
                task.classChangeTitleProvider.set(
                    target.property(ClassGraphConstants.PROPERTY_CLASS_CHANGE_TITLE).toString()
                )
            }
            if (target.hasProperty(ClassGraphConstants.PROPERTY_OUTPUT_DOT_FILE)) {
                val outputDotFilePath =
                    target.property(ClassGraphConstants.PROPERTY_OUTPUT_DOT_FILE).toString()
                task.outputDotFileProvider.set(File(outputDotFilePath))
            }
            println("DiffClass:${diffClassFile.exists()} previous:${previousDiffFile.exists()} current:${currentDiffFile.exists()}")
            if (diffClassFile.exists() && previousDiffFile.exists() && currentDiffFile.exists()) {
                task.diffClassFileProvider.set(diffClassFile)
                task.previousDiffFileProvider.set(previousDiffFile)
                task.currentDiffFileProvider.set(currentDiffFile)
            }
        }
    }

    /**
     * Register the class graph transform.
     * This transform class helps build the class graph and collect all the android component.
     */
    private fun registerClassGraphTransform(target: Project, diffFile: File) {
        val sourceSets = GradleUtils.getSourceSets(target)
        println("The source set list:")
        println("------------------------------------------------------")
        sourceSets.forEach(System.out::println)
        println("------------------------------------------------------")
        val diffClasses =
            diffFile.readLines().filter { line -> line.endsWith(".java") || line.endsWith(".kt") }
                .mapNotNull { filePath ->
                    val classPath = filePath.substringBeforeLast(".").replace('/', '.')
                    val sourceSet = sourceSets.firstOrNull { classPath.startsWith(it) }
                    if (null == sourceSet) null else classPath.substring(sourceSet.length + 1)
                }
        println("The class list:")
        println("------------------------------------------------------")
        diffClasses.forEach(System.out::println)
        println("------------------------------------------------------")
        val androidExtension = target.extensions.getByType(AppExtension::class.java)
        androidExtension.registerTransform(ClassGraphBuildTransform(target, diffClasses))
    }

    fun getClassGraphExtension(): ClassGraphExtension = extension

    private fun isAndroidProject(project: Project): Boolean {
        return null != project.plugins.findPlugin("com.android.application")
    }
}