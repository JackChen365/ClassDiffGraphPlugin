package jack.android.plugin.classdiff.dot

/**
 * The generic node for the Dot.
 * If you want to visualize your tree or graph.
 * You can convert your tree's node to this generic node then use [DotGenerator] to generate the dot file.
 */
class DotNode(val name: String, val label: String) {

    override fun toString(): String {
        return name
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DotNode

        if (name != other.name) return false
        if (label != other.label) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + label.hashCode()
        return result
    }
}