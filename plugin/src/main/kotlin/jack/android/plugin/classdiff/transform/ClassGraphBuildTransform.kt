package jack.android.plugin.classdiff.transform

import com.android.build.api.transform.*
import com.android.build.api.transform.QualifiedContent.DefaultContentType
import com.google.common.collect.ImmutableSet
import com.google.common.collect.Sets
import jack.android.plugin.classdiff.ClassGraphConstants
import jack.android.plugin.classdiff.analysis.ClassElement
import jack.android.plugin.classdiff.analysis.ClassElementAnalyzer
import jack.android.plugin.classdiff.graph.ClassElementGraph
import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.io.UncheckedIOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.function.Consumer
import java.util.jar.JarFile

/**
 * The class graph transform to collect all the AndroidComponent. besides,
 * We build a class graph to collect all the related class node.
 */
class ClassGraphBuildTransform(
    private val mProject: Project,
    private val mDiffClassList: List<String>
) : Transform() {

    private companion object {
        private const val TRANSFORM_NAME = "ClassGraphBuild"

        private const val SERIALIZE_NAME = "name"
        private const val SERIALIZE_SUPER_CLASS = "superClass"
        private const val SERIALIZE_INTERFACES = "interfaces"
        private const val SERIALIZE_FIELDS = "fields"
        private const val SERIALIZE_METHODS = "methods"
        private const val SERIALIZE_INNER_CLASSES = "innerClasses"
        private const val SERIALIZE_FILE_NAME = "class_references.json"

        private const val OBJECT_CLASS = "java.lang.Object"
        private val ANDROID_COMPONENT_LIST = mutableListOf(
            "androidx.fragment.app.Fragment",
            "androidx.fragment.app.DialogFragment",
            "androidx.fragment.app.AppCompatActivity",
            "androidx.fragment.app.FragmentActivity",
            "android.app.Dialog",
            "android.app.AlertDialog",
            "androidx.fragment.app.AlertDialog",
        )
    }

    override fun getName(): String {
        return TRANSFORM_NAME
    }

    override fun getInputTypes(): MutableSet<QualifiedContent.ContentType> {
        return ImmutableSet.of(DefaultContentType.CLASSES)
    }

    override fun getScopes(): MutableSet<in QualifiedContent.Scope> {
        return Sets.immutableEnumSet(QualifiedContent.Scope.PROJECT)
    }

    override fun isIncremental(): Boolean {
        return false
    }

    override fun transform(transformInvocation: TransformInvocation) {
        super.transform(transformInvocation)
        if (transformInvocation.isIncremental) {
            throw UnsupportedOperationException("Unsupported incremental build!")
        }
        val outputProvider = transformInvocation.outputProvider
        outputProvider.deleteAll()
        val classReferences = HashMap<String, ClassElement>()
        val clazzCallAnalyzer = ClassElementAnalyzer()
        //Copy all the jar and classes to the where they need to...
        for (input in transformInvocation.inputs) {
            input.jarInputs.parallelStream().forEach { jarInput: JarInput ->
                val dest = outputProvider.getContentLocation(
                    jarInput.name, jarInput.contentTypes, jarInput.scopes, Format.JAR
                )
                if (dest.exists()) {
                    throw RuntimeException(
                        "Jar file " + jarInput.name + " already exists!" + " src: " + jarInput.file.path + ", dest: " + dest.path
                    )
                }
                try {
                    analysisJarfile(classReferences, clazzCallAnalyzer, jarInput.file)
                    FileUtils.copyFile(jarInput.file, dest)
                } catch (e: IOException) {
                    throw UncheckedIOException(e)
                }
            }
        }
        for (input in transformInvocation.inputs) {
            input.directoryInputs.forEach(Consumer { dir: DirectoryInput ->
                try {
                    val file = dir.file
                    if (file.isDirectory) {
                        Files.walk(file.toPath()).filter { path: Path ->
                            val fileName = path.toFile().name
                            fileName.endsWith(".class") && !fileName.startsWith("R$") && "R.class" != fileName && "BuildConfig.class" != fileName
                        }.forEach { path: Path ->
                            val classFile = path.toFile()
                            val classPath =
                                classFile.absolutePath.substring(file.absolutePath.length + 1)
                            val className = classPath.replace('/', '.').removeSuffix(".class")
                            try {
                                analyzeClassBytes(
                                    className,
                                    classReferences,
                                    clazzCallAnalyzer,
                                    classFile.readBytes()
                                )
                            } catch (e: Exception) {
                                System.err.println("Process file:${classFile.name} failed.")
                            }
                        }
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                }
                try {
                    val destFolder =
                        outputProvider.getContentLocation(
                            dir.name,
                            dir.contentTypes,
                            dir.scopes,
                            Format.DIRECTORY
                        )
                    FileUtils.copyDirectory(dir.file, destFolder)
                } catch (e: IOException) {
                    throw UncheckedIOException(e)
                }
            })
        }
        val classGraph = ClassElementGraph()
        val classGraphMap: Map<String, ClassElementGraph.Node> = classGraph.build(classReferences)
        analyzeClassReferences(classGraphMap)
        analyzeComponentReferences(classGraphMap)
    }

    @Throws(IOException::class)
    fun analysisJarfile(
        classReferences: MutableMap<String, ClassElement>,
        classElementAnalyzer: ClassElementAnalyzer,
        file: File
    ) {
        val jarFile = JarFile(file)
        val jarEntries = jarFile.entries()
        while (jarEntries.hasMoreElements()) {
            val jarEntry = jarEntries.nextElement()
            val entryName = jarEntry.name
            if (entryName.endsWith(".class") && !entryName.contains("R$") && !entryName.endsWith("R.class")) {
                jarFile.getInputStream(jarEntry).use { inputStream ->
                    val className = entryName.replace('/', '.').removeSuffix(".class")
                    val bytes = inputStream.readBytes()
                    analyzeClassBytes(
                        className, classReferences, classElementAnalyzer, bytes
                    )
                }
            }
        }
    }

    /**
     * Process the the class byte and collect the class element.
     */
    private fun analyzeClassBytes(
        className: String,
        classReferences: MutableMap<String, ClassElement>,
        classElementAnalyzer: ClassElementAnalyzer,
        bytes: ByteArray
    ) {
        val classElement: ClassElement = classElementAnalyzer.getClassElement(bytes)
        classReferences[className] = classElement
    }

    /**
     * Use the class graph to analyze the class diff
     * For example: the diff class list only have one class, then we only collect the class related to the
     */
    private fun analyzeClassReferences(classGraphMap: Map<String, ClassElementGraph.Node>) {
        val traceClassSet: MutableSet<ClassElementGraph.Node> = mutableSetOf()
        mDiffClassList.forEach { className ->
            val classNode = classGraphMap[className]
            if (null != classNode) {
                traceClassSet.add(classNode)
                classNode.dependent?.forEach { node ->
                    val dependentNode = classGraphMap[node.className]
                    if (null != dependentNode) {
                        traceClassSet.add(dependentNode)
                    }
                }
                classNode.dependencies?.forEach { node ->
                    val dependenceNode = classGraphMap[node.className]
                    if (null != dependenceNode) {
                        traceClassSet.add(dependenceNode)
                    }
                }
            }
        }
        println("The diff class list:")
        println("-----------------------------------------------")
        traceClassSet.forEach { classNode ->
            println(classNode.className)
        }
        println("-----------------------------------------------")
        //Store the diff class node list
        serializeClassReferences(traceClassSet)
    }

    private fun analyzeComponentReferences(classGraphMap: Map<String, ClassElementGraph.Node>) {
        val componentList = mutableListOf<ClassElementGraph.Node>()
        classGraphMap.values.forEach { node ->
            if (!ANDROID_COMPONENT_LIST.contains(node.className)) {
                val androidComponent = findAndroidComponent(node)
                if (null != androidComponent) {
                    componentList.add(node)
                }
            }
        }
        println("The component class list:")
        println("-----------------------------------------------")
        componentList.forEach(System.out::println)
        println("-----------------------------------------------")
        val componentFolder = File(mProject.rootDir, ClassGraphConstants.OUTPUT_COMPONENT_DIR)
        if (!componentFolder.exists()) {
            componentFolder.mkdir()
        }
        File(componentFolder, ClassGraphConstants.OUTPUT_COMPONENT_FILE_NAME).bufferedWriter()
            .use { writer ->
                componentList.forEach { node ->
                    writer.write(node.className + "\n")
                }
            }
    }

    private fun findAndroidComponent(node: ClassElementGraph.Node?): ClassElementGraph.Node? {
        if (null == node) {
            return null
        } //Found the object, this means reach the root.
        if (OBJECT_CLASS == node.className) {
            return null
        } //Found the android component
        if (ANDROID_COMPONENT_LIST.contains(node.className)) {
            return node
        }
        return findAndroidComponent(node.superClass)
    }

    private fun serializeClassReferences(traceClassSet: Set<ClassElementGraph.Node>) {
        val jsonArray = JSONArray()
        traceClassSet.forEach { node -> //Add inner class
            val classJsonObject = getClassNodeJsonObject(node)
            val innerClassJsonArray = JSONArray()
            node.innerClasses?.forEach { innerClass ->
                val innerClassObject = getClassNodeJsonObject(innerClass)
                innerClassJsonArray.put(innerClassObject)
            }
            classJsonObject.put(SERIALIZE_INNER_CLASSES, innerClassJsonArray)
            jsonArray.put(classJsonObject)
        }
        val configurationFile = File(mProject.rootProject.rootDir, SERIALIZE_FILE_NAME)
        println("ConfigurationFile:" + configurationFile.absolutePath)
        configurationFile.writeText(jsonArray.toString())
    }

    private fun getClassNodeJsonObject(node: ClassElementGraph.Node): JSONObject {
        val jsonObject = JSONObject()
        jsonObject.put(SERIALIZE_NAME, node.className)
        if (node.superClass?.className != OBJECT_CLASS) {
            jsonObject.put(SERIALIZE_SUPER_CLASS, node.superClass?.className)
        }
        val interfaces = node.interfaces
        if (null != interfaces) {
            val interfaceArray = JSONArray()
            interfaces.forEach { interfaceName ->
                interfaceArray.put(interfaceName)
            }
            jsonObject.put(SERIALIZE_INTERFACES, interfaceArray)
        }
        val fieldArray = JSONArray()
        node.fieldNodes?.forEach { node ->
            fieldArray.put(node.className)
        }
        jsonObject.put(SERIALIZE_FIELDS, fieldArray)
        val methodObject = JSONObject()
        node.methodNodes?.forEach { methodNode ->
            val array = JSONArray()
            methodNode.value.forEach { node ->
                array.put(node.className)
            }
            methodObject.put(methodNode.key, array)
        }
        jsonObject.put(SERIALIZE_METHODS, methodObject)
        return jsonObject
    }
}