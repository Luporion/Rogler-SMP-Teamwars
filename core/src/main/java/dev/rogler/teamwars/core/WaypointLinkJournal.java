package dev.rogler.teamwars.core;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

/** Local-only, versioned, bounded journal. Never stores personal pins in server SavedData. */
public final class WaypointLinkJournal {
    private WaypointLinkJournal() {}
    private static final String FORMAT = "1";
    public static List<WaypointLinkPolicy.Link> load(Path file) throws IOException {
        if (!Files.exists(file)) return List.of();
        if (Files.size(file)>256_000) throw new IOException("waypoint journal too large: "+file);
        Properties p=new Properties();
        try(InputStream input=Files.newInputStream(file)){p.load(input);}
        if (!FORMAT.equals(p.getProperty("format"))) throw new IOException("unknown waypoint journal format");
        int count;
        try {count=Integer.parseInt(p.getProperty("count","-1"));}
        catch(NumberFormatException e){throw new IOException("invalid waypoint journal count",e);}
        if(count<0||count>WaypointLinkPolicy.MAX_LINKS)throw new IOException("invalid waypoint journal size");
        List<WaypointLinkPolicy.Link> links=new ArrayList<>();
        Set<UUID> ids=new HashSet<>();Set<String> positions=new HashSet<>();
        for(int i=0;i<count;i++) {
            String data=p.getProperty("link."+i);
            if(data==null)throw new IOException("missing waypoint entry #"+i);
            try (var in=new DataInputStream(new ByteArrayInputStream(Base64.getDecoder().decode(data)))) {
                var id=UUID.fromString(in.readUTF());
                var f=readPoint(in);var x=readPoint(in);
                if(in.available()!=0)throw new IOException("trailing waypoint journal bytes");
                var link=new WaypointLinkPolicy.Link(id,f,x);
                if(!ids.add(id)||!positions.add(link.locationKey()))throw new IOException("duplicate waypoint identity/location");
                links.add(link);
            } catch(IllegalArgumentException ex){throw new IOException("invalid waypoint entry #"+i,ex);}
        }
        return List.copyOf(links);
    }
    public static void save(Path file,List<WaypointLinkPolicy.Link> entries) throws IOException {
        if(entries.size()>WaypointLinkPolicy.MAX_LINKS)throw new IOException("too many waypoint links");
        Properties p=new Properties();p.setProperty("format",FORMAT);p.setProperty("count",Integer.toString(entries.size()));
        Set<UUID> ids=new HashSet<>();Set<String> locations=new HashSet<>();
        for(int i=0;i<entries.size();i++) {
            var link=entries.get(i);
            if(!ids.add(link.id())||!locations.add(link.locationKey()))throw new IOException("duplicate waypoint link");
            var bytes=new ByteArrayOutputStream();
            try(var out=new DataOutputStream(bytes)) {
                out.writeUTF(link.id().toString());writePoint(out,link.ftbBaseline());writePoint(out,link.xaeroBaseline());
            }
            p.setProperty("link."+i,Base64.getEncoder().encodeToString(bytes.toByteArray()));
        }
        Path parent=file.toAbsolutePath().getParent();
        Files.createDirectories(parent);
        Path tmp=Files.createTempFile(parent,".rogler-waypoints-",".tmp");
        try {
            try(OutputStream output=Files.newOutputStream(tmp)){p.store(output,"Rogler Core private waypoint link journal - never upload");}
            try {Files.move(tmp,file,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
            catch(AtomicMoveNotSupportedException e){Files.move(tmp,file,StandardCopyOption.REPLACE_EXISTING);}
        } finally {Files.deleteIfExists(tmp);}
    }
    private static WaypointSyncPolicy.Point readPoint(DataInputStream in) throws IOException {
        String dimension=in.readUTF();int x=in.readInt(),y=in.readInt(),z=in.readInt();
        String name=in.readUTF();int color=in.readInt();
        return new WaypointSyncPolicy.Point(dimension,x,y,z,name,color);
    }
    private static void writePoint(DataOutputStream out,WaypointSyncPolicy.Point p) throws IOException {
        out.writeUTF(p.dimension());out.writeInt(p.x());out.writeInt(p.y());out.writeInt(p.z());
        out.writeUTF(p.name());out.writeInt(p.color());
    }
}
