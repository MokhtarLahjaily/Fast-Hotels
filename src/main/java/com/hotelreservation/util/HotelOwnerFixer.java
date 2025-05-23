package com.hotelreservation.util;

import com.hotelreservation.model.Hotel;
import com.hotelreservation.model.User;
import com.hotelreservation.repository.HotelRepository;
import com.hotelreservation.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Profile("fix-owners")
public class HotelOwnerFixer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(HotelOwnerFixer.class);

    private final HotelRepository hotelRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void run(String... args) {
        logger.info("Running hotel owner fixer...");

        // Find luxury owner
        User luxuryOwner = userRepository.findByEmail("luxury.owner@example.com")
                .orElse(null);

        if (luxuryOwner == null) {
            logger.error("Luxury owner not found!");
            return;
        }

        // Find Grand Luxury Hotel
        Optional<Hotel> luxuryHotelOpt = hotelRepository.findAll().stream()
                .filter(h -> h.getName().contains("Grand Luxury Hotel") || h.getName().contains("Luxury"))
                .findFirst();

        if (luxuryHotelOpt.isPresent()) {
            Hotel luxuryHotel = luxuryHotelOpt.get();
            luxuryHotel.setOwner(luxuryOwner);
            hotelRepository.save(luxuryHotel);
            logger.info("Successfully assigned luxury owner to Grand Luxury Hotel");
        } else {
            logger.error("Grand Luxury Hotel not found!");
        }

        logger.info("Hotel owner fixing completed");
    }
}
