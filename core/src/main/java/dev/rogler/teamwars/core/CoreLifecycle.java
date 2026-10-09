package dev.rogler.teamwars.core;

import java.util.Objects;

/** Time comes from calling game/server layer; no wall-clock dependency or scheduled world mutations. */
public final class CoreLifecycle {
    public enum Mode { CARRIED, STATIONARY, MOBILE, DESTROYED }
    public enum Action { NONE, AUTO_PLACE, RETURN_TO_LAST_SAFE, DETONATE, WARNING }
    public record Position(String dimension, int x, int y, int z) {
        public Position { Objects.requireNonNull(dimension); }
    }
    public record Decision(Action action, Position target, int remainingSeconds, String reason) {}
    private final BaseZone zone;
    private final int carryLimitSeconds;
    private final int mobileLimitSeconds;
    private Mode mode = Mode.CARRIED;
    private long carriedSeconds;
    private long mobileOutsideSeconds;
    private int recoveryRemainderSeconds;
    private Position lastSafePosition;

    /** Captures runtime state for a server SavedData adapter (not yet disk-persisted by P0). */
    public record Snapshot(Mode mode, long carriedSeconds, long mobileOutsideSeconds,
                           int recoveryRemainderSeconds, Position lastSafePosition) {}
    public Snapshot snapshot() {
        return new Snapshot(mode, carriedSeconds, mobileOutsideSeconds, recoveryRemainderSeconds, lastSafePosition);
    }
    public static CoreLifecycle restore(BaseZone zone, int carryLimitSeconds, int mobileLimitSeconds, Snapshot snapshot) {
        Objects.requireNonNull(snapshot);
        if (snapshot.mode() == null || snapshot.carriedSeconds() < 0 || snapshot.mobileOutsideSeconds() < 0
                || snapshot.recoveryRemainderSeconds() < 0 || snapshot.recoveryRemainderSeconds() > 1)
            throw new IllegalArgumentException("Invalid lifecycle snapshot");
        CoreLifecycle restored = new CoreLifecycle(zone, carryLimitSeconds, mobileLimitSeconds);
        restored.mode = snapshot.mode();
        restored.carriedSeconds = snapshot.carriedSeconds();
        restored.mobileOutsideSeconds = snapshot.mobileOutsideSeconds();
        restored.recoveryRemainderSeconds = snapshot.recoveryRemainderSeconds();
        restored.lastSafePosition = snapshot.lastSafePosition();
        return restored;
    }

    public CoreLifecycle(BaseZone zone, int carryLimitSeconds, int mobileLimitSeconds) {
        if (carryLimitSeconds <= 0 || mobileLimitSeconds <= 0) throw new IllegalArgumentException("Invalid limits");
        this.zone = Objects.requireNonNull(zone);
        this.carryLimitSeconds = carryLimitSeconds;
        this.mobileLimitSeconds = mobileLimitSeconds;
    }
    public Mode mode() { return mode; }
    public long carriedSeconds() { return carriedSeconds; }
    public long mobileOutsideSeconds() { return mobileOutsideSeconds; }
    private boolean legal(Position pos) {
        return pos != null && "minecraft:overworld".equals(pos.dimension()) && zone.placementAllowed(pos.x(),pos.z());
    }
    /** Measured active carrier time only. Persist at least once per minute and on lifecycle transitions. */
    public Decision tickCarried(int elapsedSeconds, Position current) {
        if (mode != Mode.CARRIED) throw new IllegalStateException("Not carried");
        if (elapsedSeconds < 0 || elapsedSeconds > 60) throw new IllegalArgumentException("Time step must be 0..60");
        carriedSeconds += elapsedSeconds;
        if (legal(current)) lastSafePosition = current;
        int remaining = (int)Math.max(0, carryLimitSeconds - carriedSeconds);
        if (remaining == 0) {
            if (legal(current)) {
                return new Decision(Action.AUTO_PLACE, current, 0, "Attempt legal placement, acknowledge only after success");
            }
            if (lastSafePosition != null)
                return new Decision(Action.RETURN_TO_LAST_SAFE, lastSafePosition, 0, "Carrier timed out outside placement zone");
            // Important: no authorized target; caller must put core into safe escrow, NOT fabricate a position.
            return new Decision(Action.WARNING, null, 0, "No recorded in-zone coordinate: quarantine and alert admin");
        }
        return new Decision(Action.NONE, null, remaining, "Carrying");
    }
    /** ACK of successful world placement; caller checks surface, claim rights and actual BlockState first. */
    public void placeStationary(Position current) {
        if (mode != Mode.CARRIED || !legal(current)) throw new IllegalStateException("Illegal placement");
        mode = Mode.STATIONARY;
        lastSafePosition = current;
    }
    public void attachToShip() {
        if (mode != Mode.STATIONARY) throw new IllegalStateException("Must be stationary first");
        mode = Mode.MOBILE;
        mobileOutsideSeconds = 0;
    }
    /** Ship position MUST be world transformed, never the local block position. */
    public Decision tickMobile(int elapsedSeconds, Position worldPosition) {
        if (mode != Mode.MOBILE) throw new IllegalStateException("Not mobile");
        if (elapsedSeconds < 0 || elapsedSeconds > 60) throw new IllegalArgumentException("Time step must be 0..60");
        if (legal(worldPosition)) {
            lastSafePosition = worldPosition;
            int recoveryTotal = elapsedSeconds + recoveryRemainderSeconds;
            mobileOutsideSeconds = Math.max(0, mobileOutsideSeconds - recoveryTotal / 2);
            recoveryRemainderSeconds = recoveryTotal % 2;
        } else {
            recoveryRemainderSeconds = 0;
            mobileOutsideSeconds += elapsedSeconds;
        }
        int remaining = (int)Math.max(0, mobileLimitSeconds - mobileOutsideSeconds);
        if (remaining == 0) {
            mode = Mode.DESTROYED;
            return new Decision(Action.DETONATE, worldPosition, 0, "Mobile core exceeded out-of-zone budget");
        }
        return new Decision(mobileOutsideSeconds > 0 ? Action.WARNING : Action.NONE,
                null, remaining, "Mobile base zone check");
    }
    public void destroy() { mode = Mode.DESTROYED; }
}
