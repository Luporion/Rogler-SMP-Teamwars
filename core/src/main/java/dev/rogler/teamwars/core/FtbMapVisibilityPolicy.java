package dev.rogler.teamwars.core;

import dev.rogler.teamwars.core.teams.TeamAssignmentPolicy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Strict FTB Chunks map visibility, including OP players. No public/ally/OP bypass.
 * A solo player can only see their own position. A verified party member may
 * additionally see independently verified members of the same effective FTB party.
 */
public final class FtbMapVisibilityPolicy {
    private FtbMapVisibilityPolicy() {}

    public static List<UUID> visible(UUID viewer, List<UUID> candidates,
                                     Map<UUID, TeamAssignmentPolicy.Party> parties) {
        if (viewer == null || candidates == null || parties == null) return List.of();
        var own = parties.get(viewer);
        boolean valid = own != null && own.members().contains(viewer);
        List<UUID> result = new ArrayList<>();
        for (UUID candidate : candidates) {
            if (candidate == null || result.contains(candidate)) continue;
            if (viewer.equals(candidate)) { result.add(candidate); continue; }
            var other = parties.get(candidate);
            if (valid && other != null && other.id().equals(own.id())
                    && other.members().contains(candidate) && own.members().contains(candidate))
                result.add(candidate);
        }
        return List.copyOf(result);
    }
}
