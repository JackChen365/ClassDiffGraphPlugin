package jack.android.plugin.classdiff.analysis

import org.objectweb.asm.ClassReader
import org.objectweb.asm.Handle
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.*
import java.util.*

/**
 * The class element analyzer
 */
class ClassElementAnalyzer {
    companion object {
        private val FILTER_PACKAGE = listOf("org.objectweb.asm")
    }

    /**
     *
     */
    fun getClassElement(classBytes: ByteArray): ClassElement {
        val classReader = ClassReader(classBytes, 0, classBytes.size)
        val classNode = ClassNode()
        classReader.accept(classNode, ClassReader.EXPAND_FRAMES)
        val thisClassName = clazzDescToName(classNode.name)
        val classElement = ClassElement(thisClassName) //super class
        addSuperClassReferences(classNode, classElement) //super interface
        addSuperInterfaceReferences(classNode, classElement)
        addClassInFields(classElement, classNode)
        addClassFromMethod(classElement, classNode)
        return classElement
    }

    private fun addSuperClassReferences(classNode: ClassNode, classElement: ClassElement) {
        val className = clazzDescToName(classNode.superName)
        classElement.superClass = className
        classElement.interfaces = classNode.interfaces
        val classReference = clazzDescToName(classNode.superName)
        addClassReference(classElement, className, classReference)
    }

    private fun addSuperInterfaceReferences(
        classNode: ClassNode, classElement: ClassElement
    ) {
        if (classNode.interfaces != null) {
            for (anInterface in classNode.interfaces) {
                val interfaceName = anInterface.toString()
                val className = clazzDescToName(interfaceName)
                val classReference = clazzDescToName(interfaceName)
                addClassReference(classElement, className, classReference)
            }
        }
    }

    private fun addClassInFields(classElement: ClassElement, classNode: ClassNode) {
        val fieldList = classNode.fields
        for (fieldNode in fieldList) {
            val className = Type.getType(fieldNode.desc).className
            addFieldClassReference(classElement, className, className)
        }
    }

    private fun addClassFromMethod(classElement: ClassElement, classNode: ClassNode) {
        val methodList = classNode.methods
        val thisClassName = clazzDescToName(classNode.name)
        for (methodNode in methodList) { //Argument list.
            if (null != methodNode.desc) {
                for (argumentType in Type.getArgumentTypes(methodNode.desc)) {
                    val className = argumentType.className
                    val classReference = argumentType.className
                    if (!classReference.equals(thisClassName)) {
                        addClassReference(classElement, className, classReference)
                        addMethodClassReference(
                            classElement,
                            methodNode.name,
                            thisClassName,
                            classReference
                        )
                    }
                }
            } //LocalVar
            val lvNodeList = methodNode.localVariables
            if (null != lvNodeList) {
                for (lvn in lvNodeList) {
                    val className = Type.getType(lvn.desc).className
                    if (!className.equals(thisClassName)) {
                        addClassReference(classElement, className, className)
                        addMethodClassReference(
                            classElement,
                            methodNode.name,
                            thisClassName,
                            className
                        )
                    }
                }
            }

            //Exception
            val tryCatchBlockNodes = methodNode.tryCatchBlocks
            if (tryCatchBlockNodes != null) {
                for (tryCatchBlockNode in tryCatchBlockNodes) {
                    if (null != tryCatchBlockNode.type) {
                        val className = clazzDescToName(tryCatchBlockNode.type)
                        if (className != thisClassName) {
                            addClassReference(classElement, className, className)
                            addMethodClassReference(
                                classElement,
                                methodNode.name,
                                thisClassName,
                                className
                            )
                        }
                    }
                }
            } //the reference inside the method.
            addClassMethodInner(classElement, methodNode, thisClassName)
        }
        classElement.classReferences.remove(thisClassName)
    }

    private fun addClassMethodInner(
        classElement: ClassElement,
        methodNode: MethodNode,
        thisClassName: String
    ) {
        val itr: Iterator<AbstractInsnNode> = methodNode.instructions.iterator(0)
        while (itr.hasNext()) {
            val insn = itr.next()
            when (insn.type) {
                AbstractInsnNode.FIELD_INSN -> {
                    val fieldInsn = insn as FieldInsnNode
                    val fieldClassName = clazzDescToName(fieldInsn.owner)
                    addMethodClassReference(
                        classElement, methodNode.name, thisClassName, fieldClassName
                    )
                }
                AbstractInsnNode.METHOD_INSN -> {
                    val methodInsn = insn as MethodInsnNode
                    addMethodClassReference(
                        classElement,
                        methodNode.name,
                        thisClassName,
                        clazzDescToName(methodInsn.owner)
                    )
                }
                AbstractInsnNode.TYPE_INSN -> {
                    val typeInsnDesc = (insn as TypeInsnNode).desc
                    if (insn.getOpcode() == Opcodes.ANEWARRAY) {
                        addMethodClassReference(
                            classElement,
                            methodNode.name,
                            thisClassName,
                            clazzDescToName(typeInsnDesc)
                        )
                    } else {
                        addMethodClassReference(
                            classElement,
                            methodNode.name,
                            thisClassName,
                            clazzDescToName(typeInsnDesc)
                        )
                    }
                }
                AbstractInsnNode.INVOKE_DYNAMIC_INSN -> {
                    val dynamicInsn = insn as InvokeDynamicInsnNode
                    if (dynamicInsn.bsm != null && dynamicInsn.bsm is Handle) {
                        val bsmHandle = dynamicInsn.bsm
                        val dynamicInsnClassName = clazzDescToName(bsmHandle.owner)
                        addMethodClassReference(
                            classElement,
                            methodNode.name,
                            thisClassName,
                            dynamicInsnClassName
                        )
                    }
                    if (dynamicInsn.bsmArgs != null) {
                        for (bsmArg in dynamicInsn.bsmArgs) {
                            if (bsmArg is Handle) {
                                val clazzDescToName = clazzDescToName(bsmArg.owner)
                                addMethodClassReference(
                                    classElement,
                                    methodNode.name,
                                    thisClassName,
                                    clazzDescToName
                                )
                            }
                        }
                    }
                }
                AbstractInsnNode.LDC_INSN -> {
                    val cst = (insn as LdcInsnNode).cst
                    if (cst is Type) {
                        addMethodClassReference(
                            classElement, methodNode.name, thisClassName, cst.className
                        )
                    }
                }
                AbstractInsnNode.MULTIANEWARRAY_INSN -> {
                    val multiANewArrayInsn = insn as MultiANewArrayInsnNode
                    val clazzDescToName = clazzDescToName(multiANewArrayInsn.desc)
                    addMethodClassReference(
                        classElement,
                        methodNode.name,
                        thisClassName,
                        clazzDescToName
                    )
                }
            }
        }
    }

    private fun addClassReference(
        classElement: ClassElement, className: String, classReference: String
    ) {
        for (filterPackage in FILTER_PACKAGE) {
            if (!className.startsWith(filterPackage)) {
                classElement.classReferences.add(classReference)
            }
        }
    }

    private fun addFieldClassReference(
        classElement: ClassElement,
        className: String,
        classReference: String
    ) {
        for (packageName in FILTER_PACKAGE) {
            if (!className.startsWith(packageName)) {
                classElement.classFields.add(classReference)
                classElement.classReferences.add(classReference)
            }
        }
    }

    private fun addMethodClassReference(
        classElement: ClassElement,
        methodName: String,
        thisClassReference: String,
        classReference: String
    ) {
        if (thisClassReference != classReference) {
            for (packageName in FILTER_PACKAGE) {
                if (!classElement.className.startsWith(packageName)) {
                    var classReferenceList = classElement.classMethods[methodName]
                    if (null == classReferenceList) {
                        classReferenceList = HashSet()
                        classElement.classMethods[methodName] = classReferenceList
                    }
                    classReferenceList.add(classReference)
                    classElement.classReferences.add(classReference)
                }
            }
        }
    }

    private fun clazzDescToName(clazzDesc: String): String {
        return if (clazzDesc[0] == '[') {
            Type.getType(clazzDesc).elementType.className
        } else clazzDesc.replace('/', '.')
    }
}