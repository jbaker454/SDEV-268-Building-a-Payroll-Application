package com.jbaker454;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.GridPane;

import java.util.Optional;

public class HRViewController {

    @FXML
    private TextField searchField;

    @FXML
    private ComboBox<String> searchTypeCombo;

    @FXML
    private TableView<EmployeeRecord> employeeTable;

    @FXML
    private TableColumn<EmployeeRecord, Integer> idColumn;

    @FXML
    private TableColumn<EmployeeRecord, String> nameColumn;

    @FXML
    private TableColumn<EmployeeRecord, String> departmentColumn;

    @FXML
    private TableColumn<EmployeeRecord, String> statusColumn;

    @FXML
    private TextArea detailsArea;

    @FXML
    private Button searchButton;

    @FXML
    private Button viewDetailsButton;

    @FXML
    private Button addEmployeeButton;

    @FXML
    private Button editEmployeeButton;

    @FXML
    private Button generateReportButton;

    @FXML
    private Button refreshButton;

    private ObservableList<EmployeeRecord> employeeList;

    @FXML
    private void initialize() {
        // Initialize search type combo box
        searchTypeCombo.setItems(FXCollections.observableArrayList("ID", "Name", "Department"));
        searchTypeCombo.setValue("Name");

        // Initialize employee list
        employeeList = FXCollections.observableArrayList();

        // Set up table columns
        idColumn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleIntegerProperty(cellData.getValue().employeeId).asObject());
        nameColumn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().name));
        departmentColumn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().department));
        statusColumn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().status));

        employeeTable.setItems(employeeList);

        // Set initial button states
        viewDetailsButton.setDisable(true);
        editEmployeeButton.setDisable(true);

        // Load all employees on initialization
        loadAllEmployees();

        // Listen for table selection
        employeeTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                viewDetailsButton.setDisable(false);
                editEmployeeButton.setDisable(false);
                displayEmployeeDetails(newVal.employeeId);
            } else {
                viewDetailsButton.setDisable(true);
                editEmployeeButton.setDisable(true);
                detailsArea.clear();
            }
        });
    }

    @FXML
    private void handleSearch(ActionEvent event) {
        String searchTerm = searchField.getText().trim();
        String searchType = searchTypeCombo.getValue();

        if (searchTerm.isEmpty()) {
            loadAllEmployees();
            return;
        }

        employeeList.clear();
        employeeList.addAll(EmployeeService.searchEmployees(searchTerm, searchType));

        if (employeeList.isEmpty()) {
            detailsArea.setText("No employees found matching your search criteria.");
        }
    }

    @FXML
    private void handleViewDetails(ActionEvent event) {
        EmployeeRecord selected = employeeTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            displayEmployeeDetails(selected.employeeId);
        }
    }

    private void displayEmployeeDetails(int employeeId) {
        String details = EmployeeService.getHREmployeeDetails(employeeId);
        detailsArea.setText(details);
    }

    @FXML
    private void handleAddEmployee(ActionEvent event) {
        showEmployeeDialog(null);
    }

    @FXML
    private void handleEditEmployee(ActionEvent event) {
        EmployeeRecord selected = employeeTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            showEmployeeDialog(selected.employeeId);
        }
    }

    @FXML
    private void handleGenerateReport(ActionEvent event) {
        String report = EmployeeService.generatePayrollReport();
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Payroll Report");
        alert.setHeaderText("Company Payroll Summary");
        alert.getDialogPane().setPrefWidth(600);
        alert.getDialogPane().setContent(new TextArea(report) {{
            setWrapText(true);
            setEditable(false);
            setPrefHeight(400);
        }});
        alert.showAndWait();
    }

    @FXML
    private void handleRefresh(ActionEvent event) {
        searchField.clear();
        loadAllEmployees();
        detailsArea.clear();
    }

    private void loadAllEmployees() {
        employeeList.clear();
        employeeList.addAll(EmployeeService.getAllEmployees());
    }

    private void showEmployeeDialog(Integer employeeId) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(employeeId == null ? "Add New Employee" : "Edit Employee");
        dialog.setHeaderText(employeeId == null ? "Enter new employee details" : "Modify employee details");

        // Create form fields
        TextField firstNameField = new TextField();
        TextField lastNameField = new TextField();
        TextField emailField = new TextField();
        TextField dateOfBirthField = new TextField();
        ComboBox<String> jobCombo = new ComboBox<>();
        TextField departmentField = new TextField();
        ComboBox<String> statusCombo = new ComboBox<>();
        ComboBox<String> payTypeCombo = new ComboBox<>();
        TextField addressLine1Field = new TextField();
        TextField addressLine2Field = new TextField();
        TextField cityField = new TextField();
        TextField stateField = new TextField();
        TextField zipField = new TextField();

        // Set up combobox options
        jobCombo.setItems(FXCollections.observableArrayList(EmployeeService.getAllJobTitles()));
        jobCombo.setPromptText("Select job title");
        departmentField.setEditable(false);
        departmentField.setDisable(true);
        departmentField.setPromptText("Department derived from job");
        statusCombo.setItems(FXCollections.observableArrayList("Active", "Inactive", "On Leave", "Terminated"));
        payTypeCombo.setItems(FXCollections.observableArrayList("Salary", "Hourly"));
        addressLine1Field.setPromptText("Street address");
        addressLine2Field.setPromptText("Apartment, suite, etc. (optional)");
        cityField.setPromptText("City");
        stateField.setPromptText("State (e.g., CA)");
        zipField.setPromptText("Zip code");

        jobCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.trim().isEmpty()) {
                departmentField.setText(EmployeeService.getDepartmentNameForJobTitle(newVal));
            } else {
                departmentField.clear();
            }
        });

        // If editing, load current employee data
        if (employeeId != null) {
            String details = EmployeeService.getEditableEmployeeData(employeeId);
            // Parse pipe-separated values: firstName|lastName|email|dateOfBirth|department|title|status|payType|line1|line2|city|state|zip
            String[] parts = details.split("\\|");
            if (parts.length == 13) {
                firstNameField.setText(parts[0]);
                lastNameField.setText(parts[1]);
                emailField.setText(parts[2]);
                dateOfBirthField.setText(parts[3]);
                jobCombo.setValue(parts[5]);
                departmentField.setText(parts[4]);
                statusCombo.setValue(parts[6]);
                payTypeCombo.setValue(parts[7]);
                addressLine1Field.setText(parts[8]);
                addressLine2Field.setText(parts[9]);
                cityField.setText(parts[10]);
                stateField.setText(parts[11]);
                zipField.setText(parts[12]);
            } else {
                showErrorAlert("Failed to load employee data for editing.");
                return;
            }
        }
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        grid.add(new Label("First Name:"), 0, 0);
        grid.add(firstNameField, 1, 0);
        grid.add(new Label("Last Name:"), 0, 1);
        grid.add(lastNameField, 1, 1);
        grid.add(new Label("Email:"), 0, 2);
        grid.add(emailField, 1, 2);
        grid.add(new Label("Date of Birth:"), 0, 3);
        grid.add(dateOfBirthField, 1, 3);
        grid.add(new Label("Job Title:"), 0, 4);
        grid.add(jobCombo, 1, 4);
        grid.add(new Label("Department:"), 0, 5);
        grid.add(departmentField, 1, 5);
        grid.add(new Label("Status:"), 0, 6);
        grid.add(statusCombo, 1, 6);
        grid.add(new Label("Pay Type:"), 0, 7);
        grid.add(payTypeCombo, 1, 7);
        grid.add(new Label("Address Line 1:"), 0, 8);
        grid.add(addressLine1Field, 1, 8);
        grid.add(new Label("Address Line 2:"), 0, 9);
        grid.add(addressLine2Field, 1, 9);
        grid.add(new Label("City:"), 0, 10);
        grid.add(cityField, 1, 10);
        grid.add(new Label("State:"), 0, 11);
        grid.add(stateField, 1, 11);
        grid.add(new Label("Zip:"), 0, 12);
        grid.add(zipField, 1, 12);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.addEventFilter(ActionEvent.ACTION, event -> {
            if (firstNameField.getText().trim().isEmpty() || lastNameField.getText().trim().isEmpty()
                    || emailField.getText().trim().isEmpty() || dateOfBirthField.getText().trim().isEmpty()
                    || jobCombo.getValue() == null || statusCombo.getValue() == null || payTypeCombo.getValue() == null) {
                showErrorAlert("Please complete all required fields, including Job Title, Status, and Pay Type.");
                event.consume();
            }
        });

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            // Save employee data
            if (employeeId == null) {
                // Add new employee
                String message = EmployeeService.addEmployee(
                    firstNameField.getText(),
                    lastNameField.getText(),
                    emailField.getText(),
                    dateOfBirthField.getText(),
                    null,
                    jobCombo.getValue(),
                    statusCombo.getValue(),
                    payTypeCombo.getValue(),
                    addressLine1Field.getText(),
                    addressLine2Field.getText(),
                    cityField.getText(),
                    stateField.getText(),
                    zipField.getText()
                );
                showInfoAlert(message);
            } else {
                // Update existing employee
                String message = EmployeeService.updateEmployee(
                    employeeId,
                    firstNameField.getText(),
                    lastNameField.getText(),
                    emailField.getText(),
                    dateOfBirthField.getText(),
                    null,
                    jobCombo.getValue(),
                    statusCombo.getValue(),
                    payTypeCombo.getValue(),
                    addressLine1Field.getText(),
                    addressLine2Field.getText(),
                    cityField.getText(),
                    stateField.getText(),
                    zipField.getText()
                );
                showInfoAlert(message);
            }
            loadAllEmployees();
            employeeTable.getSelectionModel().clearSelection();
            detailsArea.clear();
        }
    }

    private void showInfoAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Operation Result");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showErrorAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Validation Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Inner class to represent an employee record for the table view
     */
    public static class EmployeeRecord {
        public int employeeId;
        public String name;
        public String department;
        public String status;

        public EmployeeRecord(int employeeId, String name, String department, String status) {
            this.employeeId = employeeId;
            this.name = name;
            this.department = department;
            this.status = status;
        }
    }
}
