package dev.rogler.teamwars.neoforge.base;

import dev.rogler.teamwars.core.base.CorePowerPolicy;
import dev.rogler.teamwars.core.base.BaseCoreLedger;
import dev.rogler.teamwars.neoforge.RoglerTeamWarsMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.*;

/** Persistent TEAM-owned 9-slot inventory and FE buffer. No block-entity duplication
 * during core pack, ESCROW, relocation or raid defeat. Server-thread access only.
 */
public final class CoreInfrastructureSavedData extends SavedData {
    public static final String NAME = "roglerteamwars_infrastructure";
    public final class TeamRecord {
        public final UUID team;
        public final CoreInventory inventory;
        public int energyLevel;
        private int storedFE;
        TeamRecord(UUID id) { team = id; inventory = new CoreInventory(id); }
        public int energy() { return storedFE; }
        public int receiveGeneration(int fePerTick) {
            int next = CorePowerPolicy.clampEnergy((long)storedFE + fePerTick);
            if (next != storedFE) { storedFE = next; setDirty(); }
            return storedFE;
        }
        public int extract(int requested, boolean simulate) {
            int amount = CorePowerPolicy.extractable(storedFE, requested);
            if (!simulate && amount > 0) { storedFE -= amount; setDirty(); }
            return amount;
        }
        public boolean upgradeEnergy(int oldLevel) {
            if (energyLevel != oldLevel || oldLevel < 0 || oldLevel >= CorePowerPolicy.MAX_LEVEL) return false;
            energyLevel++;setDirty();return true;
        }
    }
    public final class CoreInventory extends SimpleContainer {
        private final UUID team;
        CoreInventory(UUID id) { super(CorePowerPolicy.SLOTS); team=id; }
        @Override public void setChanged() { super.setChanged(); CoreInfrastructureSavedData.this.setDirty(); }
        @Override public boolean stillValid(Player player) {
            if (!(player instanceof ServerPlayer p) || p.getServer()==null
                || p.level()!=p.getServer().overworld()) return false;
            var membership = RoglerTeamWarsMod.assignmentFor(p);
            if (!membership.validParty() || !team.equals(membership.partyId())) return false;
            var cfg = RoglerTeamWarsMod.baseConfig();
            var e = BaseCoreSavedData.get(p.getServer(),cfg.zone(p.getServer()),cfg.carrySeconds()).ledger().get(team);
            if (e==null || e.mode()!=BaseCoreLedger.Mode.PLACED || e.location()==null) return false;
            BlockPos at=new BlockPos(e.location().x(),e.location().y(),e.location().z());
            return p.getServer().overworld().hasChunkAt(at)
                && p.getServer().overworld().getBlockState(at).is(BaseCoreRegistries.CORE_BLOCK.get())
                && p.distanceToSqr(at.getX()+0.5,at.getY()+0.5,at.getZ()+0.5)<=64;
        }
    }
    private final Map<UUID,TeamRecord> records = new HashMap<>();
    public TeamRecord getOrCreate(UUID id) {
        TeamRecord r=records.get(id);
        if (r==null) { r=new TeamRecord(id); records.put(id,r);setDirty(); }
        return r;
    }
    public TeamRecord find(UUID id) {return records.get(id);}
    /** Explicit ADMIN adoption only; never inferred from human-readable team name.
     * No move if destination has an existing private inventory or energy record.
     */
    public boolean canTransfer(UUID from,UUID to) {
        if (from.equals(to)) return false;
        var target=records.get(to);
        return target==null || (target.energyLevel==0 && target.storedFE==0 && isEmpty(target.inventory));
    }
    private static boolean isEmpty(SimpleContainer inventory) {
        for (int i=0;i<inventory.getContainerSize();i++) if(!inventory.getItem(i).isEmpty()) return false;
        return true;
    }
    public void transfer(UUID from,UUID to) {
        if(!canTransfer(from,to)) throw new IllegalStateException("Destination has core inventory or energy; adoption refused");
        TeamRecord old=records.remove(from);
        records.remove(to);
        if(old!=null) {
            TeamRecord neo=new TeamRecord(to);neo.energyLevel=old.energyLevel;neo.storedFE=old.storedFE;
            for(int i=0;i<CorePowerPolicy.SLOTS;i++)neo.inventory.setItem(i,old.inventory.getItem(i).copy());
            records.put(to,neo);
        }
        setDirty();
    }
    public static CoreInfrastructureSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
            new SavedData.Factory<>(CoreInfrastructureSavedData::new, CoreInfrastructureSavedData::load), NAME);
    }
    private static CoreInfrastructureSavedData load(CompoundTag root,HolderLookup.Provider registry) {
        CoreInfrastructureSavedData data=new CoreInfrastructureSavedData();
        for(Tag raw:root.getList("Teams",Tag.TAG_COMPOUND)) {
            CompoundTag row=(CompoundTag)raw;
            if (!row.hasUUID("Id")) continue;
            UUID id=row.getUUID("Id");TeamRecord r=data.getOrCreate(id);
            r.energyLevel=Math.max(0,Math.min(CorePowerPolicy.MAX_LEVEL,row.getInt("EnergyLevel")));
            r.storedFE=CorePowerPolicy.clampEnergy(row.getInt("StoredFE"));
            for(Tag item:row.getList("Items",Tag.TAG_COMPOUND)) {
                CompoundTag entry=(CompoundTag)item;
                int slot=entry.getInt("Slot");
                if(slot>=0 && slot<CorePowerPolicy.SLOTS)
                    r.inventory.setItem(slot,ItemStack.parseOptional(registry,entry.getCompound("Stack")));
            }
        }
        return data;
    }
    @Override public CompoundTag save(CompoundTag root,HolderLookup.Provider registry) {
        ListTag teams=new ListTag();
        for(var r:records.values()) {
            CompoundTag row=new CompoundTag();row.putUUID("Id",r.team);
            row.putInt("EnergyLevel",r.energyLevel);row.putInt("StoredFE",r.storedFE);
            ListTag items=new ListTag();
            for(int i=0;i<CorePowerPolicy.SLOTS;i++) {
                ItemStack stack=r.inventory.getItem(i);
                if(stack.isEmpty())continue;
                CompoundTag entry=new CompoundTag();entry.putInt("Slot",i);
                entry.put("Stack",stack.save(registry));items.add(entry);
            }
            row.put("Items",items);teams.add(row);
        }
        root.putInt("Schema",1);root.put("Teams",teams);return root;
    }
}
