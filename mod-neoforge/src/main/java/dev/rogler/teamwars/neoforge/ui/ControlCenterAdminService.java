package dev.rogler.teamwars.neoforge.ui;

import dev.rogler.teamwars.core.base.BaseCoreLedger;
import dev.rogler.teamwars.neoforge.RoglerTeamWarsMod;
import dev.rogler.teamwars.neoforge.base.BaseCoreSavedData;
import dev.rogler.teamwars.neoforge.protection.BaseProtectionSavedData;
import dev.rogler.teamwars.neoforge.raid.RaidSavedData;
import dev.rogler.teamwars.neoforge.teams.FtbPartyReader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.neoforge.network.PacketDistributor;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Server-thread only. No world chunk access: data comes from FTB membership and SavedData. */
public final class ControlCenterAdminService {
    private ControlCenterAdminService(){}
    private static final int PER_PAGE=5;
    private static final long MAX_LOG_BYTES=128*1024L;
    private static Path log(MinecraftServer s){return s.getWorldPath(LevelResource.ROOT)
        .resolve("serverconfig").resolve("roglercore-ui-audit.log");}
    public static void audit(MinecraftServer server, UUID actor, String operation, String outcome){
        // Log only predefined action/setting names, not arbitrary player-provided strings.
        String line=Instant.now()+" | "+actor+" | "+safe(operation,64)+" | "+safe(outcome,72)+"\n";
        try {
            Path file=log(server);Files.createDirectories(file.getParent());
            if(Files.isRegularFile(file) && Files.size(file)>MAX_LOG_BYTES){
                Files.move(file,file.resolveSibling("roglercore-ui-audit.previous.log"),StandardCopyOption.REPLACE_EXISTING);
            }
            Files.writeString(file,line,StandardCharsets.UTF_8,StandardOpenOption.CREATE,StandardOpenOption.APPEND);
        }catch(IOException e){dev.rogler.teamwars.neoforge.teams.RoglerTeamWarsLogger.warn("UI audit write failed: "+e.getClass().getSimpleName());}
    }
    private static String safe(String s,int max){
        if(s==null)return "?";
        s=s.replaceAll("[\\r\\n\\t|]","_");return s.substring(0,Math.min(max,s.length()));
    }
    public static void send(ServerPlayer viewer,String kind,int offset){
        if(!viewer.createCommandSourceStack().hasPermission(2))return;
        MinecraftServer server=viewer.getServer();if(server==null)return;
        offset=Math.clamp(offset,0,1000);
        if(kind.equals("teams")){
            var rows=directory(server);
            PacketDistributor.sendToPlayer(viewer,new ControlCenterAdminPayload(kind,offset,rows.size(),
                rows.subList(Math.min(offset,rows.size()),Math.min(offset+PER_PAGE,rows.size())),List.of()));
        }else if(kind.equals("audit") || kind.equals("status")){
            var rows=kind.equals("audit")?auditRows(server):statusRows(server);
            PacketDistributor.sendToPlayer(viewer,new ControlCenterAdminPayload(kind,offset,rows.size(),List.of(),
                rows.subList(Math.min(offset,rows.size()),Math.min(offset+PER_PAGE,rows.size()))));
        }
    }
    private static List<ControlCenterAdminPayload.TeamRow> directory(MinecraftServer server){
        Map<UUID,String> names=new HashMap<>();Map<UUID,Integer> online=new HashMap<>();
        Map<UUID,Integer> members=new HashMap<>();
        var reader=new FtbPartyReader();
        for(ServerPlayer p:server.getPlayerList().getPlayers()){
            var assignment=RoglerTeamWarsMod.assignmentFor(p);
            if(!assignment.validParty())continue;
            UUID id=assignment.partyId();
            names.put(id,assignment.displayName());online.merge(id,1,Integer::sum);
            var party=reader.partyFor(p.getUUID());
            if(party!=null && party.id().equals(id))members.put(id,party.members().size());
        }
        var cfg=RoglerTeamWarsMod.baseConfig();
        Map<UUID,BaseCoreLedger.Entry> bases=new HashMap<>();
        for(var e:BaseCoreSavedData.get(server,cfg.zone(server),cfg.carrySeconds()).ledger().all())bases.put(e.teamId(),e);
        var protection=BaseProtectionSavedData.get(server);
        var raids=RaidSavedData.get(server);
        var ids=new HashSet<UUID>(names.keySet());ids.addAll(bases.keySet());
        for(var p:protection.all())ids.add(p.team);
        ids.addAll(raids.scores().keySet());
        for(var r:raids.raids())ids.add(r.defender());
        long now=System.currentTimeMillis();
        var rows=new ArrayList<ControlCenterAdminPayload.TeamRow>();
        for(UUID id:ids){
            var core=bases.get(id);String state=core==null?"Kein Core":core.mode().name();
            String pos=core==null||core.location()==null?"-":core.location().dimension()+" "+core.location().x()+","+core.location().y()+","+core.location().z();
            if(pos.length()>75)pos=pos.substring(0,75);
            var p=protection.team(id);
            int grace=p==null?0:(int)Math.min(1000000L,Math.max(0L,(p.graceUntilMillis-now+59999L)/60000L));
            var raid=raids.raid(id);
            boolean active=raid!=null && raid.hp()>0 && raid.untilMillis()>now;
            String label=names.getOrDefault(id,"Offline/Unbekannt");if(label.length()>48)label=label.substring(0,48);
            rows.add(new ControlCenterAdminPayload.TeamRow(id,label,online.getOrDefault(id,0),
                members.getOrDefault(id,0),state,pos,grace,raids.scores().getOrDefault(id,0),active));
        }
        rows.sort(Comparator.comparing(ControlCenterAdminPayload.TeamRow::name)
            .thenComparing(row->row.id().toString()));
        return rows;
    }
    private static List<String> statusRows(MinecraftServer server){
        var cfg=RoglerTeamWarsMod.baseConfig();
        var cores=BaseCoreSavedData.get(server,cfg.zone(server),cfg.carrySeconds()).ledger().all();
        var raids=RaidSavedData.get(server).raids();
        long now=System.currentTimeMillis();
        long active=raids.stream().filter(r->r.hp()>0 && r.untilMillis()>now).count();
        long memory=(Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory())/(1024L*1024L);
        var claims=BaseProtectionSavedData.get(server).all().stream().mapToLong(r->r.createdClaims.size()).sum();
        long placed=cores.stream().filter(c->c.mode()==BaseCoreLedger.Mode.PLACED).count();
        return List.of("Spieler online: "+server.getPlayerList().getPlayerCount(),
            "Registrierte Cores: "+cores.size()+" (platziert: "+placed+")",
            "Offene Raidfenster: "+active,
            "Rogler-verwaltete Claims: "+claims,
            "Java Heap belegt: "+memory+" MiB",
            "Server-Ticks: "+server.getTickCount()+" (kein TPS-/MSPT-Beweis)");
    }
    private static List<String> auditRows(MinecraftServer server){
        try{
            Path file=log(server);if(!Files.isRegularFile(file))return List.of();
            if(Files.size(file)>MAX_LOG_BYTES+16*1024L)return List.of("Audit zu gross: vorheriges Log lokal pruefen");
            List<String> lines=Files.readAllLines(file,StandardCharsets.UTF_8);
            java.util.Collections.reverse(lines);
            return lines.stream().map(s->safe(s,180)).toList();
        }catch(IOException e){return List.of("Audit nicht lesbar: "+e.getClass().getSimpleName());}
    }
}
