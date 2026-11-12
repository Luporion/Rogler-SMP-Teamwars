package dev.rogler.teamwars.core.teams;

import dev.rogler.teamwars.core.teams.TeamManagementPolicy.Action;
import dev.rogler.teamwars.core.teams.TeamManagementPolicy.Role;
import dev.rogler.teamwars.core.ui.DiagnosticPolicy;

import java.util.UUID;

import static dev.rogler.teamwars.core.teams.TeamManagementPolicy.allowed;

public final class TeamManagementPolicyTests {
    private static int checks;

    public static void main(String[] args) {
        check(allowed(Action.INVITE, Role.OFFICER, Role.NONE, 2));
        check(!allowed(Action.INVITE, Role.MEMBER, Role.NONE, 2));
        check(!allowed(Action.INVITE, Role.OWNER, Role.INVITED, 2));
        check(allowed(Action.JOIN, Role.NONE, Role.INVITED, 0));
        check(!allowed(Action.JOIN, Role.NONE, Role.NONE, 0));
        check(!allowed(Action.JOIN, Role.MEMBER, Role.INVITED, 2));
        check(!allowed(Action.LEAVE, Role.OWNER, Role.NONE, 2));
        check(allowed(Action.LEAVE, Role.OWNER, Role.NONE, 1));
        check(allowed(Action.LEAVE, Role.OFFICER, Role.NONE, 2));
        check(allowed(Action.KICK, Role.OFFICER, Role.MEMBER, 2));
        check(!allowed(Action.KICK, Role.OFFICER, Role.OFFICER, 2));
        check(!allowed(Action.KICK, Role.OWNER, Role.INVITED, 2));
        check(!allowed(Action.KICK, Role.MEMBER, Role.MEMBER, 2));
        check(allowed(Action.PROMOTE, Role.OWNER, Role.MEMBER, 2));
        check(!allowed(Action.PROMOTE, Role.OFFICER, Role.MEMBER, 2));
        check(allowed(Action.DEMOTE, Role.OWNER, Role.OFFICER, 2));
        check(!allowed(Action.DEMOTE, Role.OWNER, Role.OWNER, 2));
        check(allowed(Action.TRANSFER, Role.OWNER, Role.OFFICER, 2));
        check(!allowed(Action.TRANSFER, Role.OFFICER, Role.MEMBER, 2));
        check(!allowed(Action.TRANSFER, Role.OWNER, Role.NONE, 2));
        check(!TeamManagementPolicy.atLimit(false, 8, 4));
        check(!TeamManagementPolicy.atLimit(true, 3, 4));
        check(TeamManagementPolicy.atLimit(true, 4, 4));
        check(TeamManagementPolicy.atLimit(true, 5, 4));

        UUID own = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        check(DiagnosticPolicy.parse("team", "0|5", 0, own).target().equals(own));
        check(DiagnosticPolicy.parse("teamadm", other + "|0|5", 2, own).target().equals(other));
        reject(() -> DiagnosticPolicy.parse("teamadm", other + "|0|5", 0, own));
        reject(() -> DiagnosticPolicy.parse("team", other + "|0|5", 0, own));
        System.out.println("v0.8.9 TeamManagementPolicyTests: " + checks + " checks OK");
    }

    private static void check(boolean condition) {
        if (!condition) throw new AssertionError("Team management check " + checks);
        checks++;
    }

    private static void reject(Runnable action) {
        try {
            action.run();
            throw new AssertionError("Request should have been denied");
        } catch (IllegalArgumentException expected) {
            checks++;
        }
    }
}
