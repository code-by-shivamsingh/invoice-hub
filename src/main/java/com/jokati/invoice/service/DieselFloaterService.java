
package com.jokati.invoice.service;

import com.jokati.invoice.model.DieselFloater;
import com.jokati.invoice.repository.DieselFloaterRepository;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * DieselFloaterService
 *
 * Provides CRUD operations and safe extraction utilities for Diesel Floater data.
 * The payload (years) is flexible and mirrors the Mongo document you provided.
 */
@Service
@Slf4j
public class DieselFloaterService {

    private final DieselFloaterRepository repository;

    public DieselFloaterService(DieselFloaterRepository repository) {
        this.repository = repository;
    }

    /**
     * Create new DieselFloater document.
     * If 'id' is null, Mongo will generate one automatically.
     */
    @Transactional
    public DieselFloater save(DieselFloater dieselFloater) {
        log.info("DieselFloaterService.save: creating new diesel floater entry");
        DieselFloater saved = repository.save(dieselFloater);
        log.info("DieselFloaterService.save: created with id={}", saved.getId());
        return saved;
    }

    /**
     * Overwrite existing DieselFloater document (upsert-like update).
     * Caller must pass the id to update.
     */
    @Transactional
    public DieselFloater update(String id, DieselFloater dieselFloater) {
        log.info("DieselFloaterService.update: updating id={}", id);
        dieselFloater.setId(id);
        DieselFloater saved = repository.save(dieselFloater);
        log.info("DieselFloaterService.update: updated id={}", saved.getId());
        return saved;
    }

    /**
     * Fetch all DieselFloater documents.
     * Typically you keep only one document in this collection.
     */
    public List<DieselFloater> findAll() {
        List<DieselFloater> all = repository.findAll();
        log.debug("DieselFloaterService.findAll: found {} documents", all.size());
        return all;
    }

    /**
     * Convenience: Pick the latest DieselFloater by ObjectId timestamp.
     * If parsing fails or list is empty, returns Optional.empty().
     */
    public Optional<DieselFloater> findLatest() {
        List<DieselFloater> all = findAll();
        if (all == null || all.isEmpty()) {
            log.warn("DieselFloaterService.findLatest: no documents found");
            return Optional.empty();
        }

        DieselFloater latest = all.stream()
                .max(Comparator.comparingInt(df -> safeObjectIdTimestamp(df.getId())))
                .orElse(all.get(0));

        log.info("DieselFloaterService.findLatest: picked id={} (latest by ObjectId timestamp)", latest.getId());
        return Optional.of(latest);
    }

    /**
     * Extract the "years" map from a DieselFloater document.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> extractYears(DieselFloater dieselFloater) {
        Map<String, Object> years = dieselFloater.getYears();
        return years != null ? years : new HashMap<>();
    }

    /**
     * Provide a list of available sources from the first year entry.
     * Source keys exclude "_id".
     *
     * NOTE: The assumption is the 'years' map contains { "YYYY": [ {sourceKey:value, ...}, ... ] }.
     */
    @SuppressWarnings("unchecked")
    public List<String> extractSources(Map<String, Object> years) {
        if (years == null || years.isEmpty()) {
            log.warn("DieselFloaterService.extractSources: empty years payload");
            return Collections.emptyList();
        }

        // Pick the earliest or first year key deterministically
        String firstYear = years.keySet().stream()
                .sorted()
                .findFirst()
                .orElse(null);

        if (firstYear == null) {
            log.warn("DieselFloaterService.extractSources: no year key found");
            return Collections.emptyList();
        }

        Object yearDataObj = years.get(firstYear);
        if (!(yearDataObj instanceof List<?> yearDataList) || yearDataList.isEmpty()) {
            log.warn("DieselFloaterService.extractSources: years[{}] is not a non-empty List", firstYear);
            return Collections.emptyList();
        }

        Object firstEntryObj = yearDataList.get(0);
        if (!(firstEntryObj instanceof Map<?, ?> firstEntry)) {
            log.warn("DieselFloaterService.extractSources: years[{}][0] is not a Map", firstYear);
            return Collections.emptyList();
        }

        // Extract keys and filter out Mongo "_id"
        List<String> sources = firstEntry.keySet().stream()
                .map(String::valueOf)
                .filter(k -> !"_id".equals(k))
                .collect(Collectors.toList());

        log.info("DieselFloaterService.extractSources: year={} sources={}", firstYear, sources);
        return sources;
    }

    /** Safely extract ObjectId timestamp from a String id; fallback to 0 if not a valid ObjectId. */
    private int safeObjectIdTimestamp(String id) {
        try {
            return new ObjectId(id).getTimestamp(); // seconds since epoch
        } catch (IllegalArgumentException e) {
            log.debug("safeObjectIdTimestamp: invalid ObjectId id={}, default=0", id);
            return 0;
        }
    }
}
