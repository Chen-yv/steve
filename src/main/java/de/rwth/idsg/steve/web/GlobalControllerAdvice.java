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
package de.rwth.idsg.steve.web;

import de.rwth.idsg.steve.config.SteveProperties;
import de.rwth.idsg.steve.repository.dto.ChargePointSelect;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Sevket Goekay <sevketgokay@gmail.com>
 * @since 15.08.2014
 */
@ControllerAdvice(basePackages = "de.rwth.idsg.steve.web.controller")
@Slf4j
@RequiredArgsConstructor
public class GlobalControllerAdvice {

    private final SteveProperties steveProperties;

    @InitBinder
    public void binder(WebDataBinder binder) {
        BatchInsertConverter batchInsertConverter = new BatchInsertConverter();

        binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));
        binder.registerCustomEditor(LocalDate.class, new LocalDateEditor());
        binder.registerCustomEditor(DateTime.class, DateTimeEditor.forMvc());
        binder.registerCustomEditor(ChargePointSelect.class, new ChargePointSelectEditor());

        binder.registerCustomEditor(List.class, "idList", batchInsertConverter);
        binder.registerCustomEditor(List.class, "mailSettings.recipients", batchInsertConverter);
    }

    @ExceptionHandler(Exception.class)
    public ModelAndView handleError(HttpServletRequest req, Exception exception) {
        log.error("Request: {} raised following exception.", req.getRequestURL(), exception);

        ModelAndView mav = new ModelAndView();
        mav.addObject("exception", exception);
        mav.setViewName("00-error");
        return mav;
    }

    /**
     * Handles binding/validation errors (e.g., when a non-numeric value like a MAC address
     * "00:00:01:00:00:01" is entered for an Integer field such as userId). Shows a clear,
     * user-friendly error message instead of the raw exception.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ModelAndView handleMethodArgumentNotValid(HttpServletRequest req, MethodArgumentNotValidException exception) {
        log.warn("Request: {} raised a validation exception.", req.getRequestURL(), exception);

        List<FieldError> fieldErrors = exception.getBindingResult().getFieldErrors();
        String message = fieldErrors.stream()
            .map(fe -> "Invalid value for '" + fe.getField() + "': " + fe.getRejectedValue())
            .collect(Collectors.joining("; "));

        ModelAndView mav = new ModelAndView();
        mav.addObject("exception", new RuntimeException(message.isEmpty() ? "Invalid input" : message));
        mav.setViewName("00-error");
        return mav;
    }

    @ModelAttribute("ocpp12Enabled")
    public boolean isOcpp12JsonEnabled() {
        return steveProperties.getOcpp().getEnabledProtocols().getV12().isEnabled();
    }

    @ModelAttribute("ocpp15Enabled")
    public boolean isOcpp15JsonEnabled() {
        return steveProperties.getOcpp().getEnabledProtocols().getV15().isEnabled();
    }

    @ModelAttribute("ocpp16Enabled")
    public boolean isOcpp16JsonEnabled() {
        return steveProperties.getOcpp().getEnabledProtocols().getV16().isEnabled();
    }
}
