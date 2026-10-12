package drs.client.controller;

/**
 * Implemented by every tab controller; MainController calls refresh() when the
 * tab becomes visible so it always shows the latest server data.
 */
public interface RefreshableView {

    /** Reloads data from the server. */
    void refresh();
}
