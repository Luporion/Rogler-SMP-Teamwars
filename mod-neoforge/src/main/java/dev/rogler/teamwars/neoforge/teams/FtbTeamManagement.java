package dev.rogler.teamwars.neoforge.teams;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import dev.rogler.teamwars.core.teams.TeamManagementPolicy;
import dev.rogler.teamwars.core.teams.TeamManagementPolicy.Action;
import dev.rogler.teamwars.core.teams.TeamManagementPolicy.Role;
import dev.rogler.teamwars.core.ui.DiagnosticPolicy;
import dev.rogler.teamwars.core.ui.DiagnosticPolicy.Row;
import dev.rogler.teamwars.neoforge.RoglerTeamWarsMod;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Server-side bridge for the FTB Teams 2101.1.11 party API and native operations. */
public final class FtbTeamManagement {
    private static final Map<UUID, Integer> LAST_ACTION = new HashMap<>();
    private static final UUID NO_PARTY = new UUID(0, 0);

    private FtbTeamManagement() {}

    public static void clear() { LAST_ACTION.clear(); }
    public static void logout(UUID id) { LAST_ACTION.remove(id); }

    private static Object call(Object target, String name, Class<?>[] types, Object... args)
        throws ReflectiveOperationException, CommandSyntaxException {
        try {
            return target.getClass().getMethod(name, types).invoke(target, args);
        } catch (InvocationTargetException failure) {
            if (failure.getCause() instanceof CommandSyntaxException commandFailure) throw commandFailure;
            throw failure;
        }
    }

    private static Object read(Object target, String name) throws ReflectiveOperationException, CommandSyntaxException {
        return call(target, name, new Class<?>[0]);
    }

    private static Object manager() throws ReflectiveOperationException, CommandSyntaxException {
        Class<?> type = Class.forName("dev.ftb.mods.ftbteams.api.FTBTeamsAPI");
        return read(type.getMethod("api").invoke(null), "getManager");
    }

    private static Object find(Object manager, String method, UUID id)
        throws ReflectiveOperationException, CommandSyntaxException {
        return ((Optional<?>)call(manager, method, new Class<?>[]{UUID.class}, id)).orElse(null);
    }

    private static boolean isParty(Object team) throws ReflectiveOperationException, CommandSyntaxException {
        return team != null && Boolean.TRUE.equals(read(team, "isPartyTeam"));
    }

    private static UUID id(Object team) throws ReflectiveOperationException, CommandSyntaxException {
        return (UUID)read(team, "getId");
    }

    private static Collection<?> members(Object party) throws ReflectiveOperationException, CommandSyntaxException {
        return (Collection<?>)read(party, "getMembers");
    }

    private static Role rank(Object party, UUID player) throws ReflectiveOperationException, CommandSyntaxException {
        return Role.fromFtb(((Enum<?>)call(party, "getRankForPlayer", new Class<?>[]{UUID.class}, player)).name());
    }

    private static String playerName(Object manager, UUID player) throws ReflectiveOperationException, CommandSyntaxException {
        return ((Component)call(manager, "getPlayerName", new Class<?>[]{UUID.class}, player)).getString();
    }

    private static String name(Object party) throws ReflectiveOperationException, CommandSyntaxException {
        return ((Component)read(party, "getName")).getString();
    }

    private static Object ownParty(Object manager, UUID player) throws ReflectiveOperationException, CommandSyntaxException {
        Object team = find(manager, "getTeamForPlayerID", player);
        if (!isParty(team)) return null;
        if (!members(team).contains(player) || !rank(team, player).member())
            throw denied("FTB-Teammitgliedschaft nicht bestätigt");
        return team;
    }

    private static int partyCount(Object manager, int stopAt) throws ReflectiveOperationException, CommandSyntaxException {
        int count = 0;
        for (Object team : (Collection<?>)read(manager, "getTeams")) {
            if (isParty(team) && ++count >= stopAt) break;
        }
        return count;
    }

    public static void checkCreate(Object manager) throws CommandSyntaxException {
        var rules = RoglerTeamWarsMod.teamSettings();
        if (!rules.enforceTeamLimits()) return;
        try {
            checkServerThread((MinecraftServer)read(manager, "getServer"));
            if (TeamManagementPolicy.atLimit(true, partyCount(manager, rules.maxParties()), rules.maxParties())) {
                RoglerTeamWarsLogger.info("FTB-Teamerstellung abgelehnt: Teamanzahllimit erreicht");
                throw denied("Teamanzahllimit erreicht (" + rules.maxParties() + ")");
            }
        } catch (ReflectiveOperationException failure) {
            throw unavailable(failure);
        }
    }

    public static void checkCapacity(Object party) throws CommandSyntaxException {
        var rules = RoglerTeamWarsMod.teamSettings();
        if (!rules.enforceTeamLimits()) return;
        try {
            checkServerThread((MinecraftServer)read(manager(), "getServer"));
            if (TeamManagementPolicy.atLimit(true, members(party).size(), rules.maxTeamMembers())) {
                RoglerTeamWarsLogger.info("FTB-Teamzugang abgelehnt: Teamgrößenlimit für " + id(party));
                throw denied("Teamgrößenlimit erreicht (" + rules.maxTeamMembers() + ")");
            }
        } catch (ReflectiveOperationException failure) {
            throw unavailable(failure);
        }
    }

    private static void checkServerThread(MinecraftServer server) throws CommandSyntaxException {
        if (server == null || !server.isSameThread()) throw denied("Teamaktion außerhalb des Serverthreads");
    }

    /** A UUID from an open menu must still match the actor's current FTB party. */
    public static int execute(ServerPlayer actor, Action action, UUID expectedParty, String argument)
        throws CommandSyntaxException {
        MinecraftServer server = actor.getServer();
        checkServerThread(server);
        UUID self = actor.getUUID();
        int now = server.getTickCount();
        Integer prior = LAST_ACTION.get(self);
        if (prior != null && now - prior < 10) throw denied("Bitte kurz warten");
        LAST_ACTION.put(self, now);
        try {
            int result = runAction(actor, action, expectedParty, argument);
            RoglerTeamWarsLogger.info("FTB-Teamaktion " + action + " von " + self + ": " + (result > 0 ? "OK" : "ohne Änderung"));
            return result;
        } catch (CommandSyntaxException failure) {
            RoglerTeamWarsLogger.info("FTB-Teamaktion " + action + " von " + self + ": abgelehnt");
            throw failure;
        } catch (ReflectiveOperationException failure) {
            throw unavailable(failure);
        }
    }

    private static int runAction(ServerPlayer actor, Action action, UUID expectedParty, String argument)
        throws ReflectiveOperationException, CommandSyntaxException {
        Object manager = manager();
        UUID self = actor.getUUID();
        Object own = ownParty(manager, self);
        UUID current = own == null ? NO_PARTY : id(own);
        if (expectedParty != null && !current.equals(expectedParty))
            throw denied("Dein Team hat sich geändert. Aktualisiere die Ansicht.");

        if (action == Action.JOIN) {
            if (own != null) throw denied("Verlasse zuerst dein aktuelles Team");
            UUID invitation = parseUuid(argument);
            Object destination = find(manager, "getTeamByID", invitation);
            if (!isParty(destination) || !TeamManagementPolicy.allowed(action, Role.NONE, rank(destination, self), 0))
                throw denied("Für dieses Team liegt keine Einladung vor");
            return (Integer)call(destination, "join", new Class<?>[]{ServerPlayer.class}, actor);
        }

        if (own == null) throw denied("Du bist in keinem FTB-Party-Team");
        Role actorRole = rank(own, self);
        if (action == Action.LEAVE) {
            if (!TeamManagementPolicy.allowed(action, actorRole, Role.NONE, members(own).size()))
                throw denied("Übertrage zuerst die Teamleitung");
            return (Integer)call(own, "leave", new Class<?>[]{UUID.class}, self);
        }

        if (action == Action.INVITE && !argument.matches("[A-Za-z0-9_]{1,16}"))
            throw denied("Gib einen gültigen Spielernamen ein");
        GameProfile target = action == Action.INVITE ? onlineTarget(actor, manager, argument) : memberTarget(manager, own, argument);
        Role targetRole = rank(own, target.getId());
        if (self.equals(target.getId()) || !TeamManagementPolicy.allowed(action, actorRole, targetRole, members(own).size()))
            throw denied("Diese Aktion ist mit den aktuellen Teamrollen nicht erlaubt");

        List<GameProfile> targets = List.of(target);
        return switch (action) {
            case INVITE, PROMOTE, DEMOTE -> (Integer)call(own, action.name().toLowerCase(Locale.ROOT),
                new Class<?>[]{ServerPlayer.class, Collection.class}, actor, targets);
            case KICK -> (Integer)call(own, "kick", new Class<?>[]{CommandSourceStack.class, Collection.class},
                actor.createCommandSourceStack(), targets);
            case TRANSFER -> (Integer)call(own, "transferOwnership", new Class<?>[]{CommandSourceStack.class, GameProfile.class},
                actor.createCommandSourceStack(), target);
            default -> throw new IllegalStateException("Unhandled team action");
        };
    }

    private static GameProfile onlineTarget(ServerPlayer actor, Object manager, String name)
        throws ReflectiveOperationException, CommandSyntaxException {
        ServerPlayer target = actor.getServer().getPlayerList().getPlayerByName(name);
        if (target == null) throw denied("Der Spieler muss für die Einladung online sein");
        Object effective = find(manager, "getTeamForPlayerID", target.getUUID());
        if (effective == null || isParty(effective)) throw denied("Der Spieler kann derzeit keinem Team beitreten");
        return target.getGameProfile();
    }

    private static GameProfile memberTarget(Object manager, Object party, String name)
        throws ReflectiveOperationException, CommandSyntaxException {
        UUID selected = name.length()==36 ? parseUuid(name) : null;
        if (selected==null && !name.matches("[A-Za-z0-9_]{1,16}"))
            throw denied("Gib einen gültigen Spielernamen oder eine Mitglieds-ID ein");
        GameProfile result = null;
        for (Object item : members(party)) {
            UUID member = (UUID)item;
            String memberName = playerName(manager, member);
            if (selected!=null ? !member.equals(selected) : !memberName.equalsIgnoreCase(name)) continue;
            if (result != null) throw denied("Der Spielername ist nicht eindeutig");
            Object effective = ownParty(manager, member);
            if (effective != null && id(effective).equals(id(party))) result = new GameProfile(member, memberName);
        }
        if (result == null) throw denied("Der Spieler gehört nicht mehr zu diesem Team");
        return result;
    }

    public record PartySummary(UUID id, String name, int members) {}

    public static List<PartySummary> parties() throws CommandSyntaxException {
        try {
            List<PartySummary> parties = new ArrayList<>();
            for (Object team : (Collection<?>)read(manager(), "getTeams")) {
                if (isParty(team)) parties.add(new PartySummary(id(team), name(team), members(team).size()));
            }
            return parties;
        } catch (ReflectiveOperationException failure) {
            throw unavailable(failure);
        }
    }

    public static List<Row> report(ServerPlayer viewer, UUID selectedTeam) throws CommandSyntaxException {
        if (selectedTeam != null && !viewer.createCommandSourceStack().hasPermission(2)) throw denied("Adminrechte erforderlich");
        try {
            Object manager = manager();
            Object party = selectedTeam == null ? ownParty(manager, viewer.getUUID()) : find(manager, "getTeamByID", selectedTeam);
            List<Row> rows = new ArrayList<>();
            if (!isParty(party)) {
                rows.add(new Row("FTB-Team", selectedTeam == null ? "Kein Party-Team" : "Team existiert nicht mehr"));
                if (selectedTeam == null) {
                    for (Object team : (Collection<?>)read(manager, "getTeams")) {
                        if (rows.size() == DiagnosticPolicy.MAX_ROWS - 1) break;
                        if (isParty(team) && rank(team, viewer.getUUID()) == Role.INVITED)
                            rows.add(new Row("Einladung", id(team) + " " + name(team)));
                    }
                }
                return rows;
            }

            UUID owner = (UUID)read(party, "getOwner");
            var rules = RoglerTeamWarsMod.teamSettings();
            int size = members(party).size();
            if (selectedTeam == null) {
                Role role=rank(party, viewer.getUUID());
                rows.add(new Row("Deine Rolle",roleName(role)));
                rows.add(new Row("Deine Aktionen",switch (role) {
                    case OWNER -> "Einladen, Rollen ändern, entfernen, Leitung übertragen, als letztes Mitglied austreten";
                    case OFFICER -> "Einladen, Mitglieder entfernen, austreten";
                    case MEMBER -> "Austreten";
                    default -> "Keine";
                }));
            }
            rows.add(new Row("FTB-Team", name(party)));
            rows.add(new Row("Teamleitung", playerName(manager, owner)));
            int ftbLimit = nativeLimit();
            String ftbSize = ftbLimit < 0 ? "unbekannt" : ftbLimit == 0 ? "offen" : Integer.toString(ftbLimit);
            rows.add(new Row("Mitglieder", size + " | FTB-Limit: " + ftbSize
                + (rules.enforceTeamLimits() ? " | Core: " + rules.maxTeamMembers() : "")));
            if ((rules.enforceTeamLimits() && size > rules.maxTeamMembers()) || (ftbLimit > 0 && size > ftbLimit))
                rows.add(new Row("Teamgröße", "Über dem Limit. Bestehende Mitglieder behalten ihren Platz."));
            if (selectedTeam != null) {
                int count = partyCount(manager, rules.maxParties() + 1);
                rows.add(new Row("Party-Limit", rules.enforceTeamLimits()
                    ? (count > rules.maxParties() ? "überschritten" : count + " / " + rules.maxParties())
                    : "Core-Durchsetzung aus"));
            }
            var sorted = new ArrayList<UUID>();
            for (Object member : members(party)) sorted.add((UUID)member);
            sorted.sort(Comparator.comparing((UUID member) -> !member.equals(owner)).thenComparing(Comparator.naturalOrder()));
            for (UUID member : sorted) {
                if (rows.size() == DiagnosticPolicy.MAX_ROWS - 1) {
                    rows.add(new Row("Mitglieder", "Weitere Mitglieder in der FTB-Teams-Ansicht"));
                    break;
                }
                boolean online = viewer.getServer().getPlayerList().getPlayer(member) != null;
                rows.add(new Row("Mitglied", DiagnosticPolicy.clean(playerName(manager, member), 40) + " · " + roleName(rank(party, member))
                    + (online ? " · online" : " · offline") + " · " + member));
            }
            return rows;
        } catch (ReflectiveOperationException failure) {
            throw unavailable(failure);
        }
    }

    private static int nativeLimit() {
        try {
            Object value = Class.forName("dev.ftb.mods.ftbteams.config.ServerConfig").getField("MAX_TEAM_SIZE").get(null);
            return ((Number)read(value, "get")).intValue();
        } catch (ReflectiveOperationException | CommandSyntaxException failure) {
            return -1;
        }
    }

    private static String roleName(Role role) {
        return switch (role) {
            case OWNER -> "Leitung";
            case OFFICER -> "Offizier";
            case MEMBER -> "Mitglied";
            case INVITED -> "Eingeladen";
            case NONE -> "Ohne Rolle";
        };
    }

    public static LiteralArgumentBuilder<CommandSourceStack> command() {
        var root = Commands.literal("manage");
        for (Action action : Action.values()) {
            var node = Commands.literal(action.name().toLowerCase(Locale.ROOT));
            if (action == Action.LEAVE) {
                node.executes(ctx -> execute(ctx.getSource().getPlayerOrException(), action, null, ""));
            } else {
                node.then(Commands.argument("ziel", StringArgumentType.word()).executes(ctx ->
                    execute(ctx.getSource().getPlayerOrException(), action, null, StringArgumentType.getString(ctx, "ziel"))));
            }
            root.then(node);
        }
        return root;
    }

    public static UUID parseUuid(String raw) throws CommandSyntaxException {
        try {
            UUID id = UUID.fromString(raw);
            if (id.toString().equalsIgnoreCase(raw)) return id;
        } catch (IllegalArgumentException ignored) {
        }
        throw denied("Ungültige Team-ID");
    }

    private static CommandSyntaxException unavailable(ReflectiveOperationException failure) {
        RoglerTeamWarsLogger.warn("FTB-Teamverwaltung: " + Objects.toString(failure.getCause(), failure.toString()));
        return denied("FTB-Teams-Schnittstelle nicht verfügbar");
    }

    private static CommandSyntaxException denied(String message) {
        return new SimpleCommandExceptionType(Component.literal(message)).create();
    }
}
