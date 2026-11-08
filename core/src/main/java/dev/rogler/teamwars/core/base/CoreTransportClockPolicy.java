package dev.rogler.teamwars.core.base;

import java.util.Objects;
import java.util.UUID;

/**
 * Counts wall-clock seconds while a Core is ISSUED, independently of its storage location,
 * player login state, dimension or whether the server was running between checkpoints.
 * A new bearer token starts a new checkpoint; the spent transport budget stays in the ledger.
 */
public final class CoreTransportClockPolicy {
    private CoreTransportClockPolicy() {}

    public record Checkpoint(UUID token, long accountedAtMillis) {
        public Checkpoint { Objects.requireNonNull(token, "token"); }
    }
    public record Step(Checkpoint checkpoint, long elapsedSeconds) {}

    public static Step advance(Checkpoint previous, UUID currentToken, long nowMillis) {
        Objects.requireNonNull(currentToken, "currentToken");
        if (previous == null || !currentToken.equals(previous.token()))
            return new Step(new Checkpoint(currentToken, nowMillis), 0);
        // Never rewind accounted time if the system clock is moved backwards.
        if (nowMillis <= previous.accountedAtMillis()) return new Step(previous, 0);
        long seconds = (nowMillis - previous.accountedAtMillis()) / 1000L;
        if (seconds <= 0) return new Step(previous, 0);
        return new Step(new Checkpoint(currentToken, previous.accountedAtMillis() + seconds * 1000L), seconds);
    }
}
