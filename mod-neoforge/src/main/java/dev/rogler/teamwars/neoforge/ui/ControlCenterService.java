package dev.rogler.teamwars.neoforge.ui;

import dev.rogler.teamwars.core.ui.ControlCenterPolicy;
import dev.rogler.teamwars.neoforge.RoglerTeamWarsMod;
import dev.rogler.teamwars.neoforge.base.BaseCoreConfig;
import dev.rogler.teamwars.neoforge.base.CorePowerConfig;
import dev.rogler.teamwars.neoforge.protection.BaseProtectionConfig;
import dev.rogler.teamwars.neoforge.teams.TeamWarsSettings;
import dev.rogler.teamwars.neoforge.teams.RoglerTeamWarsLogger;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

/** Validated, audit-logged, transactional per-key changes to the existing world-scoped config files. */
public final class ControlCenterService {
    private ControlCenterService() {}
    private static final Map<UUID,Long> LAST_WRITE = new HashMap<>();
    public static void clear(){LAST_WRITE.clear();}
    public static void logout(UUID id){LAST_WRITE.remove(id);}
    private static int permission(ServerPlayer player){return player.createCommandSourceStack().hasPermission(4)?4:player.createCommandSourceStack().hasPermission(2)?2:0;}
    private static Path file(MinecraftServer server,String group){
        String name=switch(group){
            case "teams" -> "roglerteamwars.properties";
            case "base" -> "roglerteamwars-base.properties";
            case "protection" -> "roglerteamwars-protection.properties";
            case "power" -> "roglerteamwars-core-power.properties";
            case "warium" -> "roglercore-compat.properties";
            case "sable" -> "roglercore-sable.properties";
            case "lifecycle" -> "roglercore-sable-lifecycle.properties";
            default -> throw new IllegalArgumentException("Unknown setting group");
        };
        return server.getWorldPath(LevelResource.ROOT).resolve("serverconfig").resolve(name);
    }
    private static Map<String,String> snapshot(MinecraftServer server){
        var result=new LinkedHashMap<String,String>();
        TeamWarsSettings teams=RoglerTeamWarsMod.teamSettings();
        BaseCoreConfig base=RoglerTeamWarsMod.baseConfig();
        BaseProtectionConfig protection=RoglerTeamWarsMod.protectionConfig();
        CorePowerConfig power=RoglerTeamWarsMod.powerConfig();
        result.put("teams.max_team_members",Integer.toString(teams.maxTeamMembers()));
        result.put("teams.max_parties",Integer.toString(teams.maxParties()));
        result.put("teams.refresh_interval_ticks",Integer.toString(teams.refreshIntervalTicks()));
        result.put("teams.nametags_for_teammates_only",Boolean.toString(teams.nametagsForTeammatesOnly()));
        result.put("teams.friendly_fire",Boolean.toString(teams.friendlyFire()));
        result.put("base.min_radius",Integer.toString(base.innerRadius()));
        result.put("base.max_radius",Integer.toString(base.outerRadius()));
        result.put("base.carry_seconds",Integer.toString(base.carrySeconds()));
        result.put("protection.claim_radius_chunks",Integer.toString(protection.claimRadiusChunks()));
        result.put("protection.new_team_grace_hours",Integer.toString(protection.newTeamGraceHours()));
        result.put("protection.auto_ftb_claims",Boolean.toString(protection.autoFtbClaims()));
        result.put("protection.base_guard_enabled",Boolean.toString(protection.baseGuard()));
        result.put("protection.raids_enabled",Boolean.toString(protection.raidsEnabled()));
        result.put("power.base_fe_per_tick",Integer.toString(power.baseFePerTick()));
        result.put("power.tier1_fe_per_tick",Integer.toString(power.tier1FePerTick()));
        result.put("power.tier2_fe_per_tick",Integer.toString(power.tier2FePerTick()));
        result.put("warium.warium_fusionblast_recovery",read(file(server,"warium"),"warium_fusionblast_recovery","OFF"));
        result.put("warium.warium_fusionblast_entity_id",read(file(server,"warium"),"warium_fusionblast_entity_id",""));
        result.put("sable.explosion_observation_enabled",read(file(server,"sable"),"explosion_observation_enabled","false"));
        result.put("lifecycle.enabled",read(file(server,"lifecycle"),"enabled","true"));
        result.put("lifecycle.cancel_tick_for_removed_owner",read(file(server,"lifecycle"),"cancel_tick_for_removed_owner","true"));
        result.put("lifecycle.cancel_split_for_removed_owner",read(file(server,"lifecycle"),"cancel_split_for_removed_owner","true"));
        result.put("lifecycle.cancel_split_for_removed_anchor_plot",read(file(server,"lifecycle"),"cancel_split_for_removed_anchor_plot","true"));
        result.put("lifecycle.log_rate_limit_per_minute",read(file(server,"lifecycle"),"log_rate_limit_per_minute","20"));
        return result;
    }
    private static String read(Path file,String key,String fallback){
        if(!Files.isRegularFile(file))return fallback;
        try(InputStream stream=Files.newInputStream(file)){
            Properties props=new Properties();props.load(stream);
            return props.getProperty(key,fallback);
        }catch(IOException ex){return "[unreadable]";}
    }
    private static void validateCombined(Map<String,String> values) {
        int n=0;
        for(var entry:values.entrySet())ControlCenterPolicy.require(entry.getKey()).normalize(entry.getValue());
        new TeamWarsSettings(
            parse(values,"teams.max_team_members"),parse(values,"teams.max_parties"),
            parse(values,"teams.refresh_interval_ticks"), bool(values,"teams.nametags_for_teammates_only"),
            bool(values,"teams.friendly_fire"));n++;
        new BaseCoreConfig(parse(values,"base.min_radius"),parse(values,"base.max_radius"),parse(values,"base.carry_seconds"));n++;
        new BaseProtectionConfig(parse(values,"protection.claim_radius_chunks"),
            parse(values,"protection.new_team_grace_hours"),bool(values,"protection.auto_ftb_claims"),
            bool(values,"protection.base_guard_enabled"),bool(values,"protection.raids_enabled"));n++;
        new CorePowerConfig(parse(values,"power.base_fe_per_tick"),parse(values,"power.tier1_fe_per_tick"),
            parse(values,"power.tier2_fe_per_tick"));n++;
        if(n!=4)throw new IllegalStateException("Missing config validation");
    }
    private static int parse(Map<String,String> values,String key){return Integer.parseInt(values.get(key));}
    private static boolean bool(Map<String,String> values,String key){return Boolean.parseBoolean(values.get(key));}
    private static void atomicSave(Path destination,String name,String value) throws IOException {
        Files.createDirectories(destination.getParent());
        Properties properties=new Properties();
        if(Files.isRegularFile(destination))try(InputStream in=Files.newInputStream(destination)){properties.load(in);}
        properties.setProperty(name,value);
        Path temp=Files.createTempFile(destination.getParent(),"rogler-ui-",".tmp");
        try{
            try(OutputStream out=Files.newOutputStream(temp)){properties.store(out,"Rogler Core Control Center; validated live setting");}
            try{Files.move(temp,destination,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
            catch(java.nio.file.AtomicMoveNotSupportedException ignored){Files.move(temp,destination,StandardCopyOption.REPLACE_EXISTING);}
        }finally{Files.deleteIfExists(temp);}
    }
    public static void handle(ControlCenterRequestPayload request,IPayloadContext context){
        context.enqueueWork(()->{
            if(!(context.player() instanceof ServerPlayer viewer))return;
            MinecraftServer server=viewer.getServer();if(server==null)return;
            String message="Serverstatus aktualisiert";
            int permission=permission(viewer);
            if(request.operation().equals("set")){
                if(permission<2){message="Abgelehnt: Adminrechte erforderlich";}
                else {
                    long now=server.getTickCount(), prior=LAST_WRITE.getOrDefault(viewer.getUUID(),Long.MIN_VALUE);
                    if(prior!=Long.MIN_VALUE && now-prior<5){message="Bitte Einstellungen nicht zu schnell aendern";}
                    else try {
                        ControlCenterPolicy.Field field=ControlCenterPolicy.require(request.key());
                        if(!ControlCenterPolicy.allowed(permission,field))throw new IllegalArgumentException("OP-Level 4 erforderlich");
                        String normalized=field.normalize(request.value());
                        var original=snapshot(server);
                        var changed=new LinkedHashMap<>(original);changed.put(request.key(),normalized);
                        validateCombined(changed);
                        if(!normalized.equals(original.get(request.key()))) {
                            // Never persist arbitrary client-supplied filenames or unlisted keys.
                            atomicSave(file(server,field.group()),field.key(),normalized);
                            boolean active = switch(field.group()) {
                                case "warium" -> dev.rogler.teamwars.neoforge.compat.WariumRecoveryGuard.reload(server);
                                case "sable" -> dev.rogler.teamwars.neoforge.compat.SableCompatibilityService.reload(server);
                                case "lifecycle" -> dev.rogler.teamwars.neoforge.compat.SableRemovedSublevelGuard.reload(server);
                                default -> {RoglerTeamWarsMod.reloadUiSettings(server);yield true;}
                            };
                            if(!active)throw new IllegalStateException("Datei gespeichert, aber Spezialmodul hat Einstellung abgelehnt; bitte Logs pruefen");
                            LAST_WRITE.put(viewer.getUUID(),now);
                            RoglerTeamWarsLogger.info("UI admin change by "+viewer.getUUID()+" "+request.key()+
                                " "+original.get(request.key())+" -> "+normalized);
                        }
                        message=field.label()+" gespeichert: "+normalized;
                    }catch(Exception invalid){message="Aenderung abgelehnt/ungeklaert: "+invalid.getMessage();}
                }
            } else if(!request.operation().equals("view")) message="Unbekannte Anfrage";
            var own=RoglerTeamWarsMod.assignmentFor(viewer);
            String team=own.validParty()?own.displayName()+" ("+own.partyId()+")":"Kein gueltiges FTB-Team";
            if(team.length()>128)team=team.substring(0,128);
            if(message.length()>220)message=message.substring(0,220);
            Map<String,String> current=permission>=2?snapshot(server):Map.of();
            PacketDistributor.sendToPlayer(viewer,new ControlCenterStatePayload(permission,team,message,
                server.getPlayerList().getPlayerCount(),current));
        });
    }
}
