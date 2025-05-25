package com.hotelreservation.config;

import com.hotelreservation.model.Amenity;
import com.hotelreservation.model.Hotel;
import com.hotelreservation.model.Image;
import com.hotelreservation.model.Room;
import com.hotelreservation.model.User;
import com.hotelreservation.model.UserRole;
import com.hotelreservation.repository.AmenityRepository;
import com.hotelreservation.repository.HotelRepository;
import com.hotelreservation.repository.ImageRepository;
import com.hotelreservation.repository.RoomRepository;
import com.hotelreservation.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    private final HotelRepository hotelRepository;
    private final UserRepository userRepository;
    private final RoomRepository roomRepository;
    private final AmenityRepository amenityRepository;
    private final ImageRepository imageRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        // Only seed data if the database is empty
        if (hotelRepository.count() > 0) {
            logger.info("Database already has data, checking for hotel owners...");
            createHotelOwnersForExistingHotels();
            return;
        }

        logger.info("Starting database initialization...");

        // Create admin user if it doesn't exist
        User adminUser = createAdminUser();

        // Create hotel owners
        List<User> hotelOwners = createHotelOwners();

        // Create amenities
        List<Amenity> amenities = createAmenities();

        // Create hotels with rooms and images
        createHotels(hotelOwners, amenities);

        logger.info("Database initialization completed successfully");
    }

    private User createAdminUser() {
        if (userRepository.findByEmail("admin@example.com").isPresent()) {
            logger.info("Admin user already exists");
            return userRepository.findByEmail("admin@example.com").get();
        }

        User adminUser = User.builder()
                .email("admin@example.com")
                .passwordHash(passwordEncoder.encode("admin123"))
                .firstName("Admin")
                .lastName("User")
                .role(UserRole.ADMIN)
                .build();

        adminUser = userRepository.save(adminUser);
        logger.info("Created admin user: {}", adminUser.getEmail());
        return adminUser;
    }

    private List<User> createHotelOwners() {
        // Create 5 hotel owners
        User luxuryOwner = createHotelOwner("luxury.owner@example.com", "Luxury", "Owner");
        User beachOwner = createHotelOwner("beach.owner@example.com", "Beach", "Owner");
        User boutiqueOwner = createHotelOwner("boutique.owner@example.com", "Boutique", "Owner");
        User businessOwner = createHotelOwner("business.owner@example.com", "Business", "Owner");
        User familyOwner = createHotelOwner("family.owner@example.com", "Family", "Owner");

        return Arrays.asList(luxuryOwner, beachOwner, boutiqueOwner, businessOwner, familyOwner);
    }

    private User createHotelOwner(String email, String firstName, String lastName) {
        if (userRepository.findByEmail(email).isPresent()) {
            logger.info("Hotel owner {} already exists", email);
            return userRepository.findByEmail(email).get();
        }

        User hotelOwner = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode("owner123"))
                .firstName(firstName)
                .lastName(lastName)
                .role(UserRole.HOTEL_OWNER)
                .build();

        hotelOwner = userRepository.save(hotelOwner);
        logger.info("Created hotel owner: {}", hotelOwner.getEmail());
        return hotelOwner;
    }

    private List<Amenity> createAmenities() {
        if (amenityRepository.count() > 0) {
            logger.info("Amenities already exist");
            return amenityRepository.findAll();
        }

        List<Amenity> amenities = Arrays.asList(
                Amenity.builder().name("Free WiFi").icon("wifi").build(),
                Amenity.builder().name("Swimming Pool").icon("pool").build(),
                Amenity.builder().name("Fitness Center").icon("fitness").build(),
                Amenity.builder().name("Spa").icon("spa").build(),
                Amenity.builder().name("Restaurant").icon("restaurant").build(),
                Amenity.builder().name("Bar").icon("bar").build(),
                Amenity.builder().name("Room Service").icon("room-service").build(),
                Amenity.builder().name("Parking").icon("parking").build(),
                Amenity.builder().name("Airport Shuttle").icon("shuttle").build(),
                Amenity.builder().name("Business Center").icon("business").build(),
                Amenity.builder().name("Laundry").icon("laundry").build(),
                Amenity.builder().name("Concierge").icon("concierge").build(),
                Amenity.builder().name("Pet Friendly").icon("pet").build(),
                Amenity.builder().name("Kids Club").icon("kids").build(),
                Amenity.builder().name("Beach Access").icon("beach").build()
        );

        amenities = amenityRepository.saveAll(amenities);
        logger.info("Created {} amenities", amenities.size());
        return amenities;
    }

    private void createHotels(List<User> hotelOwners, List<Amenity> allAmenities) {
        // Create 5 sample hotels

        // 1. Luxury Hotel
        Set<Amenity> luxuryAmenities = new HashSet<>(Arrays.asList(
                allAmenities.get(0), // WiFi
                allAmenities.get(1), // Pool
                allAmenities.get(2), // Fitness
                allAmenities.get(3), // Spa
                allAmenities.get(4), // Restaurant
                allAmenities.get(5), // Bar
                allAmenities.get(6), // Room Service
                allAmenities.get(11) // Concierge
        ));

        Hotel luxuryHotel = Hotel.builder()
                .name("Grand Luxury Hotel")
                .description("Experience the epitome of luxury in our 5-star hotel located in the heart of the city. Featuring elegant rooms, world-class dining, and exceptional service.")
                .address("123 Luxury Avenue")
                .city("Dubai")
                .country("UAE")
                .postalCode("10001")
                .latitude(new BigDecimal("40.7128"))
                .longitude(new BigDecimal("-74.0060"))
                .starRating((short) 5)
                .owner(hotelOwners.get(0)) // Assign luxury owner
                .amenities(luxuryAmenities)
                .build();

        luxuryHotel = hotelRepository.save(luxuryHotel);

        // Add rooms to luxury hotel
        createRooms(luxuryHotel);

        // Add images to luxury hotel
        createHotelImages(luxuryHotel, "luxury");

        // 2. Beach Resort
        Set<Amenity> beachAmenities = new HashSet<>(Arrays.asList(
                allAmenities.get(0), // WiFi
                allAmenities.get(1), // Pool
                allAmenities.get(3), // Spa
                allAmenities.get(4), // Restaurant
                allAmenities.get(5), // Bar
                allAmenities.get(14) // Beach Access
        ));

        Hotel beachResort = Hotel.builder()
                .name("Seaside Paradise Resort")
                .description("Escape to our beachfront paradise with stunning ocean views, private beach access, and tropical-inspired accommodations. Perfect for a relaxing getaway.")
                .address("456 Beachfront Drive")
                .city("Miami")
                .country("USA")
                .postalCode("33139")
                .latitude(new BigDecimal("25.7617"))
                .longitude(new BigDecimal("-80.1918"))
                .starRating((short) 4)
                .owner(hotelOwners.get(1)) // Assign beach owner
                .amenities(beachAmenities)
                .build();

        beachResort = hotelRepository.save(beachResort);

        // Add rooms to beach resort
        createRooms(beachResort);

        // Add images to beach resort
        createHotelImages(beachResort, "beach");

        // 3. Boutique Hotel
        Set<Amenity> boutiqueAmenities = new HashSet<>(Arrays.asList(
                allAmenities.get(0), // WiFi
                allAmenities.get(4), // Restaurant
                allAmenities.get(5), // Bar
                allAmenities.get(11) // Concierge
        ));

        Hotel boutiqueHotel = Hotel.builder()
                .name("Artisan Boutique Hotel")
                .description("A charming boutique hotel offering a unique blend of historic architecture and modern design. Each room is individually decorated with local artwork.")
                .address("789 Artistic Lane")
                .city("Paris")
                .country("France")
                .postalCode("75001")
                .latitude(new BigDecimal("48.8566"))
                .longitude(new BigDecimal("2.3522"))
                .starRating((short) 4)
                .owner(hotelOwners.get(2)) // Assign boutique owner
                .amenities(boutiqueAmenities)
                .build();

        boutiqueHotel = hotelRepository.save(boutiqueHotel);

        // Add rooms to boutique hotel
        createRooms(boutiqueHotel);

        // Add images to boutique hotel
        createHotelImages(boutiqueHotel, "boutique");

        // 4. Business Hotel
        Set<Amenity> businessAmenities = new HashSet<>(Arrays.asList(
                allAmenities.get(0), // WiFi
                allAmenities.get(2), // Fitness
                allAmenities.get(4), // Restaurant
                allAmenities.get(7), // Parking
                allAmenities.get(8), // Airport Shuttle
                allAmenities.get(9) // Business Center
        ));

        Hotel businessHotel = Hotel.builder()
                .name("Executive Business Hotel")
                .description("Designed for the modern business traveler, our hotel offers spacious work areas, high-speed internet, and convenient meeting facilities.")
                .address("101 Corporate Plaza")
                .city("London")
                .country("UK")
                .postalCode("EC1A 1BB")
                .latitude(new BigDecimal("51.5074"))
                .longitude(new BigDecimal("-0.1278"))
                .starRating((short) 4)
                .owner(hotelOwners.get(3)) // Assign business owner
                .amenities(businessAmenities)
                .build();

        businessHotel = hotelRepository.save(businessHotel);

        // Add rooms to business hotel
        createRooms(businessHotel);

        // Add images to business hotel
        createHotelImages(businessHotel, "business");

        // 5. Family Resort
        Set<Amenity> familyAmenities = new HashSet<>(Arrays.asList(
                allAmenities.get(0), // WiFi
                allAmenities.get(1), // Pool
                allAmenities.get(4), // Restaurant
                allAmenities.get(7), // Parking
                allAmenities.get(12), // Pet Friendly
                allAmenities.get(13) // Kids Club
        ));

        Hotel familyResort = Hotel.builder()
                .name("Family Fun Resort")
                .description("The perfect destination for family vacations with activities for all ages, spacious family rooms, and a dedicated kids' club.")
                .address("202 Family Circle")
                .city("Orlando")
                .country("USA")
                .postalCode("32801")
                .latitude(new BigDecimal("28.5383"))
                .longitude(new BigDecimal("-81.3792"))
                .starRating((short) 3)
                .owner(hotelOwners.get(4)) // Assign family owner
                .amenities(familyAmenities)
                .build();

        familyResort = hotelRepository.save(familyResort);

        // Add rooms to family resort
        createRooms(familyResort);

        // Add images to family resort
        createHotelImages(familyResort, "family");

        logger.info("Created 5 sample hotels with rooms and images");
    }

    @Transactional
    private void createHotelOwnersForExistingHotels() {
        // Create hotel owners if they don't exist
        List<User> hotelOwners = createHotelOwners();

        // Specifically assign Grand Luxury Hotel to luxury.owner@example.com
        User luxuryOwner = userRepository.findByEmail("luxury.owner@example.com")
                .orElseThrow(() -> new RuntimeException("Luxury owner not found"));

        // Find Grand Luxury Hotel
        Optional<Hotel> luxuryHotelOpt = hotelRepository.findAll().stream()
                .filter(h -> h.getName().contains("Grand Luxury Hotel") || h.getName().contains("Luxury"))
                .findFirst();

        if (luxuryHotelOpt.isPresent()) {
            Hotel luxuryHotel = luxuryHotelOpt.get();
            luxuryHotel.setOwner(luxuryOwner);
            hotelRepository.save(luxuryHotel);
            logger.info("Assigned luxury owner to Grand Luxury Hotel");
        } else {
            logger.warn("Grand Luxury Hotel not found");
        }

        // Find Seaside Paradise Resort
        Optional<Hotel> beachHotelOpt = hotelRepository.findAll().stream()
                .filter(h -> h.getName().contains("Seaside Paradise") || h.getName().contains("Beach"))
                .findFirst();

        if (beachHotelOpt.isPresent()) {
            Hotel beachHotel = beachHotelOpt.get();
            User beachOwner = userRepository.findByEmail("beach.owner@example.com")
                    .orElseThrow(() -> new RuntimeException("Beach owner not found"));
            beachHotel.setOwner(beachOwner);
            hotelRepository.save(beachHotel);
            logger.info("Assigned beach owner to Seaside Paradise Resort");
        }

        // Find Artisan Boutique Hotel
        Optional<Hotel> boutiqueHotelOpt = hotelRepository.findAll().stream()
                .filter(h -> h.getName().contains("Artisan Boutique") || h.getName().contains("Boutique"))
                .findFirst();

        if (boutiqueHotelOpt.isPresent()) {
            Hotel boutiqueHotel = boutiqueHotelOpt.get();
            User boutiqueOwner = userRepository.findByEmail("boutique.owner@example.com")
                    .orElseThrow(() -> new RuntimeException("Boutique owner not found"));
            boutiqueHotel.setOwner(boutiqueOwner);
            hotelRepository.save(boutiqueHotel);
            logger.info("Assigned boutique owner to Artisan Boutique Hotel");
        }

        // Find Executive Business Hotel
        Optional<Hotel> businessHotelOpt = hotelRepository.findAll().stream()
                .filter(h -> h.getName().contains("Executive Business") || h.getName().contains("Business"))
                .findFirst();

        if (businessHotelOpt.isPresent()) {
            Hotel businessHotel = businessHotelOpt.get();
            User businessOwner = userRepository.findByEmail("business.owner@example.com")
                    .orElseThrow(() -> new RuntimeException("Business owner not found"));
            businessHotel.setOwner(businessOwner);
            hotelRepository.save(businessHotel);
            logger.info("Assigned business owner to Executive Business Hotel");
        }

        // Find Family Fun Resort
        Optional<Hotel> familyHotelOpt = hotelRepository.findAll().stream()
                .filter(h -> h.getName().contains("Family Fun") || h.getName().contains("Family"))
                .findFirst();

        if (familyHotelOpt.isPresent()) {
            Hotel familyHotel = familyHotelOpt.get();
            User familyOwner = userRepository.findByEmail("family.owner@example.com")
                    .orElseThrow(() -> new RuntimeException("Family owner not found"));
            familyHotel.setOwner(familyOwner);
            hotelRepository.save(familyHotel);
            logger.info("Assigned family owner to Family Fun Resort");
        }

        // Assign remaining hotels to owners in a round-robin fashion
        int ownerIndex = 0;
        for (Hotel hotel : hotelRepository.findAll()) {
            if (hotel.getOwner() == null) {
                hotel.setOwner(hotelOwners.get(ownerIndex % hotelOwners.size()));
                hotelRepository.save(hotel);
                logger.info("Assigned owner {} to hotel {}",
                        hotelOwners.get(ownerIndex % hotelOwners.size()).getEmail(),
                        hotel.getName());
                ownerIndex++;
            }
        }

        logger.info("Finished assigning owners to existing hotels");
    }

    private void createRooms(Hotel hotel) {
        // Create standard room
        Room standardRoom = Room.builder()
                .hotel(hotel)
                .name("Standard Room")
                .description("Comfortable room with all the essential amenities for a pleasant stay.")
                .capacity(2)
                .pricePerNight(new BigDecimal("100.00"))
                .roomCount(10) // Added roomCount field
                .build();

        roomRepository.save(standardRoom);

        // Create deluxe room
        Room deluxeRoom = Room.builder()
                .hotel(hotel)
                .name("Deluxe Room")
                .description("Spacious room with premium amenities and a beautiful view.")
                .capacity(2)
                .pricePerNight(new BigDecimal("150.00"))
                .roomCount(8) // Added roomCount field
                .build();

        roomRepository.save(deluxeRoom);

        // Create suite
        Room suite = Room.builder()
                .hotel(hotel)
                .name("Executive Suite")
                .description("Luxurious suite with separate living area and exclusive amenities.")
                .capacity(3)
                .pricePerNight(new BigDecimal("250.00"))
                .roomCount(5) // Added roomCount field
                .build();

        roomRepository.save(suite);

        // Create family room
        Room familyRoom = Room.builder()
                .hotel(hotel)
                .name("Family Room")
                .description("Spacious room designed for families, with multiple beds and child-friendly features.")
                .capacity(4)
                .pricePerNight(new BigDecimal("200.00"))
                .roomCount(6) // Added roomCount field
                .build();

        roomRepository.save(familyRoom);
    }

    private void createHotelImages(Hotel hotel, String type) {
        // Create primary image
        Image primaryImage = Image.builder()
                .entityType("HOTEL")
                .entityId(hotel.getId())
                .url("/images/hotel-" + type + "-1.jpg")
                .isPrimary(true)
                .build();

        imageRepository.save(primaryImage);

        // Create additional images
        for (int i = 2; i <= 3; i++) {
            Image image = Image.builder()
                    .entityType("HOTEL")
                    .entityId(hotel.getId())
                    .url("/images/hotel-" + type + "-" + i + ".jpg")
                    .isPrimary(false)
                    .build();

            imageRepository.save(image);
        }
    }
}
