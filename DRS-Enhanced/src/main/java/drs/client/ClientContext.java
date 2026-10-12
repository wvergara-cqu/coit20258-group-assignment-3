package drs.client;

import drs.client.service.DRSClient;
import drs.common.model.User;

/**
 * Holds the single DRSClient for the running GUI so every tab controller shares
 * one authenticated connection.
 */
public final class ClientContext {

    private static DRSClient client;

    private ClientContext() { }

    public static DRSClient getClient() { return client; }
    public static void setClient(DRSClient c) { client = c; }

    /** @return the logged-in user, or null */
    public static User getUser() { return client == null ? null : client.getCurrentUser(); }
}
