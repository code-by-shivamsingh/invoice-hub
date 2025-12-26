
package com.jokati.invoice.service;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.jokati.invoice.common.ErrorCodes;
import com.jokati.invoice.common.ErrorDetail;
import com.jokati.invoice.exception.ApiException;

import jakarta.mail.MessagingException;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Service
public class EmailService {

	private final JavaMailSender mailSender;

	public EmailService(JavaMailSender mailSender) {
		this.mailSender = mailSender;
	}

	/**
	 * Sends an HTML email. Any MessagingException is wrapped into ApiException to
	 * keep controllers clean.
	 */
	public void sendEmail(String to, String subject, String html) {
		// Defensive validation to fail-fast with clear domain error
		if (to == null || to.isBlank()) {
			throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCodes.VALIDATION_ERROR,
					"Recipient email must not be blank", List.of(new ErrorDetail(ErrorCodes.VALIDATION_ERROR,
							"Recipient email must not be blank", "email", to)));
		}
		if (subject == null || subject.isBlank()) {
			throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCodes.VALIDATION_ERROR, "Subject must not be blank",
					List.of(new ErrorDetail(ErrorCodes.VALIDATION_ERROR, "Subject must not be blank", "subject",
							subject)));
		}
		if (html == null || html.isBlank()) {
			throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCodes.VALIDATION_ERROR,
					"Email body (HTML) must not be blank", List.of(new ErrorDetail(ErrorCodes.VALIDATION_ERROR,
							"Email body (HTML) must not be blank", "html", null)));
		}

		try {
			var mimeMessage = mailSender.createMimeMessage();
			// true = multipart if you later add inline images/attachments
			var helper = new MimeMessageHelper(mimeMessage, true, StandardCharsets.UTF_8.name());
			helper.setTo(to);
			helper.setSubject(subject);
			helper.setText(html, true); // HTML body

			// Optional: set a default "from" if your SMTP requires it
			// helper.setFrom("no-reply@jokati.example");

			mailSender.send(mimeMessage);
		} catch (MessagingException ex) {
			// Known mail construction/sending issue -> wrap as domain error
			throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCodes.EMAIL_ERROR, "Email sending failed",
					List.of(new ErrorDetail(ErrorCodes.EMAIL_ERROR, ex.getMessage(), "email",
							to + (MDC.get("traceId") != null ? " | traceId=" + MDC.get("traceId") : ""))));
		} catch (Exception ex) {
			// Any other unexpected runtime exception -> also wrap
			throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCodes.EMAIL_ERROR,
					"Unexpected error while sending email",
					List.of(new ErrorDetail(ErrorCodes.EMAIL_ERROR, ex.getMessage(), "email",
							to + (MDC.get("traceId") != null ? " | traceId=" + MDC.get("traceId") : ""))));
		}
	}

	/** Template-based send (best-effort; fill when you have a template engine). */
	public void send(String templateId, String to, Map<String, Object> model) {
		try {
			var msg = mailSender.createMimeMessage();
			var helper = new MimeMessageHelper(msg, "UTF-8");
			helper.setTo(to);
			helper.setSubject("[Template] " + templateId);
			helper.setText("Template " + templateId + " rendered with model: " + model, false);
			mailSender.send(msg);
			log.info("Email sent to {}", to);
		} catch (Exception e) {
			log.warn("Email send failed to {}: {}", to, e.getMessage());
		}
	}
}
