package dev.rogler.teamwars.core;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.CRC32;

/** Versioned, checksummed, atomic snapshot; call only from server-owned thread or detached snapshot. */
public final class ExplorationStore {
    private static final int MAGIC = 0x52545745; // RTWE
    private static final int VERSION = 1;
    private static final int MAX_PLAYERS = 1_000_000;
    private static final int MAX_CHUNKS_PER_PLAYER = 20_000_000;
    private ExplorationStore() {}
    public static void write(Path file, ExplorationIndex index) throws IOException {
        Path absolute = file.toAbsolutePath();
        Files.createDirectories(absolute.getParent());
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bytes)) {
            Map<UUID, Set<ChunkKey>> data = index.exportSnapshot();
            out.writeInt(MAGIC); out.writeInt(VERSION); out.writeInt(data.size());
            for (UUID id : new TreeSet<>(data.keySet())) {
                out.writeLong(id.getMostSignificantBits()); out.writeLong(id.getLeastSignificantBits());
                TreeSet<ChunkKey> chunks = new TreeSet<>(data.get(id));
                out.writeInt(chunks.size());
                for (ChunkKey chunk : chunks) {
                    out.writeUTF(chunk.dimension()); out.writeInt(chunk.x()); out.writeInt(chunk.z());
                }
            }
        }
        byte[] payload = bytes.toByteArray();
        CRC32 crc = new CRC32(); crc.update(payload);
        Path temp = Files.createTempFile(absolute.getParent(), "teamwars-exp-", ".tmp");
        try {
            try (DataOutputStream out = new DataOutputStream(Files.newOutputStream(temp))) {
                out.writeInt(payload.length); out.write(payload); out.writeLong(crc.getValue());
            }
            try { Files.move(temp, absolute, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException ex) { Files.move(temp, absolute, StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temp); }
    }
    public static ExplorationIndex read(Path file) throws IOException {
        try (DataInputStream in = new DataInputStream(Files.newInputStream(file))) {
            int size = in.readInt();
            if (size < 12 || size > 512_000_000) throw new IOException("Snapshot payload too large or invalid");
            byte[] bytes = new byte[size]; in.readFully(bytes);
            long stored = in.readLong();
            if (in.read() != -1) throw new IOException("Trailing snapshot bytes");
            CRC32 crc = new CRC32(); crc.update(bytes);
            if (stored != crc.getValue()) throw new IOException("Checksum mismatch");
            try (DataInputStream data = new DataInputStream(new ByteArrayInputStream(bytes))) {
                if (data.readInt() != MAGIC || data.readInt() != VERSION) throw new IOException("Unsupported snapshot format");
                int players = data.readInt();
                if (players < 0 || players > MAX_PLAYERS) throw new IOException("Invalid player count");
                Map<UUID, Set<ChunkKey>> map = new HashMap<>();
                for (int p = 0; p < players; p++) {
                    UUID id = new UUID(data.readLong(), data.readLong());
                    int count = data.readInt();
                    if (count < 0 || count > MAX_CHUNKS_PER_PLAYER) throw new IOException("Invalid chunk count");
                    Set<ChunkKey> chunks = new HashSet<>();
                    for (int i = 0; i < count; i++) chunks.add(new ChunkKey(data.readUTF(),data.readInt(),data.readInt()));
                    if (map.put(id, chunks) != null) throw new IOException("Duplicate player UUID");
                }
                if (data.read() != -1) throw new IOException("Trailing snapshot payload");
                return ExplorationIndex.fromSnapshot(map);
            }
        } catch (IllegalArgumentException ex) { throw new IOException("Invalid snapshot content", ex); }
    }
}
