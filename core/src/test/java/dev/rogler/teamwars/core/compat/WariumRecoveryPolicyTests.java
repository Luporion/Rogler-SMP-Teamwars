package dev.rogler.teamwars.core.compat;

import static dev.rogler.teamwars.core.compat.WariumRecoveryPolicy.*;

public final class WariumRecoveryPolicyTests {
    private static int checks;
    private static void eq(Object a, Object b) { checks++; if (!a.equals(b)) throw new AssertionError(a+" != "+b); }
    private static void yes(boolean b) { checks++; if (!b) throw new AssertionError("expected true"); }
    private static void no(boolean b) { checks++; if (b) throw new AssertionError("expected false"); }
    public static void main(String[] args) {
        String id="wariumce:fusionblastentity";
        yes(isFusionBlast("wariumce","FusionBlastEntityEntity",id,""));
        yes(isFusionBlast("wariumce","FusionBlastEntityEntity",id,id));
        no(isFusionBlast("wariumce","FusionBlastEntityEntity",id,"wariumce:other"));
        no(isFusionBlast("minecraft","FusionBlastEntityEntity","minecraft:fusion", ""));
        no(isFusionBlast("wariumce","AnotherNukeEntity",id,""));
        no(isFusionBlast("wariumce","FusionBlastEntityEntity","another:fusion", ""));
        no(isFusionBlast("wariumce","FusionBlastEntityEntity",null,""));
        eq(parseMode("block_disk_loaded"), Mode.BLOCK_DISK_LOADED);
        eq(parseMode(" OFF "), Mode.OFF);
        eq(decide(Mode.OFF,true,true),Action.IGNORE);
        eq(decide(Mode.OFF,true,false),Action.IGNORE);
        eq(decide(Mode.LOG_ONLY,true,true),Action.OBSERVE);
        eq(decide(Mode.LOG_ONLY,true,false),Action.OBSERVE);
        eq(decide(Mode.BLOCK_DISK_LOADED,true,true),Action.DENY_JOIN);
        eq(decide(Mode.BLOCK_DISK_LOADED,true,false),Action.OBSERVE);
        eq(decide(Mode.BLOCK_ALL_NEW,true,true),Action.OBSERVE);
        eq(decide(Mode.BLOCK_ALL_NEW,true,false),Action.DENY_JOIN);
        eq(decide(Mode.BLOCK_DISK_LOADED,false,true),Action.IGNORE);
        eq(decide(Mode.BLOCK_ALL_NEW,false,false),Action.IGNORE);
        boolean failed = false; try {parseMode("DELETE_ALL");} catch(IllegalArgumentException ex) {failed=true;}yes(failed);
        System.out.println("v0.6.1 WariumRecoveryPolicyTests: "+checks+" checks OK");
    }
}
