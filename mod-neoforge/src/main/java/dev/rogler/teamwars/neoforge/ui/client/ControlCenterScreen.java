package dev.rogler.teamwars.neoforge.ui.client;

import dev.rogler.teamwars.core.HudPreferences;
import dev.rogler.teamwars.core.ui.ControlCenterPolicy;
import dev.rogler.teamwars.neoforge.map.client.HudSettingsStore;
import dev.rogler.teamwars.neoforge.map.client.HudSettingsScreen;
import dev.rogler.teamwars.neoforge.map.client.WaypointSettingsScreen;
import dev.rogler.teamwars.neoforge.map.client.WaypointClientCommands;
import dev.rogler.teamwars.neoforge.map.client.WaypointLinkClient;
import dev.rogler.teamwars.neoforge.map.client.LocalWaypointBridge;
import dev.rogler.teamwars.neoforge.ui.ControlCenterStatePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;
import java.util.UUID;

/** One native, dependency-free, permission-aware entry point for all existing Rogler features. */
public final class ControlCenterScreen extends Screen {
    private enum Page { START, HUD, WAYPOINTS, LINKS, TEAM, CORE, ADMIN_TEAMS, ADMIN_BASE,
        ADMIN_PROTECTION, ADMIN_POWER, ADMIN_COMPAT, ADMIN_ACTIONS, ADMIN_DIAG }
    private static final int BACK=0xFF111B25, PANEL=0xFF1C2B37, ACCENT=0xFF5BD4D5,
        TEXT=0xFFE5EEF0, MUTED=0xFFA4B7C3, WARNING=0xFFFFC27A;
    private Page page=Page.START;
    private int firstSetting=0,firstAction=0,firstNav=0;
    private String compatGroup="warium";
    private String note="Einstellungen werden direkt gespeichert; bestehende Minecraft-Berechtigungen gelten.";
    private final Map<String,EditBox> fields=new LinkedHashMap<>();
    private String targetValue="", extraValue="", minutesValue="10", tokenValue="", indexValue="0", index2Value="0";
    private int x,y,bodyWidth;
    private ControlCenterStatePayload state;

    public ControlCenterScreen(){super(Component.literal("Rogler Core – Control Center"));}
    public void refreshState(){
        state=ControlCenterClient.latest();
        if(state!=null){note=state.message();rebuild();}
    }
    @Override protected void init(){super.init();state=ControlCenterClient.latest();rebuild();}
    private static String text(String value,int limit){return value.length()>limit?value.substring(0,limit-2)+"..":value;}
    private void button(String label,int xx,int yy,int ww,Runnable run){button(label,xx,yy,ww,run,null);}
    private void button(String label,int xx,int yy,int ww,Runnable run,String help){
        if(ww<16)return;
        var builder=Button.builder(Component.literal(label),b->run.run()).bounds(xx,yy,ww,19);
        if(help!=null)builder.tooltip(Tooltip.create(Component.literal(help)));
        addRenderableWidget(builder.build());
    }
    private void page(Page to){saveInputs();page=to;firstSetting=0;firstAction=0;rebuild();}
    private boolean admin(){return state!=null&&state.permission()>=2;}
    private void rebuild(){
        clearWidgets();fields.clear();
        int panelWidth=Math.min(670,width-14),left=(width-panelWidth)/2;
        x=left+145;bodyWidth=Math.max(156,panelWidth-158);y=46;
        int xx=left+7, navY=48;
        var pages=java.util.Arrays.stream(Page.values())
            .filter(p->!p.name().startsWith("ADMIN")||admin()).toList();
        int navVisible=Math.max(4,(height-132)/21);
        firstNav=Math.clamp(firstNav,0,Math.max(0,pages.size()-navVisible));
        for(int i=firstNav;i<Math.min(firstNav+navVisible,pages.size());i++){
            Page item=pages.get(i);final Page pageValue=item;
            button((item==page?"> ":"")+name(item),xx,navY,131,()->page(pageValue));navY+=21;
        }
        if(pages.size()>navVisible){
            button("< Menue",xx,Math.max(navY+3,height-86),63,()->{firstNav=Math.max(0,firstNav-navVisible);rebuild();});
            button("Menue >",xx+68,Math.max(navY+3,height-86),63,()->{firstNav=Math.min(pages.size()-navVisible,firstNav+navVisible);rebuild();});
        }
        switch(page){
            case START -> home();
            case HUD -> hud();
            case WAYPOINTS -> waypoints();
            case LINKS -> links();
            case TEAM -> team();
            case CORE -> core();
            case ADMIN_TEAMS -> settings("teams");
            case ADMIN_BASE -> settings("base");
            case ADMIN_PROTECTION -> settings("protection");
            case ADMIN_POWER -> settings("power");
            case ADMIN_COMPAT -> settingsCompat();
            case ADMIN_ACTIONS -> actions(false);
            case ADMIN_DIAG -> actions(true);
        }
        button("Aktualisieren",x,Math.max(y+8,height-48),100,()->ControlCenterClient.request("view","",""));
        button("Schliessen",x+Math.max(110,bodyWidth-100),Math.max(y+8,height-48),100,this::onClose);
    }
    private String name(Page item){return switch(item){
        case START -> "Uebersicht";case HUD -> "HUD & Navigation";case WAYPOINTS -> "Wegpunkte";
        case LINKS -> "Waypoint-Links";case TEAM -> "Mein Team";case CORE -> "Mein Base-Core";
        case ADMIN_TEAMS -> "Admin: Teams";case ADMIN_BASE -> "Admin: Basis";
        case ADMIN_PROTECTION -> "Admin: Schutz";case ADMIN_POWER -> "Admin: Energie";
        case ADMIN_COMPAT -> "Admin: Kompat";
        case ADMIN_ACTIONS -> "Admin: Aktionen";case ADMIN_DIAG -> "Admin: Diagnose";
    };}
    private void home(){
        button("HUD-Einstellungen",x,y,bodyWidth,()->page(Page.HUD));y+=23;
        button("Wegpunkte & Teamfreigaben",x,y,bodyWidth,()->page(Page.WAYPOINTS));y+=23;
        button("Teamstatus",x,y,bodyWidth,()->page(Page.TEAM));y+=23;
        button("Core-Dashboard oeffnen",x,y,bodyWidth,()->sendCommand("teamwars base menu"));y+=23;
        if(admin()){button("Server-Administration",x,y,bodyWidth,()->page(Page.ADMIN_TEAMS));y+=23;}
        button("Serverstatus (Befehl)",x,y,bodyWidth,()->sendCommand("teamwars status"));y+=23;
    }
    private void hud(){
        HudPreferences p=HudSettingsStore.get();int half=Math.max(70,(bodyWidth-5)/2);
        button("HUD: "+enabled(p.enabled()),x,y,bodyWidth,()->hudSet(p.withEnabled(!p.enabled())));y+=23;
        button("Spawn: "+enabled(p.worldSpawn()),x,y,half,()->hudSet(p.withSection("world_spawn",!p.worldSpawn())));
        button("Respawn: "+enabled(p.respawn()),x+half+5,y,half,()->hudSet(p.withSection("respawn",!p.respawn())));y+=23;
        button("Core: "+enabled(p.baseDistance()),x,y,half,()->hudSet(p.withSection("base_distance",!p.baseDistance())));
        button("Teammates: "+enabled(p.teammates()),x+half+5,y,half,()->hudSet(p.withSection("teammates",!p.teammates())));y+=23;
        button("Teammarker: "+enabled(p.teammateMarkers()),x,y,half,()->hudSet(p.withSection("team_markers",!p.teammateMarkers())));
        button("Basismarker: "+enabled(p.baseMarker()),x+half+5,y,half,()->hudSet(p.withSection("base_marker",!p.baseMarker())));y+=23;
        button("FTB-Mirror: "+enabled(p.ftbWaypointMirror()),x,y,bodyWidth,()->hudSet(p.withSection("ftb_waypoints",!p.ftbWaypointMirror())));y+=23;
        button("Position: "+p.anchor(),x,y,bodyWidth,()->hudSet(p.withAnchor(
            HudPreferences.Anchor.values()[(p.anchor().ordinal()+1)%4])));y+=23;
        button("X -8",x,y,(bodyWidth-9)/4,()->hudSet(p.withOffset(p.offsetX()-8,p.offsetY())));
        button("X +8",x+((bodyWidth+3)/4),y,(bodyWidth-9)/4,()->hudSet(p.withOffset(p.offsetX()+8,p.offsetY())));
        button("Y -8",x+2*((bodyWidth+3)/4),y,(bodyWidth-9)/4,()->hudSet(p.withOffset(p.offsetX(),p.offsetY()-8)));
        button("Y +8",x+3*((bodyWidth+3)/4),y,(bodyWidth-9)/4,()->hudSet(p.withOffset(p.offsetX(),p.offsetY()+8)));y+=23;
        button("Erweiterte HUD-Ansicht",x,y,bodyWidth,()->minecraft.setScreen(new HudSettingsScreen(this)));
    }
    private void hudSet(HudPreferences p){HudSettingsStore.update(p);rebuild();}
    private static String enabled(boolean b){return b?"AN":"AUS";}
    private void waypoints(){
        button("Wegpunktliste & Team-Sharing",x,y,bodyWidth,()->minecraft.setScreen(new WaypointSettingsScreen(this)));y+=23;
        button("FTB / Xaero: Neue importieren",x,y,bodyWidth,()->runLocal(()->LocalWaypointBridge.syncPrivate()));y+=23;
        button("Namens-/Farbkonflikte pruefen",x,y,bodyWidth,()->{WaypointClientCommands.preview();note="Konfliktvorschau im Chat";});y+=23;
        button("Verknuepfte Punkte vergleichen",x,y,bodyWidth,()->runLocal(()->{WaypointLinkClient.showChanges();return "Linkdetails im Chat";}));y+=23;
        button("Eindeutige Metadaten synchronisieren",x,y,bodyWidth,()->runLocal(WaypointLinkClient::syncLinkedMetadata));y+=23;
        button("Auto Name/Farbe: "+enabled(WaypointLinkClient.autoMetadataEnabled()),x,y,bodyWidth,
            ()->{note=WaypointLinkClient.setAutoMetadata(!WaypointLinkClient.autoMetadataEnabled());rebuild();});y+=23;
        button("Verknuepfungen verwalten",x,y,bodyWidth,()->page(Page.LINKS));y+=23;
    }
    private interface LocalAction {String run() throws Exception;}
    private void runLocal(LocalAction action){try{note=action.run();}catch(Exception failure){note="Aktion abgebrochen: "+failure.getMessage();}rebuild();}
    private EditBox input(String key,String value,int xx,int yy,int ww,int max){
        EditBox field=new EditBox(font,xx,yy,ww,19,Component.literal(key));
        field.setMaxLength(max);field.setValue(value);addRenderableWidget(field);fields.put(key,field);return field;
    }
    private String val(String key,String defaultValue){return fields.containsKey(key)?fields.get(key).getValue():defaultValue;}
    private void saveInputs(){
        targetValue=val("target",targetValue);extraValue=val("extra",extraValue);
        minutesValue=val("minutes",minutesValue);tokenValue=val("token",tokenValue);
        indexValue=val("index",indexValue);index2Value=val("index2",index2Value);
    }
    private void links(){
        input("index",indexValue,x,y,(bodyWidth-6)/2,8);input("index2",index2Value,x+(bodyWidth+6)/2,y,(bodyWidth-6)/2,8);y+=23;
        button("FTB-Index + Xaero-Index verknuepfen",x,y,bodyWidth,()->runLocal(()->{
            saveInputs();return WaypointLinkClient.link(Integer.parseInt(indexValue),Integer.parseInt(index2Value));}));y+=26;
        input("target",targetValue,x,y,bodyWidth,36);y+=23;
        input("token",tokenValue,x,y,bodyWidth,80);y+=23;
        button("Aenderung uebernehmen",x,y,bodyWidth,()->linkAction("apply"));y+=22;
        button("Gleichen Zustand bestaetigen",x,y,bodyWidth,()->linkAction("acknowledge"));y+=22;
        button("Link nur lokal aufheben",x,y,bodyWidth,()->linkAction("unlink"));y+=22;
        button("Gegenstelle loeschen (mit Bestaetigung)",x,y,bodyWidth,()->linkAction("delete"));y+=22;
        button("Alle Aenderungen im Chat ansehen",x,y,bodyWidth,()->runLocal(()->{WaypointLinkClient.showChanges();return "Status im Chat";}));y+=22;
        button("Move: FTB-Quelle",x,y,(bodyWidth-6)/2,()->linkAction("move-ftb"));
        button("Move: Xaero-Quelle",x+(bodyWidth+6)/2,y,(bodyWidth-6)/2,()->linkAction("move-xaero"));
    }
    private void linkAction(String action){
        saveInputs();
        Runnable execute=()->runLocal(()->{
            UUID id=UUID.fromString(targetValue);
            return switch(action){
                case "apply" -> WaypointLinkClient.apply(id,tokenValue);
                case "acknowledge" -> WaypointLinkClient.acknowledge(id,tokenValue);
                case "unlink" -> WaypointLinkClient.unlink(id);
                case "delete" -> WaypointLinkClient.confirmDeletion(id,tokenValue);
                case "move-ftb" -> WaypointLinkClient.confirmMove(id,"ftb",Integer.parseInt(indexValue),tokenValue);
                case "move-xaero" -> WaypointLinkClient.confirmMove(id,"xaero",Integer.parseInt(indexValue),tokenValue);
                default -> "Unbekannte Aktion";
            };
        });
        if(action.equals("delete"))confirm("Gegenstelle wirklich loeschen?",execute);
        else execute.run();
    }
    private void team(){
        button("FTB-Team-Status anzeigen",x,y,bodyWidth,()->sendCommand("teamwars team info"));y+=23;
        button("FTB-Teams-Berechtigungen (Hilfe)",x,y,bodyWidth,()->sendCommand("teamwars team help"));y+=23;
        button("Kriegspunkte ansehen",x,y,bodyWidth,()->sendCommand("teamwars war scores"));y+=23;
        button("Team-Marker und Wegpunkte",x,y,bodyWidth,()->page(Page.WAYPOINTS));y+=23;
        button("Meine Karte / Team-Exploration",x,y,bodyWidth,()->sendCommand("teamwars map status"));y+=23;
    }
    private void core(){
        button("Core-Inventar und Upgrades oeffnen",x,y,bodyWidth,()->sendCommand("teamwars base menu"));y+=23;
        button("Meinen Core-Status anzeigen",x,y,bodyWidth,()->sendCommand("teamwars base status"));y+=23;
        button("Basisabstand",x,y,bodyWidth,()->sendCommand("teamwars base distance"));y+=23;
        button("Schutz / Claims",x,y,bodyWidth,()->sendCommand("teamwars base protection"));y+=23;
        button("Energie und FE",x,y,bodyWidth,()->sendCommand("teamwars base energy"));y+=23;
        button("Raidstatus",x,y,bodyWidth,()->sendCommand("teamwars war status"));y+=23;
        button("Core einpacken (im erlaubten Bereich)",x,y,bodyWidth,
            ()->confirm("Basis-Core einpacken?",()->sendCommand("teamwars base pack")));y+=23;
        button("Eigenen Core wiederherstellen",x,y,bodyWidth,
            ()->confirm("Core-Wiederherstellung anfordern?",()->sendCommand("teamwars base recover")));
    }
    private void settingsCompat(){
        settings(compatGroup);
        // Compat mode pages are separately selectable via the buttons below.
        int yy=Math.min(height-75,260);
        button("Warium",x,yy,(bodyWidth-12)/3,()->{compatGroup="warium";firstSetting=0;settingsPageReset();});
        button("Metriken",x+(bodyWidth+6)/3,yy,(bodyWidth-12)/3,()->{compatGroup="sable";firstSetting=0;settingsPageReset();});
        button("Sable",x+2*(bodyWidth+6)/3,yy,(bodyWidth-12)/3,()->{compatGroup="lifecycle";firstSetting=0;settingsPageReset();});
    }
    private void settingsPageReset(){rebuild();}
    private void settings(String group){
        if(!admin()){page(Page.START);return;}
        List<ControlCenterPolicy.Field> matching=ControlCenterPolicy.FIELDS.stream().filter(f->f.group().equals(group)).toList();
        int visible=Math.min(5,Math.max(2,(height-145)/46));
        firstSetting=Math.clamp(firstSetting,0,Math.max(0,matching.size()-visible));
        for(int i=firstSetting;i<Math.min(firstSetting+visible,matching.size());i++){
            var field=matching.get(i);String id=field.group()+"."+field.key();
            String current=state==null?"?":state.settings().getOrDefault(id,"?");
            final int yy=y;boolean editable=state!=null&&state.permission()>=field.permission();
            if(field.booleanValue()){
                button(field.label()+": "+enabled(Boolean.parseBoolean(current))+(editable?"":" [OP4]"),x,yy,bodyWidth,
                    ()->{if(editable)settingSave(field,Boolean.toString(!Boolean.parseBoolean(current)));},field.description());
            }else{
                button(text(field.label(),22),x,yy,Math.max(95,bodyWidth-140),()->{},field.description());
                input(id,current,x+Math.max(100,bodyWidth-135),yy,60,
                    32);
                button(editable?"Speichern":"OP4",x+bodyWidth-70,yy,70,
                    ()->{if(editable)settingSave(field,val(id,current));});
            }
            y+=42;
        }
        if(matching.size()>visible){
            button("<",x,y,42,()->{firstSetting=Math.max(0,firstSetting-visible);rebuild();});
            button(">",x+bodyWidth-42,y,42,()->{firstSetting=Math.min(matching.size()-visible,firstSetting+visible);rebuild();});y+=22;
        }
        if(group.equals("teams") && note.startsWith("Einstellungen werden"))
            note="Teamgroesse/Anzahl sind nur Rogler-Beobachtungswerte. FTB-Teamlimits separat konfigurieren.";
    }
    private void settingSave(ControlCenterPolicy.Field field,String value){
        Runnable apply=()->{
            try{ControlCenterClient.request("set",field.group()+"."+field.key(),field.normalize(value));note="Server prueft: "+field.label();}
            catch(Exception e){note="Ungueltiger Wert: "+e.getMessage();}
        };
        if(ControlCenterPolicy.sensitive(field))confirm("Kritische Einstellung aendern: "+field.label()+"?",apply);
        else apply.run();
    }
    private record AdminAction(String label,String command,String argument,boolean dangerous,int permission) {}
    private static final List<AdminAction> ACTIONS=List.of(
        new AdminAction("Alle Teams","teams","",false,2),
        new AdminAction("Alle registrierten Cores","corelist","",false,2),
        new AdminAction("Verwaiste Cores","orphanlist","",false,2),
        new AdminAction("Team-Claim-Audit","claimaudit","TEAM",false,2),
        new AdminAction("Grace fuer Team","graceteam","TEAM_MIN",true,2),
        new AdminAction("Raid gegen Team starten","raidstartteam","TEAM_MIN",true,2),
        new AdminAction("Team-Raid beenden","raidstopteam","TEAM",true,2),
        new AdminAction("Raid gegen Spieler starten","raidstart","PLAYER_MIN",true,2),
        new AdminAction("Spieler-Raid beenden","raidstop","PLAYER",true,2),
        new AdminAction("Spieler-Grace setzen","grace","PLAYER_MIN",true,2),
        new AdminAction("FTB-Claims synchronisieren","syncclaims","PLAYER",true,2),
        new AdminAction("Core ausgeben","issuecore","PLAYER",true,2),
        new AdminAction("Core wiederherstellen","recovercore","PLAYER",true,2),
        new AdminAction("Vermissten Core retten","salvagemissing","PLAYER",true,2),
        new AdminAction("Core-Token widerrufen","reclaimlost","PLAYER",true,2),
        new AdminAction("Team-Core widerrufen","reclaimlostteam","TEAM",true,2),
        new AdminAction("Verwaisten Core zuordnen","adoptcore","TEAM_PLAYER",true,2)
    );
    private static final List<AdminAction> DIAG=List.of(
        new AdminAction("Status und FTB-Adapter","diagnostics","",false,2),
        new AdminAction("Team-Scoreboard resync","resync","",false,2),
        new AdminAction("Core-Integritaetsaudit","coreaudit","",false,2),
        new AdminAction("Core-Integritaetsreparatur","corerepair apply","",true,4),
        new AdminAction("Explosionstatistik","explosionstats","",false,4),
        new AdminAction("Core-Konfig neu laden","reload","",true,2),
        new AdminAction("Compat-Report","compat report","",false,4),
        new AdminAction("Sable-Bericht","compat sable","",false,4),
        new AdminAction("Modliste","compat mods","",false,4),
        new AdminAction("Sable-Explosionen","compat explosions","",false,4),
        new AdminAction("Sable-Lifecycle","compat lifecycle","",false,4),
        new AdminAction("Sable-Konfig neu laden","compat reload","",true,4),
        new AdminAction("Warium-Status","warium status","",false,4),
        new AdminAction("Warium-Eintraege","warium list","",false,4),
        new AdminAction("Warium-Kandidaten","warium candidates","",false,4),
        new AdminAction("Warium-Konfig neu laden","warium reload","",true,4)
    );
    private void actions(boolean diagnostics){
        if(!admin()){page(Page.START);return;}
        List<AdminAction> list=diagnostics?DIAG:ACTIONS;
        if(!diagnostics){
            // Ziel: Spielername oder FTB-Team-UUID; zweites Feld: Minuten.
            input("target",targetValue,x,y,Math.max(70,bodyWidth-85),36);input("minutes",minutesValue,x+bodyWidth-79,y,79,5);y+=23;
            input("extra",extraValue,x,y,bodyWidth,36);y+=23;
        }
        int shown=Math.max(3,(height-y-86)/22);
        firstAction=Math.clamp(firstAction,0,Math.max(0,list.size()-shown));
        for(int i=firstAction;i<Math.min(firstAction+shown,list.size());i++){
            AdminAction a=list.get(i);boolean allowed=state!=null&&state.permission()>=a.permission();
            button(allowed?a.label():a.label()+" [OP4]",x,y,bodyWidth,()->{if(allowed)adminAction(a);});y+=22;
        }
        button("<",x,y,45,()->{saveInputs();firstAction=Math.max(0,firstAction-shown);rebuild();});
        button(">",x+bodyWidth-45,y,45,()->{saveInputs();firstAction=Math.min(Math.max(0,list.size()-shown),firstAction+shown);rebuild();});
    }
    private void adminAction(AdminAction action){
        saveInputs();
        String args;
        try{
            String target=targetValue.trim(),extra=extraValue.trim(),minutes=minutesValue.trim();
            boolean player=action.argument().startsWith("PLAYER"),team=action.argument().startsWith("TEAM");
            if(player && !target.matches("[A-Za-z0-9_]{1,16}"))throw new IllegalArgumentException("Spielername fehlt oder ungueltig");
            if(team && !target.matches("[a-fA-F0-9-]{36}"))throw new IllegalArgumentException("FTB Team-UUID erforderlich");
            if(action.argument().equals("TEAM_PLAYER") && !extra.matches("[A-Za-z0-9_]{1,16}"))
                throw new IllegalArgumentException("Neuer Spielername fehlt");
            if(action.argument().endsWith("MIN")){
                int m=Integer.parseInt(minutes);if(m<0||m>10080)throw new IllegalArgumentException("Minuten 0..10080");
                if(action.command().contains("raidstart") && (m<1 || m>30))throw new IllegalArgumentException("Raid nur 1..30 Minuten");
                minutes=Integer.toString(m);
            }
            args=switch(action.argument()){
                case "" -> "";case "PLAYER","TEAM" -> " "+target;
                case "PLAYER_MIN","TEAM_MIN" -> " "+target+" "+minutes;
                case "TEAM_PLAYER" -> " "+target+" "+extra;
                default -> throw new IllegalArgumentException("Unbekannte Admin-Aktion");
            };
        }catch(Exception invalid){note="Nicht ausgefuehrt: "+invalid.getMessage();return;}
        Runnable task=()->sendCommand("teamwars admin "+action.command()+args);
        if(action.dangerous())confirm("Admin-Aktion wirklich ausfuehren: "+action.label()+"?",task);
        else task.run();
    }
    private void confirm(String title,Runnable accepted){
        minecraft.setScreen(new ConfirmScreen(choice->{minecraft.setScreen(this);if(choice)accepted.run();},
            Component.literal("Rogler Core - Bestaetigung"),Component.literal(title)));
    }
    private void sendCommand(String command){
        if(minecraft!=null && minecraft.player!=null){minecraft.player.connection.sendCommand(command);note="Befehl ausgefuehrt: Rueckmeldung im Chat.";}
    }
    @Override public void render(GuiGraphics g,int mouseX,int mouseY,float partial){
        g.fill(0,0,width,height,0xD009111B);
        int panelWidth=Math.min(670,width-14),left=(width-panelWidth)/2;
        g.fill(left,13,left+panelWidth,height-13,0xFF304653);
        g.fill(left+1,14,left+panelWidth-1,height-14,BACK);
        g.fill(left+4,18,left+panelWidth-4,39,PANEL);
        g.drawString(font,"ROGLER CORE // CONTROL CENTER",left+12,25,ACCENT,false);
        g.drawString(font,"v0.8.2",left+panelWidth-52,25,MUTED,false);
        g.fill(left+141,42,left+142,height-60,0xFF304653);
        g.drawString(font,name(page),x,30,TEXT,false);
        if(state==null)g.drawString(font,"Lade Serverstatus ...",x,Math.max(52,height-91),WARNING,false);
        else {
            g.drawString(font,"Spieler online: "+state.online()+"  |  "+(admin()?"OP "+state.permission():"Spieler"),x,Math.max(52,height-91),MUTED,false);
            if(page==Page.START || page==Page.TEAM)
                g.drawString(font,text("FTB: "+state.team(),Math.max(18,bodyWidth/6)),x,Math.max(63,height-79),ACCENT,false);
        }
        g.drawString(font,text(note,Math.max(25,(panelWidth-18)/6)),left+10,height-25,WARNING,false);
        super.render(g,mouseX,mouseY,partial);
        // Vanilla widgets carry their own focus, scrolling and input handling.
    }
    @Override public void onClose(){if(minecraft!=null)minecraft.setScreen(null);}
}
