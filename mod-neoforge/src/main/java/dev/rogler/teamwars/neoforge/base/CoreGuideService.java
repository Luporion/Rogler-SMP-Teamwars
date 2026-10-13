package dev.rogler.teamwars.neoforge.base;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.rogler.teamwars.core.base.BaseCoreLedger;
import dev.rogler.teamwars.neoforge.RoglerTeamWarsMod;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.util.*;

/** Team Core is a hub for finding actual installed mod manuals, not fake guide items.
 *  Only recognized handbook registry IDs may be issued, never ordinary power items.
 *  The latest book registries are discovered at runtime: no extra mod hard-dependency.
 */
public final class CoreGuideService {
    private CoreGuideService() {}
    private static final Map<String,String> KNOWN=Map.of(
        "ars_nouveau","worn_notebook",
        "botania","lexicon",
        "bloodmagic","sanguine_scientiem",
        "occultism","dictionary_of_spirits");
    private static final Set<String> NAMES=Set.of(
        "guide_book","guidebook","manual","handbook","lexicon","worn_notebook",
        "field_guide","in_game_guide","guide","spellbook_manual");
    private static Map<String,ResourceLocation> discover(){
        Map<String,ResourceLocation> result=new TreeMap<>();
        for(var id:BuiltInRegistries.ITEM.keySet()) {
            String path=id.getPath(), namespace=id.getNamespace();
            if (path.equals(KNOWN.get(namespace)) || NAMES.contains(path) || path.endsWith("_manual")
                || path.endsWith("_guidebook") || path.endsWith("_handbook")) {
                result.putIfAbsent(namespace,id);
                if(path.equals(KNOWN.get(namespace)))result.put(namespace,id);
            }
        }
        return result;
    }
    private static void say(ServerPlayer p,String message){p.sendSystemMessage(Component.literal("[TeamWars Guides] "+message));}
    private static boolean nearOwnCore(ServerPlayer p){
        if(p.level()!=p.getServer().overworld())return false;
        var a=RoglerTeamWarsMod.assignmentFor(p);
        if(!a.validParty())return false;
        var cfg=RoglerTeamWarsMod.baseConfig();
        var entry=BaseCoreSavedData.get(p.getServer(),cfg.zone(p.getServer()),cfg.carrySeconds()).ledger().get(a.partyId());
        if(entry==null||entry.mode()!=BaseCoreLedger.Mode.PLACED||entry.location()==null)return false;
        var loc=entry.location();BlockPos pos=new BlockPos(loc.x(),loc.y(),loc.z());
        return p.level().hasChunkAt(pos)&&p.level().getBlockState(pos).is(BaseCoreRegistries.CORE_BLOCK.get())
            && p.distanceToSqr(pos.getX()+0.5,pos.getY()+0.5,pos.getZ()+0.5)<=64;
    }
    public static java.util.List<java.util.Map.Entry<String,ResourceLocation>> manuals(){
        return java.util.List.copyOf(discover().entrySet());
    }
    public static int help(CommandSourceStack src) throws CommandSyntaxException {
        help(src.getPlayerOrException());
        return Command.SINGLE_SUCCESS;
    }
    public static void help(ServerPlayer p) {
        if(CoreInfrastructureService.openOwn(p))return;
        p.displayClientMessage(Component.literal("[TeamWars] Technik-Guides im Kern-Dashboard: gehe zu deinem Kern (8 Bloecke) oder nutze /teamwars base menu."),true);
    }
    public static int books(CommandSourceStack src) throws CommandSyntaxException {
        ServerPlayer p=src.getPlayerOrException();
        if(CoreInfrastructureService.openOwn(p))return Command.SINGLE_SUCCESS;
        say(p,"Am eigenen Kern findest du alle erkannten Handbuecher in der GUIDE-Seite. Nutze /teamwars base menu.");
        return 0;
    }
    public static int give(CommandSourceStack src,String modid) throws CommandSyntaxException {
        ServerPlayer p=src.getPlayerOrException();
        if(!nearOwnCore(p)) {say(p,"Stelle dich innerhalb 8 Bloecke zu DEINEM platzierten Teamkern.");return 0;}
        ResourceLocation id=discover().get(modid);
        if(id==null){say(p,"Kein bekanntes echtes Guide-Item fuer "+modid+". Nutze /teamwars guide.");return 0;}
        Item item=BuiltInRegistries.ITEM.get(id);
        if(item==Items.AIR){say(p,"Das Handbuch ist nicht geladen.");return 0;}
        // Avoid giving unbounded duplicates when the same book is already carried.
        for(int i=0;i<p.getInventory().getContainerSize();i++)
            if(p.getInventory().getItem(i).is(item)) {say(p,"Du traegst dieses Handbuch bereits.");return 0;}
        // Exchange one ordinary Minecraft BOOK for one installed mod manual. The
        // server checks both payment and output space before mutating inventory.
        int bookSlot=-1;
        for(int i=0;i<p.getInventory().getContainerSize();i++) {
            if(p.getInventory().getItem(i).is(Items.BOOK)) { bookSlot=i; break; }
        }
        if(bookSlot<0){say(p,"Du brauchst 1 normales Minecraft-Buch pro Handbuch.");return 0;}
        // One input book disappears, leaving at least one slot available unless
        // the paid stack has count > 1. Non-stackable output requires free space.
        boolean room=p.getInventory().getFreeSlot()>=0 || p.getInventory().getItem(bookSlot).getCount()==1;
        if(!dev.rogler.teamwars.core.base.CoreManualTradePolicy.mayTrade(true,false,1,room)) {
            say(p,"Inventar voll. Bitte einen freien Platz schaffen.");return 0;
        }
        ItemStack payment=p.getInventory().getItem(bookSlot);
        payment.shrink(1);
        ItemStack manual=new ItemStack(item);
        if(!p.getInventory().add(manual)) {
            // No item is granted; refund the book on any insertion failure.
            p.getInventory().add(new ItemStack(Items.BOOK));
            say(p,"Handbuch konnte nicht eingelegt werden; Buch erstattet.");return 0;
        }
        p.getInventory().setChanged();p.containerMenu.broadcastChanges();
        say(p,"1 Buch eingetauscht gegen "+id);
        return Command.SINGLE_SUCCESS;
    }
}
