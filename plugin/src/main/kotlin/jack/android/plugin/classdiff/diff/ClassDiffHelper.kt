package jack.android.plugin.classdiff.diff

/**
 * The class diff helper. We use this class to output the diff between two commits.
 * The function: [compareClassReferences] will give us a detail about how the class has changed.
 */
object ClassDiffHelper {

    /**
     * Output how the class has changed
     * @param previousList the previous class node list.
     * @param currentList the current class node list.
     * @param diffClassList we use this list to reduce the class graph.
     */
    fun outputClassReferencesDiff(
        previousList: List<ClassNode>,
        currentList: List<ClassNode>,
        diffClassList: List<String>? = null
    ): Boolean {
        val classOperationList = compareClassReferences(previousList, currentList, diffClassList)
        return classOperationList.isNotEmpty()
    }

    /**
     * Compare two class commit record.
     * @param previousList the previous class node list.
     * @param currentList the current class node list.
     * @param diffClassList we use this list to reduce the class graph.
     *
     * @return the class operation list. So we can use the [ClassOperation] to build the class diff graph.
     */
    fun compareClassReferences(
        previousList: List<ClassNode>,
        currentList: List<ClassNode>,
        diffClassList: List<String>? = null
    ): List<ClassOperation> {
        val classOperations = mutableListOf<ClassOperation>()
        val mutableClassList = currentList.toMutableList()
        val removedClassRefs = previousList.toMutableList()
        val insertClassRefs = currentList.toMutableList()
        removedClassRefs.removeAll(currentList)
        insertClassRefs.removeAll(previousList)
        if (removedClassRefs.isNotEmpty() || insertClassRefs.isNotEmpty()) {
            removedClassRefs.forEach { classNode ->
                println("- ${classNode.name}")
                classOperations.add(ClassRemove(classNode))
            }
            insertClassRefs.forEach { classNode ->
                println("+ ${classNode.name}")
                mutableClassList.remove(classNode)
                classOperations.add(ClassInsert(classNode))
            }
        }
        val previousClassMap = previousList.associateBy { node -> node.name }
        mutableClassList.forEach { currentNode ->
            val previousNode = previousClassMap[currentNode.name]
            if (null != previousNode) {
                if (null == diffClassList || diffClassList.contains(previousNode.name)) {
                    val classChange = ClassChange(currentNode, mutableListOf())
                    println("Class:${previousNode.name}")
                    compareClassFields(
                        currentNode.name,
                        classChange.classOperations,
                        previousNode.fields,
                        currentNode.fields
                    )
                    compareClassMethods(
                        currentNode,
                        classChange.classOperations,
                        previousNode.methods,
                        currentNode.methods
                    )
                    if (classChange.classOperations.isNotEmpty()) {
                        classOperations.add(classChange)
                    }
                }
            }
        }
        return classOperations
    }

    /**
     * Compare the fields of the two versions of the class
     */
    private fun compareClassFields(
        className: String,
        classOperations: MutableList<ClassOperation>,
        previousFields: List<String>,
        currentFields: List<String>
    ) {
        val removedClassRefs = previousFields.toMutableList()
        val insertClassRefs = currentFields.toMutableList()
        removedClassRefs.removeAll(currentFields)
        insertClassRefs.removeAll(previousFields)
        if (removedClassRefs.isNotEmpty() || insertClassRefs.isNotEmpty()) {
            removedClassRefs.forEach { classRef ->
                println("\t- $classRef")
                classOperations.add(ClassFieldRemove(className, classRef))
            }
            insertClassRefs.forEach { classRef ->
                println("\t+ $classRef")
                classOperations.add(ClassFieldInsert(className, classRef))
            }
        }
    }

    /**
     * Compare the methods of the two versions of the class
     */
    private fun compareClassMethods(
        classNode: ClassNode,
        classOperations: MutableList<ClassOperation>,
        previousMethods: Map<String, List<String>>,
        currentMethods: Map<String, List<String>>
    ): List<ClassOperation> {
        val mutableCurrentMethods = currentMethods.toMutableMap()
        val removeMethodList = previousMethods.keys.toMutableList()
        val insertMethodList = currentMethods.keys.toMutableList()
        removeMethodList.removeAll(previousMethods.keys)
        insertMethodList.removeAll(previousMethods.keys)
        if (removeMethodList.isNotEmpty() || insertMethodList.isNotEmpty()) {
            removeMethodList.forEach { classRef ->
                println("- $classRef")
                val methodClassRefs = classNode.methods.getOrDefault(classRef, emptyList())
                classOperations.add(ClassMethodRemove(classNode.name, classRef, methodClassRefs))
            }
            insertMethodList.forEach { classRef ->
                println("+ $classRef")
                mutableCurrentMethods.remove(classRef)
                val methodClassRefs = classNode.methods.getOrDefault(classRef, emptyList())
                classOperations.add(ClassMethodInsert(classNode.name, classRef, methodClassRefs))
            }
        }
        mutableCurrentMethods.forEach { (name, currentRefs) ->
            val previousRefs = previousMethods[name]
            if (null != previousRefs) {
                val removedClassRefs = previousRefs.toMutableList()
                val insertClassRefs = currentRefs.toMutableList()
                removedClassRefs.removeAll(currentRefs)
                insertClassRefs.removeAll(previousRefs)
                if (removedClassRefs.isNotEmpty() || insertClassRefs.isNotEmpty()) {
                    println("\tmethod:$name")
                    val classMethodChange =
                        ClassMethodChange(classNode.name, name, mutableListOf(), mutableListOf())
                    removedClassRefs.forEach { classRef ->
                        println("\t\t- $classRef")
                        classMethodChange.methodRefDeleteList.add(classRef)
                    }
                    insertClassRefs.forEach { classRef ->
                        println("\t\t+ $classRef")
                        classMethodChange.methodRefInsertList.add(classRef)
                    }
                    classOperations.add(classMethodChange)
                }
            }
        }
        return classOperations
    }

    open class ClassOperation

    class ClassInsert(val node: ClassNode) : ClassOperation() {
        override fun toString(): String {
            return "ClassInsert{item=$node}"
        }
    }

    class ClassRemove(val node: ClassNode) : ClassOperation() {
        override fun toString(): String {
            return "ClassRemove{item=$node}"
        }
    }

    class ClassFieldInsert(val className: String, val field: String) : ClassOperation() {
        override fun toString(): String {
            return "ClassFieldInsert{item=$field}"
        }
    }

    class ClassFieldRemove(val className: String, val field: String) : ClassOperation() {
        override fun toString(): String {
            return "ClassFieldRemove{item=$field}"
        }
    }

    class ClassMethodRemove(
        val className: String, val method: String,
        val methodRefs: List<String>
    ) : ClassOperation() {
        override fun toString(): String {
            return "ClassMethodRemove{item=$method}"
        }
    }

    class ClassMethodInsert(
        val className: String,
        val method: String,
        val methodRefs: List<String>
    ) :
        ClassOperation() {
        override fun toString(): String {
            return "ClassMethodInsert{item=$method, methodRefs:$methodRefs}"
        }
    }

    class ClassMethodChange(
        val className: String,
        val method: String,
        val methodRefDeleteList: MutableList<String>,
        val methodRefInsertList: MutableList<String>
    ) : ClassOperation() {
        override fun toString(): String {
            return "ClassMethodChange{item=$method, DeleteRefs:$methodRefDeleteList, InsertRefs:$methodRefInsertList}"
        }
    }

    class ClassChange(val classNode: ClassNode, val classOperations: MutableList<ClassOperation>) :
        ClassOperation() {
        override fun toString(): String {
            return "ClassChange{item=${classNode.name}, ClassChange:$classOperations}"
        }
    }

}