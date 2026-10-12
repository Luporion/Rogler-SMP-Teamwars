package dev.rogler.teamwars.core.base;

import dev.rogler.teamwars.core.BaseZone;
import java.util.UUID;

public final class BaseCoreLedgerTests {
    private static int checks;
    private static void yes(boolean ok) { checks++; if (!ok) throw new AssertionError("Check " + checks); }
    private static void reject(Runnable action) {
        checks++; try { action.run(); throw new AssertionError("Expected rejection at " + checks); }
        catch (IllegalArgumentException | IllegalStateException expected) { /* expected */ }
    }
    public static void main(String[] args) {
        var ledger = new BaseCoreLedger(new BaseZone(0, 0, 300, 2000), 3600);
        UUID red = UUID.randomUUID(), blue = UUID.randomUUID(), token = UUID.randomUUID();
        var base = new BaseCoreLedger.Location("minecraft:overworld", 700, 80, -600);
        var nether = new BaseCoreLedger.Location("minecraft:the_nether", 700, 80, -600);
        ledger.issue(red, token);
        yes(ledger.size() == 1);
        reject(() -> ledger.issue(red, UUID.randomUUID()));
        reject(() -> ledger.issue(blue, token));
        yes(ledger.canPlace(red, token, base));
        yes(!ledger.canPlace(red, token, nether));
        yes(!ledger.canPlace(blue, token, base));
        yes(!ledger.canPlace(red, token, new BaseCoreLedger.Location("minecraft:overworld", 2500, 80, 0)));
        yes(!ledger.canPlace(red, token, new BaseCoreLedger.Location("minecraft:overworld", 299, 80, 0)));
        yes(ledger.advanceCarrier(red, token, 60, base).carriedSeconds() == 60);
        yes(base.equals(ledger.get(red).lastSafe()));
        for (int i=0;i<59;i++) ledger.advanceCarrier(red, token, 60, nether);
        yes(ledger.get(red).mode() == BaseCoreLedger.Mode.ESCROW);
        yes(ledger.get(red).carriedSeconds() == 3600);
        yes(base.equals(ledger.get(red).lastSafe()));
        yes(!ledger.canPlace(red, token, base));
        reject(() -> ledger.advanceCarrier(red, token, 1, base));
        reject(() -> ledger.reissueFromEscrow(blue, UUID.randomUUID()));
        UUID newToken = UUID.randomUUID();
        ledger.reissueFromEscrow(red, newToken);
        yes(!ledger.canPlace(red, token, base));
        yes(ledger.canPlace(red, newToken, base));
        ledger.placed(red, newToken, base);
        yes(ledger.get(red).mode() == BaseCoreLedger.Mode.PLACED);
        reject(() -> ledger.placed(red, newToken, base));
        var rebooted = new BaseCoreLedger(new BaseZone(0,0,300,2000), 3600);
        rebooted.restore(ledger.get(red));
        yes(rebooted.get(red).location().equals(base));
        reject(() -> rebooted.restore(ledger.get(red)));
        reject(() -> rebooted.restore(new BaseCoreLedger.Entry(blue, UUID.randomUUID(), BaseCoreLedger.Mode.PLACED,
            0, new BaseCoreLedger.Location("minecraft:the_nether", 0, 70, 0), null)));
        yes(new BaseCoreLedger(new BaseZone(0,0,300,2000),3600).legal(new BaseCoreLedger.Location("minecraft:overworld",300,70,0)));
        yes(new BaseCoreLedger(new BaseZone(0,0,300,2000),3600).legal(new BaseCoreLedger.Location("minecraft:overworld",2000,70,0)));
        // v0.2.1: clear reasons; min 0, max 5000; accidental destruction and old-token revocation.
        var migrated = new BaseCoreLedger(new BaseZone(0, 0, 0, 5000), 3600);
        UUID yellow = UUID.randomUUID(), yellowToken = UUID.randomUUID();
        var atSpawn = new BaseCoreLedger.Location("minecraft:overworld", 0, 70, 0);
        var atBorder = new BaseCoreLedger.Location("minecraft:overworld", 5000, 70, 0);
        migrated.issue(yellow, yellowToken);
        yes(migrated.canPlace(yellow, yellowToken, atSpawn));
        yes(migrated.canPlace(yellow, yellowToken, atBorder));
        yes(!migrated.canPlace(yellow, yellowToken, new BaseCoreLedger.Location("minecraft:overworld",5001,70,0)));
        yes(migrated.placementFailure(yellow, yellowToken,
            new BaseCoreLedger.Location("minecraft:overworld",5001,70,0)) == BaseCoreLedger.PlacementFailure.TOO_FAR_FROM_SPAWN);
        yes(migrated.placementFailure(yellow, yellowToken, nether) == BaseCoreLedger.PlacementFailure.WRONG_DIMENSION);
        yes(migrated.placementFailure(yellow, UUID.randomUUID(), atSpawn) == BaseCoreLedger.PlacementFailure.TOKEN_MISMATCH);
        var lastValid = new BaseCoreLedger.Location("minecraft:overworld", 60, 70, 50);
        migrated.advanceCarrier(yellow, yellowToken, 30, lastValid);
        yes(migrated.rescueToEscrow(yellow, yellowToken, null));
        yes(migrated.get(yellow).mode() == BaseCoreLedger.Mode.ESCROW);
        yes(lastValid.equals(migrated.get(yellow).lastSafe()));
        yes(!migrated.canPlace(yellow, yellowToken, lastValid));
        yes(!migrated.rescueToEscrow(yellow, yellowToken, lastValid));
        var restoredEscrow = new BaseCoreLedger(new BaseZone(0,0,0,5000),3600);
        restoredEscrow.restore(migrated.get(yellow));
        yes(restoredEscrow.get(yellow).mode() == BaseCoreLedger.Mode.ESCROW);
        UUID replacement = UUID.randomUUID();
        restoredEscrow.reissueFromEscrow(yellow,replacement);
        yes(!restoredEscrow.canPlace(yellow,yellowToken,atSpawn));
        yes(restoredEscrow.canPlace(yellow,replacement,atSpawn));
        var inner = new BaseCoreLedger(new BaseZone(0, 0, 300, 5000), 3600);
        UUID x = UUID.randomUUID(), t = UUID.randomUUID();
        inner.issue(x,t);
        yes(inner.placementFailure(x,t,atSpawn) == BaseCoreLedger.PlacementFailure.TOO_CLOSE_TO_SPAWN);
        yes(!inner.rescueToEscrow(x, UUID.randomUUID(), null));
        yes(inner.get(x).mode() == BaseCoreLedger.Mode.ISSUED);
        // v0.2.2: safe relocation, token invalidation, time preservation, persistence.
        var moving = new BaseCoreLedger(new BaseZone(0, 0, 0, 5000), 3600);
        UUID mover = UUID.randomUUID(), first = UUID.randomUUID(), next = UUID.randomUUID();
        moving.issue(mover, first);
        moving.advanceCarrier(mover, first, 60, base);
        moving.advanceCarrier(mover, first, 15, base);
        moving.placed(mover, first, base);
        reject(() -> moving.pack(blue, first, base, UUID.randomUUID()));
        reject(() -> moving.pack(mover, UUID.randomUUID(), base, UUID.randomUUID()));
        reject(() -> moving.pack(mover, first, atSpawn, UUID.randomUUID()));
        reject(() -> moving.pack(mover, first, base, first));
        var moved = moving.pack(mover, first, base, next);
        yes(moved.mode() == BaseCoreLedger.Mode.ISSUED);
        yes(moved.location() == null);
        yes(moved.carriedSeconds() == 75);
        yes(base.equals(moved.lastSafe()));
        yes(!moving.canPlace(mover, first, atBorder));
        yes(moving.canPlace(mover, next, atBorder));
        reject(() -> moving.pack(mover, first, base, UUID.randomUUID()));
        var movedReboot = new BaseCoreLedger(new BaseZone(0,0,0,5000), 3600);
        movedReboot.restore(moving.get(mover));
        yes(movedReboot.get(mover).token().equals(next));
        movedReboot.placed(mover, next, atBorder);
        yes(movedReboot.get(mover).mode() == BaseCoreLedger.Mode.PLACED);
        reject(() -> movedReboot.rescueMissingPlaced(mover, first, atBorder));
        reject(() -> movedReboot.rescueMissingPlaced(mover, next, base));
        yes(movedReboot.rescueMissingPlaced(mover, next, atBorder).mode() == BaseCoreLedger.Mode.ESCROW);
        yes(atBorder.equals(movedReboot.get(mover).lastSafe()));
        reject(() -> movedReboot.rescueMissingPlaced(mover, next, atBorder));
        UUID rescued = UUID.randomUUID();
        movedReboot.reissueFromEscrow(mover, rescued);
        yes(!movedReboot.canPlace(mover, next, base));
        yes(movedReboot.canPlace(mover, rescued, base));
        yes(movedReboot.get(mover).carriedSeconds() == 75);
        // Recovering after full transport expiry gives one last placement tick, NOT 3600 seconds.
        var expired=new BaseCoreLedger(new BaseZone(0,0,0,5000),3600);
        UUID expiredTeam=UUID.randomUUID(), expiredToken=UUID.randomUUID();
        expired.issue(expiredTeam,expiredToken);
        expired.advanceCarrier(expiredTeam,expiredToken,60,base);
        for(int n=0;n<59;n++) expired.advanceCarrier(expiredTeam,expiredToken,60,base);
        yes(expired.get(expiredTeam).mode()==BaseCoreLedger.Mode.ESCROW);
        UUID recoveredToken=UUID.randomUUID();
        expired.reissueFromEscrow(expiredTeam,recoveredToken);
        yes(expired.get(expiredTeam).carriedSeconds()==3599);
        yes(expired.canPlace(expiredTeam,recoveredToken,base));
        yes(!expired.canPlace(expiredTeam,expiredToken,base));
        System.out.println("v0.3.0 BaseCoreLedgerTests: " + checks + " checks OK");
    }
}
