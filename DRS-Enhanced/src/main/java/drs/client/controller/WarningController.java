package drs.client.controller;

import java.util.List;
import java.util.stream.Collectors;

import drs.client.ClientContext;
import drs.client.service.ClientException;
import drs.common.model.DisasterReport;
import drs.common.model.DisasterStatus;
import drs.common.model.PublicWarning;
import drs.common.model.WarningLevel;
import drs.common.model.WarningStatus;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import javafx.util.StringConverter;

/**
 * Enhanced Feature 1 - "Public Warnings" tab (UC-6, FR-5, US-3). Every role sees
 * the live, colour-coded public warnings board (auto-refreshed every 15 s).
 * Controllers additionally get a pre-filled, signed "Issue Warning" panel and the
 * full warning register with a cancel action.
 */
public class WarningController implements RefreshableView {

    private static final int REFRESH_SECONDS = 15;

    private final ObservableList<PublicWarning> boardData = FXCollections.observableArrayList();
    private final ObservableList<PublicWarning> registerData = FXCollections.observableArrayList();
    private List<PublicWarning> activeWarnings = List.of();
    private Timeline autoRefresh;
    private boolean staff;

    @FXML private TextField areaFilterField;
    @FXML private Label boardCountLabel;
    @FXML private ListView<PublicWarning> boardList;
    @FXML private VBox issuePane;
    @FXML private ComboBox<DisasterReport> incidentComboBox;
    @FXML private ComboBox<WarningLevel> levelComboBox;
    @FXML private TextField affectedAreaField;
    @FXML private Spinner<Integer> radiusSpinner;
    @FXML private Spinner<Integer> hoursSpinner;
    @FXML private TextArea messageArea;
    @FXML private TextArea actionArea;
    @FXML private Button issueButton;
    @FXML private VBox registerPane;
    @FXML private TableView<PublicWarning> registerTable;
    @FXML private TableColumn<PublicWarning, Integer> wIdCol;
    @FXML private TableColumn<PublicWarning, Integer> wReportCol;
    @FXML private TableColumn<PublicWarning, WarningLevel> wLevelCol;
    @FXML private TableColumn<PublicWarning, String> wAreaCol;
    @FXML private TableColumn<PublicWarning, Integer> wRadiusCol;
    @FXML private TableColumn<PublicWarning, WarningStatus> wStatusCol;
    @FXML private TableColumn<PublicWarning, String> wIssuedCol;
    @FXML private TableColumn<PublicWarning, String> wExpiresCol;
    @FXML private TableColumn<PublicWarning, String> wByCol;
    @FXML private Label statusLabel;

    @FXML
    public void initialize() {
        staff = ClientContext.getUser() != null && ClientContext.getUser().isStaff();
        for (VBox pane : new VBox[]{issuePane, registerPane}) {
            pane.setVisible(staff);
            pane.setManaged(staff);
        }
        boardList.setItems(boardData);
        boardList.setCellFactory(l -> new WarningCell());
        areaFilterField.textProperty().addListener((o, a, b) -> applyFilter());

        levelComboBox.setItems(FXCollections.observableArrayList(WarningLevel.values()));
        levelComboBox.valueProperty().addListener((o, a, b) -> {
            if (b != null && (a == null || actionArea.getText().isBlank()
                    || actionArea.getText().equals(a.getDefaultAction()))) {
                actionArea.setText(b.getDefaultAction());
            }
        });
        radiusSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 200, 10));
        hoursSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 72, 12));
        radiusSpinner.setEditable(true);
        hoursSpinner.setEditable(true);
        incidentComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(DisasterReport r) {
                return r == null ? "" : r.getSummary();
            }

            @Override
            public DisasterReport fromString(String s) {
                return null;
            }
        });
        incidentComboBox.valueProperty().addListener((o, a, b) -> prefill(b));

        wIdCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        wReportCol.setCellValueFactory(new PropertyValueFactory<>("reportId"));
        wLevelCol.setCellValueFactory(new PropertyValueFactory<>("level"));
        wAreaCol.setCellValueFactory(new PropertyValueFactory<>("affectedArea"));
        wRadiusCol.setCellValueFactory(new PropertyValueFactory<>("radiusKm"));
        wStatusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        wIssuedCol.setCellValueFactory(new PropertyValueFactory<>("issuedAt"));
        wExpiresCol.setCellValueFactory(new PropertyValueFactory<>("expiresAt"));
        wByCol.setCellValueFactory(new PropertyValueFactory<>("issuedBy"));
        ViewUtil.colouredBadges(wLevelCol, WarningLevel::getColourHex);
        registerTable.setItems(registerData);

        autoRefresh = new Timeline(new KeyFrame(Duration.seconds(REFRESH_SECONDS), e -> refreshBoard()));
        autoRefresh.setCycleCount(Animation.INDEFINITE);
        autoRefresh.play();
        refresh();
    }

    @Override
    public void refresh() {
        refreshBoard();
        if (!staff) {
            return;
        }
        try {
            DisasterReport keep = incidentComboBox.getValue();
            List<DisasterReport> open = ClientContext.getClient().getReports().stream()
                    .filter(r -> r.getStatus() != DisasterStatus.RESOLVED).collect(Collectors.toList());
            incidentComboBox.setItems(FXCollections.observableArrayList(open));
            if (keep != null) {
                open.stream().filter(r -> r.getId() == keep.getId()).findFirst()
                        .ifPresent(r -> incidentComboBox.getSelectionModel().select(r));
            }
            registerData.setAll(ClientContext.getClient().getAllWarnings());
        } catch (ClientException e) {
            ViewUtil.error(statusLabel, e.getMessage());
        }
    }

    private void refreshBoard() {
        if (ClientContext.getClient() == null) {
            autoRefresh.stop();     // user has logged out
            return;
        }
        try {
            activeWarnings = ClientContext.getClient().getActiveWarnings();
            applyFilter();
        } catch (ClientException e) {
            ViewUtil.error(statusLabel, e.getMessage());
        }
    }

    private void applyFilter() {
        String f = areaFilterField.getText() == null ? "" : areaFilterField.getText().trim().toLowerCase();
        boardData.setAll(activeWarnings.stream()
                .filter(w -> f.isEmpty() || w.getAffectedArea().toLowerCase().contains(f))
                .collect(Collectors.toList()));
        boardCountLabel.setText(boardData.size() + " active warning(s)"
                + (f.isEmpty() ? "" : " matching '" + f + "'") + " - refreshes every " + REFRESH_SECONDS + " s");
    }

    /** Pre-fills the form from the server's WarningAdvisor suggestion (UC-6 step 2). */
    private void prefill(DisasterReport r) {
        if (r == null) {
            return;
        }
        try {
            PublicWarning s = ClientContext.getClient().suggestWarning(r.getId());
            levelComboBox.setValue(s.getLevel());
            affectedAreaField.setText(s.getAffectedArea());
            radiusSpinner.getValueFactory().setValue(s.getRadiusKm());
            hoursSpinner.getValueFactory().setValue(s.getValidHours());
            messageArea.setText(s.getMessage());
            actionArea.setText(s.getRecommendedAction());
            statusLabel.setText("Suggested " + s.getLevel() + " pre-filled - review and edit before issuing.");
            statusLabel.getStyleClass().removeAll("msg-error", "msg-success");
        } catch (ClientException e) {
            ViewUtil.error(statusLabel, e.getMessage());
        }
    }

    @FXML
    private void handleIssue() {
        DisasterReport r = incidentComboBox.getValue();
        if (r == null || levelComboBox.getValue() == null) {
            ViewUtil.error(statusLabel, "Select an incident and a warning level.");
            return;
        }
        if (affectedAreaField.getText().isBlank() || messageArea.getText().isBlank() || actionArea.getText().isBlank()) {
            ViewUtil.error(statusLabel, "Affected area, message and recommended action are required.");
            return;
        }
        PublicWarning w = new PublicWarning(r.getId(), levelComboBox.getValue(), affectedAreaField.getText().trim(),
                radiusSpinner.getValue(), messageArea.getText().trim(), actionArea.getText().trim(),
                hoursSpinner.getValue());
        if (w.getLevel() == WarningLevel.EMERGENCY_WARNING && !ViewUtil.confirm("Broadcast Emergency Warning",
                "Broadcast an EMERGENCY WARNING for " + w.getAffectedArea() + " (" + w.getRadiusKm() + " km)?")) {
            return;
        }
        try {
            ClientContext.getClient().issueWarning(w);
            ViewUtil.success(statusLabel, ClientContext.getClient().getLastMessage());
            refresh();
        } catch (ClientException e) {
            ViewUtil.error(statusLabel, e.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        PublicWarning w = registerTable.getSelectionModel().getSelectedItem();
        if (w == null) {
            ViewUtil.error(statusLabel, "Select a warning in the register first.");
            return;
        }
        if (w.getStatus() != WarningStatus.ACTIVE) {
            ViewUtil.error(statusLabel, "Warning #" + w.getId() + " is already " + w.getStatus() + ".");
            return;
        }
        try {
            ClientContext.getClient().cancelWarning(w.getId());
            ViewUtil.success(statusLabel, ClientContext.getClient().getLastMessage());
            refresh();
        } catch (ClientException e) {
            ViewUtil.error(statusLabel, e.getMessage());
        }
    }

    /** Renders a warning as a colour-coded card on the public board. */
    private static final class WarningCell extends ListCell<PublicWarning> {
        @Override
        protected void updateItem(PublicWarning w, boolean empty) {
            super.updateItem(w, empty);
            if (empty || w == null) {
                setText(null);
                setGraphic(null);
                setStyle("");
                return;
            }
            Label head = new Label(w.getLevel().getLabel().toUpperCase() + "  -  " + w.getDisasterType()
                    + "  -  " + w.getAffectedArea() + " (" + w.getRadiusKm() + " km)");
            head.getStyleClass().add("warning-head");
            Label msg = new Label(w.getMessage());
            msg.setWrapText(true);
            Label act = new Label("WHAT TO DO: " + w.getRecommendedAction());
            act.setWrapText(true);
            act.getStyleClass().add("warning-action");
            Label meta = new Label("Issued " + w.getIssuedAt() + "  |  valid until " + w.getExpiresAt()
                    + "  |  ref W-" + w.getId());
            meta.getStyleClass().add("warning-meta");
            VBox box = new VBox(4, head, msg, act, meta);
            box.setStyle("-fx-padding: 10; -fx-background-radius: 6; -fx-border-radius: 6; -fx-border-width: 0 0 0 8;"
                    + " -fx-border-color: " + w.getLevel().getColourHex() + "; -fx-background-color: "
                    + w.getLevel().getColourHex() + "22;");
            box.prefWidthProperty().bind(getListView().widthProperty().subtract(40));
            setText(null);
            setGraphic(box);
        }
    }
}
