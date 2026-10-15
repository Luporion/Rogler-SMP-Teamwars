package dev.rogler.teamwars.neoforge.compat;

import dev.rogler.teamwars.core.compat.WariumRecoveryPolicy;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Per-world config loaded before chunk/entity startup; never alters old TeamWars configs. */
public record WariumRecoveryConfig(WariumRecoveryPolicy.Mode mode, String exactEntityId) {
    public static final WariumRecoveryConfig SAFE_DEFAULT = new WariumRecoveryConfig(WariumRecoveryPolicy.Mode.OFF, "");

    public static Path path(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT).resolve("serverconfig/roglercore-compat.properties");
    }

    public static WariumRecoveryConfig load(MinecraftServer server) throws IOException {
        Path path = path(server);
        if (Files.notExists(path)) {
            Files.createDirectories(path.getParent());
            Properties defaults = new Properties();
            defaults.setProperty("warium_fusionblast_recovery", "OFF");
            defaults.setProperty("warium_fusionblast_entity_id", "");
            try (OutputStream output = Files.newOutputStream(path)) {
                defaults.store(output, "Rogler Core: OFF by default. See docs/releases/v0.6.1.md before recovery.");
            }
        }
        Properties props = new Properties();
        try (InputStream input = Files.newInputStream(path)) { props.load(input); }
        final WariumRecoveryPolicy.Mode mode;
        try { mode = WariumRecoveryPolicy.parseMode(props.getProperty("warium_fusionblast_recovery", "OFF")); }
        catch (IllegalArgumentException e) { throw new IOException(e.getMessage(), e); }
        String id = props.getProperty("warium_fusionblast_entity_id", "").strip().toLowerCase(java.util.Locale.ROOT);
        if (!id.isEmpty() && !id.matches("wariumce:[a-z0-9_./-]+"))
            throw new IOException("warium_fusionblast_entity_id must be empty or an exact wariumce:<entity> ID");
        return new WariumRecoveryConfig(mode,id);
    }
}
