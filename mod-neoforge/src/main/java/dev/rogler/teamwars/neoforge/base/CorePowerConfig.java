package dev.rogler.teamwars.neoforge.base;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import java.io.*;
import java.nio.file.*;
import java.util.Properties;

/** World-scoped bounded FE tuning; /teamwars admin reload rereads the file. */
public record CorePowerConfig(int baseFePerTick,int tier1FePerTick,int tier2FePerTick) {
    public static final CorePowerConfig DEFAULT=new CorePowerConfig(20,40,80);
    public CorePowerConfig {
        if(baseFePerTick<0 || tier1FePerTick<baseFePerTick || tier2FePerTick<tier1FePerTick
            || tier2FePerTick>256)
            throw new IllegalArgumentException("Require 0 <= base <= tier1 <= tier2 <= 256 FE/t");
    }
    public int rate(int tier) {
        return switch(tier) {
            case 0 -> baseFePerTick;
            case 1 -> tier1FePerTick;
            case 2 -> tier2FePerTick;
            default -> throw new IllegalArgumentException("invalid FE tier");
        };
    }
    public static CorePowerConfig load(MinecraftServer server) throws IOException {
        Path path=server.getWorldPath(LevelResource.ROOT).resolve("serverconfig/roglerteamwars-core-power.properties");
        if(Files.notExists(path)) {
            Files.createDirectories(path.getParent());
            Properties defaults=new Properties();
            defaults.setProperty("base_fe_per_tick","20");
            defaults.setProperty("tier1_fe_per_tick","40");
            defaults.setProperty("tier2_fe_per_tick","80");
            try(OutputStream stream=Files.newOutputStream(path)){
                defaults.store(stream,"TeamWars FE generator bounds; save then /teamwars admin reload");
            }
        }
        Properties p=new Properties();
        try(InputStream in=Files.newInputStream(path)){p.load(in);}
        try {
            return new CorePowerConfig(Integer.parseInt(p.getProperty("base_fe_per_tick","20")),
                Integer.parseInt(p.getProperty("tier1_fe_per_tick","40")),
                Integer.parseInt(p.getProperty("tier2_fe_per_tick","80")));
        } catch(IllegalArgumentException e) {throw new IOException("Invalid FE config: "+e.getMessage(),e);}
    }
}
