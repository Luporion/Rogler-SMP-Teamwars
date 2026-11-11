package dev.rogler.teamwars.neoforge.protection;

import dev.rogler.teamwars.core.ui.DiagnosticPolicy;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/** Read-only FTB 2101.1.23 contract. Never calls getOrCreateData, claim or ticket APIs.
 * Global claim totals are inspected only if their O(1) collection size is <=512.
 * Larger installations still get exact, bounded (at most 7x7) core-zone observations.
 */
public final class FtbChunkStatusReader {
    private FtbChunkStatusReader() {}
    @FunctionalInterface public interface Positions {Object at(int x,int z) throws ReflectiveOperationException;}
    @FunctionalInterface public interface Loaded {boolean at(int x,int z);}
    public record Chunk(int x,int z,String owner,Boolean marked,Boolean eligible,boolean loaded) {}
    public record Snapshot(Integer claimed,Integer marked,Integer maxClaims,Integer maxForced,
                           Boolean offline,List<Chunk> zone,String detail) {
        public Snapshot {zone=List.copyOf(zone);}
    }
    private static Object call(Object obj,String name,Object... args) throws ReflectiveOperationException {
        if(obj==null)throw new IllegalStateException("FTB manager not ready");
        for(Method method:obj.getClass().getMethods()) {
            if(method.getName().equals(name) && method.getParameterCount()==args.length) {
                try{return method.invoke(obj,args);}catch(IllegalArgumentException wrongOverload){/* next */}
            }
        }
        throw new NoSuchMethodException(name);
    }
    private static Boolean bool(Object obj,String method) {
        try{return call(obj,method) instanceof Boolean value?value:null;}
        catch(ReflectiveOperationException|RuntimeException ex){return null;}
    }
    private static Integer integer(Object obj,String method) {
        try {Object v=call(obj,method);return v instanceof Integer n && n>=0?n:null;}
        catch(ReflectiveOperationException|RuntimeException ex){return null;}
    }
    private static UUID team(Object data) throws ReflectiveOperationException {
        Object id=call(data,"getTeamId");
        if(!(id instanceof UUID uuid))throw new IllegalStateException("Invalid FTB team id");
        return uuid;
    }
    public static Object existingData(Object manager,UUID target) throws ReflectiveOperationException {
        Object raw=call(manager,"allTeamData");
        if(raw instanceof Collection<?> teams && teams.size()<=DiagnosticPolicy.MAX_TEAM_SCAN){
            int scanned=0;
            for(Object candidate:teams){
                if(scanned++>=DiagnosticPolicy.MAX_TEAM_SCAN)break;
                if(target.equals(team(candidate)))return candidate;
            }
        }
        return null;
    }
    public static Snapshot read(Object manager,UUID target,Integer centerX,Integer centerZ,int radius,
                                Positions positions,Loaded loaded) {
        if(target==null || radius<0 || radius>3 || (centerX==null)!=(centerZ==null))
            throw new IllegalArgumentException("Invalid diagnostic scope");
        Integer claimed=null,marked=null;Object data=null;
        String detail="FTB-API gelesen";
        var zone=new ArrayList<Chunk>();
        // Core zone first: missing optional totals never hide the useful local observations.
        if(centerX!=null)for(int x=centerX-radius;x<=centerX+radius;x++)for(int z=centerZ-radius;z<=centerZ+radius;z++) {
            String owner="unbekannt";Boolean flag=null,eligible=null;
            try {
                Object chunk=call(manager,"getChunk",positions.at(x,z));
                if(chunk==null){owner="frei";flag=false;eligible=false;}
                else {
                    Object chunkData=call(chunk,"getTeamData");
                    if(target.equals(team(chunkData))) {
                        owner="eigen";data=chunkData;
                        flag=bool(chunk,"isForceLoaded");eligible=bool(chunk,"isActuallyForceLoaded");
                    }else owner="fremd"; // no foreign team ID, name or loading policy in this report
                }
            }catch(ReflectiveOperationException|RuntimeException ex){detail="FTB-API teilweise nicht verfuegbar";}
            zone.add(new Chunk(x,z,owner,flag,eligible,loaded.at(x,z)));
        }
        try {
            Object raw=call(manager,"getAllClaimedChunks");
            if(raw instanceof Collection<?> all && all.size()<=DiagnosticPolicy.MAX_CLAIM_SCAN) {
                int own=0,forced=0,scanned=0;boolean complete=true,flagsKnown=true;
                for(Object chunk:all) {
                    if(scanned++>=DiagnosticPolicy.MAX_CLAIM_SCAN){complete=false;break;}
                    Object chunkData=call(chunk,"getTeamData");
                    if(target.equals(team(chunkData))) {
                        data=chunkData;own++;
                        Boolean flag=bool(chunk,"isForceLoaded");
                        if(flag==null)flagsKnown=false;else if(flag)forced++;
                    }
                }
                if(complete){claimed=own;if(flagsKnown)marked=forced;}
            }else detail="Teamgesamtzahlen: unbekannt (Abfragelimit)";
        }catch(ReflectiveOperationException|RuntimeException ex){detail="Teamgesamtzahlen: API nicht verfuegbar";}
        if(data==null)try {data=existingData(manager,target);}
        catch(ReflectiveOperationException|RuntimeException ignored){/* explicit unknowns below */}
        return new Snapshot(claimed,marked,integer(data,"getMaxClaimChunks"),integer(data,"getMaxForceLoadChunks"),
            bool(data,"canDoOfflineForceLoading"),zone,detail);
    }
}
