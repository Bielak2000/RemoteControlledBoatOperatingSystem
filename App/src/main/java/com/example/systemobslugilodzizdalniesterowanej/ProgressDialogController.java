package com.example.systemobslugilodzizdalniesterowanej;

import javafx.fxml.FXML;
import javafx.scene.control.ProgressBar;
import javafx.stage.Stage;

public class ProgressDialogController {
    Stage stage;

    @FXML
    private ProgressBar progressDialog;


    public ProgressDialogController(Stage stage) {
        this.stage = stage;
    }

    public void closeProgressDialogController() {
        this.stage.close();
    }
}
