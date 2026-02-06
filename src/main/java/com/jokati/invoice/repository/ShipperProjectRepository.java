package com.jokati.invoice.repository;


import java.util.List;
import java.util.Optional;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import com.jokati.invoice.model.ShipperProject;


@Repository
public interface ShipperProjectRepository extends MongoRepository<ShipperProject, ObjectId> {

    // ---------- Preferred: queries by companyId ----------

    /**
     * All projects for a company where active is true or the field is missing/null.
     * Will return an empty list if no matches (it will NOT fail).
     */
    @Query("{ 'userId': ?0, $or: [ { 'active': true }, { 'active': { $exists: false } }, { 'active': null } ] }")
    List<ShipperProject> findActiveOrUnsetByUserId(String userId);

    /**
     * Case-insensitive exact match on project name for a given companyId,
     * but only if active is true or missing/null.
     */
    @Query("{ 'companyId': ?0, 'name': { $regex: '^?1$', $options: 'i' }, $or: [ { 'active': true }, { 'active': { $exists: false } }, { 'active': null } ] }")
    Optional<ShipperProject> findFirstActiveOrUnsetByCompanyIdAndNameIgnoreCase(String companyId, String name);

    /**
     * All case-insensitive exact matches on name for a given companyId,
     * restricted to active true or missing/null.
     */
    @Query("{ 'companyId': ?0, 'name': { $regex: '^?1$', $options: 'i' }, $or: [ { 'active': true }, { 'active': { $exists: false } }, { 'active': null } ] }")
    List<ShipperProject> findActiveOrUnsetByCompanyIdAndNameIgnoreCase(String companyId, String name);



    // ---------- Legacy: if you still need userId temporarily ----------

    /**
     * Legacy lookup with userId; consider migrating callers to companyId.
     */
    List<ShipperProject> findByUserId(String userId);

    /**
     * Legacy case-insensitive name lookup by userId.
     */
    Optional<ShipperProject> findFirstByUserIdAndNameIgnoreCase(String userId, String name);

    List<ShipperProject> findByUserIdAndNameIgnoreCase(String userId, String name);
    
    Optional<ShipperProject> findFirstByCompanyIdAndNameIgnoreCase(String companyId, String name);
    
    @Query("{ 'companyId': ?0, 'name': { $regex: '^?1$', $options: 'i' }, 'active': true }")
    Optional<ShipperProject> findFirstActiveByCompanyIdAndNameIgnoreCase(String companyId, String name);

    @Query("{ 'companyId': ?0, 'name': { $regex: '^?1$', $options: 'i' }, 'active': true }")
    List<ShipperProject> findActiveByCompanyIdAndNameIgnoreCase(String companyId, String name);
}