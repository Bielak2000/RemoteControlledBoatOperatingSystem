package com.example.systemobslugilodzizdalniesterowanej;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import jssc.SerialPortException;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class ChoosePortController implements Initializable {
    Stage stage;
    private ObservableList<String> portNames = FXCollections.observableArrayList();
    String choosenPort = new String("");
    Lighting lighting = new Lighting();
    Engines engines = new Engines();
    Flaps flaps = new Flaps();
    String choosenSystem = new String("");

    public ChoosePortController(Stage stage1){
        stage=stage1;
        com.fazecast.jSerialComm.SerialPort[] ports = com.fazecast.jSerialComm.SerialPort.getCommPorts();
        for(com.fazecast.jSerialComm.SerialPort port : ports){
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
        portsTable.setOnMouseClicked(e->{
            choosenPort=portsTable.getSelectionModel().getSelectedItem();
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
        if(linuxBox.isSelected()) {
            choosenSystem = "Linux";
            windowsBox.setSelected(false);
        }
        else
            choosenSystem="";
    }

    @FXML
    void chooseWindows(ActionEvent event) {
        if(windowsBox.isSelected()) {
            choosenSystem = "Windows";
            linuxBox.setSelected(false);
        }
        else
            choosenSystem="";
    }

    @FXML
    void connect(ActionEvent event) throws IOException, SerialPortException {
        if(choosenPort.equals("") || choosenSystem.equals("")){
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Nie wybrano portu lub systemu");
            alert.setHeaderText("Port lub system nie zostal wybrany! Wybierz port oraz system.");
            alert.showAndWait();
        }
        else{
            stage.close();
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("application-window.fxml"));
            Stage mainStage = new Stage();
            SystemController controller = new SystemController(mainStage);
            fxmlLoader.setController(controller);
            Parent root = fxmlLoader.load();
            Scene scene = new Scene(root);
            mainStage.setTitle("System obslugi lodzi zdalnie sterowanej");
            mainStage.setScene(scene);
            mainStage.show();
            root.requestFocus();

            Connection connection = new Connection(controller, engines, lighting, flaps);
            connection.connect(choosenPort, choosenSystem);

            scene.setOnKeyPressed(new EventHandler<KeyEvent>() {
                @Override
                public void handle(KeyEvent keyEvent) {
                    switch(keyEvent.getCode()){
                        case UP:
                            controller.getMoveUp().arm();
                            engines.movingForward();
                            if(!engines.getTemp()) {
                                connection.sendParameters();
                              engines.setTemp(true);
                            }
                            //controller.getLightPower().setText("2");
                            break;
                        case DOWN:
                            controller.getMoveDown().arm();
                            engines.movingBack();
                            if(!engines.getTemp()) {
                                connection.sendParameters();
                                engines.setTemp(true);
                            }
                            break;
                        case LEFT:
                            controller.getMoveLeft().arm();
                            engines.turnLeft();
                            if(!engines.getTemp()) {
                                connection.sendParameters();
                                engines.setTemp(true);
                            }
                            break;
                        case RIGHT:
                            controller.getMoveRight().arm();
                            engines.turnRight();
                            if(!engines.getTemp()) {
                                connection.sendParameters();
                                engines.setTemp(true);
                            }
                            break;
                        case T:
                            controller.getLeftFlap().arm();
                            flaps.onLeftFlap();
                            if(!flaps.getTemp()){
                                connection.sendParameters();
                                flaps.setTemp(true);
                            }
                            break;
                        case Y:
                            controller.getRightFlap().arm();
                            flaps.onRightFlap();
                            if(!flaps.getTemp()){
                                connection.sendParameters();
                                flaps.setTemp(true);
                            }
                            break;
                        case Q:
                            controller.getLightDown().arm();
                            if(!lighting.getTemp()) {
                                lighting.setPower(0);
                                connection.sendParameters();
                                lighting.setTemp(true);
                            }
                            break;
                        case E:
                            controller.getLightUp().arm();
                            if(!lighting.getTemp()) {
                                lighting.setPower(100);
                                connection.sendParameters();
                                lighting.setTemp(true);
                            }
                            break;
                    }
                }
            });

            scene.setOnKeyReleased(new EventHandler<KeyEvent>() {
                @Override
                public void handle(KeyEvent keyEvent) {
                    switch (keyEvent.getCode()) {
                        case UP:
                            controller.getMoveUp().disarm();
                            engines.turnOff();
                            connection.sendParameters();
                            engines.setTemp(false);
                            break;
                        case DOWN:
                            controller.getMoveDown().disarm();
                            engines.turnOff();
                            connection.sendParameters();
                            engines.setTemp(false);
                            break;
                        case LEFT:
                            controller.getMoveLeft().disarm();
                            engines.turnOff();
                            connection.sendParameters();
                            engines.setTemp(false);
                            break;
                        case RIGHT:
                            controller.getMoveRight().disarm();
                            engines.turnOff();
                            connection.sendParameters();
                            engines.setTemp(false);
                            break;
                        case T:
                            controller.getLeftFlap().disarm();
                            flaps.offLeftFlap();
                            flaps.setTemp(false);
                            break;
                        case Y:
                            controller.getRightFlap().disarm();
                            flaps.offRightFlap();
                            flaps.setTemp(false);
                            break;
                        case Q:
                            controller.getLightDown().disarm();
                            lighting.setPower(-1);
                            connection.sendParameters();
                            lighting.setTemp(false);
                            break;
                        case E:
                            controller.getLightUp().disarm();
                            lighting.setPower(-1);
                            connection.sendParameters();
                            lighting.setTemp(false);
                            break;
                    }
                }
            });
        }
    }

    @FXML
    void exit(ActionEvent event) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("dialog-window.fxml"));
        Stage dialogStage = new Stage();
        DialogController dialogController = new DialogController(stage, dialogStage);
        fxmlLoader.setController(dialogController);
        Parent root = fxmlLoader.load();
        Scene scene = new Scene(root);
        dialogStage.setTitle("Zamknij aplikacje");
        dialogStage.setScene(scene);
        dialogStage.show();
    }
}
