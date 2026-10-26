package dev.rogler.teamwars.core.ui;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Schema shared by the server configuration adapter and the GUI; pure Java, no Minecraft. */
public final class ControlCenterPolicy {
    private ControlCenterPolicy() {}
    public record Field(String group, String key, String label, int minimum, int maximum,
                        boolean booleanValue, int permission, String description) {
        public Field {
            if (group.isBlank() || key.isBlank() || label.isBlank() || (permission != 2 && permission != 4))
                throw new IllegalArgumentException("Invalid setting definition");
        }
        public String normalize(String raw) {
            if (raw == null || raw.length() > 32) throw new IllegalArgumentException("Value too long");
            String value = raw.trim();
            if (key.equals("warium_fusionblast_recovery")) {
                value = value.toUpperCase(java.util.Locale.ROOT);
                if (!List.of("OFF", "LOG_ONLY", "BLOCK_DISK_LOADED", "BLOCK_ALL_NEW").contains(value))
                    throw new IllegalArgumentException("Recovery-Modus: OFF, LOG_ONLY, BLOCK_DISK_LOADED, BLOCK_ALL_NEW");
                return value;
            }
            if (key.equals("warium_fusionblast_entity_id")) {
                if (!value.isEmpty() && !value.matches("wariumce:[a-z0-9_./-]+"))
                    throw new IllegalArgumentException("Entity-ID muss wariumce:<id> sein (oder leer)");
                return value;
            }
            if (booleanValue) {
                if (!value.equalsIgnoreCase("true") && !value.equalsIgnoreCase("false"))
                    throw new IllegalArgumentException(label + ": true/false required");
                return value.toLowerCase(java.util.Locale.ROOT);
            }
            int parsed;
            try { parsed = Integer.parseInt(value); }
            catch (NumberFormatException e) { throw new IllegalArgumentException(label + ": integer required"); }
            if (parsed < minimum || parsed > maximum)
                throw new IllegalArgumentException(label + ": allowed " + minimum + ".." + maximum);
            return Integer.toString(parsed);
        }
    }

    // All implemented Rogler Core *.properties settings; unrelated FTB mod settings stay FTB-owned.
    public static final List<Field> FIELDS = List.of(
        new Field("teams", "max_team_members", "Teamgroesse (Anzeige)", 1, 64, false, 2, "FTB max_party_size muss separat geaendert werden"),
        new Field("teams", "max_parties", "Teamanzahl (Anzeige)", 2, 32, false, 2, "Der Wert allein erzwingt kein FTB-Erstellungslimit"),
        new Field("teams", "refresh_interval_ticks", "Team-Sync-Ticks", 1, 1200, false, 2, "Interval fuer Scoreboard-Synchronisierung"),
        new Field("teams", "nametags_for_teammates_only", "Nur Team-Nametags", 0, 0, true, 2, "Fremde HUD-Mods separat pruefen"),
        new Field("teams", "friendly_fire", "Friendly Fire", 0, 0, true, 2, "FTB-/Vanilla-PvP-Regeln gesondert pruefen"),
        new Field("base", "min_radius", "Basis Mindestradius", 0, 30000000, false, 2, "Abstand vom Spawn; bestehende Basen nicht verschoben"),
        new Field("base", "max_radius", "Basis Maximalradius", 1, 30000000, false, 2, "Abstand vom Spawn; bestehende Basen nicht verschoben"),
        new Field("base", "carry_seconds", "Transportzeit (s)", 60, 86400, false, 2, "Keine automatische Verlaengerung alter Transporte"),
        new Field("protection", "claim_radius_chunks", "Autom. Claim-Radius", 0, 3, false, 4, "Keine fremden oder manuellen Claims entfernen"),
        new Field("protection", "new_team_grace_hours", "Neue Team-Grace (h)", 0, 168, false, 2, "Gilt fuer neu beginnende Grace, nicht rueckwirkend"),
        new Field("protection", "auto_ftb_claims", "Autom. FTB-Claims", 0, 0, true, 4, "Erst FTB Team-Claim-Privatsphaere pruefen"),
        new Field("protection", "base_guard_enabled", "Core Guard", 0, 0, true, 4, "Deaktivieren kann Basen gefaehrden"),
        new Field("protection", "raids_enabled", "Core-Raid-Freigabe", 0, 0, true, 4, "Raids erfordern weiterhin ein autorisiertes Raidfenster"),
        new Field("power", "base_fe_per_tick", "FE/t Stufe 0", 0, 256, false, 2, "0 <= Basis <= Stufe 1 <= Stufe 2"),
        new Field("power", "tier1_fe_per_tick", "FE/t Stufe 1", 0, 256, false, 2, "0 <= Basis <= Stufe 1 <= Stufe 2"),
        new Field("power", "tier2_fe_per_tick", "FE/t Stufe 2", 0, 256, false, 2, "0 <= Basis <= Stufe 1 <= Stufe 2"),
        new Field("warium", "warium_fusionblast_recovery", "Warium Recovery-Modus", 0, 0, false, 4, "OFF / LOG_ONLY / BLOCK_DISK_LOADED / BLOCK_ALL_NEW. Gefaehrlich; nur mit Backup!"),
        new Field("warium", "warium_fusionblast_entity_id", "Warium exakte Entity-ID", 0, 0, false, 4, "Leer oder wariumce:<entity>; kein breites Nuke-Matching"),
        new Field("sable", "explosion_observation_enabled", "Sable Explosion-Metriken", 0, 0, true, 4, "Nur Diagnose; Loescht/veraendert keine Sable-Schiffe"),
        new Field("lifecycle", "enabled", "Lifecycle-Schutz aktiviert", 0, 0, true, 4, "Sable-Fahrzeuglebenszyklus; Kompatibilitaet pruefen"),
        new Field("lifecycle", "cancel_tick_for_removed_owner", "Tick-Schutz", 0, 0, true, 4, "Verwaiste Sable-Sublevel Ticks abbrechen"),
        new Field("lifecycle", "cancel_split_for_removed_owner", "Split-Owner-Schutz", 0, 0, true, 4, "Unmoegliche Sable-Owner-Splits unterbinden"),
        new Field("lifecycle", "cancel_split_for_removed_anchor_plot", "Split-Anker-Schutz", 0, 0, true, 4, "Verwaiste Sable-Anker-Splits unterbinden"),
        new Field("lifecycle", "log_rate_limit_per_minute", "Lifecycle Warnungen/min", 0, 100, false, 4, "Logs begrenzen")
    );
    private static final Map<String, Field> INDEX = index();
    private static Map<String, Field> index() {
        var map = new LinkedHashMap<String, Field>();
        for (Field field : FIELDS) if (map.put(field.group() + "." + field.key(), field) != null)
            throw new IllegalStateException("Duplicate control center setting");
        return Map.copyOf(map);
    }
    public static Field require(String key) {
        var field = INDEX.get(key);
        if (field == null) throw new IllegalArgumentException("Unbekannte Einstellung");
        return field;
    }
    public static boolean allowed(int permission, Field field) { return permission >= field.permission(); }
    public static boolean sensitive(Field field) {
        return field.group().equals("warium") || field.group().equals("lifecycle") ||
            (field.group().equals("protection") &&
            (field.key().equals("base_guard_enabled") || field.key().equals("raids_enabled") ||
             field.key().equals("auto_ftb_claims") || field.key().equals("claim_radius_chunks")));
    }
}
