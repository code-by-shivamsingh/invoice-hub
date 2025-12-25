
package com.jokati.invoice.service;

import jakarta.mail.MessagingException;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }
    
    public void sendEmail(String to, String subject, String html) throws MessagingException {
        var msg = mailSender.createMimeMessage();
        var helper = new MimeMessageHelper(msg, "UTF-8");
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(html, true);
        mailSender.send(msg);
    }

	public void send(String templateId, String to, Map<String, Object> model) {
		log.info("email sent to {}",to);
		// TODO Auto-generated method stub
		 var msg = mailSender.createMimeMessage();
//	        var helper = new MimeMessageHelper(msg, "UTF-8");
//	        helper.setTo(to);
//	        helper.setSubject("test finanvial email");
//	        helper.setText(html, false);
	        mailSender.send(msg);
	}
}
