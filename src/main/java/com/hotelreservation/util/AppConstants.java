package com.hotelreservation.util;

public class AppConstants {

    private AppConstants() {
        // Prevent instantiation
    }

    public static class Routes {
        public static final String API_AUTH = "/api/auth";
        public static final String API_ADMIN = "/api/admin";
        public static final String API_BOOKINGS = "/api/bookings";
        public static final String API_HOTELS = "/api/hotels";
        public static final String API_ROOMS = "/api/rooms";
        public static final String API_REVIEWS = "/api/reviews";
        public static final String API_NO_AUTH = "/no-auth";

        public static final String LOGIN = "/login";
        public static final String REGISTER = "/register";
        public static final String ERROR = "/error";
        public static final String SEARCH = "/search";

        // Static resources
        public static final String CSS = "/css/";
        public static final String JS = "/js/";
        public static final String IMAGES = "/images/";
        public static final String FONTS = "/fonts/";
        public static final String WEBJARS = "/webjars/";
        public static final String STATIC = "/static/";
        public static final String FAVICON = "/favicon.ico";
    }

    public static class Roles {
        public static final String ADMIN = "ADMIN";
        public static final String CUSTOMER = "CUSTOMER";
        public static final String HOTEL_OWNER = "HOTEL_OWNER";
    }

    public static class DefaultUsers {
        public static final String ADMIN_EMAIL = "admin@example.com";
        public static final String ADMIN_PASSWORD = "admin"; // In production, never hardcode this
        public static final String OWNER_EMAIL_PREFIX = "owner";
        public static final String CUSTOMER_EMAIL_PREFIX = "user";
        public static final String EMAIL_DOMAIN = "@example.com";
    }

    public static class Attributes {
        public static final String USER_ID = "userId";
        public static final String EMAIL = "email";
        public static final String ROLE = "role";
        public static final String JWT_COOKIE_NAME = "jwt_token";
        public static final String ANONYMOUS_USER = "anonymousUser";
    }

    public static class Messages {
        public static final String USER_NOT_FOUND = "User not found";
        public static final String UNAUTHORIZED = "Unauthorized access";
        public static final String HOTEL_NOT_FOUND = "Hotel not found";
        public static final String ROOM_NOT_FOUND = "Room not found";
        public static final String BOOKING_NOT_FOUND = "Booking not found";
    }
}
