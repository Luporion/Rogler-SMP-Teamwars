package dev.rogler.teamwars.core.base;
import java.util.UUID;
public class BaseProtectionPolicyTests {
    private static int count;
    private static void test(boolean truth) {if(!truth)throw new AssertionError("Protection test "+count); count++;}
    public static void main(String[] unused) {
        test(BaseProtectionPolicy.contains(0,0,0,0,1));
        test(BaseProtectionPolicy.contains(0,0,31,31,1));
        test(!BaseProtectionPolicy.contains(0,0,32,0,1));
        test(BaseProtectionPolicy.contains(-1,-1,-32,-32,1));
        test(!BaseProtectionPolicy.contains(-1,-1,-33,-1,1));
        test(BaseProtectionPolicy.overlaps(0,0,47,0,1));
        test(!BaseProtectionPolicy.overlaps(0,0,48,0,1));
        test(BaseProtectionPolicy.overlaps(-1,-1,-33,-33,1));
        UUID a=UUID.randomUUID(),b=UUID.randomUUID();
        test(BaseProtectionPolicy.canEdit(a,a));
        test(!BaseProtectionPolicy.canEdit(a,b));
        test(!BaseProtectionPolicy.canEdit(a,null));
        test(BaseProtectionPolicy.graceRemainingMillis(100L,200L)==100L);
        test(BaseProtectionPolicy.graceRemainingMillis(200L,100L)==0L);
        System.out.println("v0.3.0 BaseProtectionPolicyTests: "+count+" checks OK");
    }
}
