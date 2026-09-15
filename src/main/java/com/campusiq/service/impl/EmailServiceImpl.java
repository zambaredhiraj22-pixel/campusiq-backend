package com.campusiq.service.impl;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.campusiq.service.EmailService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender javaMailSender;

    @Override
    public void sendMockTestPassedEmail(
            String toEmail,
            String studentName,
            double percentage) {

        SimpleMailMessage message =
                new SimpleMailMessage();

        message.setTo(toEmail);
        message.setSubject(
                "CAMPUS-IQ Mock Test Result"
        );

        message.setText(
                "Hello " + studentName + ",\n\n"
                + "Congratulations!\n\n"
                + "You have successfully passed the CAMPUS-IQ Mock Test.\n\n"
                + "Score: " + percentage + "%\n"
                + "Required Percentage: 65%\n"
                + "Result: PASS\n\n"
                + "You are one step closer to placement readiness.\n\n"
                + "Regards,\n"
                + "CAMPUS-IQ Team"
        );

        javaMailSender.send(message);
    }

    @Override
    public void sendPlacementReadyEmail(
            String toEmail,
            String studentName,
            double mockTestPercentage,
            double interviewScore) {

        SimpleMailMessage message =
                new SimpleMailMessage();

        message.setTo(toEmail);

        message.setSubject(
                "CAMPUS-IQ - Congratulations! You Are Placement Ready"
        );

        message.setText(
                "Hello " + studentName + ",\n\n"
                + "Congratulations!\n\n"
                + "You have successfully completed the CAMPUS-IQ placement assessment process.\n\n"
                + "Mock Test Score: "
                + String.format("%.2f", mockTestPercentage)
                + "%\n"
                + "AI Interview Score: "
                + String.format("%.2f", interviewScore)
                + "%\n"
                + "Required Percentage: 65%\n\n"
                + "Mock Test Result: PASS\n"
                + "AI Interview Result: PASS\n"
                + "Placement Status: PLACEMENT READY\n\n"
                + "You are now marked as Placement Ready in CAMPUS-IQ.\n"
                + "You can proceed with eligible placement opportunities and company drives.\n\n"
                + "Best wishes for your placements!\n\n"
                + "Regards,\n"
                + "CAMPUS-IQ Team"
        );

        javaMailSender.send(message);
    }
}
