package io.github.pleromix.icebox.controller;

import io.github.pleromix.icebox.App;
import io.github.pleromix.icebox.component.Panel;
import io.github.pleromix.icebox.util.Config;
import io.github.pleromix.icebox.util.Utility;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.ResourceBundle;

public class AboutController implements Initializable {

    private static final Logger logger = LoggerFactory.getLogger(AboutController.class);

    @FXML
    public Label logoTitleLabel;
    @FXML
    public TextArea acknowledgementTextArea;

    @SneakyThrows
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        var version = Utility.getAppVersion();

        if (Objects.nonNull(version)) {
            logoTitleLabel.setText(String.format("%s v%s", logoTitleLabel.getText(), version));
        }

        try (var stream = AboutController.class.getResourceAsStream("/io/github/pleromix/icebox/asset/about.txt")) {
            if (Objects.isNull(stream)) {
                throw new FileNotFoundException("Resource not found");
            }

            var content = new String(stream.readAllBytes(), StandardCharsets.UTF_8);

            acknowledgementTextArea.setText(content);
        } catch (IOException e) {
            logger.error("Failed to load about file: {}", e.getMessage(), e);
        }
    }

    public void onOpenLink(ActionEvent actionEvent) {
        App.getApplication().getHostServices().showDocument("https://github.com/pleromix/");
    }

    public void onClose(ActionEvent actionEvent) {
        Panel.close();
    }
}
