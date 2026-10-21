package dev.rogler.teamwars.neoforge.map;

import dev.rogler.teamwars.core.WaypointSyncPolicy;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** World-owned persistence; contains ONLY explicitly shared pins, never personal map content. */
public final class TeamWaypointSavedData extends SavedData {
    private static final String NAME = "roglerteamwars_shared_waypoints";
    private final List<WaypointSyncPolicy.Shared> entries = new ArrayList<>();
    public static TeamWaypointSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
            new SavedData.Factory<>(TeamWaypointSavedData::new, (tag, lookup) -> load(tag)), NAME);
    }
    private static TeamWaypointSavedData load(CompoundTag nbt) {
        if (nbt.getInt("Schema") > 1) throw new IllegalStateException("Unsupported shared-waypoint schema");
        TeamWaypointSavedData data = new TeamWaypointSavedData();
        for (Tag raw: nbt.getList("Pins",Tag.TAG_COMPOUND)) {
            CompoundTag r=(CompoundTag)raw;
            var point=new WaypointSyncPolicy.Point(r.getString("Dimension"),r.getInt("X"),
                r.getInt("Y"),r.getInt("Z"),r.getString("Name"),r.getInt("Color"));
            data.entries.add(new WaypointSyncPolicy.Shared(r.getUUID("ID"),r.getUUID("Team"),
                r.getUUID("Owner"),point));
        }
        return data;
    }
    public List<WaypointSyncPolicy.Shared> all(){return List.copyOf(entries);}
    public boolean add(UUID team, UUID owner, WaypointSyncPolicy.Point point) {
        long count=entries.stream().filter(e->e.team().equals(team)).count();
        for (int i=0;i<entries.size();i++) {
            var old=entries.get(i);
            if(old.team().equals(team)&&old.owner().equals(owner)&&old.point().locationKey().equals(point.locationKey())){
                if(old.point().equals(point))return true;
                entries.set(i,new WaypointSyncPolicy.Shared(old.id(),team,owner,point));setDirty();return true;
            }
        }
        if(count>=WaypointSyncPolicy.MAX_TEAM_WAYPOINTS)return false;
        entries.add(new WaypointSyncPolicy.Shared(UUID.randomUUID(),team,owner,point));setDirty();return true;
    }
    public boolean revoke(UUID id, UUID owner, UUID team){
        boolean removed=entries.removeIf(e->e.id().equals(id)&&e.owner().equals(owner)&&e.team().equals(team));
        if(removed)setDirty();return removed;
    }
    @Override public CompoundTag save(CompoundTag tag,HolderLookup.Provider registry){
        tag.putInt("Schema",1);ListTag pins=new ListTag();
        for(var e:entries){
            CompoundTag row=new CompoundTag();row.putUUID("ID",e.id());row.putUUID("Team",e.team());
            row.putUUID("Owner",e.owner());var p=e.point();
            row.putString("Dimension",p.dimension());row.putInt("X",p.x());row.putInt("Y",p.y());row.putInt("Z",p.z());
            row.putString("Name",p.name());row.putInt("Color",p.color());pins.add(row);
        }
        tag.put("Pins",pins);return tag;
    }
}
