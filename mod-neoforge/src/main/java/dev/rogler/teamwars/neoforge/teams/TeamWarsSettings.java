package dev.rogler.teamwars.neoforge.teams;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Server-world-specific settings. FTB retains its independent max_party_size rule. */
public record TeamWarsSettings(int maxTeamMembers, int maxParties, int refreshIntervalTicks,
                               boolean nametagsForTeammatesOnly, boolean friendlyFire, boolean enforceTeamLimits) {
    public TeamWarsSettings(int maxTeamMembers, int maxParties, int refreshIntervalTicks,
                            boolean nametagsForTeammatesOnly, boolean friendlyFire) {
        this(maxTeamMembers, maxParties, refreshIntervalTicks, nametagsForTeammatesOnly, friendlyFire, false);
    }
    public static final TeamWarsSettings RECOMMENDED = new TeamWarsSettings(4,4,20,true,false);

    public TeamWarsSettings {
        if (maxTeamMembers < 1 || maxTeamMembers > 64) throw new IllegalArgumentException("max_team_members must be 1..64");
        if (maxParties < 2 || maxParties > 32) throw new IllegalArgumentException("max_parties must be 2..32");
        if (refreshIntervalTicks < 1 || refreshIntervalTicks > 1200)
            throw new IllegalArgumentException("refresh_interval_ticks must be 1..1200");
    }

    public static TeamWarsSettings load(Path file) throws IOException {
        if (Files.notExists(file)) {
            Files.createDirectories(file.getParent());
            Properties defaults = new Properties();
            defaults.setProperty("max_team_members", "4");
            defaults.setProperty("max_parties", "4");
            defaults.setProperty("refresh_interval_ticks", "20");
            defaults.setProperty("nametags_for_teammates_only", "true");
            defaults.setProperty("friendly_fire", "false");
            defaults.setProperty("enforce_team_limits", "false");
            try (OutputStream out = Files.newOutputStream(file)) {
                defaults.store(out, "Rogler Core. Enable enforce_team_limits to apply the Core party limits.");
            }
        }
        Properties properties = new Properties();
        try (InputStream in = Files.newInputStream(file)) { properties.load(in); }
        try {
            return new TeamWarsSettings(
                Integer.parseInt(properties.getProperty("max_team_members", "4")),
                Integer.parseInt(properties.getProperty("max_parties", "4")),
                Integer.parseInt(properties.getProperty("refresh_interval_ticks", "20")),
                parseBool(properties.getProperty("nametags_for_teammates_only", "true")),
                parseBool(properties.getProperty("friendly_fire", "false")),
                parseBool(properties.getProperty("enforce_team_limits", "false")));
        } catch (IllegalArgumentException ex) {
            throw new IOException("Invalid TeamWars config: " + ex.getMessage(), ex);
        }
    }

    private static boolean parseBool(String raw) {
        if (!raw.equalsIgnoreCase("true") && !raw.equalsIgnoreCase("false"))
            throw new IllegalArgumentException("Expected true/false, got " + raw);
        return Boolean.parseBoolean(raw);
    }
}
