
package com.jokati.invoice.controller;

import com.jokati.invoice.dto.PriceDeterminationResponseDTO;
import com.jokati.invoice.model.CarrierOffering;
import com.jokati.invoice.model.DieselFloater;
import com.jokati.invoice.model.ShipperProjects;
import com.jokati.invoice.model.ShipperExtraCosts;
import com.jokati.invoice.model.ShipperRates;
import com.jokati.invoice.model.ShipperFreightCalculationBasis;
import com.jokati.invoice.repository.CarrierOfferingRepository;
import com.jokati.invoice.repository.DieselFloaterRepository;
import com.jokati.invoice.repository.ShipperProjectsRepository;
import com.jokati.invoice.repository.ShipperExtraCostsRepository;
import com.jokati.invoice.repository.ShipperRatesRepository;
import com.jokati.invoice.repository.ShipperFreightCalculationBasisRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/preisermittlung")
@Tag(name = "Preisermittlung API", description = "Aggregate price determination data for a project")
public class PriceDeterminationController {

    private final ShipperProjectsRepository shipperProjectsRepo;
    private final ShipperFreightCalculationBasisRepository sFreightBasisRepo;
    private final ShipperRatesRepository shipperRatesRepo;
    private final ShipperExtraCostsRepository shipperExtraCostsRepo;
    private final CarrierOfferingRepository carrierOfferingRepo;
    private final DieselFloaterRepository dieselFloaterRepo;

    public PriceDeterminationController(ShipperProjectsRepository shipperProjectsRepo,
                                        ShipperFreightCalculationBasisRepository sFreightBasisRepo,
                                        ShipperRatesRepository shipperRatesRepo,
                                        ShipperExtraCostsRepository shipperExtraCostsRepo,
                                        CarrierOfferingRepository carrierOfferingRepo,
                                        DieselFloaterRepository dieselFloaterRepo) {
        this.shipperProjectsRepo = shipperProjectsRepo;
        this.sFreightBasisRepo = sFreightBasisRepo;
        this.shipperRatesRepo = shipperRatesRepo;
        this.shipperExtraCostsRepo = shipperExtraCostsRepo;
        this.carrierOfferingRepo = carrierOfferingRepo;
        this.dieselFloaterRepo = dieselFloaterRepo;
    }

    @Operation(summary = "Get aggregated documents for price determination by projectId")
    @GetMapping
    public ResponseEntity<?> get(@RequestParam(required = false) String projectId) {
        try {
            if (projectId == null || projectId.isBlank() || "null".equals(projectId)) {
                // Mirror Node semantics by returning undefined/empty
                return ResponseEntity.ok().build();
            }

            // Load base docs
            Optional<ShipperProjects> shipperProjectOpt = shipperProjectsRepo.findById(projectId);
            Optional<ShipperFreightCalculationBasis> freightBasisOpt = sFreightBasisRepo.findById(projectId);
            Optional<ShipperRates> shipperRatesOpt = shipperRatesRepo.findById(projectId);
            Optional<ShipperExtraCosts> shipperExtraCostsOpt = shipperExtraCostsRepo.findById(projectId);
            List<CarrierOffering> carrierOfferings = carrierOfferingRepo.findByProjectId(projectId);
            List<DieselFloater> dieselFloaterData = dieselFloaterRepo.findAll();

            // Prepare carrier offerings (similar to Node's prepareCarrierOfferings)
            List<Map<String, Object>> carrierData = carrierOfferings.stream()
                    .map(off -> {
                        Map<String, Object> companyProfile = off.getCompanyProfile() != null
                                ? off.getCompanyProfile() : Collections.emptyMap();

                        String email = String.valueOf(companyProfile.getOrDefault("email", ""));
                        String company = String.valueOf(companyProfile.getOrDefault("company", ""));
                        String firstName = String.valueOf(companyProfile.getOrDefault("firstName", ""));
                        String lastName = String.valueOf(companyProfile.getOrDefault("lastName", ""));

                        Map<String, Object> payload = off.getPayload() != null ? off.getPayload() : Collections.emptyMap();

                        // offering.freightCalculationBasis / extraCosts / rates may be present either as explicit fields
                        // or inside payload (Mongoose strict:false). Check both.
                        Map<String, Object> freightCalculationBasis = extractMap(offField(off, "freightCalculationBasis"),
                                payload.get("freightCalculationBasis"));
                        Map<String, Object> extraCosts = extractMap(offField(off, "extraCosts"),
                                payload.get("extraCosts"));
                        Map<String, Object> rates = extractMap(offField(off, "rates"),
                                payload.get("rates"));

                        // Filter out non-object entries in rates (Node deletes primitives)
                        Map<String, Object> filteredRates = rates.entrySet().stream()
                                .filter(e -> e.getValue() instanceof Map)
                                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

                        Map<String, Object> carrierProfile = new LinkedHashMap<>();
                        carrierProfile.put("email", email);
                        carrierProfile.put("company", company);
                        carrierProfile.put("firstName", firstName);
                        carrierProfile.put("lastName", lastName);

                        Map<String, Object> prepared = new LinkedHashMap<>();
                        prepared.put("freightCalculationBasis", freightCalculationBasis);
                        prepared.put("extraCosts", extraCosts);
                        prepared.put("rates", filteredRates);
                        prepared.put("carrierProfile", carrierProfile);

                        return prepared;
                    })
                    .collect(Collectors.toList());

            Map<String, Object> dieselFloaterMatrix = dieselFloaterData.isEmpty()
                    ? Collections.emptyMap()
                    : (Map<String, Object>) dieselFloaterData.get(0).getYears();

            Map<String, Object> meta = new LinkedHashMap<>();
            meta.put("projectName", shipperProjectOpt.map(ShipperProjects::getName).orElse(null));

            Map<String, Object> freightCalculationBasis = freightBasisOpt
                    .map(ShipperFreightCalculationBasis::getCountries)
                    .orElse(Collections.emptyMap());

            Map<String, Object> rates = shipperRatesOpt
                    .map(ShipperRates::getRates)
                    .orElse(Collections.emptyMap());

            Map<String, Object> extraCosts = shipperExtraCostsOpt
                    .map(ShipperExtraCosts::getExtraCosts)
                    .orElse(Collections.emptyMap());

            PriceDeterminationResponseDTO docs = PriceDeterminationResponseDTO.builder()
                    .meta(meta)
                    .freightCalculationBasis(freightCalculationBasis)
                    .rates(rates)
                    .extraCosts(extraCosts)
                    .carrierData(carrierData)
                    .dieselFloaterMatrix(dieselFloaterMatrix)
                    .build();

            // Node checks if (!documents) then returns undefined; here docs is always non-null.
            return ResponseEntity.ok(docs);
        } catch (Exception e) {
            // Node returns 500 with undefined
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Helper: try to read explicit field from CarrierOffering via reflection-safe getter map,
     * else return null. We keep it flexible to avoid changing your existing CarrierOffering model.
     */
    private Object offField(CarrierOffering off, String key) {
        // If you later add explicit fields (e.g., Map<String,Object> rates) in CarrierOffering,
        // you can access them directly here. For now, return null so payload fallback is used.
        return null;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractMap(Object primary, Object fallback) {
        if (primary instanceof Map<?, ?> m) {
            return (Map<String, Object>) m;
        }
        if (fallback instanceof Map<?, ?> m) {
            return (Map<String, Object>) m;
        }
        return Collections.emptyMap();
    }
}