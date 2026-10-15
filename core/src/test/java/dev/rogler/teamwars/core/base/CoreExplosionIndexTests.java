package dev.rogler.teamwars.core.base;

import java.util.List;
import java.util.UUID;

public class CoreExplosionIndexTests {
    static int checks;
    static void yes(boolean v) { checks++; if(!v) throw new AssertionError("CoreExplosionIndex #"+checks); }
    public static void main(String[] args) {
        var a = new CoreExplosionIndex.Center(UUID.randomUUID(), 0,0);
        var b = new CoreExplosionIndex.Center(UUID.randomUUID(), -257,255);
        var idx = CoreExplosionIndex.build(List.of(a),1);
        yes(idx.coveredChunks()==9);
        yes(idx.protectsBlock(0,0));
        yes(idx.protectsBlock(-16,-16));
        yes(idx.protectsBlock(31,31));
        yes(!idx.protectsBlock(32,0));
        yes(!idx.protectsBlock(-33,0));
        yes(!idx.protectsBlock(0,32));
        yes(!idx.protectsBlock(0,-33));
        yes(!idx.protectsBlock(320,320));
        var both=CoreExplosionIndex.build(List.of(a,b),1);
        yes(both.coveredChunks()>9);
        yes(both.protectsBlock(-257,255));
        yes(both.protectsBlock(-241,255));
        yes(!idx.protectsBlock(-257,255));
        yes(CoreExplosionIndex.build(List.of(),3).coveredChunks()==0);
        yes(CoreExplosionIndex.build(List.of(a),0).coveredChunks()==1);
        yes(!CoreExplosionIndex.build(List.of(a),0).protectsBlock(-1,0));
        var moved=CoreExplosionIndex.build(List.of(new CoreExplosionIndex.Center(a.team(),1600,0)),1);
        yes(!moved.protectsBlock(0,0)); // event-local index immediately reflects relocation
        yes(moved.protectsBlock(1600,0));
        for(int r=0;r<=3;r++){
            var exact=CoreExplosionIndex.build(List.of(b),r);
            for(int dx=-r-1;dx<=r+1;dx++)for(int dz=-r-1;dz<=r+1;dz++){
                int x=((b.blockX()>>4)+dx)*16;
                int z=((b.blockZ()>>4)+dz)*16;
                yes(exact.protectsBlock(x,z) == BaseProtectionPolicy.contains(b.blockX(),b.blockZ(),x,z,r));
            }
        }
        var bound=CoreExplosionIndex.build(List.of(new CoreExplosionIndex.Center(a.team(),Integer.MAX_VALUE,Integer.MIN_VALUE)),3);
        yes(bound.protectsBlock(Integer.MAX_VALUE,Integer.MIN_VALUE));
        yes(!bound.protectsBlock(Integer.MIN_VALUE,Integer.MAX_VALUE));
        try { CoreExplosionIndex.build(List.of(a),4);throw new AssertionError("radius"); }
        catch(IllegalArgumentException expected){checks++;}
        System.out.println("v0.6.2 CoreExplosionIndexTests: "+checks+" checks OK");
    }
}
