package dev.rogler.teamwars.neoforge.ui.client;

import net.minecraft.client.gui.screens.Screen;
import net.neoforged.fml.ModList;

import java.lang.reflect.Method;
import java.util.Objects;

/** Optional, read-only adapter to the verified XMMP fork. Only called from the client UI. */
final class XmmpMapStatus {
    private static final String CLIENT_CLASS = "dev.alinco8.xmmp.client.XMMPClient";
    private static final String CONFIG_CLASS = "dev.alinco8.xmmp.config.ConfigScreen";
    private static Access access;
    private static boolean resolved;

    private XmmpMapStatus() {}

    record Status(String version, String connection, String sharing, String details, boolean settings) {}

    private record Access(Object client, Method status, Method session, Method scope, Method party,
                          Method receiveTeam, Method receiveGlobal, Method allowGlobal, Method allowUploads,
                          Method diagnostics, Object config, Method screen) {}

    static Status read(String serverVersion) {
        String version = ModList.get().getModContainerById("xmmp")
            .map(mod -> mod.getModInfo().getVersion().toString()).orElse("");
        if (version.isEmpty()) return new Status("", "XMMP fehlt auf dem Client", "", "", false);
        if (!supported(version)) return new Status(version, "XMMP-Schnittstelle unbekannt", "", "", false);
        String conflict = serverVersion != null && !serverVersion.equals(version)
            ? "Versionshinweis · Server " + serverVersion + " · Client " + version : "";
        var api = access();
        if (api == null) return new Status(version, "XMMP-Status nicht verfügbar", conflict, "", false);
        try {
            Object status = api.status().invoke(api.client());
            if (status == null) return new Status(version, "Kein XMMP-Serverstatus", conflict, "", true);
            String scope = (String) api.scope().invoke(status);
            boolean globalAllowed = (boolean) api.allowGlobal().invoke(status);
            boolean uploads = (boolean) api.allowUploads().invoke(status);
            String partyScope = (String) api.party().invoke(status);
            boolean teamReceive = !partyScope.isEmpty()
                && (boolean) api.receiveTeam().invoke(status);
            boolean globalReceive = (boolean) api.receiveGlobal().invoke(status);
            String publish = scope.equals("global") ? "GLOBAL" : scope.startsWith("team-") ? "Team" : "Privat";
            String reception = (teamReceive ? "Team" : "Privat") + (globalReceive ? " + GLOBAL" : "");
            String statusVersion = serverVersion == null ? version : serverVersion;
            if (statusVersion.startsWith("0.4.2") && scope.startsWith("private-")) {
                reception = "Privat · weitere Freigaben prüfen (XMMP 0.4.2)";
            }
            String sharing = (globalAllowed ? "GLOBAL erlaubt · " : "") + "Senden: "
                + (uploads ? publish : "aus") + " · Empfang: " + reception;
            Object session = api.session().invoke(api.client());
            String details = session == null ? "" : Objects.toString(api.diagnostics().invoke(session), "");
            String connection = conflict.isEmpty() ? "XMMP verbunden" : "XMMP · Versionen prüfen";
            return new Status(version, connection, sharing, (conflict + " " + details).trim(), true);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError failure) {
            return new Status(version, "XMMP-Status nicht verfügbar", conflict, "", false);
        }
    }

    private static boolean supported(String version) {
        return version.matches("0\\.4\\.[23](?:\\+.*)?");
    }

    private static Access access() {
        if (resolved) return access;
        resolved = true;
        try {
            Class<?> client = Class.forName(CLIENT_CLASS);
            Method status = client.getMethod("getSharingStatus");
            Class<?> type = status.getReturnType();
            Method session = client.getMethod("getSession");
            Class<?> config = Class.forName(CONFIG_CLASS);
            access = new Access(client.getField("INSTANCE").get(null), status, session,
                type.getMethod("getActivePublishScope"), type.getMethod("getPartyScope"),
                type.getMethod("getReceiveTeam"), type.getMethod("getReceiveGlobal"),
                type.getMethod("getAllowGlobal"), type.getMethod("getAllowUploads"),
                session.getReturnType().getMethod("syncDiagnostics"),
                config.getField("INSTANCE").get(null), config.getMethod("createScreen", Screen.class));
        } catch (ReflectiveOperationException | RuntimeException | LinkageError failure) {
            // Keep the Core screen usable without a compatible optional mod.
        }
        return access;
    }

    static Screen settings(Screen parent) {
        var api = access();
        if (api == null) return null;
        try {
            return (Screen) api.screen().invoke(api.config(), parent);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError failure) {
            return null;
        }
    }
}
