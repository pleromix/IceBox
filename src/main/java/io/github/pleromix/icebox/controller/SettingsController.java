package io.github.pleromix.icebox.controller;

import io.github.pleromix.icebox.component.Panel;
import io.github.pleromix.icebox.component.ToggleSwitch;
import io.github.pleromix.icebox.util.Config;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;

import java.net.URL;
import java.util.ResourceBundle;

public class SettingsController implements Initializable {

    @FXML
    public ToggleSwitch showImageInfoToggleSwitch;
    @FXML
    public ToggleSwitch showWelcomePanelToggleSwitch;
    @FXML
    public ToggleSwitch askBeforeExitingApplicationToggleSwitch;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        showWelcomePanelToggleSwitch.selectedProperty().bindBidirectional(Config.getInstance().showWelcomePanelProperty);
        showImageInfoToggleSwitch.selectedProperty().bindBidirectional(Config.getInstance().showImageInfoProperty);
        askBeforeExitingApplicationToggleSwitch.selectedProperty().bindBidirectional(Config.getInstance().askBeforeExitingApplicationProperty);
    }

    public void onClose(ActionEvent actionEvent) {
        Panel.close();
    }
}
