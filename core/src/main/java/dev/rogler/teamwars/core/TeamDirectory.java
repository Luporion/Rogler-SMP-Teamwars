package dev.rogler.teamwars.core;

import java.util.*;

/** Stable UUID identities; name is presentation only. Never use team names as persistence keys. */
public final class TeamDirectory {
    private final Map<UUID, Team> teams = new HashMap<>();
    private final Map<UUID, UUID> ownerByPlayer = new HashMap<>();

    public record Team(UUID id, String name, UUID leader, Set<UUID> members) {
        public Team {
            Objects.requireNonNull(id);
            Objects.requireNonNull(leader);
            if (name == null || name.isBlank() || name.length() > 32)
                throw new IllegalArgumentException("Invalid team name");
            members = Set.copyOf(members);
            if (!members.contains(leader)) throw new IllegalArgumentException("Leader must be a member");
        }
    }
    public void create(UUID id, String name, UUID leader) {
        Objects.requireNonNull(id);
        if (teams.containsKey(id) || ownerByPlayer.containsKey(leader))
            throw new IllegalStateException("Duplicate team or player already assigned");
        Team team = new Team(id, name, leader, Set.of(leader));
        teams.put(id, team);
        ownerByPlayer.put(leader, id);
    }
    public void add(UUID teamId, UUID player) {
        if (ownerByPlayer.containsKey(player)) throw new IllegalStateException("Player already belongs to a team");
        Team existing = require(teamId);
        Set<UUID> members = new HashSet<>(existing.members());
        members.add(player);
        teams.put(teamId, new Team(existing.id(), existing.name(), existing.leader(), members));
        ownerByPlayer.put(player, teamId);
    }
    public void remove(UUID player) {
        UUID teamId = ownerByPlayer.get(player);
        if (teamId == null) return;
        Team existing = require(teamId);
        if (existing.leader().equals(player)) {
            throw new IllegalStateException("Transfer leadership before removing leader");
        }
        Set<UUID> members = new HashSet<>(existing.members());
        members.remove(player);
        teams.put(teamId, new Team(existing.id(), existing.name(), existing.leader(), members));
        ownerByPlayer.remove(player);
    }
    public void rename(UUID teamId, String name) {
        Team t = require(teamId);
        teams.put(teamId, new Team(t.id(), name, t.leader(), t.members()));
    }
    public Team require(UUID id) {
        Team t = teams.get(id);
        if (t == null) throw new IllegalArgumentException("Unknown team: " + id);
        return t;
    }
    public Optional<UUID> teamOf(UUID player) { return Optional.ofNullable(ownerByPlayer.get(player)); }
    public Set<UUID> members(UUID id) { return require(id).members(); }
    public Collection<Team> all() { return List.copyOf(teams.values()); }
}
