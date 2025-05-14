package com.hotelreservation.controller.view;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@Slf4j
public class CustomErrorController implements ErrorController {

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Object message = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
        Object exception = request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);

        // Log all request attributes for debugging
        request.getAttributeNames().asIterator().forEachRemaining(name ->
                log.debug("Request attribute: {} = {}", name, request.getAttribute(name))
        );

        if (status != null) {
            int statusCode = Integer.parseInt(status.toString());

            log.error("Error occurred: Status={}, Message={}, Exception={}",
                    status, message, exception);

            model.addAttribute("status", status);
            model.addAttribute("message", message != null ? message : "No error message available");
            model.addAttribute("exception", exception != null ? exception.toString() : "No exception details available");

            if (statusCode == HttpStatus.NOT_FOUND.value()) {
                return "error/404";
            } else if (statusCode == HttpStatus.FORBIDDEN.value()) {
                return "error/403";
            } else if (statusCode == HttpStatus.INTERNAL_SERVER_ERROR.value()) {
                return "error/500";
            }
        }

        return "error/general";
    }
}
