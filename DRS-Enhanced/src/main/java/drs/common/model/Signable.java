package drs.common.model;

/**
 * Implemented by every object whose submission must be non-repudiable.
 * The canonical form is the exact text the client signs and the server verifies,
 * so it must contain only fields supplied by the user (never server-assigned IDs
 * or timestamps).
 */
public interface Signable {

    /**
     * @return a deterministic, pipe-delimited representation of the user-supplied content
     */
    String canonicalForm();
}
