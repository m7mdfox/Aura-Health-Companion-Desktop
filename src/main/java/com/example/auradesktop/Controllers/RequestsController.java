package com.example.auradesktop.Controllers;

import com.example.auradesktop.models.RequestModel;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.util.Callback;

import java.net.URL;
import java.util.ResourceBundle;

public class RequestsController implements Initializable {

    @FXML private TableView<RequestModel> requestsTable;
    @FXML private TableColumn<RequestModel, String> nameColumn;
    @FXML private TableColumn<RequestModel, String> timeColumn;

    // This column holds Void because it doesn't display text, it displays nodes (buttons)
    @FXML private TableColumn<RequestModel, Void> statusColumn;

    // The list that holds your data
    ObservableList<RequestModel> requestList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        loadData();
    }

    private void loadData() {
        // 1. Set up standard text columns
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        timeColumn.setCellValueFactory(new PropertyValueFactory<>("time"));

        // 2. Add dummy data to test
        requestList.add(new RequestModel("Alice Blue", "10:30 AM"));
        requestList.add(new RequestModel("Mark Dark", "12:00 PM"));
        requestList.add(new RequestModel("Sarah Sky", "02:15 PM"));

        requestsTable.setItems(requestList);

        // 3. Create the Custom Button Cell
        addButtonToTable();
    }

    private void addButtonToTable() {
        Callback<TableColumn<RequestModel, Void>, TableCell<RequestModel, Void>> cellFactory = new Callback<>() {
            @Override
            public TableCell<RequestModel, Void> call(final TableColumn<RequestModel, Void> param) {
                return new TableCell<>() {

                    private final Button acceptBtn = new Button("Accept");
                    private final Button rejectBtn = new Button("Reject");
                    private final HBox pane = new HBox(10, acceptBtn, rejectBtn); // 10 is spacing between buttons

                    {
                        // Apply the CSS classes we created earlier
                        acceptBtn.getStyleClass().add("btn-accept");
                        rejectBtn.getStyleClass().add("btn-reject");

                        // --- ACCEPT LOGIC ---
                        acceptBtn.setOnAction(event -> {
                            RequestModel data = getTableView().getItems().get(getIndex());
                            System.out.println("Accepted request from: " + data.getName());
                            // Add logic here (e.g., update database)
                        });

                        // --- REJECT LOGIC ---
                        rejectBtn.setOnAction(event -> {
                            RequestModel data = getTableView().getItems().get(getIndex());

                            // Remove from table
                            getTableView().getItems().remove(data);

                            System.out.println("Rejected request from: " + data.getName());
                        });
                    }

                    @Override
                    protected void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            setGraphic(pane);
                        }
                    }
                };
            }
        };

        statusColumn.setCellFactory(cellFactory);
    }
}