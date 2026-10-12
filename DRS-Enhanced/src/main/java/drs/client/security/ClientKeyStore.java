package drs.client.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;

import drs.common.security.SignatureUtil;

/**
 * Holds each user's RSA signing key pair on the client machine
 * (~/.drs-enhanced/keys). The private key never leaves the client; only the
 * public key is enrolled with the server at login. Because only the user's
 * client can produce valid signatures, signed actions are non-repudiable.
 */
public final class ClientKeyStore {

    private static Path baseDir = Path.of(System.getProperty("user.home"), ".drs-enhanced", "keys");

    private ClientKeyStore() { }

    /** Overrides the key directory (used by tests). */
    public static void setBaseDir(Path dir) {
        baseDir = dir;
    }

    /**
     * Loads the key pair for username, generating and saving one on first use.
     */
    public static KeyPair loadOrCreate(String username) throws GeneralSecurityException, IOException {
        String safe = username.replaceAll("[^A-Za-z0-9._]", "_");
        Path priv = baseDir.resolve(safe + ".key");
        Path pub = baseDir.resolve(safe + ".pub");
        if (Files.exists(priv) && Files.exists(pub)) {
            PrivateKey pk = SignatureUtil.decodePrivateKey(Files.readString(priv, StandardCharsets.UTF_8).trim());
            PublicKey pu = SignatureUtil.decodePublicKey(Files.readString(pub, StandardCharsets.UTF_8).trim());
            return new KeyPair(pu, pk);
        }
        Files.createDirectories(baseDir);
        KeyPair kp = SignatureUtil.generateKeyPair();
        Files.writeString(priv, SignatureUtil.encodePrivateKey(kp.getPrivate()), StandardCharsets.UTF_8);
        Files.writeString(pub, SignatureUtil.encodePublicKey(kp.getPublic()), StandardCharsets.UTF_8);
        try {
            Files.setPosixFilePermissions(priv, PosixFilePermissions.fromString("rw-------"));
        } catch (UnsupportedOperationException ignored) {
            // Windows - rely on the user profile ACL
        }
        return kp;
    }
}
