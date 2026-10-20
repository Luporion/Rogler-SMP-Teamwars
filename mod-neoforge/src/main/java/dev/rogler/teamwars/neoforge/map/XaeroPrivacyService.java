package dev.rogler.teamwars.neoforge.map;

/**
 * v0.7.3: Xaero server profiles handle radar and cave mode independently.
 * The legacy fair-play chat marker is DELIBERATELY not transmitted because it
 * would also disable cave mode and has no reliable server-side selective reset.
 * See deployment/xaero/{minimap,world-map}/server_profiles/default.cfg.
 */
public final class XaeroPrivacyService {
    private XaeroPrivacyService() {}
}
