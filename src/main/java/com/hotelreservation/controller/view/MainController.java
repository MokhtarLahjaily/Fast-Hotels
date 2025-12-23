package com.hotelreservation.controller.view;

import com.hotelreservation.util.AppConstants;
import com.hotelreservation.util.LogSanitizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class MainController {
    private static final Logger logger = LoggerFactory.getLogger(MainController.class);

    @GetMapping("/about")
    public String about(Model model) {
        logger.info("Displaying about page");
        return "about";
    }

    @GetMapping("/contact")
    public String contact(Model model) {
        logger.info("Displaying contact page");
        return "contact";
    }

    @PostMapping("/contact")
    public String handleContactForm(
            @RequestParam("name") String name,
            @RequestParam("email") String email,
            @RequestParam("subject") String subject,
            @RequestParam("message") String message,
            RedirectAttributes redirectAttributes) {

        logger.info("Contact form submission received from: {}, email: {}, subject: {}",
                LogSanitizer.sanitize(name), LogSanitizer.sanitize(email), LogSanitizer.sanitize(subject));

        // In a real application, you would process the form data here
        // For example, send an email or save to database

        // Add a success message
        redirectAttributes.addFlashAttribute(AppConstants.Attributes.SUCCESS_MESSAGE,
                "Thank you for your message! We'll get back to you soon.");

        return "redirect:/contact";
    }

    // Support pages
    @GetMapping("/support/help")
    public String helpCenter(Model model) {
        logger.info("Displaying help center page");
        return "support/help";
    }

    @GetMapping("/faq")
    public String faq(Model model) {
        logger.info("Displaying FAQ page");
        return "support/faq";
    }

    @GetMapping("/privacy-policy")
    public String privacyPolicy(Model model) {
        logger.info("Displaying privacy policy page");
        return "support/privacy-policy";
    }

    @GetMapping("/terms-of-service")
    public String termsOfService(Model model) {
        logger.info("Displaying terms of service page");
        return "support/terms-of-service";
    }
}
