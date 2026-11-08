package dev.rogler.teamwars.core.ui;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Server-side allowlist: never accept command text from the Minecraft client. */
public final class AdminActionPolicy {
    private AdminActionPolicy() {}
    public record Action(String id, String label, String command, String argument, boolean dangerous, int permission) {
        public Action {
            if (!id.matches("[a-z]{2,24}") || (permission != 2 && permission != 4)
                || !List.of("", "PLAYER", "TEAM", "PLAYER_MIN", "TEAM_MIN", "TEAM_PLAYER").contains(argument))
                throw new IllegalArgumentException("Invalid administrative action");
        }
    }
    public static final List<Action> ACTIONS=List.of(
        new Action("teams", "Alle Teams", "teams", "", false, 2),
        new Action("corelist", "Registrierte Cores", "corelist", "", false, 2),
        new Action("orphanlist", "Verwaiste Cores", "orphanlist", "", false, 2),
        new Action("claimaudit", "Claim-Audit", "claimaudit", "TEAM", false, 2),
        new Action("graceteam", "Grace fuer Team", "graceteam", "TEAM_MIN", true, 2),
        new Action("raidstartteam", "Team-Raid starten", "raidstartteam", "TEAM_MIN", true, 4),
        new Action("raidstopteam", "Team-Raid beenden", "raidstopteam", "TEAM", true, 4),
        new Action("raidstart", "Spieler-Raid starten", "raidstart", "PLAYER_MIN", true, 4),
        new Action("raidstop", "Spieler-Raid beenden", "raidstop", "PLAYER", true, 4),
        new Action("grace", "Spieler-Grace", "grace", "PLAYER_MIN", true, 2),
        new Action("syncclaims", "FTB-Claims synchronisieren", "syncclaims", "PLAYER", true, 4),
        new Action("issuecore", "Core ausgeben", "issuecore", "PLAYER", true, 4),
        new Action("recovercore", "Core wiederherstellen", "recovercore", "PLAYER", true, 4),
        new Action("salvagemissing", "Core retten", "salvagemissing", "PLAYER", true, 4),
        new Action("reclaimlost", "Core-Token widerrufen", "reclaimlost", "PLAYER", true, 4),
        new Action("reclaimlostteam", "Team-Core widerrufen", "reclaimlostteam", "TEAM", true, 4),
        new Action("adoptcore", "Verwaisten Core zuordnen", "adoptcore", "TEAM_PLAYER", true, 4)
    );
    public static final List<Action> DIAGNOSTICS=List.of(
        new Action("diagnostics", "FTB-Adapter", "diagnostics", "", false, 2),
        new Action("resync", "Scoreboard neu synchronisieren", "resync", "", false, 2),
        new Action("coreaudit", "Core-Integritaetsaudit", "coreaudit", "", false, 2),
        new Action("corerepair", "Core-Reparatur", "corerepair apply", "", true, 4),
        new Action("explosionstats", "Explosionstatistik", "explosionstats", "", false, 4),
        new Action("reload", "Core-Konfig neu laden", "reload", "", true, 2),
        new Action("compatreport", "Kompatibilitaetsbericht", "compat report", "", false, 4),
        new Action("compatsable", "Sable-Bericht", "compat sable", "", false, 4),
        new Action("compatmods", "Modliste", "compat mods", "", false, 4),
        new Action("compatexplosions", "Sable-Explosionen", "compat explosions", "", false, 4),
        new Action("compatlifecycle", "Sable-Lifecycle", "compat lifecycle", "", false, 4),
        new Action("compatreload", "Sable-Konfig neu laden", "compat reload", "", true, 4),
        new Action("wariumstatus", "Warium-Status", "warium status", "", false, 4),
        new Action("wariumlist", "Warium-Eintraege", "warium list", "", false, 4),
        new Action("wariumcandidates", "Warium-Kandidaten", "warium candidates", "", false, 4),
        new Action("wariumreload", "Warium-Konfig neu laden", "warium reload", "", true, 4)
    );
    private static final Map<String, Action> BY_ID=build();
    private static Map<String,Action> build(){
        var result=new LinkedHashMap<String,Action>();
        for (Action action: ACTIONS) if(result.put(action.id(),action)!=null) throw new IllegalStateException("Duplicate action");
        for (Action action: DIAGNOSTICS) if(result.put(action.id(),action)!=null) throw new IllegalStateException("Duplicate action");
        return Map.copyOf(result);
    }
    public static Action require(String id){
        Action result=BY_ID.get(id);
        if(result==null)throw new IllegalArgumentException("Unbekannte Admin-Aktion");
        return result;
    }
    public static boolean allowed(int permission,Action action){return permission>=action.permission();}
    public static String command(String id,String value){
        Action a=require(id);
        if(value==null || value.length()>64)throw new IllegalArgumentException("Zu lange Eingabe");
        String[] parts=value.split("\\|",-1);
        String arguments=switch(a.argument()) {
            case "" -> {if(!value.isEmpty())throw new IllegalArgumentException("Unerwartete Parameter");yield "";}
            case "PLAYER" -> " "+player(value);
            case "TEAM" -> " "+team(value);
            case "PLAYER_MIN","TEAM_MIN" -> {
                if(parts.length!=2)throw new IllegalArgumentException("Ziel und Minuten erforderlich");
                int min;
                try{min=Integer.parseInt(parts[1]);}catch(NumberFormatException ex){throw new IllegalArgumentException("Ungueltige Minutenzahl");}
                if(min<0 || min>10080)throw new IllegalArgumentException("Minuten 0..10080");
                if(a.id().startsWith("raidstart") && (min<1 || min>30))throw new IllegalArgumentException("Raidfenster 1..30 Minuten");
                yield " "+(a.argument().startsWith("PLAYER")?player(parts[0]):team(parts[0]))+" "+min;
            }
            case "TEAM_PLAYER" -> {
                if(parts.length!=2)throw new IllegalArgumentException("Team und neuer Spieler erforderlich");
                yield " "+team(parts[0])+" "+player(parts[1]);
            }
            default -> throw new IllegalArgumentException("Unbekanntes Argumentschema");
        };
        return "teamwars admin "+a.command()+arguments;
    }
    private static String player(String raw){
        if(!raw.matches("[A-Za-z0-9_]{1,16}"))throw new IllegalArgumentException("Spielername erforderlich");
        return raw;
    }
    private static String team(String raw){
        try {return java.util.UUID.fromString(raw).toString();}
        catch(IllegalArgumentException ex){throw new IllegalArgumentException("Team-UUID erforderlich");}
    }
}
