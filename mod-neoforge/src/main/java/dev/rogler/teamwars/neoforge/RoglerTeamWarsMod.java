package dev.rogler.teamwars.neoforge;

import com.mojang.brigadier.Command;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** Lightweight dev entry point only; adapter intentionally does not mutate existing modpack state. */
@Mod(RoglerTeamWarsMod.MOD_ID)
public final class RoglerTeamWarsMod {
    public static final String MOD_ID = "roglerteamwars";

    public RoglerTeamWarsMod() {
        NeoForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("teamwars")
            .then(Commands.literal("status")
                .executes(context -> {
                    context.getSource().sendSuccess(
                        () -> Component.literal("Rogler TeamWars P0 scaffold loaded. Gameplay NOT enabled."),
                        false);
                    return Command.SINGLE_SUCCESS;
                })));
    }
}
