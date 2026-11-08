#!/usr/bin/env python3
"""Compile the real HUD editor against minimal Java API stubs and test save/cancel.
Does NOT verify the actual Minecraft 1.21.1 Screen ABI or renderer.
"""
from pathlib import Path
from tempfile import TemporaryDirectory
import subprocess
root=Path(__file__).resolve().parents[1]
actual=[root/'mod-neoforge/src/main/java/dev/rogler/teamwars/neoforge/map/client/HudPositionScreen.java',
        root/'core/src/main/java/dev/rogler/teamwars/core/HudPositionPolicy.java',
        root/'core/src/main/java/dev/rogler/teamwars/core/HudPreferences.java']
stubs={
'net/minecraft/network/chat/Component.java': 'package net.minecraft.network.chat; public record Component(String value) { public static Component literal(String s){return new Component(s);} }',
'net/minecraft/client/gui/Font.java': 'package net.minecraft.client.gui; public class Font {}',
'net/minecraft/client/gui/GuiGraphics.java': '''package net.minecraft.client.gui; public class GuiGraphics {
 public void fill(int a,int b,int c,int d,int color){}
 public void drawString(Font font,String s,int x,int y,int color,boolean shadow){}
 public void drawCenteredString(Font font,String s,int x,int y,int color){}
}''',
'net/minecraft/client/Minecraft.java': '''package net.minecraft.client;
public class Minecraft { public net.minecraft.client.gui.screens.Screen screen;
 public void setScreen(net.minecraft.client.gui.screens.Screen s){ screen=s; }
}''',
'net/minecraft/client/gui/components/Button.java': '''package net.minecraft.client.gui.components;
import net.minecraft.network.chat.Component;
import java.util.function.Consumer;
public class Button {
 public Component text; public Consumer<Button> action;
 Button(Component c,Consumer<Button> a){text=c;action=a;}
 public static Builder builder(Component c,Consumer<Button> a){return new Builder(c,a);}
 public static class Builder {Component c;Consumer<Button> a;Builder(Component c,Consumer<Button> a){this.c=c;this.a=a;}
 public Builder bounds(int x,int y,int w,int h){return this;}
 public Button build(){return new Button(c,a);}}
 public void press(){action.accept(this);}
}''',
'net/minecraft/client/gui/screens/Screen.java': '''package net.minecraft.client.gui.screens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import java.util.ArrayList;
public class Screen {
 public int width=450,height=300; public final Font font=new Font();
 public final Minecraft minecraft=new Minecraft(); public final java.util.List<Button> buttons=new ArrayList<>();
 public Screen(Component c){} protected void init(){}
 protected void clearWidgets(){buttons.clear();}
 protected void addRenderableWidget(Button b){buttons.add(b);}
 public boolean mouseClicked(double x,double y,int b){return false;}
 public boolean mouseDragged(double x,double y,int b,double dx,double dy){return false;}
 public boolean mouseReleased(double x,double y,int b){return false;}
 public void render(GuiGraphics g,int x,int y,float f){}
 public void onClose(){}
}''',
'dev/rogler/teamwars/neoforge/map/client/HudSettingsStore.java': '''package dev.rogler.teamwars.neoforge.map.client;
import dev.rogler.teamwars.core.HudPreferences;
public class HudSettingsStore {
 public static HudPreferences value=HudPreferences.DEFAULT;
 public static int writes;
 public static HudPreferences get(){return value;}
 public static void update(HudPreferences prefs){value=prefs;writes++;}
}''',
'dev/rogler/teamwars/neoforge/map/client/HudEditorTest.java': '''package dev.rogler.teamwars.neoforge.map.client;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
public class HudEditorTest {
 public static void main(String[] args) throws Exception {
  Screen parent = new Screen(Component.literal("parent"));
  var cancelled = new HudPositionScreen(parent);
  var init=HudPositionScreen.class.getDeclaredMethod("init");init.setAccessible(true);
  init.invoke(cancelled);
  cancelled.mouseClicked(255,26,0);cancelled.mouseDragged(120,100,0,-135,74);cancelled.mouseReleased(120,100,0);
  if(HudSettingsStore.writes!=0)throw new AssertionError("drag wrote disk early");
  cancelled.onClose();
  if(HudSettingsStore.writes!=0)throw new AssertionError("cancel saved draft");
  var editing = new HudPositionScreen(parent);init.invoke(editing);
  editing.mouseClicked(255,26,0);editing.mouseDragged(120,100,0,-135,74);editing.mouseReleased(120,100,0);
  editing.buttons.stream().filter(b->b.text.value().equals("Speichern")).findFirst().orElseThrow().press();
  if(HudSettingsStore.writes!=1 || HudSettingsStore.value.offsetX()==12)
    throw new AssertionError("drag position not saved to local store");
  System.out.println("v0.8.5 HUD editor stub save/cancel checks: 2 OK");
 }
}'''
}
with TemporaryDirectory(prefix='rogler085hud') as name:
    tmp=Path(name)
    for file,body in stubs.items():
        dst=tmp/'src'/file;dst.parent.mkdir(parents=True,exist_ok=True);dst.write_text(body)
    out=tmp/'classes';out.mkdir()
    subprocess.run(['javac','--release','21','-encoding','UTF-8','-d',str(out),
       *map(str,(tmp/'src').rglob('*.java')),*map(str,actual)],check=True,timeout=45)
    subprocess.run(['java','-ea','-cp',str(out),'dev.rogler.teamwars.neoforge.map.client.HudEditorTest'],check=True,timeout=15)
