package com.atmos.controller;

import com.atmos.model.Storage;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import static com.atmos.model.MainApp.getStorage;
import static com.atmos.model.MainApp.setNewScene;

public class SetupController {

    @FXML private VBox container;

    @FXML private CheckBox checkBox;
    @FXML private TextField textField;
    @FXML private PasswordField passwordField;

    private Storage storage;

    public SetupController() {
        storage = getStorage();
    }

    @FXML
    private void handleNewPasswordButton(ActionEvent actionEvent) {
        try {
            setPASSWORD();
            addStyleClass("finished");
        } catch (IllegalArgumentException iae) {
            addStyleClass("input-error");
        }
    }

    @FXML
    private void handleBackButton(ActionEvent actionEvent) {
        deletePASSWORD();
        // cleaning up
        removeStyleClass("finished");
        removeStyleClass("input-error");
    }

    @FXML
    private void handleCheckBox(ActionEvent actionEvent) {
        if (checkBox.isSelected()) {
            addStyleClass("show-password");
            textField.setText(passwordField.getText());
            passwordField.setText("");
        } else {
            removeStyleClass("show-password");
            passwordField.setText(textField.getText());
            textField.setText("");
        }
    }

    @FXML
    private void handleContinueButton(ActionEvent actionEvent) {
        // Setting scene 2 as the new scene for the application
        setNewScene(1);
    }

    private void setPASSWORD() throws IllegalArgumentException {
        if (textField.getText().isBlank() && passwordField.getText().isBlank())
            throw new IllegalArgumentException("Password input fields must not be empty");
        storage.setPASSWORD(!passwordField.getText().isEmpty() ? passwordField.getText() : textField.getText());
    }

    private void deletePASSWORD() {
        storage.setPASSWORD("");
        textField.setText("");
        passwordField.setText("");
    }

    private void addStyleClass(String arg) {
        if (!container.getStyleClass().contains(arg)) container.getStyleClass().add(arg);
    }

    private void removeStyleClass(String arg) {
        container.getStyleClass().remove(arg);
    }
}
