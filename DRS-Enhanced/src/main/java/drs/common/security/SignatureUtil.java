package drs.common.security;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Digital signature and hashing helpers shared by client and server.
 * Clients sign the canonical form of critical submissions with their private
 * RSA key (SHA256withRSA); the server verifies with the registered public key,
 * giving non-repudiation.
 */
public final class SignatureUtil {

    /** Signature algorithm used across the system. */
    public static final String SIGNATURE_ALGORITHM = "SHA256withRSA";
    private static final String KEY_ALGORITHM = "RSA";
    private static final int KEY_SIZE = 2048;

    private SignatureUtil() { }

    /** @return a new 2048-bit RSA key pair */
    public static KeyPair generateKeyPair() throws GeneralSecurityException {
        KeyPairGenerator gen = KeyPairGenerator.getInstance(KEY_ALGORITHM);
        gen.initialize(KEY_SIZE);
        return gen.generateKeyPair();
    }

    /**
     * Signs text with a private key.
     *
     * @return Base64-encoded signature
     */
    public static String sign(String text, PrivateKey key) throws GeneralSecurityException {
        Signature s = Signature.getInstance(SIGNATURE_ALGORITHM);
        s.initSign(key);
        s.update(text.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(s.sign());
    }

    /**
     * Verifies a Base64 signature. Never throws for bad input; returns false instead.
     *
     * @return true only if the signature is valid for the text and key
     */
    public static boolean verify(String text, String base64Signature, PublicKey key) {
        if (text == null || base64Signature == null || key == null) {
            return false;
        }
        try {
            Signature s = Signature.getInstance(SIGNATURE_ALGORITHM);
            s.initVerify(key);
            s.update(text.getBytes(StandardCharsets.UTF_8));
            return s.verify(Base64.getDecoder().decode(base64Signature));
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            return false;
        }
    }

    /** @return the Base64 X.509 encoding of a public key */
    public static String encodePublicKey(PublicKey key) {
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }

    /** @return the public key decoded from Base64 X.509 */
    public static PublicKey decodePublicKey(String base64) throws GeneralSecurityException {
        byte[] bytes = Base64.getDecoder().decode(base64);
        return KeyFactory.getInstance(KEY_ALGORITHM).generatePublic(new X509EncodedKeySpec(bytes));
    }

    /** @return the private key decoded from Base64 PKCS#8 */
    public static PrivateKey decodePrivateKey(String base64) throws GeneralSecurityException {
        byte[] bytes = Base64.getDecoder().decode(base64);
        return KeyFactory.getInstance(KEY_ALGORITHM).generatePrivate(new PKCS8EncodedKeySpec(bytes));
    }

    /** @return the Base64 PKCS#8 encoding of a private key */
    public static String encodePrivateKey(PrivateKey key) {
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }

    /** @return the lowercase hex SHA-256 digest of the text */
    public static String sha256Hex(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
