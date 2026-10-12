package dev.rogler.teamwars.core.base;

import java.util.UUID;

/** Geometry and policy independent of NeoForge. Every radius is measured in CHUNKS. */
public final class BaseProtectionPolicy {
    private BaseProtectionPolicy() {}
    public static boolean contains(int coreBlockX,int coreBlockZ,int targetBlockX,int targetBlockZ,int chunkRadius) {
        if(chunkRadius<0 || chunkRadius>3)throw new IllegalArgumentException("chunk radius 0..3");
        return Math.abs((long)(coreBlockX>>4)-(targetBlockX>>4))<=chunkRadius
            && Math.abs((long)(coreBlockZ>>4)-(targetBlockZ>>4))<=chunkRadius;
    }
    public static boolean overlaps(int ax,int az,int bx,int bz,int chunkRadius) {
        if(chunkRadius<0 || chunkRadius>3)throw new IllegalArgumentException("chunk radius 0..3");
        return Math.abs((long)(ax>>4)-(bx>>4))<=chunkRadius*2L
            && Math.abs((long)(az>>4)-(bz>>4))<=chunkRadius*2L;
    }
    public static boolean canEdit(UUID owner,UUID actor) {
        return owner != null && owner.equals(actor);
    }
    public static long graceRemainingMillis(long now,long until) {
        return Math.max(0L,until-now);
    }
}
