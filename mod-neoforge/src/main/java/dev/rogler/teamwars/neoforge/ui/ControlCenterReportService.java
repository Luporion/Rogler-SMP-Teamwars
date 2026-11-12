package dev.rogler.teamwars.neoforge.ui;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.rogler.teamwars.core.base.BaseCoreLedger;
import dev.rogler.teamwars.neoforge.teams.FtbTeamManagement;
import dev.rogler.teamwars.core.ui.DiagnosticPolicy;
import dev.rogler.teamwars.neoforge.RoglerTeamWarsMod;
import dev.rogler.teamwars.neoforge.base.BaseCoreSavedData;
import dev.rogler.teamwars.neoforge.protection.BaseProtectionSavedData;
import dev.rogler.teamwars.neoforge.protection.FtbClaimBridge;
import dev.rogler.teamwars.neoforge.protection.FtbChunkStatusReader;
import dev.rogler.teamwars.neoforge.teams.FtbPartyReader;
import net.minecraft.commands.CommandSource;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Main-thread snapshots only. Own-team scope always comes from a fresh FTB membership read. */
public final class ControlCenterReportService {
    private ControlCenterReportService() {}
    private record Snapshot(String kind,UUID target,String title,long tick,List<DiagnosticPolicy.Row> rows) {
        private Snapshot {rows=List.copyOf(rows);if(rows.size()>DiagnosticPolicy.MAX_ROWS)throw new IllegalArgumentException("Report too large");}
    }
    private static FtbPartyReader reader=new FtbPartyReader();
    private static final Map<UUID,Snapshot> REPORTS=new HashMap<>();
    private static final Map<UUID,Long> LAST_READ=new HashMap<>();
    private static final Map<UUID,Long> LAST_RESYNC=new HashMap<>();
    public static void clear(){REPORTS.clear();LAST_READ.clear();LAST_RESYNC.clear();reader=new FtbPartyReader();}
    public static void logout(UUID player){REPORTS.remove(player);LAST_READ.remove(player);LAST_RESYNC.remove(player);}
    private static int permission(ServerPlayer p){return p.createCommandSourceStack().hasPermission(4)?4:p.createCommandSourceStack().hasPermission(2)?2:0;}
    private static UUID ownTeam(ServerPlayer p) {
        var party=reader.partyFor(p.getUUID());
        return party!=null && party.members().contains(p.getUUID())?party.id():null;
    }
    private static void row(List<DiagnosticPolicy.Row> rows,String key,String value){rows.add(new DiagnosticPolicy.Row(key,value));}
    private static void throttle(Map<UUID,Long> map,ServerPlayer p,int ticks) {
        long now=p.getServer().getTickCount(),before=map.getOrDefault(p.getUUID(),Long.MIN_VALUE);
        if(before!=Long.MIN_VALUE && now-before<ticks)throw new IllegalArgumentException("Bitte kurz warten");
        map.put(p.getUUID(),now);
    }
    public static String inspect(ServerPlayer viewer,String kind,String value) {
        throttle(LAST_READ,viewer,10);
        UUID own=ownTeam(viewer);
        var request=DiagnosticPolicy.parse(kind,value,permission(viewer),own);
        Snapshot snapshot=REPORTS.get(viewer.getUUID());
        boolean same=snapshot!=null && snapshot.kind().equals(kind) && Objects.equals(snapshot.target(),request.target());
        if(kind.equals("action")) {
            if(!same)throw new IllegalArgumentException("Keine Aktionsdetails vorhanden");
        }else if(request.offset()==0 || !same || viewer.getServer().getTickCount()-snapshot.tick()>200) {
            snapshot=kind.equals("team") || kind.equals("teamadm")
                ? team(viewer,kind,request.target()) : claims(viewer,kind,request.target());
            REPORTS.put(viewer.getUUID(),snapshot);
        }
        send(viewer,snapshot,request.offset(),request.limit());
        return "Diagnose aktualisiert";
    }
    private static void send(ServerPlayer viewer,Snapshot report,int offset,int limit) {
        // Recheck authority even for a previously cached/paginated report.
        if(report==null || (!DiagnosticPolicy.isOwn(report.kind()) && permission(viewer)<2))
            throw new IllegalArgumentException("Diagnose nicht erlaubt");
        if(DiagnosticPolicy.isOwn(report.kind()) && !Objects.equals(report.target(),ownTeam(viewer))) {
            REPORTS.remove(viewer.getUUID());throw new IllegalArgumentException("Team hat sich geaendert; bitte aktualisieren");
        }
        PacketDistributor.sendToPlayer(viewer,new ControlCenterReportPayload(report.kind(),report.target(),report.title(),
            offset,report.rows().size(),report.tick(),DiagnosticPolicy.page(report.rows(),offset,limit)));
    }
    private static Snapshot team(ServerPlayer viewer, String kind, UUID target) {
        try {
            List<DiagnosticPolicy.Row> rows=FtbTeamManagement.report(viewer,kind.equals("teamadm")?target:null);
            return new Snapshot(kind,target,"Teamverwaltung",viewer.getServer().getTickCount(),rows);
        } catch (CommandSyntaxException failure) {
            throw new IllegalArgumentException(failure.getMessage(),failure);
        }
    }

    public static void refreshOwnTeam(ServerPlayer viewer) {
        try {
            Snapshot updated=team(viewer,"team",ownTeam(viewer));
            REPORTS.put(viewer.getUUID(),updated);
            send(viewer,updated,0,DiagnosticPolicy.MAX_PAGE);
        } catch (RuntimeException failure) {
            // An already completed FTB operation remains successful even if its report fails.
            REPORTS.remove(viewer.getUUID());
            dev.rogler.teamwars.neoforge.teams.RoglerTeamWarsLogger.warn("Teambericht konnte nicht aktualisiert werden: "+failure.getMessage());
        }
    }

    public static void invalidateChangedTeams(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            Snapshot previous=REPORTS.get(player.getUUID());
            if (previous==null || !DiagnosticPolicy.isOwn(previous.kind())) continue;
            if (Objects.equals(previous.target(),ownTeam(player))) continue;
            REPORTS.remove(player.getUUID());
            PacketDistributor.sendToPlayer(player,new ControlCenterReportPayload(previous.kind(),null,
                "Team geändert",0,0,server.getTickCount(),List.of()));
        }
    }

    public static String resyncOwn(ServerPlayer player) {
        throttle(LAST_RESYNC,player,40);
        UUID own=ownTeam(player);
        if(own==null)throw new IllegalArgumentException("Kein bestaetigtes FTB-Team");
        String result=FtbClaimBridge.refreshOwnClaimView(player,own);
        if(result.startsWith("FTB resync sent"))return "Eigene FTB-Kartendaten wurden erneut gesendet";
        if(result.startsWith("DENIED"))throw new IllegalArgumentException("FTB-Teamzuordnung hat sich geaendert");
        return "FTB-Kartendaten nicht gesendet: vorhandene Teamdaten oder API nicht verfuegbar";
    }
    private static Snapshot claims(ServerPlayer viewer,String kind,UUID target) {
        MinecraftServer server=viewer.getServer();var rows=new ArrayList<DiagnosticPolicy.Row>();
        row(rows,"Quelle","Server-Snapshot / Rogler Core "+RoglerTeamWarsMod.VERSION);
        if(target==null) {
            row(rows,"FTB-Team","Kein bestaetigtes Party-Team; ggf. FTB-API nicht verfuegbar");
            return new Snapshot(kind,null,"Claims & Chunkloading",server.getTickCount(),rows);
        }
        row(rows,"Team",target.toString());
        var cfg=RoglerTeamWarsMod.baseConfig();
        var core=BaseCoreSavedData.get(server,cfg.zone(server),cfg.carrySeconds()).ledger().get(target);
        boolean placed=core!=null && core.mode()==BaseCoreLedger.Mode.PLACED && core.location()!=null
            && core.location().dimension().equals("minecraft:overworld");
        row(rows,"Core (gespeichert)",core==null?"Kein registrierter Core":core.mode().name());
        if(placed)row(rows,"Core-Position",core.location().x()+", "+core.location().y()+", "+core.location().z()+" / Overworld");
        var pcfg=RoglerTeamWarsMod.protectionConfig();
        row(rows,"Core-Regel","Auto-Claims: "+DiagnosticPolicy.yesNo(pcfg.autoFtbClaims())+" | Guard: "+DiagnosticPolicy.yesNo(pcfg.baseGuard()));
        var record=BaseProtectionSavedData.get(server).team(target);
        row(rows,"Core-Claim-Belege",(record==null?0:record.createdClaims.size())+" gespeichert; kein Gesamtzaehler fuer FTB-Claims");
        Object manager=null;String unavailable=null;
        try {
            if(!ModList.get().isLoaded("ftbchunks"))throw new IllegalStateException("Mod fehlt");
            manager=Class.forName("dev.ftb.mods.ftbchunks.data.ClaimedChunkManagerImpl").getMethod("getInstance").invoke(null);
            if(manager==null)throw new IllegalStateException("Manager nicht bereit");
        }catch(ReflectiveOperationException|RuntimeException ex){unavailable=ex.getClass().getSimpleName();}
        final Object ftb=manager;
        var level=server.overworld();
        Integer cx=placed?core.location().x()>>4:null,cz=placed?core.location().z()>>4:null;
        var status=FtbChunkStatusReader.read(ftb,target,cx,cz,pcfg.claimRadiusChunks(),
            (x,z)->{
                Class<?> type=Class.forName("dev.ftb.mods.ftblibrary.math.ChunkDimPos");
                return type.getConstructor(net.minecraft.resources.ResourceKey.class,ChunkPos.class)
                    .newInstance(level.dimension(),new ChunkPos(x,z));
            },(x,z)->level.getChunkSource().getChunkNow(x,z)!=null);
        row(rows,"FTB-Integration",unavailable==null?status.detail():"nicht verfuegbar ("+unavailable+")");
        row(rows,"FTB-Teamclaims",DiagnosticPolicy.count(status.claimed())+" / "+DiagnosticPolicy.count(status.maxClaims())
            +" | frei: "+DiagnosticPolicy.remaining(status.claimed(),status.maxClaims()));
        row(rows,"FTB-Forceload markiert",DiagnosticPolicy.count(status.marked())+" / "+DiagnosticPolicy.count(status.maxForced())
            +" | frei: "+DiagnosticPolicy.remaining(status.marked(),status.maxForced()));
        row(rows,"Offline-Forceload (FTB)",DiagnosticPolicy.yesNo(status.offline())+"; Regelstatus, kein Ticketnachweis");
        row(rows,"FTB-Forceload-Modus",ftbMode());
        row(rows,"Core-eigene Ticketquote","Noch nicht implementiert; obige Quote gehoert FTB Chunks");
        if(!placed)row(rows,"Corezone","Kein platzierter Overworld-Core; keine Chunk-Abfrage");
        else {
            long own=status.zone().stream().filter(c->c.owner().equals("eigen")).count();
            long unknown=status.zone().stream().filter(c->c.owner().equals("unbekannt")).count();
            long flags=status.zone().stream().filter(c->Boolean.TRUE.equals(c.marked())).count();
            long loaded=status.zone().stream().filter(FtbChunkStatusReader.Chunk::loaded).count();
            row(rows,"Corezone",status.zone().size()+" Chunks | eigen: "+own+" | markiert: "+flags+" | geladen: "+loaded+" | unbekannt: "+unknown);
        }
        row(rows,"Bedeutung","Markierung = FTB-Wunsch; FTB-API = Regelstatus; geladen = Chunk im Speicher, kein Tick-/Maschinennachweis");
        for(var chunk:status.zone())row(rows,"Chunk "+chunk.x()+", "+chunk.z(),
            "Claim: "+chunk.owner()+" | markiert: "+DiagnosticPolicy.yesNo(chunk.marked())
            +" | FTB-API: "+DiagnosticPolicy.yesNo(chunk.eligible())+" | geladen: "+DiagnosticPolicy.yesNo(chunk.loaded()));
        return new Snapshot(kind,target,"Claims & Chunkloading",server.getTickCount(),rows);
    }
    private static String ftbMode() {
        try {
            Object setting=Class.forName("dev.ftb.mods.ftbchunks.FTBChunksWorldConfig").getField("FORCE_LOAD_MODE").get(null);
            return DiagnosticPolicy.clean(String.valueOf(setting.getClass().getMethod("get").invoke(setting)),48);
        }catch(ReflectiveOperationException|RuntimeException ex){return "unbekannt";}
    }
    /** Captures actual command feedback without changing the real player's permission/entity context. */
    public static final class CommandCapture implements CommandSource {
        private final List<DiagnosticPolicy.Row> rows=new ArrayList<>();
        private boolean truncated;
        @Override public void sendSystemMessage(Component message) {
            String text=message.getString();
            for(int start=0;start<text.length();start+=180) {
                if(rows.size()>=DiagnosticPolicy.MAX_ROWS-2){truncated=true;return;}
                rows.add(new DiagnosticPolicy.Row("Rueckmeldung "+(rows.size()+1),text.substring(start,Math.min(start+180,text.length()))));
            }
        }
        @Override public boolean acceptsSuccess(){return true;}
        @Override public boolean acceptsFailure(){return true;}
        @Override public boolean shouldInformAdmins(){return false;}
    }
    public static void actionResult(ServerPlayer viewer,String title,int result,CommandCapture capture) {
        if(permission(viewer)<2)return;
        var rows=new ArrayList<DiagnosticPolicy.Row>();
        row(rows,"Ergebnis",result>0?"Befehl ausgefuehrt (Rueckgabewert "+result+")":"Kein Erfolg (Rueckgabewert "+result+")");
        rows.addAll(capture.rows);
        if(capture.rows.isEmpty())row(rows,"Details","Dieser Befehl liefert keine Textdetails; bei Bedarf Serverlog pruefen");
        if(capture.truncated)row(rows,"Hinweis","Ausgabe begrenzt; vollstaendige Diagnose ueber den vorhandenen Befehl");
        var snapshot=new Snapshot("action",null,DiagnosticPolicy.clean(title,64),viewer.getServer().getTickCount(),rows);
        REPORTS.put(viewer.getUUID(),snapshot);send(viewer,snapshot,0,3);
    }
}
