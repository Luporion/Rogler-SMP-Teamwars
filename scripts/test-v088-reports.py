#!/usr/bin/env python3
"""Execute the real report service/payload with lightweight MC/FTB test doubles.
Tests authority, membership changes, logout, captured feedback and codec bounds.
Does not prove the actual Minecraft ABI, GUI rendering or third-party runtime.
"""
from pathlib import Path
from tempfile import TemporaryDirectory
import subprocess
r=Path(__file__).resolve().parents[1]
actual=[r/'core/src/main/java/dev/rogler/teamwars/core/ui/DiagnosticPolicy.java']
base=r/'mod-neoforge/src/main/java/dev/rogler/teamwars/neoforge'
actual += [base/p for p in ['ui/ControlCenterReportService.java','ui/ControlCenterReportPayload.java','protection/FtbChunkStatusReader.java']]
stubs={
'net/minecraft/network/chat/Component.java': '''package net.minecraft.network.chat;
public record Component(String value){public static Component literal(String s){return new Component(s);}public String getString(){return value;}}''',
'net/minecraft/commands/CommandSource.java': '''package net.minecraft.commands;
public interface CommandSource {void sendSystemMessage(net.minecraft.network.chat.Component c);boolean acceptsSuccess();boolean acceptsFailure();boolean shouldInformAdmins();}''',
'net/minecraft/commands/CommandSourceStack.java': '''package net.minecraft.commands;
public record CommandSourceStack(int permission){public boolean hasPermission(int n){return permission>=n;}}''',
'net/minecraft/server/MinecraftServer.java': '''package net.minecraft.server;
public class MinecraftServer {public int tick=100;public int getTickCount(){return tick;}public net.minecraft.server.level.ServerLevel overworld(){return new net.minecraft.server.level.ServerLevel();}}''',
'net/minecraft/server/level/ServerLevel.java': '''package net.minecraft.server.level;
public class ServerLevel {public static int probes;public ServerLevel getChunkSource(){return this;}public Object getChunkNow(int x,int z){probes++;return null;}public net.minecraft.resources.ResourceKey dimension(){return new net.minecraft.resources.ResourceKey();}}''',
'net/minecraft/server/level/ServerPlayer.java': '''package net.minecraft.server.level;
public class ServerPlayer {public final java.util.UUID id=java.util.UUID.randomUUID();public final net.minecraft.server.MinecraftServer server=new net.minecraft.server.MinecraftServer();public int permission;
public java.util.UUID getUUID(){return id;}public net.minecraft.server.MinecraftServer getServer(){return server;}public net.minecraft.commands.CommandSourceStack createCommandSourceStack(){return new net.minecraft.commands.CommandSourceStack(permission);}}''',
'net/minecraft/world/level/ChunkPos.java': 'package net.minecraft.world.level; public record ChunkPos(int x,int z){}',
'net/minecraft/resources/ResourceKey.java': 'package net.minecraft.resources; public class ResourceKey {}',
'net/minecraft/resources/ResourceLocation.java': '''package net.minecraft.resources;public record ResourceLocation(String ns,String path){public static ResourceLocation fromNamespaceAndPath(String ns,String path){return new ResourceLocation(ns,path);}}''',
'net/neoforged/fml/ModList.java': '''package net.neoforged.fml;public class ModList {public static ModList get(){return new ModList();}public boolean isLoaded(String s){return false;}}''',
'net/neoforged/neoforge/network/PacketDistributor.java': '''package net.neoforged.neoforge.network;
public class PacketDistributor {public static Object last;public static int sends;public static void sendToPlayer(net.minecraft.server.level.ServerPlayer p,Object payload){last=payload;sends++;}}''',
'net/minecraft/network/protocol/common/custom/CustomPacketPayload.java': '''package net.minecraft.network.protocol.common.custom;
public interface CustomPacketPayload {record Type<T>(net.minecraft.resources.ResourceLocation id){}Type<? extends CustomPacketPayload> type();}''',
'net/minecraft/network/codec/StreamCodec.java': '''package net.minecraft.network.codec;
public record StreamCodec<B,V>(java.util.function.BiConsumer<B,V> writer,java.util.function.Function<B,V> reader){public static <B,V> StreamCodec<B,V> of(java.util.function.BiConsumer<B,V> w,java.util.function.Function<B,V> r){return new StreamCodec<>(w,r);}public void encode(B b,V v){writer.accept(b,v);}public V decode(B b){return reader.apply(b);}}''',
'net/minecraft/network/RegistryFriendlyByteBuf.java': '''package net.minecraft.network;
public class RegistryFriendlyByteBuf {private final java.util.List<Object> values=new java.util.ArrayList<>();private int at;
public void writeVarInt(int v){values.add(v);}public int readVarInt(){return (Integer)values.get(at++);}
public void writeLong(long v){values.add(v);}public long readLong(){return (Long)values.get(at++);}
public void writeBoolean(boolean v){values.add(v);}public boolean readBoolean(){return (Boolean)values.get(at++);}
public void writeUUID(java.util.UUID v){values.add(v);}public java.util.UUID readUUID(){return (java.util.UUID)values.get(at++);}
public void writeUtf(String s,int max){if(s.length()>max)throw new IllegalArgumentException();values.add(s);}
public String readUtf(int max){String s=(String)values.get(at++);if(s.length()>max)throw new IllegalArgumentException();return s;}}''',
'dev/rogler/teamwars/core/base/BaseCoreLedger.java': '''package dev.rogler.teamwars.core.base;
public class BaseCoreLedger {public enum Mode {PLACED,ISSUED}public record Location(String dimension,int x,int y,int z){}public record Entry(Mode mode,Location location){}public Entry get(java.util.UUID id){return null;}}''',
'dev/rogler/teamwars/neoforge/RoglerTeamWarsMod.java': '''package dev.rogler.teamwars.neoforge;
public class RoglerTeamWarsMod {public static final String MOD_ID="roglerteamwars",VERSION="0.8.8";
public record Config(){public Object zone(Object s){return null;}public int carrySeconds(){return 3600;}public int claimRadiusChunks(){return 1;}public boolean autoFtbClaims(){return true;}public boolean baseGuard(){return true;}}
public static Config baseConfig(){return new Config();}public static Config protectionConfig(){return new Config();}}''',
'dev/rogler/teamwars/neoforge/base/BaseCoreSavedData.java': '''package dev.rogler.teamwars.neoforge.base;
public class BaseCoreSavedData {public static BaseCoreSavedData get(Object s,Object z,int c){return new BaseCoreSavedData();}public dev.rogler.teamwars.core.base.BaseCoreLedger ledger(){return new dev.rogler.teamwars.core.base.BaseCoreLedger();}}''',
'dev/rogler/teamwars/neoforge/protection/BaseProtectionSavedData.java': '''package dev.rogler.teamwars.neoforge.protection;
public class BaseProtectionSavedData {public static class Record {public java.util.Set<Long> createdClaims=new java.util.HashSet<>();}public static BaseProtectionSavedData get(Object s){return new BaseProtectionSavedData();}public Record team(java.util.UUID id){return null;}}''',
'dev/rogler/teamwars/neoforge/protection/FtbClaimBridge.java': '''package dev.rogler.teamwars.neoforge.protection;
public class FtbClaimBridge {public static java.util.UUID resynced;public static String refreshOwnClaimView(Object p,java.util.UUID team){resynced=team;return "FTB resync sent to authorized member";}}''',
'dev/rogler/teamwars/neoforge/teams/FtbPartyReader.java': '''package dev.rogler.teamwars.neoforge.teams;
public class FtbPartyReader {public record Party(java.util.UUID id,java.util.Set<java.util.UUID> members){}public static Party current;public static int calls;public static int revokeAt=-1;
public Party partyFor(java.util.UUID player){calls++;if(calls==revokeAt)current=null;return current;}}''',
'dev/rogler/teamwars/neoforge/ui/ReportIntegrationTest.java': '''package dev.rogler.teamwars.neoforge.ui;
import java.util.*;import dev.rogler.teamwars.neoforge.teams.FtbPartyReader;import dev.rogler.teamwars.neoforge.protection.FtbClaimBridge;
import net.minecraft.server.level.ServerPlayer;import net.neoforged.neoforge.network.PacketDistributor;import net.minecraft.network.chat.Component;
public class ReportIntegrationTest {
static int checks;static void check(boolean v){checks++;if(!v)throw new AssertionError("check "+checks);}
static void deny(Runnable r){int before=PacketDistributor.sends;boolean refused=false;try{r.run();}catch(IllegalArgumentException e){refused=true;}check(refused);check(PacketDistributor.sends==before);}
static ControlCenterReportPayload packet(){return (ControlCenterReportPayload)PacketDistributor.last;}
static void advance(ServerPlayer p){p.server.tick+=50;}
public static void main(String[] args){
var p=new ServerPlayer();UUID own=UUID.randomUUID(),other=UUID.randomUUID();
FtbPartyReader.current=new FtbPartyReader.Party(own,Set.of(p.id));
ControlCenterReportService.inspect(p,"own","0|8");check(packet().target().equals(own));check(packet().rows().size()<=8);
advance(p);deny(()->ControlCenterReportService.inspect(p,"claims",other+"|0|3"));
advance(p);deny(()->ControlCenterReportService.inspect(p,"own",other+"|0|3"));
advance(p);FtbPartyReader.current=new FtbPartyReader.Party(other,Set.of(p.id));
ControlCenterReportService.inspect(p,"own","1|3");check(packet().target().equals(other));
advance(p);FtbPartyReader.current=new FtbPartyReader.Party(own,Set.of(UUID.randomUUID()));
ControlCenterReportService.inspect(p,"own","0|3");check(packet().target()==null);
advance(p);FtbPartyReader.current=new FtbPartyReader.Party(own,Set.of(p.id));
check(ControlCenterReportService.resyncOwn(p).contains("gesendet"));check(FtbClaimBridge.resynced.equals(own));
deny(()->ControlCenterReportService.resyncOwn(p));
p.permission=2;advance(p);ControlCenterReportService.inspect(p,"claims",other+"|0|8");check(packet().target().equals(other));
p.permission=0;advance(p);deny(()->ControlCenterReportService.inspect(p,"claims",other+"|1|8"));
// Membership revoked between the initial scope lookup and final packet send.
ControlCenterReportService.clear();FtbPartyReader.current=new FtbPartyReader.Party(own,Set.of(p.id));FtbPartyReader.calls=0;FtbPartyReader.revokeAt=2;
deny(()->ControlCenterReportService.inspect(p,"own","0|3"));FtbPartyReader.revokeAt=-1;
p.permission=2;advance(p);var capture=new ControlCenterReportService.CommandCapture();
capture.sendSystemMessage(Component.literal("Real command detail"));ControlCenterReportService.actionResult(p,"FTB-Adapter",1,capture);
check(packet().kind().equals("action"));check(packet().rows().stream().anyMatch(r->r.value().contains("Real command detail")));
var buf=new net.minecraft.network.RegistryFriendlyByteBuf();ControlCenterReportPayload.STREAM_CODEC.encode(buf,packet());check(ControlCenterReportPayload.STREAM_CODEC.decode(buf).equals(packet()));
capture=new ControlCenterReportService.CommandCapture();capture.sendSystemMessage(Component.literal("x".repeat(40000)));
ControlCenterReportService.actionResult(p,"Long output",1,capture);check(packet().total()==96);
advance(p);ControlCenterReportService.inspect(p,"action","90|8");check(packet().rows().size()==6);check(packet().rows().getLast().label().equals("Hinweis"));
ControlCenterReportService.logout(p.id);advance(p);deny(()->ControlCenterReportService.inspect(p,"action","0|3"));
check(net.minecraft.server.level.ServerLevel.probes==0);
System.out.println("v0.8.8 real report service/payload stub tests: "+checks+" checks OK (scope changes, denied packets, cache/logout, feedback and bounds)");
}}
'''
}
with TemporaryDirectory() as d:
    d=Path(d);files=[]
    for name,source in stubs.items():
        p=d/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(source,encoding='utf-8');files.append(str(p))
    subprocess.run(['javac','--release','21','-encoding','UTF-8','-d',str(d/'classes'),*files,*map(str,actual)],check=True)
    subprocess.run(['java','-ea','-cp',str(d/'classes'),'dev.rogler.teamwars.neoforge.ui.ReportIntegrationTest'],check=True)
