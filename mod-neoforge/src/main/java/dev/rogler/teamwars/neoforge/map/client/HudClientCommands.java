package dev.rogler.teamwars.neoforge.map.client;

import dev.rogler.teamwars.neoforge.RoglerTeamWarsMod;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

/** In-game local settings, no server permission or server-side config required. */
@EventBusSubscriber(modid=RoglerTeamWarsMod.MOD_ID,value=Dist.CLIENT)
public final class HudClientCommands {
    private HudClientCommands() {}
    @SubscribeEvent public static void register(RegisterClientCommandsEvent e){
        e.getDispatcher().register(Commands.literal("roglerhud").executes(ctx->{
            Minecraft client=Minecraft.getInstance();
            client.execute(()->client.setScreen(new HudSettingsScreen(null)));
            return 1;
        }));
    }
}
