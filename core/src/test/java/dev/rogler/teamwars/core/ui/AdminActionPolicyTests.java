package dev.rogler.teamwars.core.ui;

public final class AdminActionPolicyTests {
    private static int checks;
    private static void require(boolean condition){checks++;if(!condition)throw new AssertionError("AdminActionPolicy check "+checks);}
    private static void reject(String id,String args){
        boolean refused=false;
        try{AdminActionPolicy.command(id,args);}catch(IllegalArgumentException expected){refused=true;}
        require(refused);
    }
    public static void main(String[] args){
        require(AdminActionPolicy.ACTIONS.size()>=15);
        require(AdminActionPolicy.DIAGNOSTICS.size()>=10);
        for(var action:AdminActionPolicy.ACTIONS){
            require(AdminActionPolicy.require(action.id())==action);
            require(AdminActionPolicy.allowed(action.permission(),action));
            require(!AdminActionPolicy.allowed(0,action));
            if(action.permission()==4)require(!AdminActionPolicy.allowed(2,action));
            if(action.argument().isEmpty()){
                require(AdminActionPolicy.command(action.id(),"").equals("teamwars admin "+action.command()));
                reject(action.id(),"foo");
            }
        }
        String uuid="12345678-1234-1234-1234-123456789012";
        require(AdminActionPolicy.command("claimaudit",uuid).endsWith(" "+uuid));
        require(AdminActionPolicy.command("raidstartteam",uuid+"|10").endsWith(" "+uuid+" 10"));
        require(AdminActionPolicy.command("raidstopteam",uuid).endsWith(" "+uuid));
        require(AdminActionPolicy.command("grace","Valid_Name|120").endsWith(" Valid_Name 120"));
        require(AdminActionPolicy.command("adoptcore",uuid+"|GoodName").endsWith(" "+uuid+" GoodName"));
        reject("nonexistent","anything");
        reject("grace","Name|abc");reject("grace","Name|-1");reject("grace","Name|10081");
        reject("raidstartteam",uuid+"|31");reject("raidstartteam",uuid+"|0");
        reject("claimaudit","not-a-uuid");reject("claimaudit",uuid+" |say hack");
        reject("grace","Bad;Name|10");reject("grace","Bad Name|10");
        reject("grace","GoodName|10|evil");reject("adoptcore",uuid+"|Name|evil");
        reject("adoptcore",uuid+"|Bad Name");reject("diagnostics","not-empty");
        reject("raidstopteam",uuid+"; stop");
        reject("grace","X".repeat(67));
        System.out.println("v0.8.4 AdminActionPolicyTests: "+checks+" checks OK");
    }
}
