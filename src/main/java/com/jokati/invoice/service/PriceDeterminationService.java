
package com.jokati.invoice.service;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;

import com.jokati.invoice.dto.PriceDeterminationResponseDTO;
import com.jokati.invoice.model.CarrierOffering;
import com.jokati.invoice.model.DieselFloater;
import com.jokati.invoice.model.ShipperExtraCosts;
import com.jokati.invoice.model.ShipperFreightCalculationBasis;
import com.jokati.invoice.model.ShipperProject;
import com.jokati.invoice.model.ShipperRates;
import com.jokati.invoice.repository.CarrierOfferingRepository;
import com.jokati.invoice.repository.DieselFloaterRepository;
import com.jokati.invoice.repository.ShipperExtraCostsRepository;
import com.jokati.invoice.repository.ShipperFreightCalculationBasisRepository;
import com.jokati.invoice.repository.ShipperProjectRepository;
import com.jokati.invoice.repository.ShipperRatesRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PriceDeterminationService {

    private final ShipperProjectRepository shipperProjectsRepo;
    private final ShipperFreightCalculationBasisRepository sFreightBasisRepo;
    private final ShipperRatesRepository shipperRatesRepo;
    private final ShipperExtraCostsRepository shipperExtraCostsRepo;
    private final CarrierOfferingRepository carrierOfferingRepo;
    private final DieselFloaterRepository dieselFloaterRepo;

    public Optional<PriceDeterminationResponseDTO> aggregateByProjectId(String projectId) {
        ObjectId oid;
        try {
            oid = new ObjectId(projectId);
        } catch (IllegalArgumentException e) {
            // invalid ObjectId format → mirror Node by returning empty/undefined
            return Optional.empty();
        }

        Optional<ShipperProject> shipperProjectOpt = shipperProjectsRepo.findById(oid);
        Optional<ShipperFreightCalculationBasis> freightBasisOpt = sFreightBasisRepo.findById(oid);
        Optional<ShipperRates> shipperRatesOpt = shipperRatesRepo.findById(projectId);
        Optional<ShipperExtraCosts> shipperExtraCostsOpt = shipperExtraCostsRepo.findById(projectId);

        List<CarrierOffering> carrierOfferings = carrierOfferingRepo.findByProjectId(projectId);
        List<DieselFloater> dieselFloaterData = dieselFloaterRepo.findAll();

        List<Map<String, Object>> carrierData = prepareCarrierOfferings(carrierOfferings);

        Map<String, Object> dieselFloaterMatrix = dieselFloaterData.isEmpty()
                ? Collections.emptyMap()
                : safeCastToMap(dieselFloaterData.get(0).getYears());

        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("projectName", shipperProjectOpt.map(ShipperProject::getName).orElse(null));

        Map<String, Object> freightCalculationBasis = freightBasisOpt
                .map(ShipperFreightCalculationBasis::getCountries)
                .map(this::safeCastToMap)
                .orElse(Collections.emptyMap());

        Map<String, Object> rates = shipperRatesOpt
                .map(ShipperRates::getRates)
                .map(this::safeCastToMap)
                .orElse(Collections.emptyMap());

        Map<String, Object> extraCosts = shipperExtraCostsOpt
                .map(ShipperExtraCosts::getExtraCosts)
                .map(this::safeCastToMap)
                .orElse(Collections.emptyMap());

        PriceDeterminationResponseDTO docs = PriceDeterminationResponseDTO.builder()
                .meta(meta)
                .freightCalculationBasis(freightCalculationBasis)
                .rates(rates)
                .extraCosts(extraCosts)
                .carrierData(carrierData)
                .dieselFloaterMatrix(dieselFloaterMatrix)
                .build();

        return Optional.of(docs);
    }

    /* ---------------- helpers ---------------- */

    private List<Map<String, Object>> prepareCarrierOfferings(List<CarrierOffering> carrierOfferings) {
        return carrierOfferings.stream()
                .map(off -> {
                    Map<String, Object> companyProfile = off.getCompanyProfile() != null
                            ? off.getCompanyProfile() : Collections.emptyMap();

                    String email = String.valueOf(companyProfile.getOrDefault("email", ""));
                    String company = String.valueOf(companyProfile.getOrDefault("company", ""));
                    String firstName = String.valueOf(companyProfile.getOrDefault("firstName", ""));
                    String lastName = String.valueOf(companyProfile.getOrDefault("lastName", ""));

                    Map<String, Object> payload = off.getPayload() != null ? off.getPayload() : Collections.emptyMap();

                    Map<String, Object> freightCalculationBasis = extractMap(off.getFreightCalculationBasis(),
                            payload.get("freightCalculationBasis"));
                    Map<String, Object> extraCosts = extractMap(off.getExtraCosts(),
                            payload.get("extraCosts"));
                    Map<String, Object> rates = extractMap(off.getRates(),
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
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> safeCastToMap(Object obj) {
        if (obj instanceof Map<?, ?> m) {
            return (Map<String, Object>) m;
        }
        return Collections.emptyMap();
    }

    @SafeVarargs
    private final Map<String, Object> extractMap(Object... candidates) {
        for (Object o : candidates) {
            if (o instanceof Map<?, ?> m) {
                return safeCastToMap(m);
            }
        }
        return Collections.emptyMap();
    }
}
