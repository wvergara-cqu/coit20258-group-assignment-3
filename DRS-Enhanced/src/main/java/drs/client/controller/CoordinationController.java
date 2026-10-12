package drs.client.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import drs.client.ClientContext;
import drs.client.service.ClientException;
import drs.common.model.Department;
import drs.common.model.DisasterReport;
import drs.common.model.DisasterResponse;
import drs.common.model.DisasterStatus;
import drs.common.model.Responder;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.StringConverter;

/**
 * "Coordinate Response" tab (UC-3). Shows recommended agencies for the selected
 * incident, lets the controller choose departments and responders, and saves a
 * signed response which dispatches one field task per responder.
 */
public class CoordinationController implements RefreshableView {

    private final ObservableList<DisasterResponse> responsesData = FXCollections.observableArrayList();
    private List<Responder> availableResponders = new ArrayList<>();
    private List<Department> recommended = new ArrayList<>();

    @FXML private ComboBox<DisasterReport> disasterComboBox;
    @FXML private Label disasterDetailLabel;
    @FXML private Label recommendedLabel;
    @FXML private ListView<Department> departmentListView;
    @FXML private ListView<Responder> responderListView;
    @FXML private CheckBox chkWarningEvacuation;
    @FXML private CheckBox chkSearchRescue;
    @FXML private CheckBox chkImmediateAssistance;
    @FXML private CheckBox chkAssessingDamage;
    @FXML private CheckBox chkContinuingAssistance;
    @FXML private CheckBox chkRestoration;
    @FXML private TextArea notesArea;
    @FXML private Label statusLabel;
    @FXML private TableView<DisasterResponse> responsesTable;
    @FXML private TableColumn<DisasterResponse, Integer> respIdCol;
    @FXML private TableColumn<DisasterResponse, Integer> respDisasterCol;
    @FXML private TableColumn<DisasterResponse, String> respDeptCol;
    @FXML private TableColumn<DisasterResponse, Integer> respPersonCountCol;
    @FXML private TableColumn<DisasterResponse, String> respByCol;
    @FXML private TableColumn<DisasterResponse, String> respCreatedCol;

    @FXML
    public void initialize() {
        departmentListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        responderListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        disasterComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(DisasterReport r) {
                return r == null ? "" : r.getSummary() + " - " + r.getStatus();
            }

            @Override
            public DisasterReport fromString(String s) {
                return null;
            }
        });
        departmentListView.getSelectionModel().getSelectedItems()
                .addListener((ListChangeListener<Department>) c -> filterResponders());
        disasterComboBox.valueProperty().addListener((o, a, b) -> onDisasterSelected(b));

        respIdCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        respDisasterCol.setCellValueFactory(new PropertyValueFactory<>("reportId"));
        respDeptCol.setCellValueFactory(new PropertyValueFactory<>("departmentNames"));
        respPersonCountCol.setCellValueFactory(new PropertyValueFactory<>("responderCount"));
        respByCol.setCellValueFactory(new PropertyValueFactory<>("createdBy"));
        respCreatedCol.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
        responsesTable.setItems(responsesData);
        refresh();
    }

    @Override
    public void refresh() {
        try {
            DisasterReport keep = disasterComboBox.getValue();
            List<DisasterReport> open = ClientContext.getClient().getReports().stream()
                    .filter(r -> r.getStatus() != DisasterStatus.RESOLVED).collect(Collectors.toList());
            disasterComboBox.setItems(FXCollections.observableArrayList(open));
            if (keep != null) {
                open.stream().filter(r -> r.getId() == keep.getId()).findFirst()
                        .ifPresent(r -> disasterComboBox.getSelectionModel().select(r));
            }
            if (disasterComboBox.getValue() == null && !open.isEmpty()) {
                disasterComboBox.getSelectionModel().selectFirst();
            }
            loadResources();
            responsesData.setAll(ClientContext.getClient().getResponses());
        } catch (ClientException e) {
            ViewUtil.error(statusLabel, e.getMessage());
        }
    }

    private void loadResources() throws ClientException {
        departmentListView.setItems(FXCollections.observableArrayList(
                ClientContext.getClient().getAssignableDepartments()));
        availableResponders = ClientContext.getClient().getResponders().stream()
                .filter(Responder::isAvailable).collect(Collectors.toList());
        filterResponders();
    }

    private void onDisasterSelected(DisasterReport r) {
        if (r == null) {
            disasterDetailLabel.setText("No incident selected.");
            recommendedLabel.setText("");
            return;
        }
        disasterDetailLabel.setText(String.format("Type: %s  |  Severity: %s  |  Location: %s  |  Priority: %d",
                r.getType(), r.getSeverity(), r.getLocation(), r.getPriority()));
        try {
            recommended = ClientContext.getClient().getRecommendedDepartments(r.getId());
            recommendedLabel.setText("Recommended agencies: "
                    + recommended.stream().map(Department::getName).collect(Collectors.joining(", ")));
        } catch (ClientException e) {
            recommendedLabel.setText(e.getMessage());
        }
    }

    @FXML
    private void handleSelectRecommended() {
        departmentListView.getSelectionModel().clearSelection();
        for (Department d : recommended) {
            int idx = departmentListView.getItems().indexOf(d);
            if (idx >= 0) {
                departmentListView.getSelectionModel().select(idx);
            }
        }
        if (departmentListView.getSelectionModel().getSelectedItems().isEmpty()) {
            ViewUtil.error(statusLabel, "None of the recommended agencies currently has available responders.");
        }
    }

    private void filterResponders() {
        List<Department> chosen = departmentListView.getSelectionModel().getSelectedItems();
        List<Responder> shown = chosen.isEmpty() ? availableResponders
                : availableResponders.stream().filter(r -> chosen.contains(r.getDepartment()))
                .collect(Collectors.toList());
        responderListView.setItems(FXCollections.observableArrayList(shown));
    }

    @FXML
    private void handleSaveResponse() {
        DisasterReport r = disasterComboBox.getValue();
        if (r == null) {
            ViewUtil.error(statusLabel, "Please select an incident first.");
            return;
        }
        List<Department> depts = new ArrayList<>(departmentListView.getSelectionModel().getSelectedItems());
        List<Responder> people = new ArrayList<>(responderListView.getSelectionModel().getSelectedItems());
        if (depts.isEmpty() && people.isEmpty()) {
            ViewUtil.error(statusLabel, "Select at least one department or responder.");
            return;
        }
        DisasterResponse resp = new DisasterResponse(0, r.getId(), depts, people, buildNotes());
        try {
            ClientContext.getClient().saveResponse(resp);
            ViewUtil.success(statusLabel, ClientContext.getClient().getLastMessage());
            clearActivities();
            refresh();
        } catch (ClientException e) {
            ViewUtil.error(statusLabel, e.getMessage());
        }
    }

    private String buildNotes() {
        StringBuilder sb = new StringBuilder();
        CheckBox[] boxes = {chkWarningEvacuation, chkSearchRescue, chkImmediateAssistance,
                chkAssessingDamage, chkContinuingAssistance, chkRestoration};
        for (CheckBox b : boxes) {
            if (b.isSelected()) {
                sb.append("- ").append(b.getText()).append('\n');
            }
        }
        String extra = notesArea.getText().trim();
        if (!extra.isEmpty()) {
            sb.append(extra);
        }
        return sb.toString().trim();
    }

    private void clearActivities() {
        for (CheckBox b : new CheckBox[]{chkWarningEvacuation, chkSearchRescue, chkImmediateAssistance,
                chkAssessingDamage, chkContinuingAssistance, chkRestoration}) {
            b.setSelected(false);
        }
        notesArea.clear();
    }
}
