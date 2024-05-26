package com.example.streetinkbookingsystem.controllers;

import com.example.streetinkbookingsystem.models.Booking;
import com.example.streetinkbookingsystem.models.Client;
import com.example.streetinkbookingsystem.models.ProjectPicture;
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
import org.thymeleaf.context.Context;

import java.io.IOException;
import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import java.time.format.DateTimeFormatter;
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
    @Autowired
    EmailService emailService;

    /**
     *
     * @Author Nanna & Tara
     * @param model The model to be populated with attributes for rendering the view.
     * @param session The HttpSession object to check if the user is logged in.
     * @param bookingId The ID of the booking that needs to be retrieved.
     * @return Booking view,
     *         If the user is not logged in, it redirects to the home page.
     */
    @GetMapping("/booking")
            public String booking(Model model, HttpSession session, @RequestParam int bookingId,
                                  @RequestParam(required = false) Integer bookingIdToDelete) {
                // Check if the user is logged in
                if (!loginService.isUserLoggedIn(session)) {
                    return "redirect:/";
        }

        // Add logged-in user information to the model
        loginService.addLoggedInUserInfo(model, session, tattooArtistService);

        // Retrieve booking details using the provided booking ID
        Booking booking = bookingService.getBookingDetail(bookingId);
        model.addAttribute("booking", booking);

        // Retrieve and convert project pictures to base64 format
        List<String> base64Images = projectPictureService.convertToBase64(booking.getProjectPictures());
        model.addAttribute("base64Images", base64Images);

        // For deleting the booking if necessary
        if (bookingIdToDelete != null) {
            model.addAttribute("bookingIdToDelete", bookingIdToDelete);
        }

        return "home/booking";
    }


    /**
     * @Author Tara
     * @param model
     * @param session Used to determine if the user is logged in or not. User will be redirected
     *                to index page if not logged in.
     * @param date Is used, sp that we create af booking on the specific date.
     * @return den gemte booking.
     */
    @GetMapping("/create-new-booking")
    public String createNewBooking(Model model, HttpSession session, @RequestParam LocalDate date, @RequestParam (required = false) Integer bookingId ){
        if (!loginService.isUserLoggedIn(session)) {
            return "redirect:/";
        }
        loginService.addLoggedInUserInfo(model, session, tattooArtistService);
        if (bookingId != null) {
            Booking booking =bookingService.getBookingDetail(bookingId);
            model.addAttribute("booking",booking); // used when going back in the booking process so that no information is lost for the user
        }
        model.addAttribute("date", date); //needed to return to day page in "back-arrow".
        return "home/create-new-booking";
    }

    /**
     * @summary: Saves a new booking or updates an existing booking based on user input.
     *
     * @Author Tara & Nanna
     * @param startTimeSlot The start time of the booking slot.
     * @param endTimeSlot The end time of the booking slot.
     * @param date The date of the booking.
     * @param session The HttpSession object to retrieve the username of the logged-in user.
     * @param projectTitle The title of the project for the booking.
     * @param projectDesc The description of the project for the booking.
     * @param personalNote The personal notes for the booking.
     * @param isDepositPayed A boolean indicating whether the deposit is paid for the booking (default is false).
     * @param projectPictures The pictures uploaded for the project.
     * @param action The action to be taken after saving the booking (add-client or existing-client).
     * @param bookingId The ID of the booking to be updated (optional).
     * @param redirectAttributes Used for adding flash attributes for redirection.
     * @param model The model to add attributes for rendering the view.
     * @return Redirects to add-client or existing-client page based on the action parameter.
     */
    @PostMapping("/create-new-booking")
    public String saveNewBooking(@RequestParam LocalTime startTimeSlot,
                                 @RequestParam LocalTime endTimeSlot,
                                 @RequestParam LocalDate date,
                                 HttpSession session,
                                 @RequestParam String projectTitle,
                                 @RequestParam String projectDesc,
                                 @RequestParam String personalNote,
                                 @RequestParam(name = "isDepositPayed", defaultValue = "false") boolean isDepositPayed,
                                 @RequestParam("projectPictures") MultipartFile[] projectPictures,
                                 @RequestParam String action,
                                 @RequestParam(required = false) Integer bookingId,
                                 RedirectAttributes redirectAttributes, Model model) {
        String username = (String) session.getAttribute("username");

        // Ensure that booking cannot have a end-time that is before the start-time
        if (endTimeSlot.isBefore(startTimeSlot)) {
            model.addAttribute("errorMessage", "End time cannot be before start time.");
            return "home/create-new-booking?date="+date;
        }
        //Either create new booking or update the one in the making
        Booking booking;
        if (bookingId != null) {
            // Update existing booking
            bookingService.updateBooking(bookingId, startTimeSlot, endTimeSlot, date, projectTitle, projectDesc, personalNote, isDepositPayed, getPictureList(projectPictures));
            booking = bookingService.getBookingDetail(bookingId);
            projectPictureService.updateProjectPictures(bookingId, getPictureList(projectPictures));
        } else {
            // Create new booking
            // The new booking will have a default client until another is chosen
            booking = bookingService.createNewBooking(startTimeSlot, endTimeSlot, date, username, projectTitle, projectDesc, personalNote, isDepositPayed, getPictureList(projectPictures));
            projectPictureService.saveProjectPictures(booking.getId(), getPictureList(projectPictures));
        }

        int savedBookingId = booking.getId();
        //Create new client or choose an existing client:
        if ("new-client".equals(action)) {
            return "redirect:/add-client?bookingId=" + savedBookingId + "&date=" + date;
        } else if ("existing-client".equals(action)) {
           // return "redirect:/choose-client?bookingId=" + savedBookingId + "&username=" + username + "&date=" + date;
            return "redirect:/client-list?bookingId=" + savedBookingId +  "&date=" + date;
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Invalid action.");
            return "redirect:/create-new-booking?date=" + date;
        }
    }

    /**
     * Converts an array of MultipartFile objects to a list of byte arrays.
     *
     * @param projectPictures The array of MultipartFile objects.
     * @return A list of byte arrays.
     */
    private List<byte[]> getPictureList(MultipartFile[] projectPictures) {
        // Stream the array of MultipartFile objects
        return Stream.of(projectPictures)
                // Filter out empty MultipartFile objects
                .filter(file -> !file.isEmpty())
                // Map each MultipartFile object to its byte array representation
                .map(file -> {
                    try {
                        // Retrieve the byte array content of the MultipartFile object
                        return file.getBytes();
                    } catch (IOException e) {
                        e.printStackTrace();
                        return null;
                    }
                })
                // Collect the byte arrays into a list
                .collect(Collectors.toList());
    }


    /**
     * @Author Tara
     * @param model
     * @param session
     * @param bookingId
     * @param clientId
     * @return booking-preview
     */
    @GetMapping("/booking-preview")
    public String bookingPreview (Model model, HttpSession session, @RequestParam int bookingId,
                                  @RequestParam int clientId){

        if (!loginService.isUserLoggedIn(session)) {
            return "redirect:/";
        }
        loginService.addLoggedInUserInfo(model, session, tattooArtistService);

        //tilføjer den "nye" ClientId til bookingen
        clientService.updateClientOnBooking(bookingId, clientId);
        Booking booking = bookingService.getBookingDetail(bookingId);
        model.addAttribute("booking", booking);

        // Henter billeder fra den specifikke booking
        List<String> base64Images = projectPictureService.convertToBase64(booking.getProjectPictures());
        model.addAttribute("base64Images", base64Images);

        return "home/booking-preview";
    }

    /**
     * @summary Deletes the booking and redirects to the day page.
     *
     * @author Nanna
     * @param bookingId The ID of the booking to be deleted.
     * @param date The date of the booking used to redirect back to the day page.
     * @return Redirects to the day page with the specified date.
     */
    @GetMapping("/cancel-booking")
    public String cancelBooking(@RequestParam int bookingId, @RequestParam String date) {
        bookingService.deleteBooking(bookingId);
        System.out.println("here");
        return "redirect:/day?date=" + date;
    }

    /**
     * @summary Sends a confirmation email to the user.
     *
     * @author Nanna
     * @param bookingId The ID of the booking to be saved.
     * @param session The HttpSession object to retrieve user information.
     * @return A redirection to the booking details page with the specified bookingId.
     */
    @GetMapping("/save-booking")
    public String saveBooking(@RequestParam int bookingId, HttpSession session) {
        String username = (String) session.getAttribute("username");
        emailService.sendConfirmationMail(bookingId, username);
        return "redirect:/booking?bookingId="+bookingId;
    }

    @GetMapping("/edit-booking")
    public String editBooking(Model model, HttpSession session, @RequestParam("bookingId") int bookingId, RedirectAttributes redirectAttributes) {
        if (!loginService.isUserLoggedIn(session)) {
            return "redirect:/";
        }
        loginService.addLoggedInUserInfo(model, session, tattooArtistService);

        Booking booking = bookingService.getBookingDetail(bookingId);
        model.addAttribute("booking", booking);

        //Henter de billeder der allerede findes
        List<ProjectPicture> projectPictures = projectPictureService.getPicturesAsObjects(bookingId);
        model.addAttribute("projectPictures", projectPictures);
        List<String> base64Images = projectPictureService.getPicturesByBooking(bookingId);
        model.addAttribute("base64Images", base64Images);

        return "home/edit-booking";
    }

    @PostMapping("/edit-booking")
    public String updateBooking(@RequestParam LocalTime startTimeSlot, @RequestParam LocalTime endTimeSlot, @RequestParam LocalDate date,
                                @RequestParam String projectTitle, @RequestParam String projectDesc,
                                @RequestParam String personalNote, @RequestParam boolean isDepositPayed, @RequestParam(required = false) List<Integer> deletePictures,
                                @RequestParam("projectPictures") MultipartFile[] projectPictures,
                                @RequestParam int bookingId, Model model, HttpSession session) {


        if (deletePictures != null) {
            projectPictureService.deleteProjectPictures(deletePictures);
        }

        List<byte[]> pictureList = Stream.of(projectPictures).filter(file -> !file.isEmpty())
                .map(file -> {
                    try {
                        return file.getBytes();
                    } catch (IOException e){
                        e.printStackTrace();
                        return null;
                    }
                })
                .collect(Collectors.toList());

        bookingService.updateBooking(bookingId, startTimeSlot, endTimeSlot, date, projectTitle, projectDesc, personalNote, isDepositPayed, pictureList);
        return "redirect:/booking?bookingId=" + bookingId + "&username=" + session.getAttribute("username");
    }

    /*@GetMapping("/confirm-delete-booking")
    public String confirmDeleteBooking(Model model, HttpSession session, @RequestParam int bookingIdToDelete) {
        if (!loginService.isUserLoggedIn(session)) {
            return "redirect:/";
        }
        loginService.addLoggedInUserInfo(model, session, tattooArtistService);

        Booking booking = bookingService.getBookingDetail(bookingIdToDelete);
        model.addAttribute("booking", booking);
        model.addAttribute("bookingIdToDelete", bookingIdToDelete);

        return "home/confirm-delete-booking";
    }*/

    @PostMapping("/booking")
    public String deleteBookingWithWarning(@RequestParam Integer bookingIdToDelete, RedirectAttributes redirectAttributes, HttpSession session, Model model) {
        if (!loginService.isUserLoggedIn(session)) {
            return "redirect:/";
        }

        if (!loginService.isUserLoggedIn(session)) {
            return "redirect:/";
        }
        loginService.addLoggedInUserInfo(model, session, tattooArtistService);

        Booking booking = bookingService.getBookingDetail(bookingIdToDelete);
        model.addAttribute("booking", booking);
        model.addAttribute("bookingIdToDelete", bookingIdToDelete);

        loginService.addLoggedInUserInfo(model, session, tattooArtistService);

        redirectAttributes.addAttribute("bookingIdToDelete", bookingIdToDelete);
        return "redirect:/booking?bookingId=" + bookingIdToDelete;
    }

    /**
     *
     * @param bookingIdToDelete
     * @param session
     * @param model
     * @return
     */
    @PostMapping("/delete-booking")
    public String deleteBooking(@RequestParam Integer bookingIdToDelete, HttpSession session, Model model) {
        if (!loginService.isUserLoggedIn(session)) {
            return "redirect:/";
        }

        loginService.addLoggedInUserInfo(model, session, tattooArtistService);
        bookingService.deleteBooking(bookingIdToDelete);
        return "redirect:/calendar";
    }




}



