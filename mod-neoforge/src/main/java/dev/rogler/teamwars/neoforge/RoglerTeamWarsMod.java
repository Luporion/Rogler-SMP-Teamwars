package dev.rogler.teamwars.neoforge;

import com.mojang.brigadier.Command;
import net.minecraft.commands.arguments.EntityArgument;
import dev.rogler.teamwars.neoforge.base.BaseCoreConfig;
import dev.rogler.teamwars.neoforge.base.BaseCoreService;
import dev.rogler.teamwars.neoforge.base.BaseCoreRegistries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.neoforged.neoforge.event.entity.item.ItemExpireEvent;
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
 * v0.5.1: stable teams/claims, core-only raids, persistent core inventory and bounded FE.
 * Destructive general raiding, Xaero and Sable integrations are not enabled.
 */
@Mod(RoglerTeamWarsMod.MOD_ID)
public final class RoglerTeamWarsMod {
    public static final String MOD_ID = "roglerteamwars";
    public static final String VERSION = "0.5.2";
    private static RoglerTeamWarsMod INSTANCE;
    private BaseCoreConfig baseConfig = BaseCoreConfig.DEFAULT;
    private dev.rogler.teamwars.neoforge.base.CorePowerConfig powerConfig = dev.rogler.teamwars.neoforge.base.CorePowerConfig.DEFAULT;
    private dev.rogler.teamwars.neoforge.protection.BaseProtectionConfig protectionConfig =
        dev.rogler.teamwars.neoforge.protection.BaseProtectionConfig.DEFAULT;

    public static BaseCoreConfig baseConfig() { return INSTANCE.baseConfig; }
    public static dev.rogler.teamwars.neoforge.base.CorePowerConfig powerConfig() {return INSTANCE.powerConfig;}
    public static dev.rogler.teamwars.neoforge.protection.BaseProtectionConfig protectionConfig() {
        return INSTANCE.protectionConfig;
    }
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
        modBus.addListener((net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent e) ->
            e.enqueueWork(BaseCoreRegistries::registerCreateStress));
        modBus.addListener(dev.rogler.teamwars.neoforge.base.CoreInfrastructureService::registerCapabilities);
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
        try { protectionConfig = dev.rogler.teamwars.neoforge.protection.BaseProtectionConfig.load(activeServer); }
        catch (IOException e) { protectionConfig = dev.rogler.teamwars.neoforge.protection.BaseProtectionConfig.DEFAULT;
            RoglerTeamWarsLogger.warn("Protection config invalid, safe defaults: " + e.getMessage()); }
        try { powerConfig = dev.rogler.teamwars.neoforge.base.CorePowerConfig.load(activeServer); }
        catch(IOException e) {powerConfig=dev.rogler.teamwars.neoforge.base.CorePowerConfig.DEFAULT;
            RoglerTeamWarsLogger.warn("Invalid FE configuration; safe defaults: "+e.getMessage());}
        scoreboard.refresh(activeServer, settings);
        dev.rogler.teamwars.neoforge.protection.BaseGuardService.reconcile(activeServer);
        RoglerTeamWarsLogger.info("v" + VERSION + " loaded. FTB Teams is authoritative. "
            + "Team name tags: " + (settings.nametagsForTeammatesOnly() ? "teammates only" : "vanilla")
            + ". Base Core, private FTB auto-claims and Core Guard enabled; core-only raids opt-in.");
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
        dev.rogler.teamwars.neoforge.base.CoreInfrastructureService.tick(activeServer);
        if (activeServer.getTickCount() % settings.refreshIntervalTicks() == 0)
            scoreboard.refresh(activeServer, settings);
        if (activeServer.getTickCount() % 20 == 0)
            BaseCoreService.oneSecond(activeServer);
        if (activeServer.getTickCount() % 20 == 0) {
            BaseCoreService.cleanupCreativeGhosts(activeServer);
            dev.rogler.teamwars.neoforge.raid.RaidService.tick(activeServer);
        }
        if (activeServer.getTickCount() % 1200 == 0)
            dev.rogler.teamwars.neoforge.protection.BaseGuardService.reconcile(activeServer);
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
    public void onCoreExpire(ItemExpireEvent event) {
        if (event.getEntity().getItem().is(BaseCoreRegistries.CORE_ITEM.get()))
            BaseCoreService.monitorDroppedCore(event.getEntity(), true);
    }

    /** Sneak-right-click on the registered block (server authoritative). */
    @SubscribeEvent
    public void onCorePickup(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND
            || !event.getLevel().getBlockState(event.getPos()).is(BaseCoreRegistries.CORE_BLOCK.get())) return;
        // Sneak + held item must reach normal item use, e.g. Crafts & Additions
        // connectors, Create wrench/shaft and block placements on the core face.
        if(event.getEntity().isShiftKeyDown()
            && !event.getEntity().getItemInHand(InteractionHand.MAIN_HAND).isEmpty()) return;
        if (event.getEntity() instanceof ServerPlayer player) {
            if (player.isShiftKeyDown()) BaseCoreService.pack(player, event.getPos());
            else if (player.getItemInHand(InteractionHand.MAIN_HAND).is(net.minecraft.world.item.Items.BOOK))
                dev.rogler.teamwars.neoforge.base.CoreGuideService.help(player);
            else dev.rogler.teamwars.neoforge.base.CoreInfrastructureService.open(player, event.getPos());
        }
        // Also consume client use action, preventing held items from interfering.
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide));
    }

    @SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.HIGHEST)
    public void onProtectedLeftClick(PlayerInteractEvent.LeftClickBlock event) {
        dev.rogler.teamwars.neoforge.protection.BaseGuardService.onLeftClick(event);
    }

    @SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.HIGHEST)
    public void onProtectedBreakSpeed(PlayerEvent.BreakSpeed event) {
        dev.rogler.teamwars.neoforge.protection.BaseGuardService.onBreakSpeed(event);
    }

    @SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.HIGHEST)
    public void onCoreBreak(BlockEvent.BreakEvent event) {
        if (event.getState().is(BaseCoreRegistries.CORE_BLOCK.get())) {
            event.setCanceled(true); // Only authorized core pack changes the core lifecycle.
        } else {
            dev.rogler.teamwars.neoforge.protection.BaseGuardService.onBreak(event);
        }
    }

    @SubscribeEvent
    public void onProtectedPlace(BlockEvent.EntityPlaceEvent event) {
        dev.rogler.teamwars.neoforge.protection.BaseGuardService.onPlace(event);
    }

    @SubscribeEvent
    public void onProtectedInteract(PlayerInteractEvent.RightClickBlock event) {
        dev.rogler.teamwars.neoforge.protection.BaseGuardService.onInteract(event);
    }

    @SubscribeEvent
    public void onProtectedExplosion(net.neoforged.neoforge.event.level.ExplosionEvent.Detonate event) {
        dev.rogler.teamwars.neoforge.protection.BaseGuardService.onExplosion(event);
    }

    @SubscribeEvent
    public void onProtectedDamage(net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent event) {
        dev.rogler.teamwars.neoforge.protection.BaseGuardService.onDamage(event);
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
            .then(Commands.literal("war")
                .then(Commands.literal("status").executes(ctx -> dev.rogler.teamwars.neoforge.raid.RaidService.status(ctx.getSource())))
                .then(Commands.literal("scores").executes(ctx -> dev.rogler.teamwars.neoforge.raid.RaidService.scores(ctx.getSource()))))
            .then(Commands.literal("guide")
                .executes(ctx -> dev.rogler.teamwars.neoforge.base.CoreGuideService.help(ctx.getSource()))
                .then(Commands.literal("books")
                    .executes(ctx -> dev.rogler.teamwars.neoforge.base.CoreGuideService.books(ctx.getSource())))
                .then(Commands.literal("give")
                    .then(Commands.argument("modid", com.mojang.brigadier.arguments.StringArgumentType.word())
                        .executes(ctx -> dev.rogler.teamwars.neoforge.base.CoreGuideService.give(ctx.getSource(),
                            com.mojang.brigadier.arguments.StringArgumentType.getString(ctx,"modid"))))))
            .then(Commands.literal("base")
                .then(Commands.literal("status").executes(ctx -> BaseCoreService.ownStatus(ctx.getSource())))
                .then(Commands.literal("distance").executes(ctx -> BaseCoreService.distance(ctx.getSource())))
                .then(Commands.literal("pack").executes(ctx -> BaseCoreService.packCommand(ctx.getSource())))
                .then(Commands.literal("upgrade")
                    .then(Commands.literal("status").executes(ctx ->
                        dev.rogler.teamwars.neoforge.base.CoreUpgradeService.status(ctx.getSource())))
                    .then(Commands.literal("armor").executes(ctx ->
                        dev.rogler.teamwars.neoforge.base.CoreUpgradeService.buyArmor(ctx.getSource())))
                    .then(Commands.literal("energy").executes(ctx ->
                        dev.rogler.teamwars.neoforge.base.CoreUpgradeService.buyEnergy(ctx.getSource())))
                    .then(Commands.literal("create").executes(ctx ->
                        dev.rogler.teamwars.neoforge.base.CoreUpgradeService.buyCreate(ctx.getSource()))))
                .then(Commands.literal("energy").executes(ctx ->
                    dev.rogler.teamwars.neoforge.base.CoreInfrastructureService.status(ctx.getSource())))
                .then(Commands.literal("protection").executes(ctx ->
                    dev.rogler.teamwars.neoforge.protection.BaseGuardService.ownStatus(ctx.getSource())))
                .then(Commands.literal("recover").executes(ctx -> BaseCoreService.issue(
                    ctx.getSource(), ctx.getSource().getPlayerOrException(), true))))
            .then(Commands.literal("admin").requires(source -> source.hasPermission(2))
                .then(Commands.literal("corelist").executes(ctx -> BaseCoreService.info(ctx.getSource())))
                .then(Commands.literal("teams").executes(ctx ->
                    dev.rogler.teamwars.neoforge.protection.BaseGuardService.teams(ctx.getSource())))
                .then(Commands.literal("claimaudit")
                    .then(Commands.argument("team", com.mojang.brigadier.arguments.StringArgumentType.word())
                        .executes(ctx -> dev.rogler.teamwars.neoforge.protection.BaseGuardService.auditClaims(
                            ctx.getSource(), parseTeamId(ctx.getSource(),
                                com.mojang.brigadier.arguments.StringArgumentType.getString(ctx,"team"))))))
                .then(Commands.literal("reclaimlostteam")
                    .then(Commands.argument("team", com.mojang.brigadier.arguments.StringArgumentType.word())
                        .executes(ctx -> BaseCoreService.reclaimLostTeam(ctx.getSource(),
                            parseTeamId(ctx.getSource(),com.mojang.brigadier.arguments.StringArgumentType.getString(ctx,"team"))))))
                .then(Commands.literal("graceteam")
                    .then(Commands.argument("team", com.mojang.brigadier.arguments.StringArgumentType.word())
                        .then(Commands.argument("minutes", com.mojang.brigadier.arguments.IntegerArgumentType.integer(0,10080))
                            .executes(ctx -> dev.rogler.teamwars.neoforge.protection.BaseGuardService.setGraceTeam(
                                ctx.getSource(),parseTeamId(ctx.getSource(),
                                    com.mojang.brigadier.arguments.StringArgumentType.getString(ctx,"team")),
                                com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx,"minutes"))))))
                .then(Commands.literal("raidstartteam")
                    .then(Commands.argument("team", com.mojang.brigadier.arguments.StringArgumentType.word())
                        .then(Commands.argument("minutes", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1,30))
                            .executes(ctx -> dev.rogler.teamwars.neoforge.raid.RaidService.openTeam(
                                ctx.getSource(),parseTeamId(ctx.getSource(),
                                    com.mojang.brigadier.arguments.StringArgumentType.getString(ctx,"team")),
                                com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx,"minutes"))))))
                .then(Commands.literal("raidstopteam")
                    .then(Commands.argument("team", com.mojang.brigadier.arguments.StringArgumentType.word())
                        .executes(ctx -> dev.rogler.teamwars.neoforge.raid.RaidService.stopTeam(
                            ctx.getSource(),parseTeamId(ctx.getSource(),
                                com.mojang.brigadier.arguments.StringArgumentType.getString(ctx,"team"))))))
                .then(Commands.literal("raidstart")
                    .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("minutes", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1,30))
                            .executes(ctx -> dev.rogler.teamwars.neoforge.raid.RaidService.open(ctx.getSource(),
                                EntityArgument.getPlayer(ctx,"player"),
                                com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx,"minutes"))))))
                .then(Commands.literal("raidstop")
                    .then(Commands.argument("player", EntityArgument.player())
                        .executes(ctx -> dev.rogler.teamwars.neoforge.raid.RaidService.stop(ctx.getSource(),
                            EntityArgument.getPlayer(ctx,"player")))))
                .then(Commands.literal("orphanlist").executes(ctx ->
                    dev.rogler.teamwars.neoforge.protection.BaseGuardService.orphanList(ctx.getSource())))
                .then(Commands.literal("adoptcore")
                    .then(Commands.argument("oldTeam", com.mojang.brigadier.arguments.StringArgumentType.word())
                        .then(Commands.argument("player", EntityArgument.player())
                            .executes(ctx -> {
                                String text = com.mojang.brigadier.arguments.StringArgumentType.getString(ctx,"oldTeam");
                                try {
                                    java.util.UUID old = java.util.UUID.fromString(text);
                                    return dev.rogler.teamwars.neoforge.protection.BaseGuardService.adoptOrphan(
                                        ctx.getSource(), old, EntityArgument.getPlayer(ctx,"player"));
                                } catch (IllegalArgumentException badUUID) {
                                    ctx.getSource().sendFailure(Component.literal("[TeamWars] Invalid UUID: " + text));
                                    return 0;
                                }
                            }))))
                .then(Commands.literal("syncclaims")
                    .then(Commands.argument("player", EntityArgument.player())
                        .executes(ctx -> dev.rogler.teamwars.neoforge.protection.BaseGuardService.claimSync(
                            ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))))
                .then(Commands.literal("grace")
                    .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("minutes", com.mojang.brigadier.arguments.IntegerArgumentType.integer(0, 10080))
                            .executes(ctx -> dev.rogler.teamwars.neoforge.protection.BaseGuardService.setGrace(
                                ctx.getSource(), EntityArgument.getPlayer(ctx, "player"),
                                com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "minutes"))))))
                .then(Commands.literal("salvagemissing")
                    .then(Commands.argument("player", EntityArgument.player())
                        .executes(ctx -> BaseCoreService.salvageMissing(ctx.getSource(),
                            EntityArgument.getPlayer(ctx, "player")))))
                .then(Commands.literal("reclaimlost")
                    .then(Commands.argument("player", EntityArgument.player())
                        .executes(ctx -> BaseCoreService.reclaimLost(ctx.getSource(),
                            EntityArgument.getPlayer(ctx, "player")))))
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
                    try { protectionConfig = dev.rogler.teamwars.neoforge.protection.BaseProtectionConfig.load(server); }
                    catch (IOException e) { RoglerTeamWarsLogger.warn("Protection reload failed: " + e.getMessage()); }
                    try { powerConfig=dev.rogler.teamwars.neoforge.base.CorePowerConfig.load(server); }
                    catch(IOException e) { RoglerTeamWarsLogger.warn("FE reload refused: "+e.getMessage()); }
                    scoreboard.refresh(server, settings);
                    dev.rogler.teamwars.neoforge.protection.BaseGuardService.reconcile(server);
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

    private static java.util.UUID parseTeamId(net.minecraft.commands.CommandSourceStack src,String raw)
        throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        try { return java.util.UUID.fromString(raw); }
        catch(IllegalArgumentException invalid) {
            throw new com.mojang.brigadier.exceptions.CommandSyntaxException(
                new com.mojang.brigadier.exceptions.SimpleCommandExceptionType(Component.literal("Invalid FTB team UUID")),
                Component.literal("Invalid team UUID: "+raw));
        }
    }
    private int status(net.minecraft.commands.CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("Rogler TeamWars v" + VERSION
            + " | FTB party projection: " + scoreboard.status()
            + " | online parties: " + scoreboard.partyCount()
            + " | quarantined: " + scoreboard.quarantinedCount()
            + " | FTB API errors: " + scoreboard.apiFailures()), false);
        source.sendSuccess(() -> Component.literal("Features: FTB team sync, private nametags, /teamwars team; "
            + "core guard, private auto FTB claims, team UUID admin tools, armor tiers; raids opt-in; NO Xaero share yet."), false);
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
