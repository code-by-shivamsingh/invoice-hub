
package com.jokati.invoice.controller;

import java.net.URI;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jokati.invoice.common.ApiResponse;
import com.jokati.invoice.common.ErrorCodes;
import com.jokati.invoice.common.ErrorDetail;
import com.jokati.invoice.common.ResponseUtil;
import com.jokati.invoice.dto.CreateTemplateRequestDTO;
import com.jokati.invoice.dto.SendPlainEmailRequestDTO;
import com.jokati.invoice.dto.SendTemplateEmailRequestDTO;
import com.jokati.invoice.dto.UpdateTemplateRequestDTO;
import com.jokati.invoice.exception.ApiException;
import com.jokati.invoice.model.EmailTemplate;
import com.jokati.invoice.service.EmailService;
import com.jokati.invoice.service.EmailTemplateService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Emails", description = "Send plain emails and template-based emails; manage templates")
@RestController
@RequestMapping("/api/v1/emails")
@RequiredArgsConstructor
public class EmailController {

    private final EmailService emailService;
    private final EmailTemplateService templateService;

    // A) Send a plain HTML email
    @Operation(summary = "Send a plain HTML email")
    @PostMapping("/plain")
    public ResponseEntity<ApiResponse<Object>> sendPlain(@Valid @RequestBody SendPlainEmailRequestDTO req) {
        try {
            emailService.sendEmail(req.getTo(), req.getSubject(), req.getHtml());
            return ResponseUtil.withStatusEmpty(HttpStatus.ACCEPTED, "Email accepted for delivery");
        } catch (ApiException ex) {
            return ResponseUtil.error(ex.getStatus(), ex.getMessage(), ex.getErrors());
        }
    }

    // B) Create template
    @Operation(summary = "Create a new email template")
    @PostMapping("/templates")
    public ResponseEntity<ApiResponse<Object>> createTemplate(@Valid @RequestBody CreateTemplateRequestDTO req) {
        try {
            EmailTemplate saved = templateService.createTemplate(req);
            // 201 Created with Location header
            return ResponseEntity.created(URI.create("/api/v1/emails/templates/" + saved.getId()))
                    .headers(ResponseUtil.okEmpty("Template created").getHeaders())
                    .body(ApiResponse.success(
                            HttpStatus.CREATED.value(),
                            "Template created",
                            saved,
                            ResponseUtil.currentTraceId(),
                            ResponseUtil.currentApplication()
                    ));
        } catch (ApiException ex) {
            return ResponseUtil.error(ex.getStatus(), ex.getMessage(), ex.getErrors());
        }
    }

    // Read template by id
    @Operation(summary = "Get template by id")
    @GetMapping("/templates/{id}")
    public ResponseEntity<ApiResponse<Object>> getTemplate(@PathVariable String id) {
        try {
            EmailTemplate tpl = templateService.getTemplateById(id);
            return ResponseUtil.okObject(tpl, "Template fetched");
        } catch (ApiException ex) {
            return ResponseUtil.error(ex.getStatus(), ex.getMessage(), ex.getErrors());
        }
    }

    // Read template by name
    @Operation(summary = "Get template by name")
    @GetMapping("/templates/by-name/{name}")
    public ResponseEntity<ApiResponse<Object>> getTemplateByName(@PathVariable String name) {
        try {
            EmailTemplate tpl = templateService.getTemplateByName(name);
            return ResponseUtil.okObject(tpl, "Template fetched");
        } catch (ApiException ex) {
            return ResponseUtil.error(ex.getStatus(), ex.getMessage(), ex.getErrors());
        }
    }

    // Update template by id
    @Operation(summary = "Update template by id")
    @PutMapping("/templates/{id}")
    public ResponseEntity<ApiResponse<Object>> updateTemplate(@PathVariable String id,
                                                              @RequestBody UpdateTemplateRequestDTO req) {
        try {
            EmailTemplate saved = templateService.updateTemplate(id, req);
            return ResponseUtil.okObject(saved, "Template updated");
        } catch (ApiException ex) {
            return ResponseUtil.error(ex.getStatus(), ex.getMessage(), ex.getErrors());
        }
    }

    // Delete template
    @Operation(summary = "Delete template by id")
    @DeleteMapping("/templates/{id}")
    public ResponseEntity<ApiResponse<Object>> deleteTemplate(@PathVariable String id) {
        try {
            templateService.deleteTemplate(id);
            return ResponseUtil.okEmpty("Template deleted");
        } catch (ApiException ex) {
            return ResponseUtil.error(ex.getStatus(), ex.getMessage(), ex.getErrors());
        }
    }

    // C) Send using template (by id or name)
    @Operation(summary = "Render and send an email using a stored template (by id or name)")
    @PostMapping("/send-template")
    public ResponseEntity<ApiResponse<Object>> sendWithTemplate(@Valid @RequestBody SendTemplateEmailRequestDTO req) {
        boolean byId   = req.getTemplateId()   != null && !req.getTemplateId().isBlank();
        boolean byName = req.getTemplateName() != null && !req.getTemplateName().isBlank();

        if (!byId && !byName) {
            return ResponseUtil.error(
                    HttpStatus.BAD_REQUEST,
                    "Provide either templateId or templateName",
                    new ErrorDetail(ErrorCodes.VALIDATION_ERROR, "Provide either templateId or templateName", "template", null)
            );
        }

        try {
            if (byId) {
                emailService.send(req.getTemplateId(),req.getTo(),req.getModel());
            } else {
                emailService.send(req.getTemplateName(),req.getTo(),req.getModel());
       }
          return ResponseUtil.withStatusEmpty(HttpStatus.ACCEPTED,"Templated email accepted for delivery");

     } catch (ApiException ex) {
            return ResponseUtil.error(ex.getStatus(),
                                      ex.getMessage(),
                                      ex.getErrors());
        }

    }
}

