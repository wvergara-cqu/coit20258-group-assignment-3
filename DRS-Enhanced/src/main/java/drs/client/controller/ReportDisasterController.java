package drs.client.controller;

import drs.client.ClientContext;
import drs.client.service.ClientException;
import drs.common.model.DisasterReport;
import drs.common.model.DisasterType;
import drs.common.model.Severity;
import drs.common.util.InputValidator;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

/**
 * "Report Disaster" tab (UC-1). Validates input on the client for immediate
 * feedback, signs the report and submits it; the server re-validates, stores it
 * in MySQL and returns a tracking ID.
 */
public class ReportDisasterController implements RefreshableView {

    private final ObservableList<DisasterReport> tableData = FXCollections.observableArrayList();

    @FXML private ComboBox<DisasterType> typeComboBox;
    @FXML private ComboBox<Severity> severityComboBox;
    @FXML private TextField locationField;
    @FXML private TextField reporterNameField;
    @FXML private TextField reporterPhoneField;
    @FXML private TextArea descriptionArea;
    @FXML private Label priorityPreviewLabel;
    @FXML private Label statusLabel;
    @FXML private TableView<DisasterReport> myReportsTable;
    @FXML private TableColumn<DisasterReport, Integer> idCol;
    @FXML private TableColumn<DisasterReport, String> typeCol;
    @FXML private TableColumn<DisasterReport, String> severityCol;
    @FXML private TableColumn<DisasterReport, String> locationCol;
    @FXML private TableColumn<DisasterReport, Integer> priorityCol;
    @FXML private TableColumn<DisasterReport, String> statusCol;
    @FXML private TableColumn<DisasterReport, String> timestampCol;

    @FXML
    public void initialize() {
        typeComboBox.setItems(FXCollections.observableArrayList(DisasterType.values()));
        severityComboBox.setItems(FXCollections.observableArrayList(Severity.values()));
        typeComboBox.getSelectionModel().selectFirst();
        severityComboBox.getSelectionModel().selectFirst();
        typeComboBox.valueProperty().addListener((o, a, b) -> updatePreview());
        severityComboBox.valueProperty().addListener((o, a, b) -> updatePreview());

        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        typeCol.setCellValueFactory(new PropertyValueFactory<>("type"));
        severityCol.setCellValueFactory(new PropertyValueFactory<>("severity"));
        locationCol.setCellValueFactory(new PropertyValueFactory<>("location"));
        priorityCol.setCellValueFactory(new PropertyValueFactory<>("priority"));
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        timestampCol.setCellValueFactory(new PropertyValueFactory<>("timestamp"));
        ViewUtil.priorityBadges(priorityCol);
        myReportsTable.setItems(tableData);

        if (ClientContext.getUser() != null) {
            reporterNameField.setText(ClientContext.getUser().getFullName());
        }
        updatePreview();
        refresh();
    }

    @Override
    public void refresh() {
        try {
            tableData.setAll(ClientContext.getClient().getMyReports());
        } catch (ClientException e) {
            ViewUtil.error(statusLabel, e.getMessage());
        }
    }

    private void updatePreview() {
        int p = DisasterReport.computePriority(typeComboBox.getValue(), severityComboBox.getValue());
        priorityPreviewLabel.setText("Estimated priority: " + p);
        priorityPreviewLabel.setStyle("-fx-text-fill: " + ViewUtil.priorityColour(p) + "; -fx-font-weight: bold;");
    }

    @FXML
    private void handleSubmit() {
        String err = validate();
        if (err != null) {
            ViewUtil.error(statusLabel, err);
            return;
        }
        DisasterReport report = new DisasterReport(0, typeComboBox.getValue(), severityComboBox.getValue(),
                locationField.getText().trim(), descriptionArea.getText().trim(),
                reporterNameField.getText().trim(), reporterPhoneField.getText().trim());
        try {
            ClientContext.getClient().submitReport(report);
            ViewUtil.success(statusLabel, ClientContext.getClient().getLastMessage());
            clearForm();
            refresh();
        } catch (ClientException e) {
            ViewUtil.error(statusLabel, e.getMessage());
        }
    }

    private String validate() {
        if (typeComboBox.getValue() == null) {
            return "Please select a disaster type.";
        }
        if (severityComboBox.getValue() == null) {
            return "Please select a severity level.";
        }
        String err = InputValidator.required("Location", locationField.getText(), InputValidator.MAX_SHORT);
        if (err == null) {
            err = InputValidator.required("Reporter name", reporterNameField.getText(), InputValidator.MAX_SHORT);
        }
        if (err == null) {
            err = InputValidator.phone(reporterPhoneField.getText());
        }
        if (err == null) {
            err = InputValidator.required("Description", descriptionArea.getText(), InputValidator.MAX_LONG);
        }
        return err;
    }

    @FXML
    private void handleClear() {
        clearForm();
        statusLabel.setText("");
    }

    private void clearForm() {
        typeComboBox.getSelectionModel().selectFirst();
        severityComboBox.getSelectionModel().selectFirst();
        locationField.clear();
        reporterPhoneField.clear();
        descriptionArea.clear();
    }
}
