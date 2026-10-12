package drs.client.controller;

import java.util.List;
import java.util.stream.Collectors;

import drs.client.ClientContext;
import drs.client.service.ClientException;
import drs.common.model.FieldTask;
import drs.common.model.Role;
import drs.common.model.TaskStatus;
import drs.common.model.TaskStatusUpdate;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;

/**
 * Enhanced Feature 2 - "Field Tasks" tab (UC-5). Responders see their own tasks
 * and progress them through the status state machine with signed, timestamped
 * updates; controllers see every task live, with its full history.
 */
public class FieldTaskController implements RefreshableView {

    private static final String ALL = "All open";

    private final ObservableList<FieldTask> taskData = FXCollections.observableArrayList();
    private final ObservableList<TaskStatusUpdate> historyData = FXCollections.observableArrayList();
    private List<FieldTask> allTasks = List.of();
    private boolean responderMode;

    @FXML private Label titleLabel;
    @FXML private ComboBox<String> filterComboBox;
    @FXML private TableView<FieldTask> taskTable;
    @FXML private TableColumn<FieldTask, Integer> taskIdCol;
    @FXML private TableColumn<FieldTask, Integer> reportCol;
    @FXML private TableColumn<FieldTask, String> disasterCol;
    @FXML private TableColumn<FieldTask, String> locationCol;
    @FXML private TableColumn<FieldTask, String> responderCol;
    @FXML private TableColumn<FieldTask, String> deptCol;
    @FXML private TableColumn<FieldTask, TaskStatus> taskStatusCol;
    @FXML private TableColumn<FieldTask, String> updatedCol;
    @FXML private Label instructionsLabel;
    @FXML private VBox updateBox;
    @FXML private ComboBox<TaskStatus> nextStatusComboBox;
    @FXML private TextField noteField;
    @FXML private Button updateButton;
    @FXML private TableView<TaskStatusUpdate> historyTable;
    @FXML private TableColumn<TaskStatusUpdate, String> hTimeCol;
    @FXML private TableColumn<TaskStatusUpdate, TaskStatus> hFromCol;
    @FXML private TableColumn<TaskStatusUpdate, TaskStatus> hToCol;
    @FXML private TableColumn<TaskStatusUpdate, String> hNoteCol;
    @FXML private TableColumn<TaskStatusUpdate, String> hByCol;
    @FXML private TableColumn<TaskStatusUpdate, String> hSigCol;
    @FXML private TextArea instructionsArea;
    @FXML private Label statusLabel;

    @FXML
    public void initialize() {
        responderMode = ClientContext.getUser() != null && ClientContext.getUser().getRole() == Role.RESPONDER;
        titleLabel.setText(responderMode ? "My Field Tasks" : "Field Task Tracking - all dispatched tasks");
        updateBox.setVisible(responderMode);
        updateBox.setManaged(responderMode);

        taskIdCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        reportCol.setCellValueFactory(new PropertyValueFactory<>("reportId"));
        disasterCol.setCellValueFactory(new PropertyValueFactory<>("disasterType"));
        locationCol.setCellValueFactory(new PropertyValueFactory<>("location"));
        responderCol.setCellValueFactory(new PropertyValueFactory<>("responderName"));
        deptCol.setCellValueFactory(new PropertyValueFactory<>("departmentName"));
        taskStatusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        updatedCol.setCellValueFactory(new PropertyValueFactory<>("updatedAt"));
        ViewUtil.colouredBadges(taskStatusCol, TaskStatus::getColourHex);
        taskTable.setItems(taskData);
        taskTable.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> onSelected(b));

        hTimeCol.setCellValueFactory(new PropertyValueFactory<>("updatedAt"));
        hFromCol.setCellValueFactory(new PropertyValueFactory<>("fromStatus"));
        hToCol.setCellValueFactory(new PropertyValueFactory<>("toStatus"));
        hNoteCol.setCellValueFactory(new PropertyValueFactory<>("note"));
        hByCol.setCellValueFactory(new PropertyValueFactory<>("updatedBy"));
        hSigCol.setCellValueFactory(new PropertyValueFactory<>("verification"));
        ViewUtil.colouredBadges(hToCol, TaskStatus::getColourHex);
        historyTable.setItems(historyData);

        filterComboBox.getItems().add(ALL);
        filterComboBox.getItems().add("All");
        for (TaskStatus s : TaskStatus.values()) {
            filterComboBox.getItems().add(s.getLabel());
        }
        filterComboBox.getSelectionModel().select(responderMode ? "All" : ALL);
        filterComboBox.valueProperty().addListener((o, a, b) -> applyFilter());
        onSelected(null);
        refresh();
    }

    @Override
    public void refresh() {
        try {
            int keep = selectedId();
            allTasks = responderMode ? ClientContext.getClient().getMyTasks() : ClientContext.getClient().getAllTasks();
            applyFilter();
            taskData.stream().filter(t -> t.getId() == keep).findFirst()
                    .ifPresent(t -> taskTable.getSelectionModel().select(t));
        } catch (ClientException e) {
            ViewUtil.error(statusLabel, e.getMessage());
        }
    }

    private int selectedId() {
        FieldTask t = taskTable.getSelectionModel().getSelectedItem();
        return t == null ? -1 : t.getId();
    }

    private void applyFilter() {
        String f = filterComboBox.getValue();
        taskData.setAll(allTasks.stream().filter(t -> f == null || "All".equals(f)
                        || (ALL.equals(f) && !t.getStatus().isTerminal()) || t.getStatus().getLabel().equals(f))
                .collect(Collectors.toList()));
    }

    private void onSelected(FieldTask t) {
        historyData.clear();
        nextStatusComboBox.getItems().clear();
        if (t == null) {
            instructionsArea.setText("");
            instructionsLabel.setText("Select a task to see its instructions and history.");
            updateButton.setDisable(true);
            return;
        }
        instructionsLabel.setText(String.format("Task #%d - %s at %s (report #%d) - %s",
                t.getId(), t.getDisasterType(), t.getLocation(), t.getReportId(), t.getStatus()));
        instructionsArea.setText(t.getInstructions() == null || t.getInstructions().isBlank()
                ? "(no instructions)" : t.getInstructions());
        nextStatusComboBox.getItems().setAll(t.getStatus().nextStatuses());
        nextStatusComboBox.getSelectionModel().selectFirst();
        updateButton.setDisable(t.getStatus().isTerminal());
        try {
            historyData.setAll(ClientContext.getClient().getTaskHistory(t.getId()));
        } catch (ClientException e) {
            ViewUtil.error(statusLabel, e.getMessage());
        }
    }

    @FXML
    private void handleUpdate() {
        FieldTask t = taskTable.getSelectionModel().getSelectedItem();
        TaskStatus next = nextStatusComboBox.getValue();
        if (t == null || next == null) {
            ViewUtil.error(statusLabel, "Select a task and its new status.");
            return;
        }
        if (next == TaskStatus.DECLINED && noteField.getText().trim().isEmpty()) {
            ViewUtil.error(statusLabel, "Please give a reason when declining a task.");
            return;
        }
        try {
            ClientContext.getClient().updateTask(new TaskStatusUpdate(t.getId(), next, noteField.getText().trim()));
            ViewUtil.success(statusLabel, ClientContext.getClient().getLastMessage() + " (signed and time-stamped)");
            noteField.clear();
            refresh();
        } catch (ClientException e) {
            ViewUtil.error(statusLabel, e.getMessage());
        }
    }

    @FXML
    private void handleRefresh() {
        refresh();
    }
}
