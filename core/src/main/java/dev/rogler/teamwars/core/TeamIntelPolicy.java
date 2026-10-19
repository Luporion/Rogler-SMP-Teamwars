package dev.rogler.teamwars.core;

import dev.rogler.teamwars.core.teams.TeamAssignmentPolicy;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Pure privacy boundary for outgoing teammate coordinates.
 * Only FTB party members whose CURRENT, independently read effective-party ID
 * agrees with the viewer's are eligible. No FTB team => NO teammate coordinates.
 */
public final class TeamIntelPolicy {
    public static final int MAX_MARKERS = 32;
    private TeamIntelPolicy() {}

    public record Presence(UUID playerId, String name, String dimension, int x, int z) {
        public Presence {
            Objects.requireNonNull(playerId);
            Objects.requireNonNull(name);
            Objects.requireNonNull(dimension);
        }
    }

    public static List<Presence> authorized(UUID viewer, TeamAssignmentPolicy.Party party,
                                             Map<UUID, TeamAssignmentPolicy.Party> verified,
                                             List<Presence> online) {
        Objects.requireNonNull(viewer);
        Objects.requireNonNull(verified);
        Objects.requireNonNull(online);
        if (party == null || !party.members().contains(viewer) ||
            !sameParty(verified.get(viewer), party.id(), viewer)) return List.of();
        List<Presence> result = new ArrayList<>();
        for (Presence p : online) {
            if (p.playerId().equals(viewer) || !party.members().contains(p.playerId())) continue;
            if (!sameParty(verified.get(p.playerId()), party.id(), p.playerId())) continue;
            result.add(new Presence(p.playerId(), safeName(p.name()), safeDimension(p.dimension()), p.x(), p.z()));
        }
        result.sort(Comparator.comparing(p -> p.playerId().toString()));
        return List.copyOf(result.subList(0, Math.min(result.size(), MAX_MARKERS)));
    }

    private static boolean sameParty(TeamAssignmentPolicy.Party current, UUID expected, UUID player) {
        return current != null && current.id().equals(expected) && current.members().contains(player);
    }

    public static String safeName(String s) {
        String filtered = s.replaceAll("[\\p{Cntrl}\\u00a7]", "");
        return filtered.length() > 32 ? filtered.substring(0, 32) : filtered;
    }

    public static String safeDimension(String s) {
        return s.length() > 128 ? "unknown" : s;
    }
}
