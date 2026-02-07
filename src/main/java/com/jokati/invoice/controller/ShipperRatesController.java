
package com.jokati.invoice.controller;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jokati.invoice.common.ApiResponse;
import com.jokati.invoice.common.ResponseUtil;
import com.jokati.invoice.dto.ShipperRateUpdateRequestDTO;
import com.jokati.invoice.dto.ShipperRateZonePriceUpdateRequestDTO;
import com.jokati.invoice.dto.ShipperRatesRequestDTO;
import com.jokati.invoice.dto.ShipperRatesResponseDTO;
import com.jokati.invoice.model.ShipperRates;
import com.jokati.invoice.service.ShipperRatesService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/shipper-rates")
@Tag(name = "Shipper Rates API", description = "Manage shipper rates")
public class ShipperRatesController {
	private static final Logger log = LoggerFactory.getLogger(ShipperRatesController.class);

	private final ShipperRatesService service;

	public ShipperRatesController(ShipperRatesService service) {
		this.service = service;
	}

//    @Operation(summary = "Create shipper rates")
//    @PostMapping
//    public ResponseEntity<ApiResponse<Object>> create(
//            @Valid @RequestBody ShipperRatesRequestDTO request,
//            @RequestParam(defaultValue = "0") int page,
//            @RequestParam(defaultValue = "4") int size
//    ) {
//
//        log.info("Request create shipper rates : {}", request);
//
//      
//        ShipperRates entity = toEntity(request);
//        ShipperRates saved = service.save(entity);
//
//        //  Reuse existing pagination logic for response
//        Object paginatedResponse = service.getRateByCountryPaginated(
//                saved.getProjectId(),
//                null,   // countryCode not passed → first country auto
//                page,
//                size
//        );
//
//        //Return paginated response
//        return ResponseUtil.okObject(
//                paginatedResponse,
//                "Shipper rates saved successfully"
//        );
//    }

	@Operation(summary = "Create shipper rates")
	@PostMapping
	public ResponseEntity<ApiResponse<Object>> create(@Valid @RequestBody ShipperRatesRequestDTO request,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "4") int size) {

		log.info("Request create shipper rates : {}", request);

		// ✅ Upsert projectId and append/update country inside rates
		ShipperRates saved = service.createOrUpdateCountryWise(request);

		// ✅ You said POST will have only ONE country at a time
		String countryCode = request.getRates() != null && !request.getRates().isEmpty()
				? request.getRates().keySet().iterator().next()
				: null;

		// Reuse existing pagination logic, but for the country that was just
		// added/updated
		Object paginatedResponse = service.getRateByCountryPaginated(saved.getProjectId(), countryCode, page, size);

		return ResponseUtil.okObject(paginatedResponse, "Shipper rates saved successfully");
	}

	@Operation(summary = "Update shipper rates")
	@PutMapping("/{id}")
	public ResponseEntity<ApiResponse<Object>> update(@PathVariable String id,
			@Valid @RequestBody ShipperRatesRequestDTO request) {
		log.info("Request update for id : {} and request {}", id, request);

		ShipperRates entity = toEntity(request);
		ShipperRates updated = service.update(id, entity);

		return ResponseUtil.okObject(toResponseDTO(updated), "Shipper rates updated successfully");
	}
	

	@Operation(summary = "Update shipper rate zone price(s) for a country (bulk updates)")
	@PatchMapping("/country-rate/zone-price")
	public ResponseEntity<ApiResponse<Object>> updateZonePrices(
	        @Valid @RequestBody ShipperRateZonePriceUpdateRequestDTO request
	) {
	
	    log.info("Request update zone prices projectId={}, countryCode={}, items={}",
	            request.getProjectId(),
	            request.getCountryCode(),
	            request.getUpdates() != null ? request.getUpdates().size() : 0
	    );
	
	    service.updateZonePricesBulk(request);
	
	    return ResponseUtil.okEmpty("Zone prices updated successfully");
	}



	@Operation(summary = "Update shipper rates country data using paginated payload (TariffType, ZipCodes, Weights, Prices)")
	@PutMapping("/country-rate/paginated/sync")
	public ResponseEntity<ApiResponse<Object>> syncCountryRateFromPaginatedPayload(
	        @RequestParam String projectId,
	        @RequestBody Map<String, Object> payload
	) {
	    log.info("Request sync country rate from paginated payload projectId={}, keys={}",
	            projectId, payload != null ? payload.keySet() : null);

	    service.syncCountryRateFromPaginatedPayload(projectId, payload);

	    return ResponseUtil.okEmpty("Rates updated successfully");
	}

	@Operation(summary = "Get shipper rates by ID")
	@GetMapping("/{id}")
	public ResponseEntity<ApiResponse<Object>> getShipperRatesById(@PathVariable String id) {
		log.info("Request getShipperRatesById : {}", id);

		return service.findById(id)
				.map(rates -> ResponseUtil.okObject(toResponseDTO(rates), "Shipper rates fetched successfully"))
				// Node-style: 200 OK with {} when missing/invalid
				.orElse(ResponseUtil.okEmpty("OK"));
	}

	@Operation(summary = "Get shipper rates by ProjectId")
	@GetMapping("/projectid")
	public ResponseEntity<ApiResponse<Object>> getShipperRatesByProjectId(@RequestParam String projectId) {
		log.info("Request getShipperRatesById : {}", projectId);

		return service.findByProjectId(projectId)
				.map(rates -> ResponseUtil.okObject(toResponseDTO(rates), "Shipper rates fetched successfully"))
				// Node-style: 200 OK with {} when missing/invalid
				.orElse(ResponseUtil.okEmpty("OK"));
	}

	@Operation(summary = "Delete shipper rates by ID")
	@DeleteMapping("/{id}")
	public ResponseEntity<ApiResponse<Object>> deleteShipperRatesById(@PathVariable String id) {
		log.info("Request deleteShipperRatesById : {}", id);
		service.delete(id);
		// Envelope message carries the deletion message; data = {}
		return ResponseUtil.okEmpty("Shipper rates deleted successfully");
	}

	@Operation(summary = "Get shipper rates countries by projectId")
	@GetMapping("/countries")
	public ResponseEntity<ApiResponse<Object>> getCountriesByProjectId(@RequestParam String projectId) {
		log.info("Request get shipper rate countries projectId={}", projectId);

		var countries = service.getCountriesByProjectId(projectId);

		return ResponseUtil.okObject(countries, "Countries fetched successfully");
	}

	@Operation(summary = "Get shipper rates by projectId and countryCode")
	@GetMapping("/country-rate")
	public ResponseEntity<ApiResponse<Object>> getRateByCountry(@RequestParam String projectId,
			@RequestParam(required = false) String countryCode) {

		log.info("Request get shipper rate projectId={}, countryCode={}", projectId, countryCode);

		Object rate = service.getRateByCountry(projectId, countryCode);

		return ResponseUtil.okObject(rate, "Rate fetched successfully");
	}

	@Operation(summary = "Get shipper rates with pagination by projectId and countryCode")
	@GetMapping("/country-rate/paginated")
	public ResponseEntity<ApiResponse<Object>> getRateByCountryPaginated(@RequestParam String projectId,
			@RequestParam(required = false) String countryCode, @RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "4") int size) {

		log.info("Request get paginated shipper rate projectId={}, countryCode={}, page={}, size={}", projectId,
				countryCode, page, size);

		Object rate = service.getRateByCountryPaginated(projectId, countryCode, page, size);

		return ResponseUtil.okObject(rate, "Rate fetched successfully with pagination");
	}

	@Operation(summary = "Update shipper rates for visible (paginated) items only")
	@PutMapping("/country-rate")
	public ResponseEntity<ApiResponse<Object>> updateRates(@RequestBody ShipperRateUpdateRequestDTO request) {

		log.info("Request update shipper rates projectId={}, countryCode={}, items={}", request.getProjectId(),
				request.getCountryCode(), request.getUpdates() != null ? request.getUpdates().size() : 0);

		service.updateVisibleRates(request);

		return ResponseUtil.okEmpty("Rates updated successfully");
	}

	@Operation(summary = "Create shipper rates (save only)")
	@PostMapping("/save")
	public ResponseEntity<ApiResponse<Object>> saveOnly(@Valid @RequestBody ShipperRatesRequestDTO request) {

		log.info("Request save-only shipper rates : {}", request);

		ShipperRates entity = toEntity(request);
		ShipperRates saved = service.save(entity);

		return ResponseUtil.okObject(Map.of("id", saved.getId()), "Shipper rates saved successfully");
	}

	/* ---------- mapping helpers ---------- */

	private ShipperRates toEntity(ShipperRatesRequestDTO request) {
		return ShipperRates.builder().projectId(request.getProjectId()).rates(request.getRates()).build();
	}

	private ShipperRatesResponseDTO toResponseDTO(ShipperRates entity) {
		return ShipperRatesResponseDTO.builder().id(entity.getId()) // If ObjectId, use entity.getId().toHexString()
				.projectId(entity.getProjectId()).rates(entity.getRates()).build();
	}

}
