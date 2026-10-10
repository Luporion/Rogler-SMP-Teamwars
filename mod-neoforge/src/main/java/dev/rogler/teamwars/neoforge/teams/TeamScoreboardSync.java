package dev.rogler.teamwars.neoforge.teams;

import dev.rogler.teamwars.core.teams.TeamAssignmentPolicy;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.Team;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** One-way FTB -> Vanilla scoreboard projection; never mutates FTB or other teams' data. */
public final class TeamScoreboardSync {
    private final FtbPartyReader ftb = new FtbPartyReader();
    private int partyCount;
    private int quarantinedCount;
    private int apiFailures;
    private String lastStatus = "Not synchronized";

    public void refresh(MinecraftServer server, TeamWarsSettings settings) {
        Scoreboard scoreboard = server.getScoreboard();
        Map<String, String> desired = new HashMap<>();
        Map<String, java.util.UUID> keyIdentity = new HashMap<>();
        Set<java.util.UUID> actualParties = new HashSet<>();
        quarantinedCount = 0;
        apiFailures = 0;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            var party = ftb.partyFor(player.getUUID());
            if (ftb.apiFailure()) apiFailures++;
            var assignment = TeamAssignmentPolicy.assign(player.getUUID(), party, settings.maxTeamMembers());
            if (assignment.validParty()) {
                actualParties.add(assignment.partyId());
                var existing = keyIdentity.putIfAbsent(assignment.scoreboardKey(), assignment.partyId());
                if (existing != null && !existing.equals(assignment.partyId())) {
                    // Even a cryptographic key collision must not leak player visibility.
                    assignment = TeamAssignmentPolicy.solo(player.getUUID(), "Team key collision");
                }
            }
            if (!assignment.validParty() && party != null) quarantinedCount++;
            String key = assignment.scoreboardKey();
            PlayerTeam team = scoreboard.getPlayerTeam(key);
            if (team == null) team = scoreboard.addPlayerTeam(key);
            Team.Visibility visibility = settings.nametagsForTeammatesOnly()
                ? Team.Visibility.HIDE_FOR_OTHER_TEAMS : Team.Visibility.ALWAYS;
            if (team.getNameTagVisibility() != visibility) team.setNameTagVisibility(visibility);
            if (team.isAllowFriendlyFire() != settings.friendlyFire())
                team.setAllowFriendlyFire(settings.friendlyFire());
            ChatFormatting selectedColor = assignment.validParty() ? color(assignment.partyId()) : ChatFormatting.GRAY;
            if (team.getColor() != selectedColor) team.setColor(selectedColor);
            Component label = Component.literal(assignment.displayName());
            if (!team.getDisplayName().equals(label)) team.setDisplayName(label);
            String playerName = player.getScoreboardName();
            desired.put(playerName, key);
            if (scoreboard.getPlayersTeam(playerName) != team)
                scoreboard.addPlayerToTeam(playerName, team);
        }
        // Remove stale players & orphan teams that belong exclusively to us.
        for (PlayerTeam existing : Set.copyOf(scoreboard.getPlayerTeams())) {
            if (!isOurs(existing.getName())) continue;
            for (String member : Set.copyOf(existing.getPlayers())) {
                if (!existing.getName().equals(desired.get(member)))
                    scoreboard.removePlayerFromTeam(member, existing);
            }
            if (existing.getPlayers().isEmpty()) scoreboard.removePlayerTeam(existing);
        }
        partyCount = actualParties.size();
        lastStatus = apiFailures > 0 ? "FTB ADAPTER ERROR: " + apiFailures + " reader failure(s)"
            : (quarantinedCount > 0 ? "QUARANTINED " + quarantinedCount + " player(s)" : "OK");
        if (actualParties.size() > settings.maxParties())
            lastStatus += " | WARNING: " + actualParties.size() + " online parties, max " + settings.maxParties();
    }

    private static boolean isOurs(String key) {
        return key.length() == 16 && (key.startsWith("rt") || key.startsWith("rs"))
            && key.substring(2).matches("[0-9a-f]{14}");
    }

    private static ChatFormatting color(java.util.UUID id) {
        ChatFormatting[] colors = {ChatFormatting.RED, ChatFormatting.BLUE, ChatFormatting.GREEN,
            ChatFormatting.YELLOW, ChatFormatting.LIGHT_PURPLE, ChatFormatting.AQUA};
        return colors[Math.floorMod(id.hashCode(), colors.length)];
    }

    public int partyCount() { return partyCount; }
    public int quarantinedCount() { return quarantinedCount; }
    public int apiFailures() { return apiFailures; }
    public String status() { return lastStatus; }
    public String ftbStatus() { return ftb.lastError(); }
    public TeamAssignmentPolicy.Assignment forPlayer(ServerPlayer player, TeamWarsSettings settings) {
        return TeamAssignmentPolicy.assign(player.getUUID(), ftb.partyFor(player.getUUID()), settings.maxTeamMembers());
    }
}
