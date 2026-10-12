package dev.rogler.teamwars.neoforge.raid;

import dev.rogler.teamwars.core.base.RaidPolicy;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.*;

/** World-persistent raid windows and team scores. All mutation on the server thread. */
public final class RaidSavedData extends SavedData {
    private static final String NAME="roglerteamwars_raids";
    private final Map<UUID,RaidPolicy.Window> windows=new HashMap<>();
    private final Map<UUID,Integer> scores=new HashMap<>();
    public RaidPolicy.Window raid(UUID defender){return windows.get(defender);}
    public Collection<RaidPolicy.Window> raids(){return List.copyOf(windows.values());}
    public Map<UUID,Integer> scores(){return Map.copyOf(scores);}
    public void open(RaidPolicy.Window r){
        if(windows.containsKey(r.defender()))throw new IllegalStateException("Raid already open");
        windows.put(r.defender(),r);setDirty();
    }
    public void update(RaidPolicy.Window r){
        if(!windows.containsKey(r.defender()))throw new IllegalStateException("Raid no longer active");
        windows.put(r.defender(),r);setDirty();
    }
    public void close(UUID team){if(windows.remove(team)!=null)setDirty();}
    public void score(UUID winner){scores.merge(winner,1,Integer::sum);setDirty();}
    public static RaidSavedData get(MinecraftServer server){
        return server.overworld().getDataStorage().computeIfAbsent(
            new SavedData.Factory<>(RaidSavedData::new,(tag,registry)->load(tag)),NAME);
    }
    private static RaidSavedData load(CompoundTag tag){
        RaidSavedData result=new RaidSavedData();
        for(Tag e:tag.getList("Windows",Tag.TAG_COMPOUND)){
            CompoundTag row=(CompoundTag)e;
            try{
                UUID defender=row.getUUID("Defender");
                UUID attacker=row.hasUUID("Attacker")?row.getUUID("Attacker"):null;
                RaidPolicy.Window w=new RaidPolicy.Window(defender,attacker,row.getLong("Until"),
                    row.getInt("Hp"),row.getLong("LastHit"));
                result.windows.put(defender,w);
            }catch(IllegalArgumentException ex){ /* corrupt raid ignored, fail closed */ }
        }
        for(Tag e:tag.getList("Scores",Tag.TAG_COMPOUND)){
            CompoundTag row=(CompoundTag)e;
            if(row.hasUUID("Team") && row.getInt("Points")>=0)result.scores.put(row.getUUID("Team"),row.getInt("Points"));
        }
        return result;
    }
    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registry){
        ListTag list=new ListTag();
        for(RaidPolicy.Window w:windows.values()){
            CompoundTag row=new CompoundTag();row.putUUID("Defender",w.defender());
            if(w.attacker()!=null)row.putUUID("Attacker",w.attacker());
            row.putLong("Until",w.untilMillis());row.putInt("Hp",w.hp());row.putLong("LastHit",w.lastHitMillis());list.add(row);
        }
        tag.put("Windows",list);
        ListTag scoreRows=new ListTag();
        for(var entry:scores.entrySet()){
            CompoundTag row=new CompoundTag();row.putUUID("Team",entry.getKey());row.putInt("Points",entry.getValue());scoreRows.add(row);
        }
        tag.put("Scores",scoreRows);tag.putInt("Schema",1);return tag;
    }
}
