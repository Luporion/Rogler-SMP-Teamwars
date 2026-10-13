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
    public static int help(CommandSourceStack src) throws CommandSyntaxException {
        help(src.getPlayerOrException());
        return Command.SINGLE_SUCCESS;
    }
    public static void help(ServerPlayer p) {
        say(p,"Willkommen im Technik-Hub! Rechtsklick = 9-Slot-Inventar; Buch in der Hand + Rechtsklick = Hilfe.");
        say(p,"FE: Kabel/AE2 Energy Acceptor DIREKT neben Kern; 20/40/80 FE/t, max. 160 FE/t Output.");
        say(p,"Create: 16 RPM / 256 SU nach /teamwars base upgrade create. Mit Welle an der Achsen-Seite verbinden.");
        say(p,"Create-Lernen: Create-Item in JEI/EMI ansehen und W gedrueckt halten -> Ponder-Animation. Bald eigene Wasserraeder/Dampfmaschine bauen!");
        say(p,"AE2-Lernen: W beim Ueberfahren passender AE2-Items; in vielen AE2-Menus ?-Hilfeschaltflaeche.");
        say(p,"Buecher: /teamwars guide books; /teamwars guide give <modid> am eigenen Kern. Es werden NUR vorhandene echte Buecher angeboten.");
    }
    public static int books(CommandSourceStack src) throws CommandSyntaxException {
        ServerPlayer p=src.getPlayerOrException();Map<String,ResourceLocation> books=discover();
        if(books.isEmpty())say(p,"Keine bekannten Handbuch-Items gefunden. Nutze JEI/EMI, Ponder und AE2-Ingame-Guide.");
        else for(var book:books.entrySet())say(p,book.getKey()+" -> "+book.getValue()+" (mit /teamwars guide give "+book.getKey()+")");
        say(p,"Nicht jede Mod besitzt ein Buch. Automatische Erkennung ist erweiterbar und keine Garantie fuer ALLE Mods.");
        return Command.SINGLE_SUCCESS;
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
        ItemStack stack=new ItemStack(item);
        if(!p.getInventory().add(stack)) {say(p,"Inventar voll. Bitte erst einen Slot freimachen.");return 0;}
        p.getInventory().setChanged();p.containerMenu.broadcastChanges();
        say(p,"Erhalten: "+id);
        return Command.SINGLE_SUCCESS;
    }
}
