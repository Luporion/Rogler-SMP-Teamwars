package dev.rogler.teamwars.core.base;

/** Pure rule for exchanging a real vanilla book for a mod's registered guide.
 *  The item transfer itself remains atomic and server-authoritative.
 */
public final class CoreManualTradePolicy {
    private CoreManualTradePolicy() {}
    public static boolean mayTrade(boolean nearOwnCore,boolean carriesGuide,int vanillaBooks,boolean outputRoom){
        return nearOwnCore && !carriesGuide && vanillaBooks>=1 && outputRoom;
    }
}
