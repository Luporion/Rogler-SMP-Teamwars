package dev.rogler.teamwars.neoforge.protection;

import dev.rogler.teamwars.neoforge.teams.RoglerTeamWarsLogger;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.fml.ModList;
import java.lang.reflect.*;
import java.util.*;

/** Optional 2101.1.x FTB Chunks adapter. No bundled or copied FTB code.
 * Fails closed, never unclaims anything not recorded as created by TeamWars. */
public final class FtbClaimBridge {
    private FtbClaimBridge() {}
    private static final Set<String> loggedFailures=new HashSet<>();
    public record Result(int created,int existing,int failed,String detail) {}
    private record Api(Object manager,Object teamData,Class<?> chunkDimPos,Method getChunk,Method claim,Method unclaim) {}
    private static Object call(Object obj,String name,Object... args) throws ReflectiveOperationException {
        for(Method m:obj.getClass().getMethods()) {
            if(m.getName().equals(name) && m.getParameterCount()==args.length) {
                try { return m.invoke(obj,args); }
                catch(IllegalArgumentException ignore) { /* overload */ }
            }
        }
        throw new NoSuchMethodException(obj.getClass().getName()+"."+name+"("+args.length+")");
    }
    private static Method method(Object obj,String name,int count) throws NoSuchMethodException {
        for(Method m:obj.getClass().getMethods())if(m.getName().equals(name)&&m.getParameterCount()==count)return m;
        throw new NoSuchMethodException(obj.getClass().getName()+"."+name);
    }
    private static Api bind(UUID teamId) throws ReflectiveOperationException {
        Class<?> ftbTeams=Class.forName("dev.ftb.mods.ftbteams.api.FTBTeamsAPI");
        Object teamsApi=ftbTeams.getMethod("api").invoke(null);
        Object teamsManager=call(teamsApi,"getManager");
        Object teamOptional=call(teamsManager,"getTeamByID",teamId);
        if(!(teamOptional instanceof Optional<?> opt) || opt.isEmpty())throw new IllegalStateException("Unknown FTB team " + teamId);
        Object team=opt.get();
        Class<?> mgr=Class.forName("dev.ftb.mods.ftbchunks.data.ClaimedChunkManagerImpl");
        Object claims=mgr.getMethod("getInstance").invoke(null);
        Object data=call(claims,"getOrCreateData",team);
        Class<?> cdp=Class.forName("dev.ftb.mods.ftblibrary.math.ChunkDimPos");
        return new Api(claims,data,cdp,method(claims,"getChunk",1),method(data,"claim",3),method(data,"unclaim",4));
    }
    private static Object pos(Api api,ServerLevel level,int cx,int cz) throws ReflectiveOperationException {
        for(Constructor<?> ctor:api.chunkDimPos().getConstructors()) {
            if(ctor.getParameterCount()==2) {
                try {return ctor.newInstance(level.dimension(),new ChunkPos(cx,cz));}
                catch(IllegalArgumentException e) { /* alternate signature */ }
            }
        }
        throw new NoSuchMethodException("ChunkDimPos(ResourceKey<Level>, ChunkPos)");
    }
    private static boolean success(Object result) throws ReflectiveOperationException {
        return result != null && Boolean.TRUE.equals(call(result,"isSuccess"));
    }
    private static UUID owner(Object claim) throws ReflectiveOperationException {
        return (UUID)call(call(claim,"getTeamData"),"getTeamId");
    }
    public static Result claim(ServerLevel level, UUID team,int cx,int cz,int radius,
                                BaseProtectionSavedData.Record record,CommandSourceStack src,
                                BaseProtectionSavedData saved) {
        if(!ModList.get().isLoaded("ftbchunks"))return new Result(0,0,0,"FTB Chunks absent, native guard remains");
        int created=0,existing=0,failed=0;
        try {
            Api api=bind(team);
            for(int x=cx-radius;x<=cx+radius;x++)for(int z=cz-radius;z<=cz+radius;z++) {
                long k=BaseProtectionSavedData.key(x,z);
                Object cdp=pos(api,level,x,z);
                Object existingClaim=api.getChunk().invoke(api.manager(),cdp);
                if(existingClaim!=null) {
                    if(owner(existingClaim).equals(team))existing++;
                    else failed++;
                    continue;
                }
                Object outcome=api.claim().invoke(api.teamData(),src,cdp,false);
                if(success(outcome)) { record.createdClaims.add(k);saved.changed();created++; }
                else failed++;
            }
            RoglerTeamWarsLogger.info("FTB_CLAIM_SYNC team="+team+" created="+created+" existing="+existing+" rejected="+failed);
            return new Result(created,existing,failed,"OK");
        } catch (ReflectiveOperationException | RuntimeException err) {
            Throwable cause=err instanceof InvocationTargetException ex?ex.getTargetException():err;
            String reason=cause.getClass().getSimpleName()+": "+cause.getMessage();
            if(loggedFailures.add(reason))RoglerTeamWarsLogger.warn("FTB claim adapter failed; native guard remains active. "+reason);
            return new Result(created,existing,failed,reason);
        }
    }
    public static void release(ServerLevel level,UUID team,BaseProtectionSavedData.Record record,
                               CommandSourceStack src,BaseProtectionSavedData saved) {
        if(record==null || record.createdClaims.isEmpty() || !ModList.get().isLoaded("ftbchunks"))return;
        try {
            Api api=bind(team);
            for(long k:new HashSet<>(record.createdClaims)) {
                Object cdp=pos(api,level,BaseProtectionSavedData.x(k),BaseProtectionSavedData.z(k));
                Object existing=api.getChunk().invoke(api.manager(),cdp);
                if(existing==null) {record.createdClaims.remove(k);saved.changed();continue;}
                if(!owner(existing).equals(team))continue; // NEVER unclaim enemy ownership
                if(success(api.unclaim().invoke(api.teamData(),src,cdp,false,false))) {
                    record.createdClaims.remove(k);saved.changed();
                }
            }
        } catch(ReflectiveOperationException | RuntimeException e) {
            RoglerTeamWarsLogger.warn("FTB claim release incomplete for team "+team+": "+e.getMessage());
        }
    }
}
