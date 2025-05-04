package io.github.pleromix.icebox.controller;

import io.github.pleromix.icebox.App;
import io.github.pleromix.icebox.component.Notification;
import io.github.pleromix.icebox.component.Panel;
import io.github.pleromix.icebox.dto.Metadata;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;

import java.net.URL;
import java.util.Objects;
import java.util.ResourceBundle;

public class MetadataController implements Initializable {

    @FXML
    public TextField titleTextField;
    @FXML
    public TextField subjectTextField;
    @FXML
    public TextField creatorTextField;
    @FXML
    public TextField authorTextField;
    @FXML
    public TextField producerTextField;
    @FXML
    public TextField keywordsTextField;
    @FXML
    public Button applyButton;
    @FXML
    public Button removeButton;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        final var metadata = App.getController().getMetadata();

        if (Objects.nonNull(metadata)) {
            titleTextField.setText(metadata.getTitle());
            authorTextField.setText(metadata.getAuthor());
            subjectTextField.setText(metadata.getSubject());
            producerTextField.setText(metadata.getProducer());
            creatorTextField.setText(metadata.getCreator());
            keywordsTextField.setText(metadata.getKeywords());
            removeButton.setDisable(false);
        }

        applyButton.disableProperty().bind(
                titleTextField.textProperty().isEmpty()
                        .and(authorTextField.textProperty().isEmpty())
                        .and(subjectTextField.textProperty().isEmpty())
                        .and(producerTextField.textProperty().isEmpty())
                        .and(creatorTextField.textProperty().isEmpty())
                        .and(keywordsTextField.textProperty().isEmpty())
        );
    }

    public void onApply(ActionEvent actionEvent) {
        final var metadata = new Metadata();

        metadata.setTitle(titleTextField.getText().trim());
        metadata.setAuthor(authorTextField.getText().trim());
        metadata.setSubject(subjectTextField.getText().trim());
        metadata.setProducer(producerTextField.getText().trim());
        metadata.setCreator(creatorTextField.getText().trim());
        metadata.setKeywords(keywordsTextField.getText().trim());

        App.getController().setMetadata(metadata);

        Panel.close();

        Notification.create("PDF Info", "Metadata updated successfully!");
    }

    public void onRemove(ActionEvent actionEvent) {
        titleTextField.clear();
        authorTextField.clear();
        subjectTextField.clear();
        producerTextField.clear();
        creatorTextField.clear();
        keywordsTextField.clear();

        App.getController().setMetadata(null);

        Panel.close();

        Notification.create("PDF Info", "Metadata is removed!");
    }

    public void onClose(ActionEvent actionEvent) {
        Panel.close();
    }
}
