package com.example.systemobslugilodzizdalniesterowanej;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import javafx.event.ActionEvent;

public class StartSwimmingDialogController {
    Stage stage;

    public StartSwimmingDialogController(Stage stage) {
        this.stage = stage;
    }

    @FXML
    private Button confirmSwimming;

    @FXML
    private Button noConfirmSwimming;

    @FXML
    void swimming(ActionEvent event) {

    }

    @FXML
    void notSwimming(ActionEvent event) {
        this.stage.close();
    }
}
