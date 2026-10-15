package dev.rogler.teamwars.core.compat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Locale;

/** Read-only TOML line scanner. Never changes sub_level_splitting or the heatmap. */
public final class SableSplitConfigAudit {
    private SableSplitConfigAudit() {}
    public enum State { ENABLED, DISABLED, MISSING, INVALID, AMBIGUOUS }
    public record Result(State state, String heatmapSteps, int occurrences) {}
    public static Result inspect(Reader input) throws IOException {
        BufferedReader r = input instanceof BufferedReader br?br:new BufferedReader(input);
        Boolean splitting=null;
        int n=0;
        String heatmap="unavailable";
        for(int lineNumber=0;lineNumber<4096;lineNumber++) {
            String line=r.readLine();
            if(line==null)break;
            String normalized=line.split("#",2)[0].trim();
            int equals=normalized.indexOf('=');
            if(equals<=0)continue;
            String key=normalized.substring(0,equals).trim().toLowerCase(Locale.ROOT);
            String value=normalized.substring(equals+1).trim().toLowerCase(Locale.ROOT);
            if(key.equals("sub_level_splitting")) {
                n++;
                if(!value.equals("true")&&!value.equals("false"))return new Result(State.INVALID,heatmap,n);
                splitting=Boolean.parseBoolean(value);
            } else if(key.equals("sub_level_splitting_heatmap_steps")) {
                if(value.matches("[0-9]{1,7}"))heatmap=value;
                else heatmap="invalid";
            }
        }
        if(n>1)return new Result(State.AMBIGUOUS,heatmap,n);
        if(n==0)return new Result(State.MISSING,heatmap,0);
        return new Result(Boolean.TRUE.equals(splitting)?State.ENABLED:State.DISABLED,heatmap,n);
    }
}
