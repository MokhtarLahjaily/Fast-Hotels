package com.hotelreservation.controller;

import org.springframework.context.ApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * Controller dedicated to serving the favicon.ico file
 * This provides better control over caching and error handling
 */
@Controller
public class FaviconController {

    private static final Logger logger = LoggerFactory.getLogger(FaviconController.class);

    private final ApplicationContext applicationContext;

    public FaviconController(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    /**
     * Serves the favicon.ico file with appropriate caching headers
     * 
     * @return ResponseEntity containing the favicon resource
     */
    @GetMapping("/favicon.ico")
    @ResponseBody
    public ResponseEntity<Resource> favicon() {
        logger.debug("Serving favicon.ico");

        try {
            // Try to get the favicon from the static directory
            Resource resource = new ClassPathResource("static/favicon.ico");

            // If the resource doesn't exist, try the fallback location
            if (!resource.exists()) {
                logger.debug("Favicon not found in static directory, trying fallback location");
                resource = new ClassPathResource("static/images/favicon.ico");
            }

            // If still not found, use a default icon from classpath
            if (!resource.exists()) {
                logger.debug("Favicon not found in fallback location, using default");
                resource = new ClassPathResource("static/images/logo.png");
            }

            // Return the resource with caching headers
            return ResponseEntity.ok()
                    .contentType(MediaType.valueOf("image/x-icon"))
                    .cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS))
                    .body(resource);

        } catch (Exception e) {
            logger.error("Error serving favicon", e);
            // Return empty response with 404 status
            return ResponseEntity.notFound().build();
        }
    }
}
