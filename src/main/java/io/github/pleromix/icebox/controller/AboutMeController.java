package io.github.pleromix.icebox.controller;

import io.github.pleromix.icebox.App;
import io.github.pleromix.icebox.component.Panel;
import io.github.pleromix.icebox.util.Utility;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;

import java.net.URL;
import java.util.Objects;
import java.util.ResourceBundle;

public class AboutMeController implements Initializable {

    @FXML
    public Label logoTitleLabel;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        var version = Utility.getAppVersion();

        if (Objects.nonNull(version)) {
            logoTitleLabel.setText(String.format("%s v%s", logoTitleLabel.getText(), version));
        }
    }

    public void onOpenLink(ActionEvent actionEvent) {
        App.application.getHostServices().showDocument("https://www.linkedin.com/in/mohammadreza-faraji/");
    }

    public void onClose(ActionEvent actionEvent) {
        Panel.close();
    }
}
