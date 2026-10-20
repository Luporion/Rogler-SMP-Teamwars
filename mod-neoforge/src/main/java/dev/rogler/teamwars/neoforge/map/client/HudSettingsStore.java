package dev.rogler.teamwars.neoforge.map.client;

import dev.rogler.teamwars.core.HudPreferences;
import net.minecraft.client.Minecraft;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;

/** Client-local file, lazy load; never touches world or team SavedData. */
public final class HudSettingsStore {
    private static HudPreferences current;
    private HudSettingsStore() {}
    private static Path path(){return Minecraft.getInstance().gameDirectory.toPath().resolve("config/roglercore-hud.properties");}
    public static synchronized HudPreferences get(){
        if(current!=null)return current;
        current=HudPreferences.DEFAULT;
        Path file=path();
        if(Files.isRegularFile(file))try(InputStream in=Files.newInputStream(file)){
            Properties p=new Properties();p.load(in);current=HudPreferences.fromProperties(p);
        }catch(Exception e){System.err.println("[Rogler Core] HUD config invalid; defaults used, file preserved: "+e);}
        return current;
    }
    public static synchronized void update(HudPreferences value){
        current=value;
        Path file=path(), temp=null;
        try{
            Files.createDirectories(file.getParent());
            temp=Files.createTempFile(file.getParent(),"roglercore-hud-",".tmp");
            try(OutputStream out=Files.newOutputStream(temp)){value.toProperties().store(out,"Client only; /roglerhud opens settings");}
            try{Files.move(temp,file,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
            catch(java.nio.file.AtomicMoveNotSupportedException e){Files.move(temp,file,StandardCopyOption.REPLACE_EXISTING);}
        }catch(Exception e){System.err.println("[Rogler Core] HUD config save failed: "+e);}
        finally{if(temp!=null)try{Files.deleteIfExists(temp);}catch(Exception ignored){}}
    }
}
