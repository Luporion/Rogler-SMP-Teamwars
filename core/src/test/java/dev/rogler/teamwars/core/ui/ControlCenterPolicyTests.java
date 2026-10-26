package dev.rogler.teamwars.core.ui;

public final class ControlCenterPolicyTests {
    static int checks;
    static void yes(boolean result){if(!result)throw new AssertionError("Check " + checks);checks++;}
    static void reject(Runnable work){boolean threw=false;try{work.run();}catch(IllegalArgumentException ok){threw=true;}yes(threw);}
    public static void main(String[] args){
        yes(ControlCenterPolicy.FIELDS.size()==24);
        for(var field:ControlCenterPolicy.FIELDS){
            yes(ControlCenterPolicy.require(field.group()+"."+field.key())==field);
            yes(ControlCenterPolicy.allowed(4,field));
            yes(ControlCenterPolicy.allowed(2,field)==(field.permission()==2));
            if(field.key().equals("warium_fusionblast_recovery")) {
                yes(field.normalize("log_only").equals("LOG_ONLY"));reject(()->field.normalize("EXPLODE"));
            } else if(field.key().equals("warium_fusionblast_entity_id")) {
                yes(field.normalize("wariumce:fusionblast").equals("wariumce:fusionblast"));
                reject(()->field.normalize("minecraft:creeper"));
            } else if(field.booleanValue()){
                yes(field.normalize("TRUE").equals("true"));yes(field.normalize("false").equals("false"));
                reject(()->field.normalize("yes"));
            }else{
                yes(field.normalize(Integer.toString(field.minimum())).equals(Integer.toString(field.minimum())));
                yes(field.normalize(Integer.toString(field.maximum())).equals(Integer.toString(field.maximum())));
                if(field.minimum()>0)reject(()->field.normalize(Integer.toString(field.minimum()-1)));
                reject(()->field.normalize(Integer.toString(field.maximum()+1)));
            }
        }
        reject(()->ControlCenterPolicy.require("java.class"));
        reject(()->ControlCenterPolicy.require("../../config"));
        reject(()->ControlCenterPolicy.require("teams.."));
        yes(ControlCenterPolicy.sensitive(ControlCenterPolicy.require("protection.base_guard_enabled")));
        yes(!ControlCenterPolicy.sensitive(ControlCenterPolicy.require("teams.max_parties")));
        System.out.println("v0.8.2 ControlCenterPolicyTests: "+checks+" checks OK");
    }
}
