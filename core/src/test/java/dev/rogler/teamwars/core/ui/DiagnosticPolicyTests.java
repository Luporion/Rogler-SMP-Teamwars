package dev.rogler.teamwars.core.ui;

import java.util.List;
import java.util.UUID;

public final class DiagnosticPolicyTests {
    private static int checks;
    private static void check(boolean result){checks++;if(!result)throw new AssertionError("check "+checks);}
    private static void denied(Runnable work){boolean refused=false;try{work.run();}catch(IllegalArgumentException ex){refused=true;}check(refused);}
    public static void main(String[] args) {
        UUID own=UUID.randomUUID(),other=UUID.randomUUID();
        var request=DiagnosticPolicy.parse("own","0|3",0,own);
        check(request.target().equals(own));check(request.limit()==3);
        denied(()->DiagnosticPolicy.parse("own",other+"|0|3",0,own));
        denied(()->DiagnosticPolicy.parse("claims",other+"|0|3",0,own));
        denied(()->DiagnosticPolicy.parse("action","0|3",0,own));
        check(DiagnosticPolicy.parse("claims",other+"|0|8",2,own).target().equals(other));
        check(DiagnosticPolicy.parse("own","0|1",0,null).target()==null);
        for(String kind:List.of("set","adminrun","../../claims",""))denied(()->DiagnosticPolicy.parse(kind,"0|3",4,own));
        for(String value:List.of("-1|3","96|3","0|0","0|9","x|1","0|3|extra","0","0|"))
            denied(()->DiagnosticPolicy.parse("own",value,4,own));
        var rows=List.of(new DiagnosticPolicy.Row("a","b"),new DiagnosticPolicy.Row("c","d"));
        check(DiagnosticPolicy.page(rows,0,1).equals(rows.subList(0,1)));
        check(DiagnosticPolicy.page(rows,1,8).equals(rows.subList(1,2)));
        check(DiagnosticPolicy.page(rows,2,3).isEmpty());
        check(DiagnosticPolicy.page(rows,95,1).isEmpty());
        denied(()->DiagnosticPolicy.page(rows,0,9));
        denied(()->DiagnosticPolicy.page(java.util.Collections.nCopies(97,rows.getFirst()),0,3));
        check(new DiagnosticPolicy.Row("x".repeat(100),"y".repeat(500)).label().length()==48);
        check(new DiagnosticPolicy.Row("x","y".repeat(500)).value().length()==180);
        check(!new DiagnosticPolicy.Row("a\nb","c\t\u00a7d").toString().contains("\n"));
        check(DiagnosticPolicy.yesNo(null).equals("unbekannt"));
        check(DiagnosticPolicy.yesNo(false).equals("nein"));
        check(DiagnosticPolicy.count(-1).equals("unbekannt"));
        check(DiagnosticPolicy.remaining(9,25).equals("16"));
        check(DiagnosticPolicy.remaining(30,25).equals("0"));
        check(DiagnosticPolicy.remaining(null,25).equals("unbekannt"));
        System.out.println("v0.8.8 DiagnosticPolicyTests: "+checks+" checks OK");
    }
}
