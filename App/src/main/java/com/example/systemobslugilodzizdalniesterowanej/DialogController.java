package com.example.systemobslugilodzizdalniesterowanej;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.stage.Stage;

import java.io.IOException;

public class DialogController {
    Stage mainStage;
    Stage dialogStage = new Stage();

    DialogController(Stage mainStage1, Stage dialogStage1) throws IOException {
        this.mainStage=mainStage1;
        this.dialogStage=dialogStage1;
    }

    @FXML
    private Button noConfirmExit;

    @FXML
    private Button confirmExit;

    @FXML
    void exit(ActionEvent event) {
        dialogStage.close();
        mainStage.close();
        System.exit(0);
    }

    @FXML
    void notExit(ActionEvent event) {
        dialogStage.close();
    }
}
