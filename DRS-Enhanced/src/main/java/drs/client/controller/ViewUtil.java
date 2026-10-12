package drs.client.controller;

import java.util.function.Function;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;

/**
 * Shared presentation helpers: feedback labels and colour-coded table cells
 * (visual priority cues from the A1 usability design).
 */
public final class ViewUtil {

    private ViewUtil() { }

    /** Shows a green success message. */
    public static void success(Label label, String message) {
        label.getStyleClass().removeAll("msg-error", "msg-success");
        label.getStyleClass().add("msg-success");
        label.setText(message);
    }

    /** Shows a red error message. */
    public static void error(Label label, String message) {
        label.getStyleClass().removeAll("msg-error", "msg-success");
        label.getStyleClass().add("msg-error");
        label.setText(message);
    }

    /** @return the CSS colour for a priority score */
    public static String priorityColour(int p) {
        if (p >= 45) {
            return "#c0392b";
        }
        if (p >= 35) {
            return "#e67e22";
        }
        if (p >= 25) {
            return "#d4ac0d";
        }
        return "#27ae60";
    }

    /** Renders an Integer priority column as a coloured badge. */
    public static <S> void priorityBadges(TableColumn<S, Integer> col) {
        col.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(Integer p, boolean empty) {
                super.updateItem(p, empty);
                if (empty || p == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(String.valueOf(p));
                    setStyle("-fx-background-color: " + priorityColour(p)
                            + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-alignment: CENTER;");
                }
            }
        });
    }

    /** Renders any column as a coloured badge whose colour is derived from the value. */
    public static <S, T> void colouredBadges(TableColumn<S, T> col, Function<T, String> colour) {
        col.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(T v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(v.toString());
                    setStyle("-fx-background-color: " + colour.apply(v)
                            + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-alignment: CENTER;");
                }
            }
        });
    }

    /** @return true if the user confirms */
    public static boolean confirm(String title, String message) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.OK, ButtonType.CANCEL);
        a.setTitle(title);
        a.setHeaderText(null);
        return a.showAndWait().filter(b -> b == ButtonType.OK).isPresent();
    }
}
