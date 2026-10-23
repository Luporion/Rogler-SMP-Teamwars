package dev.rogler.teamwars.core;

import java.nio.file.Files;
import java.util.List;
import java.util.UUID;

public final class WaypointLinkJournalTests {
    private static int checks;
    private static void ok(boolean value){checks++;if(!value)throw new AssertionError("journal test "+checks);}
    private static WaypointSyncPolicy.Point p(String n){return new WaypointSyncPolicy.Point("minecraft:overworld",31,67,-77,n,0xaabbcc);}
    public static void main(String[] args) throws Exception {
        var dir=Files.createTempDirectory("rogler-waypoint-journal-test");var file=dir.resolve("links.properties");
        try {
            ok(WaypointLinkJournal.load(file).isEmpty());
            var link=new WaypointLinkPolicy.Link(UUID.randomUUID(),p("§Test Ä"),p("Original"));
            WaypointLinkJournal.save(file,List.of(link));
            ok(Files.exists(file));ok(WaypointLinkJournal.load(file).equals(List.of(link)));
            ok(!Files.readString(file).contains("§Test Ä"));
            var old=Files.readAllBytes(file);
            try {WaypointLinkJournal.save(file,List.of(link,link));throw new AssertionError();}
            catch(java.io.IOException expected){checks++;}
            ok(java.util.Arrays.equals(old,Files.readAllBytes(file)));
            Files.writeString(file,"format=999\ncount=0\n");
            try {WaypointLinkJournal.load(file);throw new AssertionError();}catch(java.io.IOException expected){checks++;}
            Files.writeString(file,"format=1\ncount=1\nlink.0=invalid base64!!\n");
            try {WaypointLinkJournal.load(file);throw new AssertionError();}catch(java.io.IOException expected){checks++;}
            WaypointLinkJournal.save(file,List.of());ok(WaypointLinkJournal.load(file).isEmpty());
            Files.writeString(file,"format=1\ncount=129\n");
            try {WaypointLinkJournal.load(file);throw new AssertionError();}catch(java.io.IOException expected){checks++;}
            System.out.println("v0.8.0 WaypointLinkJournalTests: "+checks+" checks OK");
        } finally {Files.deleteIfExists(file);Files.deleteIfExists(dir);}
    }
}
