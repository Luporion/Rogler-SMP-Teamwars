package dev.rogler.teamwars.neoforge.base;

import dev.rogler.teamwars.neoforge.RoglerTeamWarsMod;
import dev.rogler.teamwars.neoforge.protection.BaseProtectionSavedData;
import dev.rogler.teamwars.neoforge.raid.RaidService;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import java.util.UUID;

/** Server-authoritative, future-extensible Core dashboard. The only authoritative actions
 * are handled by clickMenuButton on the logical server; tabs are client-only presentation.
 * Data slots are intentionally <= 32767 (vanilla short wire format).
 */
public final class CoreDashboardMenu extends AbstractContainerMenu {
    public static final int ARMOR=0, RAID_HP=1, ENERGY_TIER=2, FE_BUFFER=3, FE_RATE=4,
        CREATE_TIER=5, GRACE_MIN=6, CLAIMS=7, RAID_ACTIVE=8, FE_NEIGHBORS=9,
        AE2_ACCEPTORS=10, FEEDBACK=11, FE_SIDES=12, AE2_SIDES=13, DATA_COUNT=14;
    public static final int BUY_ARMOR=1, BUY_ENERGY=2, BUY_CREATE=3, MANUAL_BASE=100;
    private final Container core;
    private final ContainerData data;
    private final boolean serverMenu;

    /** Client factory: no server-only team identity or SavedData crosses the network. */
    public CoreDashboardMenu(int id, Inventory playerInventory) {
        this(id, playerInventory, new SimpleContainer(9), new SimpleContainerData(DATA_COUNT), false);
    }
    public CoreDashboardMenu(int id, Inventory playerInventory, Container core, UUID team) {
        this(id, playerInventory, core, new LiveData((ServerPlayer)playerInventory.player,team), true);
    }
    private CoreDashboardMenu(int id, Inventory playerInventory, Container core,
                              ContainerData data, boolean serverMenu) {
        super(BaseCoreRegistries.CORE_DASHBOARD.get(),id);
        checkContainerSize(core,9);
        this.core=core;
        this.data=data;
        this.serverMenu=serverMenu;
        // Shared core storage + normal player inventory are always displayed at the bottom.
        for(int i=0;i<9;i++)addSlot(new Slot(core,i,51+i*18,153));
        for(int y=0;y<3;y++)for(int x=0;x<9;x++)
            addSlot(new Slot(playerInventory,x+y*9+9,51+x*18,185+y*18));
        for(int x=0;x<9;x++)addSlot(new Slot(playerInventory,x,51+x*18,243));
        addDataSlots(data);
    }
    public int value(int index){return data.get(index);}
    @Override public boolean stillValid(Player player){return core.stillValid(player);}
    @Override public ItemStack quickMoveStack(Player player,int index){
        ItemStack result=ItemStack.EMPTY;
        if(index<0||index>=slots.size())return result;
        Slot slot=slots.get(index);
        if(!slot.hasItem())return result;
        ItemStack stack=slot.getItem();result=stack.copy();
        if(index<9){if(!moveItemStackTo(stack,9,45,true))return ItemStack.EMPTY;}
        else if(!moveItemStackTo(stack,0,9,false))return ItemStack.EMPTY;
        if(stack.isEmpty())slot.setByPlayer(ItemStack.EMPTY);else slot.setChanged();
        return result;
    }
    @Override public boolean clickMenuButton(Player player,int buttonId){
        if(!serverMenu || !(player instanceof ServerPlayer sp) || !stillValid(sp))return false;
        int count=CoreGuideService.manuals().size();
        boolean survival=!sp.isCreative()&&!sp.isSpectator();
        boolean active=dev.rogler.teamwars.neoforge.raid.RaidService.active(sp.getServer(),
            RoglerTeamWarsMod.assignmentFor(sp).partyId());
        if(!dev.rogler.teamwars.core.base.CoreDashboardPolicy.allowed(buttonId,count,true,survival,active)) {
            data.set(FEEDBACK,-Math.abs(buttonId));
            return true;
        }
        try{
            var source=sp.createCommandSourceStack();
            boolean success = switch(buttonId){
                case BUY_ARMOR, BUY_ENERGY, BUY_CREATE -> CoreUpgradeService.buyFromDashboard(source,buttonId)>0;
                default -> {
                    int bookIndex=buttonId-MANUAL_BASE;
                    var books=CoreGuideService.manuals();
                    yield bookIndex>=0 && bookIndex<books.size()
                        && CoreGuideService.give(source,books.get(bookIndex).getKey())>0;
                }
            };
            data.set(FEEDBACK,success?buttonId:-buttonId);
            return true; // acknowledged action (even if denied); status is synced to client
        }catch(com.mojang.brigadier.exceptions.CommandSyntaxException ex){return false;}
    }
    /** Dynamic read-only server snapshot, synchronized by vanilla AbstractContainerMenu. */
    private static final class LiveData implements ContainerData {
        private final ServerPlayer viewer;
        private final UUID team;
        private int feedback;
        private LiveData(ServerPlayer viewer,UUID team){this.viewer=viewer;this.team=team;}
        @Override public int get(int index){
            var server=viewer.getServer();
            if(server==null)return 0;
            var infra=CoreInfrastructureSavedData.get(server).find(team);
            var protection=BaseProtectionSavedData.get(server).team(team);
            switch(index){
                case ARMOR: return protection==null?0:protection.armorLevel;
                case RAID_HP: return dev.rogler.teamwars.core.base.CoreUpgradePolicy.maxHealth(protection==null?0:protection.armorLevel);
                case ENERGY_TIER: return infra==null?0:infra.energyLevel;
                case FE_BUFFER: return infra==null?0:infra.energy();
                case FE_RATE: return RoglerTeamWarsMod.powerConfig().rate(infra==null?0:infra.energyLevel);
                case CREATE_TIER: return infra==null?0:infra.createLevel;
                case GRACE_MIN: return protection==null?0:(int)Math.min(32767,Math.max(0,((protection.graceUntilMillis-System.currentTimeMillis())+59999)/60000));
                case CLAIMS: return protection==null?0:protection.createdClaims.size();
                case RAID_ACTIVE: return RaidService.active(server,team)?1:0;
                case FE_NEIGHBORS: return countNeighbors(server.overworld(),team,false);
                case AE2_ACCEPTORS: return countNeighbors(server.overworld(),team,true);
                case FEEDBACK: return feedback;
                case FE_SIDES: return sideMask(server.overworld(),team,false);
                case AE2_SIDES: return sideMask(server.overworld(),team,true);
                default: return 0;
            }
        }
        private int countNeighbors(ServerLevel world,UUID team,boolean onlyAE2){
            return Integer.bitCount(sideMask(world,team,onlyAE2));
        }
        private int sideMask(ServerLevel world,UUID team,boolean onlyAE2){
            var cfg=RoglerTeamWarsMod.baseConfig();
            var entry=BaseCoreSavedData.get(world.getServer(),cfg.zone(world.getServer()),cfg.carrySeconds()).ledger().get(team);
            if(entry==null||entry.location()==null||!"minecraft:overworld".equals(entry.location().dimension()))return 0;
            BlockPos corePos=new BlockPos(entry.location().x(),entry.location().y(),entry.location().z());
            if(!world.hasChunkAt(corePos)||!world.getBlockState(corePos).is(BaseCoreRegistries.CORE_BLOCK.get()))return 0;
            int mask=0;
            for(Direction side:Direction.values()){
                BlockPos other=corePos.relative(side);
                if(!world.hasChunkAt(other))continue;
                if(onlyAE2){
                    String id=BuiltInRegistries.BLOCK.getKey(world.getBlockState(other).getBlock()).toString();
                    if(id.equals("ae2:energy_acceptor") || id.equals("ae2:energy_cell"))mask|=(1<<side.ordinal());
                }else{
                    var cap=world.getCapability(Capabilities.EnergyStorage.BLOCK,other,side.getOpposite());
                    if(cap!=null&&cap.canReceive())mask|=(1<<side.ordinal());
                }
            }
            return mask;
        }
        @Override public void set(int index,int value) { if(index==FEEDBACK)feedback=value; }
        @Override public int getCount(){return DATA_COUNT;}
    }
}
