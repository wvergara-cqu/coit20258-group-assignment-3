package drs.common.util;

import java.util.regex.Pattern;

/**
 * Input validation rules shared by the client (immediate feedback) and the server
 * (authoritative check). Each method returns null when valid or an error message.
 */
public final class InputValidator {

    /** Maximum length of short text fields. */
    public static final int MAX_SHORT = 150;
    /** Maximum length of free-text fields. */
    public static final int MAX_LONG = 2000;

    private static final Pattern PHONE = Pattern.compile("^[0-9+()\\-\\s]{8,20}$");
    private static final Pattern USERNAME = Pattern.compile("^[a-zA-Z0-9._]{3,30}$");

    private InputValidator() { }

    /** @return null if the value is present and within maxLength, otherwise an error */
    public static String required(String fieldName, String value, int maxLength) {
        if (value == null || value.trim().isEmpty()) {
            return fieldName + " is required.";
        }
        if (value.trim().length() > maxLength) {
            return fieldName + " must be " + maxLength + " characters or fewer.";
        }
        return null;
    }

    /** @return null for a plausible phone number, otherwise an error */
    public static String phone(String value) {
        String req = required("Phone", value, 20);
        if (req != null) {
            return req;
        }
        return PHONE.matcher(value.trim()).matches() ? null
                : "Phone must contain 8-20 digits, spaces, '+', '-' or brackets.";
    }

    /** @return null for a valid username, otherwise an error */
    public static String username(String value) {
        if (value == null || !USERNAME.matcher(value.trim()).matches()) {
            return "Username must be 3-30 letters, digits, '.' or '_'.";
        }
        return null;
    }

    /**
     * Password policy: at least 8 characters with a letter and a digit.
     *
     * @return null for an acceptable password, otherwise an error
     */
    public static String password(String value) {
        if (value == null || value.length() < 8) {
            return "Password must be at least 8 characters.";
        }
        boolean letter = value.chars().anyMatch(Character::isLetter);
        boolean digit = value.chars().anyMatch(Character::isDigit);
        return letter && digit ? null : "Password must contain a letter and a digit.";
    }
}
