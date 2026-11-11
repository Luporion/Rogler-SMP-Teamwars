package dev.rogler.teamwars.neoforge.ui.client;

import dev.rogler.teamwars.neoforge.RoglerTeamWarsMod;
import dev.rogler.teamwars.neoforge.ui.ControlCenterRequestPayload;
import dev.rogler.teamwars.neoforge.ui.ControlCenterAdminPayload;
import dev.rogler.teamwars.neoforge.ui.ControlCenterStatePayload;
import dev.rogler.teamwars.neoforge.ui.ControlCenterReportPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.commands.Commands;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;

/** O key /roglerui, independent of the legacy /rogler server command. */
public final class ControlCenterClient {
    private static ControlCenterStatePayload latest;
    private static ControlCenterAdminPayload adminData;
    private static ControlCenterReportPayload reportData;
    private ControlCenterClient(){}
    public static ControlCenterStatePayload latest(){return latest;}
    public static ControlCenterAdminPayload adminData(){return adminData;}
    public static ControlCenterReportPayload reportData(){return reportData;}
    public static void onReport(ControlCenterReportPayload packet){
        if(Minecraft.getInstance().player==null)return;
        if(!packet.kind().equals("own") && (latest==null || latest.permission()<2))return;
        reportData=packet;
        if(Minecraft.getInstance().screen instanceof ControlCenterScreen screen)screen.refreshReportData();
    }
    public static void requestReport(String kind,java.util.UUID target,int offset,int limit){
        reportData=null;
        String value=(kind.equals("claims")?target+"|":"")+offset+"|"+limit;
        request("inspect",kind,value);
    }
    public static void onAdminState(ControlCenterAdminPayload packet){
        if(Minecraft.getInstance().player==null)return;
        adminData=packet;
        if(Minecraft.getInstance().screen instanceof ControlCenterScreen screen)screen.refreshAdminData();
    }
    public static void requestAdmin(String kind,int offset){
        if(!kind.equals("teams") && !kind.equals("audit") && !kind.equals("status"))return;
        adminData=null;
        request("adminview",kind,Integer.toString(offset));
    }
    public static void onState(ControlCenterStatePayload packet){
        if(Minecraft.getInstance().player==null)return;
        latest=packet;
        if(packet.permission()<2){
            adminData=null;
            if(reportData!=null && !reportData.kind().equals("own"))reportData=null;
        }
        if(Minecraft.getInstance().screen instanceof ControlCenterScreen screen)screen.refreshState();
    }
    public static void open(){
        var mc=Minecraft.getInstance();if(mc.player==null || mc.level==null)return;
        latest=null;adminData=null;reportData=null;
        mc.setScreen(new ControlCenterScreen());
        PacketDistributor.sendToServer(new ControlCenterRequestPayload("view","",""));
    }
    public static void request(String operation,String key,String value){
        if(Minecraft.getInstance().player!=null)
            PacketDistributor.sendToServer(new ControlCenterRequestPayload(operation,key,value));
    }
    @EventBusSubscriber(modid=RoglerTeamWarsMod.MOD_ID,value=Dist.CLIENT)
    public static final class CommandsAndKeys {
        private static final KeyMapping OPEN = new KeyMapping("key.roglerteamwars.control_center",
            InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_O,"key.categories.roglerteamwars");
        private CommandsAndKeys(){}
        @SubscribeEvent public static void logout(net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut event){
            latest=null;adminData=null;reportData=null;
        }
        @SubscribeEvent public static void commands(RegisterClientCommandsEvent e){
            e.getDispatcher().register(Commands.literal("roglerui").executes(c->{Minecraft.getInstance().execute(ControlCenterClient::open);return 1;}));
            e.getDispatcher().register(Commands.literal("roglerconfig").executes(c->{Minecraft.getInstance().execute(ControlCenterClient::open);return 1;}));
        }
        @SubscribeEvent public static void tick(ClientTickEvent.Post e){
            if(Minecraft.getInstance().screen!=null)return;
            while(OPEN.consumeClick())ControlCenterClient.open();
        }
    }
    @EventBusSubscriber(modid=RoglerTeamWarsMod.MOD_ID,bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
    public static final class KeyRegistration {
        private KeyRegistration(){}
        @SubscribeEvent public static void register(RegisterKeyMappingsEvent event){event.register(CommandsAndKeys.OPEN);}
    }
}
