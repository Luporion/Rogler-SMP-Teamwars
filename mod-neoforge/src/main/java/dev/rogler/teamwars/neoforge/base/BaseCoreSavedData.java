package dev.rogler.teamwars.neoforge.base;

import dev.rogler.teamwars.core.BaseZone;
import dev.rogler.teamwars.core.base.BaseCoreLedger;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.UUID;

/** Single authoritative save in overworld/data/roglerteamwars_basecores.dat. */
public final class BaseCoreSavedData extends SavedData {
    private static final String SAVE_NAME = "roglerteamwars_basecores";
    private final BaseCoreLedger ledger;

    private BaseCoreSavedData(BaseZone zone, int carrySeconds) {
        this.ledger = new BaseCoreLedger(zone, carrySeconds);
    }

    public static BaseCoreSavedData get(MinecraftServer server, BaseZone zone, int carrySeconds) {
        return server.overworld().getDataStorage().computeIfAbsent(
            new SavedData.Factory<>(() -> new BaseCoreSavedData(zone, carrySeconds),
                (tag, registry) -> load(tag, zone, carrySeconds)), SAVE_NAME);
    }

    public BaseCoreLedger ledger() { return ledger; }
    public void changed() { setDirty(); }

    private static BaseCoreSavedData load(CompoundTag tag, BaseZone zone, int carrySeconds) {
        BaseCoreSavedData store = new BaseCoreSavedData(zone, carrySeconds);
        for (Tag raw : tag.getList("Cores", Tag.TAG_COMPOUND)) {
            CompoundTag row = (CompoundTag) raw;
            UUID team = row.getUUID("Team"), token = row.getUUID("Token");
            BaseCoreLedger.Mode mode = BaseCoreLedger.Mode.valueOf(row.getString("Mode"));
            long elapsed = row.getLong("Elapsed");
            var location = readPosition(row, "Location");
            var lastSafe = readPosition(row, "LastSafe");
            // Crash recovery never invents a second core: reject duplicate or malformed state.
            store.ledger.restore(new BaseCoreLedger.Entry(team, token, mode, elapsed, location, lastSafe));
        }
        return store;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (BaseCoreLedger.Entry entry : ledger.all()) {
            CompoundTag row = new CompoundTag();
            row.putUUID("Team", entry.teamId());
            row.putUUID("Token", entry.token());
            row.putString("Mode", entry.mode().name());
            row.putLong("Elapsed", entry.carriedSeconds());
            writePosition(row, "Location", entry.location());
            writePosition(row, "LastSafe", entry.lastSafe());
            list.add(row);
        }
        tag.putInt("Schema", 1);
        tag.put("Cores", list);
        return tag;
    }

    private static void writePosition(CompoundTag tag, String key, BaseCoreLedger.Location value) {
        if (value == null) return;
        CompoundTag coordinate = new CompoundTag();
        coordinate.putString("Dimension", value.dimension());
        coordinate.putInt("X", value.x()); coordinate.putInt("Y", value.y()); coordinate.putInt("Z", value.z());
        tag.put(key, coordinate);
    }
    private static BaseCoreLedger.Location readPosition(CompoundTag row, String key) {
        if (!row.contains(key, Tag.TAG_COMPOUND)) return null;
        CompoundTag p = row.getCompound(key);
        return new BaseCoreLedger.Location(p.getString("Dimension"), p.getInt("X"), p.getInt("Y"), p.getInt("Z"));
    }
}
