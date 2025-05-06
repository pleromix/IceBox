package io.github.pleromix.icebox.controller;

import io.github.pleromix.icebox.App;
import io.github.pleromix.icebox.component.Notification;
import io.github.pleromix.icebox.component.Panel;
import io.github.pleromix.icebox.dto.Metadata;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;

import java.net.URL;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.function.UnaryOperator;

public class MetadataController implements Initializable {

    private static final int MAX_LENGTH = 128;

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
    @FXML
    public Label noteLabel;

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
                titleTextField
                        .textProperty()
                        .isEmpty()
                        .and(authorTextField.textProperty().isEmpty())
                        .and(subjectTextField.textProperty().isEmpty())
                        .and(producerTextField.textProperty().isEmpty())
                        .and(creatorTextField.textProperty().isEmpty())
                        .and(keywordsTextField.textProperty().isEmpty())
        );

        final UnaryOperator<TextFormatter.Change> changeUnaryOperator = change -> {
            if (change.getControlNewText().length() > MAX_LENGTH) {
                return null;
            }

            if (change.getText().matches("[\\sa-zA-Z0-9_-]*")) {
                return change;
            }

            return null;
        };

        final UnaryOperator<TextFormatter.Change> keywordsChangeUnaryOperator = change -> {
            if (change.getControlNewText().length() > MAX_LENGTH) {
                return null;
            }

            if (change.getText().matches("[\\sa-zA-Z0-9,]*")) {
                return change;
            }

            return null;
        };

        titleTextField.setTextFormatter(new TextFormatter<>(changeUnaryOperator));
        authorTextField.setTextFormatter(new TextFormatter<>(changeUnaryOperator));
        subjectTextField.setTextFormatter(new TextFormatter<>(changeUnaryOperator));
        producerTextField.setTextFormatter(new TextFormatter<>(changeUnaryOperator));
        creatorTextField.setTextFormatter(new TextFormatter<>(changeUnaryOperator));
        keywordsTextField.setTextFormatter(new TextFormatter<>(keywordsChangeUnaryOperator));

        noteLabel.setText(String.format("Each text field allows a maximum of %d characters.", MAX_LENGTH));
    }

    public void onApply(ActionEvent actionEvent) {
        final var metadata = new Metadata();

        metadata.setTitle(titleTextField.getText().trim().replaceAll("\\s{2,}", " "));
        metadata.setAuthor(authorTextField.getText().trim().replaceAll("\\s{2,}", " "));
        metadata.setSubject(subjectTextField.getText().trim().replaceAll("\\s{2,}", " "));
        metadata.setProducer(producerTextField.getText().trim().replaceAll("\\s{2,}", " "));
        metadata.setCreator(creatorTextField.getText().trim().replaceAll("\\s{2,}", " "));
        metadata.setKeywords(keywordsTextField.getText().trim().replaceAll(",", "").replaceAll("\\s{2,}", " ").replaceAll("\\s", ", ").replaceAll(",$", ""));

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
