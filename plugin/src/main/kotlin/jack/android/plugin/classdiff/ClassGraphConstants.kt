package jack.android.plugin.classdiff

/**
 * All the plugin related constants
 */
interface ClassGraphConstants {
    companion object {
        /* The properties we will pass to the plugin */
        const val PROPERTY_CLASS_CHANGE_TITLE = "class_change_title"
        const val PROPERTY_OUTPUT_DOT_FILE = "output_dot_file"
        const val PROPERTY_DIFF_FILE = "diff_file"
        const val PROPERTY_DIFF_CLASS_FILE = "diff_class_file"
        const val PROPERTY_DIFF_OUTPUT_CLASS_FILE = "output_diff_classes"
        const val PROPERTY_PREVIOUS_DIFF_FILE = "previous_diff_file"
        const val PROPERTY_CURRENT_DIFF_FILE = "current_diff_file"

        /* The properties we will pass to the plugin */
        const val TASK_OUTPUT_DIFF_CLASSES = "outputDiffClassesTask"
        const val TASK_PRINT_OUT_CLASS_GRAPH_DIFF = "printOutClassGraphDiffTask"
        const val TASK_OUTPUT_CLASS_GRAPH_DIFF_DOT = "outputClassGraphDotTask"
        const val TASK_GROUP_NAME = "classdiff"
        const val PLUGIN_ID = "classdiff"
        const val OUTPUT_COMPONENT_DIR = "component_folder"
        const val OUTPUT_COMPONENT_FILE_NAME = "component_list.txt"
    }
}