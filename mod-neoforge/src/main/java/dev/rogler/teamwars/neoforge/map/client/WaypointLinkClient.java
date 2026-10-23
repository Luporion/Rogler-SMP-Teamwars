package dev.rogler.teamwars.neoforge.map.client;

import dev.rogler.teamwars.core.WaypointLinkJournal;
import dev.rogler.teamwars.core.WaypointLinkPolicy;
import dev.rogler.teamwars.core.WaypointReconcilePolicy;
import net.minecraft.client.Minecraft;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Explicit, local-only persistent waypoint pair journal. No server packets,
 * no destructive deletion/move, and no modification of third-party save files.
 */
public final class WaypointLinkClient {
    private WaypointLinkClient() {}
    private static Path journalPath() throws Exception {
        Minecraft mc=Minecraft.getInstance();
        if(mc.player==null||mc.level==null)throw new IllegalStateException("Not connected");
        String server;
        if(mc.getSingleplayerServer()!=null)
            server="singleplayer:"+mc.getSingleplayerServer().getWorldData().getLevelName();
        else if(mc.getCurrentServer()!=null)
            server="multiplayer:"+mc.getCurrentServer().ip;
        else throw new IllegalStateException("No world/server identity");
        // Hash local identity (not a pathname from an untrusted remote server).
        var digest=MessageDigest.getInstance("SHA-256").digest((server+"|"+mc.player.getUUID()).getBytes(StandardCharsets.UTF_8));
        StringBuilder hash=new StringBuilder();
        for(int i=0;i<16;i++)hash.append(Character.forDigit((digest[i]>>>4)&15,16))
            .append(Character.forDigit(digest[i]&15,16));
        return mc.gameDirectory.toPath().resolve("config").resolve("roglercore")
            .resolve("private-waypoint-links").resolve(hash+".properties");
    }
    private static String dimension() {
        return Minecraft.getInstance().level.dimension().location().toString();
    }
    private static List<WaypointLinkPolicy.Review> current() throws Exception {
        var links=WaypointLinkJournal.load(journalPath());
        var dim=dimension();
        return WaypointLinkPolicy.review(links.stream().filter(l->l.ftbBaseline().dimension().equals(dim)).toList(),
            FtbXaeroWaypointMirror.readFtbPoints(),LocalWaypointBridge.xaeroPoints());
    }
    public static String link(int fi,int xi) throws Exception {
        var path=journalPath();var links=new ArrayList<>(WaypointLinkJournal.load(path));
        var ftb=FtbXaeroWaypointMirror.readFtbPoints();var xaero=LocalWaypointBridge.xaeroPoints();
        if(fi<0||fi>=ftb.size()||xi<0||xi>=xaero.size())return "Ungueltiger Listenindex: /roglerwaypoints list";
        var f=ftb.get(fi);var x=xaero.get(xi);
        if(!WaypointLinkPolicy.canLink(links,f,x,ftb,xaero))
            return "Nicht verknuepft: Paar muss dieselben Koordinaten und Dimension haben, ohne Duplikate";
        var pair=new WaypointLinkPolicy.Link(UUID.randomUUID(),f,x);
        links.add(pair);
        WaypointLinkJournal.save(path,links);
        return "Lokal verknuepft: "+pair.id()+" (nur auf diesem Client und Serverprofil)";
    }
    public static void showChanges() throws Exception {
        var reviews=current();
        WaypointClientCommands.chat("Private Links in dieser Dimension: "+reviews.size()+". Keine Serveruebertragung.");
        int shown=0;
        for(var r:reviews) {
            if(r.state()==WaypointLinkPolicy.State.UNCHANGED)continue;
            if(++shown>12){WaypointClientCommands.chat("Weitere Links nicht angezeigt");break;}
            WaypointClientCommands.chat(r.link().id()+" : "+r.state());
            if(r.canApply())WaypointClientCommands.chat("Nur Name/Farbe: /roglerwaypoints apply "+r.link().id()+" "+r.token());
            else if(r.state()==WaypointLinkPolicy.State.CONVERGED)
                WaypointClientCommands.chat("Beide gleich: /roglerwaypoints acknowledge "+r.link().id()+" "+r.token());
        }
        if(shown==0)WaypointClientCommands.chat("Alle verknuepften Punkte unveraendert.");
    }
    private static WaypointLinkPolicy.Review find(UUID id) throws Exception {
        return current().stream().filter(r->r.link().id().equals(id)).findFirst()
            .orElseThrow(()->new IllegalArgumentException("Link nicht in dieser Dimension vorhanden"));
    }
    private static void saveBaseline(WaypointLinkPolicy.Review original,
                                     dev.rogler.teamwars.core.WaypointSyncPolicy.Point ftb,
                                     dev.rogler.teamwars.core.WaypointSyncPolicy.Point xaero) throws Exception {
        var path=journalPath();var links=new ArrayList<>(WaypointLinkJournal.load(path));
        boolean replaced=false;
        for(int i=0;i<links.size();i++)if(links.get(i).equals(original.link())) {
            links.set(i,new WaypointLinkPolicy.Link(original.link().id(),ftb,xaero));replaced=true;break;
        }
        if(!replaced)throw new IllegalStateException("Link wurde waehrend Aktion veraendert");
        WaypointLinkJournal.save(path,links);
    }
    /** Linked entries block import resurrection after a deliberate delete in either map. */
    public static java.util.Set<String> reservedLocations() throws Exception {
        var dim=dimension();
        return WaypointLinkJournal.load(journalPath()).stream()
            .filter(l->l.ftbBaseline().dimension().equals(dim))
            .map(WaypointLinkPolicy.Link::locationKey)
            .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }
    /** Applies one explicitly confirmed metadata-only change, with re-read optimistic guard. */
    public static String apply(UUID id,String token) throws Exception {
        var r=find(id);
        if(!WaypointLinkPolicy.safeToApply(r,token))
            return "Abbruch: Token veraltet, Konflikt, geloescht oder verschoben. /roglerwaypoints changes";
        String source=r.state()==WaypointLinkPolicy.State.FTB_METADATA_CHANGED?"ftb":"xaero";
        var sources=source.equals("ftb")?FtbXaeroWaypointMirror.readFtbPoints():LocalWaypointBridge.xaeroPoints();
        var targets=source.equals("ftb")?LocalWaypointBridge.xaeroPoints():FtbXaeroWaypointMirror.readFtbPoints();
        var winner=source.equals("ftb")?r.ftb():r.xaero();
        var target=source.equals("ftb")?r.xaero():r.ftb();
        int index=sources.indexOf(winner);
        if(index<0||sources.stream().filter(p->p.locationKey().equals(winner.locationKey())).count()!=1 ||
           targets.stream().filter(p->p.locationKey().equals(target.locationKey())).count()!=1)
            return "Abbruch: Quell- oder Zielwegpunkt ist nicht mehr eindeutig";
        var commandToken=WaypointReconcilePolicy.token(source,winner,target);
        String result=LocalWaypointBridge.resolveMetadata(source,index,commandToken);
        if(!result.startsWith("Name/Farbe von "))return result;
        // No silent success if the API has saved neither change; state on disk remains unchanged.
        var after=find(id);
        if(after.ftb()==null || after.xaero()==null ||
           !after.ftb().locationKey().equals(r.link().locationKey()) ||
           !after.xaero().locationKey().equals(r.link().locationKey()))
            return "Wegpunkt-Aenderung nicht eindeutig bestaetigt; Journal unveraendert";
        // Xaero colors are palette ordinals. The closest palette color may differ
        // from the original FTB RGB; journal the ACTUAL values on both sides.
        boolean confirmed = source.equals("ftb")
            ? after.ftb().equals(r.ftb()) && after.xaero().name().equals(r.ftb().name())
              && after.xaero().color()==XaeroWaypointPalette.nearestRgb(r.ftb().color())
            : after.xaero().equals(r.xaero()) && after.ftb().equals(r.xaero());
        if(!confirmed)
            return "Wegpunkt-Aenderung nicht bestaetigt; Journal unveraendert. /roglerwaypoints changes";
        saveBaseline(r,after.ftb(),after.xaero());
        return "Name/Farbe uebernommen und reale Farben im Link-Journal gespeichert: "+id;
    }
    /** Repair journal after a mutation succeeded but persistence was interrupted. */
    public static String acknowledge(UUID id,String token) throws Exception {
        var r=find(id);
        if(r.state()!=WaypointLinkPolicy.State.CONVERGED || !r.token().equals(token))
            return "Abbruch: Beide Wegpunkte muessen eindeutig und gleich sein; Token aktuell?";
        saveBaseline(r,r.ftb(),r.xaero());
        return "Gemeinsamen Zustand als neue Link-Basis bestaetigt: "+id;
    }
    /** Forgets just Rogler's local identity; never removes a real waypoint. */
    public static String unlink(UUID id) throws Exception {
        var path=journalPath();var links=new ArrayList<>(WaypointLinkJournal.load(path));
        int before=links.size();
        links.removeIf(l->l.id().equals(id) && l.ftbBaseline().dimension().equals(dimension()));
        if(links.size()==before)return "Link nicht gefunden oder andere Dimension";
        WaypointLinkJournal.save(path,links);
        return "Nur Rogler-Verknuepfung entfernt: "+id+". Private FTB-/Xaero-Wegpunkte bleiben erhalten.";
    }
}
