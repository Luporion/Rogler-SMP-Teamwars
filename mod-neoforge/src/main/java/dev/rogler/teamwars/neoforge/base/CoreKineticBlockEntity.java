package dev.rogler.teamwars.neoforge.base;

import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import dev.rogler.teamwars.core.base.CoreKineticPolicy;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.server.level.ServerLevel;
import java.util.List;
import java.util.UUID;

/** Create 6.0.10 physical kinetic source; no misleading fake SU status.
 *  Default unupgraded core produces 0 RPM, purchased starter drive 16 RPM/256 SU.
 */
public class CoreKineticBlockEntity extends GeneratingKineticBlockEntity {
    public CoreKineticBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) { super(type,pos,state); }
    /** Cached on the server tick and included in block-entity packets and disk NBT.
     * Create and C2ME may serialize a block entity while its chunk is saving:
     * no world/ledger/level queries are permitted in write() or getGeneratedSpeed().
     */
    private boolean cachedDriveEnabled;
    private int serverRefreshCountdown=2;

    /** Server-thread reconciliation only; NEVER call from a serializer or an async task. */
    private boolean refreshDriveFromLedger() {
        if (!(level instanceof ServerLevel server)) return false;
        UUID team=CoreInfrastructureService.recordedOwner(server,worldPosition);
        var data=team==null?null:CoreInfrastructureSavedData.get(server.getServer()).find(team);
        boolean nowEnabled=data!=null && data.createLevel>=1;
        boolean changed=nowEnabled!=cachedDriveEnabled;
        cachedDriveEnabled=nowEnabled;
        return changed;
    }

    private boolean enabled() { return cachedDriveEnabled; }

    @Override public float getGeneratedSpeed() {
        // This getter can run from Create's serialization or non-tick callbacks.
        return cachedDriveEnabled?CoreKineticPolicy.RPM:0;
    }
    @Override public void initialize() {
        // If the chunk is being loaded, refresh from the authoritative ledger only
        // on the main server thread. This does not inspect/load any world chunk.
        if (level instanceof ServerLevel) refreshDriveFromLedger();
        super.initialize();
        if(level instanceof ServerLevel &&
                (!hasSource() || getGeneratedSpeed()>getTheoreticalSpeed()))
            updateGeneratedRotation();
    }
    @Override public void tick() {
        super.tick();
        if(!(level instanceof ServerLevel))return;
        // A Create purchase can happen in-place; reconcile at a bounded interval.
        if(--serverRefreshCountdown<=0){
            serverRefreshCountdown=20;
            boolean changed=refreshDriveFromLedger();
            float desired=getGeneratedSpeed();
            if(changed || (!hasSource() && getTheoreticalSpeed()!=desired))refreshEngine();
        }
    }
    @Override protected void write(CompoundTag tag,HolderLookup.Provider registries,boolean clientPacket){
        super.write(tag,registries,clientPacket);
        // CRITICAL: never call owner(), getBlockState(), or consult SavedData here.
        tag.putBoolean("TeamWarsDriveEnabled",cachedDriveEnabled);
    }
    @Override protected void read(CompoundTag tag,HolderLookup.Provider registries,boolean clientPacket){
        cachedDriveEnabled=tag.getBoolean("TeamWarsDriveEnabled");
        super.read(tag,registries,clientPacket);
    }
    public void refreshEngine() {
        if(level instanceof ServerLevel) {
            refreshDriveFromLedger();
            updateGeneratedRotation(); // Real Create kinetic network, server thread only.
            setChanged();
            sendData(); // Cached state reaches client and Create goggles.
        }
    }
    @Override public boolean addToGoggleTooltip(List<Component> tooltip,boolean sneaking) {
        super.addToGoggleTooltip(tooltip,sneaking);
        tooltip.add(Component.literal(enabled()
            ? "TeamWars Startantrieb: 16 RPM / 256 SU – bald weitere Stromerzeugung bauen!"
            : "TeamWars: Create-Startantrieb noch nicht gekauft (/teamwars base upgrade create)")
            .withStyle(enabled()?ChatFormatting.AQUA:ChatFormatting.GRAY));
        return true;
    }
}
