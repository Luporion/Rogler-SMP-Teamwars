package dev.rogler.teamwars.neoforge.teams;

import net.neoforged.fml.ModList;
import java.util.Optional;
import java.util.UUID;

/** Check actual FTB party UUIDs; names must NEVER grant ownership. Fail closed. */
public final class FtbTeamLookup {
    public enum State { EXISTS, MISSING, UNAVAILABLE }
    private FtbTeamLookup() {}
    public static State state(UUID id) {
        if (!ModList.get().isLoaded("ftbteams")) return State.UNAVAILABLE;
        try {
            Class<?> apiClass = Class.forName("dev.ftb.mods.ftbteams.api.FTBTeamsAPI");
            Object api = apiClass.getMethod("api").invoke(null);
            Object manager = api.getClass().getMethod("getManager").invoke(api);
            Object response = manager.getClass().getMethod("getTeamByID",UUID.class).invoke(manager,id);
            if (!(response instanceof Optional<?> opt)) return State.UNAVAILABLE;
            return opt.isPresent() ? State.EXISTS : State.MISSING;
        } catch (ReflectiveOperationException | RuntimeException err) {
            return State.UNAVAILABLE;
        }
    }
}
