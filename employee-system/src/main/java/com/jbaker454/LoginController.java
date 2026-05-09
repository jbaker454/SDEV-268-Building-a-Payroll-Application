package com.jbaker454;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.scene.Node;

import java.io.IOException;

public class LoginController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label errorLabel;

    @FXML
    private void handleLogin(ActionEvent event) throws IOException {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Please fill all fields.");
            return;
        }

        AuthService.User user = AuthService.authenticate(username, password);
        if (user != null) {
            AuthService.setCurrentUser(user);
            String role = user.role == null ? "" : user.role.toUpperCase();
            String fxmlPath;
            String title;
            switch (role) {
                case "ADMIN" -> {
                    fxmlPath = "/fxml/AdminView.fxml";
                    title = "Admin Dashboard";
                }
                case "EMPLOYEE" -> {
                    fxmlPath = "/fxml/EmployeeView.fxml";
                    title = "Employee View";
                }
                case "HR" -> {
                    fxmlPath = "/fxml/HRView.fxml";
                    title = "HR Dashboard";
                }
                default -> {
                    errorLabel.setText("Unknown role assigned to user.");
                    return;
                }
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle(title);
        } else {
            errorLabel.setText("Invalid credentials.");
        }
    }
}