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
        this.boatModeController = BoatModeController.getInstance();
        this.chosenPort = chosenPort;
        this.chosenSystem = chosenSystem;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        try {
            checkConnectionWithInternet();
            osmMap = new OSMMap(mapView, boatModeController);
        } catch (InterruptedException | IOException e) {
            throw new RuntimeException(e);
        }
        this.connection = new Connection(engines, lighting, flaps, connectionStatus, lightPower, networkStatus, osmMap, stage);
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
    private CheckBox mapOsmCheckBox;

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
            changeBoatMode(BoatMode.AUTONOMIC);
            setViewForAutonomicBoatMode();
        } else {
            changeBoatMode(BoatMode.KEYBOARD_CONTROL);
            setViewForKeyboardControlBoatMode();
        }
    }

    @FXML
    void startSwimming(ActionEvent event) throws IOException {
        if (osmMap.getFoundBoatPosition() && osmMap.designatedWaypoints()) {
            if (osmMap.designatedWaypoints()) {
                FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("start-swimming-dialog.fxml"));
                Stage mainStage = new Stage();
                StartSwimmingDialogController startSwimmingDialogController = new StartSwimmingDialogController(mainStage);
                fxmlLoader.setController(startSwimmingDialogController);
                Parent root = fxmlLoader.load();
                Scene scene = new Scene(root);
                mainStage.setScene(scene);
                mainStage.show();
            }
        } else {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("information-dialog.fxml"));
            Stage mainStage = new Stage();
            DialogInformationController dialogInformationController = new DialogInformationController(mainStage);
            fxmlLoader.setController(dialogInformationController);
            Parent root = fxmlLoader.load();
            Scene scene = new Scene(root);
            mainStage.setScene(scene);
            if (!osmMap.designatedWaypoints() && !osmMap.getFoundBoatPosition())
                dialogInformationController.setInformation("Nie wyznaczono pozycji docelowej łódki i jej aktualnego położenia.");
            else if (!osmMap.designatedWaypoints())
                dialogInformationController.setInformation("Nie wyznaczono pozycji docelowej łódki.");
            else if (!osmMap.getFoundBoatPosition())
                dialogInformationController.setInformation("Nie wyznaczono aktualnego położenia łódki.");
            mainStage.show();
        }
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
            dialogNotConnect("Brak internetu", "Aplikacja nie moze polaczyc sie z internetem!");
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

    private void setViewForAutonomicBoatMode() {
        lightingText.setVisible(false);
        lightDown.setVisible(false);
        lightUp.setVisible(false);
        flapsText.setVisible(false);
        leftFlap.setVisible(false);
        rightFlap.setVisible(false);
        moveDown.setVisible(false);
        moveLeft.setVisible(false);
        moveRight.setVisible(false);
        moveUp.setVisible(false);
        startSwimming.setVisible(true);
        clearTrace.setVisible(true);
    }

    private void setViewForKeyboardControlBoatMode() {
        lightingText.setVisible(true);
        lightDown.setVisible(true);
        lightUp.setVisible(true);
        flapsText.setVisible(true);
        leftFlap.setVisible(true);
        rightFlap.setVisible(true);
        moveDown.setVisible(true);
        moveLeft.setVisible(true);
        moveRight.setVisible(true);
        moveUp.setVisible(true);
        startSwimming.setVisible(false);
        clearTrace.setVisible(false);
    }

    private void dialogNotConnect(String title, String text) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(text);
        alert.showAndWait();
    }
}
