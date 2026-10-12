package dev.rogler.teamwars.neoforge.protection;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import java.io.*;
import java.nio.file.*;
import java.util.Properties;

/** World-specific, conservative defaults. Destructive raids intentionally unsupported. */
public record BaseProtectionConfig(int claimRadiusChunks, int newTeamGraceHours,
                                   boolean autoFtbClaims, boolean baseGuard, boolean raidsEnabled) {
    public static final BaseProtectionConfig DEFAULT = new BaseProtectionConfig(1, 24, false, true, false);
    public BaseProtectionConfig {
        if (claimRadiusChunks < 0 || claimRadiusChunks > 3) throw new IllegalArgumentException("claim_radius_chunks 0..3");
        if (newTeamGraceHours < 0 || newTeamGraceHours > 168) throw new IllegalArgumentException("new_team_grace_hours 0..168");
        if (raidsEnabled) throw new IllegalArgumentException("raids_enabled unsupported in v0.3.0 (must be false)");
    }
    public static BaseProtectionConfig load(MinecraftServer server) throws IOException {
        Path file = server.getWorldPath(LevelResource.ROOT).resolve("serverconfig/roglerteamwars-protection.properties");
        if (Files.notExists(file)) {
            Files.createDirectories(file.getParent());
            Properties defaults = new Properties();
            defaults.setProperty("claim_radius_chunks", "1");
            defaults.setProperty("new_team_grace_hours", "24");
            defaults.setProperty("auto_ftb_claims", "false");
            defaults.setProperty("base_guard_enabled", "true");
            defaults.setProperty("raids_enabled", "false");
            try (OutputStream out = Files.newOutputStream(file)) {
                defaults.store(out, "TeamWars v0.3.0: 3x3 core zone, no destructive raiding");
            }
        }
        Properties p = new Properties();
        try (InputStream in = Files.newInputStream(file)) { p.load(in); }
        try {
            return new BaseProtectionConfig(Integer.parseInt(p.getProperty("claim_radius_chunks", "1")),
                Integer.parseInt(p.getProperty("new_team_grace_hours", "24")),
                bool(p,"auto_ftb_claims",false), bool(p,"base_guard_enabled",true),
                bool(p,"raids_enabled",false));
        } catch (IllegalArgumentException ex) { throw new IOException("Invalid protection config: " + ex.getMessage(), ex); }
    }
    private static boolean bool(Properties p,String name,boolean fallback) {
        String raw=p.getProperty(name,String.valueOf(fallback));
        if (!raw.equalsIgnoreCase("true") && !raw.equalsIgnoreCase("false"))
            throw new IllegalArgumentException(name+" must be true/false");
        return Boolean.parseBoolean(raw);
    }
}
