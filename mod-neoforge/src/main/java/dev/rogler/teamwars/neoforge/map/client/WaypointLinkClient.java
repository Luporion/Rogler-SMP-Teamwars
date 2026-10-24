package dev.rogler.teamwars.neoforge.map.client;

import dev.rogler.teamwars.core.WaypointLinkJournal;
import dev.rogler.teamwars.core.WaypointLinkPolicy;
import dev.rogler.teamwars.core.WaypointReconcilePolicy;
import dev.rogler.teamwars.core.WaypointSyncPolicy;
import net.minecraft.client.Minecraft;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import dev.rogler.teamwars.neoforge.RoglerTeamWarsMod;

/**
 * Explicit, local-only persistent waypoint pair journal. No server packets,
 * destructive counterpart operations require explicit token-checked commands;
 * no modification of third-party raw save files.
 */
@EventBusSubscriber(modid=RoglerTeamWarsMod.MOD_ID,value=Dist.CLIENT)
public final class WaypointLinkClient {
    private static boolean autoMetadata;
    private static int autoTicks;
    private static boolean autoBusy;
    /** Session-only opt-in: moves/deletes and team sharing are NEVER automatic. */
    public static boolean autoMetadataEnabled(){return autoMetadata;}
    public static String setAutoMetadata(boolean enable) {
        autoMetadata=enable;autoTicks=0;
        return enable ? "Automatischer privater Name/Farbe-Abgleich AN (nur eindeutige Paare; keine Moves/Deletes)"
                      : "Automatischer privater Name/Farbe-Abgleich AUS";
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        var mc=Minecraft.getInstance();
        if(mc.level==null||mc.player==null){autoMetadata=false;autoTicks=0;return;}
        if(!autoMetadata || autoBusy || ++autoTicks%200!=0)return;
        autoBusy=true;
        try {syncLinkedMetadata();}
        catch(Exception failure) {
            autoMetadata=false;
            WaypointClientCommands.chat("Auto-Abgleich deaktiviert: "+failure.getClass().getSimpleName()+" (keine unsichere Fortsetzung)");
        } finally {autoBusy=false;}
    }

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
            if(r.state()==WaypointLinkPolicy.State.MISSING_FTB||r.state()==WaypointLinkPolicy.State.MISSING_XAERO){
                WaypointClientCommands.chat("VORSICHT: Geloescht ODER verschoben? Nie automatisch entscheiden.");
                WaypointClientCommands.chat("Wirklich geloescht: /roglerwaypoints delete-linked "+r.link().id()+" "+r.token());
                String source=r.state()==WaypointLinkPolicy.State.MISSING_FTB?"ftb":"xaero";
                WaypointClientCommands.chat("Tatsaechlich verschoben: /roglerwaypoints move-linked "+r.link().id()+" "+source+" <Index> "+r.token());
            }
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
    /** Associate points just created by the EXPLICIT import, once both real mod stores confirm
     *  one personal point per location. No existing point is claimed by an ambient scan.
     */
    public static int linkImported(List<WaypointSyncPolicy.Point> imported) throws Exception {
        if(imported.isEmpty())return 0;
        var path=journalPath();var saved=new ArrayList<>(WaypointLinkJournal.load(path));
        var ftb=FtbXaeroWaypointMirror.readFtbPoints();var xaero=LocalWaypointBridge.xaeroPoints();
        var seen=new java.util.HashSet<String>();int added=0;
        for(var entry:imported){
            if(!seen.add(entry.locationKey()))continue;
            var f=ftb.stream().filter(p->p.locationKey().equals(entry.locationKey())).toList();
            var x=xaero.stream().filter(p->p.locationKey().equals(entry.locationKey())).toList();
            if(f.size()!=1 || x.size()!=1 || !f.get(0).name().equals(x.get(0).name()))continue;
            if(!WaypointLinkPolicy.canLink(saved,f.get(0),x.get(0),ftb,xaero))continue;
            saved.add(new WaypointLinkPolicy.Link(UUID.randomUUID(),f.get(0),x.get(0)));
            added++;
        }
        if(added>0)WaypointLinkJournal.save(path,saved);
        return added;
    }

    /** Explicitly reconcile only clean one-sided NAME/COLOR changes of linked private pairs.
     *  No deletion, teleportation, movement, share or collision resolution is performed.
     */
    public static String syncLinkedMetadata() throws Exception {
        int count=0,skipped=0;
        for(var r:current()) {
            if(!r.canApply()) {if(r.state()!=WaypointLinkPolicy.State.UNCHANGED)skipped++;continue;}
            String response=apply(r.link().id(),r.token());
            if(response.startsWith("Name/Farbe uebernommen und reale Farben"))count++;
            else {skipped++; throw new IllegalStateException("Abgleich fehlgeschlagen: "+response);}
        }
        return "Verknuepfte private Punkte: "+count+" sichere Name/Farbe-Aenderungen uebernommen; "+skipped+
            " Konflikte/Moves/Deletes nicht angefasst.";
    }
    /** Linked entries block import resurrection after a deliberate delete in either map. */
    public static java.util.Set<String> reservedLocations() throws Exception {
        var dim=dimension();
        var links=WaypointLinkJournal.load(journalPath()).stream()
            .filter(l->l.ftbBaseline().dimension().equals(dim)).toList();
        // A moved AND renamed waypoint could otherwise look like a brand-new
        // waypoint to safeMissing(). Block EVERY add-only import until that
        // unfinished one-sided move/delete has been explicitly resolved.
        var reviews=WaypointLinkPolicy.review(links,
            FtbXaeroWaypointMirror.readFtbPoints(),LocalWaypointBridge.xaeroPoints());
        for(var review:reviews) {
            if(review.state()==WaypointLinkPolicy.State.MISSING_FTB ||
               review.state()==WaypointLinkPolicy.State.MISSING_XAERO ||
               review.state()==WaypointLinkPolicy.State.CONFLICT ||
               review.state()==WaypointLinkPolicy.State.AMBIGUOUS)
                throw new IllegalStateException("Ungelöster Link: /roglerwaypoints changes; zuerst klaeren");
        }
        return links.stream().map(WaypointLinkPolicy.Link::locationKey)
            .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }
    /** EXPLICIT confirmed deletion of the surviving counterpart only. No automatic deletion.
     *  Leave the journal record as a permanent tombstone, blocking /sync resurrection.
     */
    public static String confirmDeletion(UUID id, String token) throws Exception {
        var r=find(id);
        if(!WaypointLinkPolicy.safeToDelete(r,token))
            return "Abbruch: kein eindeutiger einseitiger Verlust oder Gegenstelle geaendert. /roglerwaypoints changes";
        var path=journalPath(); // abort on unavailable/invalid journal BEFORE any mutation
        if(WaypointLinkJournal.load(path).stream().noneMatch(l->l.equals(r.link())))
            return "Abbruch: Journal geaendert";
        if(r.state()==WaypointLinkPolicy.State.MISSING_FTB)
            WaypointMutationBridge.deleteXaero(r.xaero());
        else WaypointMutationBridge.deleteFtb(r.ftb());
        // Keep baseline as a tombstone, never re-import either side at these coordinates.
        return "Gegenstelle geloescht; Rogler-Link bleibt als Wiederherstellungs-Sperre erhalten: "+id;
    }

    /** The caller EXPLICITLY selects a potential moved point by index. Missing does not imply moved.
     *  Metadata must still match last baseline; anything ambiguous must be fixed manually.
     */
    public static String confirmMove(UUID id, String source, int index, String token) throws Exception {
        if(!source.equals("ftb")&&!source.equals("xaero"))return "Quelle muss ftb oder xaero sein";
        var path=journalPath();var links=WaypointLinkJournal.load(path);
        var r=find(id);
        var ftb=FtbXaeroWaypointMirror.readFtbPoints();
        var xaero=LocalWaypointBridge.xaeroPoints();
        var points=source.equals("ftb")?ftb:xaero;
        if(index<0||index>=points.size())return "Ungueltiger Quell-Index: /roglerwaypoints list";
        var moved=points.get(index);
        if(!WaypointLinkPolicy.safeToMove(r,token,source,moved,ftb,xaero,links))
            return "Abbruch: keine eindeutig bestaetigte reine Verschiebung; Quelle oder Ziel geaendert/mehrdeutig";
        if(source.equals("ftb"))WaypointMutationBridge.moveXaero(r.xaero(),moved);
        else WaypointMutationBridge.moveFtb(r.ftb(),moved);
        var nowFtb=FtbXaeroWaypointMirror.readFtbPoints();
        var nowXaero=LocalWaypointBridge.xaeroPoints();
        var ff=nowFtb.stream().filter(p->p.locationKey().equals(moved.locationKey())).toList();
        var xx=nowXaero.stream().filter(p->p.locationKey().equals(moved.locationKey())).toList();
        if(ff.size()!=1||xx.size()!=1 || !ff.get(0).name().equals(r.link().ftbBaseline().name()) ||
           !xx.get(0).name().equals(r.link().xaeroBaseline().name()))
            return "Verschiebung nicht bestaetigt: Link-Journal unveraendert. Bitte beide Karten pruefen";
        // Re-read journal, no merge if another action modified it.
        var before=WaypointLinkJournal.load(path);
        if(!before.equals(links))return "Journal parallel geaendert: bitte pruefen /roglerwaypoints changes";
        var updated=new ArrayList<>(links);
        if(!updated.remove(r.link()))return "Link nicht mehr vorhanden";
        updated.add(WaypointLinkPolicy.moved(r.link(),ff.get(0),xx.get(0)));
        WaypointLinkJournal.save(path,updated);
        return "Private Verschiebung in beide Karten uebernommen und Link-Basis aktualisiert: "+id;
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
