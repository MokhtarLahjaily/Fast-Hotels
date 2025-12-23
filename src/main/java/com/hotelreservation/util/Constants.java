package com.hotelreservation.util;

/**
 * Project-wide constants to eliminate duplication and ensure consistency.
 */
public final class Constants {

    private Constants() {
        // Private constructor to prevent instantiation
    }

    // --- Message Keys (Attribute Names) ---
    public static final String ATTR_ERROR = "error";
    public static final String ATTR_ERROR_MSG = "errorMessage";
    public static final String ATTR_SUCCESS_MSG = "successMessage";
    public static final String ATTR_PAGE_TITLE = "pageTitle";

    // --- Common Error Messages ---
    public static final String MSG_ERR_USER_NOT_FOUND = "User not found";
    public static final String MSG_ERR_HOTEL_NOT_FOUND = "Hotel not found";
    public static final String MSG_ERR_BOOKING_NOT_FOUND = "Booking not found";
    public static final String MSG_ERR_ROOM_NOT_FOUND = "Room not found";
    public static final String MSG_ERR_REVIEW_NOT_FOUND = "Review not found";
    public static final String MSG_ERR_GENERIC = "An error occurred: ";
    public static final String MSG_ERR_UNAUTHORIZED = "You are not authorized to perform this action";

    // --- Common Success Messages ---
    public static final String MSG_SUCCESS_GENERIC = "Operation completed successfully";
    public static final String MSG_PROFILE_UPDATED = "Profile updated successfully";

    // --- View Names ---
    public static final String VIEW_ADMIN_DASHBOARD = "admin/dashboard";
    public static final String VIEW_ADMIN_HOTELS = "admin/hotels";
    public static final String VIEW_ADMIN_ROOMS = "admin/rooms";
    public static final String VIEW_ADMIN_USERS = "admin/users";
    public static final String VIEW_ADMIN_BOOKINGS = "admin/bookings";
    public static final String VIEW_ADMIN_REVIEWS = "admin/reviews";
    public static final String VIEW_ADMIN_SYSTEM = "admin/system";

    public static final String VIEW_OWNER_DASHBOARD = "hotel-owner/dashboard";
    public static final String VIEW_OWNER_HOTELS = "hotel-owner/hotels";
    public static final String VIEW_OWNER_HOTEL_FORM = "hotel-owner/hotel-form";
    public static final String VIEW_OWNER_HOTEL_DETAIL = "hotel-owner/hotel-detail";
    public static final String VIEW_OWNER_HOTEL_EDIT = "hotel-owner/hotel-edit";
    public static final String VIEW_OWNER_ROOM_DETAIL = "hotel-owner/room-detail";

    public static final String VIEW_PROFILE = "profile/index";
    public static final String VIEW_AUTH_LOGIN = "auth/login";
    public static final String VIEW_AUTH_REGISTER = "auth/register";
    public static final String VIEW_ERROR_403 = "error/403";
    public static final String VIEW_ERROR_404 = "error/404";
    public static final String VIEW_ERROR_500 = "error/500";

    // --- Redirects ---
    public static final String REDIRECT_PREFIX = "redirect:";

    public static final String REDIRECT_HOME = REDIRECT_PREFIX + "/";
    public static final String REDIRECT_LOGIN = REDIRECT_PREFIX + "/login";
    public static final String REDIRECT_REGISTER = REDIRECT_PREFIX + "/register";
    public static final String REDIRECT_PROFILE = REDIRECT_PREFIX + "/profile";
    public static final String REDIRECT_BOOKINGS = REDIRECT_PREFIX + "/bookings";

    public static final String REDIRECT_ADMIN_DASHBOARD = REDIRECT_PREFIX + "/admin/dashboard";
    public static final String REDIRECT_ADMIN_HOTELS = REDIRECT_PREFIX + "/admin/hotels";
    public static final String REDIRECT_ADMIN_BOOKINGS = REDIRECT_PREFIX + "/admin/bookings";
    public static final String REDIRECT_ADMIN_USERS = REDIRECT_PREFIX + "/admin/users";
    public static final String REDIRECT_REVIEWS = REDIRECT_PREFIX + "/reviews";

    public static final String REDIRECT_OWNER_DASHBOARD = REDIRECT_PREFIX + "/hotel-owner/dashboard";
    public static final String REDIRECT_OWNER_HOTELS = REDIRECT_PREFIX + "/hotel-owner/hotels";

    // --- Misc ---
    public static final String DEFAULT_NA = "N/A";
}
