package drs.server.security;

import java.security.PublicKey;

import drs.common.model.User;

/**
 * Per-connection security context. Each ClientHandler thread owns exactly one
 * Session, so no synchronisation is required.
 */
public class Session {

    private User user;
    private PublicKey publicKey;
    private final String remoteAddress;

    /** @param remoteAddress client socket address, for logging */
    public Session(String remoteAddress) {
        this.remoteAddress = remoteAddress;
    }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public PublicKey getPublicKey() { return publicKey; }
    public void setPublicKey(PublicKey publicKey) { this.publicKey = publicKey; }
    public String getRemoteAddress() { return remoteAddress; }

    /** @return true once LOGIN has succeeded */
    public boolean isAuthenticated() { return user != null; }

    /** @return the username or "anonymous" */
    public String getUsername() { return user == null ? "anonymous" : user.getUsername(); }

    /** Clears the session on logout. */
    public void clear() {
        user = null;
        publicKey = null;
    }
}
