package drs.client.controller;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import drs.client.ClientContext;
import drs.client.DRSClientApp;
import drs.client.service.ClientException;
import drs.common.model.IncidentSummary;
import drs.common.model.Role;
import drs.common.model.User;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

/**
 * Main window: header bar plus a TabPane whose tabs depend on the user's role
 * (the client mirrors the server's access-control matrix so users only see what
 * they may use).
 */
public class MainController {

    @FXML private TabPane mainTabPane;
    @FXML private Label userLabel;
    @FXML private Label serverLabel;
    @FXML private Label summaryLabel;

    private final Map<Tab, RefreshableView> controllers = new HashMap<>();

    /** Adds the tabs permitted for the logged-in role. */
    public void buildTabsForRole() throws IOException {
        User u = ClientContext.getUser();
        userLabel.setText(u.getFullName() + "  |  " + u.getRole().getLabel());
        serverLabel.setText("Server " + ClientContext.getClient().getHost() + ":" + ClientContext.getClient().getPort());
        Role r = u.getRole();
        if (r == Role.CITIZEN || r == Role.CONTROLLER || r == Role.ADMIN) {
            addTab("Report Disaster", "reportDisaster.fxml");
        }
        if (r == Role.CONTROLLER || r == Role.ADMIN) {
            addTab("Assess & Prioritise", "assessDisaster.fxml");
            addTab("Coordinate Response", "coordination.fxml");
        }
        addTab(r == Role.RESPONDER ? "My Field Tasks" : "Field Tasks", "fieldTasks.fxml", r != Role.CITIZEN);
        addTab("Public Warnings", "warnings.fxml");
        if (r == Role.CONTROLLER || r == Role.ADMIN) {
            addTab("History Log", "history.fxml");
            addTab("Resources", "resources.fxml");
        }
        if (r == Role.ADMIN) {
            addTab("Audit Log", "audit.fxml");
        }
        mainTabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            RefreshableView v = controllers.get(newTab);
            if (v != null) {
                v.refresh();
            }
            refreshSummary();
        });
        refreshSummary();
    }

    private void addTab(String title, String fxml) throws IOException {
        addTab(title, fxml, true);
    }

    private void addTab(String title, String fxml, boolean include) throws IOException {
        if (!include) {
            return;
        }
        FXMLLoader loader = DRSClientApp.load(fxml);
        Parent content = loader.load();
        Tab tab = new Tab(title, content);
        tab.setId(fxml.replace(".fxml", "Tab"));
        controllers.put(tab, loader.getController());
        mainTabPane.getTabs().add(tab);
    }

    /** Updates the header statistics for staff users. */
    private void refreshSummary() {
        User u = ClientContext.getUser();
        if (u == null || !u.isStaff()) {
            summaryLabel.setText("");
            return;
        }
        try {
            IncidentSummary s = ClientContext.getClient().getSummary();
            summaryLabel.setText(String.format("Reported %d  |  Assessing %d  |  Responding %d  |  Resolved %d"
                            + "  |  Active warnings %d  |  Open tasks %d", s.getReported(), s.getAssessing(),
                    s.getResponding(), s.getResolved(), s.getActiveWarnings(), s.getOpenTasks()));
        } catch (ClientException e) {
            summaryLabel.setText(e.getMessage());
        }
    }

    @FXML
    private void handleLogout() {
        try {
            ClientContext.getClient().logout();
        } catch (ClientException ignored) {
            // connection may already be gone
        }
        ClientContext.getClient().close();
        ClientContext.setClient(null);
        try {
            DRSClientApp.getInstance().showLogin();
        } catch (IOException e) {
            summaryLabel.setText("Cannot show login: " + e.getMessage());
        }
    }

    /** @return the tab pane (used by the screenshot harness) */
    public TabPane getTabPane() {
        return mainTabPane;
    }
}
