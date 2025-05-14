package com.hotelreservation.controller.view;

import com.hotelreservation.dto.request.BookingRequest;
import com.hotelreservation.dto.response.HotelResponse;
import com.hotelreservation.dto.response.RoomResponse;
import com.hotelreservation.service.BookingService;
import com.hotelreservation.service.HotelService;
import com.hotelreservation.service.RoomService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Controller
@RequestMapping("/booking")
public class BookingViewController {

    private static final Logger logger = LoggerFactory.getLogger(BookingViewController.class);

    private final RoomService roomService;
    private final HotelService hotelService;
    private final BookingService bookingService;

    @Autowired
    public BookingViewController(RoomService roomService, HotelService hotelService, BookingService bookingService) {
        this.roomService = roomService;
        this.hotelService = hotelService;
        this.bookingService = bookingService;
    }

    @GetMapping("/new")
    public String bookingForm(
            @RequestParam Long roomId,
            @RequestParam LocalDate checkIn,
            @RequestParam LocalDate checkOut,
            @RequestParam Integer guests,
            Model model) {

        logger.info("Loading booking form for roomId: {}, checkIn: {}, checkOut: {}, guests: {}",
                roomId, checkIn, checkOut, guests);

        try {
            // Get room details
            RoomResponse room = roomService.getRoomById(roomId);

            // Get hotel details
            HotelResponse hotel = hotelService.getHotelById(room.getHotelId());

            // Calculate number of nights
            long nights = ChronoUnit.DAYS.between(checkIn, checkOut);

            // Calculate total price (room price * nights + taxes and fees)
            BigDecimal roomTotal = room.getPricePerNight().multiply(BigDecimal.valueOf(nights));
            BigDecimal taxRate = new BigDecimal("0.12"); // 12% for taxes and fees
            BigDecimal taxesAndFees = roomTotal.multiply(taxRate).setScale(2, RoundingMode.HALF_UP);
            BigDecimal totalPrice = roomTotal.add(taxesAndFees).setScale(2, RoundingMode.HALF_UP);

            // Create booking request object
            BookingRequest bookingRequest = new BookingRequest();
            bookingRequest.setRoomId(roomId);
            bookingRequest.setCheckInDate(checkIn);
            bookingRequest.setCheckOutDate(checkOut);
            bookingRequest.setGuestCount(guests);
            bookingRequest.setTotalPrice(totalPrice);

            model.addAttribute("room", room);
            model.addAttribute("hotel", hotel);
            model.addAttribute("bookingRequest", bookingRequest);
            model.addAttribute("nights", nights);
            model.addAttribute("taxesAndFees", taxesAndFees);
            model.addAttribute("roomTotal", roomTotal);

            return "booking/form";
        } catch (Exception e) {
            logger.error("Error loading booking form", e);
            return "redirect:/hotels";
        }
    }

    @PostMapping("/create")
    public String createBooking(BookingRequest bookingRequest, RedirectAttributes redirectAttributes) {
        logger.info("Creating booking for roomId: {}, checkIn: {}, checkOut: {}",
                bookingRequest.getRoomId(), bookingRequest.getCheckInDate(), bookingRequest.getCheckOutDate());

        try {
            // Create booking
            var bookingResponse = bookingService.createBooking(bookingRequest);

            // Add success message
            redirectAttributes.addFlashAttribute("successMessage",
                    "Booking confirmed! Your booking reference is: " + bookingResponse.getId());

            // Redirect to booking confirmation page
            return "redirect:/booking/confirmation?bookingId=" + bookingResponse.getId();
        } catch (Exception e) {
            logger.error("Error creating booking", e);

            // Add error message
            redirectAttributes.addFlashAttribute("errorMessage",
                    "An error occurred while processing your booking: " + e.getMessage());

            // Redirect back to the booking form
            return "redirect:/booking/new?roomId=" + bookingRequest.getRoomId() +
                    "&checkIn=" + bookingRequest.getCheckInDate() +
                    "&checkOut=" + bookingRequest.getCheckOutDate() +
                    "&guests=" + bookingRequest.getGuestCount();
        }
    }

    @GetMapping("/confirmation")
    public String bookingConfirmation(@RequestParam Long bookingId, Model model) {
        logger.info("Loading booking confirmation for bookingId: {}", bookingId);

        try {
            // Get booking details
            var booking = bookingService.getBookingById(bookingId);

            // Add to model
            model.addAttribute("booking", booking);

            return "booking/confirmation";
        } catch (Exception e) {
            logger.error("Error loading booking confirmation", e);
            return "redirect:/";
        }
    }
}