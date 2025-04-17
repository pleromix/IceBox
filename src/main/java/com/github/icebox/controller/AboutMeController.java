package com.github.icebox.controller;

import com.github.icebox.App;
import com.github.icebox.component.Panel;
import com.github.icebox.util.Utility;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;

import java.net.URL;
import java.util.ResourceBundle;

public class AboutMeController implements Initializable {

    @FXML
    public Label logoTitleLabel;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logoTitleLabel.setText(String.format("%s v%s", logoTitleLabel.getText(), Utility.getAppVersion()));
    }

    public void onOpenLink(ActionEvent actionEvent) {
        App.application.getHostServices().showDocument("https://www.linkedin.com/in/mohammadreza-faraji/");
    }

    public void onClose(ActionEvent actionEvent) {
        Panel.close();
    }
}
