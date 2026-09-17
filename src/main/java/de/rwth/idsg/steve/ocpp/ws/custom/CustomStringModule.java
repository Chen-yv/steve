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
package de.rwth.idsg.steve.ocpp.ws.custom;

import org.owasp.encoder.Encode;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.Version;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.deser.jdk.StringDeserializer;
import tools.jackson.databind.module.SimpleModule;

/**
 * @author Sevket Goekay <sevketgokay@gmail.com>
 * @since 17.08.2022
 */
public class CustomStringModule extends SimpleModule {

    public CustomStringModule() {
        super("CustomStringModule", new Version(0, 0, 1, null, "de.rwth.idsg", "steve"));

        // Only escape incoming strings as XSS defense-in-depth. Outgoing strings must be
        // sent as-is, because escaping corrupts protocol payloads (e.g. JSON in DataTransfer).
        super.addDeserializer(String.class, new CustomStringDeserializer());
    }

    private static class CustomStringDeserializer extends StringDeserializer {

        @Override
        public String deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
            String val = super.deserialize(p, ctxt);
            return Encode.forHtml(val);
        }
    }
}
