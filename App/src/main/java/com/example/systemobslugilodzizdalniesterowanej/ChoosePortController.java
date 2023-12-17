package com.example.systemobslugilodzizdalniesterowanej;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import jssc.SerialPortException;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class ChoosePortController implements Initializable {
    Stage stage;
    private ObservableList<String> portNames = FXCollections.observableArrayList();
    String chosenPort = new String("");
    String chosenSystem = new String("");

    public ChoosePortController(Stage stage1) {
        stage = stage1;
        com.fazecast.jSerialComm.SerialPort[] ports = com.fazecast.jSerialComm.SerialPort.getCommPorts();
        for (com.fazecast.jSerialComm.SerialPort port : ports) {
            portNames.add(port.getSystemPortName());
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        exit.setCancelButton(true);
        exit.setFocusTraversable(false);
        connectButton.setFocusTraversable(false);
        column.setCellValueFactory(data -> new SimpleStringProperty(data.getValue()));
        portsTable.setItems(portNames);
        portsTable.setOnMouseClicked(e -> {
            chosenPort = portsTable.getSelectionModel().getSelectedItem();
        });
    }

    @FXML
    private Button exit;

    @FXML
    private TableView<String> portsTable;

    @FXML
    private TableColumn<String, String> column;

    @FXML
    private Button connectButton;

    @FXML
    private CheckBox windowsBox;

    @FXML
    private CheckBox linuxBox;


    @FXML
    void chooseLinux(ActionEvent event) {
        if (linuxBox.isSelected()) {
            chosenSystem = "Linux";
            windowsBox.setSelected(false);
        } else
            chosenSystem = "";
    }

    @FXML
    void chooseWindows(ActionEvent event) {
        if (windowsBox.isSelected()) {
            chosenSystem = "Windows";
            linuxBox.setSelected(false);
        } else
            chosenSystem = "";
    }

    @FXML
    void connect(ActionEvent event) throws IOException, SerialPortException {
        if (chosenPort.equals("") || chosenSystem.equals("")) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Nie wybrano portu lub systemu");
            alert.setHeaderText("Port lub system nie zostal wybrany! Wybierz port oraz system.");
            alert.showAndWait();
        } else {
            stage.close();
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("application-window.fxml"));
            Stage mainStage = new Stage();
            SystemController systemController = new SystemController(mainStage, chosenPort, chosenSystem);
            fxmlLoader.setController(systemController);
            Parent root = fxmlLoader.load();
            Scene scene = new Scene(root);
            mainStage.setTitle("System obslugi lodzi zdalnie sterowanej");
            mainStage.setScene(scene);
            mainStage.show();
            root.requestFocus();
            systemController.initializeKeyboardHandler();
        }
    }

    @FXML
    void exit(ActionEvent event) throws IOException {
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
}
