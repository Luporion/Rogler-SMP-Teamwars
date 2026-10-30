#!/usr/bin/env python3
"""Compile actual CoreKineticBlockEntity against minimal Create/MC stubs and
exercise its save/read/tick cache. NOT a substitute for NeoForge CI/runtime.
"""
from pathlib import Path
from tempfile import TemporaryDirectory
import subprocess
root=Path(__file__).resolve().parents[1]
actual=root/'mod-neoforge/src/main/java/dev/rogler/teamwars/neoforge/base/CoreKineticBlockEntity.java'
stubs={
'net/minecraft/core/BlockPos.java': 'package net.minecraft.core; public record BlockPos(int x,int y,int z) {}',
'net/minecraft/core/HolderLookup.java': 'package net.minecraft.core; public class HolderLookup { public interface Provider {} }',
'net/minecraft/ChatFormatting.java': 'package net.minecraft; public enum ChatFormatting { AQUA, GRAY }',
'net/minecraft/nbt/CompoundTag.java': '''package net.minecraft.nbt; public class CompoundTag {
    private final java.util.Map<String, Boolean> values = new java.util.HashMap<>();
    public void putBoolean(String key, boolean value) {values.put(key,value);}
    public boolean getBoolean(String key) {return values.getOrDefault(key,false);}
}''',
'net/minecraft/network/chat/Component.java': '''package net.minecraft.network.chat;
public class Component {public static Component literal(String s){return new Component();}
public Component withStyle(net.minecraft.ChatFormatting fmt){return this;}}''',
'net/minecraft/server/MinecraftServer.java': 'package net.minecraft.server; public class MinecraftServer {}',
'net/minecraft/server/level/ServerLevel.java': '''package net.minecraft.server.level;
public class ServerLevel extends net.minecraft.world.level.Level {
 private final net.minecraft.server.MinecraftServer server=new net.minecraft.server.MinecraftServer();
 public net.minecraft.server.MinecraftServer getServer(){return server;}
}''',
'net/minecraft/world/level/Level.java': 'package net.minecraft.world.level; public class Level { public boolean isClientSide=false; }',
'net/minecraft/world/level/block/entity/BlockEntityType.java': 'package net.minecraft.world.level.block.entity; public class BlockEntityType<T> {}',
'net/minecraft/world/level/block/state/BlockState.java': 'package net.minecraft.world.level.block.state; public class BlockState {}',
'com/simibubi/create/content/kinetics/base/GeneratingKineticBlockEntity.java': '''package com.simibubi.create.content.kinetics.base;
import java.util.List;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
public class GeneratingKineticBlockEntity {
 public net.minecraft.world.level.Level level;
 protected BlockPos worldPosition;
 public int updates;
 public GeneratingKineticBlockEntity(BlockEntityType<?> t,BlockPos p,BlockState s){worldPosition=p;}
 public float getGeneratedSpeed(){return 0;}
 public void initialize(){} public void tick(){}
 protected void write(CompoundTag tag,HolderLookup.Provider registries,boolean clientPacket){}
 protected void read(CompoundTag tag,HolderLookup.Provider registries,boolean clientPacket){}
 public boolean hasSource(){return false;} public float getTheoreticalSpeed(){return 0;}
 public void updateGeneratedRotation(){updates++;}
 public void setChanged(){} public void sendData(){}
 public boolean addToGoggleTooltip(List<Component> tooltip,boolean sneaking){return false;}
}''',
'dev/rogler/teamwars/core/base/CoreKineticPolicy.java': 'package dev.rogler.teamwars.core.base; public class CoreKineticPolicy { public static final float RPM=16; }',
'dev/rogler/teamwars/neoforge/base/CoreInfrastructureService.java': '''package dev.rogler.teamwars.neoforge.base;
public class CoreInfrastructureService {
 public static int ledgerLookups;
 public static final java.util.UUID OWNER=java.util.UUID.randomUUID();
 public static java.util.UUID recordedOwner(net.minecraft.server.level.ServerLevel l,net.minecraft.core.BlockPos pos){ledgerLookups++;return OWNER;}
}''',
'dev/rogler/teamwars/neoforge/base/CoreInfrastructureSavedData.java': '''package dev.rogler.teamwars.neoforge.base;
public class CoreInfrastructureSavedData {
 public static class TeamRecord { public int createLevel=1; }
 public static TeamRecord record=new TeamRecord();
 public static CoreInfrastructureSavedData get(net.minecraft.server.MinecraftServer s){return new CoreInfrastructureSavedData();}
 public TeamRecord find(java.util.UUID id){return record;}
}''',
'dev/rogler/teamwars/neoforge/base/SerializationTest.java': '''package dev.rogler.teamwars.neoforge.base;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntityType;
public class SerializationTest {
 public static void main(String[] args){
  var be=new CoreKineticBlockEntity(new BlockEntityType<>(),new BlockPos(1,64,1),new BlockState());
  be.level=new ServerLevel();
  be.initialize();
  if(be.getGeneratedSpeed()!=16 || CoreInfrastructureService.ledgerLookups<1)throw new AssertionError("Initial drive sync failed");
  CoreInfrastructureService.ledgerLookups=0;
  var tag=new CompoundTag();
  be.write(tag,null,false);
  be.getGeneratedSpeed();
  be.addToGoggleTooltip(new java.util.ArrayList<>(),false);
  if(!tag.getBoolean("TeamWarsDriveEnabled") || CoreInfrastructureService.ledgerLookups!=0)
    throw new AssertionError("Serialization or speed getter accessed world/ledger");
  CoreInfrastructureSavedData.record.createLevel=0;
  for(int i=0;i<22;i++)be.tick();
  if(be.getGeneratedSpeed()!=0)throw new AssertionError("Tick did not reconcile downgrade");
  CoreInfrastructureService.ledgerLookups=0;
  var disabled=new CompoundTag();be.write(disabled,null,false);
  if(disabled.getBoolean("TeamWarsDriveEnabled") || CoreInfrastructureService.ledgerLookups!=0)
    throw new AssertionError("Saved stale drive state or queried ledger");
  be.read(tag,null,false);
  if(be.getGeneratedSpeed()!=16)throw new AssertionError("NBT load lost cache");
  for(int i=0;i<20;i++)be.tick();
  if(be.getGeneratedSpeed()!=0)throw new AssertionError("NBT cache was not reconciled with server ledger");
  System.out.println("v0.8.3 Create kinetic serialization/tick/read stub tests: 6 checks OK");
 }
}'''
}
with TemporaryDirectory(prefix='rogler083kinetic') as temp:
    tmp=Path(temp)
    for name,content in stubs.items():
        p=tmp/'src'/name; p.parent.mkdir(parents=True,exist_ok=True);p.write_text(content)
    files=[str(f) for f in (tmp/'src').rglob('*.java')]+[str(actual)]
    output=tmp/'classes';output.mkdir()
    subprocess.run(['javac','--release','21','-encoding','UTF-8','-d',str(output),*files],check=True,timeout=45)
    subprocess.run(['java','-ea','-cp',str(output),'dev.rogler.teamwars.neoforge.base.SerializationTest'],check=True,timeout=15)
