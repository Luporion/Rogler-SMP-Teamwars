package dev.rogler.teamwars.core.base;

import java.util.UUID;

/** Server authoritative core-only raid rules. Never grant regular block edit access. */
public final class RaidPolicy {
    private RaidPolicy() {}
    public static final int CORE_HP=10;
    public static final long HIT_COOLDOWN_MILLIS=750L;
    public record Window(UUID defender, UUID attacker, long untilMillis, int hp, long lastHitMillis) {
        public Window {
            if(defender==null || hp<0 || hp>CORE_HP)throw new IllegalArgumentException("Invalid raid state");
        }
    }
    public static boolean eligible(Window raid, UUID defender, UUID attacker, long now,
                                   long graceUntil, boolean defenderOnline) {
        return raid!=null && raid.defender().equals(defender) && attacker!=null && !attacker.equals(defender)
            && (raid.attacker()==null || raid.attacker().equals(attacker))
            && raid.hp()>0 && now<raid.untilMillis() && now>=graceUntil && defenderOnline;
    }
    public static Window hit(Window raid, UUID attacker, long now) {
        if(raid==null || attacker==null || attacker.equals(raid.defender()))throw new IllegalStateException("Invalid attacker");
        if(raid.attacker()!=null && !raid.attacker().equals(attacker))throw new IllegalStateException("Raid attacker already locked");
        if(raid.lastHitMillis()>0 && now-raid.lastHitMillis()<HIT_COOLDOWN_MILLIS)
            throw new IllegalStateException("Hit cooldown");
        if(raid.hp()<=0 || now>=raid.untilMillis())throw new IllegalStateException("Raid inactive");
        return new Window(raid.defender(),attacker,raid.untilMillis(),raid.hp()-1,now);
    }
}
