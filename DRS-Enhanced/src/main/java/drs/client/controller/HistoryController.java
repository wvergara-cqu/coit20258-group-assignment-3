package drs.client.controller;

import drs.client.ClientContext;
import drs.client.service.ClientException;
import drs.common.model.DisasterReport;
import drs.common.model.DisasterStatus;
import drs.common.model.DisasterType;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.cell.PropertyValueFactory;

/**
 * "History Log" tab (A2 creative feature 1). Filters are executed by the server
 * as SQL queries rather than in client memory.
 */
public class HistoryController implements RefreshableView {

    private static final String ALL = "All";

    private final ObservableList<DisasterReport> tableData = FXCollections.observableArrayList();

    @FXML private ComboBox<String> typeFilterComboBox;
    @FXML private ComboBox<String> statusFilterComboBox;
    @FXML private Label countLabel;
    @FXML private TableView<DisasterReport> historyTable;
    @FXML private TableColumn<DisasterReport, Integer> idCol;
    @FXML private TableColumn<DisasterReport, String> typeCol;
    @FXML private TableColumn<DisasterReport, String> severityCol;
    @FXML private TableColumn<DisasterReport, String> locationCol;
    @FXML private TableColumn<DisasterReport, Integer> priorityCol;
    @FXML private TableColumn<DisasterReport, String> statusCol;
    @FXML private TableColumn<DisasterReport, String> reporterCol;
    @FXML private TableColumn<DisasterReport, String> createdCol;
    @FXML private TableColumn<DisasterReport, String> updatedCol;
    @FXML private TextArea detailArea;
    @FXML private Label statusLabel;

    @FXML
    public void initialize() {
        typeFilterComboBox.getItems().add(ALL);
        for (DisasterType t : DisasterType.values()) {
            typeFilterComboBox.getItems().add(t.name());
        }
        statusFilterComboBox.getItems().add(ALL);
        for (DisasterStatus s : DisasterStatus.values()) {
            statusFilterComboBox.getItems().add(s.name());
        }
        typeFilterComboBox.getSelectionModel().selectFirst();
        statusFilterComboBox.getSelectionModel().selectFirst();
        typeFilterComboBox.valueProperty().addListener((o, a, b) -> refresh());
        statusFilterComboBox.valueProperty().addListener((o, a, b) -> refresh());

        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        typeCol.setCellValueFactory(new PropertyValueFactory<>("type"));
        severityCol.setCellValueFactory(new PropertyValueFactory<>("severity"));
        locationCol.setCellValueFactory(new PropertyValueFactory<>("location"));
        priorityCol.setCellValueFactory(new PropertyValueFactory<>("priority"));
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        reporterCol.setCellValueFactory(new PropertyValueFactory<>("reporterName"));
        createdCol.setCellValueFactory(new PropertyValueFactory<>("timestamp"));
        updatedCol.setCellValueFactory(new PropertyValueFactory<>("updatedAt"));
        ViewUtil.priorityBadges(priorityCol);
        historyTable.setItems(tableData);
        historyTable.getSelectionModel().selectedItemProperty().addListener((o, a, r) ->
                detailArea.setText(r == null ? "" : String.format(
                        "Report DRS-%d%nType: %s   Severity: %s   Priority: %d   Status: %s%nLocation: %s%n"
                                + "Reporter: %s (%s)   Submitted by account: %s%nCreated: %s   Last updated: %s%n%n%s",
                        r.getId(), r.getType(), r.getSeverity(), r.getPriority(), r.getStatus(), r.getLocation(),
                        r.getReporterName(), r.getReporterPhone(),
                        r.getReportedBy() == null ? "imported from DRS-Initial" : r.getReportedBy(),
                        r.getTimestamp(), r.getUpdatedAt(), r.getDescription())));
        refresh();
    }

    @Override
    public void refresh() {
        try {
            DisasterType type = ALL.equals(typeFilterComboBox.getValue()) ? null
                    : DisasterType.valueOf(typeFilterComboBox.getValue());
            DisasterStatus status = ALL.equals(statusFilterComboBox.getValue()) ? null
                    : DisasterStatus.valueOf(statusFilterComboBox.getValue());
            tableData.setAll(ClientContext.getClient().filterReports(type, status));
            countLabel.setText(tableData.size() + " record(s)");
        } catch (ClientException e) {
            ViewUtil.error(statusLabel, e.getMessage());
        }
    }

    @FXML
    private void handleReset() {
        typeFilterComboBox.getSelectionModel().selectFirst();
        statusFilterComboBox.getSelectionModel().selectFirst();
        refresh();
    }
}
