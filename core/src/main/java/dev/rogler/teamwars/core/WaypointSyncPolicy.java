package dev.rogler.teamwars.core;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** Permission and merge rules independent of Minecraft, FTB and Xaero. */
public final class WaypointSyncPolicy {
    private WaypointSyncPolicy() {}
    public static final int MAX_TEAM_WAYPOINTS = 64;
    public static final int MAX_NAME = 48;
    public static final int MAX_DIMENSION = 128;

    public record Point(String dimension, int x, int y, int z, String name, int color) {
        public Point {
            Objects.requireNonNull(dimension); Objects.requireNonNull(name);
            if (dimension.isBlank() || dimension.length() > MAX_DIMENSION ||
                !dimension.matches("[a-z0-9_.-]+:[a-z0-9_./-]+"))
                throw new IllegalArgumentException("invalid dimension");
            if (name.isBlank() || name.length() > MAX_NAME || name.chars().anyMatch(c -> c < 32))
                throw new IllegalArgumentException("invalid name");
            if (Math.abs((long) x) > 30_000_000 || Math.abs((long) z) > 30_000_000 || y < -2048 || y > 2048)
                throw new IllegalArgumentException("invalid coordinates");
            if (color < 0 || color > 0xffffff) throw new IllegalArgumentException("invalid color");
        }
        public String locationKey() { return dimension + ":" + x + ":" + y + ":" + z; }
    }
    /** A shared record is immutable, tied to BOTH team and sharing player's UUID. */
    public record Shared(UUID id, UUID team, UUID owner, Point point) {
        public Shared {
            Objects.requireNonNull(id); Objects.requireNonNull(team);
            Objects.requireNonNull(owner); Objects.requireNonNull(point);
        }
    }
    public static boolean canSee(Shared entry, UUID viewer, UUID activeTeam, Set<UUID> currentMembers) {
        return activeTeam != null && activeTeam.equals(entry.team()) &&
            currentMembers.contains(viewer) && currentMembers.contains(entry.owner());
    }
    public static boolean canRevoke(Shared entry, UUID requester, UUID activeTeam, Set<UUID> currentMembers) {
        return entry.owner().equals(requester) && canSee(entry, requester, activeTeam, currentMembers);
    }
    public static List<Shared> visible(Collection<Shared> stored, UUID viewer, UUID activeTeam, Set<UUID> currentMembers) {
        return stored.stream().filter(p -> canSee(p, viewer, activeTeam, currentMembers))
            .sorted((a,b) -> a.id().compareTo(b.id())).limit(MAX_TEAM_WAYPOINTS).toList();
    }
    /** Merge for explicit LOCAL imports. Never infers a server share or deletes originals. */
    public static List<Point> missing(Collection<Point> source, Collection<Point> target) {
        Set<String> present = target.stream().map(Point::locationKey).collect(java.util.stream.Collectors.toSet());
        Map<String, Point> unique = new LinkedHashMap<>();
        for (Point point : source) if (!present.contains(point.locationKey()))
            unique.putIfAbsent(point.locationKey(), point);
        return unique.values().stream().limit(256).toList();
    }
}
