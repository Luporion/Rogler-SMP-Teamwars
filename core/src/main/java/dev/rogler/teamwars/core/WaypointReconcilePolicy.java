package dev.rogler.teamwars.core;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Conservative, read-only comparison of two PRIVATE waypoint stores. No mutation and
 * no network transport. Coordinates have no durable point identity: name changes
 * at another position are deliberately treated as possible moves/deletes.
 */
public final class WaypointReconcilePolicy {
    private WaypointReconcilePolicy() {}
    public enum Kind { SAME, METADATA_CONFLICT, POSSIBLE_MOVE, AMBIGUOUS, FTB_ONLY, XAERO_ONLY }
    public record Difference(Kind kind, WaypointSyncPolicy.Point ftb, WaypointSyncPolicy.Point xaero) {}
    public record Report(List<Difference> rows, int same, int metadata, int possibleMoves, int ambiguous,
                         int ftbOnly, int xaeroOnly) {
        public Report { rows = List.copyOf(rows); }
        public int conflicts() { return metadata + possibleMoves + ambiguous; }
    }
    private static String nameKey(WaypointSyncPolicy.Point p) {
        return p.dimension() + ":" + p.name().strip().toLowerCase(Locale.ROOT);
    }
    private static Map<String,List<WaypointSyncPolicy.Point>> index(Collection<WaypointSyncPolicy.Point> points, boolean location) {
        Map<String,List<WaypointSyncPolicy.Point>> map = new HashMap<>();
        for (var p : points) map.computeIfAbsent(location ? p.locationKey() : nameKey(p), k -> new ArrayList<>()).add(p);
        return map;
    }
    public static Report compare(Collection<WaypointSyncPolicy.Point> ftb, Collection<WaypointSyncPolicy.Point> xaero) {
        var fLoc = index(ftb, true); var xLoc = index(xaero, true);
        var fNames = index(ftb, false); var xNames = index(xaero, false);
        List<Difference> rows = new ArrayList<>();
        int same=0, metadata=0, moves=0, ambiguous=0, onlyF=0, onlyX=0;
        Set<String> all = new java.util.TreeSet<>(fLoc.keySet()); all.addAll(xLoc.keySet());
        for (String key : all) {
            var f = fLoc.getOrDefault(key, List.of()); var x = xLoc.getOrDefault(key,List.of());
            var fp=f.isEmpty()?null:f.get(0); var xp=x.isEmpty()?null:x.get(0);
            Kind k;
            if (f.size()>1 || x.size()>1) {k=Kind.AMBIGUOUS;ambiguous++;}
            else if (fp != null && xp != null) {
                k=fp.name().equals(xp.name()) && fp.color()==xp.color() ? Kind.SAME : Kind.METADATA_CONFLICT;
                if(k==Kind.SAME)same++;else metadata++;
            } else if(fp!=null) {
                k=xNames.containsKey(nameKey(fp))?Kind.POSSIBLE_MOVE:Kind.FTB_ONLY;
                if(k==Kind.POSSIBLE_MOVE)moves++;else onlyF++;
            } else {
                k=fNames.containsKey(nameKey(xp))?Kind.POSSIBLE_MOVE:Kind.XAERO_ONLY;
                if(k==Kind.POSSIBLE_MOVE)moves++;else onlyX++;
            }
            rows.add(new Difference(k,fp,xp));
        }
        return new Report(rows,same,metadata,moves,ambiguous,onlyF,onlyX);
    }
    /** Only new, non-ambiguous coordinates with no likely moved/renamed names are imported. */
    public static List<WaypointSyncPolicy.Point> safeMissing(Collection<WaypointSyncPolicy.Point> source,
                                                              Collection<WaypointSyncPolicy.Point> target) {
        var srcLoc=index(source,true); var dstLoc=index(target,true);
        var srcNames=index(source,false); var dstNames=index(target,false);
        List<WaypointSyncPolicy.Point> result=new ArrayList<>();
        Set<String> added=new HashSet<>();
        for(var point:source) {
            if(result.size()>=256)break;
            if(srcLoc.get(point.locationKey()).size()!=1 || dstLoc.containsKey(point.locationKey()))continue;
            // If a name exists elsewhere in either store, we cannot prove this isn't a moved/deleted point.
            if(srcNames.get(nameKey(point)).size()!=1 || dstNames.containsKey(nameKey(point)))continue;
            if(added.add(point.locationKey()))result.add(point);
        }
        return List.copyOf(result);
    }
    /** Optimistic concurrency fingerprint: source side + both current values. */
    public static String token(String source, WaypointSyncPolicy.Point sourcePoint, WaypointSyncPolicy.Point targetPoint) {
        if (!source.equals("ftb") && !source.equals("xaero"))throw new IllegalArgumentException("invalid source");
        if(sourcePoint==null||targetPoint==null||!sourcePoint.locationKey().equals(targetPoint.locationKey()))
            throw new IllegalArgumentException("target mismatch");
        String material=source+"|"+sourcePoint+"|"+targetPoint;
        try {
            byte[] digest=MessageDigest.getInstance("SHA-256").digest(material.getBytes(StandardCharsets.UTF_8));
            StringBuilder b=new StringBuilder();for(int i=0;i<8;i++)b.append(String.format("%02x",digest[i]&255));
            return b.toString();
        }catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}
    }
    public static boolean canReplaceMetadata(List<WaypointSyncPolicy.Point> source,
                                             List<WaypointSyncPolicy.Point> target,
                                             WaypointSyncPolicy.Point selected) {
        if (selected == null) return false;
        if(source.stream().filter(p->p.locationKey().equals(selected.locationKey())).count()!=1)return false;
        if(target.stream().filter(p->p.locationKey().equals(selected.locationKey())).count()!=1)return false;
        // Duplicate names at different positions are ambiguous and must never be overwritten.
        if(source.stream().anyMatch(p->!p.locationKey().equals(selected.locationKey()) && nameKey(p).equals(nameKey(selected))))return false;
        return target.stream().noneMatch(p->!p.locationKey().equals(selected.locationKey()) && nameKey(p).equals(nameKey(selected)));
    }
}
