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

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A single log record with its structured parts. A record spans one header line
 * and zero or more continuation lines (stack trace / multi-line message).
 *
 * @author SteVe Community Team
 */
public record LogRecord(String header, String level, String logger, String[] continuation) {

    // LOG_PATTERN is "[%5level] %d %c \(%thread\) - %msg%n"
    private static final Pattern HEADER_PATTERN = Pattern.compile("^\\[\\s*(TRACE|DEBUG|INFO|WARN|ERROR)\\s*\\]");
    private static final Pattern TS_PATTERN =
        Pattern.compile("^\\[\\s*\\w+\\s*\\]\\s+(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}),(\\d{3})");

    public static boolean isHeader(String line) {
        return HEADER_PATTERN.matcher(line).find();
    }

    public static LogRecord parse(String headerLine) {
        Matcher m = HEADER_PATTERN.matcher(headerLine);
        if (!m.find()) {
            return new LogRecord(headerLine, "INFO", "", new String[0]);
        }
        return new LogRecord(headerLine, m.group(1), loggerOf(headerLine), new String[0]);
    }

    /**
     * Timestamp parsed from the header, normalized to dot-milliseconds form
     * (live-stream entries use the same format).
     */
    public String timestamp() {
        Matcher m = TS_PATTERN.matcher(header);
        return m.find() ? m.group(1) + "." + m.group(2) : "";
    }

    public LogRecord withContinuation(String[] lines) {
        return new LogRecord(header, level, logger, lines);
    }

    public boolean hasStackTrace() {
        return continuation.length > 0;
    }

    public boolean contains(String keywordLower) {
        if (header.toLowerCase(Locale.ROOT).contains(keywordLower)) {
            return true;
        }
        for (String line : continuation) {
            if (line.toLowerCase(Locale.ROOT).contains(keywordLower)) {
                return true;
            }
        }
        return false;
    }

    private static String loggerOf(String headerLine) {
        int end = headerLine.indexOf(" (");
        if (end < 0) {
            return "";
        }
        String head = headerLine.substring(0, end);
        int start = head.lastIndexOf(' ');
        return start < 0 ? head : head.substring(start + 1);
    }
}
