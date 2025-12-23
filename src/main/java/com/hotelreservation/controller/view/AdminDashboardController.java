package com.hotelreservation.controller.view;

import com.hotelreservation.dto.response.HotelResponse;
import com.hotelreservation.dto.response.ReviewResponse;
import com.hotelreservation.dto.response.RoomResponse;
import com.hotelreservation.dto.response.UserResponse;
import com.hotelreservation.model.BookingStatus;
import com.hotelreservation.repository.BookingRepository;
import com.hotelreservation.repository.HotelRepository;
import com.hotelreservation.repository.ReviewRepository;
import com.hotelreservation.repository.UserRepository;
import com.hotelreservation.service.AdminService;
import com.hotelreservation.service.BookingService;
import com.hotelreservation.service.BookingStatusUpdateService;
import com.hotelreservation.service.HotelService;
import com.hotelreservation.util.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class AdminDashboardController {

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final HotelRepository hotelRepository;
    private final ReviewRepository reviewRepository;
    private final BookingService bookingService;
    private final BookingStatusUpdateService bookingStatusUpdateService;
    private final AdminService adminService;
    private final HotelService hotelService;

    @GetMapping({ "", "/", "/dashboard" })
    public String dashboard(Model model) {
        log.info("Accessing admin dashboard");
        try {
            // Get counts for dashboard
            long totalUsers = userRepository.count();
            long totalHotels = hotelRepository.count();
            long totalBookings = bookingRepository.count();
            long totalReviews = reviewRepository.count();

            // Get booking counts by status
            long pendingBookings = bookingService.countByStatus(BookingStatus.PENDING);
            long confirmedBookings = bookingService.countByStatus(BookingStatus.CONFIRMED);
            long completedBookings = bookingService.countByStatus(BookingStatus.COMPLETED);
            long cancelledBookings = bookingService.countByStatus(BookingStatus.CANCELLED);

            // Add to model
            model.addAttribute("totalUsers", totalUsers);
            model.addAttribute("totalHotels", totalHotels);
            model.addAttribute("totalBookings", totalBookings);
            model.addAttribute("totalReviews", totalReviews);

            model.addAttribute("pendingBookings", pendingBookings);
            model.addAttribute("confirmedBookings", confirmedBookings);
            model.addAttribute("completedBookings", completedBookings);
            model.addAttribute("cancelledBookings", cancelledBookings);

            // Add page title
            model.addAttribute(Constants.ATTR_PAGE_TITLE, "Admin Dashboard");

            return Constants.VIEW_ADMIN_DASHBOARD;
        } catch (Exception e) {
            log.error("Error loading admin dashboard: {}", e.getMessage(), e);
            model.addAttribute(Constants.ATTR_ERROR, "Error loading dashboard data: " + e.getMessage());
            return Constants.VIEW_ADMIN_DASHBOARD;
        }
    }

    @GetMapping("/hotels")
    public String hotels(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String status,
            Model model) {
        log.info("Accessing admin hotels page - page: {}, size: {}, search: {}", page, size, search);

        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
            Page<HotelResponse> hotelsPage = adminService.getAllHotelsForApproval(pageable, search, city, status);

            model.addAttribute("hotels", hotelsPage);
            model.addAttribute("search", search);
            model.addAttribute("city", city);
            model.addAttribute("status", status);
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", hotelsPage.getTotalPages());
            model.addAttribute(Constants.ATTR_PAGE_TITLE, "Hotel Management");

            log.info("Loaded {} hotels for admin page", hotelsPage.getTotalElements());
            return Constants.VIEW_ADMIN_HOTELS;
        } catch (Exception e) {
            log.error("Error loading admin hotels page: {}", e.getMessage(), e);
            model.addAttribute(Constants.ATTR_ERROR, "Error loading hotels: " + e.getMessage());
            model.addAttribute("hotels", Page.empty());
            return Constants.VIEW_ADMIN_HOTELS;
        }
    }

    @GetMapping("/rooms")
    public String rooms(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long hotelId,
            @RequestParam(required = false) String roomType,
            @RequestParam(required = false) String status,
            Model model) {
        log.info("Accessing admin rooms page - page: {}, size: {}, hotelId: {}, roomType: {}", page, size, hotelId,
                roomType);

        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());

            // Get all hotels for the dropdown
            try {
                Page<HotelResponse> hotelsPage = hotelService.getAllHotels(PageRequest.of(0, 100));
                model.addAttribute("hotels", hotelsPage.getContent());
            } catch (Exception e) {
                log.warn("Could not load hotels for dropdown: {}", e.getMessage());
                model.addAttribute("hotels", java.util.Collections.emptyList());
            }

            // Get rooms with pagination and filters
            Page<RoomResponse> roomsPage = adminService.getAllRooms(pageable, hotelId, roomType, status);
            model.addAttribute("rooms", roomsPage);
            model.addAttribute("selectedHotelId", hotelId);
            model.addAttribute("selectedRoomType", roomType);
            model.addAttribute("selectedStatus", status);
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", roomsPage.getTotalPages());
            model.addAttribute(Constants.ATTR_PAGE_TITLE, "Room Management");

            log.info("Loaded {} rooms for admin page", roomsPage.getTotalElements());
            return Constants.VIEW_ADMIN_ROOMS;
        } catch (Exception e) {
            log.error("Error loading admin rooms page: {}", e.getMessage(), e);
            model.addAttribute(Constants.ATTR_ERROR, "Error loading rooms: " + e.getMessage());
            model.addAttribute("rooms", Page.empty());
            model.addAttribute("hotels", java.util.Collections.emptyList());
            return Constants.VIEW_ADMIN_ROOMS;
        }
    }

    @GetMapping("/users")
    public String users(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status,
            Model model) {
        log.info("Accessing admin users page - page: {}, size: {}, search: {}", page, size, search);

        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
            Page<UserResponse> usersPage = adminService.getAllUsers(pageable, search, role, status);

            model.addAttribute("users", usersPage);
            model.addAttribute("search", search);
            model.addAttribute("role", role);
            model.addAttribute("status", status);
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", usersPage.getTotalPages());
            model.addAttribute(Constants.ATTR_PAGE_TITLE, "User Management");

            log.info("Loaded {} users for admin page", usersPage.getTotalElements());
            return Constants.VIEW_ADMIN_USERS;
        } catch (Exception e) {
            log.error("Error loading admin users page: {}", e.getMessage(), e);
            model.addAttribute(Constants.ATTR_ERROR, "Error loading users: " + e.getMessage());
            model.addAttribute("users", Page.empty());
            return Constants.VIEW_ADMIN_USERS;
        }
    }

    @GetMapping("/reviews")
    public String reviews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long hotelId,
            @RequestParam(required = false) Integer rating,
            @RequestParam(required = false) String status,
            Model model) {
        log.info("Accessing admin reviews page - page: {}, size: {}, hotelId: {}", page, size, hotelId);

        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());

            try {
                Page<HotelResponse> hotelsPage = hotelService.getAllHotels(PageRequest.of(0, 100));
                model.addAttribute("hotels", hotelsPage.getContent());
            } catch (Exception e) {
                log.warn("Could not load hotels for dropdown: {}", e.getMessage());
                model.addAttribute("hotels", java.util.Collections.emptyList());
            }

            Page<ReviewResponse> reviewsPage = adminService.getAllReviews(pageable, hotelId, rating, status);
            model.addAttribute("reviews", reviewsPage);
            model.addAttribute("hotelId", hotelId);
            model.addAttribute("rating", rating);
            model.addAttribute("status", status);
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", reviewsPage.getTotalPages());
            model.addAttribute(Constants.ATTR_PAGE_TITLE, "Review Management");

            log.info("Loaded {} reviews for admin page", reviewsPage.getTotalElements());
            return Constants.VIEW_ADMIN_REVIEWS;
        } catch (Exception e) {
            log.error("Error loading admin reviews page: {}", e.getMessage(), e);
            model.addAttribute(Constants.ATTR_ERROR, "Error loading reviews: " + e.getMessage());
            model.addAttribute("reviews", Page.empty());
            model.addAttribute("hotels", java.util.Collections.emptyList());
            return Constants.VIEW_ADMIN_REVIEWS;
        }
    }

    @GetMapping("/system")
    public String system(Model model) {
        log.info("Accessing admin system page");
        model.addAttribute(Constants.ATTR_PAGE_TITLE, "System Settings");
        return Constants.VIEW_ADMIN_SYSTEM;
    }
}
