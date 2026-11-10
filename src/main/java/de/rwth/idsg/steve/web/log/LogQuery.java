/*
 * SteVe - SteckdosenVerwaltung - https://github.com/steve-community/steve
 * Copyright (C) 2013-2026 SteVe Community Team
 * All Rights Reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package de.rwth.idsg.steve.web.log;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Shared filter for log queries and live streams. Records are matched as a whole:
 * keyword may hit the header or any stack trace line.
 *
 * @author SteVe Community Team
 */
public class LogQuery {

    private static final List<String> LEVEL_ORDER = List.of("TRACE", "DEBUG", "INFO", "WARN", "ERROR");
    private static final String OCPP_LOGGER_PREFIX = "de.rwth.idsg.steve.ocpp";
    private static final String AUDIT_LOGGER = "de.rwth.idsg.steve.web.log.AuthEventLogger";
    private static final int MAX_TAIL = 100_000;

    /**
     * Loggers hidden from the default view: framework/JVM internals and web
     * sign-in audit lines. OCPP traffic is always visible.
     */
    private static final Set<String> HIDDEN_LOGGER_PREFIXES = Set.of(
        "org.springframework.", "org.eclipse.jetty.", "com.zaxxer.hikari.",
        "org.hibernate.", "org.apache.cxf.", "org.jooq.", "org.springdoc.",
        "org.apache.jasper.", "org.apache.tomcat.",
        "de.rwth.idsg.steve.SteveApplication",
        "de.rwth.idsg.steve.SteveApplicationStartupListener",
        "de.rwth.idsg.steve.config.",
        "de.rwth.idsg.steve.ocpp.ws.ocpp12.Ocpp12WebSocketEndpoint",
        "de.rwth.idsg.steve.ocpp.ws.ocpp15.Ocpp15WebSocketEndpoint",
        "de.rwth.idsg.steve.ocpp.ws.ocpp16.Ocpp16WebSocketEndpoint",
        "de.rwth.idsg.steve.web.log.AuthEventLogger"
    );


    /** Selected levels; empty set means no level filter (show everything). */
    private final Set<String> levels;
    private final String contains;
    private final String containsLower;
    private final int tail;

    /**
     * @param levelsParam comma-separated levels to show (e.g. "INFO,WARN"), or ALL/empty for no filter
     */
    public LogQuery(String levelsParam, String contains, int tail) {
        Set<String> parsed = new HashSet<>();
        if (levelsParam != null && !"ALL".equalsIgnoreCase(levelsParam.trim())) {
            for (String s : levelsParam.split(",")) {
                String t = s.trim().toUpperCase(Locale.ROOT);
                if (LEVEL_ORDER.contains(t)) {
                    parsed.add(t);
                }
            }
        }
        this.levels = parsed;
        this.contains = contains == null ? "" : contains.trim();
        this.containsLower = this.contains.toLowerCase(Locale.ROOT);
        this.tail = Math.min(Math.max(tail, 0), MAX_TAIL);
    }

    /**
     * Full match for a record. Level selection is a set: only the chosen levels are shown.
     * WARN/ERROR are always visible whatever their source; non-OCPP INFO is hidden
     * as framework noise.
     */
    public boolean matches(LogRecord r) {
        if (!matchesLevel(r.level()) || !loggerVisible(r.logger(), r.level())) {
            return false;
        }
        return !hasKeyword() || r.contains(containsLower);
    }

    /**
     * Live-stream variant: same rules on the structured event fields.
     */
    public boolean matchesLive(String logger, String level, String message) {
        if (!matchesLevel(level) || !loggerVisible(logger, level)) {
            return false;
        }
        return !hasKeyword() || message.toLowerCase(Locale.ROOT).contains(containsLower);
    }

    /**
     * WARN/ERROR are always visible whatever their source; INFO is visible only
     * for OCPP loggers.
     */
    public boolean loggerVisible(String logger, String level) {
        // sign-in audit lines never appear in the OCPP view (kept in the file only)
        if (logger != null && logger.startsWith(AUDIT_LOGGER)) {
            return false;
        }
        if ("WARN".equals(level) || "ERROR".equals(level)) {
            return true;
        }
        if (logger == null || logger.startsWith(OCPP_LOGGER_PREFIX)) {
            return true;
        }
        for (String prefix : HIDDEN_LOGGER_PREFIXES) {
            if (logger.startsWith(prefix)) {
                return false;
            }
        }
        return true;
    }

    public boolean matchesLevel(String lineLevel) {
        return levels.isEmpty() || lineLevel == null || levels.contains(lineLevel);
    }

    public boolean hasKeyword() {
        return !contains.isEmpty();
    }

    public boolean containsKeyword(String s) {
        return s.toLowerCase(Locale.ROOT).contains(containsLower);
    }

    public String getContains() {
        return contains;
    }

    public int getTail() {
        return tail;
    }
}
