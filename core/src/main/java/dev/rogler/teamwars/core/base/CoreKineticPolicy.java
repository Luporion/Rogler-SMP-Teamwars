package dev.rogler.teamwars.core.base;
/** Pure policy: little Create utility power, never a substitute for real power. */
public final class CoreKineticPolicy {
    private CoreKineticPolicy() {}
    public static final int RPM=16;
    public static final int CAPACITY_PER_RPM=16;
    public static final int STRESS_UNITS=RPM*CAPACITY_PER_RPM;
    public static final int ALLOY_COST=16;
    public static final int SHAFT_COST=8;
    public static boolean canUpgrade(int level,int alloy,int shafts,boolean placed,boolean raid,boolean survival){
        return level==0 && alloy>=ALLOY_COST && shafts>=SHAFT_COST && placed && !raid && survival;
    }
}
