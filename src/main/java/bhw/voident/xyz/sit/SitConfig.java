package bhw.voident.xyz.sit;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.fabricmc.loader.api.FabricLoader;

public final class SitConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("sit.json");
    private static final int DEFAULT_BLOCK_REACH_DISTANCE = 4;
    private static SitConfig instance;

    public int blockReachDistance = DEFAULT_BLOCK_REACH_DISTANCE;

    private SitConfig() {
    }

    public static SitConfig get() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }

    public int getBlockReachDistance() {
        return Math.max(0, blockReachDistance);
    }

    private static SitConfig load() {
        if (Files.exists(CONFIG_PATH)) {
            try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                SitConfig config = GSON.fromJson(reader, SitConfig.class);
                if (config != null) {
                    return config;
                }
            } catch (IOException ignored) {
                // Fallback to defaults below.
            }
        }

        SitConfig config = new SitConfig();
        save(config);
        return config;
    }

    private static void save(SitConfig config) {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(config, writer);
            }
        } catch (IOException ignored) {
            // Ignore config write failures.
        }
    }
}
