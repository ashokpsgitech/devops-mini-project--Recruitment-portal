package com.example.recruitment.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.http.HttpServletRequest;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ModelAndView handleNotFound(IllegalArgumentException ex, HttpServletRequest req) {
        log.warn("Not found: {} at {}", ex.getMessage(), req.getRequestURI());
        return errorView("Not Found", ex.getMessage(), HttpStatus.NOT_FOUND.value());
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ModelAndView handleConflict(IllegalStateException ex, HttpServletRequest req) {
        log.warn("Conflict: {} at {}", ex.getMessage(), req.getRequestURI());
        return errorView("Conflict", ex.getMessage(), HttpStatus.CONFLICT.value());
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ModelAndView handleAccessDenied(AccessDeniedException ex) {
        return errorView("Access Denied", "You do not have permission to perform this action.",
                HttpStatus.FORBIDDEN.value());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ModelAndView handleGeneric(Exception ex, HttpServletRequest req) {
        log.error("Unexpected error at {}", req.getRequestURI(), ex);
        return errorView("Internal Server Error",
                "An unexpected error occurred. Please try again later.",
                HttpStatus.INTERNAL_SERVER_ERROR.value());
    }

    private ModelAndView errorView(String title, String message, int status) {
        ModelAndView mv = new ModelAndView("error");
        mv.addObject("errorTitle", title);
        mv.addObject("errorMessage", message);
        mv.addObject("status", status);
        return mv;
    }
}
