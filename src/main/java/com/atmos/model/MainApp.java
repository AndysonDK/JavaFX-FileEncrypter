package com.atmos.model;

import com.atmos.controller.CrashController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URISyntaxException;

public class MainApp extends Application {
    private static Scene[] scenes;
    private static Stage window;
    private static Storage storage;

    private static Exception potentialException;

    public MainApp() {
        scenes = new Scene[3];
        try {
            storage = new Storage();
            scenes[0] = new Scene(FXMLLoader.load(getClass().getResource("/fxml/Setup.fxml")));
            scenes[1] = new Scene(FXMLLoader.load(getClass().getResource("/fxml/Main.fxml")));
        } catch (IOException | URISyntaxException | CryptoException exc) {
            potentialException = exc;
        }
    }

    public static Storage getStorage() {
        return storage;
    }

    public static void setNewScene(int sceneIndex) {
        window.setScene(scenes[sceneIndex]);
    }

    public static void setNewException(Exception e) {
        potentialException = e;
    }

    private boolean isFirstTime() {
        return !storage.hasPassword();
    }

    public static void crashVisualsHandling() {
        window.show();
        // I'm creating the crash view like this or else i wouldn't be able to retrieve it's controller
        // The crash model is a special kind of model!
        FXMLLoader crashLoader = new FXMLLoader(MainApp.class.getResource("/fxml/Crash.fxml"));
        try {
            scenes[2] = new Scene(crashLoader.load());
        } catch (IOException e) {
            e.printStackTrace();
        }
        ((CrashController) crashLoader.getController()).setCrashMessage(potentialException);
        setNewScene(2);
        crash();
    }

    // The exception handler for every single thrown exception that I'm not catching earlier or handling properly
    private static void crash() {
        System.out.println("An unknown error occurred which i haven't chosen to handle properly");
        potentialException.printStackTrace();
        try {
            Thread.sleep(7000);
        } catch (InterruptedException IExc) {
            IExc.printStackTrace();
            System.exit(1);
        }
        System.exit(1);
    }

    @Override
    public void start(Stage stage) {
        window = stage;
        stage.getIcons().add(new Image("/images/textFileLock.png"));
        stage.setTitle("File Encrypter and Decrypter");
        stage.setResizable(false);
        stage.setOnHiding(event -> {
            try {
                storage.saveData();
            } catch (IOException | CryptoException exc) {
                potentialException = exc;
                crashVisualsHandling();
            }
        });
        if (potentialException != null) crashVisualsHandling();
        stage.setScene(isFirstTime() ? scenes[0] : scenes[1]);
        stage.show();
    }

    /**
     * The main() method is ignored in correctly deployed JavaFX application.
     * main() serves only as fallback in case the application can not be
     * launched through deployment artifacts, potentialException.g., in IDEs with limited FX
     * support. NetBeans ignores main().
     *
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }
}