package dev.rogler.teamwars.neoforge.base;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.rogler.teamwars.core.base.BaseCoreLedger;
import dev.rogler.teamwars.core.base.CoreUpgradePolicy;
import dev.rogler.teamwars.core.base.CorePowerPolicy;
import dev.rogler.teamwars.core.base.CoreKineticPolicy;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import dev.rogler.teamwars.neoforge.RoglerTeamWarsMod;
import dev.rogler.teamwars.neoforge.protection.BaseProtectionSavedData;
import dev.rogler.teamwars.neoforge.raid.RaidService;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.util.UUID;

/** Server-only atomic material cost, per-team persistent armor 0/1/2. */
public final class CoreUpgradeService {
    private CoreUpgradeService() {}
    private static void say(ServerPlayer p,String s) {p.sendSystemMessage(Component.literal("[TeamWars] "+s));}
    private static int count(ServerPlayer p,Item kind) {
        int n=0;
        for(int i=0;i<p.getInventory().getContainerSize();i++) {
            ItemStack s=p.getInventory().getItem(i);if(s.is(kind))n+=s.getCount();
        }
        return n;
    }
    private static void consume(ServerPlayer p,Item kind,int amount) {
        for(int i=0;i<p.getInventory().getContainerSize() && amount>0;i++) {
            ItemStack s=p.getInventory().getItem(i);
            if(!s.is(kind))continue;
            int take=Math.min(amount,s.getCount());s.shrink(take);amount-=take;
        }
        if(amount!=0) throw new IllegalStateException("inventory cost changed on server thread");
        p.getInventory().setChanged();p.containerMenu.broadcastChanges();
    }
    private static BaseCoreLedger.Entry ownPlaced(ServerPlayer p) {
        var a=RoglerTeamWarsMod.assignmentFor(p);
        if(!a.validParty())return null;
        var cfg=RoglerTeamWarsMod.baseConfig();
        var db=BaseCoreSavedData.get(p.getServer(),cfg.zone(p.getServer()),cfg.carrySeconds());
        var entry=db.ledger().get(a.partyId());
        if(entry==null || entry.mode()!=BaseCoreLedger.Mode.PLACED || entry.location()==null)return null;
        var loc=entry.location();
        if(p.level()!=p.getServer().overworld() || !p.getServer().overworld().hasChunkAt(new BlockPos(loc.x(),loc.y(),loc.z()))
            || !p.getServer().overworld().getBlockState(new BlockPos(loc.x(),loc.y(),loc.z())).is(BaseCoreRegistries.CORE_BLOCK.get())) return null;
        double dx=p.getX()-(loc.x()+0.5),dy=p.getY()-(loc.y()+0.5),dz=p.getZ()-(loc.z()+0.5);
        return dx*dx+dy*dy+dz*dz <= 64 ? entry : null;
    }
    public static int status(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player=source.getPlayerOrException();var a=RoglerTeamWarsMod.assignmentFor(player);
        if(!a.validParty()){say(player,"Join an FTB party first.");return 0;}
        var rec=BaseProtectionSavedData.get(player.getServer()).team(a.partyId());
        int lvl=rec==null?0:rec.armorLevel;
        String next=lvl>=2?"MAX":CoreUpgradePolicy.diamondsForNext(lvl)+" diamonds + "+CoreUpgradePolicy.netheriteForNext(lvl)+" netherite ingots";
        var infra=CoreInfrastructureSavedData.get(player.getServer()).getOrCreate(a.partyId());
        int energyLevel=infra.energyLevel;
        say(player,"Core armor tier "+lvl+"/2; raid HP "+CoreUpgradePolicy.maxHealth(lvl)+"; next armor cost: "+next
            +" | energy tier "+energyLevel+"/2, "+RoglerTeamWarsMod.powerConfig().rate(energyLevel)+" FE/t"
            +" | Create drive "+(infra.createLevel>0?"16 RPM / 256 SU":"not installed")+".");
        return Command.SINGLE_SUCCESS;
    }
    public static int buyArmor(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer p=source.getPlayerOrException();var e=ownPlaced(p);
        if(e==null){say(p,"Your own placed core must be loaded and within 8 blocks.");return 0;}
        if(p.isCreative() || p.isSpectator()){say(p,"Armor must be purchased in survival mode.");return 0;}
        UUID id=e.teamId();var saved=BaseProtectionSavedData.get(p.getServer());
        var rec=saved.team(id);int lvl=rec==null?0:rec.armorLevel;
        if(lvl>=CoreUpgradePolicy.MAX_ARMOR_LEVEL){say(p,"Core armor is already maximum.");return 0;}
        boolean raid=RaidService.active(p.getServer(),id);
        int diamondCost=CoreUpgradePolicy.diamondsForNext(lvl),netheriteCost=CoreUpgradePolicy.netheriteForNext(lvl);
        if(!CoreUpgradePolicy.canBuy(lvl,count(p,Items.DIAMOND),count(p,Items.NETHERITE_INGOT),true,raid)) {
            say(p,"Upgrade refused: active raid or insufficient resources (need "+diamondCost+" diamonds and "+netheriteCost+" netherite ingots).");return 0;
        }
        // All costs validated before consuming. Server tick is single threaded.
        consume(p,Items.DIAMOND,diamondCost);consume(p,Items.NETHERITE_INGOT,netheriteCost);
        if(!saved.upgradeArmor(id,lvl)) throw new IllegalStateException("Team armor changed during atomic upgrade");
        say(p,"Armor tier "+(lvl+1)+" purchased; next raid core starts with "+CoreUpgradePolicy.maxHealth(lvl+1)+" HP.");
        return Command.SINGLE_SUCCESS;
    }
    /** One bounded utility upgrade at a time; no creative purchases or raid-time modifications. */
    /** Buy one real Create kinetic generator: 16 rpm * 16 capacity = 256 SU. */
    public static int buyCreate(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer p=source.getPlayerOrException();var core=ownPlaced(p);
        if(core==null){say(p,"Your placed, loaded core must be within 8 blocks.");return 0;}
        var record=CoreInfrastructureSavedData.get(p.getServer()).getOrCreate(core.teamId());
        Item alloy=BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("create","andesite_alloy"));
        Item shaft=BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("create","shaft"));
        if(alloy==Items.AIR || shaft==Items.AIR){say(p,"Create material registry unavailable.");return 0;}
        if(!CoreKineticPolicy.canUpgrade(record.createLevel,count(p,alloy),count(p,shaft),true,
            RaidService.active(p.getServer(),core.teamId()),!p.isCreative()&&!p.isSpectator())){
            say(p,"Create drive costs 16 Andesite Alloy + 8 Shafts; no upgrades during raids.");return 0;
        }
        consume(p,alloy,CoreKineticPolicy.ALLOY_COST);
        consume(p,shaft,CoreKineticPolicy.SHAFT_COST);
        record.createLevel=1;
        // Existing TeamRecord marks SavedData dirty via inventory? Ensure persistence explicitly.
        CoreInfrastructureSavedData.get(p.getServer()).setDirty();
        var loc=core.location();
        var be=p.getServer().overworld().getBlockEntity(new BlockPos(loc.x(),loc.y(),loc.z()));
        if(be instanceof CoreKineticBlockEntity kinetic) kinetic.refreshEngine();
        say(p,"Create starter drive activated: 16 RPM / 256 SU. Build additional generation soon!");
        return Command.SINGLE_SUCCESS;
    }

    public static int buyEnergy(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer p=source.getPlayerOrException();var core=ownPlaced(p);
        if(core==null) {say(p,"Your placed, loaded core must be within 8 blocks.");return 0;}
        if(p.isCreative() || p.isSpectator()) {say(p,"Core power must be purchased in Survival.");return 0;}
        if(RaidService.active(p.getServer(),core.teamId())) {say(p,"No upgrades during raids.");return 0;}
        var record=CoreInfrastructureSavedData.get(p.getServer()).getOrCreate(core.teamId());
        int tier=record.energyLevel;
        if(tier>=CorePowerPolicy.MAX_LEVEL) {say(p,"Core generator is at maximum tier.");return 0;}
        int red=CorePowerPolicy.redstoneForNext(tier),iron=CorePowerPolicy.ironForNext(tier),gold=CorePowerPolicy.goldForNext(tier);
        if(!CorePowerPolicy.canUpgrade(tier,count(p,Items.REDSTONE),count(p,Items.IRON_INGOT),
            count(p,Items.GOLD_INGOT),true,false)) {
            say(p,"Insufficient materials: "+red+" redstone, "+iron+" iron, "+gold+" gold.");return 0;
        }
        // All deductions and the tier change are on the authoritative server thread.
        consume(p,Items.REDSTONE,red);
        if(iron>0) consume(p,Items.IRON_INGOT,iron);
        if(gold>0) consume(p,Items.GOLD_INGOT,gold);
        if(!record.upgradeEnergy(tier)) throw new IllegalStateException("Energy upgrade tier changed mid-purchase");
        say(p,"Power upgraded to tier "+(tier+1)+" ("+RoglerTeamWarsMod.powerConfig().rate(tier+1)+" FE/t).");
        return Command.SINGLE_SUCCESS;
    }

}
