package dev.rogler.teamwars.neoforge.teams;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RoglerTeamWarsLogger {
    private static final Logger LOGGER = LoggerFactory.getLogger("Rogler TeamWars");
    private RoglerTeamWarsLogger() {}
    public static void info(String text) { LOGGER.info("{}", text); }
    public static void warn(String text) { LOGGER.warn("{}", text); }
}
