package com.hotelreservation.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

@ControllerAdvice(basePackages = "com.hotelreservation.controller.view")
@Slf4j
public class GlobalViewExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ModelAndView handleResourceNotFoundException(ResourceNotFoundException ex) {
        log.error("Resource not found: {}", ex.getMessage());
        ModelAndView mav = new ModelAndView("error/404");
        mav.addObject("status", HttpStatus.NOT_FOUND.value());
        mav.addObject("message", ex.getMessage());
        return mav;
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ModelAndView handleUnauthorizedException(UnauthorizedException ex) {
        log.error("Unauthorized access: {}", ex.getMessage());
        ModelAndView mav = new ModelAndView("error/403");
        mav.addObject("status", HttpStatus.FORBIDDEN.value());
        mav.addObject("message", ex.getMessage());
        return mav;
    }

    @ExceptionHandler(Exception.class)
    public ModelAndView handleGlobalException(Exception ex) {
        log.error("Unhandled exception occurred in view controller", ex);
        ModelAndView mav = new ModelAndView("error/general");
        mav.addObject("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        mav.addObject("message", "An unexpected error occurred. Please try again later.");
        mav.addObject("exception", ex.getMessage());
        return mav;
    }
}
