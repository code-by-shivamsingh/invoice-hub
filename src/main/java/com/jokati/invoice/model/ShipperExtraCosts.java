package com.jokati.invoice.model;

import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Root document for shipper extra-costs (country-wise and flexible payload).
 * The service ensures Mongo-safe map keys before persisting.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "shipper-extra-costs")
public class ShipperExtraCosts {
	@Id
	private String id;

	private String projectId;

	/**
	 * Flexible structure; may contain nested objects/lists (e.g., DieselFloater
	 * config, tiers).
	 */
	private Map<String, Object> extraCosts;
}