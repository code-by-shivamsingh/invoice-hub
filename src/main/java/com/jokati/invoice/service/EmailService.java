
package com.jokati.invoice.service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import com.jokati.invoice.common.ErrorCodes;
import com.jokati.invoice.common.ErrorDetail;
import com.jokati.invoice.exception.ApiException;
import com.jokati.invoice.model.EmailTemplate;
import com.jokati.invoice.repository.EmailTemplateRepository;

import jakarta.mail.MessagingException;
import lombok.extern.slf4j.Slf4j;

/**
 * Centralized email sending service:
 *  - sendEmail(): plain HTML email
 *  - sendEmailWithTemplateId()/sendEmailWithTemplateName(): render template (subject/body) and send
 *
 * Templating rules:
 *   {{key}}   -> HTML-escaped substitution
 *   {{{key}}} -> RAW substitution (use with caution)
 */
@Slf4j
@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final EmailTemplateRepository templateRepository;

    /** Optional default From address (configure in application.properties) */
    @Value("${spring.mail.from:}")
    private String defaultFrom;

    public EmailService(JavaMailSender mailSender, EmailTemplateRepository templateRepository) {
        this.mailSender = mailSender;
        this.templateRepository = templateRepository;
    }

    /** Sends a plain HTML email. */
    public void sendEmail(String to, String subject, String html) {
        validateBasicsOrThrow(to, subject, html);
        try {
            var mimeMessage = mailSender.createMimeMessage();
            var helper = new MimeMessageHelper(mimeMessage, true, StandardCharsets.UTF_8.name());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);

            if (defaultFrom != null && !defaultFrom.isBlank()) {
                helper.setFrom(defaultFrom);
            }

            mailSender.send(mimeMessage);
            log.info("Email sent to {}", to);
        } catch (MessagingException ex) {
            throw emailError("Email sending failed", "email", buildDetailsWithTrace(to, ex));
        } catch (Exception ex) {
            throw emailError("Unexpected error while sending email", "email", buildDetailsWithTrace(to, ex));
        }
    }

    /** Sends an email using a stored template (by Mongo template id). */
    public void sendEmailWithTemplateId(String to, String templateId, @Nullable Map<String, Object> model) {
        var tpl = templateRepository.findById(templateId).orElseThrow(() ->
                notFoundError("Template not found", "templateId", templateId));
        sendRendered(to, tpl, model);
    }

    /** Sends an email using a stored template (by unique template name). */
    public void sendEmailWithTemplateName(String to, String templateName, @Nullable Map<String, Object> model) {
        var tpl = templateRepository.findByName(templateName).orElseThrow(() ->
                notFoundError("Template not found", "templateName", templateName));
        sendRendered(to, tpl, model);
    }

    // --------------------- internal helpers ---------------------

    private void sendRendered(String to, EmailTemplate tpl, @Nullable Map<String, Object> model) {
        String subject = render(tpl.getSubject(), model);
        String body    = render(tpl.getBody(), model);

        validateBasicsOrThrow(to, subject, body);
        sendEmail(to, subject, body);
    }

    /** Aggregates validation errors into a single ApiException. */
    private void validateBasicsOrThrow(String to, String subject, String html) {
        List<ErrorDetail> details = new ArrayList<>();

        if (to == null || to.isBlank()) {
            details.add(new ErrorDetail(ErrorCodes.VALIDATION_ERROR, "Recipient email must not be blank", "email", null));
        }
        if (subject == null || subject.isBlank()) {
            details.add(new ErrorDetail(ErrorCodes.VALIDATION_ERROR, "Subject must not be blank", "subject", null));
        }
        if (html == null || html.isBlank()) {
            details.add(new ErrorDetail(ErrorCodes.VALIDATION_ERROR, "Email body (HTML) must not be blank", "html", null));
        }

        if (!details.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCodes.VALIDATION_ERROR, "Email validation failed", details);
        }
    }

    /**
     * Minimal template renderer:
     *  - Triple mustache {{{key}}} -> raw (unescaped)
     *  - Double mustache  {{key}}   -> HTML-escaped
     */
    private String render(@Nullable String template, @Nullable Map<String, Object> model) {
        if (template == null || template.isEmpty()) return "";
        if (model == null || model.isEmpty()) return template;

        String rendered = template;

        // Raw first
        for (Map.Entry<String, Object> e : model.entrySet()) {
            String key = e.getKey();
            String rawToken = "{{{" + key + "}}}";
            String rawVal = e.getValue() == null ? "" : String.valueOf(e.getValue());
            rendered = rendered.replace(rawToken, rawVal);
        }

        // Escaped second
        for (Map.Entry<String, Object> e : model.entrySet()) {
            String key = e.getKey();
            String token = "{{" + key + "}}";
            String safeVal = HtmlUtils.htmlEscape(e.getValue() == null ? "" : String.valueOf(e.getValue()));
            rendered = rendered.replace(token, safeVal);
        }

        return rendered;
    }

    // --------------------- error helpers ---------------------

    private ApiException notFoundError(String message, String field, String value) {
        return new ApiException(
                HttpStatus.NOT_FOUND,
                ErrorCodes.NOT_FOUND,
                message,
                List.of(new ErrorDetail(ErrorCodes.NOT_FOUND, message, field, addTraceId(value)))
        );
    }

    private ApiException emailError(String message, String field, String details) {
        return new ApiException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ErrorCodes.EMAIL_ERROR,
                message,
                List.of(new ErrorDetail(ErrorCodes.EMAIL_ERROR, message, field, addTraceId(details)))
        );
    }

    private String buildDetailsWithTrace(String to, Exception ex) {
        String base = "recipient=" + to + "; error=" + ex.getClass().getSimpleName() + ": " + safeMsg(ex);
        return addTraceId(base);
    }

    private String addTraceId(String details) {
        String traceId = MDC.get("traceId");
        return details + (traceId != null ? " | traceId=" + traceId : "");
    }

    private String safeMsg(Throwable t) {
        return t == null ? "n/a" : (t.getMessage() == null ? "n/a" : t.getMessage());
    }
}
