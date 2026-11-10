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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.rwth.idsg.steve.utils.LogFileRetriever;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

/**
 * Websocket endpoint for the log viewer. The browser sends filter updates and date
 * selections as JSON; the server replies with a snapshot of the requested day and
 * streams matching live events. Session authentication is inherited from the
 * surrounding /manager/** security chain.
 *
 * @author Sevket Goekay <sevketgokay@gmail.com>
 * @since 15.08.2014
 */
@Slf4j
public class LogStreamWebSocketHandler extends TextWebSocketHandler {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Safety cap for a single snapshot; a rolled day is far below this in practice. */
    private static final int MAX_SNAPSHOT_RECORDS = 50_000;
    private static final Pattern DATE_PATTERN = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");

    /** A subscriber that cannot keep up must never block the threads writing application logs. */
    static final int SEND_TIME_LIMIT_MS = 2000;
    static final int SEND_BUFFER_LIMIT_BYTES = 256 * 1024;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws IOException {
        ConcurrentWebSocketSessionDecorator safe = new ConcurrentWebSocketSessionDecorator(
            session, SEND_TIME_LIMIT_MS, SEND_BUFFER_LIMIT_BYTES);
        LogStreamRegistry.INSTANCE.add(safe);
        try {
            sendSnapshot(safe, "today");
        } catch (Exception e) {
            // client vanished between connect and snapshot: drop it quietly instead of
            // spraying ERROR stacks from the decorator
            LogStreamRegistry.INSTANCE.remove(safe);
            try { session.close(); } catch (Exception ignore) { /* already closing */ }
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
        JsonNode node;
        try {
            node = MAPPER.readTree(message.getPayload());
        } catch (Exception e) {
            return;
        }
        String type = node.path("type").asText("");
        if ("ping".equals(type)) {
            // keepalive from the browser; reply so it knows the connection is healthy
            session.sendMessage(new TextMessage("{\"type\":\"pong\"}"));
            return;
        }
        if ("filters".equals(type)) {
            LogQuery q = new LogQuery(
                node.path("level").asText("ALL"),
                node.path("contains").asText(""),
                0
            );
            LogStreamRegistry.INSTANCE.updateFilters(session, q);
            return;
        }
        if ("load".equals(type)) {
            sendSnapshot(session, node.path("date").asText("today"));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        LogStreamRegistry.INSTANCE.remove(session);
    }

    private void sendSnapshot(WebSocketSession session, String date) throws IOException {
        Optional<Path> file = resolveDateFile(date);
        List<LogRecord> records = file.map(this::readFile).orElse(List.of());

        // hide framework internals and sign-in audit lines from snapshots
        LogQuery defaultFilter = new LogQuery("ALL", "", 0);
        List<LogRecord> filtered = records.stream().filter(defaultFilter::matches).toList();
        int from = Math.max(0, filtered.size() - MAX_SNAPSHOT_RECORDS);
        List<LogRecord> recent = filtered.subList(from, filtered.size());

        synchronized (session) {
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(LogStreamRegistry.initialSnapshot(recent, date)));
            }
        }
    }

    // (send failures inside snapshots surface as IOException; callers handle)

    /**
     * "today" resolves to the active log file; any other date to its rolled
     * steve-YYYY-MM-DD.log.gz (or uncompressed leftover) in the same directory.
     */
    private Optional<Path> resolveDateFile(String date) throws IOException {
        if (date == null || date.isBlank() || "today".equals(date)) {
            return LogFileRetriever.INSTANCE.getPath();
        }
        if (!DATE_PATTERN.matcher(date).matches()) {
            return Optional.empty();
        }
        Optional<Path> active = LogFileRetriever.INSTANCE.getPath();
        if (active.isEmpty()) {
            return Optional.empty();
        }
        Path dir = active.get().getParent();
        Path gz = dir.resolve("steve-" + date + ".log.gz");
        if (Files.exists(gz)) {
            return Optional.of(gz);
        }
        Path plain = dir.resolve("steve-" + date + ".log");
        if (Files.exists(plain)) {
            return Optional.of(plain);
        }
        return Optional.empty();
    }

    /**
     * Parse the whole file into records. Reuses the same record-grouping rules as the
     * log page: stack traces belong to their header line. Reads plain and gzipped files.
     */
    private List<LogRecord> readFile(Path file) {
        List<LogRecord> records = new ArrayList<>();
        try (Stream<String> lines = openLines(file)) {
            String header = null;
            List<String> cont = new ArrayList<>();
            var it = lines.iterator();
            while (it.hasNext()) {
                String line = it.next();
                if (LogRecord.isHeader(line)) {
                    if (header != null) {
                        records.add(LogRecord.parse(header).withContinuation(cont.toArray(String[]::new)));
                    }
                    header = line;
                    cont = new ArrayList<>();
                } else if (header != null) {
                    cont.add(line);
                }
            }
            if (header != null) {
                records.add(LogRecord.parse(header).withContinuation(cont.toArray(String[]::new)));
            }
        } catch (IOException e) {
            log.error("Couldn't read the log file [{}]", file, e);
        }
        return records;
    }

    private static Stream<String> openLines(Path file) throws IOException {
        InputStream in = Files.newInputStream(file);
        if (file.getFileName().toString().endsWith(".gz")) {
            in = new GZIPInputStream(in);
        }
        return new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)).lines();
    }
}
