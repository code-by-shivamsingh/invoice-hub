package com.jokati.invoice.util;

import com.jokati.invoice.dto.ChargesDTO;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

public final class ChargesZeroExtrasPruner {

    private static final Set<String> DEFAULT_EXCLUDES = Set.of("currency");

    // Cache BigDecimal fields of ChargesDTO except excluded ones
    private static final List<Field> BIGDECIMAL_FIELDS;

    static {
        BIGDECIMAL_FIELDS = Arrays.stream(ChargesDTO.class.getDeclaredFields())
                .filter(f -> f.getType() == BigDecimal.class)
                .filter(f -> !DEFAULT_EXCLUDES.contains(f.getName()))
                .peek(f -> f.setAccessible(true))
                .collect(Collectors.toUnmodifiableList());
    }

    private ChargesZeroExtrasPruner() {}

    /**
     * If enabled = true:
     *   For each BigDecimal field present in ChargesDTO:
     *     If both charges.<field> and orderCharges.<field> are zero (or null), set them to null.
     *
     * If enabled = false: no-op.
     *
     * @param charges       Shipment charges
     * @param orderCharges  Shipment order charges
     * @param enabled       Feature toggle
     */
    public static void pruneCommonZeroFields(ChargesDTO charges, ChargesDTO orderCharges, boolean enabled) {
        if (!enabled || charges == null || orderCharges == null) return;

        for (Field f : BIGDECIMAL_FIELDS) {
            try {
                BigDecimal v1 = (BigDecimal) f.get(charges);
                BigDecimal v2 = (BigDecimal) f.get(orderCharges);

                if (isZero(v1) && isZero(v2)) {
                    f.set(charges, null);
                    f.set(orderCharges, null);
                }
            } catch (IllegalAccessException ignore) {
                // Optionally log
            }
        }
    }

    private static boolean isZero(BigDecimal bd) {
        // Treat null as zero for this rule
        return bd == null || bd.compareTo(BigDecimal.ZERO) == 0;
    }
}