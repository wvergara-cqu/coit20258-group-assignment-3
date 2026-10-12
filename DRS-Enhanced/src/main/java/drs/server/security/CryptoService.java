package drs.server.security;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * AES-256-GCM encryption and decryption of personal data at rest (reporter name
 * and phone). Each value gets a fresh 12-byte IV which is stored in front of the
 * ciphertext; GCM's authentication tag detects any tampering with stored values.
 */
public class CryptoService {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final SecretKey key;

    /**
     * @param base64Key a Base64-encoded 256-bit AES key
     */
    public CryptoService(String base64Key) {
        byte[] raw = Base64.getDecoder().decode(base64Key);
        if (raw.length != 32) {
            throw new IllegalArgumentException("AES key must be 256 bits (32 bytes)");
        }
        this.key = new SecretKeySpec(raw, "AES");
    }

    /** @return a new random Base64 AES-256 key (used to provision server.properties) */
    public static String generateKey() throws GeneralSecurityException {
        KeyGenerator gen = KeyGenerator.getInstance("AES");
        gen.init(256);
        return Base64.getEncoder().encodeToString(gen.generateKey().getEncoded());
    }

    /** @return Base64(IV || ciphertext || tag) */
    public String encrypt(String plainText) {
        if (plainText == null) {
            return null;
        }
        try {
            byte[] iv = new byte[IV_BYTES];
            RANDOM.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            byte[] ct = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(ByteBuffer.allocate(iv.length + ct.length).put(iv).put(ct).array());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Encryption failed", e);
        }
    }

    /**
     * @return the decrypted text
     * @throws IllegalStateException if the value was tampered with or the key is wrong
     */
    public String decrypt(String encoded) {
        if (encoded == null) {
            return null;
        }
        try {
            byte[] all = Base64.getDecoder().decode(encoded);
            ByteBuffer buf = ByteBuffer.wrap(all);
            byte[] iv = new byte[IV_BYTES];
            buf.get(iv);
            byte[] ct = new byte[buf.remaining()];
            buf.get(ct);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            return new String(cipher.doFinal(ct), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new IllegalStateException("Decryption failed - data corrupted or wrong key", e);
        }
    }
}
