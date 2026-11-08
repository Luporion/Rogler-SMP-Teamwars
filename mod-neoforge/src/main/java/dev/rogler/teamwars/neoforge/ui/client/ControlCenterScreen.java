package dev.rogler.teamwars.neoforge.ui.client;

import dev.rogler.teamwars.core.HudPreferences;
import dev.rogler.teamwars.core.ui.ControlCenterPolicy;
import dev.rogler.teamwars.core.ui.AdminActionPolicy;
import dev.rogler.teamwars.neoforge.map.client.HudSettingsStore;
import dev.rogler.teamwars.neoforge.map.client.HudSettingsScreen;
import dev.rogler.teamwars.neoforge.map.client.HudPositionScreen;
import dev.rogler.teamwars.neoforge.map.client.WaypointSettingsScreen;
import dev.rogler.teamwars.neoforge.map.client.WaypointClientCommands;
import dev.rogler.teamwars.neoforge.map.client.WaypointLinkClient;
import dev.rogler.teamwars.neoforge.map.client.LocalWaypointBridge;
import dev.rogler.teamwars.neoforge.ui.ControlCenterStatePayload;
import dev.rogler.teamwars.neoforge.ui.ControlCenterAdminPayload;
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
        ADMIN_PROTECTION, ADMIN_POWER, ADMIN_COMPAT, ADMIN_SEARCH, ADMIN_DIRECTORY, ADMIN_DETAIL, ADMIN_RAIDS, ADMIN_AUDIT, ADMIN_STATUS, ADMIN_ACTIONS, ADMIN_DIAG }
    private static final int BACK=0xFF111B25, PANEL=0xFF1C2B37, ACCENT=0xFF5BD4D5,
        TEXT=0xFFE5EEF0, MUTED=0xFFA4B7C3, WARNING=0xFFFFC27A;
    private Page page=Page.START;
    private int firstSetting=0,firstAction=0,firstNav=0,adminOffset=0;
    private String compatGroup="warium";
    private String settingFilter="";
    private String actionFilter="";
    private int detailActionOffset=0;
    private ControlCenterAdminPayload.TeamRow selectedTeam;
    private String note="Einstellungen werden direkt gespeichert; bestehende Minecraft-Berechtigungen gelten.";
    private final Map<String,EditBox> fields=new LinkedHashMap<>();
    private final Map<String,String> draftSettings=new LinkedHashMap<>();
    private String pendingSettingKey, pendingSettingValue;
    private String targetValue="", extraValue="", minutesValue="10", tokenValue="", indexValue="0", index2Value="0";
    private int x,y,bodyWidth;
    private ControlCenterStatePayload state;

    public ControlCenterScreen(){super(Component.literal("Rogler Core – Control Center"));}
    public void refreshState(){
        // A passive server refresh must not erase a field the admin is still typing.
        saveInputs();
        state=ControlCenterClient.latest();
        if(state!=null){
            if(pendingSettingKey!=null && pendingSettingValue!=null
                && pendingSettingValue.equals(state.settings().get(pendingSettingKey))){
                draftSettings.remove(pendingSettingKey);
                pendingSettingKey=null;pendingSettingValue=null;
            }
            note=state.message();rebuild();
        }
    }
    public void refreshAdminData(){rebuild();}
    @Override protected void init(){super.init();state=ControlCenterClient.latest();rebuild();}
    private static String text(String value,int limit){return value.length()>limit?value.substring(0,limit-2)+"..":value;}
    private void button(String label,int xx,int yy,int ww,Runnable run){button(label,xx,yy,ww,run,null);}
    private void button(String label,int xx,int yy,int ww,Runnable run,String help){
        if(ww<16)return;
        var builder=Button.builder(Component.literal(label),b->run.run()).bounds(xx,yy,ww,19);
        if(help!=null)builder.tooltip(Tooltip.create(Component.literal(help)));
        addRenderableWidget(builder.build());
    }
    private void page(Page to){
        saveInputs();page=to;firstSetting=0;firstAction=0;adminOffset=0;
        if(to==Page.ADMIN_DIRECTORY || to==Page.ADMIN_RAIDS)ControlCenterClient.requestAdmin("teams",0);
        else if(to==Page.ADMIN_AUDIT)ControlCenterClient.requestAdmin("audit",0);
        else if(to==Page.ADMIN_STATUS)ControlCenterClient.requestAdmin("status",0);
        rebuild();
    }
    private boolean admin(){return state!=null&&state.permission()>=2;}
    private void rebuild(){
        clearWidgets();fields.clear();
        int panelWidth=Math.min(670,width-14),left=(width-panelWidth)/2;
        x=left+145;bodyWidth=Math.max(156,panelWidth-158);y=46;
        int xx=left+7, navY=48;
        var pages=java.util.Arrays.stream(Page.values())
            .filter(p->!p.name().startsWith("ADMIN")||admin())
            .filter(p->p!=Page.ADMIN_DETAIL || selectedTeam!=null).toList();
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
            case ADMIN_SEARCH -> searchSettings();
            case ADMIN_DIRECTORY -> adminDirectory(false);
            case ADMIN_DETAIL -> adminDetail();
            case ADMIN_RAIDS -> adminDirectory(true);
            case ADMIN_AUDIT -> adminAudit();
            case ADMIN_STATUS -> adminStatus();
            case ADMIN_ACTIONS -> actions(false);
            case ADMIN_DIAG -> actions(true);
        }
        button(page==Page.ADMIN_DETAIL?"Zur Teamliste":"Aktualisieren",x,height-48,110,()->{
            if(page==Page.ADMIN_DETAIL){page(Page.ADMIN_DIRECTORY);return;}
            ControlCenterClient.request("view","","");
            if(page==Page.ADMIN_AUDIT)ControlCenterClient.requestAdmin("audit",adminOffset);
            else if(page==Page.ADMIN_STATUS)ControlCenterClient.requestAdmin("status",0);
            else if(page==Page.ADMIN_DIRECTORY || page==Page.ADMIN_RAIDS)
                ControlCenterClient.requestAdmin("teams",adminOffset);
        });
        button("Schliessen",x+Math.max(110,bodyWidth-100),height-48,100,this::onClose);
    }
    private String name(Page item){return switch(item){
        case START -> "Uebersicht";case HUD -> "HUD & Navigation";case WAYPOINTS -> "Wegpunkte";
        case LINKS -> "Waypoint-Links";case TEAM -> "Mein Team";case CORE -> "Mein Base-Core";
        case ADMIN_TEAMS -> "Admin: Teams";case ADMIN_BASE -> "Admin: Basis";
        case ADMIN_PROTECTION -> "Admin: Schutz";case ADMIN_POWER -> "Admin: Energie";
        case ADMIN_COMPAT -> "Admin: Kompat";case ADMIN_SEARCH -> "Admin: Suche";
        case ADMIN_DIRECTORY -> "Admin: Teams/Cores";case ADMIN_DETAIL -> "Admin: Teamdetails";case ADMIN_RAIDS -> "Admin: Raidsteuerung";
        case ADMIN_AUDIT -> "Admin: Audit-Log";case ADMIN_STATUS -> "Admin: Status";
        case ADMIN_ACTIONS -> "Admin: Aktionen";case ADMIN_DIAG -> "Admin: Diagnose";
    };}
    private void home(){
        button("HUD-Einstellungen",x,y,bodyWidth,()->page(Page.HUD));y+=23;
        button("Wegpunkte & Teamfreigaben",x,y,bodyWidth,()->page(Page.WAYPOINTS));y+=23;
        button("Teamstatus",x,y,bodyWidth,()->page(Page.TEAM));y+=23;
        button("Core-Dashboard oeffnen",x,y,bodyWidth,()->sendCommand("teamwars base menu"));y+=23;
        if(admin()){
            button("Team- und Core-Verwaltung",x,y,bodyWidth,()->page(Page.ADMIN_DIRECTORY));y+=23;
            button("Server-Administration",x,y,bodyWidth,()->page(Page.ADMIN_SEARCH));y+=23;
        }
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
        button("HUD frei positionieren (Drag & Drop)",x,y,bodyWidth,
            ()->minecraft.setScreen(new HudPositionScreen(this)),"Aktueller Anker: "+p.anchor()+" / X "+p.offsetX()+" / Y "+p.offsetY());y+=23;
        button("Weitere HUD-Optionen & Feinausrichtung",x,y,bodyWidth,
            ()->minecraft.setScreen(new HudSettingsScreen(this)));
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
        field.setMaxLength(max);field.setValue(value);
        if(key.indexOf('.')>=0){
            field.setResponder(updated->{
                String baseline=state==null?null:state.settings().get(key);
                if(updated.equals(baseline)) draftSettings.remove(key);
                else draftSettings.put(key,updated);
            });
        }
        addRenderableWidget(field);fields.put(key,field);return field;
    }
    private String val(String key,String defaultValue){return fields.containsKey(key)?fields.get(key).getValue():defaultValue;}
    private void saveInputs(){
        targetValue=val("target",targetValue);extraValue=val("extra",extraValue);
        minutesValue=val("minutes",minutesValue);tokenValue=val("token",tokenValue);
        indexValue=val("index",indexValue);index2Value=val("index2",index2Value);
        actionFilter=val("actionFilter",actionFilter);
        settingFilter=val("filter",settingFilter);
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
    private void searchSettings(){
        if(!admin()){page(Page.START);return;}
        input("filter",settingFilter,x,y,Math.max(70,bodyWidth-92),64);
        button("Suchen",x+bodyWidth-87,y,87,()->{
            settingFilter=val("filter",settingFilter).trim();firstSetting=0;rebuild();
        });
        y+=24;
        String term=settingFilter.toLowerCase(java.util.Locale.ROOT);
        List<ControlCenterPolicy.Field> matching=ControlCenterPolicy.FIELDS.stream()
            .filter(f->term.isEmpty() || (f.group()+"."+f.key()+" "+f.label()+" "+f.description())
                .toLowerCase(java.util.Locale.ROOT).contains(term))
            .toList();
        renderSettings(matching,true);
    }
    private void settingsPageReset(){rebuild();}
    private void settings(String group){
        if(!admin()){page(Page.START);return;}
        List<ControlCenterPolicy.Field> matching=ControlCenterPolicy.FIELDS.stream().filter(f->f.group().equals(group)).toList();
        renderSettings(matching,false);
    }
    private void renderSettings(List<ControlCenterPolicy.Field> matching,boolean showGroup){
        int visible=Math.min(5,Math.max(2,(height-y-104)/46));
        firstSetting=Math.clamp(firstSetting,0,Math.max(0,matching.size()-visible));
        for(int i=firstSetting;i<Math.min(firstSetting+visible,matching.size());i++){
            var field=matching.get(i);String id=field.group()+"."+field.key();
            String current=state==null?"?":state.settings().getOrDefault(id,"?");
            final int yy=y;boolean editable=state!=null&&state.permission()>=field.permission();
            if(field.booleanValue()){
                button((showGroup?field.group()+": ":"")+field.label()+": "+enabled(Boolean.parseBoolean(current))+(editable?"":" [OP4]"),x,yy,bodyWidth,
                    ()->{if(editable)settingSave(field,Boolean.toString(!Boolean.parseBoolean(current)));},field.description());
            }else{
                button(text((showGroup?field.group()+": ":"")+field.label(),22),x,yy,Math.max(95,bodyWidth-140),()->{},field.description());
                input(id,draftSettings.getOrDefault(id,current),x+Math.max(100,bodyWidth-135),yy,60,
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
        if(!showGroup && matching.stream().anyMatch(f->f.group().equals("teams")) && note.startsWith("Einstellungen werden"))
            note="Teamgroesse/Anzahl sind nur Rogler-Beobachtungswerte. FTB-Teamlimits separat konfigurieren.";
    }
    private void settingSave(ControlCenterPolicy.Field field,String value){
        Runnable apply=()->{
            try{
                if(state==null)throw new IllegalStateException("Serverstatus fehlt");
                String id=field.group()+"."+field.key();
                String before=state.settings().get(id);
                if(before==null)throw new IllegalStateException("Einstellung nicht geladen");
                String after=field.normalize(value);
                if(before.equals(after)){draftSettings.remove(id);note="Keine Aenderung: "+field.label();rebuild();return;}
                pendingSettingKey=id;pendingSettingValue=after;
                ControlCenterClient.request("cas",id,before+"|"+after);
                note="Server prueft: "+field.label();
            }catch(Exception e){note="Ungueltiger Wert: "+e.getMessage();}
        };
        if(ControlCenterPolicy.sensitive(field))confirm("Kritische Einstellung aendern: "+field.label()+"?",apply);
        else apply.run();
    }
    private void requestAdminPage(String kind,int nextOffset){
        adminOffset=Math.max(0,Math.min(1000,nextOffset));
        ControlCenterClient.requestAdmin(kind,adminOffset);rebuild();
    }
    private void adminDirectory(boolean raids){
        if(!admin()){page(Page.START);return;}
        var data=ControlCenterClient.adminData();
        int pageSize=Math.max(1,Math.min(5,(height-y-155-(raids?27:0))/47));
        if(raids){
            input("minutes",minutesValue,x,y,64,5);
            button("Start fuer Ziel",x+70,y,Math.max(70,(bodyWidth-78)/2),()->raidSelected(true));
            button("Stopp fuer Ziel",x+76+Math.max(70,(bodyWidth-78)/2),y,Math.max(70,(bodyWidth-78)/2),()->raidSelected(false));
            y+=27;
        }
        if(data!=null && data.kind().equals("teams") && data.offset()==adminOffset){
            for(var row:data.teams().stream().limit(pageSize).toList()){
                final String id=row.id().toString();
                button("Auswaehlen",x+bodyWidth-92,y+9,92,()->{
                    saveInputs();targetValue=id;note="FTB Team: "+row.name()+" ("+id+")";
                    if(!raids){selectedTeam=row;detailActionOffset=0;page(Page.ADMIN_DETAIL);}else rebuild();
                },"Team-UUID ins Aktionsformular uebernehmen");
                y+=47;
            }
            int total=data.total();
            button("< Zurueck",x,y,100,()->requestAdminPage("teams",adminOffset-pageSize));
            button("Weiter >",x+bodyWidth-100,y,100,()->requestAdminPage("teams",Math.min(1000,adminOffset+pageSize)));
            y+=22;
            // Preserve server action status instead of overwriting it with the selected target.
        }else button("Daten laden",x,y,bodyWidth,()->requestAdminPage("teams",adminOffset));
    }
    /** Contextual admin details, only from a server-authored OP directory snapshot.
     * Every action still passes through the server allowlist and permissions.
     */
    private void adminDetail(){
        if(!admin()){page(Page.START);return;}
        if(selectedTeam==null){page(Page.ADMIN_DIRECTORY);return;}
        // The full UUID is server-provided; no player-entered command strings.
        targetValue=selectedTeam.id().toString();
        y=124;
        input("minutes",minutesValue,x,y,80,5);
        button("Aktionen fuer gewaehltes Team",x+85,y,Math.max(70,bodyWidth-85),()->page(Page.ADMIN_ACTIONS));
        y+=25;
        var permitted=List.of("claimaudit","graceteam","raidstartteam","raidstopteam","reclaimlostteam")
            .stream().map(AdminActionPolicy::require)
            .filter(a->state!=null && state.permission()>=a.permission()).toList();
        int visible=Math.max(1,Math.min(4,(height-y-94)/22));
        detailActionOffset=Math.clamp(detailActionOffset,0,Math.max(0,permitted.size()-visible));
        for(int i=detailActionOffset;i<Math.min(detailActionOffset+visible,permitted.size());i++){
            var action=permitted.get(i);
            button(action.label()+(action.dangerous()?" [Bestaetigung]":""),x,y,bodyWidth,
                ()->detailAction(action));
            y+=22;
        }
        if(permitted.size()>visible){
            button("< Aktionen",x,y,Math.max(75,bodyWidth/2-3),()->{detailActionOffset=Math.max(0,detailActionOffset-visible);saveInputs();rebuild();});
            button("Aktionen >",x+bodyWidth/2+3,y,Math.max(75,bodyWidth/2-3),()->{detailActionOffset=Math.min(permitted.size()-visible,detailActionOffset+visible);saveInputs();rebuild();});
        }
    }
    private void detailAction(AdminActionPolicy.Action action){
        if(selectedTeam==null)return;
        saveInputs();
        String team=selectedTeam.id().toString();
        String value=switch(action.argument()) {
            case "TEAM_MIN" -> team+"|"+minutesValue.trim();
            case "TEAM" -> team;
            default -> "";
        };
        adminRequest(action.id(),value);
    }
    private void raidSelected(boolean start){
        saveInputs();
        String id=start?"raidstartteam":"raidstopteam";
        String args=targetValue.trim()+(start?"|"+minutesValue.trim():"");
        adminRequest(id,args);
    }
    private void adminAudit(){
        if(!admin()){page(Page.START);return;}
        var data=ControlCenterClient.adminData();
        int shown=Math.max(1,Math.min(5,(height-y-136)/24));
        if(data!=null && data.kind().equals("audit") && data.offset()==adminOffset){
            y+=shown*24;
            button("< Zurueck",x,y,100,()->requestAdminPage("audit",adminOffset-shown));
            button("Weiter >",x+bodyWidth-100,y,100,()->requestAdminPage("audit",Math.min(1000,adminOffset+shown)));
        }else button("Audit laden",x,y,bodyWidth,()->requestAdminPage("audit",adminOffset));
    }
    private void adminStatus(){
        if(!admin()){page(Page.START);return;}
        var info=ControlCenterClient.adminData();
        if(info==null || !info.kind().equals("status")){
            button("Status laden",x,y,bodyWidth,()->requestAdminPage("status",0));
        }else{
            y+=Math.min(5,info.audit().size())*24;
            button("Weitere Diagnoseaktionen",x,y,bodyWidth,()->page(Page.ADMIN_DIAG));
        }
    }
    private void adminRequest(String id,String value){
        try{
            var action=AdminActionPolicy.require(id);
            AdminActionPolicy.command(id,value); // client prevalidation; server repeats and is authoritative
            Runnable run=()->{ControlCenterClient.request("adminrun",id,value);
                note="Admin-Aktion an Server uebermittelt: "+action.label();};
            if(action.dangerous())confirm("Admin-Aktion ausfuehren: "+action.label()+"?",run);
            else run.run();
        }catch(Exception problem){note="Nicht ausgefuehrt: "+problem.getMessage();rebuild();}
    }
    private void actions(boolean diagnostics){
        if(!admin()){page(Page.START);return;}
        List<AdminActionPolicy.Action> all=diagnostics?AdminActionPolicy.DIAGNOSTICS:AdminActionPolicy.ACTIONS;
        input("actionFilter",actionFilter,x,y,Math.max(68,bodyWidth-91),64);
        button("Suchen",x+bodyWidth-86,y,86,()->{
            actionFilter=val("actionFilter",actionFilter).trim();firstAction=0;saveInputs();rebuild();
        });y+=23;
        String term=actionFilter.trim().toLowerCase(java.util.Locale.ROOT);
        List<AdminActionPolicy.Action> list=all.stream().filter(a->term.isEmpty()
            || (a.label()+" "+a.id()+" "+a.argument()).toLowerCase(java.util.Locale.ROOT).contains(term)).toList();
        if(!diagnostics){
            // Ziel: Spielername oder FTB-Team-UUID; zweites Feld: Minuten.
            input("target",targetValue,x,y,Math.max(70,bodyWidth-85),36);input("minutes",minutesValue,x+bodyWidth-79,y,79,5);y+=23;
            input("extra",extraValue,x,y,bodyWidth,36);y+=23;
        }
        int shown=Math.max(3,(height-y-86)/22);
        firstAction=Math.clamp(firstAction,0,Math.max(0,list.size()-shown));
        for(int i=firstAction;i<Math.min(firstAction+shown,list.size());i++){
            AdminActionPolicy.Action a=list.get(i);boolean allowed=state!=null&&state.permission()>=a.permission();
            button(allowed?a.label():a.label()+" [OP4]",x,y,bodyWidth,()->{if(allowed)adminAction(a);});y+=22;
        }
        button("<",x,y,45,()->{saveInputs();firstAction=Math.max(0,firstAction-shown);rebuild();});
        button(">",x+bodyWidth-45,y,45,()->{saveInputs();firstAction=Math.min(Math.max(0,list.size()-shown),firstAction+shown);rebuild();});
    }
    private void adminAction(AdminActionPolicy.Action action){
        saveInputs();
        if(action.id().equals("teams") || action.id().equals("corelist") || action.id().equals("orphanlist")){
            page(Page.ADMIN_DIRECTORY);return;
        }
        String input=switch(action.argument()){
            case "" -> "";
            case "PLAYER", "TEAM" -> targetValue.trim();
            case "PLAYER_MIN", "TEAM_MIN" -> targetValue.trim()+"|"+minutesValue.trim();
            case "TEAM_PLAYER" -> targetValue.trim()+"|"+extraValue.trim();
            default -> "";
        };
        adminRequest(action.id(),input);
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
        g.drawString(font,"v0.8.5",left+panelWidth-52,25,MUTED,false);
        g.fill(left+141,42,left+142,height-60,0xFF304653);
        g.drawString(font,name(page),x,30,TEXT,false);
        if(state==null)g.drawString(font,"Lade Serverstatus ...",x,Math.max(52,height-91),WARNING,false);
        else {
            g.drawString(font,"Spieler online: "+state.online()+"  |  "+(admin()?"OP "+state.permission():"Spieler"),x,Math.max(52,height-91),MUTED,false);
            if(page==Page.START || page==Page.TEAM)
                g.drawString(font,text("FTB: "+state.team(),Math.max(18,bodyWidth/6)),x,Math.max(63,height-79),ACCENT,false);
        }
        if((page==Page.ADMIN_DIRECTORY || page==Page.ADMIN_RAIDS) && admin()){
            if(page==Page.ADMIN_RAIDS)
                g.drawString(font,text("Ziel: "+(targetValue.isEmpty()?"bitte Team auswaehlen":targetValue),Math.max(16,bodyWidth/6)),
                    x,Math.max(45,height-65),ACCENT,false);
            var snapshot=ControlCenterClient.adminData();
            int start=page==Page.ADMIN_RAIDS?73:46;
            int limit=Math.max(1,Math.min(5,(height-46-155-(page==Page.ADMIN_RAIDS?27:0))/47));
            if(snapshot!=null && snapshot.kind().equals("teams") && snapshot.offset()==adminOffset){
                int i=0;
                for(var row:snapshot.teams()){
                    if(i>=limit)break;
                    int yy=start+i*47;
                    String first=row.name()+" | "+row.online()+" online / "+row.members()+" Mitglieder";
                    String second=row.core()+" | "+row.location();
                    String third="Punkte "+row.score()+" | Grace "+row.graceMinutes()+"m | Raid "+(row.raid()?"AKTIV":"aus");
                    g.drawString(font,text(first,Math.max(14,(bodyWidth-98)/6)),x,yy,MUTED,false);
                    g.drawString(font,text(second,Math.max(14,(bodyWidth-98)/6)),x,yy+12,TEXT,false);
                    g.drawString(font,text(third,Math.max(14,(bodyWidth-98)/6)),x,yy+24,row.raid()?WARNING:ACCENT,false);
                    i++;
                }
                g.drawString(font,"Teams "+(snapshot.total()==0?0:adminOffset+1)+"-"+
                    Math.min(snapshot.total(),adminOffset+limit)+" / "+snapshot.total(),x,Math.max(54,height-79),MUTED,false);
            }
        }
        if(page==Page.ADMIN_DETAIL && admin() && selectedTeam!=null){
            var row=selectedTeam;
            g.drawString(font,text("Team: "+row.name(),Math.max(16,bodyWidth/6)),x,50,ACCENT,false);
            g.drawString(font,text("UUID: "+row.id(),Math.max(16,bodyWidth/6)),x,63,MUTED,false);
            g.drawString(font,text("Core: "+row.core()+" | "+row.location(),Math.max(16,bodyWidth/6)),x,77,TEXT,false);
            g.drawString(font,"Mitglieder: "+row.members()+" | online: "+row.online(),x,91,MUTED,false);
            g.drawString(font,"Punkte: "+row.score()+" | Grace: "+row.graceMinutes()+"m | Raid: "+(row.raid()?"aktiv":"aus"),
                x,105,row.raid()?WARNING:ACCENT,false);
        }
        if((page==Page.ADMIN_AUDIT || page==Page.ADMIN_STATUS) && admin()){
            var audit=ControlCenterClient.adminData();
            if(audit!=null && audit.kind().equals(page==Page.ADMIN_STATUS?"status":"audit")
                && (page==Page.ADMIN_STATUS || audit.offset()==adminOffset)){
                int i=0,limit=Math.max(1,Math.min(5,(height-46-136)/24));
                for(String line:audit.audit()){
                    if(i>=limit)break;
                    g.drawString(font,text(line,Math.max(14,bodyWidth/6)),x,46+i*24,TEXT,false);
                    i++;
                }
            }
        }
        g.drawString(font,text(note,Math.max(25,(panelWidth-18)/6)),left+10,height-25,WARNING,false);
        super.render(g,mouseX,mouseY,partial);
        // Vanilla widgets carry their own focus, scrolling and input handling.
    }
    @Override public void onClose(){if(minecraft!=null)minecraft.setScreen(null);}
}
