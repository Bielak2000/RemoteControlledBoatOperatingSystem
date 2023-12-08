package com.example.systemobslugilodzizdalniesterowanej;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.text.Text;
import javafx.stage.Stage;

public class DialogInformationController {
    private Stage stage;

    public DialogInformationController(Stage stage) {
        this.stage = stage;
    }

    @FXML
    private Button cancelDialog;

    @FXML
    private Text information;

    @FXML
    void cancelDialog(ActionEvent event) {
        this.stage.close();
    }

    public void setInformation(String information) {
        this.information.setText(information);
    }
}
