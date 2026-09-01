package com.bookmall.aftersale.ai.tool;

import java.util.Map;

final class ToolArgumentReader {

    private ToolArgumentReader() {
    }

    static Long longValue(Map<String, Object> arguments, String key) {
        Object value = arguments.get(key);
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return Long.valueOf(text);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    static String stringValue(Map<String, Object> arguments, String key) {
        Object value = arguments.get(key);
        return value == null ? null : String.valueOf(value);
    }
}
