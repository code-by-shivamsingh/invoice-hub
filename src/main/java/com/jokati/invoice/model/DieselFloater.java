package com.jokati.invoice.model;

import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Diesel floater matrix persisted separately (if you use this collection). Keys
 * may nest years/months → values; importer/service must ensure Mongo-safe keys.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "diesel-floater")
public class DieselFloater {
	@Id
	private String id;

	/** Flexible nested structure: years -> months -> source/value maps, etc. */
	private Map<String, Object> years;
}