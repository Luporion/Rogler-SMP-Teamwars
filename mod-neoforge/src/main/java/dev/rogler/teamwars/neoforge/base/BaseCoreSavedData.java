package dev.rogler.teamwars.neoforge.base;

import dev.rogler.teamwars.core.BaseZone;
import dev.rogler.teamwars.core.base.BaseCoreLedger;
import dev.rogler.teamwars.core.base.CoreTransportClockPolicy;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.UUID;
import java.util.Map;
import java.util.HashMap;

/** Single authoritative save in overworld/data/roglerteamwars_basecores.dat. */
public final class BaseCoreSavedData extends SavedData {
    private static final String SAVE_NAME = "roglerteamwars_basecores";
    private final BaseCoreLedger ledger;
    private final Map<UUID, CoreTransportClockPolicy.Checkpoint> transportCheckpoints = new HashMap<>();

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

    /** Persistent wall-clock accounting for every unplaced core, including Ender Chests. */
    public long transportSecondsDue(UUID team, UUID token, long nowMillis) {
        var previous = transportCheckpoints.get(team);
        var step = CoreTransportClockPolicy.advance(previous, token, nowMillis);
        if (!step.checkpoint().equals(previous)) {
            transportCheckpoints.put(team, step.checkpoint());
            setDirty();
        }
        return step.elapsedSeconds();
    }

    /** Removal is only a storage cleanup; switching tokens also invalidates old checkpoints. */
    public void clearTransportClock(UUID team) {
        if (transportCheckpoints.remove(team) != null) setDirty();
    }

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
        // Schema 1 saves have no checkpoint: they start accounting from first observation
        // without retroactively guessing how long an item was kept in a container.
        for (Tag raw : tag.getList("TransportClocks", Tag.TAG_COMPOUND)) {
            CompoundTag row = (CompoundTag) raw;
            if (!row.hasUUID("Team") || !row.hasUUID("Token")) continue;
            UUID team = row.getUUID("Team");
            var core = store.ledger.get(team);
            if (core == null || core.mode() != BaseCoreLedger.Mode.ISSUED
                || !core.token().equals(row.getUUID("Token"))) continue;
            store.transportCheckpoints.put(team, new CoreTransportClockPolicy.Checkpoint(
                row.getUUID("Token"), row.getLong("AccountedAtMillis")));
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
        ListTag timers = new ListTag();
        for (var clock : transportCheckpoints.entrySet()) {
            var core = ledger.get(clock.getKey());
            if (core == null || core.mode() != BaseCoreLedger.Mode.ISSUED
                || !core.token().equals(clock.getValue().token())) continue;
            CompoundTag row = new CompoundTag();
            row.putUUID("Team", clock.getKey());
            row.putUUID("Token", clock.getValue().token());
            row.putLong("AccountedAtMillis", clock.getValue().accountedAtMillis());
            timers.add(row);
        }
        tag.putInt("Schema", 2);
        tag.put("Cores", list);
        tag.put("TransportClocks", timers);
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
