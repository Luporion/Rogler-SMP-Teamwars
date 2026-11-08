package dev.rogler.teamwars.neoforge.map.client;

import dev.rogler.teamwars.core.HudPositionPolicy;
import dev.rogler.teamwars.core.HudPreferences;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** A local-only drag-and-drop HUD positioning preview. No client packet is sent. */
public final class HudPositionScreen extends Screen {
    private final Screen parent;
    private HudPreferences original, draft;
    private boolean dragging;
    private int grabX, grabY;
    private static final int PREVIEW_HEIGHT = 62;

    public HudPositionScreen(Screen parent) {
        super(Component.literal("Rogler Core - HUD verschieben"));
        this.parent = parent;
    }
    @Override protected void init() {
        super.init();
        if (draft == null) { original = HudSettingsStore.get(); draft = original; }
        rebuild();
    }
    private int previewWidth() { return Math.min(184, Math.max(80, width - 16)); }
    private HudPositionPolicy.Point pos() {
        return HudPositionPolicy.origin(draft.anchor(), width, height,
            previewWidth(), PREVIEW_HEIGHT, draft.offsetX(), draft.offsetY());
    }
    private void place(int px, int py) {
        var offsets = HudPositionPolicy.offsets(draft.anchor(), width, height,
            previewWidth(), PREVIEW_HEIGHT, px, py);
        draft = draft.withOffset(offsets.x(), offsets.y());
    }
    private void anchor(HudPreferences.Anchor next) {
        var old = pos();
        draft = draft.withAnchor(next);
        place(old.x(), old.y()); // Changing the anchor must not teleport the preview.
        rebuild();
    }
    private void button(String label, int x, int y, int w, Runnable action) {
        addRenderableWidget(Button.builder(Component.literal(label), b -> action.run())
            .bounds(x, y, w, 19).build());
    }
    private void rebuild() {
        clearWidgets();
        int top = height - 50;
        int fullWidth = Math.min(width - 12, 440);
        int cellWidth = Math.max(35, (fullWidth - 12) / 4);
        int left = (width - (cellWidth * 4 + 12)) / 2;
        button("Oben links", left, top, cellWidth, () -> anchor(HudPreferences.Anchor.TOP_LEFT));
        button("Oben rechts", left + cellWidth + 4, top, cellWidth, () -> anchor(HudPreferences.Anchor.TOP_RIGHT));
        button("Unten links", left + 2*(cellWidth + 4), top, cellWidth, () -> anchor(HudPreferences.Anchor.BOTTOM_LEFT));
        button("Unten rechts", left + 3*(cellWidth + 4), top, cellWidth, () -> anchor(HudPreferences.Anchor.BOTTOM_RIGHT));
        button("Speichern", left, top + 24, cellWidth*2+4, () -> {
            HudSettingsStore.update(draft); minecraft.setScreen(parent);
        });
        button("Abbrechen", left + 2*(cellWidth+4), top + 24, cellWidth*2+4, () -> {
            draft = original; minecraft.setScreen(parent);
        });
    }
    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Widgets always get first refusal, so dragging cannot accidentally click Save.
        if (super.mouseClicked(mouseX, mouseY, button)) return true;
        var p = pos();
        if (button == 0 && mouseX >= p.x() && mouseX <= p.x()+previewWidth()
            && mouseY >= p.y() && mouseY <= p.y()+PREVIEW_HEIGHT) {
            dragging = true;
            grabX = (int)mouseX - p.x(); grabY = (int)mouseY - p.y();
            return true;
        }
        return false;
    }
    @Override public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (dragging && button == 0) {
            place((int)mouseX - grabX, (int)mouseY - grabY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }
    @Override public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (dragging && button == 0) { dragging = false; return true; }
        return super.mouseReleased(mouseX, mouseY, button);
    }
    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        g.fill(0,0,width,height,0xCC09111B);
        var p = pos();
        int w = previewWidth();
        g.fill(p.x(), p.y(), p.x()+w, p.y()+PREVIEW_HEIGHT, 0xBB172A39);
        g.fill(p.x(), p.y(), p.x()+w, p.y()+2, 0xFF5BD4D5);
        int tx=p.x()+6, ty=p.y()+8;
        g.drawString(font,"TEAM // HUD",tx,ty,0xFF5BD4D5,false);
        g.drawString(font,"Worldspawn: 420m",tx,ty+13,0xFF72E6AB,false);
        g.drawString(font,"Basis: 180m",tx,ty+26,0xFFF1CA71,false);
        g.drawString(font,"Teammitglied: 95m",tx,ty+39,0xFF76C7F3,false);
        g.drawCenteredString(font,"Vorschau ziehen | Anker: "+draft.anchor(),width/2,14,0xFFE5EEF0);
        g.drawCenteredString(font,"Position: "+draft.offsetX()+" / "+draft.offsetY()+"  (clientlokal)",width/2,28,0xFFA4B7C3);
        super.render(g,mouseX,mouseY,partial);
    }
    @Override public void onClose() { if (minecraft != null) minecraft.setScreen(parent); }
}
