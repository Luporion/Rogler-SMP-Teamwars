package dev.rogler.teamwars.core.compat;

import java.io.StringReader;

public final class SableSplitConfigAuditTests {
    private static int checks;
    private static void eq(String input,SableSplitConfigAudit.State expected,String steps) throws Exception {
        checks++;
        var r=SableSplitConfigAudit.inspect(new StringReader(input));
        if(r.state()!=expected||!r.heatmapSteps().equals(steps))throw new AssertionError("split audit: "+r);
    }
    public static void main(String[] ignored) throws Exception {
        eq("sub_level_splitting = true\n".replace("\\n","\n"),SableSplitConfigAudit.State.ENABLED,"unavailable");
        eq("#sub_level_splitting=false\nsub_level_splitting=true # yes",SableSplitConfigAudit.State.ENABLED,"unavailable");
        eq("sub_level_splitting = false",SableSplitConfigAudit.State.DISABLED,"unavailable");
        eq("sub_level_splitting = YES",SableSplitConfigAudit.State.INVALID,"unavailable");
        eq("",SableSplitConfigAudit.State.MISSING,"unavailable");
        eq("sub_level_splitting = true\nsub_level_splitting=false",SableSplitConfigAudit.State.AMBIGUOUS,"unavailable");
        eq("sub_level_splitting=true\nsub_level_splitting_heatmap_steps=200",SableSplitConfigAudit.State.ENABLED,"200");
        eq("sub_level_splitting=true\nsub_level_splitting_heatmap_steps=garbage",SableSplitConfigAudit.State.ENABLED,"invalid");
        eq("other_setting=true",SableSplitConfigAudit.State.MISSING,"unavailable");
        System.out.println("v0.6.3 SableSplitConfigAuditTests: "+checks+" checks OK");
    }
}
