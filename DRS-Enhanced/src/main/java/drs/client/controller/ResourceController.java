package drs.client.controller;

import drs.client.ClientContext;
import drs.client.service.ClientException;
import drs.common.model.Department;
import drs.common.model.Responder;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

/**
 * "Resources" tab (A2 creative feature 2): live availability of departments and
 * responders, with manual availability toggles.
 */
public class ResourceController implements RefreshableView {

    private final ObservableList<Department> deptData = FXCollections.observableArrayList();
    private final ObservableList<Responder> respData = FXCollections.observableArrayList();

    @FXML private TableView<Department> departmentTable;
    @FXML private TableColumn<Department, Integer> dIdCol;
    @FXML private TableColumn<Department, String> dNameCol;
    @FXML private TableColumn<Department, String> dTypeCol;
    @FXML private TableColumn<Department, String> dContactCol;
    @FXML private TableColumn<Department, String> dPhoneCol;
    @FXML private TableColumn<Department, String> dStatusCol;
    @FXML private TableView<Responder> responderTable;
    @FXML private TableColumn<Responder, Integer> rIdCol;
    @FXML private TableColumn<Responder, String> rNameCol;
    @FXML private TableColumn<Responder, String> rRoleCol;
    @FXML private TableColumn<Responder, String> rDeptCol;
    @FXML private TableColumn<Responder, String> rLoginCol;
    @FXML private TableColumn<Responder, String> rStatusCol;
    @FXML private Label summaryLabel;
    @FXML private Label statusLabel;

    @FXML
    public void initialize() {
        dIdCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        dNameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        dTypeCol.setCellValueFactory(new PropertyValueFactory<>("type"));
        dContactCol.setCellValueFactory(new PropertyValueFactory<>("contactPerson"));
        dPhoneCol.setCellValueFactory(new PropertyValueFactory<>("phone"));
        dStatusCol.setCellValueFactory(new PropertyValueFactory<>("availabilityStatus"));
        rIdCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        rNameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        rRoleCol.setCellValueFactory(new PropertyValueFactory<>("role"));
        rDeptCol.setCellValueFactory(new PropertyValueFactory<>("departmentName"));
        rLoginCol.setCellValueFactory(new PropertyValueFactory<>("username"));
        rStatusCol.setCellValueFactory(new PropertyValueFactory<>("availabilityStatus"));
        ViewUtil.colouredBadges(dStatusCol, s -> "Available".equals(s) ? "#27ae60" : "#c0392b");
        ViewUtil.colouredBadges(rStatusCol, s -> "Available".equals(s) ? "#27ae60" : "#c0392b");
        departmentTable.setItems(deptData);
        responderTable.setItems(respData);
        refresh();
    }

    @Override
    public void refresh() {
        try {
            deptData.setAll(ClientContext.getClient().getDepartments());
            respData.setAll(ClientContext.getClient().getResponders());
            long dAvail = deptData.stream().filter(Department::isAvailable).count();
            long rAvail = respData.stream().filter(Responder::isAvailable).count();
            summaryLabel.setText(String.format("Departments available: %d / %d     Responders available: %d / %d",
                    dAvail, deptData.size(), rAvail, respData.size()));
        } catch (ClientException e) {
            ViewUtil.error(statusLabel, e.getMessage());
        }
    }

    @FXML
    private void handleToggleDepartment() {
        Department d = departmentTable.getSelectionModel().getSelectedItem();
        if (d == null) {
            ViewUtil.error(statusLabel, "Select a department first.");
            return;
        }
        try {
            ClientContext.getClient().toggleDepartment(d.getId());
            ViewUtil.success(statusLabel, ClientContext.getClient().getLastMessage());
            refresh();
        } catch (ClientException e) {
            ViewUtil.error(statusLabel, e.getMessage());
        }
    }

    @FXML
    private void handleToggleResponder() {
        Responder r = responderTable.getSelectionModel().getSelectedItem();
        if (r == null) {
            ViewUtil.error(statusLabel, "Select a responder first.");
            return;
        }
        try {
            ClientContext.getClient().toggleResponder(r.getId());
            ViewUtil.success(statusLabel, ClientContext.getClient().getLastMessage());
            refresh();
        } catch (ClientException e) {
            ViewUtil.error(statusLabel, e.getMessage());
        }
    }
}
