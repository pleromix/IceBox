package io.github.pleromix.icebox.component;

import io.github.pleromix.icebox.App;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.fxml.FXMLLoader;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import lombok.Getter;

import java.io.IOException;
import java.util.Objects;

public class Panel {

    public static final String ABOUT = "about";
    public static final String METADATA = "metadata";
    public static final String CREATION = "optimization";
    public static final String WELCOME = "welcome";
    public static final String SETTINGS = "settings";

    private static final StackPane overlay = new StackPane();

    @Getter
    private static Stage currentStage;

    private Panel() {
    }

    public static void open(String fxmlFileName) {
        close();

        try {
            var stage = new Stage(StageStyle.TRANSPARENT);
            var fxmlLoader = new FXMLLoader(App.class.getResource(String.format("%s%s", fxmlFileName, ".fxml")));
            var scene = new Scene(fxmlLoader.load());
            var root = (AnchorPane) scene.getRoot();

            overlay.getStyleClass().add("overlay");

            stage.setScene(scene);

            root.setStyle("-fx-background-color: transparent;");
            scene.setFill(Color.TRANSPARENT);

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

            stage.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                if (event.getCode() == KeyCode.ESCAPE) {
                    close();
                }
            });

            stage.setOnCloseRequest(event -> close());

            App.getController().root.getChildren().add(overlay);

            stage.initOwner(App.getPrimaryStage());
            stage.initModality(Modality.WINDOW_MODAL);
            stage.show();

            currentStage = stage;

            var timeline = new Timeline(
                    new KeyFrame(Duration.ZERO, new KeyValue(root.scaleXProperty(), 1.0D), new KeyValue(root.scaleYProperty(), 1.0D)),
                    new KeyFrame(Duration.millis(50), new KeyValue(root.scaleXProperty(), 0.95D), new KeyValue(root.scaleYProperty(), 0.95D)),
                    new KeyFrame(Duration.millis(100), new KeyValue(root.scaleXProperty(), 1.0D), new KeyValue(root.scaleYProperty(), 1.0D))
            );

            timeline.setCycleCount(3);
            timeline.play();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void close() {
        if (Objects.nonNull(currentStage) && currentStage != App.getPrimaryStage()) {
            currentStage.close();
            App.getController().root.getChildren().remove(overlay);
            currentStage = null;
        }
    }
}
