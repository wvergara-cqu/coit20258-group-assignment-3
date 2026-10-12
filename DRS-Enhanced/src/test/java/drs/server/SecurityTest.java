package drs.server;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import drs.common.model.Role;
import drs.common.net.Command;
import drs.server.security.AccessControl;
import drs.server.security.CryptoService;
import drs.server.security.PasswordHasher;

/** Password hashing, AES-GCM encryption/decryption and RBAC matrix. */
class SecurityTest {

    @Test
    @DisplayName("PBKDF2 hash matches only the correct password")
    void passwordHashing() {
        String salt = PasswordHasher.newSalt();
        String hash = PasswordHasher.hash("Citizen@2026", salt);
        assertTrue(PasswordHasher.matches("Citizen@2026", salt, hash));
        assertFalse(PasswordHasher.matches("citizen@2026", salt, hash));
        assertNotEquals(hash, PasswordHasher.hash("Citizen@2026", PasswordHasher.newSalt()), "salt must change the hash");
    }

    @Test
    @DisplayName("AES-GCM round-trips and uses a fresh IV every time")
    void encryptDecrypt() throws Exception {
        CryptoService c = new CryptoService(CryptoService.generateKey());
        String a = c.encrypt("Jane Doe");
        String b = c.encrypt("Jane Doe");
        assertNotEquals("Jane Doe", a);
        assertNotEquals(a, b, "same plaintext must not give the same ciphertext");
        assertEquals("Jane Doe", c.decrypt(a));
        assertEquals("Jane Doe", c.decrypt(b));
    }

    @Test
    @DisplayName("Tampered ciphertext or wrong key is detected")
    void tamperDetected() throws Exception {
        CryptoService c = new CryptoService(CryptoService.generateKey());
        String enc = c.encrypt("0400-123-456");
        char[] chars = enc.toCharArray();
        chars[20] = chars[20] == 'A' ? 'B' : 'A';
        assertThrows(IllegalStateException.class, () -> c.decrypt(new String(chars)));
        CryptoService other = new CryptoService(CryptoService.generateKey());
        assertThrows(IllegalStateException.class, () -> other.decrypt(enc));
    }

    @Test
    void rejectsShortKey() {
        assertThrows(IllegalArgumentException.class, () -> new CryptoService("AAAA"));
    }

    @Test
    @DisplayName("RBAC: anonymous users may only ping, log in or register")
    void anonymousAccess() {
        assertTrue(AccessControl.isAllowed(null, Command.LOGIN));
        assertTrue(AccessControl.isAllowed(null, Command.REGISTER));
        assertFalse(AccessControl.isAllowed(null, Command.SUBMIT_REPORT));
        assertFalse(AccessControl.isAllowed(null, Command.GET_ACTIVE_WARNINGS));
    }

    @Test
    @DisplayName("RBAC: each role gets exactly its permitted commands")
    void roleMatrix() {
        assertTrue(AccessControl.isAllowed(Role.CITIZEN, Command.SUBMIT_REPORT));
        assertTrue(AccessControl.isAllowed(Role.CITIZEN, Command.GET_ACTIVE_WARNINGS));
        assertFalse(AccessControl.isAllowed(Role.CITIZEN, Command.GET_REPORTS));
        assertFalse(AccessControl.isAllowed(Role.CITIZEN, Command.ISSUE_WARNING));

        assertTrue(AccessControl.isAllowed(Role.RESPONDER, Command.UPDATE_TASK_STATUS));
        assertFalse(AccessControl.isAllowed(Role.RESPONDER, Command.SAVE_RESPONSE));
        assertFalse(AccessControl.isAllowed(Role.RESPONDER, Command.GET_REPORTS));

        assertTrue(AccessControl.isAllowed(Role.CONTROLLER, Command.ISSUE_WARNING));
        assertTrue(AccessControl.isAllowed(Role.CONTROLLER, Command.SAVE_RESPONSE));
        assertFalse(AccessControl.isAllowed(Role.CONTROLLER, Command.UPDATE_TASK_STATUS));
        assertFalse(AccessControl.isAllowed(Role.CONTROLLER, Command.GET_AUDIT_LOG));

        assertTrue(AccessControl.isAllowed(Role.ADMIN, Command.GET_AUDIT_LOG));
        assertFalse(AccessControl.isAllowed(Role.ADMIN, null));
    }

    @Test
    void personalDataVisibility() {
        assertTrue(AccessControl.canViewPersonalData(Role.CONTROLLER));
        assertTrue(AccessControl.canViewPersonalData(Role.ADMIN));
        assertFalse(AccessControl.canViewPersonalData(Role.RESPONDER));
        assertFalse(AccessControl.canViewPersonalData(Role.CITIZEN));
    }
}
