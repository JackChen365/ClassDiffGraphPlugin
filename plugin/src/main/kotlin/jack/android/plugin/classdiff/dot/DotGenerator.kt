package jack.android.plugin.classdiff.dot

import java.io.File
import kotlin.math.abs

/**
 * Output the graph into a dot file in order to visualize the graph.
 * We use the dot engine to visualize all the diff graph.
 * Visit this website to see how your dot file looks like.
 * https://dreampuf.github.io/GraphvizOnline/
 *
 */
class DotGenerator {
    companion object {
        /**
         * Display horizontally
         */
        const val RANK_DIR_LR = "LR"

        /**
         * Display vertically
         */
        const val RANK_DIR_TB = "TB"
    }

    /**
     * @param dest The output dot file.
     * @param classGraph The root node for the graph or tree
     * @param title The title of the graph or tree
     * @param rankDir This param determine the graph displayed horizontally or vertically.
     * [RANK_DIR_TB] means top to bottom
     * [RANK_DIR_LR] means left to right
     */
    fun generate(
        dest: File,
        classGraph: Map<String, Set<DotNode>>,
        componentMapper: Map<String, String>,
        libraryMapper: Map<String, String>,
        filterPackageList: List<String>,
        title: String? = null,
        rankDir: String = RANK_DIR_LR,
    ) {
        val output = StringBuilder(
            """
            digraph astgraph {
              node [fontsize=36, height=1];rankdir = "$rankDir";
            """.trimIndent()
        )
        if (null != title) {
            output.append("labelloc = \"t\";label = \"$title\";fontcolor=black;fontsize=64;")
        }
        output.append("\n")
        val nodeLabelSet = mutableSetOf<String>()
        val nodeConnectMap = mutableMapOf<String, MutableSet<String>>()
        for ((name, nodeList) in classGraph) {
            if (filterPackageList.contains(name)) continue
            val currentLabel =
                addNodeLabel(libraryMapper, componentMapper, nodeLabelSet, output, name)
            for (node in nodeList) {
                if (filterPackageList.contains(node.name)) continue
                val nodeLabel =
                    addNodeLabel(
                        libraryMapper,
                        componentMapper,
                        nodeLabelSet,
                        output,
                        node.toString()
                    )
                var labelSet = nodeConnectMap[currentLabel]
                if (null == labelSet) {
                    labelSet = mutableSetOf()
                    nodeConnectMap[currentLabel] = labelSet
                }
                if (!labelSet.contains(nodeLabel)) {
                    labelSet.add(nodeLabel)
                    output.append(
                        "\tnode_${abs(currentLabel.hashCode())} -> node_${abs(nodeLabel.hashCode())} [label=\"${node.label}\";fontsize=48;arrowsize=2]\n"
                    )
                }
            }
        }
        output.append("}")
        dest.writeText(output.toString())
    }

    private fun addNodeLabel(
        libraryMapper: Map<String, String>,
        componentMapper: Map<String, String>,
        nodeLabelSet: MutableSet<String>,
        output: StringBuilder,
        name: String
    ): String {
        //Try to find the component
        var highLightStyle = ""
        var nodeLabel = name.substringAfterLast(".")
        val component = componentMapper[name]
        if (null != component) {
            nodeLabel = component
            highLightStyle = "; color = red; fontcolor=red"
        }
        //Check whether the package belongs to a library.
        val libraryDesc =
            libraryMapper.entries.find { (library, _) -> name.startsWith(library) }?.value
        if (null != libraryDesc) {
            nodeLabel = libraryDesc
            highLightStyle = "; color = green; fontcolor=green"
        }
        if (!nodeLabelSet.contains(nodeLabel)) {
            nodeLabelSet.add(nodeLabel)
            output.append("\tnode_${abs(nodeLabel.hashCode())} [label = \"$nodeLabel\"$highLightStyle]\n")
        }
        return nodeLabel
    }

}