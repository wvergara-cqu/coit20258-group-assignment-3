package drs.server.security;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Salted password hashing with PBKDF2-HMAC-SHA256. Plain-text passwords are never
 * stored or logged.
 */
public final class PasswordHasher {

    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 65_536;
    private static final int KEY_LENGTH_BITS = 256;
    private static final int SALT_BYTES = 16;
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordHasher() { }

    /** @return a new random Base64 salt */
    public static String newSalt() {
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    /** @return the Base64 PBKDF2 hash of password with salt */
    public static String hash(String password, String salt) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), Base64.getDecoder().decode(salt),
                    ITERATIONS, KEY_LENGTH_BITS);
            byte[] hash = SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded();
            spec.clearPassword();
            return Base64.getEncoder().encodeToString(hash);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("PBKDF2 unavailable", e);
        }
    }

    /**
     * Constant-time comparison of a candidate password with a stored hash.
     *
     * @return true if the password matches
     */
    public static boolean matches(String password, String salt, String expectedHash) {
        if (password == null || salt == null || expectedHash == null) {
            return false;
        }
        byte[] actual = Base64.getDecoder().decode(hash(password, salt));
        byte[] expected = Base64.getDecoder().decode(expectedHash);
        return MessageDigest.isEqual(actual, expected);
    }
}
