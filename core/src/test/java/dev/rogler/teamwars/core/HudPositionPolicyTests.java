package dev.rogler.teamwars.core;

/** Regression checks for no-clipping, all anchors, inverse mapping and local-only data. */
public final class HudPositionPolicyTests {
    private static int checks;
    private static void check(boolean ok) { if (!ok) throw new AssertionError("HUD position check "+checks); checks++; }
    public static void main(String[] args) {
        for (var anchor : HudPreferences.Anchor.values()) {
            for (int ox : new int[]{0, 12, 40, 800}) {
                for (int oy : new int[]{0, 24, 60, 1200}) {
                    var pos = HudPositionPolicy.origin(anchor, 450, 300, 128, 67, ox, oy);
                    check(pos.x() >= 0 && pos.x() <= 322);
                    check(pos.y() >= 0 && pos.y() <= 233);
                    var off = HudPositionPolicy.offsets(anchor, 450, 300, 128, 67, pos.x(), pos.y());
                    var roundTrip = HudPositionPolicy.origin(anchor, 450, 300, 128, 67, off.x(), off.y());
                    check(roundTrip.equals(pos));
                }
            }
            var far = HudPositionPolicy.offsets(anchor, 70, 45, 180, 80, 500, -900);
            var tiny = HudPositionPolicy.origin(anchor, 70, 45, 180, 80, far.x(), far.y());
            check(tiny.equals(new HudPositionPolicy.Point(0, 0)));
            var dragged = HudPositionPolicy.offsets(anchor, 450, 300, 128, 67, -20, 999);
            var visible = HudPositionPolicy.origin(anchor, 450, 300, 128, 67, dragged.x(), dragged.y());
            check(visible.equals(new HudPositionPolicy.Point(0, 233)));
        }
        for (var a : HudPreferences.Anchor.values()) {
            for (var b : HudPreferences.Anchor.values()) {
                var p = HudPositionPolicy.origin(a, 450, 300, 128, 67, 17, 31);
                var shifted = HudPositionPolicy.offsets(b, 450, 300, 128, 67, p.x(), p.y());
                check(HudPositionPolicy.origin(b, 450, 300, 128, 67, shifted.x(), shifted.y()).equals(p));
            }
        }
        for (var a : HudPreferences.Anchor.values()) {
            try { HudPositionPolicy.origin(a, 0, 100, 50, 25, 2, 2); throw new AssertionError("Accepted invalid geometry"); }
            catch (IllegalArgumentException expected) { checks++; }
        }
        System.out.println("v0.8.5 HudPositionPolicyTests: "+checks+" checks OK");
    }
}
