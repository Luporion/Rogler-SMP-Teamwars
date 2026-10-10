package dev.rogler.teamwars.neoforge.teams;

import dev.rogler.teamwars.core.teams.TeamAssignmentPolicy;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.ModList;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Read-only adapter. FTB Teams 2101.1.x is the sole authority for team membership,
 * invites, kicks and parties. No FTB classes are linked into the compilation unit.
 * Reflection permits launching without FTB; failure is deliberately fail-closed.
 */
public final class FtbPartyReader {
    private String lastError = "Not queried";
    private boolean warned;
    private boolean apiFailure;

    public TeamAssignmentPolicy.Party partyFor(UUID player) {
        if (!ModList.get().isLoaded("ftbteams")) {
            lastError = "FTB Teams mod not loaded";
            apiFailure = true;
            return null;
        }
        try {
            Class<?> apiClass = Class.forName("dev.ftb.mods.ftbteams.api.FTBTeamsAPI");
            Object api = apiClass.getMethod("api").invoke(null);
            Object manager = api.getClass().getMethod("getManager").invoke(api);
            Object response = manager.getClass().getMethod("getTeamForPlayerID", UUID.class).invoke(manager, player);
            if (!(response instanceof Optional<?> optional) || optional.isEmpty()) {
                lastError = "Player has no FTB party";
                apiFailure = false;
                return null;
            }
            Object team = optional.get();
            if (!(Boolean) team.getClass().getMethod("isPartyTeam").invoke(team)) {
                lastError = "Personal FTB team (solo)";
                apiFailure = false;
                return null;
            }
            UUID id = (UUID) team.getClass().getMethod("getId").invoke(team);
            Object rawName = team.getClass().getMethod("getName").invoke(team);
            String name = rawName instanceof Component text ? text.getString() : String.valueOf(rawName);
            Object members = team.getClass().getMethod("getMembers").invoke(team);
            if (!(members instanceof Collection<?> rawMembers))
                throw new IllegalStateException("FTB getMembers did not return a Collection");
            Set<UUID> ids = new HashSet<>();
            for (Object member : rawMembers) {
                if (!(member instanceof UUID uuid)) throw new IllegalStateException("Non-UUID member from FTB");
                ids.add(uuid);
            }
            lastError = "OK";
            apiFailure = false;
            warned = false;
            return new TeamAssignmentPolicy.Party(id, name, ids);
        } catch (ReflectiveOperationException | RuntimeException failure) {
            Throwable cause = failure instanceof InvocationTargetException wrapped
                ? wrapped.getTargetException() : failure;
            lastError = cause.getClass().getSimpleName() + ": " + cause.getMessage();
            apiFailure = true;
            if (!warned) {
                warned = true;
                RoglerTeamWarsLogger.warn("FTB Teams integration failed; assigning isolated nametag teams. " + lastError);
            }
            return null;
        }
    }
    public String lastError() { return lastError; }
    public boolean apiFailure() { return apiFailure; }
}
