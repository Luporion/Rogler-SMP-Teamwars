package dev.rogler.teamwars.core.teams;

/** Authorization rules for the control center and its matching commands. */
public final class TeamManagementPolicy {
    private TeamManagementPolicy() {}

    public enum Role {
        NONE, INVITED, MEMBER, OFFICER, OWNER;

        public static Role fromFtb(String name) {
            return switch (name) {
                case "INVITED" -> INVITED;
                case "MEMBER" -> MEMBER;
                case "OFFICER" -> OFFICER;
                case "OWNER" -> OWNER;
                default -> NONE;
            };
        }

        public boolean member() {
            return ordinal() >= MEMBER.ordinal();
        }
    }

    public enum Action {
        INVITE, JOIN, LEAVE, KICK, PROMOTE, DEMOTE, TRANSFER
    }

    public static boolean allowed(Action action, Role actor, Role target, int members) {
        return switch (action) {
            case JOIN -> actor == Role.NONE && target == Role.INVITED;
            case INVITE -> actor.ordinal() >= Role.OFFICER.ordinal() && target == Role.NONE;
            case LEAVE -> actor.member() && (actor != Role.OWNER || members == 1);
            case KICK -> actor.ordinal() >= Role.OFFICER.ordinal() && target.member()
                && actor.ordinal() > target.ordinal();
            case PROMOTE -> actor == Role.OWNER && target == Role.MEMBER;
            case DEMOTE -> actor == Role.OWNER && target == Role.OFFICER;
            case TRANSFER -> actor == Role.OWNER && (target == Role.MEMBER || target == Role.OFFICER);
        };
    }

    public static boolean atLimit(boolean enforce, int current, int max) {
        return enforce && current >= max;
    }
}
