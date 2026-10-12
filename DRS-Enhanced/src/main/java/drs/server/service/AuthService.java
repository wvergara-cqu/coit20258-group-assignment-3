package drs.server.service;

import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.sql.SQLException;
import java.util.Optional;

import drs.common.model.Role;
import drs.common.model.User;
import drs.common.security.SignatureUtil;
import drs.common.util.InputValidator;
import drs.server.db.UserDAO;
import drs.server.security.PasswordHasher;

/**
 * Authentication, citizen self-registration and public-key enrolment.
 */
public class AuthService {

    private final UserDAO users;

    public AuthService(UserDAO users) {
        this.users = users;
    }

    /**
     * @return the authenticated user
     * @throws ServiceException with a deliberately generic message on failure
     */
    public User login(String username, String password) throws ServiceException, SQLException {
        if (username == null || password == null) {
            throw new ServiceException("Invalid username or password.");
        }
        Optional<UserDAO.Credentials> cred = users.findByUsername(username.trim());
        if (cred.isEmpty() || !PasswordHasher.matches(password, cred.get().salt, cred.get().passwordHash)) {
            throw new ServiceException("Invalid username or password.");
        }
        users.updateLastLogin(cred.get().user.getId());
        return cred.get().user;
    }

    /**
     * Self-registration is limited to the CITIZEN role; staff accounts are
     * provisioned by an administrator.
     *
     * @return the new user
     */
    public User registerCitizen(String username, String password, String fullName)
            throws ServiceException, SQLException {
        String err = InputValidator.username(username);
        if (err == null) {
            err = InputValidator.password(password);
        }
        if (err == null) {
            err = InputValidator.required("Full name", fullName, InputValidator.MAX_SHORT);
        }
        if (err != null) {
            throw new ServiceException(err);
        }
        if (users.findByUsername(username.trim()).isPresent()) {
            throw new ServiceException("Username is already taken.");
        }
        String salt = PasswordHasher.newSalt();
        int id = users.insert(username.trim(), fullName.trim(), Role.CITIZEN, PasswordHasher.hash(password, salt), salt);
        return new User(id, username.trim(), fullName.trim(), Role.CITIZEN);
    }

    /**
     * Enrols (or re-enrols) the RSA public key the user's client will sign with.
     *
     * @return the decoded key
     * @throws ServiceException if the key is malformed
     */
    public PublicKey enrolKey(User user, String publicKeyBase64) throws ServiceException, SQLException {
        try {
            PublicKey key = SignatureUtil.decodePublicKey(publicKeyBase64);
            String stored = users.findPublicKey(user.getId());
            if (!publicKeyBase64.equals(stored)) {
                users.updatePublicKey(user.getId(), publicKeyBase64);
            }
            return key;
        } catch (GeneralSecurityException | IllegalArgumentException | NullPointerException e) {
            throw new ServiceException("A valid signing key is required to log in.");
        }
    }
}
