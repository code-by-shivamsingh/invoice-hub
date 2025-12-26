
package com.jokati.invoice.service;

import java.util.List;
import java.util.Map;

import org.bson.types.ObjectId;
import org.springframework.dao.DataAccessException;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.jokati.invoice.dto.CarrierOfferingRequestDTO;
import com.jokati.invoice.email.EmailTemplates;
import com.jokati.invoice.exception.ApiException;
import com.jokati.invoice.model.CarrierOffering;
import com.jokati.invoice.repository.CarrierOfferingRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class CarrierOfferingService {

    private final CarrierOfferingRepository repo;
    private final MongoTemplate mongo;
    private final EmailService emailService;
    private final EmailTemplates templates;

    public CarrierOfferingService(CarrierOfferingRepository repo,
                                  MongoTemplate mongo,
                                  EmailService emailService,
                                  EmailTemplates templates) {
        this.repo = repo;
        this.mongo = mongo;
        this.emailService = emailService;
        this.templates = templates;
    }

    public List<CarrierOffering> findByProjectId(String projectId) {
        return repo.findByProjectId(projectId);
    }

    /**
     * Mirrors Node: findOneAndUpdate({ carrierProjectId }, { ...payload, _id: id }, { upsert: true, returnNew: true })
     */
    public CarrierOffering upsertByCarrierProjectId(CarrierOfferingRequestDTO request) {
        final String subject = "Neues Angebot zur Ihrer Frachtausschreibung";

        String carrierProjectId = request.getCarrierProjectId();
        if (carrierProjectId == null || carrierProjectId.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_ARGUMENT", "carrierProjectId is required");
        }

        // Build query by carrierProjectId
        Query query = new Query(Criteria.where("carrierProjectId").is(carrierProjectId));

        // Build update from payload (spread)
        Update update = new Update()
                .set("carrierProjectId", carrierProjectId)
                .set("projectId", request.getProjectId())
                .set("shipperEmail", request.getShipperEmail());

        if (request.getCompanyProfile() != null) update.set("companyProfile", request.getCompanyProfile());
        if (request.getPayload() != null) update.set("payload", request.getPayload());
        if (request.getFreightCalculationBasis() != null) update.set("freightCalculationBasis", request.getFreightCalculationBasis());
        if (request.getExtraCosts() != null) update.set("extraCosts", request.getExtraCosts());
        if (request.getRates() != null) update.set("rates", request.getRates());

        // Set _id on insert (ObjectId derived from carrierProjectId if valid hex)
        ObjectId idToInsert = toObjectIdOrNull(carrierProjectId);
        if (idToInsert != null) {
            update.setOnInsert("_id", idToInsert);
        } else {
            // Fallback: let Mongo generate _id
            update.setOnInsert("_id", new ObjectId());
            log.warn("carrierProjectId '{}' is not a valid ObjectId hex; inserting with a generated _id.", carrierProjectId);
        }

        FindAndModifyOptions options = FindAndModifyOptions.options().upsert(true).returnNew(true);

        try {
            CarrierOffering updated = mongo.findAndModify(query, update, options, CarrierOffering.class, "carrier-offering");
            if (updated == null) {
                // Very unlikely; upsert should return the new doc. Fallback to re-read.
                updated = mongo.findOne(query, CarrierOffering.class, "carrier-offering");
            }

            // Send email (if company name present)
            String companyName = null;
            Map<String, Object> profile = updated.getCompanyProfile();
            if (profile != null) {
                Object c = profile.get("company");
                if (c instanceof String s && !s.isBlank()) companyName = s;
            }
            if (companyName != null && updated.getShipperEmail() != null && !updated.getShipperEmail().isBlank()) {
                String html = templates.angebotErhaltenTemplate(companyName);
				emailService.sendEmail(updated.getShipperEmail(), subject, html);
            }

            return updated;
        } catch (DataAccessException ex) {
            log.error("POST/PUT carrier offering server error", ex);
            throw ex; // will be handled by GlobalExceptionHandler
        }
    }

    public void deleteById(String id) {
        repo.deleteById(id);
    }

    private static ObjectId toObjectIdOrNull(String hex) {
        try {
            return (hex != null && hex.length() == 24) ? new ObjectId(hex) : null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
