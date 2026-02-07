
package com.jokati.invoice.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.jokati.invoice.common.ErrorCodes;
import com.jokati.invoice.common.ErrorDetail;
import com.jokati.invoice.dto.CreateTemplateRequestDTO;
import com.jokati.invoice.dto.UpdateTemplateRequestDTO;
import com.jokati.invoice.exception.ApiException;
import com.jokati.invoice.model.EmailTemplate;
import com.jokati.invoice.repository.EmailTemplateRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmailTemplateService {

    private final EmailTemplateRepository templateRepository;

    public EmailTemplate createTemplate(CreateTemplateRequestDTO req) {
        if (templateRepository.existsByName(req.getName())) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    ErrorCodes.CONFLICT,
                    "Template name already exists",
                    List.of(new ErrorDetail(ErrorCodes.CONFLICT, "Template name already exists", "name", req.getName()))
            );
        }
        var entity = EmailTemplate.builder()
                .name(req.getName())
                .subject(req.getSubject())
                .body(req.getBody())
                .build();

        return templateRepository.save(entity);
    }

    public EmailTemplate getTemplateById(String id) {
        return templateRepository.findById(id).orElseThrow(() ->
                new ApiException(
                        HttpStatus.NOT_FOUND,
                        ErrorCodes.NOT_FOUND,
                        "Template not found",
                        List.of(new ErrorDetail(ErrorCodes.NOT_FOUND, "Template not found", "id", id))
                ));
    }

    public EmailTemplate getTemplateByName(String name) {
        return templateRepository.findByName(name).orElseThrow(() ->
                new ApiException(
                        HttpStatus.NOT_FOUND,
                        ErrorCodes.NOT_FOUND,
                        "Template not found",
                        List.of(new ErrorDetail(ErrorCodes.NOT_FOUND, "Template not found", "name", name))
                ));
    }

    public EmailTemplate updateTemplate(String id, UpdateTemplateRequestDTO req) {
        var existing = templateRepository.findById(id).orElseThrow(() ->
                new ApiException(
                        HttpStatus.NOT_FOUND,
                        ErrorCodes.NOT_FOUND,
                        "Template not found",
                        List.of(new ErrorDetail(ErrorCodes.NOT_FOUND, "Template not found", "id", id))
                ));

        if (req.getName() != null && !req.getName().isBlank() && !req.getName().equals(existing.getName())) {
            if (templateRepository.existsByName(req.getName())) {
                throw new ApiException(
                        HttpStatus.CONFLICT,
                        ErrorCodes.CONFLICT,
                        "Template name already exists",
                        List.of(new ErrorDetail(ErrorCodes.CONFLICT, "Template name already exists", "name", req.getName()))
                );
            }
            existing.setName(req.getName());
        }
        if (req.getSubject() != null) existing.setSubject(req.getSubject());
        if (req.getBody() != null)    existing.setBody(req.getBody());

        return templateRepository.save(existing);
    }

    public void deleteTemplate(String id) {
        if (!templateRepository.existsById(id)) {
            throw new ApiException(
                    HttpStatus.NOT_FOUND,
                    ErrorCodes.NOT_FOUND,
                    "Template not found",
                    List.of(new ErrorDetail(ErrorCodes.NOT_FOUND, "Template not found", "id", id))
            );
        }
        templateRepository.deleteById(id);
    }
}
