package com.example.systemobslugilodzizdalniesterowanej;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.text.Text;
import javafx.stage.Stage;

public class ProgressDialogController {
    Stage stage;

    @FXML
    private Text progressDialogContent;
    @FXML
    private Label progressDialogHeader;


    public ProgressDialogController(Stage stage) {
        this.stage = stage;
    }

    public void setDescriptions(String header, String content) {
        this.progressDialogHeader.setText(header);
        this.progressDialogContent.setText(content);
    }

    public void closeProgressDialogController() {
        this.stage.close();
    }
}
