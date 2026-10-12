package drs.common;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import drs.common.util.InputValidator;

/** Input validation shared by client and server. */
class InputValidatorTest {

    @Test
    void requiredField() {
        assertNotNull(InputValidator.required("Location", "  ", 10));
        assertNotNull(InputValidator.required("Location", null, 10));
        assertNotNull(InputValidator.required("Location", "12345678901", 10));
        assertNull(InputValidator.required("Location", "Rockhampton", 20));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0400-123-456", "+61 7 4930 9000", "(07) 4930 9000"})
    void validPhones(String p) {
        assertNull(InputValidator.phone(p));
    }

    @ParameterizedTest
    @ValueSource(strings = {"12345", "phone-number", "0400;DROP TABLE"})
    void invalidPhones(String p) {
        assertNotNull(InputValidator.phone(p));
    }

    @Test
    void passwordPolicy() {
        assertNotNull(InputValidator.password("short1"));
        assertNotNull(InputValidator.password("allletters"));
        assertNotNull(InputValidator.password("12345678"));
        assertNull(InputValidator.password("Citizen@2026"));
    }

    @Test
    void usernamePolicy() {
        assertNull(InputValidator.username("jane.doe"));
        assertNotNull(InputValidator.username("ab"));
        assertNotNull(InputValidator.username("bad name"));
        assertNotNull(InputValidator.username("x' OR '1'='1"));
    }
}
