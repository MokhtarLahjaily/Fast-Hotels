package com.hotelreservation.controller.view;

import com.hotelreservation.dto.response.HotelResponse;
import com.hotelreservation.service.HotelService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class HomeController {

    private static final Logger logger = LoggerFactory.getLogger(HomeController.class);

    private final HotelService hotelService;

    @Autowired
    public HomeController(HotelService hotelService) {
        this.hotelService = hotelService;
    }

    @GetMapping("/")
    public String home(Model model) {
        logger.info("Loading home page");

        try {
            // Get featured hotels
            List<HotelResponse> featuredHotels = hotelService.getFeaturedHotels();

            // Add to model
            model.addAttribute("featuredHotels", featuredHotels);

            logger.debug("Added {} featured hotels to model", featuredHotels.size());
            return "index";
        } catch (Exception e) {
            logger.error("Error loading home page", e);
            model.addAttribute("errorMessage", "An error occurred while loading featured hotels. Please try again later.");
            return "index";
        }
    }
}