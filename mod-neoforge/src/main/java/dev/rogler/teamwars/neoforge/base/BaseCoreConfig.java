package dev.rogler.teamwars.neoforge.base;

import dev.rogler.teamwars.core.BaseZone;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public record BaseCoreConfig(int innerRadius, int outerRadius, int carrySeconds) {
    public static final BaseCoreConfig DEFAULT = new BaseCoreConfig(300, 2000, 3600);
    public BaseCoreConfig {
        new BaseZone(0, 0, innerRadius, outerRadius);
        if (carrySeconds < 60 || carrySeconds > 86400) throw new IllegalArgumentException("carry_seconds must be 60..86400");
    }
    public BaseZone zone(MinecraftServer server) {
        var spawn = server.overworld().getSharedSpawnPos();
        return new BaseZone(spawn.getX(), spawn.getZ(), innerRadius, outerRadius);
    }
    public static BaseCoreConfig load(MinecraftServer server) throws IOException {
        Path path = server.getWorldPath(LevelResource.ROOT).resolve("serverconfig").resolve("roglerteamwars-base.properties");
        if (Files.notExists(path)) {
            Files.createDirectories(path.getParent());
            Properties defaults = new Properties();
            defaults.setProperty("min_radius", "300"); defaults.setProperty("max_radius", "2000");
            defaults.setProperty("carry_seconds", "3600");
            try (OutputStream output = Files.newOutputStream(path)) {
                defaults.store(output, "Rogler TeamWars Base Core v0.2.0; change radius only BEFORE first core is issued");
            }
        }
        Properties values = new Properties();
        try (InputStream input = Files.newInputStream(path)) { values.load(input); }
        try {
            return new BaseCoreConfig(Integer.parseInt(values.getProperty("min_radius", "300")),
                Integer.parseInt(values.getProperty("max_radius", "2000")),
                Integer.parseInt(values.getProperty("carry_seconds", "3600")));
        } catch (IllegalArgumentException ex) { throw new IOException("Invalid base config: " + ex.getMessage(), ex); }
    }
}
