package com.hotelreservation.util;

public class LogSanitizer {

    /**
     * Sanitizes input string by replacing line-ending characters and tabs
     * with a safe placeholder to prevent log injection attacks (CWE-117).
     *
     * @param input The input string to sanitize
     * @return The sanitized string, or "null" if input is null
     */
    public static String sanitize(String input) {
        if (input == null) {
            return "null";
        }
        return input.replace('\n', '_').replace('\r', '_').replace('\t', '_');
    }

    /**
     * Overload for Object input specifically for logging purposes.
     * 
     * @param input The object to sanitize
     * @return The sanitized string representation
     */
    public static String sanitize(Object input) {
        if (input == null) {
            return "null";
        }
        return sanitize(input.toString());
    }
}
