
package com.jokati.invoice.controller;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jokati.invoice.common.ApiResponse;
import com.jokati.invoice.common.ResponseUtil;
import com.jokati.invoice.dto.ShipperFreightCalculationBasisRequestDTO;
import com.jokati.invoice.dto.ShipperFreightCalculationBasisResponseDTO;
import com.jokati.invoice.model.ShipperFreightCalculationBasis;
import com.jokati.invoice.service.ShipperFreightCalculationBasisService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/shipper-freight-calculation-basis")
@Tag(name = "Shipper Freight Calculation Basis API", description = "Manage freight calculation basis")
public class ShipperFreightCalculationBasisController {
	private static final Logger log = LoggerFactory.getLogger(ShipperFreightCalculationBasisController.class);

	private final ShipperFreightCalculationBasisService service;

	public ShipperFreightCalculationBasisController(ShipperFreightCalculationBasisService service) {
		this.service = service;
	}

	@Operation(summary = "Get freight calculation basis by ID")
	@GetMapping("/projectid")
	public ResponseEntity<ApiResponse<Object>> getByProjectId(@RequestParam String projectId) {
	    log.info("Request get : {}", projectId);
	    return service.findByProjectId(projectId)
	            .map(basis ->
	                    ResponseUtil.okObject(
	                            toResponseDTO(basis),
	                            "Freight calculation basis fetched successfully"
	                    )
	            )
	            .orElse(ResponseUtil.okEmpty("OK"));
	}


	@Operation(summary = "Create freight calculation basis")
	@PostMapping
	public ResponseEntity<ApiResponse<Object>> create(
			@Valid @RequestBody ShipperFreightCalculationBasisRequestDTO request) {
		log.info("Request create : {}", request);

		var entity = service.fromRequestDTO(request);
		var saved = service.save(entity);

		return ResponseUtil.okObject(toResponseDTO(saved), "Freight calculation basis saved successfully");
	}

	@Operation(summary = "Update freight calculation basis")
	@PutMapping("/{id}")
	public ResponseEntity<ApiResponse<Object>> update(@PathVariable String id,
			@Valid @RequestBody ShipperFreightCalculationBasisRequestDTO request) {
		log.info("Request update by id : {}, request {}", id, request);

		var entity = service.fromRequestDTO(request);
		var updated = service.update(id, entity);

		return ResponseUtil.okObject(toResponseDTO(updated), "Freight calculation basis updated successfully");
	}

	@Operation(summary = "Delete freight calculation basis by ID")
	@DeleteMapping("/{id}")
	public ResponseEntity<ApiResponse<Object>> delete(@PathVariable String id) {
		log.info("Request delete : {}", id);
		service.delete(id);
		return ResponseUtil.okEmpty("Freight calculation basis deleted successfully");
	}
	
	@Operation(summary = "Get freight calculation countries by projectId")
	@GetMapping("/countries")
	public ResponseEntity<ApiResponse<Object>> getCountriesByProjectId(
	        @RequestParam String projectId) {

	    log.info("Request get countries projectId={}", projectId);

	    List<String> countries = service.getCountriesByProjectId(projectId);

	    return ResponseUtil.okObject(
	            Map.of(
	                "projectId", projectId,
	                "countries", countries
	            ),
	            "Countries fetched successfully"
	    );
	}

	
	@Operation(summary = "Get freight calculation basis by projectId and countryCode")
	@GetMapping("/country-basis")
	public ResponseEntity<ApiResponse<Object>> getBasisByCountry(
	        @RequestParam String projectId,
	        @RequestParam(required = false) String countryCode) {

	    Object data = service.getBasisByCountry(projectId, countryCode);

	    if (data == null) {
	        return ResponseUtil.okEmpty("No freight basis found");
	    }

	   
	    return ResponseUtil.okObject(
	            data,
	            "Freight basis fetched successfully"
	    );
	}


	/* ---------- mapping helper ---------- */

	private ShipperFreightCalculationBasisResponseDTO toResponseDTO(ShipperFreightCalculationBasis entity) {
		return ShipperFreightCalculationBasisResponseDTO.builder()
				.id(entity.getId() != null ? entity.getId().toHexString() : null).projectId(entity.getProjectId())
				.carrierProjectId(entity.getCarrierProjectId()).countries(entity.getCountries())
				.firebaseId(entity.getFirebaseId()).extra(entity.getExtra()).createdAt(entity.getCreatedAt())
				.updatedAt(entity.getUpdatedAt()).build();
	}
}
