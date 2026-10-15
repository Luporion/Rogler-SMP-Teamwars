package dev.rogler.teamwars.neoforge.compat;

import dev.rogler.teamwars.core.compat.SableLifecyclePolicy;
import dev.rogler.teamwars.neoforge.teams.RoglerTeamWarsLogger;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.fml.ModList;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 2.0.6-only, pre-mutation checks for Sable's HeatMapManager. This intentionally
 * does NOT catch assembleBlocks exceptions or return null to its caller.
 * It never mutates Sable data, blocks, plots, physics bodies or region files.
 * Sable: Destructive, if installed, owns its own split guards instead.
 *
 * Verified against Sable tag mc1.21.1-2.0.6-neoforge source. Actual loaded JAR
 * and interactions with Shtreimel must still be tested before production use.
 */
public final class SableRemovedSublevelGuard {
    private SableRemovedSublevelGuard() {}

    private static volatile boolean active;
    private static volatile boolean checkOwnerTick = true;
    private static volatile boolean checkOwnerSplit = true;
    private static volatile boolean checkAnchor = true;
    private static volatile String status = "not initialized";
    private static volatile int maxWarningsPerMinute = 20;
    private static MinecraftServer server;
    private static final AtomicLong observedTicks = new AtomicLong();
    private static final AtomicLong observedSplits = new AtomicLong();
    private static final AtomicLong tickSkips = new AtomicLong();
    private static final AtomicLong splitOwnerSkips = new AtomicLong();
    private static final AtomicLong splitAnchorSkips = new AtomicLong();
    private static final AtomicLong preflightFailures = new AtomicLong();
    private static long logWindowStart;
    private static int logCount;

    // These are the *actual* 2.0.6 field names. No class reference to Sable at
    // build-time; optional integration cannot trigger NoClassDefFoundError.
    private record Fields(Field owner, Field splits, Field indices, Field solidCount) {}
    private static final class Group {
        final BlockPos anchor;
        int size;
        Group(BlockPos anchor) {this.anchor=anchor;}
    }
    private static final ClassValue<Fields> FIELDS = new ClassValue<>() {
        @Override protected Fields computeValue(Class<?> type) {
            try {
                Field owner = type.getDeclaredField("subLevel"); owner.setAccessible(true);
                Field splits = type.getDeclaredField("subLevelSplits"); splits.setAccessible(true);
                Field indices = type.getDeclaredField("splitIndexMap"); indices.setAccessible(true);
                Field count = type.getDeclaredField("solidCount"); count.setAccessible(true);
                return new Fields(owner,splits,indices,count);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Sable 2.0.6 heatmap field layout differs",e);
            }
        }
    };
    private static final ClassValue<Method> REMOVED = new ClassValue<>() {
        @Override protected Method computeValue(Class<?> type) {
            try {return type.getMethod("isRemoved");}
            catch(NoSuchMethodException e) {throw new IllegalStateException("Missing Sable isRemoved",e);}
        }
    };
    private static final ClassValue<Method> LEVEL = new ClassValue<>() {
        @Override protected Method computeValue(Class<?> type) {
            try {return type.getMethod("getLevel");}
            catch(NoSuchMethodException e) {throw new IllegalStateException("Missing Sable getLevel",e);}
        }
    };
    private static volatile Object helper;
    private static volatile Method getContaining;

    private static Path file(MinecraftServer s) {
        return s.getWorldPath(LevelResource.ROOT).resolve("serverconfig/roglercore-sable-lifecycle.properties");
    }
    private static boolean bool(Properties p,String key,boolean fallback) throws IOException {
        String raw=p.getProperty(key,String.valueOf(fallback)).trim().toLowerCase(Locale.ROOT);
        if (!raw.equals("true")&&!raw.equals("false")) throw new IOException("invalid boolean: "+key);
        return Boolean.parseBoolean(raw);
    }
    private static String versionOf(String id) {
        for(var mod:ModList.get().getMods())if(mod.getModId().equals(id))return String.valueOf(mod.getVersion());
        return null;
    }
    private static boolean destructivePresent() {
        for(var mod:ModList.get().getMods()) {
            String id=mod.getModId();
            if(id.equals("sabledestructive")||id.equals("sable_destructive") ||
                mod.getDisplayName().equalsIgnoreCase("Sable: Destructive"))return true;
        }
        return false;
    }
    /** Does not touch Sable's own common config; splitting stays true if configured by user. */
    public static synchronized boolean initialize(MinecraftServer s) {
        server=s;
        return reload(s);
    }
    public static synchronized boolean reload(MinecraftServer s) {
        active=false;
        boolean enabled=true;
        boolean ownerTick=true,ownerSplit=true,anchor=true;
        int limit=20;
        Path path=file(s);
        try {
            if (Files.isRegularFile(path,LinkOption.NOFOLLOW_LINKS)) {
                if(Files.size(path)>32_768)throw new IOException("oversized lifecycle config");
                Properties p=new Properties();
                try(var reader=Files.newBufferedReader(path)){p.load(reader);}
                enabled=bool(p,"enabled",true);
                ownerTick=bool(p,"cancel_tick_for_removed_owner",true);
                ownerSplit=bool(p,"cancel_split_for_removed_owner",true);
                anchor=bool(p,"cancel_split_for_removed_anchor_plot",true);
                limit=Integer.parseInt(p.getProperty("log_rate_limit_per_minute","20"));
                if(limit<0||limit>100)throw new IOException("log_rate_limit_per_minute must be 0..100");
            }
        } catch(IOException|NumberFormatException ex) {
            status="INVALID CONFIG: guard OFF ("+ex.getMessage()+")";
            RoglerTeamWarsLogger.warn("SABLE_LIFECYCLE_064 "+status);
            return false;
        }
        var reason=SableLifecyclePolicy.eligibility(enabled,versionOf("sable"),destructivePresent());
        checkOwnerTick=ownerTick;checkOwnerSplit=ownerSplit;checkAnchor=anchor;
        maxWarningsPerMinute=limit;
        active=reason==SableLifecyclePolicy.Reason.ACTIVE && (ownerTick||ownerSplit||anchor);
        status="gate="+reason+" guard="+(active?"ON":"OFF")+
            " owner_tick="+ownerTick+" owner_split="+ownerSplit+" anchor_preflight="+anchor+
            " (Sable splitting setting NEVER modified)";
        if(active)RoglerTeamWarsLogger.info("SABLE_LIFECYCLE_064 "+status);
        else RoglerTeamWarsLogger.warn("SABLE_LIFECYCLE_064 "+status);
        return true;
    }
    public static synchronized void shutdown(){server=null;active=false;helper=null;getContaining=null;status="server stopped";}

    private static Object owner(Object manager) throws ReflectiveOperationException {
        return FIELDS.get(manager.getClass()).owner().get(manager);
    }
    private static boolean removed(Object sublevel) throws ReflectiveOperationException {
        return sublevel != null && Boolean.TRUE.equals(REMOVED.get(sublevel.getClass()).invoke(sublevel));
    }
    private static synchronized void warn(String type,Object owner,String detail) {
        long now=System.nanoTime();
        if(logWindowStart==0 || now-logWindowStart>60_000_000_000L) {logWindowStart=now;logCount=0;}
        if(logCount++>=maxWarningsPerMinute)return;
        String id="unknown";
        if(owner!=null)try{id=String.valueOf(owner.getClass().getMethod("getUniqueId").invoke(owner));}
        catch(ReflectiveOperationException ignored){}
        RoglerTeamWarsLogger.warn("SABLE_LIFECYCLE_064 " + type + " sublevel="+id+" " + detail);
    }
    public static boolean shouldCancelTick(Object manager) {
        if(!active||server==null||!checkOwnerTick)return false;
        observedTicks.incrementAndGet();
        try {
            Object owner=owner(manager);
            if(!SableLifecyclePolicy.skipTick(removed(owner)))return false;
            tickSkips.incrementAndGet();warn("TICK_SKIP_REMOVED_OWNER",owner,"no Sable state changed");return true;
        } catch(ReflectiveOperationException|RuntimeException e) {
            preflightFailures.incrementAndGet();warn("PREFLIGHT_ERROR",null,e.getClass().getSimpleName());
            return true; // fail closed for this heatmap only; no global splitting change
        }
    }
    private static synchronized Method containing() throws ReflectiveOperationException {
        if(getContaining!=null)return getContaining;
        Class<?> sable=Class.forName("dev.ryanhcode.sable.Sable");
        helper=sable.getField("HELPER").get(null);
        // Sable 2.0.6 defines the overload (Level, Vec3i); BlockPos IS a Vec3i.
        getContaining=helper.getClass().getMethod("getContaining",Level.class,Vec3i.class);
        return getContaining;
    }

    /** Preflight BEFORE Sable's split() clears indices or calls any SplitListener.
     * Never intercept assembleBlocks() after some fragments have moved.
     */
    public static boolean shouldCancelSplit(Object manager) {
        if(!active||server==null)return false;
        observedSplits.incrementAndGet();
        Object owner=null;
        try {
            Fields f=FIELDS.get(manager.getClass());
            owner=f.owner().get(manager);
            if(checkOwnerSplit && SableLifecyclePolicy.skipSplit(removed(owner),false)) {
                splitOwnerSkips.incrementAndGet();warn("SPLIT_SKIP_REMOVED_OWNER",owner,"before mutation");return true;
            }
            if(!checkAnchor)return false;
            Long2IntOpenHashMap splits=(Long2IntOpenHashMap) f.splits().get(manager);
            IntArrayList indices=(IntArrayList) f.indices().get(manager);
            int solidCount=f.solidCount().getInt(manager);
            if(splits.isEmpty())return false;
            // Same grouping and full-sublevel / retained-largest rules as upstream
            // Sable 2.0.6 SubLevelHeatMapManager.split().
            var groups=new Int2ObjectOpenHashMap<Group>();
            for(long key:splits.keySet()) {
                int idx=indices.getInt(splits.get(key));
                groups.computeIfAbsent(idx,unused->new Group(BlockPos.of(key))).size++;
            }
            int total=0;
            for(Group group:groups.values())total+=group.size;
            if(!SableLifecyclePolicy.assemblesAnyGroup(groups.size(),total,solidCount))return false;
            if(SableLifecyclePolicy.retainLargest(groups.size(),total,solidCount)) {
                int biggest=0;Integer retain=null;
                for(var entry:groups.int2ObjectEntrySet()) {
                    if(entry.getValue().size>biggest){biggest=entry.getValue().size;retain=entry.getIntKey();}
                }
                if(retain!=null)groups.remove((int)retain);
            }
            Object level=LEVEL.get(owner.getClass()).invoke(owner);
            Method method=containing();
            for(Group group:groups.values()) {
                Object containing=method.invoke(helper,level,group.anchor);
                if(removed(containing)) {
                    splitAnchorSkips.incrementAndGet();
                    warn("SPLIT_SKIP_REMOVED_ANCHOR",owner,"anchor="+group.anchor+" before mutation");
                    return true;
                }
            }
            return false;
        } catch(ReflectiveOperationException|RuntimeException e) {
            preflightFailures.incrementAndGet();
            warn("PREFLIGHT_ERROR",owner,e.getClass().getSimpleName()+"; split cancelled before changes");
            return true;
        }
    }

    public static int report(CommandSourceStack src) {
        src.sendSuccess(()->Component.literal("[Rogler Core] Sable lifecycle "+status),false);
        src.sendSuccess(()->Component.literal("[Rogler Core] observed mixin tick/split calls="+observedTicks.get()+"/"+observedSplits.get()+" (zero does not prove hook applied)"),false);
        src.sendSuccess(()->Component.literal("[Rogler Core] removed-owner ticks="+tickSkips.get()
            +", removed-owner splits="+splitOwnerSkips.get()+", removed-anchor splits="+splitAnchorSkips.get()
            +", guard inspection errors="+preflightFailures.get()),false);
        src.sendSuccess(()->Component.literal("[Rogler Core] Guard only at tick/split HEAD; third-party SplitListener reentrancy NOT yet proven safe."),false);
        return 1;
    }
}
