package dev.rogler.teamwars.neoforge.map.client;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import dev.rogler.teamwars.core.WaypointSyncPolicy;
import dev.rogler.teamwars.core.WaypointReconcilePolicy;
import dev.rogler.teamwars.neoforge.RoglerTeamWarsMod;
import dev.rogler.teamwars.neoforge.map.WaypointMutationPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.List;
import java.util.UUID;

/** Explicit sharing commands. No private local pin crosses the network without a user action. */
@EventBusSubscriber(modid=RoglerTeamWarsMod.MOD_ID,value=Dist.CLIENT)
public final class WaypointClientCommands {
    private WaypointClientCommands(){}
    public static void chat(String m){
        var player=Minecraft.getInstance().player;
        if(player!=null)player.displayClientMessage(Component.literal("[Rogler] "+m),false);
    }
    @SubscribeEvent public static void register(RegisterClientCommandsEvent e){
        e.getDispatcher().register(Commands.literal("roglerwaypoints")
            .executes(c->{chat("/roglerwaypoints list | preview | sync | link <FTB> <Xaero> | changes | apply <UUID> <Token> | acknowledge <UUID> <Token> | unlink <UUID> | sync-linked | autosync on/off | delete-linked <UUID> <Token> | move-linked <UUID> ftb/xaero <Index> <Token> | share | unshare");return 1;})
            .then(Commands.literal("list").executes(c->{list();return 1;}))
            .then(Commands.literal("preview").executes(c->{preview();return 1;}))
            .then(Commands.literal("resolve")
                .then(Commands.argument("source",StringArgumentType.word())
                .then(Commands.argument("index",IntegerArgumentType.integer(0,255))
                .then(Commands.argument("token",StringArgumentType.word()).executes(c->{
                    String source=StringArgumentType.getString(c,"source");
                    int index=IntegerArgumentType.getInteger(c,"index");
                    String token=StringArgumentType.getString(c,"token");
                    try{chat(LocalWaypointBridge.resolveMetadata(source,index,token));}
                    catch(Exception fail){chat("Abgleich abgebrochen: "+fail.getClass().getSimpleName());}
                    return 1;
                })))))
            .then(Commands.literal("sync").executes(c->{
                try{chat(LocalWaypointBridge.syncPrivate());}
                catch(Exception failure){chat("Private Sync nicht verfuegbar: "+failure.getClass().getSimpleName());}
                return 1;
            }))
            .then(Commands.literal("sync-linked").executes(c->{
                try{chat(WaypointLinkClient.syncLinkedMetadata());}
                catch(Exception fail){chat("Link-Abgleich gestoppt: "+fail.getMessage());}
                return 1;
            }))
            .then(Commands.literal("autosync")
                .then(Commands.literal("on").executes(c->{chat(WaypointLinkClient.setAutoMetadata(true));return 1;}))
                .then(Commands.literal("off").executes(c->{chat(WaypointLinkClient.setAutoMetadata(false));return 1;})))
            .then(Commands.literal("delete-linked")
                .then(Commands.argument("linkId",StringArgumentType.word())
                .then(Commands.argument("linkToken",StringArgumentType.word()).executes(c->{
                    try{chat(WaypointLinkClient.confirmDeletion(
                        UUID.fromString(StringArgumentType.getString(c,"linkId")),
                        StringArgumentType.getString(c,"linkToken")));}
                    catch(Exception err){chat("Loeschung abgebrochen: "+err.getMessage());}
                    return 1;
                }))))
            .then(Commands.literal("move-linked")
                .then(Commands.argument("linkId",StringArgumentType.word())
                .then(Commands.argument("source",StringArgumentType.word())
                .then(Commands.argument("index",IntegerArgumentType.integer(0,255))
                .then(Commands.argument("linkToken",StringArgumentType.word()).executes(c->{
                    try{chat(WaypointLinkClient.confirmMove(
                        UUID.fromString(StringArgumentType.getString(c,"linkId")),
                        StringArgumentType.getString(c,"source"),
                        IntegerArgumentType.getInteger(c,"index"),
                        StringArgumentType.getString(c,"linkToken")));}
                    catch(Exception err){chat("Verschiebung abgebrochen: "+err.getMessage());}
                    return 1;
                }))))))
            .then(Commands.literal("link")
                .then(Commands.argument("ftbIndex",IntegerArgumentType.integer(0,255))
                .then(Commands.argument("xaeroIndex",IntegerArgumentType.integer(0,255)).executes(c->{
                    try {chat(WaypointLinkClient.link(IntegerArgumentType.getInteger(c,"ftbIndex"),IntegerArgumentType.getInteger(c,"xaeroIndex")));}
                    catch(Exception err){chat("Verknuepfung fehlgeschlagen: "+err.getMessage());}return 1;
                }))))
            .then(Commands.literal("changes").executes(c->{
                try{WaypointLinkClient.showChanges();}
                catch(Exception err){chat("Journal nicht lesbar (keine Daten ueberschrieben): "+err.getMessage());}
                return 1;
            }))
            .then(Commands.literal("apply")
                .then(Commands.argument("linkId",StringArgumentType.word())
                .then(Commands.argument("linkToken",StringArgumentType.word()).executes(c->{
                    try{chat(WaypointLinkClient.apply(UUID.fromString(StringArgumentType.getString(c,"linkId")),StringArgumentType.getString(c,"linkToken")));}
                    catch(Exception err){chat("Abgleich abgebrochen: "+err.getMessage());}
                    return 1;
                }))))
            .then(Commands.literal("acknowledge")
                .then(Commands.argument("linkId",StringArgumentType.word())
                .then(Commands.argument("linkToken",StringArgumentType.word()).executes(c->{
                    try{chat(WaypointLinkClient.acknowledge(UUID.fromString(StringArgumentType.getString(c,"linkId")),StringArgumentType.getString(c,"linkToken")));}
                    catch(Exception err){chat("Bestaetigung abgebrochen: "+err.getMessage());}
                    return 1;
                }))))
            .then(Commands.literal("unlink")
                .then(Commands.argument("linkId",StringArgumentType.word()).executes(c->{
                    try{chat(WaypointLinkClient.unlink(UUID.fromString(StringArgumentType.getString(c,"linkId"))));}
                    catch(Exception err){chat("Unlink fehlgeschlagen: "+err.getMessage());}
                    return 1;
                })))
            .then(Commands.literal("share")
                .then(Commands.argument("source",StringArgumentType.word())
                .then(Commands.argument("index",IntegerArgumentType.integer(0,255)).executes(c->{
                    String src=StringArgumentType.getString(c,"source");int i=IntegerArgumentType.getInteger(c,"index");
                    share(src,i);return 1;
                }))))
            .then(Commands.literal("unshare").then(Commands.argument("id",StringArgumentType.word()).executes(c->{
                try{UUID id=UUID.fromString(StringArgumentType.getString(c,"id"));
                    PacketDistributor.sendToServer(new WaypointMutationPayload(true,id,null));chat("Widerruf angefordert: "+id);}
                catch(IllegalArgumentException invalid){chat("Ungueltige Wegpunkt-UUID");}
                return 1;
            }))));
    }
    public static void preview(){
        try {
            var ftb=FtbXaeroWaypointMirror.readFtbPoints();
            var xaero=LocalWaypointBridge.xaeroPoints();
            var report=WaypointReconcilePolicy.compare(ftb,xaero);
            chat("Nur Vorschau, keine Aenderungen: gleich="+report.same()+", Metadaten-Konflikte="+report.metadata()+
                ", moeglicherweise verschoben/geloescht="+report.possibleMoves()+
                ", mehrdeutig="+report.ambiguous()+", nur FTB="+report.ftbOnly()+", nur Xaero="+report.xaeroOnly());
            int shown=0;
            for(var diff:report.rows()) {
                if(diff.kind()!=WaypointReconcilePolicy.Kind.METADATA_CONFLICT)continue;
                if(shown++>=8){chat("Weitere Konflikte nicht angezeigt; Index pruefen.");break;}
                int fi=ftb.indexOf(diff.ftb()),xi=xaero.indexOf(diff.xaero());
                if(!WaypointReconcilePolicy.canReplaceMetadata(ftb,xaero,diff.ftb()) ||
                   !WaypointReconcilePolicy.canReplaceMetadata(xaero,ftb,diff.xaero())){
                    chat("Nicht eindeutig: "+diff.ftb().locationKey()+" - bitte in der Kartenmod manuell pruefen");continue;
                }
                chat("Konflikt "+diff.ftb().locationKey()+": FTB '"+diff.ftb().name()+"' / Xaero '"+diff.xaero().name()+"'");
                chat("FTB behalten: /roglerwaypoints resolve ftb "+fi+" "+WaypointReconcilePolicy.token("ftb",diff.ftb(),diff.xaero()));
                chat("Xaero behalten: /roglerwaypoints resolve xaero "+xi+" "+WaypointReconcilePolicy.token("xaero",diff.xaero(),diff.ftb()));
            }
            if(report.possibleMoves()>0||report.ambiguous()>0)
                chat("Verschobene, geloeschte und doppelte Wegpunkte werden NIE automatisch ueberschrieben oder geloescht.");
        }catch(Exception ex){chat("Abgleichvorschau nicht verfuegbar: "+ex.getClass().getSimpleName());}
    }
    private static void list(){
        try{
            List<WaypointSyncPolicy.Point> ftb=FtbXaeroWaypointMirror.readFtbPoints();
            List<WaypointSyncPolicy.Point> xaero=LocalWaypointBridge.xaeroPoints();
            chat("Private FTB: "+ftb.size()+" | Xaero: "+xaero.size()+" | Team-Freigaben: "+TeamWaypointClient.active().size());
            for(int i=0;i<Math.min(ftb.size(),16);i++)chat("ftb "+i+": "+ftb.get(i).name()+" "+ftb.get(i).locationKey());
            for(int i=0;i<Math.min(xaero.size(),16);i++)chat("xaero "+i+": "+xaero.get(i).name()+" "+xaero.get(i).locationKey());
            for(var pin:TeamWaypointClient.active())chat("team "+pin.id()+" / "+pin.point().name()+" (Widerruf nur durch Urheber)");
            if(ftb.size()>16||xaero.size()>16)chat("Nur die ersten 16 lokalen Wegpunkte angezeigt; weitere Indizes bleiben verwendbar.");
        }catch(Exception failure){chat("Wegpunktliste nicht verfuegbar: "+failure.getClass().getSimpleName());}
    }
    public static void share(String src,int index){
        try{
            List<WaypointSyncPolicy.Point> points=switch(src.toLowerCase(java.util.Locale.ROOT)){
                case "ftb" -> FtbXaeroWaypointMirror.readFtbPoints();
                case "xaero" -> LocalWaypointBridge.xaeroPoints();
                default -> throw new IllegalArgumentException("Verwende ftb oder xaero");
            };
            if(index>=points.size())throw new IllegalArgumentException("Index existiert nicht");
            var point=points.get(index);
            PacketDistributor.sendToServer(new WaypointMutationPayload(false,UUID.randomUUID(),point));
            chat("Nur dieser Wegpunkt wird an dein aktuelles FTB-Team freigegeben: "+point.name());
        }catch(Exception invalid){chat("Freigabe fehlgeschlagen: "+invalid.getMessage());}
    }
}
