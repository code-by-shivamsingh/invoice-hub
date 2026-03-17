package com.jokati.invoice.util;

import com.jokati.invoice.dto.ChargesDTO;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Positive deviation shipper policy:
 * If enforcePositiveOnly == true:
 *  - Keep only Charges fields where (charges - orderCharges) > 0
 *  - Shipment: difference <- positiveDifference; orderSurchargeTotal <- orderPositiveSurchargeTotal; orderTotal = null
 *  - Invoice:  invoiceDifference <- positiveInvoiceDifference; orderTotal = null
 *
 * The remap uses multiple fallbacks: getter->setter, field->setter, field->field.
 * No POJO changes required.
 */
public final class ShipperPositiveDeviationPolicy {

    private ShipperPositiveDeviationPolicy() {}

    // Cache ChargesDTO BigDecimal fields (exclude "currency")
    private static final List<Field> CHARGES_FIELDS = Arrays.stream(ChargesDTO.class.getDeclaredFields())
            .filter(f -> f.getType() == BigDecimal.class)
            .filter(f -> !"currency".equals(f.getName()))
            .peek(f -> f.setAccessible(true))
            .collect(Collectors.toUnmodifiableList());

    /** Call this with the already-computed flag. */
    public static void apply(Object invoiceResponseDto, boolean enforcePositiveOnly) {
        if (invoiceResponseDto == null || !enforcePositiveOnly) return;

        // 1) Per-shipment charges filtering and remaps
        List<?> shipments = getShipments(invoiceResponseDto);
        if (shipments != null) {
            for (Object s : shipments) {
                ChargesDTO charges = getCharges(s);
                ChargesDTO orderCharges = getOrderCharges(s);
                keepOnlyPositiveDelta(charges, orderCharges);

                // Shipment: difference <- positiveDifference
                copyValue(
                        s,
                        // try these getters on the shipment response/entity
                        List.of("getPositiveDifference", "isPositiveDifference"),
                        // or directly read the field if no getter
                        List.of("positiveDifference"),
                        // write into these setters first
                        List.of("setDifference"),
                        // fallback to direct field write
                        List.of("difference")
                );

                // Shipment: orderSurchargeTotal <- orderPositiveSurchargeTotal
                copyValue(
                        s,
                        List.of("getOrderPositiveSurchargeTotal", "isOrderPositiveSurchargeTotal"),
                        List.of("orderPositiveSurchargeTotal"),
                        List.of("setOrderSurchargeTotal"),
                        List.of("orderSurchargeTotal")
                );

                // Shipment: orderTotal = null
                nullifyOrderTotal(s);
            }
        }

        // 2) Invoice-level remap: invoiceDifference <- positiveInvoiceDifference
        copyValue(
                invoiceResponseDto,
                List.of("getPositiveInvoiceDifference", "isPositiveInvoiceDifference"),
                List.of("positiveInvoiceDifference"),
                List.of("setInvoiceDifference"),
                List.of("invoiceDifference")
        );

        // Invoice: orderTotal = null
        nullifyOrderTotal(invoiceResponseDto);
    }

    // ---------- Keep only positive deltas on Charges ----------
    private static void keepOnlyPositiveDelta(ChargesDTO charges, ChargesDTO orderCharges) {
        if (charges == null || orderCharges == null) return;

        for (Field f : CHARGES_FIELDS) {
            try {
                BigDecimal c = (BigDecimal) f.get(charges);
                BigDecimal o = (BigDecimal) f.get(orderCharges);

                BigDecimal cSafe = (c != null) ? c : BigDecimal.ZERO;
                BigDecimal oSafe = (o != null) ? o : BigDecimal.ZERO;

                BigDecimal delta = cSafe.subtract(oSafe);
                if (delta.compareTo(BigDecimal.ZERO) <= 0) {
                    // hide from both sides
                    f.set(charges, null);
                    f.set(orderCharges, null);
                }
            } catch (IllegalAccessException ignored) {}
        }
    }

    // ---------- Copy helper with multiple fallbacks ----------
    private static void copyValue(Object target,
                                  List<String> sourceGetterNames,
                                  List<String> sourceFieldNames,
                                  List<String> targetSetterNames,
                                  List<String> targetFieldNames) {
        boolean sourcePresent = false;
        Object val = null;

        // 1) Try getters
        for (String g : sourceGetterNames) {
            Object v = invokeGetter(target, g);
            if (v != Missing.INSTANCE) {
                sourcePresent = true;
                val = v; // could be null (we still copy null)
                break;
            }
        }

        // 2) Try direct field if no getter
        if (!sourcePresent) {
            for (String fn : sourceFieldNames) {
                if (hasField(target, fn)) {
                    sourcePresent = true;
                    val = readField(target, fn); // may be null
                    break;
                }
            }
        }

        if (!sourcePresent) {
            // nothing to copy
            return;
        }

        // 3) Try setters with type coercion
        for (String s : targetSetterNames) {
            if (tryCoercedSetter(target, s, val)) return;
        }

        // 4) Fallback to direct field assignment
        for (String fn : targetFieldNames) {
            if (writeField(target, fn, val)) return;
        }
        // If nothing matched, silently skip
    }

    private static Object invokeGetter(Object target, String getterName) {
        try {
            Method m = target.getClass().getMethod(getterName);
            return m.invoke(target);
        } catch (NoSuchMethodException e) {
            return Missing.INSTANCE;
        } catch (Exception ignored) {
            return Missing.INSTANCE;
        }
    }

    private static boolean tryCoercedSetter(Object target, String setterName, Object value) {
        // Common numeric types for monetary totals
        if (invokeSetter(target, setterName, coerce(value, BigDecimal.class), BigDecimal.class)) return true;
        if (invokeSetter(target, setterName, coerce(value, Double.class), Double.class)) return true;
        if (invokeSetter(target, setterName, coerce(value, double.class), double.class)) return true;
        return false;
    }

    private static boolean invokeSetter(Object target, String setterName, Object coercedValue, Class<?> paramType) {
        try {
            Method m = target.getClass().getMethod(setterName, paramType);
            if (paramType.isPrimitive() && coercedValue == null) {
                // safe default for primitive
                if (paramType == double.class) m.invoke(target, 0.0d);
                else return false;
            } else {
                m.invoke(target, coercedValue);
            }
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static Object coerce(Object v, Class<?> to) {
        if (v == null) return null;
        if (to.isInstance(v)) return v;

        if (to == BigDecimal.class) {
            if (v instanceof BigDecimal bd) return bd;
            if (v instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
            try { return new BigDecimal(v.toString()); } catch (Exception ignored) { return null; }
        }
        if (to == Double.class || to == double.class) {
            if (v instanceof Number n) return n.doubleValue();
            try { return Double.parseDouble(v.toString()); } catch (Exception ignored) { return null; }
        }
        return null;
    }

    private static boolean hasField(Object target, String fieldName) {
        return getField(target.getClass(), fieldName) != null;
    }

    private static Object readField(Object target, String fieldName) {
        Field f = getField(target.getClass(), fieldName);
        if (f == null) return null;
        try {
            f.setAccessible(true);
            return f.get(target);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static boolean writeField(Object target, String fieldName, Object value) {
        Field f = getField(target.getClass(), fieldName);
        if (f == null) return false;
        try {
            f.setAccessible(true);
            Class<?> t = f.getType();
            Object coerced = coerce(value, t == double.class ? double.class : t);
            if (t.isPrimitive() && coerced == null) {
                if (t == double.class) coerced = 0.0d;
                else return false;
            }
            f.set(target, coerced);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static Field getField(Class<?> c, String name) {
        Class<?> cur = c;
        while (cur != null && cur != Object.class) {
            try { return cur.getDeclaredField(name); }
            catch (NoSuchFieldException ignored) { cur = cur.getSuperclass(); }
        }
        return null;
    }

    // ---------- Accessors ----------
    @SuppressWarnings("unchecked")
    private static List<?> getShipments(Object invoiceDto) {
        try {
            Method m = invoiceDto.getClass().getMethod("getShipments");
            Object v = m.invoke(invoiceDto);
            if (v instanceof List<?>) return (List<?>) v;
        } catch (Exception ignored) {}
        return null;
    }

    private static ChargesDTO getCharges(Object shipmentDto) {
        try {
            Method m = shipmentDto.getClass().getMethod("getCharges");
            Object v = m.invoke(shipmentDto);
            return (v instanceof ChargesDTO) ? (ChargesDTO) v : null;
        } catch (Exception ignore) { return null; }
    }

    private static ChargesDTO getOrderCharges(Object shipmentDto) {
        try {
            Method m = shipmentDto.getClass().getMethod("getOrderCharges");
            Object v = m.invoke(shipmentDto);
            return (v instanceof ChargesDTO) ? (ChargesDTO) v : null;
        } catch (Exception ignore) { return null; }
    }

    private static void nullifyOrderTotal(Object dto) {
        // Try setter (BigDecimal/Double/double), then field
        if (tryCoercedSetter(dto, "setOrderTotal", null)) return;
        writeField(dto, "orderTotal", null);
    }

    private enum Missing { INSTANCE }
}