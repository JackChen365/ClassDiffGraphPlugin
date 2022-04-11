package jack.android.plugin.classdiff

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import jack.android.plugin.classdiff.diff.ClassNode
import org.apache.commons.io.FileUtils
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Shared
import spock.lang.Specification

import java.nio.file.Files

/**
 *
 */
class BuildClassGraphTest extends Specification {
    @Rule
    TemporaryFolder testProjectDir = new TemporaryFolder(new File("build/tmp"))
    @Shared
    private def diffInputAssetsProvider = new TestAssetsProvider("src", "test", "assets", "file")

    def "test class graph"() {
        given:
        FileUtils.copyDirectory(diffInputAssetsProvider.functionalAssetsDir, testProjectDir.root)
        def classReferencesBefore = new File(testProjectDir.root, "class_references_before.json")
        def classReferencesAfter = new File(testProjectDir.root, "class_references_after.json")
        def gson = new Gson()
        def type = new TypeToken<ArrayList<ClassNode>>() {}.type
        def beforeText = new String(Files.readAllBytes(classReferencesBefore.toPath()))
        def afterText = new String(Files.readAllBytes(classReferencesAfter.toPath()))
        def previousNodeList = gson.fromJson(beforeText, type)
        def currentNodeList = gson.fromJson(afterText, type)
        expect:
        previousNodeList.size != currentNodeList.size
    }
}