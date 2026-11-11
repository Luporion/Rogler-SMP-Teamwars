package dev.rogler.teamwars.neoforge.protection;

import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Executes the real reflection reader, including traps for writes and unbounded scans. */
public final class FtbChunkStatusReaderTests {
    private static int checks;
    private static void check(boolean value){checks++;if(!value)throw new AssertionError("check "+checks);}
    public record Pos(int x,int z) {}
    public static class Data {
        public final UUID id;public int maxClaims=500,maxForced=25;
        public Data(UUID id){this.id=id;}
        public UUID getTeamId(){return id;}
        public int getMaxClaimChunks(){return maxClaims;}
        public int getMaxForceLoadChunks(){return maxForced;}
        public boolean canDoOfflineForceLoading(){return false;}
        public Object getClaimedChunks(){throw new AssertionError("Cache-building getter forbidden");}
        public void markDirty(){throw new AssertionError("Diagnostic write");}
    }
    public record Claim(Data data,boolean marked,boolean active) {
        public Data getTeamData(){return data;}
        public boolean isForceLoaded(){return marked;}
        public boolean isActuallyForceLoaded(){return active;}
    }
    public static class Manager {
        public final Map<Pos,Claim> zone=new HashMap<>();
        public Collection<Claim> all=new ArrayList<>();
        public Collection<Data> teams=new ArrayList<>();
        public int lookups;
        public Claim getChunk(Pos pos){lookups++;return zone.get(pos);}
        public Collection<Claim> getAllClaimedChunks(){return all;}
        public Collection<Data> allTeamData(){return teams;}
        public Object getOrCreateData(Object t){throw new AssertionError("getOrCreateData forbidden");}
        public void claim(){throw new AssertionError("claim forbidden");}
        public void forceLoad(){throw new AssertionError("ticket mutation forbidden");}
    }
    private static FtbChunkStatusReader.Snapshot read(Manager m,UUID id,Integer x,Integer z,int radius){
        return FtbChunkStatusReader.read(m,id,x,z,radius,Pos::new,(cx,cz)->cx==0&&cz==0);
    }
    public static void main(String[] args) throws Exception {
        UUID own=UUID.randomUUID(),foreign=UUID.randomUUID();Data a=new Data(own),b=new Data(foreign);
        Manager manager=new Manager();
        var ownClaim=new Claim(a,true,false);var foreignClaim=new Claim(b,true,true);
        manager.zone.put(new Pos(0,0),ownClaim);manager.zone.put(new Pos(1,0),foreignClaim);
        manager.all=List.of(ownClaim,foreignClaim);manager.teams=List.of(a,b);
        var result=read(manager,own,0,0,1);
        check(manager.lookups==9);check(result.zone().size()==9);
        check(result.claimed()==1);check(result.marked()==1);check(result.maxForced()==25);
        check(Boolean.FALSE.equals(result.offline()));
        var c=result.zone().stream().filter(v->v.x()==0&&v.z()==0).findFirst().orElseThrow();
        check(c.owner().equals("eigen"));check(c.marked());check(!c.eligible());check(c.loaded());
        var f=result.zone().stream().filter(v->v.x()==1&&v.z()==0).findFirst().orElseThrow();
        check(f.owner().equals("fremd"));check(f.marked()==null&&f.eligible()==null);
        check(!result.toString().contains(foreign.toString()));
        var free=result.zone().stream().filter(v->v.x()==-1&&v.z()==-1).findFirst().orElseThrow();
        check(free.owner().equals("frei")&&!free.marked()&&!free.loaded());
        manager.lookups=0;read(manager,own,null,null,1);check(manager.lookups==0);
        read(manager,own,0,0,3);check(manager.lookups==49);
        // A large collection must never even be iterated, including cache-building getters.
        manager.all=new AbstractCollection<>() {
            public int size(){return 513;}
            public Iterator<Claim> iterator(){throw new AssertionError("Unbounded global claim scan");}
        };
        result=read(manager,own,0,0,0);
        check(result.claimed()==null&&result.marked()==null);check(result.maxClaims()==500);
        check(result.zone().getFirst().owner().equals("eigen"));
        // Unknown FTB API still returns the independent loaded observation.
        result=FtbChunkStatusReader.read(new Object(),own,0,0,0,Pos::new,(x,z)->true);
        check(result.claimed()==null&&result.maxForced()==null&&result.offline()==null);
        check(result.zone().getFirst().owner().equals("unbekannt")&&result.zone().getFirst().loaded());
        // No existing team data is never invented for diagnosis.
        Manager empty=new Manager();result=read(empty,own,null,null,0);
        check(result.claimed()==0&&result.marked()==0);check(result.maxClaims()==null);
        check(FtbChunkStatusReader.existingData(empty,own)==null);
        // Unknown quota sentinel remains unknown rather than claiming unlimited/free capacity.
        a.maxClaims=-1;result=read(manager,own,0,0,0);check(result.maxClaims()==null);
        // Even a collection lying about size cannot cause more than 512 processed entries.
        final int[] visited={0};
        manager.all=new AbstractCollection<>() {
            public int size(){return 1;}
            public Iterator<Claim> iterator(){return new Iterator<>(){
                public boolean hasNext(){return visited[0]<10000;}
                public Claim next(){visited[0]++;return foreignClaim;}
            };}
        };
        result=read(manager,own,null,null,0);check(visited[0]<=513);check(result.claimed()==null);
        System.out.println("v0.8.8 FtbChunkStatusReaderTests: "+checks+" checks OK (real adapter, read-only traps, scopes and scan bounds)");
    }
}
