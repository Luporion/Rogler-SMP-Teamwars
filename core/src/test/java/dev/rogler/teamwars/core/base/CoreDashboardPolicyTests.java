package dev.rogler.teamwars.core.base;

public final class CoreDashboardPolicyTests {
    private static int checks;
    private static void check(boolean value){checks++;if(!value)throw new AssertionError("dashboard policy check "+checks);}
    public static void main(String[] args){
        for(int button=1;button<=3;button++){
            check(CoreDashboardPolicy.allowed(button,0,true,true,false));
            check(!CoreDashboardPolicy.allowed(button,0,false,true,false));
            check(!CoreDashboardPolicy.allowed(button,0,true,false,false));
            check(!CoreDashboardPolicy.allowed(button,0,true,true,true));
        }
        check(CoreDashboardPolicy.allowed(100,1,true,false,true));
        check(CoreDashboardPolicy.allowed(102,3,true,true,false));
        check(!CoreDashboardPolicy.allowed(101,1,true,true,false));
        check(!CoreDashboardPolicy.allowed(100,0,true,true,false));
        check(!CoreDashboardPolicy.allowed(100,1,false,true,false));
        check(!CoreDashboardPolicy.allowed(-1,5,true,true,false));
        check(!CoreDashboardPolicy.allowed(0,5,true,true,false));
        check(!CoreDashboardPolicy.allowed(10,5,true,true,false));
        check(!CoreDashboardPolicy.allowed(99,5,true,true,false));
        check(CoreDashboardPolicy.section(2).equals("ENERGY"));
        check(CoreDashboardPolicy.section(9).equals("INVALID"));
        System.out.println("v0.5.3 CoreDashboardPolicyTests: "+checks+" checks OK");
    }
}
