package io.github.pleromix.icebox;

import io.github.pleromix.icebox.controller.AppController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import nu.pattern.OpenCV;

import java.io.IOException;

public class App extends Application {

    public static App application;
    public static Stage primaryStage;
    public static AppController controller;

    public static void main(String[] args) {
        OpenCV.loadLocally();
        launch(args);
    }

    @Override
    public void start(Stage stage) throws IOException {
        App.application = this;
        App.primaryStage = stage;

        var fxmlLoader = new FXMLLoader(App.class.getResource("app.fxml"));
        var scene = new Scene(fxmlLoader.load());

        controller = fxmlLoader.getController();

        stage.setMinWidth(1000.0D);
        stage.setMinHeight(700.0D);
        stage.setWidth(1000.0D);
        stage.setHeight(700.0D);

        stage.setTitle("IceBox Application");
        stage.setScene(scene);
        stage.show();
    }
}