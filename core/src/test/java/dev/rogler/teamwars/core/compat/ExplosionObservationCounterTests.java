package dev.rogler.teamwars.core.compat;
public final class ExplosionObservationCounterTests {
 private static int checks;
 private static void ok(boolean b){checks++;if(!b)throw new AssertionError("counter "+checks);}
 public static void main(String[] args){
   var c=new ExplosionObservationCounter();
   ok(c.events()==0);ok(c.maximumAffected()==0);
   c.observe("minecraft:overworld",80);c.observe("minecraft:overworld",10_000);c.observe("sable:plot",-100);
   ok(c.events()==3);ok(c.blockPositions()==10_080);ok(c.maximumAffected()==10_000);
   ok(c.largeEvents()==1);ok(c.dimensions().get("minecraft:overworld")==2);
   ok(c.dimensions().get("sable:plot")==1);
   for(int i=0;i<100;i++)c.observe("dimension:"+i,i);
   ok(c.dimensions().size()<=ExplosionObservationCounter.MAX_DIMENSIONS);
   ok(c.dimensions().containsKey("other"));ok(c.events()==103);
   c.clear();ok(c.events()==0);ok(c.dimensions().isEmpty());ok(c.largeEvents()==0);
   System.out.println("v0.6.3 ExplosionObservationCounterTests: "+checks+" checks OK");
 }
}
