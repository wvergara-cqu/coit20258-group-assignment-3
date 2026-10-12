package drs.client.controller;

import drs.client.ClientContext;
import drs.client.DRSClientApp;
import drs.client.service.ClientException;
import drs.client.service.DRSClient;
import drs.common.util.InputValidator;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

/**
 * Login and citizen self-registration screen.
 */
public class LoginController {

    @FXML private TextField hostField;
    @FXML private TextField portField;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private TextField fullNameField;
    @FXML private PasswordField confirmField;
    @FXML private VBox registerBox;
    @FXML private Button loginButton;
    @FXML private Button registerToggle;
    @FXML private Button createAccountButton;
    @FXML private Label statusLabel;

    @FXML
    public void initialize() {
        showRegister(false);
    }

    @FXML
    private void handleLogin() {
        String user = usernameField.getText().trim();
        String pass = passwordField.getText();
        if (user.isEmpty() || pass.isEmpty()) {
            ViewUtil.error(statusLabel, "Enter your username and password.");
            return;
        }
        DRSClient client = null;
        try {
            client = connect();
            client.login(user, pass);
            ClientContext.setClient(client);
            DRSClientApp.getInstance().showMain();
        } catch (ClientException e) {
            if (client != null) {
                client.close();
            }
            ViewUtil.error(statusLabel, e.getMessage());
        } catch (Exception e) {
            ViewUtil.error(statusLabel, "Could not open the main window: " + e.getMessage());
        }
    }

    @FXML
    private void handleToggleRegister() {
        showRegister(!registerBox.isVisible());
    }

    @FXML
    private void handleRegister() {
        String user = usernameField.getText().trim();
        String pass = passwordField.getText();
        String err = InputValidator.username(user);
        if (err == null) {
            err = InputValidator.password(pass);
        }
        if (err == null && !pass.equals(confirmField.getText())) {
            err = "Passwords do not match.";
        }
        if (err == null) {
            err = InputValidator.required("Full name", fullNameField.getText(), InputValidator.MAX_SHORT);
        }
        if (err != null) {
            ViewUtil.error(statusLabel, err);
            return;
        }
        try (DRSClient client = connect()) {
            client.register(user, pass, fullNameField.getText().trim());
            ViewUtil.success(statusLabel, client.getLastMessage());
            showRegister(false);
        } catch (ClientException e) {
            ViewUtil.error(statusLabel, e.getMessage());
        }
    }

    private DRSClient connect() throws ClientException {
        int port;
        try {
            port = Integer.parseInt(portField.getText().trim());
        } catch (NumberFormatException e) {
            throw new ClientException("Port must be a number.");
        }
        return new DRSClient(hostField.getText().trim(), port);
    }

    private void showRegister(boolean show) {
        registerBox.setVisible(show);
        registerBox.setManaged(show);
        loginButton.setVisible(!show);
        loginButton.setManaged(!show);
        registerToggle.setText(show ? "Back to login" : "New citizen? Create an account");
    }
}
