package dev.rogler.teamwars.neoforge.teams;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Dependency-free settings smoke test compiled without NeoForge. */
public final class TeamWarsSettingsTests {
    private static int checks;
    private static void check(boolean ok, String msg) {
        checks++;
        if (!ok) throw new AssertionError(msg);
    }
    public static void main(String[] args) throws Exception {
        Path directory = Files.createTempDirectory("teamwars-v010-config");
        Path file = directory.resolve("serverconfig/roglerteamwars.properties");
        try {
            var loaded = TeamWarsSettings.load(file);
            check(Files.exists(file), "creates world-specific config");
            check(loaded.maxTeamMembers() == 4, "default max 4");
            check(loaded.refreshIntervalTicks() == 20, "default 1 second refresh");
            check(loaded.nametagsForTeammatesOnly(), "default names are private");
            check(!loaded.friendlyFire(), "default no friendly fire");
            Files.writeString(file, "max_team_members=8\nmax_parties=4\nrefresh_interval_ticks=40\nnametags_for_teammates_only=true\nfriendly_fire=true\n");
            var custom = TeamWarsSettings.load(file);
            check(custom.maxTeamMembers()==8 && custom.friendlyFire(), "valid override");
            Files.writeString(file, "max_team_members=-5\n");
            boolean refused = false;
            try { TeamWarsSettings.load(file); } catch(IOException expected) { refused = true; }
            check(refused,"reject illegal size");
            Files.writeString(file, "nametags_for_teammates_only=maybe\n");
            refused=false;
            try { TeamWarsSettings.load(file); } catch(IOException expected) { refused = true; }
            check(refused,"reject invalid boolean");
        } finally {
            Files.deleteIfExists(file);
            Files.deleteIfExists(file.getParent());
            Files.deleteIfExists(directory);
        }
        System.out.println("v0.1.0 TeamWarsSettingsTests: " + checks + " checks OK");
    }
}
