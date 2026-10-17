package dev.rogler.teamwars.core;

import java.util.Objects;
import java.util.UUID;

/** Never publish positions to a client solely because it can see an entity nearby. */
public final class TeamMapPrivacyPolicy {
    private TeamMapPrivacyPolicy() {}

    /** Party IDs are authoritative and must be non-null and identical. */
    public static boolean sharePosition(UUID viewer, UUID subject, UUID viewerParty, UUID subjectParty) {
        Objects.requireNonNull(viewer);
        Objects.requireNonNull(subject);
        if (viewer.equals(subject)) return true;
        return viewerParty != null && viewerParty.equals(subjectParty);
    }

    /** A team core marker is only available to its FTB UUID owner. */
    public static boolean shareCore(UUID viewerParty, UUID coreParty) {
        return coreParty != null && coreParty.equals(viewerParty);
    }
}
