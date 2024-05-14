package com.example.streetinkbookingsystem.controllers;

import com.example.streetinkbookingsystem.models.Booking;
import com.example.streetinkbookingsystem.models.TattooArtist;
import com.example.streetinkbookingsystem.services.BookingService;
import com.example.streetinkbookingsystem.services.LoginService;
import com.example.streetinkbookingsystem.services.TattooArtistService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;
import java.time.LocalDate;

@Controller
public class BookingController {

    @Autowired
    private BookingService bookingService;
    @Autowired
    LoginService loginService;
    @Autowired
    TattooArtistService tattooArtistService;

    @GetMapping("/booking")
    public String booking(Model model, HttpSession session, @RequestParam int bookingId, @RequestParam String username/*, Principal principal*/){
        boolean loggedIn = loginService.isUserLoggedIn(session);
        if (loggedIn) {
            model.addAttribute("loggedIn", loggedIn);
            model.addAttribute("username", session.getAttribute(username));
            TattooArtist profile = tattooArtistService.getTattooArtistByUsername(username);
            model.addAttribute("profile", profile);
        } else {
            return "redirect:/";
        }
       // fjerner denne så man ikke skal bruge en godkendelse endnu.
        //String tattooArtistId = principal.getName();
        //Hardcodet artist username
        String tattooArtistId = username;
        model.addAttribute("booking", bookingService.getBookingDetail(bookingId));
        return "home/booking";
    }


    @GetMapping("/create-new-booking")
        public String createNewBooking(Model model, @RequestParam LocalDate date){
            model.addAttribute("date",date);
            return "home/create-new-booking";
        }


}
