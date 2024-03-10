package com.example.systemobslugilodzizdalniesterowanej;

import com.sothawo.mapjfx.MapView;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.*;
import java.util.ResourceBundle;

public class SystemController implements Initializable {
    Stage stage;
    Boolean networkStatus;
    OSMMap osmMap;
    BoatModeController boatModeController;
    KeyboardHandler keyboardHandler;
    Connection connection;
    String chosenPort;
    Lighting lighting = new Lighting();
    Engines engines = new Engines();
    Flaps flaps = new Flaps();
    String chosenSystem;

    public Stage getStage() {
        return stage;
    }

    public SystemController(Stage stage, String chosenPort, String chosenSystem) {
        this.stage = stage;
        this.chosenPort = chosenPort;
        this.chosenSystem = chosenSystem;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        this.boatModeController = BoatModeController.getInstance(leftFlap, lightDown, lightPower, lightUp, moveDown, moveLeft, moveRight,
                moveUp, rightFlap, lightingText, flapsText, startSwimming, clearTrace, modeChooser, exit, runningBoatInformation, stopSwimmingButton,
                gpsCourse, expectedCourse, sensorCourse, gpsCourseText, sensorCourseText, expectedCourseText);
        try {
            checkConnectionWithInternet();
            osmMap = new OSMMap(mapView, boatModeController, expectedCourse);
        } catch (InterruptedException | IOException e) {
            throw new RuntimeException(e);
        }
        this.connection = new Connection(engines, lighting, flaps, connectionStatus, lightPower, networkStatus, osmMap, stage,
                boatModeController, runningBoatInformation, gpsCourse, sensorCourse, expectedCourse);
        connection.connect(chosenPort, chosenSystem);
        networkStatus = false;
        lightPower.setText("0%");
        exit.setCancelButton(true);
        exit.setFocusTraversable(false);
    }

    public void initializeKeyboardHandler() {
        this.keyboardHandler = new KeyboardHandler(stage.getScene(), connection, boatModeController,
                moveUp, moveDown, moveLeft, moveRight, leftFlap, rightFlap, lightDown, lightUp, engines, lighting, flaps);
        keyboardHandler.keyboardHandler();
    }

    @FXML
    private Label connectionStatus;

    @FXML
    private Label runningBoatInformation;

    @FXML
    private Label networkConnection;

    @FXML
    private Label lightingText;

    @FXML
    private Label flapsText;

    @FXML
    private ToggleButton modeChooser;

    @FXML
    private Button startSwimming;

    @FXML
    private MapView mapView;

    @FXML
    private Button leftFlap;

    @FXML
    private Button lightDown;

    @FXML
    private Label lightPower;

    @FXML
    private Button lightUp;

    @FXML
    private Button moveDown;

    @FXML
    private Button moveLeft;

    @FXML
    private Button moveRight;

    @FXML
    private Button moveUp;

    @FXML
    private Button rightFlap;

    @FXML
    private Button exit;

    @FXML
    private Button clearTrace;

    @FXML
    private Button stopSwimmingButton;

    @FXML
    private CheckBox mapOsmCheckBox;

    // TODO: w celu testach
    @FXML
    private Label gpsCourse;

    @FXML
    private Label expectedCourse;

    @FXML
    private Label sensorCourse;

    @FXML
    private Label gpsCourseText;

    @FXML
    private Label expectedCourseText;

    @FXML
    private Label sensorCourseText;

    public Label getNetworkConnection() {
        return networkConnection;
    }

    @FXML
    void closeApplication(ActionEvent event) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("dialog-window.fxml"));
        Stage dialogStage = new Stage();
        ExitDialogController exitDialogController = new ExitDialogController(stage, dialogStage);
        fxmlLoader.setController(exitDialogController);
        Parent root = fxmlLoader.load();
        Scene scene = new Scene(root);
        dialogStage.setTitle("Zamknij aplikacje");
        dialogStage.setScene(scene);
        dialogStage.show();
    }

    @FXML
    void clearTrace(ActionEvent event) {
        osmMap.clearMap();
    }

    @FXML
    void changeMode(ActionEvent event) {
        if (modeChooser.isSelected()) {
            keyboardHandler.stopBoat();
            changeBoatMode(BoatMode.AUTONOMIC);
        } else {
            changeBoatMode(BoatMode.KEYBOARD_CONTROL);
        }
    }

    @FXML
    void startSwimming(ActionEvent event) throws IOException {
//        if (osmMap.getFoundBoatPosition() && osmMap.designatedWaypoints()) {
        if (true) {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("start-swimming-dialog.fxml"));
            Stage mainStage = new Stage();
            StartSwimmingDialogController startSwimmingDialogController = new StartSwimmingDialogController(mainStage, boatModeController, connection);
            fxmlLoader.setController(startSwimmingDialogController);
            Parent root = fxmlLoader.load();
            Scene scene = new Scene(root);
            mainStage.setScene(scene);
            mainStage.show();
        } else {
            String text = "";
            if (!osmMap.designatedWaypoints() && !osmMap.getFoundBoatPosition())
                text = "Nie wyznaczono pozycji docelowej łódki i jej aktualnego położenia.";
            else if (!osmMap.designatedWaypoints())
                text = "Nie wyznaczono pozycji docelowej łódki.";
            else if (!osmMap.getFoundBoatPosition())
                text = "Nie wyznaczono aktualnego położenia łódki.";
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Brak danych");
            alert.setHeaderText(text);
            alert.showAndWait();
        }
    }

    @FXML
    void stopSwimming(ActionEvent event) {
        connection.sendStopSwimmingInfo();
    }

    @FXML
    void changeToOsmMap(ActionEvent event) {
        if (mapOsmCheckBox.isSelected()) {
            osmMap.changeMapTypeToOSM();
        } else {
            osmMap.changeMapTypeToWMSMap();
        }
    }

    public void checkConnectionWithInternet() throws InterruptedException, IOException {
        try {
            URL url1 = new URL("https://www.geeksforgeeks.org/");
            URLConnection connection = url1.openConnection();
            connection.connect();
            networkConnection.setText("Polaczono z internetem!");
            networkStatus = true;
        } catch (Exception e) {
            modeChooser.setDisable(true);
            mapOsmCheckBox.setDisable(true);
            dialogNotConnect("Brak internetu", "Aplikacja nie może połączyć się z internetem! Brak możliwości przejścia w tryb autonomiczny.");
            getNetworkConnection().setTextFill(Color.color(1, 0, 0));
            getNetworkConnection().setText("Brak polaczenia z internetem! Brak lokalizacji!");
        }
    }

    private void changeBoatMode(BoatMode boatMode) {
        this.boatModeController.setBoatMode(boatMode);
        if (this.boatModeController.getBoatMode() == BoatMode.KEYBOARD_CONTROL) {
            osmMap.removeAllMarkersAndLinesWithoutBoatPosition();
        }
    }

    private void dialogNotConnect(String title, String text) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(text);
        alert.getDialogPane().setMaxWidth(500);
        alert.showAndWait();
    }
}
