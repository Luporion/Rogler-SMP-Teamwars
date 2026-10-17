package dev.rogler.teamwars.core.teams;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * A pure, fail-closed projection of an external FTB party into a vanilla scoreboard identity.
 * No authoritative player membership is created here; FTB Teams owns membership.
 */
public final class TeamAssignmentPolicy {
    private TeamAssignmentPolicy() {}

    public record Party(UUID id, String displayName, Set<UUID> members) {
        public Party {
            Objects.requireNonNull(id);
            Objects.requireNonNull(displayName);
            members = Set.copyOf(members);
        }
    }

    public record Assignment(UUID player, UUID partyId, String scoreboardKey, String displayName,
                             boolean validParty, String warning) {
        public Assignment {
            Objects.requireNonNull(player);
            Objects.requireNonNull(scoreboardKey);
            Objects.requireNonNull(displayName);
        }
    }

    /** Solo players get isolated scoreboard teams, so outsiders see no nametags. */
    public static Assignment assign(UUID player, Party party, int maximumMembers) {
        Objects.requireNonNull(player);
        if (maximumMembers < 1 || maximumMembers > 64)
            throw new IllegalArgumentException("maximumMembers must be 1..64");
        if (party == null) return solo(player, "FTB personal team or party unavailable");
        if (party.members().isEmpty() || !party.members().contains(player))
            return solo(player, "FTB returned a party without this member");
        // FTB Teams is authoritative for membership. The TeamWars size setting is
        // a policy warning, NOT permission to evict an existing real FTB party
        // member from their own Core, claims, storage or scoreboard group.
        // FTB's separate max_party_size setting must enforce join limits.
        String warning = party.members().size() > maximumMembers
            ? "FTB party has " + party.members().size() + " members; TeamWars configured limit is "
                + maximumMembers + ". Adjust FTB max_party_size if needed."
            : "";
        return new Assignment(player, party.id(), key("rt", party.id()),
            party.displayName().isBlank() ? "Team" : party.displayName(), true, warning);
    }

    public static Assignment solo(UUID player, String explanation) {
        Objects.requireNonNull(player);
        return new Assignment(player, null, key("rs", player), "Solo", false, explanation);
    }

    /** Scoreboard-safe <=16 characters, distinct namespaces for real parties and solo players. */
    public static String key(String prefix, UUID id) {
        if (!"rt".equals(prefix) && !"rs".equals(prefix)) throw new IllegalArgumentException("Bad prefix");
        Objects.requireNonNull(id);
        try {
            byte[] packed = ByteBuffer.allocate(16).putLong(id.getMostSignificantBits())
                .putLong(id.getLeastSignificantBits()).array();
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(packed);
            return prefix + HexFormat.of().formatHex(digest, 0, 7);
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 missing in Java runtime", impossible);
        }
    }
}
