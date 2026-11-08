package dev.rogler.teamwars.core;

/** Geometry-only conversion between anchored HUD offsets and screen pixels.
 * No Minecraft dependency and no world, team or server coordinates.
 */
public final class HudPositionPolicy {
    private HudPositionPolicy() {}
    public record Point(int x, int y) {}

    public static Point origin(HudPreferences.Anchor anchor, int screenWidth, int screenHeight,
                               int hudWidth, int hudHeight, int offsetX, int offsetY) {
        check(anchor, screenWidth, screenHeight, hudWidth, hudHeight);
        int maxX = Math.max(0, screenWidth - hudWidth);
        int maxY = Math.max(0, screenHeight - hudHeight);
        return new Point(Math.clamp(right(anchor) ? maxX - offsetX : offsetX, 0, maxX),
            Math.clamp(bottom(anchor) ? maxY - offsetY : offsetY, 0, maxY));
    }

    /** Converts a dragged top-left pixel position to the exact anchor-relative offsets.
     * Clamps to the visible screen even on unusually small GUI scales.
     */
    public static Point offsets(HudPreferences.Anchor anchor, int screenWidth, int screenHeight,
                                int hudWidth, int hudHeight, int pixelX, int pixelY) {
        check(anchor, screenWidth, screenHeight, hudWidth, hudHeight);
        int maxX = Math.max(0, screenWidth - hudWidth);
        int maxY = Math.max(0, screenHeight - hudHeight);
        int x = Math.clamp(pixelX, 0, maxX), y = Math.clamp(pixelY, 0, maxY);
        return new Point(Math.clamp(right(anchor) ? maxX - x : x, 0, 4000),
            Math.clamp(bottom(anchor) ? maxY - y : y, 0, 4000));
    }
    private static void check(HudPreferences.Anchor anchor, int screenWidth, int screenHeight,
                              int hudWidth, int hudHeight) {
        if (anchor == null || screenWidth <= 0 || screenHeight <= 0 || hudWidth <= 0 || hudHeight <= 0)
            throw new IllegalArgumentException("Invalid HUD geometry");
    }
    private static boolean right(HudPreferences.Anchor anchor) {
        return anchor == HudPreferences.Anchor.TOP_RIGHT || anchor == HudPreferences.Anchor.BOTTOM_RIGHT;
    }
    private static boolean bottom(HudPreferences.Anchor anchor) {
        return anchor == HudPreferences.Anchor.BOTTOM_LEFT || anchor == HudPreferences.Anchor.BOTTOM_RIGHT;
    }
}
