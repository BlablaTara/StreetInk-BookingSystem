package com.example.streetinkbookingsystem.controllers;

import com.example.streetinkbookingsystem.models.Booking;
import com.example.streetinkbookingsystem.models.TattooArtist;
import com.example.streetinkbookingsystem.services.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Controller
public class BookingController {

    @Autowired
    private BookingService bookingService;
    @Autowired
    LoginService loginService;
    @Autowired
    TattooArtistService tattooArtistService;
    @Autowired
    ProjectPictureService projectPictureService;
    @Autowired
    ClientService clientService;

    /**
     * @Author Nanna
     * @param model
     * @param session
     * @param bookingId
     * @param username
     * @return
     */
    @GetMapping("/booking")
     public String booking(Model model, HttpSession session, @RequestParam int bookingId, @RequestParam String username){
        boolean loggedIn = loginService.isUserLoggedIn(session);
        if (!loggedIn) {
            return "redirect:/";
        }

        model.addAttribute("loggedIn", loggedIn);
        model.addAttribute("username", session.getAttribute(username));
        TattooArtist tattooArtist = tattooArtistService.getTattooArtistByUsername(username);
        model.addAttribute("tattooArtist", tattooArtist);

        Booking booking = bookingService.getBookingDetail(bookingId);
        model.addAttribute("booking", booking);

        // Henter billeder fra den specifikke booking
        List<String> base64Images = projectPictureService.convertToBase64(booking.getProjectPictures());
        model.addAttribute("base64Images", base64Images);



        return "home/booking";
    }


    /**
     * @Author Tara
     * @param model
     * @param session
     * @param date
     * @return "home/create-new-booking"
     */
    @GetMapping("/create-new-booking")
    public String createNewBooking(Model model, HttpSession session, @RequestParam LocalDate date){
        boolean loggedIn = loginService.isUserLoggedIn(session);
        if (loggedIn){
            model.addAttribute("loggedIn", loggedIn);
        } else {
            return "redirect:/";
        }

        String username = (String) session.getAttribute("username");
        TattooArtist tattooArtist = tattooArtistService.getTattooArtistByUsername(username);

        model.addAttribute("loggedIn", loggedIn);
        model.addAttribute("username", username);
        model.addAttribute("tattooArtist", tattooArtist);
        model.addAttribute("date", date);

        return "home/create-new-booking";
    }

    /**
     * @Author Tara
     * @param startTimeSlot
     * @param endTimeSlot
     * @param date
     * @param session
     * @param projectTitle
     * @param projectDesc
     * @param personalNote
     * @param isDepositPayed
     * @param projectPictures
     * @param action
     * @param redirectAttributes
     * @return
     */
    @PostMapping("/save-tattoo-info")
    public String saveNewBooking(@RequestParam LocalTime startTimeSlot,
                                 @RequestParam LocalTime endTimeSlot,
                                 @RequestParam LocalDate date,
                                 HttpSession session,
                                 @RequestParam String projectTitle,
                                 @RequestParam String projectDesc,
                                 @RequestParam String personalNote,
                                 @RequestParam(name = "isDepositPayed", defaultValue = "false") boolean isDepositPayed,
                                 //Multipartfile er en datatypen. Disse objekter repræsenterer filer, som er blevet uploadet via en html-formular. Der kanvære flere filer som er uploadet.
                                 @RequestParam("projectPictures") MultipartFile[] projectPictures,
                                 @RequestParam String action, // Tilføjet parameter for handling af knap handlinger
                                 RedirectAttributes redirectAttributes) {
        try {
            String username = (String) session.getAttribute("username");

            if (username == null){
                redirectAttributes.addFlashAttribute("errorMessge", "You session ran out, log in again.");
                return "redirect:/";
            }

            List<byte[]> pictureList = Stream.of(projectPictures).filter(file -> !file.isEmpty())
                    .map(file -> {
                        try { //læs op på dette
                            return file.getBytes();
                        } catch (IOException e){
                            e.printStackTrace(); //læs op på dette
                            return null;
                        }
                    })
                    .collect(Collectors.toList());

            // Gemmer booking og henter den gemte entitet
           Booking newBooking = bookingService.createNewBooking(startTimeSlot, endTimeSlot, date, username, projectTitle,
                    projectDesc, personalNote, isDepositPayed, pictureList);

           projectPictureService.saveProjectPictures(newBooking.getId(), pictureList);

           //henter bookingId fra den gemte entitet
            int bookingId = newBooking.getId() ;

            if ("new-client".equals(action)) {
                //Omdirigerer til add-client med det gemte bookingId
                return "redirect:/add-client?bookingId=" + bookingId + "&username=" + username;
            } else if ("existing-client".equals(action)) {
                return "redirect:/choose-client?bookingId=" + bookingId + "&username=" + username;
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Invalid actions.");
                return "redirect:/create-new-booking?date=" + date;
            }

        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", "Something went wrong, try again.");
            return "redirect:/create-new-booking?date=" + date;

        }

    }

    @GetMapping("/booking-preview")
    public String bookingPreview (Model model, HttpSession session, @RequestParam int bookingId,
                                  @RequestParam String username,
                                  @RequestParam int clientId){
        System.out.println("Booking ID: " + bookingId);
        System.out.println("Username: " + username);
        System.out.println("Client ID: " + clientId);
        boolean loggedIn = loginService.isUserLoggedIn(session);
        if (!loggedIn) {
            return "redirect:/";
        }

        System.out.println("Booking ID: " + bookingId);
        System.out.println("Username: " + username);
        System.out.println("Client ID: " + clientId);

        clientService.updateClientOnBooking(bookingId, clientId);

        model.addAttribute("loggedIn", loggedIn);
        model.addAttribute("username", session.getAttribute(username));
        TattooArtist tattooArtist = tattooArtistService.getTattooArtistByUsername(username);
        model.addAttribute("tattooArtist", tattooArtist);

        Booking booking = bookingService.getBookingDetail(bookingId);
        model.addAttribute("booking", booking);

        // Henter billeder fra den specifikke booking
        List<String> base64Images = projectPictureService.convertToBase64(booking.getProjectPictures());
        model.addAttribute("base64Images", base64Images);



        return "home/booking-preview";
    }

}



