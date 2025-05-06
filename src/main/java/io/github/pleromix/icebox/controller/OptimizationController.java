package io.github.pleromix.icebox.controller;

import io.github.pleromix.icebox.App;
import io.github.pleromix.icebox.component.CurveBinder;
import io.github.pleromix.icebox.component.Panel;
import io.github.pleromix.icebox.component.ToggleSwitch;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.UUID;
import java.util.function.UnaryOperator;

public class OptimizationController implements Initializable {

    @FXML
    public CurveBinder curveBinder;
    @FXML
    public ToggleButton flipHorizontalToggleButton;
    @FXML
    public ToggleButton flipVerticalToggleButton;
    @FXML
    public ToggleButton rotate90DegCcwToggleButton;
    @FXML
    public ToggleButton rotate90DegCwToggleButton;
    @FXML
    public ToggleSwitch grayscaleToggleSwitch;
    @FXML
    public Button guidButton;
    @FXML
    public TextField pdfFileNameTextField;
    @FXML
    public Button applyButton;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initPdfFileNameTextFieldContextMenu();
        applyButton.disableProperty().bind(pdfFileNameTextField.textProperty().isEmpty());

        final UnaryOperator<TextFormatter.Change> changeUnaryOperator = change -> {
            if (change.getText().matches("[\\sa-zA-Z0-9_-]*")) {
                return change;
            }

            return null;
        };

        pdfFileNameTextField.setTextFormatter(new TextFormatter<>(changeUnaryOperator));
    }

    public void onApply(ActionEvent actionEvent) {
        App.getController().createPdf(
                pdfFileNameTextField.getText().trim().replaceAll("\\s{2,}", " "),
                curveBinder.getValue(),
                rotate90DegCcwToggleButton.isSelected(),
                rotate90DegCwToggleButton.isSelected(),
                flipHorizontalToggleButton.isSelected(),
                flipVerticalToggleButton.isSelected(),
                grayscaleToggleSwitch.isSelected()
        );
    }

    public void onGuid(ActionEvent actionEvent) {
        pdfFileNameTextField.setText(UUID.randomUUID().toString());
    }

    private void initPdfFileNameTextFieldContextMenu() {
        final var contextMenu = new ContextMenu();
        final var cutMenuItem = new MenuItem("Cut");
        final var copyMenuItem = new MenuItem("Copy");
        final var pasteMenuItem = new MenuItem("Paste");
        final var selectAllMenuItem = new MenuItem("Select All");
        final var deleteMenuItem = new MenuItem("Delete");
        final var uuidMenuItem = new MenuItem("Get a GUID");

        cutMenuItem.setOnAction(event -> pdfFileNameTextField.cut());
        copyMenuItem.setOnAction(event -> pdfFileNameTextField.copy());
        pasteMenuItem.setOnAction(event -> pdfFileNameTextField.paste());
        selectAllMenuItem.setOnAction(event -> pdfFileNameTextField.selectAll());
        deleteMenuItem.setOnAction(event -> pdfFileNameTextField.clear());
        uuidMenuItem.setOnAction(event -> pdfFileNameTextField.setText(UUID.randomUUID().toString()));

        cutMenuItem.disableProperty().bind(pdfFileNameTextField.selectedTextProperty().isEmpty());
        copyMenuItem.disableProperty().bind(pdfFileNameTextField.selectedTextProperty().isEmpty());
        selectAllMenuItem.disableProperty().bind(pdfFileNameTextField.textProperty().isEmpty());
        deleteMenuItem.disableProperty().bind(pdfFileNameTextField.textProperty().isEmpty());

        contextMenu.getItems().addAll(
                cutMenuItem,
                copyMenuItem,
                pasteMenuItem,
                new SeparatorMenuItem(),
                selectAllMenuItem,
                deleteMenuItem,
                new SeparatorMenuItem(),
                uuidMenuItem
        );

        pdfFileNameTextField.setContextMenu(contextMenu);
    }

    public void onClose(ActionEvent actionEvent) {
        Panel.close();
    }
}
