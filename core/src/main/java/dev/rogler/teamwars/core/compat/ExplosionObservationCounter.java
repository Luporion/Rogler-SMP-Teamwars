package dev.rogler.teamwars.core.compat;

import java.util.LinkedHashMap;
import java.util.Map;

/** Bounded, per-server-thread counters. No BlockPos iteration, Chunk access or retained entities. */
public final class ExplosionObservationCounter {
    public static final int MAX_DIMENSIONS=16;
    private long events, blockPositions, maximumAffected, largeEvents;
    private final Map<String,Long> dimensionCounts=new LinkedHashMap<>();
    public void observe(String dimension,int affected) {
        if(dimension==null||dimension.isBlank())dimension="unknown";
        int n=Math.max(0,affected);
        events=saturatedAdd(events,1);
        blockPositions=saturatedAdd(blockPositions,n);
        maximumAffected=Math.max(maximumAffected,n);
        if(n>=10_000)largeEvents=saturatedAdd(largeEvents,1);
        // Cap distinct keys: count all excess in a single overflow bucket.
        String key=dimensionCounts.containsKey(dimension) || dimensionCounts.size()<MAX_DIMENSIONS-1
            ? dimension:"other";
        dimensionCounts.merge(key,1L,ExplosionObservationCounter::saturatedAdd);
    }
    private static long saturatedAdd(long a,long b){return Long.MAX_VALUE-a<b?Long.MAX_VALUE:a+b;}
    public long events(){return events;}
    public long blockPositions(){return blockPositions;}
    public long maximumAffected(){return maximumAffected;}
    public long largeEvents(){return largeEvents;}
    public Map<String,Long> dimensions(){return Map.copyOf(dimensionCounts);}
    public void clear(){events=blockPositions=maximumAffected=largeEvents=0;dimensionCounts.clear();}
}
