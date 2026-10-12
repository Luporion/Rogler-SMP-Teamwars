package dev.rogler.teamwars.neoforge.base;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.rogler.teamwars.core.base.BaseCoreLedger;
import dev.rogler.teamwars.core.base.CoreUpgradePolicy;
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
        say(player,"Core armor tier "+lvl+"/2; raid HP "+CoreUpgradePolicy.maxHealth(lvl)+"; next cost: "+next);
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
}
