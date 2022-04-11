package jack.android.plugin.classdiff.analysis

import java.util.HashMap
import java.util.HashSet

/**
 * The class element build by the [ClassElementAnalyzer]
 *
 * the [classReferences] contains all the class references that used in this class no matter it is field or in method.
 */
class ClassElement(val className: String) {
    var superClass: String? = null
    var interfaces: List<String>? = null
    var classFields: MutableSet<String> = HashSet()
    var classMethods: MutableMap<String, MutableSet<String>> = HashMap()
    var classReferences: MutableSet<String> = HashSet()
}