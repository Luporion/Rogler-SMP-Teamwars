package dev.rogler.teamwars.neoforge.map.client;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import dev.rogler.teamwars.core.WaypointSyncPolicy;
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
            .executes(c->{chat("/roglerwaypoints list | sync | share ftb|xaero <Index> | unshare <UUID>");return 1;})
            .then(Commands.literal("list").executes(c->{list();return 1;}))
            .then(Commands.literal("sync").executes(c->{
                try{chat(LocalWaypointBridge.syncPrivate());}
                catch(Exception failure){chat("Private Sync nicht verfuegbar: "+failure.getClass().getSimpleName());}
                return 1;
            }))
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
