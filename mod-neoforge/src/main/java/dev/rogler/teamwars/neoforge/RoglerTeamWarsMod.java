package dev.rogler.teamwars.neoforge;

import com.mojang.brigadier.Command;
import net.minecraft.commands.arguments.EntityArgument;
import dev.rogler.teamwars.neoforge.base.BaseCoreConfig;
import dev.rogler.teamwars.neoforge.base.BaseCoreService;
import dev.rogler.teamwars.neoforge.base.BaseCoreRegistries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.level.BlockEvent;
import dev.rogler.teamwars.core.teams.TeamAssignmentPolicy;
import dev.rogler.teamwars.neoforge.teams.TeamScoreboardSync;
import dev.rogler.teamwars.neoforge.teams.TeamWarsSettings;
import dev.rogler.teamwars.neoforge.teams.RoglerTeamWarsLogger;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.io.IOException;
import java.nio.file.Path;

/**
 * v0.2.0: FTB-owned party membership, scoreboard privacy and first server-authoritative Base Core.
 * Claims, raids, Xaero map and ship support are intentionally deferred.
 */
@Mod(RoglerTeamWarsMod.MOD_ID)
public final class RoglerTeamWarsMod {
    public static final String MOD_ID = "roglerteamwars";
    public static final String VERSION = "0.2.0";
    private static RoglerTeamWarsMod INSTANCE;
    private BaseCoreConfig baseConfig = BaseCoreConfig.DEFAULT;

    public static BaseCoreConfig baseConfig() { return INSTANCE.baseConfig; }
    public static TeamAssignmentPolicy.Assignment assignmentFor(ServerPlayer player) {
        return INSTANCE.scoreboard.forPlayer(player, INSTANCE.settings);
    }
    public static void warn(String message) { RoglerTeamWarsLogger.warn(message); }

    private MinecraftServer activeServer;
    private TeamWarsSettings settings = TeamWarsSettings.RECOMMENDED;
    private final TeamScoreboardSync scoreboard = new TeamScoreboardSync();
    private String configResult = "Not loaded";

    public RoglerTeamWarsMod(IEventBus modBus) {
        INSTANCE = this;
        BaseCoreRegistries.register(modBus);
        NeoForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void started(ServerStartedEvent event) {
        activeServer = event.getServer();
        reloadSettings(activeServer);
        try { baseConfig = BaseCoreConfig.load(activeServer); }
        catch (IOException e) {
            baseConfig = BaseCoreConfig.DEFAULT;
            RoglerTeamWarsLogger.warn("Invalid base-core config; safe defaults: " + e.getMessage());
        }
        scoreboard.refresh(activeServer, settings);
        RoglerTeamWarsLogger.info("v" + VERSION + " loaded. FTB Teams is authoritative. "
            + "Team name tags: " + (settings.nametagsForTeammatesOnly() ? "teammates only" : "vanilla")
            + ". Base Core issue/place enabled; FTB claim/raid/map sync not enabled.");
    }

    @SubscribeEvent
    public void stopping(ServerStoppingEvent event) {
        activeServer = null;
    }

    @SubscribeEvent
    public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (activeServer != null && event.getEntity() instanceof ServerPlayer player) {
            scoreboard.refresh(player.getServer(), settings);
        }
    }

    @SubscribeEvent
    public void tick(ServerTickEvent.Post event) {
        if (activeServer == null) return;
        if (activeServer.getTickCount() % settings.refreshIntervalTicks() == 0)
            scoreboard.refresh(activeServer, settings);
        if (activeServer.getTickCount() % 20 == 0)
            BaseCoreService.oneSecond(activeServer);
    }

    private Path configPath(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT).resolve("serverconfig").resolve("roglerteamwars.properties");
    }

    private void reloadSettings(MinecraftServer server) {
        try {
            settings = TeamWarsSettings.load(configPath(server));
            configResult = "OK: " + configPath(server);
        } catch (IOException ex) {
            // Do not crash production worlds on a corrupt config; preserve safe privacy defaults.
            settings = TeamWarsSettings.RECOMMENDED;
            configResult = "INVALID: " + ex.getMessage() + " (safe defaults in use)";
            RoglerTeamWarsLogger.warn(configResult);
        }
    }

    @SubscribeEvent
    public void onCoreBreak(BlockEvent.BreakEvent event) {
        if (event.getState().is(BaseCoreRegistries.CORE_BLOCK.get())) {
            event.setCanceled(true); // Prevent survival/creative bypass; use future admin recovery flow.
        }
    }

    @SubscribeEvent
    public void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("teamwars")
            .executes(ctx -> status(ctx.getSource()))
            .then(Commands.literal("status").executes(ctx -> status(ctx.getSource())))
            .then(Commands.literal("team")
                .executes(ctx -> ownTeam(ctx.getSource()))
                .then(Commands.literal("info").executes(ctx -> ownTeam(ctx.getSource())))
                .then(Commands.literal("help").executes(ctx -> teamHelp(ctx.getSource()))))
            .then(Commands.literal("base")
                .then(Commands.literal("status").executes(ctx -> BaseCoreService.ownStatus(ctx.getSource())))
                .then(Commands.literal("distance").executes(ctx -> BaseCoreService.distance(ctx.getSource()))))
            .then(Commands.literal("admin").requires(source -> source.hasPermission(2))
                .then(Commands.literal("corelist").executes(ctx -> BaseCoreService.info(ctx.getSource())))
                .then(Commands.literal("issuecore")
                    .then(Commands.argument("player", EntityArgument.player())
                        .executes(ctx -> BaseCoreService.issue(ctx.getSource(),
                            EntityArgument.getPlayer(ctx, "player"), false))))
                .then(Commands.literal("recovercore")
                    .then(Commands.argument("player", EntityArgument.player())
                        .executes(ctx -> BaseCoreService.issue(ctx.getSource(),
                            EntityArgument.getPlayer(ctx, "player"), true))))
                .then(Commands.literal("reload").executes(ctx -> {
                    MinecraftServer server = ctx.getSource().getServer();
                    reloadSettings(server);
                    scoreboard.refresh(server, settings);
                    ctx.getSource().sendSuccess(() -> Component.literal("TeamWars config: " + configResult), false);
                    return Command.SINGLE_SUCCESS;
                }))
                .then(Commands.literal("resync").executes(ctx -> {
                    scoreboard.refresh(ctx.getSource().getServer(), settings);
                    ctx.getSource().sendSuccess(() -> Component.literal("TeamWars FTB -> scoreboard synchronized: " + scoreboard.status()), false);
                    return Command.SINGLE_SUCCESS;
                }))
                .then(Commands.literal("diagnostics").executes(ctx -> {
                    ctx.getSource().sendSuccess(() -> Component.literal("FTB adapter: " + scoreboard.ftbStatus()
                        + " | Config: " + configResult + " | Online party count: " + scoreboard.partyCount()), false);
                    return Command.SINGLE_SUCCESS;
                }))));
    }

    private int status(net.minecraft.commands.CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("Rogler TeamWars v" + VERSION
            + " | FTB party projection: " + scoreboard.status()
            + " | online parties: " + scoreboard.partyCount()
            + " | quarantined: " + scoreboard.quarantinedCount()
            + " | FTB API errors: " + scoreboard.apiFailures()), false);
        source.sendSuccess(() -> Component.literal("Features: FTB team sync, private nametags, /teamwars team; "
            + "base core issue/place enabled; NO claim/raid/Xaero share yet."), false);
        return Command.SINGLE_SUCCESS;
    }

    private int ownTeam(net.minecraft.commands.CommandSourceStack source)
        throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        TeamAssignmentPolicy.Assignment group = scoreboard.forPlayer(player, settings);
        if (!group.validParty()) {
            source.sendSuccess(() -> Component.literal("No valid FTB party; you are isolated for nametag privacy. "
                + group.warning() + ". Use /ftbteams party commands or its GUI."), false);
        } else {
            source.sendSuccess(() -> Component.literal("FTB Team: " + group.displayName()
                + " (" + group.partyId() + ")"), false);
        }
        return Command.SINGLE_SUCCESS;
    }

    private int teamHelp(net.minecraft.commands.CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("Manage memberships/invites using FTB Teams GUI or /ftbteams party. "
            + "TeamWars v0.1 only mirrors FTB and applies nametag rules."), false);
        return Command.SINGLE_SUCCESS;
    }
}
