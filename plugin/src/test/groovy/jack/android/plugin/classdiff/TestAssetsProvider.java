package jack.android.plugin.classdiff;

import java.io.File;
import java.nio.file.Paths;

public class TestAssetsProvider {
    private final File functionalAssetsDir;

    public TestAssetsProvider(String assetsFolderName) {
        functionalAssetsDir = Paths.get("src", "test", "assets", assetsFolderName).toFile();
    }

    public TestAssetsProvider(String first, String... more) {
        functionalAssetsDir = Paths.get(first,more).toFile();
    }

    public File getAssetFile(String relativePath) {
        return new File(functionalAssetsDir, relativePath);
    }

    public File getFunctionalAssetsDir() {
        return functionalAssetsDir;
    }

    public File requireAssetFile(String relativePath) {
        File asset = new File(functionalAssetsDir, relativePath);
        if (!asset.exists()) {
            throw new IllegalArgumentException("Asset '$relativePath' not found in '$assetsFolderName'");
        }
        return asset;
    }
}