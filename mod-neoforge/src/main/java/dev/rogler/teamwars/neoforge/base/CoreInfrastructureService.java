package dev.rogler.teamwars.neoforge.base;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.rogler.teamwars.core.base.BaseCoreLedger;
import dev.rogler.teamwars.core.base.CorePowerPolicy;
import dev.rogler.teamwars.neoforge.RoglerTeamWarsMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.energy.IEnergyStorage;
import java.util.UUID;

/** Vanilla 9-slot owner-only GUI + read/extract-only NeoForge FE capability.
 * No unrestricted item capability: AE2/Create automation must not siphon enemy bases.
 */
public final class CoreInfrastructureService {
    private CoreInfrastructureService() {}
    public static UUID owner(ServerLevel level,BlockPos pos) {
        if(level!=level.getServer().overworld() || !level.getBlockState(pos).is(BaseCoreRegistries.CORE_BLOCK.get())) return null;
        var cfg=RoglerTeamWarsMod.baseConfig();
        var cores=BaseCoreSavedData.get(level.getServer(),cfg.zone(level.getServer()),cfg.carrySeconds());
        for(var e:cores.ledger().all()) {
            if(e.mode()!=BaseCoreLedger.Mode.PLACED || e.location()==null)continue;
            var at=e.location();
            if(at.x()==pos.getX() && at.y()==pos.getY() && at.z()==pos.getZ()
                && "minecraft:overworld".equals(at.dimension())) return e.teamId();
        }
        return null;
    }
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlock(Capabilities.EnergyStorage.BLOCK,
            (level,pos,state,be,side) -> {
                if(!(level instanceof ServerLevel serverLevel)) return null;
                UUID team=owner(serverLevel,pos);
                return team==null ? null : new TeamEnergy(serverLevel,pos,team);
            }, BaseCoreRegistries.CORE_BLOCK.get());
    }
    private record TeamEnergy(ServerLevel level,BlockPos pos,UUID team) implements IEnergyStorage {
        private CoreInfrastructureSavedData.TeamRecord current() {
            return team.equals(owner(level,pos))
                ? CoreInfrastructureSavedData.get(level.getServer()).getOrCreate(team) : null;
        }
        @Override public int receiveEnergy(int maxReceive,boolean simulate) {return 0;}
        @Override public int extractEnergy(int maxExtract,boolean simulate) {
            var r=current();return r==null?0:r.extract(maxExtract,simulate);
        }
        @Override public int getEnergyStored() {var r=current();return r==null?0:r.energy();}
        @Override public int getMaxEnergyStored() {return CorePowerPolicy.MAX_FE;}
        @Override public boolean canExtract() {return true;}
        @Override public boolean canReceive() {return false;}
    }
    /** Only a confirmed online member of the owning FTB party can open this shared menu. */
    public static boolean open(ServerPlayer p,BlockPos pos) {
        if(!(p.level() instanceof ServerLevel level) || p.isSpectator() || p.distanceToSqr(
            pos.getX()+0.5,pos.getY()+0.5,pos.getZ()+0.5)>64)return false;
        UUID team=owner(level,pos);
        var a=RoglerTeamWarsMod.assignmentFor(p);
        if(team==null || !a.validParty() || !team.equals(a.partyId())) return false;
        var inventory=CoreInfrastructureSavedData.get(p.getServer()).getOrCreate(team).inventory;
        p.openMenu(new SimpleMenuProvider((id,inv,viewer)->new ChestMenu(MenuType.GENERIC_9x1,id,inv,inventory,1),
            Component.literal("TeamWars Core – 9 slots")));
        return true;
    }
    public static void tick(MinecraftServer server) {
        var cfg=RoglerTeamWarsMod.baseConfig();
        var saved=BaseCoreSavedData.get(server,cfg.zone(server),cfg.carrySeconds());
        var infrastructure=CoreInfrastructureSavedData.get(server);
        for(var e:saved.ledger().all()) {
            if(e.mode()==BaseCoreLedger.Mode.PLACED && e.location()!=null
                && "minecraft:overworld".equals(e.location().dimension())) {
                BlockPos pos=new BlockPos(e.location().x(),e.location().y(),e.location().z());
                // Never force-load chunks merely to generate energy.
                if(server.overworld().hasChunkAt(pos)
                    && server.overworld().getBlockState(pos).is(BaseCoreRegistries.CORE_BLOCK.get())) {
                    var record=infrastructure.getOrCreate(e.teamId());
                    record.receiveGeneration(RoglerTeamWarsMod.powerConfig().rate(record.energyLevel));
                    // Push into neighboring FE consumers (incl. AE2 Energy Acceptor).
                    // Never cause a chunk load and never move more than 160 FE per tick.
                    int budget=CorePowerPolicy.MAX_EXTRACT_PER_CALL;
                    for(Direction direction:Direction.values()) {
                        if(budget<=0 || record.energy()<=0) break;
                        BlockPos neighbor=pos.relative(direction);
                        if(!server.overworld().hasChunkAt(neighbor))continue;
                        IEnergyStorage dest=server.overworld().getCapability(
                            Capabilities.EnergyStorage.BLOCK,neighbor,direction.getOpposite());
                        if(dest==null || !dest.canReceive())continue;
                        int offered=Math.min(budget,record.energy());
                        int accepted=Math.min(offered,Math.max(0,dest.receiveEnergy(offered,false)));
                        if(accepted>0) {
                            record.extract(accepted,false);
                            budget-=accepted;
                        }
                    }
                }
            }
        }
    }
    public static int status(net.minecraft.commands.CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player=source.getPlayerOrException();var a=RoglerTeamWarsMod.assignmentFor(player);
        if(!a.validParty())return 0;
        var rec=CoreInfrastructureSavedData.get(player.getServer()).find(a.partyId());
        int level=rec==null?0:rec.energyLevel;
        int stored=rec==null?0:rec.energy();
        player.sendSystemMessage(Component.literal("[TeamWars] Core FE "+stored+"/"+CorePowerPolicy.MAX_FE
            +"; generator tier "+level+"/2 ("+RoglerTeamWarsMod.powerConfig().rate(level)+" FE/t while placed and loaded). "
            +"Right-click core = private inventory, sneak-right-click = pack."));
        return Command.SINGLE_SUCCESS;
    }
}
