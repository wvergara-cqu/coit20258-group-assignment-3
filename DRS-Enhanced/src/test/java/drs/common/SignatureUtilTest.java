package drs.common;

import static org.junit.jupiter.api.Assertions.*;

import java.security.KeyPair;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import drs.common.security.SignatureUtil;

/** Non-repudiation: RSA signing / verification and hashing. */
class SignatureUtilTest {

    private static KeyPair alice;
    private static KeyPair mallory;

    @BeforeAll
    static void keys() throws Exception {
        alice = SignatureUtil.generateKeyPair();
        mallory = SignatureUtil.generateKeyPair();
    }

    @Test
    @DisplayName("A signature verifies with the signer's public key")
    void signAndVerify() throws Exception {
        String sig = SignatureUtil.sign("REPORT|FIRE|HIGH|x", alice.getPrivate());
        assertTrue(SignatureUtil.verify("REPORT|FIRE|HIGH|x", sig, alice.getPublic()));
    }

    @Test
    @DisplayName("Tampered content fails verification")
    void tamperedContentFails() throws Exception {
        String sig = SignatureUtil.sign("REPORT|FIRE|LOW|x", alice.getPrivate());
        assertFalse(SignatureUtil.verify("REPORT|FIRE|CRITICAL|x", sig, alice.getPublic()));
    }

    @Test
    @DisplayName("Another user's key cannot forge a signature")
    void wrongKeyFails() throws Exception {
        String sig = SignatureUtil.sign("data", mallory.getPrivate());
        assertFalse(SignatureUtil.verify("data", sig, alice.getPublic()));
    }

    @Test
    @DisplayName("Garbage or missing signatures return false rather than throwing")
    void garbageIsRejected() {
        assertFalse(SignatureUtil.verify("data", "not-base64!!", alice.getPublic()));
        assertFalse(SignatureUtil.verify("data", null, alice.getPublic()));
        assertFalse(SignatureUtil.verify("data", "AAAA", null));
    }

    @Test
    @DisplayName("Keys survive Base64 encoding round-trips")
    void keyEncodingRoundTrip() throws Exception {
        String pub = SignatureUtil.encodePublicKey(alice.getPublic());
        String priv = SignatureUtil.encodePrivateKey(alice.getPrivate());
        String sig = SignatureUtil.sign("x", SignatureUtil.decodePrivateKey(priv));
        assertTrue(SignatureUtil.verify("x", sig, SignatureUtil.decodePublicKey(pub)));
    }

    @Test
    void sha256KnownVector() {
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", SignatureUtil.sha256Hex("abc"));
    }
}
