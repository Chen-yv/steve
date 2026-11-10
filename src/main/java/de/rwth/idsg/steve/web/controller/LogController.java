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
package de.rwth.idsg.steve.web.controller;

import de.rwth.idsg.steve.service.ChargePointService;
import de.rwth.idsg.steve.utils.LogFileRetriever;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Single log page: live stream with minimal controls. Framework startup logs
 * (Spring/Jetty) are hidden by default; operators care about station activity.
 *
 * @author Sevket Goekay <sevketgokay@gmail.com>
 * @since 15.08.2014
 */
@RequiredArgsConstructor
@Slf4j
@Controller
@RequestMapping(value = "/manager")
public class LogController {

    private final ChargePointService chargePointService;

    @GetMapping("/log")
    public String log(Model model) {
        model.addAttribute("logFilePath", LogFileRetriever.INSTANCE.getLogFilePathOrErrorMessage());
        model.addAttribute("logAvailable", LogFileRetriever.INSTANCE.getPath().isPresent());
        return "log";
    }

    private static final Pattern ROLLED_FILE = Pattern.compile("steve-(\\d{4}-\\d{2}-\\d{2})\\.log\\.gz");

    /**
     * Dates of the rolled log files available for viewing, most recent first.
     * Today's active file is not in the list; the log page treats it as "today".
     */
    @GetMapping(value = "/log/dates", produces = "application/json")
    @ResponseBody
    public List<String> logDates() {
        return LogFileRetriever.INSTANCE.getPath()
            .map(active -> {
                Path dir = active.getParent();
                try (Stream<Path> files = Files.list(dir)) {
                    return files.map(f -> ROLLED_FILE.matcher(f.getFileName().toString()))
                                .filter(m -> m.matches() && m.groupCount() >= 1)
                                .map(m -> m.group(1))
                                .distinct()
                                .sorted(Comparator.reverseOrder())
                                .toList();
                } catch (IOException e) {
                    log.error("Couldn't list rolled log files in {}", dir, e);
                    return List.<String>of();
                }
            })
            .orElse(List.of());
    }

    /** Legacy live URL redirects to the single log page. */
    @GetMapping("/log/live")
    public String logLive() {
        return "redirect:/manager/log";
    }

    /**
     * Full station list for the log page's station filter. The list discovered from
     * log entries only covers what was logged recently; this returns every registered
     * station so the dropdown is complete regardless of log turnover.
     */
    @GetMapping(value = "/log/stations", produces = "application/json")
    @ResponseBody
    public List<String> stations() {
        return chargePointService.getChargeBoxIds();
    }

    public String getLogFilePath() {
        return LogFileRetriever.INSTANCE.getLogFilePathOrErrorMessage();
    }
}
