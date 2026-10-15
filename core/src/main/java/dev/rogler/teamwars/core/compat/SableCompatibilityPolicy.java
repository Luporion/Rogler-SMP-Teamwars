package dev.rogler.teamwars.core.compat;

import java.util.List;
import java.util.Locale;

/** Pure, conservative readiness assessment: observations never enable foreign hooks. */
public final class SableCompatibilityPolicy {
    private SableCompatibilityPolicy() {}
    public record Mod(String id, String displayName, String version) {
        public Mod {
            id = id == null ? "" : id.trim().toLowerCase(Locale.ROOT);
            displayName = displayName == null ? "" : displayName.trim();
            version = version == null ? "?" : version.trim();
        }
    }
    public record Finding(String status, String detail, boolean warning) {}

    public static Mod exact(List<Mod> mods, String id) {
        return mods.stream().filter(m -> m.id().equals(id)).findFirst().orElse(null);
    }

    /** Display-name fallback is informative only; it may not activate a dependency. */
    public static Mod byName(List<Mod> mods, String needle) {
        String lower = needle.toLowerCase(Locale.ROOT);
        return mods.stream().filter(m -> m.displayName().toLowerCase(Locale.ROOT).contains(lower))
            .findFirst().orElse(null);
    }

    public static int[] majorMinorPatch(String version) {
        if (version == null) return new int[] {-1,-1,-1};
        String v = version.trim();
        if (v.startsWith("v")) v = v.substring(1);
        String[] parts = v.split("[.+\\-]",4);
        if (parts.length < 3) return new int[]{-1,-1,-1};
        try {
            return new int[] {Integer.parseInt(parts[0]),Integer.parseInt(parts[1]),Integer.parseInt(parts[2])};
        } catch (NumberFormatException ex) {return new int[]{-1,-1,-1};}
    }
    public static boolean atLeast(String actual,int major,int minor,int patch) {
        int[] v=majorMinorPatch(actual);
        if(v[0]<0)return false;
        if(v[0]!=major)return v[0]>major;
        if(v[1]!=minor)return v[1]>minor;
        return v[2]>=patch;
    }
    public static Finding assess(List<Mod> mods, SableSplitConfigAudit.State splitting) {
        Mod sable=exact(mods,"sable");
        Mod destructive=exact(mods,"sabledestructive");
        // Name fallback must NOT be trusted as a loaded dependency / permission to invoke API.
        boolean collision=mods.stream().anyMatch(m -> m.id().contains("collisiondamage")
            || m.displayName().toLowerCase(Locale.ROOT).contains("sable: collision damage"));
        if(splitting==SableSplitConfigAudit.State.DISABLED)
            return new Finding("WARN","sub_level_splitting=false: User requires true; adjust manually before testing",true);
        if(splitting==SableSplitConfigAudit.State.AMBIGUOUS)
            return new Finding("WARN","Multiple/conflicting splitting properties: config requires manual audit",true);
        if(splitting==SableSplitConfigAudit.State.INVALID)
            return new Finding("WARN","Splitting config unreadable/invalid: DO NOT modify automatically",true);
        if(sable==null && destructive!=null)
            return new Finding("WARN","Sable: Destructive registered without exact 'sable' mod ID; verify installed dependencies",true);
        if(sable==null)
            return new Finding("ABSENT","Sable not confirmed via exact mod ID; no Sable hooks installed",false);
        if(!atLeast(sable.version(),2,0,3))
            return new Finding("WARN","Sable version unknown/older than 2.0.3; inspect compatibility",true);
        if(destructive==null)
            return new Finding("OBSERVE","Sable present but Destructive not confirmed; removed-sublevel fixes not verified",true);
        if(!atLeast(destructive.version(),2,2,0))
            return new Finding("WARN","Sable: Destructive older/unknown vs target 2.2.0; test split regression",true);
        if(collision)
            return new Finding("A/B_TEST","Destructive + collision add-on detected; check duplicate damage; no automatic disabling",true);
        if(splitting!=SableSplitConfigAudit.State.ENABLED)
            return new Finding("CHECK_CONFIG","Sable/Destructive present; splitting not confirmed true; read-only audit",true);
        return new Finding("READY_TO_TEST","Sable and Destructive versions appear sufficient; NOT runtime-verified",false);
    }
}
