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

import tools.jackson.databind.ObjectMapper;
import de.rwth.idsg.steve.ocpp.ws.JsonObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Sevket Goekay <sevketgokay@gmail.com>
 * @since 17.08.2022
 */
public class CustomStringModuleTest {

    private final ObjectMapper mapper = JsonObjectMapper.INSTANCE.getMapper();

    @Test
    public void testNormalString() throws Exception {
        SimpleJsonModel input = new SimpleJsonModel("normal string");
        String output = mapper.writeValueAsString(input);
        Assertions.assertEquals("{\"someText\":\"normal string\"}", output);
    }

    @Test
    public void testLink() throws Exception {
        // outgoing strings must not be escaped, otherwise payloads like JSON in DataTransfer get corrupted
        SimpleJsonModel input = new SimpleJsonModel("<a href=\"link\">Some link</a>");
        String output = mapper.writeValueAsString(input);
        Assertions.assertEquals("{\"someText\":\"<a href=\\\"link\\\">Some link</a>\"}", output);
    }

    @Test
    public void testScript() throws Exception {
        SimpleJsonModel input = new SimpleJsonModel("<script src=\"http://someurl.com/script.js\"/>");
        String output = mapper.writeValueAsString(input);
        Assertions.assertEquals("{\"someText\":\"<script src=\\\"http://someurl.com/script.js\\\"/>\"}", output);
    }

    @Test
    public void testDeserializeEscapes() throws Exception {
        // incoming strings are still escaped as XSS defense-in-depth
        String input = "{\"someText\":\"<script src=\\\"http://someurl.com/script.js\\\"/>\"}";
        SimpleJsonModel output = mapper.readValue(input, SimpleJsonModel.class);
        Assertions.assertEquals("&lt;script src=&#34;http://someurl.com/script.js&#34;/&gt;", output.getSomeText());
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class SimpleJsonModel {
        private String someText;
    }
}
