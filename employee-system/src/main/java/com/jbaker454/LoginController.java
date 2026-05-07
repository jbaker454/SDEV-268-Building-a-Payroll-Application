package com.jbaker454;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private ComboBox<String> roleComboBox;

    @FXML
    private Label errorLabel;

    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        String role = roleComboBox.getValue();

        if (username.isEmpty() || password.isEmpty() || role == null) {
            errorLabel.setText("Please fill all fields.");
            return;
        }

        AuthService.User user = AuthService.authenticate(username, password);
        if (user != null && user.role.equals(role)) {
            errorLabel.setText("Login successful! Welcome " + user.role);
            // TODO: Navigate to role-specific view
        } else {
            errorLabel.setText("Invalid credentials.");
        }
    }
}