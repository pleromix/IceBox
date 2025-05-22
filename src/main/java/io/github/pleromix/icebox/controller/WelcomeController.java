package io.github.pleromix.icebox.controller;

import io.github.pleromix.icebox.App;
import io.github.pleromix.icebox.component.Panel;
import io.github.pleromix.icebox.component.ToggleSwitch;
import io.github.pleromix.icebox.util.Config;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;

import java.net.URL;
import java.util.ResourceBundle;

public class WelcomeController implements Initializable {

    @FXML
    public ToggleSwitch showWelcomePanelToggleSwitch;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        showWelcomePanelToggleSwitch.selectedProperty().bindBidirectional(Config.getInstance().showWelcomePanelProperty);
    }

    public void onAddImages(ActionEvent actionEvent) {
        App.getController().onImportFiles(actionEvent);
    }

    public void onClose(ActionEvent actionEvent) {
        Panel.close();
    }
}
