package dev.rogler.teamwars.neoforge.client;

import dev.rogler.teamwars.neoforge.base.CoreDashboardMenu;
import dev.rogler.teamwars.neoforge.base.CoreGuideService;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Compact industrial hub. Drawn with vanilla GuiGraphics, no external UI library.
 * The real 27+36 inventory slots are always visible; pages replace chat walls.
 * All state-changing actions go through server-validated menu buttons.
 */
public final class CoreDashboardScreen extends AbstractContainerScreen<CoreDashboardMenu> {
    private static final int BACK=0xFF111B25, PANEL=0xFF1D2B36, EDGE=0xFF3A5160,
        TEXT=0xFFE4ECED, MUTED=0xFF9EAEB8, COPPER=0xFFEAA74F,
        CYAN=0xFF5CD3DC, GREEN=0xFF68CE98, RED=0xFFE18779;
    private static final String[] TABS={"STATUS","UPGRADES","ENERGIE","GUIDE","ZUKUNFT"};
    private int tab=0;
    private int bookIndex=0;

    public CoreDashboardScreen(CoreDashboardMenu menu,Inventory inventory,Component title){
        super(menu,inventory,title);
        imageWidth=270;
        imageHeight=304;
        inventoryLabelY=210;
    }
    @Override protected void init(){
        super.init();
        buildControls();
    }
    private void selectTab(int i){
        tab=i;
        clearWidgets();
        buildControls();
    }
    private void action(int id){
        var mc=Minecraft.getInstance();
        if(mc.gameMode!=null)mc.gameMode.handleInventoryButtonClick(menu.containerId,id);
    }
    private void button(String label,int x,int y,int w,int h,Runnable click){
        addRenderableWidget(Button.builder(Component.literal(label),b->click.run()).bounds(leftPos+x,topPos+y,w,h).build());
    }
    private void buildControls(){
        for(int i=0;i<TABS.length;i++){
            final int idx=i;
            button(TABS[i],8+i*51,25,49,20,()->selectTab(idx));
        }
        switch(tab){
            case 1 -> {
                button("Ruestung",10,123,80,19,()->action(CoreDashboardMenu.BUY_ARMOR));
                button("FE-Generator",95,123,80,19,()->action(CoreDashboardMenu.BUY_ENERGY));
                button("Create",180,123,80,19,()->action(CoreDashboardMenu.BUY_CREATE));
            }
            case 3 -> {
                var books=CoreGuideService.manuals();
                if(!books.isEmpty()){
                    bookIndex=Math.floorMod(bookIndex,books.size());
                    button("<",12,122,23,19,()->{bookIndex=Math.floorMod(bookIndex-1,books.size());selectTab(3);});
                    button("Buch tauschen (1 Buch)",40,122,183,19,()->action(CoreDashboardMenu.MANUAL_BASE+bookIndex));
                    button(">",228,122,29,19,()->{bookIndex=(bookIndex+1)%books.size();selectTab(3);});
                }
            }
            default -> { }
        }
    }
    private void text(GuiGraphics g,String s,int x,int y,int color){
        g.drawString(font,s,leftPos+x,topPos+y,color,false);
    }
    private void stat(GuiGraphics g,String label,String value,int x,int y,int valueColor){
        text(g,label,x,y,MUTED);
        text(g,value,x,y+12,valueColor);
    }
    private void line(GuiGraphics g,String s,int y){text(g,s,13,y,TEXT);}
    private void panel(GuiGraphics g,int x,int y,int w,int h){
        g.fill(leftPos+x,topPos+y,leftPos+x+w,topPos+y+h,EDGE);
        g.fill(leftPos+x+1,topPos+y+1,leftPos+x+w-1,topPos+y+h-1,PANEL);
    }
    private void drawPanel(GuiGraphics g){
        panel(g,8,49,254,97);
        switch(tab){
            case 0 -> {
                stat(g,"KERN-RUESTUNG",menu.value(CoreDashboardMenu.ARMOR)+" / 2",15,57,COPPER);
                stat(g,"KERN-HP",menu.value(CoreDashboardMenu.RAID_HP)+" HP",108,57,CYAN);
                stat(g,"RAID",menu.value(CoreDashboardMenu.RAID_ACTIVE)>0?"AKTIV":"GESCHUETZT",196,57,
                    menu.value(CoreDashboardMenu.RAID_ACTIVE)>0?RED:GREEN);
                stat(g,"GRACE",menu.value(CoreDashboardMenu.GRACE_MIN)+" min",15,91,TEXT);
                stat(g,"FTB CLAIMS",String.valueOf(menu.value(CoreDashboardMenu.CLAIMS)),108,91,TEXT);
                stat(g,"FE-SPEICHER",menu.value(CoreDashboardMenu.FE_BUFFER)+" / 20000",196,91,CYAN);
                text(g,"Inventar und Module bleiben beim Kern-Umzug erhalten.",13,133,MUTED);
            }
            case 1 -> {
                line(g,"RUESTUNG "+menu.value(0)+"/2     FE "+menu.value(2)+"/2     CREATE "+menu.value(5)+"/1",56);
                text(g,"Naechste Kosten (aus Spielerinventar):",13,71,MUTED);
                text(g,armorCost(),13,83,TEXT);
                text(g,energyCost(),13,95,TEXT);
                text(g,menu.value(5)>0?"Create: bereits installiert":"Create: 16 Andesitlegierung + 8 Wellen",13,103,TEXT);
                int result=menu.value(CoreDashboardMenu.FEEDBACK);
                if(result!=0)text(g,result>0?"Upgrade erfolgreich - Anzeigen aktualisieren sich":"Kauf abgelehnt: Material/Team/Raid pruefen",13,114,result>0?GREEN:RED);
            }
            case 2 -> {
                stat(g,"GENERATOR",menu.value(CoreDashboardMenu.FE_RATE)+" FE/t",15,56,CYAN);
                stat(g,"PUFFER",menu.value(CoreDashboardMenu.FE_BUFFER)+" FE",112,56,COPPER);
                stat(g,"FE-ANSCHLUESSE",String.valueOf(menu.value(CoreDashboardMenu.FE_NEIGHBORS)),209,56,GREEN);
                line(g,"AE2 Energy Acceptor direkt neben dem Kern:",91);
                text(g,menu.value(CoreDashboardMenu.AE2_ACCEPTORS)>0?"AE2-Block erkannt; FE-Eingang separat pruefen":"NICHT ERKANNT (direkt angrenzend platzieren)",13,105,
                    menu.value(CoreDashboardMenu.AE2_ACCEPTORS)>0?GREEN:MUTED);
                int mask=menu.value(CoreDashboardMenu.FE_SIDES);
                int ae2mask=menu.value(CoreDashboardMenu.AE2_SIDES);
                text(g,"FE-EINGAENGE (Seiten)  AE2: orange",13,117,MUTED);
                String[] sides={"UNT","OB","N","S","W","O"};
                // Direction order: DOWN, UP, NORTH, SOUTH, WEST, EAST.
                for(int i=0;i<6;i++){
                    boolean fe=(mask&(1<<i))!=0, ae=(ae2mask&(1<<i))!=0;
                    text(g,sides[i]+":"+(fe?"OK":"--"),13+i*40,131,ae?COPPER:(fe?GREEN:MUTED));
                }
            }
            case 3 -> {
                line(g,"CREATE: Maus ueber Create-Item halten und W druecken.",56);
                line(g,"AE2: W auf AE2-Item oder ? im AE2-Menue.",68);
                line(g,"JEI / EMI: Rezepte und Verwendungen ansehen.",80);
                var manuals=CoreGuideService.manuals();
                if(manuals.isEmpty())text(g,"Keine bekannten echten Handbuecher gefunden.",13,101,MUTED);
                else {
                    text(g,"Handbuch "+(bookIndex+1)+"/"+manuals.size()+": "+manuals.get(Math.floorMod(bookIndex,manuals.size())).getKey(),13,99,COPPER);
                    text(g,manuals.get(Math.floorMod(bookIndex,manuals.size())).getValue().toString(),13,110,MUTED);
                    // Payment requirement is also stated on the button.
                }
            }
            case 4 -> {
                line(g,"MODULPLATZ - GEPLANTE ERWEITERUNGEN",57);
                line(g,"Sophisticated: Inventar-Upgrades / Filter",72);
                line(g,"AE2: ME-Terminal + Drive (nicht implementiert)",84);
                line(g,"Radar / Pflanzenwachstum / Chunkloading",96);
                line(g,"Xaero Teamkarte / Voice / Teamhandel / Events",108);
                text(g,"Noch keine Kauefe oder Zusagen fuer diese Module.",13,132,MUTED);
            }
            default -> { }
        }
    }
    private String armorCost(){
        return switch(menu.value(0)){
            case 0 -> "Ruestung 1: 8 Diamanten";
            case 1 -> "Ruestung 2: 16 Diamanten + 1 Netherite";
            default -> "Ruestung: MAX";
        };
    }
    private String energyCost(){
        return switch(menu.value(2)){
            case 0 -> "FE 1: 16 Redstone + 8 Eisen";
            case 1 -> "FE 2: 32 Redstone + 8 Gold";
            default -> "FE: MAX";
        };
    }
    @Override protected void renderBg(GuiGraphics g,float delta,int mouseX,int mouseY){
        // No external texture pack is needed; UI remains legible in any resource pack.
        g.fill(leftPos,topPos,leftPos+imageWidth,topPos+imageHeight,EDGE);
        g.fill(leftPos+2,topPos+2,leftPos+imageWidth-2,topPos+imageHeight-2,BACK);
        g.fill(leftPos+7,topPos+6,leftPos+263,topPos+21,0xFF253946);
        text(g,"ROGLER  //  BASIS-KERN",13,10,COPPER);
        drawPanel(g);
        text(g,"TEAM-INVENTAR (27)",51,144,CYAN);
        text(g,"SPIELERINVENTAR",51,210,MUTED);
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)slotBg(g,51+col*18,153+row*18);
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)slotBg(g,51+col*18,221+row*18);
        for(int col=0;col<9;col++)slotBg(g,51+col*18,279);
        text(g,"v"+dev.rogler.teamwars.neoforge.RoglerTeamWarsMod.VERSION,213,300,MUTED);
    }
    private void slotBg(GuiGraphics g,int x,int y){
        g.fill(leftPos+x-1,topPos+y-1,leftPos+x+17,topPos+y+17,0xFF485967);
        g.fill(leftPos+x,topPos+y,leftPos+x+16,topPos+y+16,0xFF15222D);
    }
    @Override protected void renderLabels(GuiGraphics g,int mouseX,int mouseY) { /* Drawn in renderBg */ }
    @Override public void render(GuiGraphics g,int mouseX,int mouseY,float delta){
        super.render(g,mouseX,mouseY,delta);
        renderTooltip(g,mouseX,mouseY);
    }
}
