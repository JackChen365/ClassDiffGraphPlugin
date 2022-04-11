package jack.android.plugin.classdiff.diff

/**
 * The class node.
 */
class ClassNode(
    val name: String,
    val superClass: String? = null,
    val interfaces: List<String>? = null,
    val innerClasses: List<ClassNode> = emptyList(),
    val fields: List<String> = emptyList(),
    val methods: Map<String, List<String>>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as ClassNode
        if (name != other.name) return false

        return true
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }

    override fun toString(): String {
        return "ClassNode(name='$name')"
    }


}