package dev.rogler.teamwars.core;

import java.nio.file.Path;
import java.util.*;

/** No Minecraft dependency. Demonstrates the core contract offline, NOT a server-installable mod. */
public final class Main {
    private Main() {}
    public static void main(String[] args) throws Exception {
        UUID anna = UUID.nameUUIDFromBytes("anna".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        UUID ben = UUID.nameUUIDFromBytes("ben".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        ExplorationIndex idx = new ExplorationIndex();
        ChunkKey shared = new ChunkKey("minecraft:overworld", 3, 4);
        ChunkKey onlyAnna = new ChunkKey("minecraft:overworld", 3, 5);
        idx.discover(anna, shared); idx.discover(anna, onlyAnna); idx.discover(ben, shared);
        System.out.println("Team map before: " + idx.visibleTo(Set.of(anna,ben)));
        System.out.println("After Anna leaves: " + idx.visibleTo(Set.of(ben)));
        System.out.println("Visibility revoked: " + idx.delta(Set.of(anna,ben), Set.of(ben)).revoked());
        if (args.length == 2 && args[0].equals("--snapshot")) {
            ExplorationStore.write(Path.of(args[1]), idx);
            System.out.println("Roundtrip: " + ExplorationStore.read(Path.of(args[1])).visibleTo(Set.of(anna,ben)));
        }
    }
}
