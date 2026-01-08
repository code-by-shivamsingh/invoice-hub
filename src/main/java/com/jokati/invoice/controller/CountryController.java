package com.jokati.invoice.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.jokati.invoice.common.ApiResponse;
import com.jokati.invoice.common.ResponseUtil;
import com.jokati.invoice.dto.CountryRequestDTO;
import com.jokati.invoice.mapper.CountryMapper;
import com.jokati.invoice.model.Country;
import com.jokati.invoice.service.CountryService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/countries")
@Tag(name = "Country API", description = "Create, fetch, update and delete countries")
public class CountryController {

    private static final Logger log = LoggerFactory.getLogger(CountryController.class);

    private final CountryService countryService;

    public CountryController(CountryService countryService) {
        this.countryService = countryService;
    }


    @Operation(summary = "Get all countries")
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Object>> getCountries() {
        log.info("Request getCountries");

        List<Country> countries = countryService.getAllCountries();

        if (countries == null || countries.isEmpty()) {
            // Node-style: 200 OK with {}
            return ResponseUtil.okEmpty("OK");
        }

        return ResponseUtil.okObject(countries, "Countries fetched successfully");
    }

  
    @Operation(summary = "Create country")
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE,
                 produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Object>> createCountry(
            @RequestBody CountryRequestDTO request) {

        log.info("Request createCountry : {}", request);

        if (request.getCode() == null || request.getCode().isBlank()) {
            throw new IllegalArgumentException("Country code is required");
        }

        if (request.getName() == null || request.getName().isBlank()) {
            throw new IllegalArgumentException("Country name is required");
        }

        Country entity = CountryMapper.toEntity(request);
        Country saved = countryService.saveCountry(entity);

        return ResponseUtil.okObject(saved, "Country created successfully");
    }

    
    @Operation(summary = "Update country by id")
    @PutMapping(value = "/{id}",
                consumes = MediaType.APPLICATION_JSON_VALUE,
                produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Object>> updateCountry(
            @PathVariable String id,
            @RequestBody CountryRequestDTO request) {

        log.info("Request updateCountry : id={}, body={}", id, request);

        Country updated = CountryMapper.toEntity(request);
        Country saved = countryService.updateCountry(id, updated);

        return ResponseUtil.okObject(saved, "Country updated successfully");
    }

  
    @Operation(summary = "Delete country by id")
    @DeleteMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Object>> deleteCountry(@PathVariable String id) {
        log.info("Request deleteCountry : {}", id);

        countryService.deleteCountry(id);

        log.info("Successfully processed delete for country");
        return ResponseUtil.okEmpty("Country deleted (if existed)");
    }
}
