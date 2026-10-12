package drs.client;

import java.io.IOException;

import drs.client.controller.MainController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * DRS-Enhanced JavaFX client (presentation tier). Shows the login screen, then
 * the role-specific main window.
 *
 * Run: mvn javafx:run   (start DRSServer first)
 */
public class DRSClientApp extends Application {

    private static final String TITLE = "Disaster Response System - DRS-Enhanced";
    private static DRSClientApp instance;
    private Stage stage;

    @Override
    public void start(Stage primaryStage) throws IOException {
        instance = this;
        this.stage = primaryStage;
        stage.setTitle(TITLE);
        stage.setMinWidth(900);
        stage.setMinHeight(620);
        showLogin();
        stage.show();
    }

    /** Replaces the scene with the login screen. */
    public void showLogin() throws IOException {
        Parent root = load("login.fxml").load();
        stage.setScene(styled(new Scene(root, 1000, 680)));
        stage.setTitle(TITLE);
    }

    /** Replaces the scene with the main window for the logged-in user. */
    public void showMain() throws IOException {
        FXMLLoader loader = load("main.fxml");
        Parent root = loader.load();
        MainController mc = loader.getController();
        mc.buildTabsForRole();
        stage.setScene(styled(new Scene(root, 1200, 820)));
        stage.setTitle(TITLE + " - " + ClientContext.getUser());
    }

    /** @return a loader for a view in /drs/client/view */
    public static FXMLLoader load(String fxml) {
        return new FXMLLoader(DRSClientApp.class.getResource("/drs/client/view/" + fxml));
    }

    private static Scene styled(Scene scene) {
        scene.getStylesheets().add(DRSClientApp.class.getResource("/drs/client/view/styles.css").toExternalForm());
        return scene;
    }

    /** @return the running application */
    public static DRSClientApp getInstance() {
        return instance;
    }

    @Override
    public void stop() {
        if (ClientContext.getClient() != null) {
            ClientContext.getClient().close();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
