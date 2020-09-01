package com.atmos.controller;

import com.atmos.model.CryptoException;
import com.atmos.model.MainApp;
import com.atmos.model.Storage;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.input.DragEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.VBox;
import javafx.scene.media.AudioClip;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import static com.atmos.model.Encryption.decryptFile;
import static com.atmos.model.Encryption.encryptFile;

public class MainController implements Initializable {
    @FXML private VBox container;
    @FXML private PasswordField passwordField;

    private IntegerProperty remainingTriesLeft;
    private StringProperty timerBeforeRetry;
    private Timeline timeline;
    private int time;

    private Map<String, List<File>> unTouchedFiles;
    private FileChooser fileChooser;
    private List<File> encFiles;
    private List<File> files;
    private Storage storage;

    private AudioClip accessGranted;
    private AudioClip fileDrop;
    private AudioClip operation;
    private AudioClip error;

    private boolean access;

    public MainController() {
        accessGranted = new AudioClip(getClass().getResource("/sound/WindowsStartUpSound.mp3").toExternalForm());
        fileDrop = new AudioClip(getClass().getResource("/sound/Click.mp3").toExternalForm());
        operation = new AudioClip(getClass().getResource("/sound/CarLock.mp3").toExternalForm());
        error = new AudioClip(getClass().getResource("/sound/Error.mp3").toExternalForm());
        unTouchedFiles = new HashMap<>();
        unTouchedFiles.put("cancellation", new ArrayList<>());
        unTouchedFiles.put("undecryptable", new ArrayList<>());
        unTouchedFiles.put("unencryptable", new ArrayList<>());
        unTouchedFiles.put("improper file extension", new ArrayList<>());
        unTouchedFiles.put("no file data", new ArrayList<>());
        unTouchedFiles.put("wrong password", new ArrayList<>());
        unTouchedFiles.put("nonexistent", new ArrayList<>());
        files = new ArrayList<>();
        encFiles = new ArrayList<>();
        storage = MainApp.getStorage();
        timerBeforeRetry = new SimpleStringProperty();
        remainingTriesLeft = new SimpleIntegerProperty(storage.getRemainingTries());
        access = false;
        calculateTime();
        setTimerBeforeRetry(time);
        fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("All Files (*.*)", "*.*"),
                new FileChooser.ExtensionFilter("Encrypted files (*.enc)", "*.enc"));
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        if (storage.getFailedTimes() > 1) {
            setRemainingTriesLeft(0);
            timerBeforeNewPasswordPrompt();
            addStyleClass("wrong-password");
            addStyleClass("too-many-attempts");
        } else if (storage.getRemainingTries() < 6) {
            addStyleClass("wrong-password");
        }
    }

    @FXML
    private void handlePassword(ActionEvent actionEvent) {
        access = checkForInputtedPassword();
        if (access) {
            accessGranted.play();
            storage.setFailedTimes(1);
            storage.setRemainingTries(6);
            timerBeforeNewPasswordPrompt();
            removeStyleClass("wrong-password");
            addStyleClass("access-granted");
        } else {
            addStyleClass("wrong-password");
            System.out.println("Wrong password dude!");
            try {
                decrementAvailableTries();
            } catch (IllegalStateException iae) {
                storage.setFailedTimes(storage.getFailedTimes() + 1);
                timerBeforeNewPasswordPrompt();
                addStyleClass("too-many-attempts");
            }
        }
        passwordField.setText("");
        actionEvent.consume();
    }

    // *** ENCRYPTION ***
    @FXML
    private void onDragEnteredEncryption(DragEvent dragEvent) {
        onDragEnteredVisuals(dragEvent);
    }

    @FXML
    private void onDragOverEncryption(DragEvent dragEvent) {
        onDragOverOperation(dragEvent);
    }

    @FXML
    private void onDragDroppedEncryption(DragEvent dragEvent) {
        removeStyleClass("show-decrypt-button");
        addStyleClass("show-encrypt-button");
        onDragDroppedOperation(dragEvent, files);
    }

    @FXML
    private void onDragExitedEncryption(DragEvent dragEvent) {
        onDragExitedVisuals(dragEvent);
    }

    @FXML
    private void encryptOperation(ActionEvent actionEvent) {
        fileChooser.setTitle("Save the encrypted file somewhere");
        fileChooser.setSelectedExtensionFilter(fileChooser.getExtensionFilters().get(1));
        pauseTimeline();
        for (File file : files) {
            if (file.exists()) {
                if (file.length() > 0) {
                    File tempFile = null;
                    try {
                        try {
                            tempFile = new File(storage.loadParentDirectory() + "/encrypted-file-data");
                            encryptFile(storage.getPASSWORD(), file, tempFile);
                            fileChooser.setInitialFileName(file + ".enc");
                            fileChooser.setInitialDirectory(file.getParentFile());
                            operation.play();
                            File newFile = fileChooser.showSaveDialog(container.getScene().getWindow());
                            if (newFile != null) {
                                Files.copy(tempFile.toPath(), newFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                                if (!newFile.setReadOnly())
                                    throw new IOException("Couldn't lock (set only readable) the newly saved file");
                            } else unTouchedFiles.get("cancellation").add(file);
                            if (!tempFile.delete()) throw new IOException("Couldn't delete a specific file");
                        } catch (CryptoException CE) {
                            unTouchedFiles.get("unencryptable").add(file);
                            if (!tempFile.delete()) throw new IOException("Couldn't delete a specific file");
                        }
                    } catch (IOException | URISyntaxException exc) {
                        MainApp.setNewException(exc);
                        MainApp.crashVisualsHandling();
                    }
                } else unTouchedFiles.get("no file data").add(file);
            } else unTouchedFiles.get("nonexistent").add(file);
        }
        playTimeline();
        if (doesUntouchedFilesHaveFiles()) {
            displayReasonForUntouchedFiles("encrypted");
            emptyValuesOfUnTouchedFilesMap();
        }
        files.clear();
        removeStyleClass("show-encrypt-button");
        if (!encFiles.isEmpty()) addStyleClass("show-decrypt-button");
        actionEvent.consume();
    }
    // *** END ENCRYPTION ***

    // *** DECRYPTION ***
    @FXML
    private void onDragEnteredDecryption(DragEvent dragEvent) {
        onDragEnteredVisuals(dragEvent);
    }

    @FXML
    private void onDragOverDecrpytion(DragEvent dragEvent) {
        onDragOverOperation(dragEvent);
    }

    @FXML
    private void onDragDroppedDecryption(DragEvent dragEvent) {
        removeStyleClass("show-encrypt-button");
        addStyleClass("show-decrypt-button");
        onDragDroppedOperation(dragEvent, encFiles);
    }

    @FXML
    private void onDragExitedDecryption(DragEvent dragEvent) {
        onDragExitedVisuals(dragEvent);
    }

    @FXML
    public void decryptOperation(ActionEvent actionEvent) {
        fileChooser.setTitle("Save the decrypted file somewhere");
        fileChooser.setSelectedExtensionFilter(fileChooser.getExtensionFilters().get(0));
        pauseTimeline();
        for (File encFile : encFiles) {
            if (encFile.exists()) {
                if (encFile.length() > 0) {
                    String encFileName = encFile.getName();
                    if (encFileName.endsWith(".enc")) {
                        File tempFile = null;
                        try {
                            try {
                                tempFile = new File(storage.loadParentDirectory() + "/decrypted-file-data");
                                decryptFile(storage.getPASSWORD(), encFile, tempFile);
                                fileChooser.setInitialFileName(encFileName.substring(0, encFileName.length() - 4));
                                fileChooser.setInitialDirectory(encFile.getParentFile());
                                operation.play();
                                File newFile = fileChooser.showSaveDialog(container.getScene().getWindow());
                                if (newFile != null)
                                    Files.copy(tempFile.toPath(), newFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                                else unTouchedFiles.get("cancellation").add(encFile);
                                if (!tempFile.delete()) throw new IOException("Couldn't delete a specific file");
                            } catch (CryptoException CE) {
                                if (CE.getCause().getClass().equals(BadPaddingException.class)) {
                                    unTouchedFiles.get("wrong password").add(encFile);
                                    if (!tempFile.delete()) throw new IOException("Couldn't delete a specific file");
                                } else if (CE.getCause().getClass().equals(CryptoException.class) || CE.getCause().getClass().equals(IllegalBlockSizeException.class)) {
                                    unTouchedFiles.get("undecryptable").add(encFile);
                                    if (!tempFile.delete()) throw new IOException("Couldn't delete a specific file");
                                } else {
                                    if (!tempFile.delete()) throw new IOException("Couldn't delete a specific file");
                                    throw CE;
                                }
                            }
                        } catch (IOException | CryptoException | URISyntaxException exc) {
                            MainApp.setNewException(exc);
                            MainApp.crashVisualsHandling();
                        }
                    } else unTouchedFiles.get("improper file extension").add(encFile);
                } else unTouchedFiles.get("no file data").add(encFile);
            } else unTouchedFiles.get("nonexistent").add(encFile);
        }
        playTimeline();
        if (doesUntouchedFilesHaveFiles()) {
            displayReasonForUntouchedFiles("decrypted");
            emptyValuesOfUnTouchedFilesMap();
        }
        encFiles.clear();
        removeStyleClass("show-decrypt-button");
        if (!files.isEmpty()) addStyleClass("show-encrypt-button");
        actionEvent.consume();
    }
    // *** END DECRYPTION ***

    private void emptyValuesOfUnTouchedFilesMap() {
        for (Map.Entry<String, List<File>> entry : unTouchedFiles.entrySet()) {
            unTouchedFiles.put(entry.getKey(), new ArrayList<>());
        }
    }

    private boolean doesUntouchedFilesHaveFiles() {
        for (Map.Entry<String, List<File>> entry : unTouchedFiles.entrySet()) {
            if (!entry.getValue().isEmpty()) return true;
        }
        return false;
    }

    private void displayReasonForUntouchedFiles(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);

        TextArea textArea = new TextArea();
        textArea.setText(produceUntouchedFilesMessage());
        textArea.setMinHeight(500);
        textArea.setMinWidth(1000);
        textArea.setEditable(false);

        VBox dialogPaneContent = new VBox();
        dialogPaneContent.getChildren().add(textArea);

        ((Stage) alert.getDialogPane().getScene().getWindow()).getIcons().add(new Image("/images/textFileLock.png"));
        alert.setTitle("Error message about: File operations (Encryption/decryption)");
        alert.setHeaderText(String.format("These following files couldn't be %s because:", message));
        alert.getDialogPane().setStyle("-fx-font-size: 16px;");
        alert.getDialogPane().setContent(dialogPaneContent);
        error.play();
        alert.showAndWait();
    }

    private String produceUntouchedFilesMessage() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, List<File>> kv : unTouchedFiles.entrySet()) {
            for (File file : kv.getValue()) {
                switch (kv.getKey()) {
                    case "cancellation":
                        sb.append(String.format("%s : You didn't specify where the new file should be saved%n%n", file));
                        break;
                    case "undecryptable":
                        sb.append(String.format("%s : The file contains undecryptable data%n%n", file));
                        break;
                    case "improper file extension":
                        sb.append(String.format("%s : The file does not have a \".enc\" file extension%n%n", file));
                        break;
                    case "no file data":
                        sb.append(String.format("%s : The file contains no data, it's empty so to speak xD%n%n", file));
                        break;
                    case "wrong password":
                        sb.append(String.format("%s : The file isn't decryptable because of WRONG PASSWORD%n%n", file));
                        break;
                    case "unencryptable":
                        sb.append(String.format("%s : The file can't be encrypted, either because of the content of the file or the file type%n%n", file));
                        break;
                    case "nonexistent":
                        sb.append(String.format("%s : The file doesn't exist at the place where it got dragged and dropped from, or it has been deleted", file));
                        break;
                }
            }
        }
        return sb.toString();
    }

    private void onDragEnteredVisuals(DragEvent dragEvent) {
        if (!access) System.out.println("You haven't gotten access dude!");
        else if (dragEvent.getDragboard().hasFiles()) addStyleClass("on-drag-entered");
        dragEvent.consume();
    }

    private void onDragOverOperation(DragEvent dragEvent) {
        if (access)
            if (dragEvent.getDragboard().hasFiles()) dragEvent.acceptTransferModes(TransferMode.MOVE);
        dragEvent.consume();
    }

    private void onDragDroppedOperation(DragEvent dragEvent, List<File> files) {
        fileDrop.play();
        gatherFiles(dragEvent.getDragboard().getFiles(), files);
        dragEvent.setDropCompleted(true);
        dragEvent.consume();
    }

    private void gatherFiles(List<File> userFiles, List<File> files) {
        for (File file : userFiles) {
            if (file.isDirectory()) {
                File[] listOfFiles = file.listFiles();
                if (listOfFiles != null) gatherFiles(Arrays.asList(listOfFiles), files);
            } else files.add(file);
        }
    }

    private void onDragExitedVisuals(DragEvent dragEvent) {
        removeStyleClass("on-drag-entered");
        dragEvent.consume();
    }

    public String getTimerBeforeRetry() {
        return timerBeforeRetry.get();
    }

    private void setTimerBeforeRetry(String timerBeforeRetry) {
        this.timerBeforeRetry.set(timerBeforeRetry);
    }

    private void setTimerBeforeRetry(int seconds) {
        int minutes = seconds / 60;
        int remainingSeconds = seconds % 60;
        setTimerBeforeRetry(String.format("%02d:%02d", minutes, remainingSeconds));
    }

    public StringProperty timerBeforeRetryProperty() {
        return timerBeforeRetry;
    }

    public int getRemainingTriesLeft() {
        return remainingTriesLeft.get();
    }

    private void setRemainingTriesLeft(int remainingTriesLeft) {
        storage.setRemainingTries(remainingTriesLeft);
        this.remainingTriesLeft.set(remainingTriesLeft);
    }

    public IntegerProperty remainingTriesLeftProperty() {
        return remainingTriesLeft;
    }

    private void decrementAvailableTries() throws IllegalStateException {
        setRemainingTriesLeft(getRemainingTriesLeft() - 1);
        if (getRemainingTriesLeft() == 0) {
            throw new IllegalStateException("No more tries left dude");
        }
    }

    private void timerBeforeNewPasswordPrompt() {
        calculateTime();
        setTimerBeforeRetry(time);
        timeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> setTimerBeforeRetry(time--)));
        // playFromStart() is an asynchronous call, Animation may not start immediately. Therefor I'm adding 2 to the cycleCount
        timeline.setCycleCount(time + 2);
        timeline.playFromStart();
        timeline.setOnFinished(event -> reset());
    }

    private void pauseTimeline() {
        timeline.pause();
    }

    private void playTimeline() {
        timeline.play();
    }

    private void calculateTime() {
        time = 30 * (int) Math.pow(storage.getFailedTimes(), 2);
    }

    private void reset() {
        files.clear();
        removeStyleClass("show-encrypt-button");
        removeStyleClass("show-decrypt-button");
        removeStyleClass("on-drag-entered");
        removeStyleClass("too-many-attempts");
        removeStyleClass("access-granted");
        removeStyleClass("wrong-password");
        storage.setFailedTimes(1);
        setRemainingTriesLeft(6);
        setTimerBeforeRetry(time);
        access = false;
    }

    private boolean checkForInputtedPassword() {
        return storage.getPASSWORD().equals(passwordField.getText());
    }

    private void addStyleClass(String arg) {
        if (!container.getStyleClass().contains(arg)) container.getStyleClass().add(arg);
    }

    private void removeStyleClass(String arg) {
        container.getStyleClass().remove(arg);
    }
}