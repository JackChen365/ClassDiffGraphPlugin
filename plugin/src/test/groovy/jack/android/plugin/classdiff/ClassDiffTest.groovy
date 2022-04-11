package jack.android.plugin.classdiff

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import jack.android.plugin.classdiff.diff.ClassNode
import jack.android.plugin.classdiff.diff.ClassDiffHelper
import org.apache.commons.io.FileUtils
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Shared
import spock.lang.Specification

import java.nio.file.Files

class ClassDiffTest extends Specification {
    @Rule
    TemporaryFolder testProjectDir = new TemporaryFolder(new File("build/tmp"))
    @Shared
    private def diffInputAssetsProvider = new TestAssetsProvider("src", "test", "assets", "file")

    def "test json"() {
        given:
        FileUtils.copyDirectory(diffInputAssetsProvider.functionalAssetsDir, testProjectDir.root)
        def previousClassReferencesFile = new File(testProjectDir.root, "class_references_before.json")
        def currentClassReferencesFile = new File(testProjectDir.root, "class_references_after.json")
        def gson = new Gson()
        def type = new TypeToken<ArrayList<ClassNode>>() {}.type
        def beforeText = new String(Files.readAllBytes(previousClassReferencesFile.toPath()))
        def afterText = new String(Files.readAllBytes(currentClassReferencesFile.toPath()))
        def previousNodeList = gson.fromJson(beforeText, type)
        def currentNodeList = gson.fromJson(afterText, type)
        expect:
        previousNodeList.size != currentNodeList.size
    }

    def "test diff"() {
        given:
        FileUtils.copyDirectory(diffInputAssetsProvider.functionalAssetsDir, testProjectDir.root)
        def previousClassReferencesFile = new File(testProjectDir.root, "class_references_before.json")
        def currentClassReferencesFile = new File(testProjectDir.root, "class_references_after.json")
        def diffFile = new File(testProjectDir.root, "diff_classes.txt")
        def gson = new Gson()
        def type = new TypeToken<ArrayList<ClassNode>>() {}.type
        def beforeText = new String(Files.readAllBytes(previousClassReferencesFile.toPath()))
        def afterText = new String(Files.readAllBytes(currentClassReferencesFile.toPath()))
        def previousNodeList = gson.fromJson(beforeText, type)
        def currentNodeList = gson.fromJson(afterText, type)
        expect:
        ClassDiffHelper.INSTANCE.outputClassReferencesDiff(previousNodeList, currentNodeList, diffFile.readLines())
    }
}