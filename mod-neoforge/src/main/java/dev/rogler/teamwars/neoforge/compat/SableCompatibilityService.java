package dev.rogler.teamwars.neoforge.compat;

import dev.rogler.teamwars.core.compat.ExplosionObservationCounter;
import dev.rogler.teamwars.core.compat.SableCompatibilityPolicy;
import dev.rogler.teamwars.core.compat.SableSplitConfigAudit;
import dev.rogler.teamwars.neoforge.teams.RoglerTeamWarsLogger;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.level.ExplosionEvent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

/** Non-invasive Sable/Destructive readiness inventory; NO optional-mod classes or mixins.
 * Nothing in this service can alter a sublevel, explosion, claim or saved Minecraft region.
 */
public final class SableCompatibilityService {
    private SableCompatibilityService() {}
    private static MinecraftServer activeServer;
    private static boolean observeExplosions;
    private static String configStatus="not loaded";
    private static final ExplosionObservationCounter observations=new ExplosionObservationCounter();

    private static Path ownConfig(MinecraftServer server) {
        return server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT)
            .resolve("serverconfig/roglercore-sable.properties");
    }
    private static List<SableCompatibilityPolicy.Mod> loaded() {
        var mods=new ArrayList<SableCompatibilityPolicy.Mod>();
        for (var info : ModList.get().getMods()) {
            mods.add(new SableCompatibilityPolicy.Mod(info.getModId(),info.getDisplayName(),
                String.valueOf(info.getVersion())));
        }
        return List.copyOf(mods);
    }
    private static SableSplitConfigAudit.Result splitting() {
        // Sable's common TOML belongs to Sable; NEVER write, migrate or reformat it.
        Path file=Path.of("config", "sable-common.toml");
        if (!Files.isRegularFile(file,LinkOption.NOFOLLOW_LINKS))
            return new SableSplitConfigAudit.Result(SableSplitConfigAudit.State.MISSING,"unavailable",0);
        try {
            if (Files.size(file)>262_144) return new SableSplitConfigAudit.Result(
                SableSplitConfigAudit.State.INVALID,"oversized",0);
            try(var reader=Files.newBufferedReader(file)) {
                return SableSplitConfigAudit.inspect(reader);
            }
        } catch(IOException ex) {
            RoglerTeamWarsLogger.warn("SABLE_CONFIG_AUDIT: config not readable: "+ex.getClass().getSimpleName());
            return new SableSplitConfigAudit.Result(SableSplitConfigAudit.State.INVALID,"unreadable",0);
        }
    }
    /** Read only our own config; metrics OFF by default; never touches other mods' configs. */
    public static boolean reload(MinecraftServer server) {
        observeExplosions=false;
        observations.clear();
        Path path=ownConfig(server);
        if (!Files.isRegularFile(path,LinkOption.NOFOLLOW_LINKS)) {
            configStatus="defaults: explosion metrics OFF (optional config absent)";
            return true;
        }
        try {
            if (Files.size(path)>65_536)throw new IOException("oversized config");
            Properties p=new Properties();
            try(var reader=Files.newBufferedReader(path)) {p.load(reader);}
            String mode=p.getProperty("explosion_observation_enabled","false").trim().toLowerCase(Locale.ROOT);
            if(!mode.equals("true")&&!mode.equals("false"))throw new IOException("invalid boolean");
            observeExplosions=Boolean.parseBoolean(mode);
            configStatus="valid: explosion metrics "+(observeExplosions?"ON":"OFF");
            return true;
        } catch(IOException ex) {
            observeExplosions=false;
            configStatus="INVALID: forced metrics OFF ("+ex.getClass().getSimpleName()+")";
            RoglerTeamWarsLogger.warn("SABLE_OBSERVATION_CONFIG invalid; metrics disabled: "+ex.getMessage());
            return false;
        }
    }
    public static void initialize(MinecraftServer server) {
        activeServer=server;
        reload(server);
        var finding=SableCompatibilityPolicy.assess(loaded(),splitting().state());
        String line="SABLE_COMPAT_064 status="+finding.status()+" detail="+finding.detail()
            +" | splitting not changed; v0.6.4 lifecycle guard reported separately; "+configStatus;
        if(finding.warning())RoglerTeamWarsLogger.warn(line);
        else RoglerTeamWarsLogger.info(line);
    }
    public static void shutdown() {
        activeServer=null;
        observeExplosions=false;
        observations.clear();
    }
    /** Constant overhead only when opted in; no per-block scans or chunk loads. */
    public static void onExplosion(ExplosionEvent.Detonate event) {
        if (!observeExplosions || !(event.getLevel() instanceof ServerLevel level)
            || activeServer!=level.getServer())return;
        observations.observe(level.dimension().location().toString(),event.getAffectedBlocks().size());
    }
    private static void reply(CommandSourceStack src,String text) {
        src.sendSuccess(()->Component.literal("[Rogler Core] "+text),false);
    }
    public static int report(CommandSourceStack src) {
        var mods=loaded();
        var split=splitting();
        var finding=SableCompatibilityPolicy.assess(mods,split.state());
        reply(src,"Sable status="+finding.status()+": "+finding.detail());
        reply(src,"Sable common TOML: splitting="+split.state()+", heatmap_steps="+split.heatmapSteps()
            +" (read-only, relative to server config directory)");
        reply(src,"Sable: Destructive optional: when present it owns its split guards; Rogler lifecycle guard auto-disables. No Destructive API/ME bridge.");
        reply(src,"Config: "+configStatus+". /rogler admin compat explosions for event counts.");
        dev.rogler.teamwars.neoforge.compat.SableRemovedSublevelGuard.report(src);
        return 1;
    }
    public static int mods(CommandSourceStack src) {
        var mods=loaded();
        String[] ids={"sable","sabledestructive","wariumce","mahou_tsukai","ftbteams","ftbchunks","create"};
        for(String id:ids) {
            var mod=SableCompatibilityPolicy.exact(mods,id);
            reply(src,id+"="+(mod==null?"not found by exact ID":mod.version()));
        }
        String[] names={"destructive","mianbao","mahou","tree physics","aeronautics","collision damage"};
        for(String name:names) {
            var guess=SableCompatibilityPolicy.byName(mods,name);
            if(guess!=null)reply(src,"possible "+name+" (DISPLAY-NAME ONLY, not API verified): "
                +guess.id()+"@"+guess.version());
        }
        reply(src,"All optional mod-ID guesses are diagnostic only; no external mod classes loaded.");
        return 1;
    }
    public static int explosions(CommandSourceStack src) {
        reply(src,"Explosion observation="+(observeExplosions?"ON":"OFF")
            +" events="+observations.events()+" affected="+observations.blockPositions()
            +" max_event_blocks="+observations.maximumAffected()
            +" over_9999="+observations.largeEvents());
        reply(src,"Dimensions (up to 16 buckets): "+observations.dimensions());
        reply(src,"This is EVENT input only, not total nuke runtime or Sable plot damage; restart resets counters.");
        return 1;
    }
    public static int reloadCommand(CommandSourceStack src) {
        boolean result=reload(src.getServer());
        boolean lifecycle=dev.rogler.teamwars.neoforge.compat.SableRemovedSublevelGuard.reload(src.getServer());
        reply(src,"Sable diagnostic config="+(result?"OK":"INVALID/OFF")+
            ", lifecycle config="+(lifecycle?"OK":"INVALID/OFF")+
            "; Sable common settings were not modified.");
        return result&&lifecycle?1:0;
    }
}
