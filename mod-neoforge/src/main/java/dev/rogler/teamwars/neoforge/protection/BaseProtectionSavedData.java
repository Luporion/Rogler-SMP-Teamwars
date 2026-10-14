package dev.rogler.teamwars.neoforge.protection;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.*;

/** Stores per-team grace and exact FTB claims created by TeamWars, not user claims. */
public final class BaseProtectionSavedData extends SavedData {
    public static final String NAME = "roglerteamwars_protection";
    public static final class Record {
        public final UUID team;
        public long graceUntilMillis;
        public int armorLevel;
        /** Actual placed core axis for renderer/kinetics after safe repair; defaults to X on old worlds. */
        public String coreAxis="x";
        public final Set<Long> createdClaims = new HashSet<>();
        Record(UUID team, long graceUntilMillis) { this.team=team;this.graceUntilMillis=graceUntilMillis; }
    }
    private final Map<UUID,Record> teams = new HashMap<>();
    public Collection<Record> all() { return Collections.unmodifiableCollection(teams.values()); }
    public Record team(UUID id) { return teams.get(id); }
    public Record ensure(UUID id,long now,int hours) {
        Record result = teams.get(id);
        if (result == null) {
            long grace = Math.addExact(now, hours * 3600000L);
            result=new Record(id,grace); teams.put(id,result);setDirty();
        }
        return result;
    }
    /** Transfer grace without refreshing it. Claimed chunks must be dealt with separately. */
    public void transferOrphanRecord(UUID from, UUID to) {
        if (from.equals(to)) throw new IllegalStateException("Same team UUID");
        Record previous = teams.get(from), next = teams.get(to);
        if (previous != null && !previous.createdClaims.isEmpty())
            throw new IllegalStateException("Old TeamWars-managed FTB claims must be audited before adoption");
        if (next != null && !next.createdClaims.isEmpty())
            throw new IllegalStateException("Destination has existing TeamWars-managed claims");
        // Never grant a fresh 24h timer when an orphan never had a protection record.
        long until = previous == null ? (next == null ? 0L : next.graceUntilMillis)
            : next == null ? previous.graceUntilMillis
            : Math.min(previous.graceUntilMillis, next.graceUntilMillis);
        Record adopted = new Record(to, until);
        // Paid core armor belongs to the persistent TeamWars identity. Do not lose
        // upgrades when an administrator explicitly adopts a confirmed orphan.
        adopted.coreAxis = previous == null ? (next == null ? "x" : next.coreAxis) : previous.coreAxis;
        adopted.armorLevel = Math.max(previous == null ? 0 : previous.armorLevel,
            next == null ? 0 : next.armorLevel);
        teams.put(to, adopted);
        teams.remove(from);
        setDirty();
    }

    public void setCoreAxis(UUID id,String axis) {
        if(!axis.equals("x") && !axis.equals("y") && !axis.equals("z"))
            throw new IllegalArgumentException("Invalid axis");
        Record r=ensure(id,0,0);
        if(!r.coreAxis.equals(axis)){r.coreAxis=axis;setDirty();}
    }
    public void setGrace(UUID id,long until) {
        Record r=ensure(id,0,0);r.graceUntilMillis=until;setDirty();
    }
    public boolean upgradeArmor(UUID team, int expectedOldLevel) {
        Record r=teams.get(team);
        if (r==null || r.armorLevel!=expectedOldLevel || expectedOldLevel<0 || expectedOldLevel>=2) return false;
        r.armorLevel++; setDirty(); return true;
    }
    public void changed() { setDirty(); }
    public static long key(int chunkX,int chunkZ) {
        return ((long)chunkX << 32) | (chunkZ & 0xffffffffL);
    }
    public static int x(long key) { return (int)(key>>32); }
    public static int z(long key) { return (int)key; }
    public static BaseProtectionSavedData get(MinecraftServer s) {
        return s.overworld().getDataStorage().computeIfAbsent(
            new SavedData.Factory<>(BaseProtectionSavedData::new, (tag,registry)->load(tag)),NAME);
    }
    private static BaseProtectionSavedData load(CompoundTag tag) {
        BaseProtectionSavedData data=new BaseProtectionSavedData();
        for(Tag rowTag:tag.getList("Teams",Tag.TAG_COMPOUND)) {
            CompoundTag row=(CompoundTag)rowTag;
            UUID id=row.getUUID("Id");Record r=new Record(id,row.getLong("GraceUntil"));
            r.armorLevel=Math.max(0,Math.min(2,row.getInt("ArmorLevel")));
            String axis=row.getString("CoreAxis");
            r.coreAxis=(axis.equals("x")||axis.equals("y")||axis.equals("z"))?axis:"x";
            for(long key:row.getLongArray("FTBClaims"))r.createdClaims.add(key);
            data.teams.put(id,r);
        }
        return data;
    }
    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registry) {
        ListTag list=new ListTag();
        for(Record r:teams.values()) {
            CompoundTag e=new CompoundTag();e.putUUID("Id",r.team);
            e.putLong("GraceUntil",r.graceUntilMillis);e.putInt("ArmorLevel",r.armorLevel);e.putString("CoreAxis",r.coreAxis);
            e.putLongArray("FTBClaims",r.createdClaims.stream().mapToLong(Long::longValue).toArray());
            list.add(e);
        }
        tag.putInt("Schema",1);tag.put("Teams",list);return tag;
    }
}
