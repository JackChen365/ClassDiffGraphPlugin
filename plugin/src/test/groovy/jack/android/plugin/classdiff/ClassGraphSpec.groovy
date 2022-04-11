package jack.android.plugin.classdiff

import org.apache.commons.io.FileUtils
import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Shared
import spock.lang.Specification

class ClassGraphSpec extends Specification {
    private static final String OUTPUT_FILE_NAME = "class_references.json"
    private static final String REFERENCE_BEFORE_FILE_NAME = "class_references_before.json"
    private static final String REFERENCE_AFTER_FILE_NAME = "class_references_after.json"
    @Rule
    TemporaryFolder testProjectDir = new TemporaryFolder(new File("build/tmp"))
    @Shared
    private TestAssetsProvider diffInputAssetsProvider = new TestAssetsProvider("file")
    @Shared
    private def inputAssetsProvider1 = new TestAssetsProvider("app")
    @Shared
    private def inputAssetsProvider2 = new TestAssetsProvider("app2")

    def "test class graph app1"() {
        given:
        FileUtils.cleanDirectory(testProjectDir.root.parentFile)
        FileUtils.copyDirectory(diffInputAssetsProvider.functionalAssetsDir, testProjectDir.root)
        FileUtils.copyDirectory(inputAssetsProvider1.functionalAssetsDir, testProjectDir.root)
        def tmpLocalProperties = new File(testProjectDir.root, "local.properties")
        tmpLocalProperties.append("sdk.dir=" + getAndroidSdkDir())

        def buildScript = new File(testProjectDir.root, "build.gradle")
        buildScript.text = buildScript.text.replaceAll("classpath", "//classpath")

        def appBuildScript = new File(testProjectDir.root, "app/build.gradle")
        appBuildScript.text = appBuildScript.text.replace("//id 'plugin-placeholder'", "id '${ClassGraphConstants.PLUGIN_ID}'")

        def diffFile = new File(testProjectDir.root, "diff.txt")
        def outputFile = new File(testProjectDir.root, OUTPUT_FILE_NAME)
        def result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments(':app:transformClassesWithClassGraphBuildForDebug', "--stacktrace", "-Pdiff_file=${diffFile.absolutePath}")
                .withDebug(true)
                .forwardOutput()
                .withPluginClasspath()
                .build()
        def referenceFile = new File(diffInputAssetsProvider.getFunctionalAssetsDir(), REFERENCE_BEFORE_FILE_NAME)
        referenceFile.write(outputFile.text)
        expect:
        null != result && outputFile.exists()
    }

    def "test class graph app2"() {
        given:
        FileUtils.cleanDirectory(testProjectDir.root.parentFile)
        FileUtils.copyDirectory(diffInputAssetsProvider.functionalAssetsDir, testProjectDir.root)
        FileUtils.copyDirectory(inputAssetsProvider2.functionalAssetsDir, testProjectDir.root)
        def tmpLocalProperties = new File(testProjectDir.root, "local.properties")
        tmpLocalProperties.append("sdk.dir=" + getAndroidSdkDir())

        def buildScript = new File(testProjectDir.root, "build.gradle")
        buildScript.text = buildScript.text.replaceAll("classpath", "//classpath")

        def appBuildScript = new File(testProjectDir.root, "app/build.gradle")
        appBuildScript.text = appBuildScript.text.replace("//id 'plugin-placeholder'", "id '${ClassGraphConstants.PLUGIN_ID}'")

        def diffFile = new File(testProjectDir.root, "diff.txt")
        def outputFile = new File(testProjectDir.root, OUTPUT_FILE_NAME)
        def result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments(':app:transformClassesWithClassGraphBuildForDebug', "--stacktrace", "-Pdiff_file=${diffFile.absolutePath}")
                .withDebug(true)
                .forwardOutput()
                .withPluginClasspath()
                .build()
        def referenceFile = new File(diffInputAssetsProvider.getFunctionalAssetsDir(), REFERENCE_AFTER_FILE_NAME)
        referenceFile.write(outputFile.text)
        expect:
        null != result && outputFile.exists()
    }

    private def getAndroidSdkDir() {
        def localPropertiesFile = new File('../local.properties')
        if (localPropertiesFile.exists()) {
            Properties local = new Properties()
            local.load(new FileReader(localPropertiesFile))
            if (local.containsKey('sdk.dir')) {
                def property = local.getProperty("sdk.dir")
                if (null != property) {
                    File sdkDir = new File(property)
                    if (sdkDir.exists()) {
                        return property
                    }
                }
            }
        }
        return new NullPointerException("Can not found the initial android SDK configuration.")
    }
}
