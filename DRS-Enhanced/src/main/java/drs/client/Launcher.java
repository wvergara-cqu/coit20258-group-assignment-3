package drs.client;

/**
 * Plain main class for running the client from a shaded (fat) jar, where the
 * JavaFX launcher requires the main class not to extend Application.
 */
public final class Launcher {

    private Launcher() { }

    public static void main(String[] args) {
        DRSClientApp.main(args);
    }
}
