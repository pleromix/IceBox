package com.github.icebox.component;

import com.github.icebox.App;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import javafx.util.Pair;

import java.util.List;
import java.util.Objects;

public final class MessageBox {

    private final Stage stage = new Stage(StageStyle.TRANSPARENT);
    private final StackPane overlay = new StackPane();
    private final VBox box = new VBox();
    private final Label titleLabel = new Label();
    private final HBox content = new HBox();
    private final Region icon = new Region();
    private final Label contentLabel = new Label();
    private final HBox controlBox = new HBox();

    public MessageBox() {
        initialize();
    }

    public static void create(String title, String content, MessageType messageType, List<Pair<String, EventHandler<ActionEvent>>> pairs) {
        var messageBox = new MessageBox();
        var flag = true;
        Runnable close = () -> {
            App.controller.root.getChildren().remove(messageBox.overlay);
            messageBox.stage.close();
        };
        Button defaultButton = null;

        messageBox.titleLabel.setText(title);
        messageBox.contentLabel.setText(content);

        for (var pair : pairs) {
            var button = new Button(pair.getKey());

            if (flag) {
                button.setDefaultButton(true);
                defaultButton = button;
                flag = false;
            }

            button.addEventHandler(ActionEvent.ACTION, event -> {
                pair.getValue().handle(event);
                close.run();
            });

            messageBox.controlBox.getChildren().add(button);
        }

        messageBox.stage.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                close.run();
            }
        });

        switch (messageType) {
            case Error:
                messageBox.icon.getStyleClass().add("error");
                break;
            case Warning:
                messageBox.icon.getStyleClass().add("warning");
                break;
            case Info:
                messageBox.icon.getStyleClass().add("info");
                break;
            case Question:
                messageBox.icon.getStyleClass().add("question");
                break;
            default:
                throw new RuntimeException("Unhandled MessageType: " + messageType);
        }

        App.controller.root.getChildren().add(messageBox.overlay);

        if (Objects.nonNull(defaultButton)) {
            defaultButton.requestFocus();
        }

        messageBox.stage.initOwner(App.primaryStage);
        messageBox.stage.initModality(Modality.WINDOW_MODAL);
        messageBox.stage.show();

        initAnimation(messageBox);
    }

    private static void initAnimation(MessageBox messageBox) {
        var timeline = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(messageBox.box.scaleXProperty(), 1.0D), new KeyValue(messageBox.box.scaleYProperty(), 1.0D)),
                new KeyFrame(Duration.millis(50), new KeyValue(messageBox.box.scaleXProperty(), 0.95D), new KeyValue(messageBox.box.scaleYProperty(), 0.95D)),
                new KeyFrame(Duration.millis(100), new KeyValue(messageBox.box.scaleXProperty(), 1.0D), new KeyValue(messageBox.box.scaleYProperty(), 1.0D))
        );

        timeline.setCycleCount(3);
        timeline.play();
    }

    private void initialize() {
        final var anchorPane = new AnchorPane(box);
        final var scene = new Scene(anchorPane);

        box.setMaxWidth(VBox.USE_PREF_SIZE);
        box.setMaxHeight(VBox.USE_PREF_SIZE);

        contentLabel.setMaxHeight(Double.MAX_VALUE);

        titleLabel.getStyleClass().add("title");
        content.getStyleClass().add("content");
        icon.getStyleClass().add("icon");
        contentLabel.getStyleClass().add("content-label");
        controlBox.getStyleClass().add("control-box");
        box.getStyleClass().add("message-box");
        overlay.getStyleClass().add("overlay");

        VBox.setMargin(controlBox, new Insets(4.0D, 0.0D, 0.0D, 0.0D));

        titleLabel.setGraphic(icon);
        content.getChildren().addAll(contentLabel);
        box.getChildren().addAll(titleLabel, content, controlBox);
        //overlay.getChildren().add(box);


        scene.getStylesheets().add(Objects.requireNonNull(MessageBox.class.getResource("/com/github/icebox/css/app.css")).toExternalForm());

        scene.setOnMousePressed(pressedEvent -> {
            scene.setOnMouseDragged(draggedEvent -> {
                stage.setX(draggedEvent.getScreenX() - pressedEvent.getX());
                stage.setY(draggedEvent.getScreenY() - pressedEvent.getY());
                scene.setCursor(Cursor.MOVE);
            });
        });

        scene.setOnMouseReleased(releasedEvent -> {
            scene.setCursor(Cursor.DEFAULT);
        });

        AnchorPane.setTopAnchor( box, 24.0D);
        AnchorPane.setRightAnchor( box, 24.0D);
        AnchorPane.setBottomAnchor( box, 24.0D);
        AnchorPane.setLeftAnchor( box, 24.0D);

        anchorPane.setStyle("-fx-background-color: transparent;");
        scene.setFill(Color.TRANSPARENT);

        stage.setScene(scene);
    }

    public enum MessageType {
        Error,
        Warning,
        Info,
        Question
    }
}
