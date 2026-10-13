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
    private boolean clientDriveEnabled;
    private int serverRefreshCountdown=2;
    private boolean enabled() {
        // The owning team's saved-data is server-only. The client receives the
        // purchased-drive bit with Create's BlockEntity update packet.
        if(level!=null && level.isClientSide)return clientDriveEnabled;
        if(!(level instanceof ServerLevel l))return false;
        UUID team=CoreInfrastructureService.owner(l,worldPosition);
        var data=team==null?null:CoreInfrastructureSavedData.get(l.getServer()).find(team);
        return data!=null && data.createLevel>=1;
    }
    @Override public float getGeneratedSpeed() {return enabled()?CoreKineticPolicy.RPM:0;}
    @Override public void initialize() {
        super.initialize();
        if(level!=null && !level.isClientSide &&
                (!hasSource() || getGeneratedSpeed()>getTheoreticalSpeed()))
            updateGeneratedRotation();
    }
    @Override public void tick() {
        super.tick();
        if(level==null || level.isClientSide)return;
        // An in-place purchase can occur after Create initialized the entity
        // with zero rotation. A bounded periodic reconciliation makes the
        // generation start without requiring core relocation or chunk reload.
        if(--serverRefreshCountdown<=0){
            serverRefreshCountdown=20;
            float desired=getGeneratedSpeed();
            if(!hasSource() && getTheoreticalSpeed()!=desired) refreshEngine();
        }
    }
    @Override protected void write(CompoundTag tag,HolderLookup.Provider registries,boolean clientPacket){
        super.write(tag,registries,clientPacket);
        tag.putBoolean("TeamWarsDriveEnabled",enabled());
    }
    @Override protected void read(CompoundTag tag,HolderLookup.Provider registries,boolean clientPacket){
        clientDriveEnabled=tag.getBoolean("TeamWarsDriveEnabled");
        super.read(tag,registries,clientPacket);
    }
    public void refreshEngine() {
        if(level!=null && !level.isClientSide) {
            updateGeneratedRotation(); // Creates/propagates the real kinetic network.
            setChanged();
            sendData(); // Guarantees the purchase state reaches Create goggles.
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
