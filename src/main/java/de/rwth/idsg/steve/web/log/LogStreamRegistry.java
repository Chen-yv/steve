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

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxy;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.owasp.encoder.Encode;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Registry of live-log websocket subscribers. Each session carries its own filter set
 * which can be updated at runtime by the client. Log events are fanned out per subscriber.
 *
 * @author SteVe Community Team
 */
@Slf4j
public enum LogStreamRegistry {
    INSTANCE;

    private static final DateTimeFormatter TS_FORMAT = DateTimeFormatter
        .ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
        .withZone(ZoneId.systemDefault());
    private static final int MAX_STACK_LINES = 60;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final Set<Subscriber> subscribers = new CopyOnWriteArraySet<>();

    public void add(WebSocketSession session) {
        subscribers.add(new Subscriber(session));
    }

    public void remove(WebSocketSession session) {
        subscribers.removeIf(s -> s.session.getId().equals(session.getId()));
    }

    public void updateFilters(WebSocketSession session, LogQuery filters) {
        for (Subscriber s : subscribers) {
            if (s.session.getId().equals(session.getId())) {
                s.filters = filters;
                return;
            }
        }
    }

    public void broadcast(ILoggingEvent event) {
        if (subscribers.isEmpty()) {
            return;
        }
        String message = buildMessage(event);
        String logger = event.getLoggerName() == null ? "" : event.getLoggerName();
        for (Subscriber s : subscribers) {
            if (!s.matches(logger, event.getLevel().toString(), message)) {
                continue;
            }
            s.send(toJson(event, message));
        }
    }

    // -------------------------------------------------------------------------
    // Subscriber
    // -------------------------------------------------------------------------

    private static final class Subscriber {
        private final WebSocketSession session;
        private volatile LogQuery filters = new LogQuery("ALL", "", 0);

        private Subscriber(WebSocketSession session) {
            this.session = session;
        }

        private boolean matches(String logger, String level, String message) {
            LogQuery f = filters;
            return f.matchesLive(logger, level, message);
        }

        private void send(String json) {
            try {
                session.sendMessage(new TextMessage(json));
            } catch (Exception e) {
                // broken or too-slow subscriber (the session decorator enforces send time /
                // buffer limits): drop it instead of blocking the threads that write logs
                LogStreamRegistry.INSTANCE.remove(session);
                try {
                    session.close();
                } catch (Exception ignore) {
                    // already closing
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Message building
    // -------------------------------------------------------------------------

    private static String buildMessage(ILoggingEvent event) {
        StringBuilder sb = new StringBuilder(event.getFormattedMessage());
        if (event.getThrowableProxy() instanceof ThrowableProxy tp) {
            var st = tp.getStackTraceElementProxyArray();
            int limit = Math.min(st.length, MAX_STACK_LINES);
            for (int i = 0; i < limit; i++) {
                sb.append('\n').append(st[i].getSTEAsString());
            }
            if (st.length > limit) {
                sb.append("\n... ").append(st.length - limit).append(" more lines");
            }
        }
        return sb.toString();
    }

    private static String toJson(ILoggingEvent event, String message) {
        ObjectNode n = MAPPER.createObjectNode();
        n.put("type", "entry");
        n.put("ts", TS_FORMAT.format(Instant.ofEpochMilli(event.getTimeStamp())));
        n.put("level", event.getLevel().toString());
        n.put("logger", shortenLogger(event.getLoggerName()));
        n.put("message", Encode.forHtml(message));
        return n.toString();
    }

    private static String shortenLogger(String logger) {
        if (logger == null) {
            return "";
        }
        int i = logger.lastIndexOf('.');
        return i < 0 ? logger : logger.substring(i + 1);
    }

    // -------------------------------------------------------------------------
    // JSON protocol helpers for the websocket handler
    // -------------------------------------------------------------------------

    public static String initialSnapshot(List<LogRecord> recent, String date) {
        ObjectNode n = MAPPER.createObjectNode();
        n.put("type", "snapshot");
        n.put("date", date == null ? "today" : date);
        var arr = n.putArray("entries");
        for (LogRecord r : recent) {
            var e = arr.addObject();
            e.put("ts", r.timestamp());
            e.put("level", r.level());
            e.put("logger", shortenLogger(r.logger()));
            StringBuilder msg = new StringBuilder(r.header());
            for (String line : r.continuation()) {
                msg.append('\n').append(line);
            }
            e.put("message", Encode.forHtml(msg.toString()));
        }
        return n.toString();
    }

    public static String controlAck(String what, boolean state) {
        ObjectNode n = MAPPER.createObjectNode();
        n.put("type", "ack");
        n.put("what", what);
        n.put("state", state);
        return n.toString();
    }
}
