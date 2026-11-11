package dev.rogler.teamwars.core.ui;

import java.util.List;
import java.util.UUID;

/** Bounded diagnostic snapshots; no Minecraft or third-party API dependencies. */
public final class DiagnosticPolicy {
    private DiagnosticPolicy() {}
    public static final int MAX_ROWS=96, MAX_PAGE=8, MAX_CLAIM_SCAN=512, MAX_TEAM_SCAN=128;
    public record Row(String label,String value) {
        public Row {
            if(label==null || value==null)throw new IllegalArgumentException("Missing diagnostic text");
            label=clean(label,48);value=clean(value,180);
        }
    }
    public record Request(String kind,UUID target,int offset,int limit) {}
    public static Request parse(String kind,String value,int permission,UUID ownTeam) {
        if(kind==null || value==null || value.length()>96)throw new IllegalArgumentException("Ungueltige Diagnose");
        if(!List.of("own","claims","action").contains(kind))throw new IllegalArgumentException("Unbekannte Diagnose");
        if(!kind.equals("own") && permission<2)throw new IllegalArgumentException("Adminrechte erforderlich");
        String[] parts=value.split("\\|",-1);
        int expected=kind.equals("claims")?3:2;
        if(parts.length!=expected)throw new IllegalArgumentException("Ungueltige Diagnoseparameter");
        UUID target=kind.equals("claims")?UUID.fromString(parts[0]):kind.equals("own")?ownTeam:null;
        int offset=Integer.parseInt(parts[expected-2]),limit=Integer.parseInt(parts[expected-1]);
        if(offset<0 || offset>=MAX_ROWS || limit<1 || limit>MAX_PAGE)
            throw new IllegalArgumentException("Ungueltige Diagnoseseite");
        return new Request(kind,target,offset,limit);
    }
    public static List<Row> page(List<Row> rows,int offset,int limit) {
        if(rows.size()>MAX_ROWS || offset<0 || offset>=MAX_ROWS || limit<1 || limit>MAX_PAGE)
            throw new IllegalArgumentException("Diagnostic page out of bounds");
        return List.copyOf(rows.subList(Math.min(offset,rows.size()),Math.min(offset+limit,rows.size())));
    }
    public static String clean(String text,int max) {
        if(text==null)return "unbekannt";
        String s=text.replaceAll("[\\p{Cntrl}\\u00a7]"," ");
        return s.substring(0,Math.min(max,s.length()));
    }
    public static String yesNo(Boolean value) {return value==null?"unbekannt":value?"ja":"nein";}
    public static String count(Integer value) {return value==null || value<0?"unbekannt":value.toString();}
    public static String remaining(Integer used,Integer max) {
        return used==null || max==null || used<0 || max<0?"unbekannt":Integer.toString(Math.max(0,max-used));
    }
}
