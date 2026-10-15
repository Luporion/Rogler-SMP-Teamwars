package dev.rogler.teamwars.neoforge.compat;

import dev.rogler.teamwars.core.compat.WariumRecoveryPolicy;
import dev.rogler.teamwars.neoforge.teams.RoglerTeamWarsLogger;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;

/** Optional admission guard, never keeps Entity/Level objects or performs any chunk/biome lookup.
 * Canceled join is a temporary load-time quarantine, NOT guaranteed persistent entity deletion.
 */
public final class WariumRecoveryGuard {
    private WariumRecoveryGuard() {}
    private static WariumRecoveryConfig config = WariumRecoveryConfig.SAFE_DEFAULT;
    private static MinecraftServer configuredServer;
    private static long seen, denied;
    private static final int HISTORY_LIMIT = 20;
    private static final Deque<String> last = new ArrayDeque<>();
    private static String detectedId = "none";

    public static void initialize(MinecraftServer server) {
        configuredServer = server;
        seen = denied = 0;
        last.clear();
        detectedId = "none";
        reload(server);
    }

    public static boolean reload(MinecraftServer server) {
        try {
            WariumRecoveryConfig next = WariumRecoveryConfig.load(server);
            config = next;
            RoglerTeamWarsLogger.info("Rogler Core Warium recovery=" + next.mode() + " target="
                + (next.exactEntityId().isEmpty() ? "verified class+namespace" : next.exactEntityId())
                + " installed=" + ModList.get().isLoaded("wariumce"));
            return true;
        } catch (IOException ex) {
            // Invalid configuration may never result in an unrequested deletion/quarantine.
            config = WariumRecoveryConfig.SAFE_DEFAULT;
            RoglerTeamWarsLogger.warn("Warium recovery config INVALID; forced OFF: " + ex.getMessage());
            return false;
        }
    }

    public static void shutdown() {
        configuredServer = null;
        config = WariumRecoveryConfig.SAFE_DEFAULT;
        last.clear();
        detectedId = "none";
    }

    public static void onJoin(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !ModList.get().isLoaded("wariumce")) return;
        // Startup config is normally loaded from ServerAboutToStartEvent before any entity chunks.
        // Never read files or request chunks here if a server is still initializing.
        if (configuredServer != level.getServer() || config.mode() == WariumRecoveryPolicy.Mode.OFF) return;
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(event.getEntity().getType());
        if (id == null) return;
        boolean target = WariumRecoveryPolicy.isFusionBlast(id.getNamespace(),
            event.getEntity().getClass().getSimpleName(),id.toString(),config.exactEntityId());
        if (!target) return;
        detectedId = id.toString();
        WariumRecoveryPolicy.Action action = WariumRecoveryPolicy.decide(config.mode(),true,event.loadedFromDisk());
        if (action == WariumRecoveryPolicy.Action.IGNORE) return;
        seen++;
        if (action == WariumRecoveryPolicy.Action.DENY_JOIN) {
            event.setCanceled(true);
            denied++;
        }
        var entity = event.getEntity();
        String entry="uuid="+entity.getUUID()+" type="+id+" dim="+level.dimension().location()
            +" pos="+entity.blockPosition()+" disk="+event.loadedFromDisk()+" action="+action;
        if (last.size() >= HISTORY_LIMIT) last.removeFirst();
        last.addLast(entry);
        if (seen <= 5 || seen % 100 == 0)
            RoglerTeamWarsLogger.warn("WARIUM_FUSIONBLAST " + entry + " | seen="+seen+" denied="+denied);
    }

    public static int status(CommandSourceStack source) {
        final String message="[Rogler Core] Warium: installed="+ModList.get().isLoaded("wariumce")
            +" recovery="+config.mode()+" exact_id="+(config.exactEntityId().isEmpty()?"auto class+namespace":config.exactEntityId())
            +" detected_id="+detectedId+" seen="+seen+" blocked="+denied
            +" | blocked joins may recur after restart; no .mca is altered";
        source.sendSuccess(()->Component.literal(message),false);
        return 1;
    }

    public static int list(CommandSourceStack source) {
        if (last.isEmpty()) source.sendSuccess(()->Component.literal("[Rogler Core] No FusionBlast joins recorded this session."),false);
        for (String line:last) source.sendSuccess(()->Component.literal("[Rogler Core] " + line),false);
        return 1;
    }

    /** List candidate Warium registry keys without spawning entities or loading chunks. */
    public static int candidates(CommandSourceStack source) {
        java.util.List<String> found = new java.util.ArrayList<>();
        for (ResourceLocation id : BuiltInRegistries.ENTITY_TYPE.keySet()) {
            if ("wariumce".equals(id.getNamespace()) &&
                (id.getPath().contains("fusion") || id.getPath().contains("blast")))
                found.add(id.toString());
        }
        found.sort(String::compareTo);
        source.sendSuccess(()->Component.literal("[Rogler Core] Warium registry candidates: " + found.size()
            + " (names alone DO NOT prove which one is FusionBlastEntityEntity)"),false);
        for (String id : found.stream().limit(30).toList())
            source.sendSuccess(()->Component.literal("[Rogler Core] " + id),false);
        return 1;
    }

    public static int report(CommandSourceStack source) {
        source.sendSuccess(()->Component.literal("[Rogler Core] Compatibility: Warium="+ModList.get().isLoaded("wariumce")
            +" (optional); mode="+config.mode()+"; no Sable/Mahou/Aether mixins; CoreGuard unchanged"),false);
        return status(source);
    }
}
