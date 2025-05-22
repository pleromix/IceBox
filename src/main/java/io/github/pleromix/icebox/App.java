package io.github.pleromix.icebox;

import io.github.pleromix.icebox.component.MessageBox;
import io.github.pleromix.icebox.controller.AppController;
import io.github.pleromix.icebox.util.Config;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.util.Pair;
import lombok.Getter;
import nu.pattern.OpenCV;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class App extends Application {

    private static final Logger logger = LoggerFactory.getLogger(App.class);

    @Getter
    private static App application;
    @Getter
    private static Stage primaryStage;
    @Getter
    private static AppController controller;

    public static void main(String[] args) {
        OpenCV.loadLocally();
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        try {
            App.application = this;
            App.primaryStage = stage;

            var fxmlLoader = new FXMLLoader(App.class.getResource("app.fxml"));
            var scene = new Scene(fxmlLoader.load());

            controller = fxmlLoader.getController();

            stage.setMinWidth(900.0D);
            stage.setMinHeight(500.0D);
            stage.setWidth(900.0D);
            stage.setHeight(500.0D);

            stage.setTitle("IceBox Application");
            stage.setScene(scene);
            stage.show();

            stage.setOnCloseRequest(event -> {
                if (Config.getInstance().getAskBeforeExitingApplication()) {
                    event.consume();

                    MessageBox.create(
                            "Exit Application",
                            "Are you sure you want to exit the application?",
                            MessageBox.MessageType.Question,
                            List.of(
                                    new Pair<>("Yes", e -> Platform.exit()),
                                    new Pair<>("No", e -> {
                                    })
                            )
                    );
                }
            });
        } catch (Exception e) {
            logger.error("Could not open FXML file: {}", e.getMessage(), e);
        }
    }
}