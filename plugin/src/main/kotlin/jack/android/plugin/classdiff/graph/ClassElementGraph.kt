package jack.android.plugin.classdiff.graph

import jack.android.plugin.classdiff.analysis.ClassElement
import java.util.*

/**
 * This is a tool to analyze the class bytecode to know the whole invocation class graph.
 * Please check the test case: TestClassAnalysis for more information.
 */
class ClassElementGraph {
    companion object {
        private const val OBJECT_CLASS = "java.lang.Object"
    }

    /**
     * Build the class graph by the given class reference list.
     * The class: [Node] include the class dependency and dependent so we can use the class graph to analyze the diff.
     */
    fun build(classReferences: Map<String, ClassElement?>): Map<String, Node> {
        val nodeMap: MutableMap<String, Node> = HashMap()
        for ((className, classElement) in classReferences) {
            if (null == classElement) continue
            val classNode = obtainClassNode(nodeMap, className)
            val superClass = classElement.superClass
            if (null != superClass) {
                classNode.superClass = obtainClassNode(nodeMap, superClass)
            }
            classNode.interfaces = classElement.interfaces?.map { it.replace("/", ".") }
            classElement.classFields.forEach { field ->
                val fieldClassNode = obtainClassNode(nodeMap, field)
                classNode.addFieldReference(fieldClassNode)
            }
            classElement.classMethods.forEach { (method, references) ->
                references.forEach { reference ->
                    val fieldClassNode = obtainClassNode(nodeMap, reference)
                    classNode.addMethodReference(method, fieldClassNode)
                }
            }
            for (classReferenceName in classElement.classReferences) {
                val relateClassNode = obtainClassNode(nodeMap, classReferenceName)
                classNode.addDependency(relateClassNode)
                relateClassNode.addDependent(classNode)
            }
            //Add the inner class.
            if (className.contains("$")) {
                //inner class, We are supposed to change its class name.
                val superClassNode = classNode.superClass
                if (null != superClassNode && superClassNode.className != OBJECT_CLASS) {
                    val superClassName = superClassNode.className.substringAfterLast(".")
                    classNode.className += ":(${superClassName})"
                }
                val interfaces = classNode.interfaces
                if (null != interfaces && interfaces.isNotEmpty()) {
                    val interfaceArray = interfaces.joinToString(separator = ",") { interfaceName ->
                        interfaceName.substringAfterLast(".")
                    }
                    classNode.className += ":I[$interfaceArray]"
                }
                val parentClass = className.substringBeforeLast("$")
                val parentClassNode = obtainClassNode(nodeMap, parentClass)
                parentClassNode.addInnerClass(classNode)
            }
        }
        //Remove the inner class and return the class map.
        return nodeMap.filterKeys { !it.contains("$") }
    }

    /**
     * Obtain the class node from the cache map. If it doesn't exist, We will create a new one.
     */
    private fun obtainClassNode(nodeMap: MutableMap<String, Node>, className: String): Node {
        var node = nodeMap[className]
        if (null == node) {
            node = Node(className)
            nodeMap[className] = node
        }
        return node
    }

    class Node(var className: String) {
        var superClass: Node? = null
        var interfaces: List<String>? = null
        var innerClasses: MutableList<Node>? = null
        var fieldNodes: MutableList<Node>? = null
        var methodNodes: MutableMap<String, MutableList<Node>>? = null
        var dependent: MutableSet<Node>? = null
        var dependencies: MutableSet<Node>? = null

        fun addDependent(node: Node) {
            if (null == dependent) {
                dependent = HashSet()
            }
            dependent?.add(node)
        }

        fun addDependency(node: Node) {
            if (null == dependencies) {
                dependencies = HashSet()
            }
            dependencies?.add(node)
        }

        fun addInnerClass(node: Node) {
            if (null == innerClasses) {
                innerClasses = ArrayList()
            }
            innerClasses?.add(node)
        }

        fun addFieldReference(fieldClassNode: Node) {
            if (null == fieldNodes) {
                fieldNodes = ArrayList()
            }
            fieldNodes?.add(fieldClassNode)
        }

        fun addMethodReference(name: String, classNode: Node) {
            if (null == methodNodes) {
                methodNodes = mutableMapOf()
            }
            var nodeList = methodNodes?.get(name)
            if (null == nodeList) {
                nodeList = mutableListOf()
                methodNodes?.put(name, nodeList)
            }
            nodeList.add(classNode)
        }

        override fun equals(o: Any?): Boolean {
            if (this === o) return true
            if (o == null || javaClass != o.javaClass) return false
            val node = o as Node
            return className == node.className
        }

        override fun hashCode(): Int {
            return Objects.hash(className)
        }

        override fun toString(): String {
            return "Node{className='$className'}"
        }
    }
}