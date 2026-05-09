package com.jbaker454;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;

public class EmployeeViewController {

    @FXML
    private Label welcomeLabel;

    @FXML
    private TextArea statusTextArea;

    @FXML
    private Button clockInButton;

    @FXML
    private Button clockOutButton;

    @FXML
    private Button viewProfileButton;

    private Integer employeeId;

    @FXML
    private void initialize() {
        AuthService.User currentUser = AuthService.getCurrentUser();
        if (currentUser == null || currentUser.employeeId == null) {
            welcomeLabel.setText("Employee session not found.");
            statusTextArea.setText("Unable to load your profile. Please log in again.");
            clockInButton.setDisable(true);
            clockOutButton.setDisable(true);
            viewProfileButton.setDisable(true);
            return;
        }

        employeeId = currentUser.employeeId;
        welcomeLabel.setText("Welcome back, " + currentUser.username + "!");
        viewProfileButton.setDisable(false);
        refreshStatus();
    }

    @FXML
    private void handleClockIn(ActionEvent event) {
        if (employeeId == null) {
            statusTextArea.setText("Unable to clock in: missing employee ID.");
            return;
        }
        String message = EmployeeService.clockIn(employeeId);
        statusTextArea.setText(message + "\n\n" + EmployeeService.getDailySummary(employeeId));
        refreshStatus();
    }

    @FXML
    private void handleClockOut(ActionEvent event) {
        if (employeeId == null) {
            statusTextArea.setText("Unable to clock out: missing employee ID.");
            return;
        }
        String message = EmployeeService.clockOut(employeeId);
        statusTextArea.setText(message + "\n\n" + EmployeeService.getDailySummary(employeeId));
        refreshStatus();
    }

    @FXML
    private void handleCalculatePay(ActionEvent event) {
        if (employeeId == null) {
            statusTextArea.setText("Unable to calculate pay: missing employee ID.");
            return;
        }
        statusTextArea.setText(EmployeeService.calculatePay(employeeId));
    }

    @FXML
    private void handleViewProfile(ActionEvent event) {
        if (employeeId == null) {
            statusTextArea.setText("Unable to load profile: missing employee ID.");
            return;
        }
        statusTextArea.setText(EmployeeService.getProfileSummary(employeeId));
    }

    @FXML
    private void handleViewSchedule(ActionEvent event) {
        statusTextArea.setText("Schedule functionality is not available yet.\nPlease contact HR for your current shift schedule.");
    }

    private void refreshStatus() {
        statusTextArea.setText(EmployeeService.getDailySummary(employeeId));
        boolean currentlyClockedIn = EmployeeService.isCurrentlyClockedIn(employeeId);
        clockInButton.setDisable(currentlyClockedIn);
        clockOutButton.setDisable(!currentlyClockedIn);
    }
}
