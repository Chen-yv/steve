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

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationEvent;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.InteractiveAuthenticationSuccessEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Audit logging for web sign-in attempts. Spring Security only logs failures at
 * DEBUG (invisible at root INFO) and its loggers are filtered out as framework
 * noise, so authentication activity is recorded here explicitly under a
 * SteVe logger that the default log view shows.
 *
 * @author SteVe Community Team
 */
@Slf4j
@Component
public class AuthEventLogger {

    @EventListener
    public void onAuthenticationEvent(AbstractAuthenticationEvent event) {
        try {
            String user = event.getAuthentication() == null
                ? "-" : String.valueOf(event.getAuthentication().getName());
            String ip = currentIp();

            if (event instanceof AbstractAuthenticationFailureEvent failure) {
                log.warn("Sign-in FAILED for user '{}' from {}: {}",
                    user, ip, failure.getException().getMessage());
            } else if (event instanceof InteractiveAuthenticationSuccessEvent) {
                // form-login success; the provider-level AuthenticationSuccessEvent is
                // skipped to avoid double logging the same sign-in
                log.info("Sign-in success for user '{}' from {}", user, ip);
            }
        } catch (Exception e) {
            log.error("Couldn't log authentication event", e);
        }
    }

    /**
     * The app sits behind an Apache proxy in production, so prefer the forwarded
     * client address when present.
     */
    private static String currentIp() {
        var attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes servletAttrs) {
            HttpServletRequest request = servletAttrs.getRequest();
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isEmpty()) {
                return forwarded.split(",")[0].trim();
            }
            return request.getRemoteAddr();
        }
        return "-";
    }
}
