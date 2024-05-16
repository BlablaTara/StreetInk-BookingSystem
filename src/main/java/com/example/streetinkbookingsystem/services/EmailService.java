package com.example.streetinkbookingsystem.services;

import com.example.streetinkbookingsystem.models.Booking;
import com.example.streetinkbookingsystem.models.Client;
import com.example.streetinkbookingsystem.models.TattooArtist;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.MimeMessagePreparator;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender javaMailSender;

    @Autowired
    private TemplateEngine templateEngine;

    public void sendConfirmationMail(String clientEmail, Context context) {
      String processedHTMLTemplate =  templateEngine.process("home/confirmation-mail", context);
        // Start preparing the email
        MimeMessagePreparator preparator = message -> {
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setFrom("nannahofgaard@gmail.com"); //change to client email
            helper.setTo(clientEmail);
            helper.setSubject("Booking Confirmation");
            helper.setText(processedHTMLTemplate, true);
        };

        javaMailSender.send(preparator); //send the email
        System.out.println("sent");
    }


    public void sendEmail(String recipient, String subject, String content) {
        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(recipient);
        message.setSubject(subject);
        message.setText(content);
        javaMailSender.send(message);
    }

    //Used regex to verify that the email format is correct
    public boolean isValidEmail(String email) {
        String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
        Pattern pattern = Pattern.compile(emailRegex);
        Matcher matcher = pattern.matcher(email);
        return matcher.matches();
    }
}
