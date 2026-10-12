package drs.client.controller;

import drs.client.ClientContext;
import drs.client.service.ClientException;
import drs.common.model.DisasterReport;
import drs.common.model.DisasterStatus;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

/**
 * "Assess & Prioritise" tab (UC-2). Controllers work through the server-side
 * triage queue, review details, override priority and move reports forward.
 */
public class AssessDisasterController implements RefreshableView {

    private final ObservableList<DisasterReport> tableData = FXCollections.observableArrayList();

    @FXML private TableView<DisasterReport> reportsTable;
    @FXML private TableColumn<DisasterReport, Integer> idCol;
    @FXML private TableColumn<DisasterReport, String> typeCol;
    @FXML private TableColumn<DisasterReport, String> severityCol;
    @FXML private TableColumn<DisasterReport, String> locationCol;
    @FXML private TableColumn<DisasterReport, Integer> priorityCol;
    @FXML private TableColumn<DisasterReport, String> statusCol;
    @FXML private TableColumn<DisasterReport, String> reporterCol;
    @FXML private TableColumn<DisasterReport, String> timestampCol;
    @FXML private Label queueLabel;
    @FXML private Label detailIdLabel;
    @FXML private Label detailTypeLabel;
    @FXML private Label detailSeverityLabel;
    @FXML private Label detailLocationLabel;
    @FXML private Label detailReporterLabel;
    @FXML private Label detailDescLabel;
    @FXML private Label detailTimeLabel;
    @FXML private TextField priorityOverrideField;
    @FXML private Button startAssessmentBtn;
    @FXML private Button respondingBtn;
    @FXML private Button markResolvedBtn;
    @FXML private Button overridePriorityBtn;
    @FXML private Label statusLabel;

    private DisasterReport selected;

    @FXML
    public void initialize() {
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        typeCol.setCellValueFactory(new PropertyValueFactory<>("type"));
        severityCol.setCellValueFactory(new PropertyValueFactory<>("severity"));
        locationCol.setCellValueFactory(new PropertyValueFactory<>("location"));
        priorityCol.setCellValueFactory(new PropertyValueFactory<>("priority"));
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        reporterCol.setCellValueFactory(new PropertyValueFactory<>("reporterName"));
        timestampCol.setCellValueFactory(new PropertyValueFactory<>("timestamp"));
        ViewUtil.priorityBadges(priorityCol);
        reportsTable.setItems(tableData);
        reportsTable.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> onSelected(b));
        onSelected(null);
        refresh();
    }

    @Override
    public void refresh() {
        try {
            int keep = selected == null ? -1 : selected.getId();
            tableData.setAll(ClientContext.getClient().getReports());
            long waiting = tableData.stream().filter(r -> r.getStatus() == DisasterStatus.REPORTED).count();
            queueLabel.setText("Triage queue: " + waiting + " report(s) awaiting assessment");
            tableData.stream().filter(r -> r.getId() == keep).findFirst()
                    .ifPresent(r -> reportsTable.getSelectionModel().select(r));
        } catch (ClientException e) {
            ViewUtil.error(statusLabel, e.getMessage());
        }
    }

    @FXML
    private void handleNextInQueue() {
        try {
            DisasterReport next = ClientContext.getClient().nextInTriage();
            if (next == null) {
                ViewUtil.success(statusLabel, ClientContext.getClient().getLastMessage());
                return;
            }
            tableData.stream().filter(r -> r.getId() == next.getId()).findFirst().ifPresent(r -> {
                reportsTable.getSelectionModel().select(r);
                reportsTable.scrollTo(r);
            });
            ViewUtil.success(statusLabel, "Most urgent: report #" + next.getId() + ". "
                    + ClientContext.getClient().getLastMessage());
        } catch (ClientException e) {
            ViewUtil.error(statusLabel, e.getMessage());
        }
    }

    @FXML private void handleStartAssessment() { changeStatus(DisasterStatus.ASSESSING); }
    @FXML private void handleMarkResponding() { changeStatus(DisasterStatus.RESPONDING); }

    @FXML
    private void handleMarkResolved() {
        if (selected != null && ViewUtil.confirm("Resolve incident",
                "Mark report #" + selected.getId() + " as resolved? Active warnings will be cancelled.")) {
            changeStatus(DisasterStatus.RESOLVED);
        }
    }

    private void changeStatus(DisasterStatus status) {
        if (selected == null) {
            return;
        }
        try {
            ClientContext.getClient().updateStatus(selected.getId(), status);
            ViewUtil.success(statusLabel, ClientContext.getClient().getLastMessage());
            refresh();
        } catch (ClientException e) {
            ViewUtil.error(statusLabel, e.getMessage());
        }
    }

    @FXML
    private void handleOverridePriority() {
        if (selected == null) {
            return;
        }
        try {
            int p = Integer.parseInt(priorityOverrideField.getText().trim());
            if (!DisasterReport.isValidPriority(p)) {
                ViewUtil.error(statusLabel, "Priority must be between 1 and 100.");
                return;
            }
            ClientContext.getClient().updatePriority(selected.getId(), p);
            ViewUtil.success(statusLabel, ClientContext.getClient().getLastMessage());
            refresh();
        } catch (NumberFormatException e) {
            ViewUtil.error(statusLabel, "Priority must be a whole number.");
        } catch (ClientException e) {
            ViewUtil.error(statusLabel, e.getMessage());
        }
    }

    private void onSelected(DisasterReport r) {
        selected = r;
        boolean none = r == null;
        startAssessmentBtn.setDisable(none || r.getStatus() != DisasterStatus.REPORTED);
        respondingBtn.setDisable(none || r.getStatus() != DisasterStatus.ASSESSING);
        markResolvedBtn.setDisable(none || r.getStatus() == DisasterStatus.RESOLVED);
        overridePriorityBtn.setDisable(none || r.getStatus() == DisasterStatus.RESOLVED);
        if (none) {
            for (Label l : new Label[]{detailIdLabel, detailTypeLabel, detailSeverityLabel, detailLocationLabel,
                    detailReporterLabel, detailDescLabel, detailTimeLabel}) {
                l.setText("-");
            }
            priorityOverrideField.clear();
            return;
        }
        detailIdLabel.setText("DRS-" + r.getId());
        detailTypeLabel.setText(r.getType().toString());
        detailSeverityLabel.setText(r.getSeverity().toString());
        detailLocationLabel.setText(r.getLocation());
        detailReporterLabel.setText(r.getReporterName() + "  (" + r.getReporterPhone() + ")");
        detailDescLabel.setText(r.getDescription());
        detailTimeLabel.setText(r.getTimestamp() + "   (updated " + r.getUpdatedAt() + ")");
        priorityOverrideField.setText(String.valueOf(r.getPriority()));
    }
}
