package dev.rogler.teamwars.core;

import java.util.*;

/** Pure accounting; FTB ticket creation/removal belongs to adapter, not here. */
public final class ChunkLoadingBudget {
    private final int manualLimit;
    private final int coreLimit;
    private final Set<ChunkKey> manual = new HashSet<>();
    private final Set<ChunkKey> core = new HashSet<>();
    public ChunkLoadingBudget(int manualLimit, int coreLimit) {
        if (manualLimit < 0 || coreLimit < 0) throw new IllegalArgumentException("Negative limits");
        this.manualLimit = manualLimit;
        this.coreLimit = coreLimit;
    }
    public boolean addManual(ChunkKey chunk) {
        if (manual.contains(chunk)) return true;
        return manual.size() < manualLimit && manual.add(chunk);
    }
    public boolean addCore(ChunkKey chunk) {
        if (core.contains(chunk)) return true;
        return core.size() < coreLimit && core.add(chunk);
    }
    public void clearCore() { core.clear(); }
    public int actualDistinctLoaded() {
        Set<ChunkKey> all = new HashSet<>(manual);
        all.addAll(core);
        return all.size();
    }
    public int manualCount() { return manual.size(); }
    public int coreCount() { return core.size(); }
}
