package drs.client.controller;

import drs.client.ClientContext;
import drs.client.service.ClientException;
import drs.common.model.AuditEntry;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

/**
 * "Audit Log" tab (administrators only): the time-stamped, append-only record of
 * every state-changing action and its signature verification result.
 */
public class AuditController implements RefreshableView {

    private final ObservableList<AuditEntry> data = FXCollections.observableArrayList();

    @FXML private TableView<AuditEntry> auditTable;
    @FXML private TableColumn<AuditEntry, Integer> aIdCol;
    @FXML private TableColumn<AuditEntry, String> aTimeCol;
    @FXML private TableColumn<AuditEntry, String> aUserCol;
    @FXML private TableColumn<AuditEntry, String> aActionCol;
    @FXML private TableColumn<AuditEntry, String> aEntityCol;
    @FXML private TableColumn<AuditEntry, Integer> aEntityIdCol;
    @FXML private TableColumn<AuditEntry, String> aHashCol;
    @FXML private TableColumn<AuditEntry, String> aSigCol;
    @FXML private Label countLabel;
    @FXML private Label statusLabel;

    @FXML
    public void initialize() {
        aIdCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        aTimeCol.setCellValueFactory(new PropertyValueFactory<>("loggedAt"));
        aUserCol.setCellValueFactory(new PropertyValueFactory<>("username"));
        aActionCol.setCellValueFactory(new PropertyValueFactory<>("action"));
        aEntityCol.setCellValueFactory(new PropertyValueFactory<>("entity"));
        aEntityIdCol.setCellValueFactory(new PropertyValueFactory<>("entityId"));
        aHashCol.setCellValueFactory(new PropertyValueFactory<>("shortHash"));
        aSigCol.setCellValueFactory(new PropertyValueFactory<>("signatureStatus"));
        ViewUtil.colouredBadges(aSigCol, s -> switch (s) {
            case "Verified" -> "#27ae60";
            case "INVALID" -> "#c0392b";
            default -> "#7f8c8d";
        });
        auditTable.setItems(data);
        refresh();
    }

    @Override
    public void refresh() {
        try {
            data.setAll(ClientContext.getClient().getAuditLog());
            long signed = data.stream().filter(AuditEntry::isSigned).count();
            long rejected = data.stream().filter(e -> e.isSigned() && !e.isVerified()).count();
            countLabel.setText(String.format("%d most recent entries  |  %d signed  |  %d rejected signatures",
                    data.size(), signed, rejected));
        } catch (ClientException e) {
            ViewUtil.error(statusLabel, e.getMessage());
        }
    }

    @FXML
    private void handleRefresh() {
        refresh();
    }
}
