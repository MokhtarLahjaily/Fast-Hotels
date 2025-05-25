package com.hotelreservation.controller.view;

import com.hotelreservation.dto.response.HotelResponse;
import com.hotelreservation.dto.response.RoomResponse;
import com.hotelreservation.service.HotelService;
import com.hotelreservation.service.RoomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
@RequiredArgsConstructor
@Slf4j
public class RoomViewController {

    private final RoomService roomService;
    private final HotelService hotelService;

    @GetMapping("/rooms/{id}")
    public String viewRoom(@PathVariable Long id, Model model) {
        try {
            RoomResponse room = roomService.getRoomById(id);
            HotelResponse hotel = hotelService.getHotelById(room.getHotelId());

            model.addAttribute("room", room);
            model.addAttribute("hotel", hotel);

            return "room/detail";
        } catch (Exception e) {
            log.error("Error viewing room: {}", id, e);
            model.addAttribute("errorMessage", "Room not found");
            return "error/404";
        }
    }
}
