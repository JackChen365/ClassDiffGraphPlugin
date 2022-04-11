package jack.android.plugin.classdiff


import org.apache.commons.io.FileUtils
import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Shared
import spock.lang.Specification

class OutputDiffClassTaskTest extends Specification {
    @Rule
    TemporaryFolder testProjectDir = new TemporaryFolder(new File("build/tmp"))
    @Shared
    private def diffInputAssetsProvider = new TestAssetsProvider("src", "test", "assets", "file")
    @Shared
    private def inputAssetsProvider1 = new TestAssetsProvider("app")

    def "test output diff classes task"() {
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
        def diffClassesOutputFile = new File(testProjectDir.root, "diff_classes.txt")
        def result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments(ClassGraphConstants.TASK_OUTPUT_DIFF_CLASSES, "--stacktrace",
                        "-P${ClassGraphConstants.PROPERTY_DIFF_FILE}=${diffFile.absolutePath}",
                        "-P${ClassGraphConstants.PROPERTY_DIFF_OUTPUT_CLASS_FILE}=${diffClassesOutputFile.absolutePath}")
                .withDebug(true)
                .forwardOutput()
                .withPluginClasspath()
                .build()
        expect:
        null != result && diffClassesOutputFile.exists()
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