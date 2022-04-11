package jack.android.plugin.classdiff.task

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import jack.android.plugin.classdiff.ClassGraphConstants
import jack.android.plugin.classdiff.ClassGraphPlugin
import jack.android.plugin.classdiff.diff.ClassDiffHelper
import jack.android.plugin.classdiff.diff.ClassNode
import jack.android.plugin.classdiff.dot.DotGenerator
import jack.android.plugin.classdiff.dot.DotNode
import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.lang.reflect.Type

abstract class OutputClassGraphDotTask : DefaultTask() {
    companion object {
        private const val DEFAULT_CLASS_CHANGE_TITLE = "class change graph"
    }

    @get:Input
    abstract val diffClassFileProvider: Property<File>

    @get:Input
    abstract val previousDiffFileProvider: Property<File>

    @get:Input
    abstract val currentDiffFileProvider: Property<File>

    @get:Input
    abstract val classChangeTitleProvider: Property<String>

    @get:Input
    abstract val outputDotFileProvider: Property<File>

    @TaskAction
    fun outputClassGraphDiffDotTask() {
        if (!diffClassFileProvider.isPresent || !previousDiffFileProvider.isPresent || !currentDiffFileProvider.isPresent || !outputDotFileProvider.isPresent) return
        val diffClassFile = diffClassFileProvider.get()
        val previousDiffFile = previousDiffFileProvider.get()
        val currentDiffFile = currentDiffFileProvider.get()
        val outputDotFile = outputDotFileProvider.get()
        var classChangeTitle = DEFAULT_CLASS_CHANGE_TITLE
        if (classChangeTitleProvider.isPresent) {
            classChangeTitle = classChangeTitleProvider.get()
        }
        val gson = Gson()
        val type: Type = object : TypeToken<ArrayList<ClassNode>>() {}.type
        val previousClassList: List<ClassNode> = gson.fromJson(previousDiffFile.readText(), type)
        val currentClassList: List<ClassNode> = gson.fromJson(currentDiffFile.readText(), type)
        val diffClassList = diffClassFile.readLines()

        val classOperationList =
            ClassDiffHelper.compareClassReferences(
                previousClassList,
                currentClassList,
                diffClassList
            )
        var classGraph = mutableMapOf<String, MutableSet<DotNode>>()
        classOperationList.forEach { operation ->
            visitClassOperation(operation, classGraph)
        }
        if (!project.buildDir.exists()) {
            project.buildDir.mkdir()
        }
        val classGraphPlugin = project.plugins.findPlugin(ClassGraphPlugin::class.java)
        val classGraphExtension = classGraphPlugin?.getClassGraphExtension()
        val componentMapper = mutableMapOf<String, String>()
        val componentFiles = classGraphExtension?.getComponentFiles()
        //Default file
        val componentFileList = mutableListOf<File>()
        val componentFolder =
            File(project.rootProject.projectDir, ClassGraphConstants.OUTPUT_COMPONENT_DIR)
        if (null != componentFolder.listFiles()) {
            componentFileList.addAll(componentFolder.listFiles())
        }
        if (null != componentFiles) {
            componentFileList.addAll(componentFiles)
        }
        componentFileList.forEach { componentFile ->
            componentFile.forEachLine { component ->
                val simpleClassName = component.substringAfterLast(".")
                componentMapper[component] =
                    simpleClassName.replace("(Activity)|(Fragment)".toRegex(), "")
            }
        }
        val libraryMapper = classGraphExtension?.getLibraryMapper() ?: emptyMap()
        val filterPackageList = classGraphExtension?.getFilterPackageList() ?: emptyList()
        println("ClassGraphExtension:$classGraphExtension")
        println("The filter package:$filterPackageList")
        println("The library mapper:$libraryMapper")
        DotGenerator().generate(
            outputDotFile,
            classGraph.filterValues { it.isNotEmpty() },
            componentMapper,
            libraryMapper,
            filterPackageList,
            classChangeTitle
        )
    }

    private fun visitClassOperation(
        operation: ClassDiffHelper.ClassOperation,
        classGraph: MutableMap<String, MutableSet<DotNode>>
    ) {
        if (null == operation) return
        if (operation is ClassDiffHelper.ClassInsert) {
            visitClassInsert(operation, classGraph)
        }
        if (operation is ClassDiffHelper.ClassRemove) {
            visitClassRemove(operation, classGraph)
        }
        if (operation is ClassDiffHelper.ClassFieldInsert) {
            visitClassFieldInsert(operation, classGraph)
        }
        if (operation is ClassDiffHelper.ClassFieldRemove) {
            visitClassFieldRemove(operation, classGraph)
        }
        if (operation is ClassDiffHelper.ClassMethodInsert) {
            visitClassMethodInsert(operation, classGraph)
        }
        if (operation is ClassDiffHelper.ClassMethodRemove) {
            visitClassMethodRemove(operation, classGraph)
        }
        if (operation is ClassDiffHelper.ClassMethodChange) {
            visitClassMethodChange(operation, classGraph)
        }
        if (operation is ClassDiffHelper.ClassChange) {
            operation.classOperations.forEach { operation ->
                visitClassOperation(operation, classGraph)
            }
        }
    }

    private fun visitClassInsert(
        operation: ClassDiffHelper.ClassInsert, classGraph: MutableMap<String, MutableSet<DotNode>>
    ) {
        val classNode = operation.node
        val classRefSet = mutableSetOf<String>()
        classRefSet.addAll(classNode.fields.toMutableSet())
        classRefSet.addAll(classNode.methods.values.flatten().toSet())
        classNode.innerClasses?.forEach { innerClassNode ->
            classRefSet.addAll(getClassRefSet(innerClassNode))
        }
        classRefSet.remove(classNode.name)
        if (classRefSet.isNotEmpty()) {
            classGraph[classNode.name] =
                classRefSet.map { DotNode(it, "+") }.toMutableSet()
        }
    }

    private fun visitClassRemove(
        operation: ClassDiffHelper.ClassRemove, classGraph: MutableMap<String, MutableSet<DotNode>>
    ) {
        val classNode = operation.node
        val classRefSet = HashSet<String>()
        classRefSet.addAll(classNode.fields.toMutableSet())
        classRefSet.addAll(classNode.methods.values.flatten())
        classNode.innerClasses?.forEach { innerClassNode ->
            classRefSet.addAll(getClassRefSet(innerClassNode))
        }
        classRefSet.remove(classNode.name)
        if (classRefSet.isNotEmpty()) {
            classGraph[classNode.name] = classRefSet.map {
                DotNode(it, "-")
            }.toMutableSet()
        }
    }

    private fun visitClassFieldInsert(
        operation: ClassDiffHelper.ClassFieldInsert,
        classGraph: MutableMap<String, MutableSet<DotNode>>
    ) {
        val nodeSet = classGraph.getOrPut(operation.className) { mutableSetOf() }
        nodeSet.add(DotNode(operation.field, "+"))
    }

    private fun visitClassFieldRemove(
        operation: ClassDiffHelper.ClassFieldRemove,
        classGraph: MutableMap<String, MutableSet<DotNode>>
    ) {
        val nodeSet = classGraph.getOrPut(operation.className) { mutableSetOf() }
        nodeSet.add(DotNode(operation.field, "-"))
    }

    private fun visitClassMethodRemove(
        operation: ClassDiffHelper.ClassMethodRemove,
        classGraph: MutableMap<String, MutableSet<DotNode>>
    ) {
        val nodeSet = classGraph.getOrPut(operation.className) { mutableSetOf() }
        operation.methodRefs.forEach { methodRef ->
            nodeSet.add(DotNode(methodRef, "-"))
        }
    }

    private fun visitClassMethodInsert(
        operation: ClassDiffHelper.ClassMethodInsert,
        classGraph: MutableMap<String, MutableSet<DotNode>>
    ) {
        val nodeSet = classGraph.getOrPut(operation.className) { mutableSetOf() }
        operation.methodRefs.forEach { methodRef ->
            nodeSet.add(DotNode(methodRef, "+"))
        }
    }

    private fun visitClassMethodChange(
        operation: ClassDiffHelper.ClassMethodChange,
        classGraph: MutableMap<String, MutableSet<DotNode>>
    ) {
        val nodeSet = classGraph.getOrPut(operation.className) { mutableSetOf() }
        operation.methodRefDeleteList.forEach { classRef ->
            nodeSet.add(DotNode(classRef, "-"))
        }
        operation.methodRefInsertList.forEach { classRef ->
            nodeSet.add(DotNode(classRef, "+"))
        }
    }

    private fun getClassRefSet(classNode: ClassNode): Set<String> {
        val classRefSet = mutableSetOf<String>()
        classRefSet.addAll(classNode.methods.values.flatten())
        classRefSet.addAll(classNode.fields)
        return classRefSet
    }
}