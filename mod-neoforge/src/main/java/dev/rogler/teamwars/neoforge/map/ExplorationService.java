package dev.rogler.teamwars.neoforge.map;

import dev.rogler.teamwars.core.ChunkKey;
import dev.rogler.teamwars.core.ExplorationIndex;
import dev.rogler.teamwars.core.ExplorationStore;
import dev.rogler.teamwars.core.ExplorationTrail;
import dev.rogler.teamwars.core.TeamMapSession;
import dev.rogler.teamwars.neoforge.teams.FtbPartyReader;
import dev.rogler.teamwars.neoforge.teams.RoglerTeamWarsLogger;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Tracks personally visited chunk positions, never pregeneration. This is the server-side
 * provenance layer only: no arbitrary Xaero client tile injection, no disclosure to enemies.
 * On broken disk data, collection is suspended rather than overwriting evidence.
 */
public final class ExplorationService {
    private static final String FILE = "roglerteamwars_exploration.rtwe";
    private static ExplorationIndex index;
    private static final Map<UUID, TeamMapSession> sessions = new HashMap<>();
    private static final FtbPartyReader ftb = new FtbPartyReader();
    private static ExplorationTrail trail;
    private static Path path;
    private static boolean enabled;
    private static int pending;
    private static ExecutorService writer;
    private static Future<?> runningWrite;
    private ExplorationService() {}

    public static void start(MinecraftServer server) {
        path = server.getWorldPath(LevelResource.ROOT).resolve("data").resolve(FILE);
        enabled = false;
        try {
            sessions.clear();
            index = Files.exists(path) ? ExplorationStore.read(path) : new ExplorationIndex();
            trail = new ExplorationTrail(index, 8);
            pending = 0;
            writer = Executors.newSingleThreadExecutor(task -> {
                Thread t = new Thread(task, "rogler-map-snapshot-writer");
                t.setDaemon(true);
                return t;
            });
            runningWrite = null;
            enabled = true;
            RoglerTeamWarsLogger.info("MAP_PROVENANCE loaded; personal chunk records=" + total());
        } catch (Exception problem) {
            index = null; trail = null;
            RoglerTeamWarsLogger.warn("MAP_PROVENANCE DISABLED: snapshot cannot be read; preserved for recovery: " + problem.getMessage());
        }
    }

    public static void tick(MinecraftServer server) {
        if (!enabled || server.getTickCount() % 5 != 0) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ChunkKey position = ChunkKey.fromBlock(player.level().dimension().location().toString(), player.blockPosition().getX(), player.blockPosition().getZ());
            pending += trail.sample(player.getUUID(),position);
        }
        // Re-evaluate only on personal discovery changes or FTB membership changes.
        // This holds authorization state server-side but does NOT send Xaero textures.
        if (server.getTickCount() % 40 == 0) reconcileSessions(server);
        // Save at most once every 10 minutes, avoid writing every player movement.
        if (pending > 0 && server.getTickCount() % 12000 == 0) checkpoint();
    }

    private static void reconcileSessions(MinecraftServer server) {
        Set<UUID> online = new HashSet<>();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            UUID id = player.getUUID();
            online.add(id);
            var currentParty = ftb.partyFor(id);
            // A missing or stale FTB roster never grants another player's map.
            sessions.computeIfAbsent(id, TeamMapSession::new).reconcile(currentParty, index);
        }
        sessions.keySet().removeIf(id -> !online.contains(id));
    }

    private static int total() {
        if (index == null) return 0;
        return index.exportSnapshot().values().stream().mapToInt(Set::size).sum();
    }

    private static void checkpoint() {
        if (!enabled || pending == 0) return;
        // Serialize disk writes on one worker and avoid IO stalls on the server tick.
        // The only main-thread work is a detached, consistent snapshot of visited chunks.
        if (runningWrite != null && !runningWrite.isDone()) return;
        if (runningWrite != null) finishLastWrite();
        var snapshot = ExplorationIndex.fromSnapshot(index.exportSnapshot());
        final Path target = path;
        runningWrite = writer.submit(() -> {
            try {ExplorationStore.write(target, snapshot);}
            catch (Exception error) {
                RoglerTeamWarsLogger.warn("MAP_PROVENANCE checkpoint failed; existing file preserved: " + error.getMessage());
            }
        });
        pending = 0;
    }

    private static void finishLastWrite() {
        if (runningWrite == null) return;
        try {runningWrite.get();}
        catch (Exception e) {RoglerTeamWarsLogger.warn("MAP_PROVENANCE writer failed: " + e.getMessage());}
        finally {runningWrite = null;}
    }

    public static void logout(UUID player) {
        if (enabled) trail.logout(player);
        sessions.remove(player);
    }

    public static void stop() {
        if (enabled) {
            finishLastWrite();
            // Final disk write at normal server stop contains every sample, including last seconds.
            if (pending > 0) {
                try {ExplorationStore.write(path, index);}
                catch (Exception error) {
                    RoglerTeamWarsLogger.warn("MAP_PROVENANCE final save failed; file preserved: " + error.getMessage());
                }
            }
        }
        if (writer != null) writer.shutdown();
        sessions.clear();
        enabled = false; index = null; trail = null; path = null;
        writer = null; runningWrite = null; pending = 0;
    }

    public static int zone(CommandSourceStack source) {
        ServerPlayer player;
        try {player=source.getPlayerOrException();} catch (Exception e) {source.sendFailure(Component.literal("Player required"));return 0;}
        if (player.level() != source.getServer().overworld()) {
            source.sendSuccess(()->Component.literal("Rogler base zone only applies in the Overworld."), false);
            return 1;
        }
        var spawn = source.getServer().overworld().getSharedSpawnPos();
        var position = player.blockPosition();
        double distance = Math.hypot((double)position.getX()-spawn.getX(), (double)position.getZ()-spawn.getZ());
        var config = dev.rogler.teamwars.neoforge.RoglerTeamWarsMod.baseConfig();
        boolean inside = distance >= config.innerRadius() && distance <= config.outerRadius();
        source.sendSuccess(() -> Component.literal(String.format(java.util.Locale.ROOT,
            "Spawn distance: %.0f blocks; allowed base radius %d..%d; currently %s",
            distance, config.innerRadius(), config.outerRadius(), inside ? "INSIDE" : "OUTSIDE")), false);
        return 1;
    }

    /** Private per-viewer authorization snapshot; never broadcasts map lists or enemy locations. */
    public static int sessionStatus(CommandSourceStack source) {
        if (!enabled) {source.sendFailure(Component.literal("Exploration tracker unavailable"));return 0;}
        ServerPlayer player;
        try {player=source.getPlayerOrException();} catch (Exception e) {source.sendFailure(Component.literal("Player required"));return 0;}
        var session = sessions.computeIfAbsent(player.getUUID(), TeamMapSession::new);
        var change = session.reconcile(ftb.partyFor(player.getUUID()), index);
        source.sendSuccess(()->Component.literal("Private map authorization: " + session.visibleCount() + " chunks, "
            + session.members() + " current FTB members, generation " + session.generation() + "."),false);
        source.sendSuccess(()->Component.literal("Last calculated delta: +" + change.revealed().size()
            + " / -" + change.revoked().size() + "; roster changed: " + change.membershipChanged()),false);
        source.sendSuccess(()->Component.literal("Xaero terrain rendering and cache revocation are NOT implemented. "
            + "Never install global shared-map mods on a secret-team server."),false);
        return 1;
    }

    public static int status(CommandSourceStack source) {
        if (!enabled) {
            source.sendFailure(Component.literal("Exploration tracker disabled: inspect server logs and backup."));
            return 0;
        }
        ServerPlayer player;
        try {player=source.getPlayerOrException();}catch(Exception ex) {source.sendFailure(Component.literal("Player required")); return 0;}
        Set<UUID> members = new HashSet<>();
        var party = new FtbPartyReader().partyFor(player.getUUID());
        if (party != null && party.members().contains(player.getUUID())) members.addAll(party.members());
        else members.add(player.getUUID());
        int personal=index.personal(player.getUUID()).size();
        int merged=index.visibleTo(members).size();
        source.sendSuccess(()->Component.literal("Rogler map provenance: " + personal + " personally visited chunks; " + merged + " currently in team union (" + members.size() + " member(s))."),false);
        source.sendSuccess(()->Component.literal("Map rendering NOT yet synchronized to Xaero. This is safe server data collection only."),false);
        return 1;
    }
}
