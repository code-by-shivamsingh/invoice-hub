package com.jokati.invoice.util;

public final class TextSanitizer {

    private TextSanitizer() {}

    /**
     * Normalizes IDs by removing:
     * - leading/trailing Unicode whitespace
     * - ALL whitespace inside string (\n, \r, \t, spaces)
     * - NBSP (non-breaking space)
     *
     * Good for shipmentId/projectId-like identifiers.
     */
    public static String normalizeId(String value) {
        if (value == null) return null;

        String s;
        try {
            s = value.strip(); // Java 11+
        } catch (Throwable t) {
            s = value.trim();  // fallback
        }

        // remove NBSP
        s = s.replace("\u00A0", "");

        // remove ALL whitespace: spaces, tabs, newlines, etc.
        s = s.replaceAll("\\s+", "");

        return s.isEmpty() ? null : s;
    }

    /**
     * Only trims Unicode whitespace at both ends. Keeps internal spaces.
     */
    public static String trimUnicode(String value) {
        if (value == null) return null;
        try {
            return value.strip();
        } catch (Throwable t) {
            return value.trim();
        }
    }
}