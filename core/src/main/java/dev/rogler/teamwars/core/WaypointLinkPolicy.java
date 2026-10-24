package dev.rogler.teamwars.core;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Local-only opt-in link journal between two PERSONAL waypoint stores.
 * UUIDs are Rogler journal identities, NOT identities assigned by FTB or Xaero.
 * A missing or moved point is never implicitly recreated, moved, or deleted.
 */
public final class WaypointLinkPolicy {
    private WaypointLinkPolicy() {}
    public static final int MAX_LINKS = 128;
    public record Link(UUID id, WaypointSyncPolicy.Point ftbBaseline,
                       WaypointSyncPolicy.Point xaeroBaseline) {
        public Link {
            Objects.requireNonNull(id);
            Objects.requireNonNull(ftbBaseline);
            Objects.requireNonNull(xaeroBaseline);
            if (!ftbBaseline.locationKey().equals(xaeroBaseline.locationKey()))
                throw new IllegalArgumentException("links require identical dimensions and coordinates");
        }
        public String locationKey() { return ftbBaseline.locationKey(); }
    }
    public enum State {
        UNCHANGED, FTB_METADATA_CHANGED, XAERO_METADATA_CHANGED, CONVERGED,
        CONFLICT, MISSING_FTB, MISSING_XAERO, MISSING_BOTH, AMBIGUOUS
    }
    public record Review(Link link, State state, WaypointSyncPolicy.Point ftb,
                         WaypointSyncPolicy.Point xaero, String token) {
        public Review { Objects.requireNonNull(link); Objects.requireNonNull(state); }
        public boolean canApply() { return state == State.FTB_METADATA_CHANGED || state == State.XAERO_METADATA_CHANGED; }
    }
    private static Map<String,List<WaypointSyncPolicy.Point>> byLocation(Collection<WaypointSyncPolicy.Point> input) {
        Map<String,List<WaypointSyncPolicy.Point>> found = new HashMap<>();
        for (var point : input) found.computeIfAbsent(point.locationKey(), ignored -> new ArrayList<>()).add(point);
        return found;
    }
    public static boolean canLink(Collection<Link> existing, WaypointSyncPolicy.Point ftb,
                                  WaypointSyncPolicy.Point xaero,
                                  Collection<WaypointSyncPolicy.Point> ftbAll,
                                  Collection<WaypointSyncPolicy.Point> xaeroAll) {
        if (ftb == null || xaero == null || !ftb.locationKey().equals(xaero.locationKey())) return false;
        if (existing.size() >= MAX_LINKS || existing.stream().anyMatch(l -> l.locationKey().equals(ftb.locationKey()))) return false;
        return byLocation(ftbAll).getOrDefault(ftb.locationKey(),List.of()).size() == 1 &&
               byLocation(xaeroAll).getOrDefault(xaero.locationKey(),List.of()).size() == 1;
    }
    public static List<Review> review(Collection<Link> links,
                                      Collection<WaypointSyncPolicy.Point> ftbPoints,
                                      Collection<WaypointSyncPolicy.Point> xaeroPoints) {
        var ftb = byLocation(ftbPoints); var xaero = byLocation(xaeroPoints);
        List<Review> result = new ArrayList<>();
        for (var link : links) {
            var fs = ftb.getOrDefault(link.locationKey(), List.of());
            var xs = xaero.getOrDefault(link.locationKey(), List.of());
            WaypointSyncPolicy.Point f = fs.size()==1 ? fs.get(0) : null;
            WaypointSyncPolicy.Point x = xs.size()==1 ? xs.get(0) : null;
            State state;
            if (fs.size()>1 || xs.size()>1) state=State.AMBIGUOUS;
            else if (fs.isEmpty() && xs.isEmpty()) state=State.MISSING_BOTH;
            else if (fs.isEmpty()) state=State.MISSING_FTB;
            else if (xs.isEmpty()) state=State.MISSING_XAERO;
            else {
                boolean fChanged=!f.equals(link.ftbBaseline());
                boolean xChanged=!x.equals(link.xaeroBaseline());
                if (!fChanged && !xChanged) state=State.UNCHANGED;
                else if (f.equals(x)) state=State.CONVERGED;
                else if (fChanged && xChanged) state=State.CONFLICT;
                else state=fChanged ? State.FTB_METADATA_CHANGED : State.XAERO_METADATA_CHANGED;
            }
            result.add(new Review(link,state,f,x,fingerprint(link,state,f,x)));
        }
        return List.copyOf(result);
    }
    public static boolean safeToApply(Review r, String token) {
        return r.canApply() && r.token().equals(token) && r.ftb()!=null && r.xaero()!=null &&
            r.ftb().locationKey().equals(r.link().locationKey()) && r.xaero().locationKey().equals(r.link().locationKey());
    }
    /** A missing side is NOT proof of deletion. A human must explicitly confirm it.
     *  The surviving point must exactly equal the last trusted baseline. */
    public static boolean safeToDelete(Review r, String token) {
        if (token == null || !token.equals(r.token())) return false;
        return switch (r.state()) {
            case MISSING_FTB -> r.ftb() == null && r.link().xaeroBaseline().equals(r.xaero());
            case MISSING_XAERO -> r.xaero() == null && r.link().ftbBaseline().equals(r.ftb());
            default -> false;
        };
    }

    /** A move cannot be inferred safely: caller must select the moved waypoint.
     *  It must retain the original name and color, and be unique at its NEW location.
     *  Neither map may already contain another point at the new location.
     */
    public static boolean safeToMove(Review r, String token, String source, WaypointSyncPolicy.Point moved,
                                    Collection<WaypointSyncPolicy.Point> ftbAll,
                                    Collection<WaypointSyncPolicy.Point> xaeroAll,
                                    Collection<Link> existing) {
        if (token == null || !token.equals(r.token()) || moved == null || source == null) return false;
        WaypointSyncPolicy.Point original;
        if (source.equals("ftb") && r.state() == State.MISSING_FTB &&
            r.link().xaeroBaseline().equals(r.xaero())) original=r.link().ftbBaseline();
        else if (source.equals("xaero") && r.state() == State.MISSING_XAERO &&
            r.link().ftbBaseline().equals(r.ftb())) original=r.link().xaeroBaseline();
        else return false;
        if (!original.dimension().equals(moved.dimension()) || original.locationKey().equals(moved.locationKey()) ||
            !original.name().equals(moved.name()) || original.color()!=moved.color()) return false;
        if (existing.stream().anyMatch(l -> !l.id().equals(r.link().id()) && l.locationKey().equals(moved.locationKey()))) return false;
        return ftbAll.stream().filter(p->p.locationKey().equals(moved.locationKey())).count() == (source.equals("ftb")?1:0)
            && xaeroAll.stream().filter(p->p.locationKey().equals(moved.locationKey())).count() == (source.equals("xaero")?1:0);
    }

    /** Journal persists both real colors separately after a confirmed move. */
    public static Link moved(Link old, WaypointSyncPolicy.Point ftb, WaypointSyncPolicy.Point xaero) {
        if (!ftb.locationKey().equals(xaero.locationKey())) throw new IllegalArgumentException("move is not converged");
        return new Link(old.id(),ftb,xaero);
    }

    public static Link committed(Review r) {
        if (!r.canApply() && r.state()!=State.CONVERGED) throw new IllegalArgumentException("not resolved");
        WaypointSyncPolicy.Point winner = r.state()==State.XAERO_METADATA_CHANGED ? r.xaero() : r.ftb();
        if (r.state()==State.CONVERGED) winner=r.ftb();
        return new Link(r.link().id(),winner,winner);
    }
    /** A deterministic optimistic token prevents using an old preview after either store changes. */
    private static String fingerprint(Link link,State state,WaypointSyncPolicy.Point ftb,WaypointSyncPolicy.Point xaero) {
        String material=link.id()+"|"+link.ftbBaseline()+"|"+link.xaeroBaseline()+"|"+state+"|"+ftb+"|"+xaero;
        try {
            byte[] bytes=MessageDigest.getInstance("SHA-256").digest(material.getBytes(StandardCharsets.UTF_8));
            StringBuilder out=new StringBuilder();
            for (int i=0;i<10;i++) out.append(Character.forDigit((bytes[i]>>4)&15,16)).append(Character.forDigit(bytes[i]&15,16));
            return out.toString();
        } catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
}
