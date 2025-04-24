package io.github.pleromix.icebox.component;

import io.github.pleromix.icebox.App;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.util.Duration;

import java.util.Objects;
import java.util.Stack;

public class Notification extends VBox {

    public static Stack<Notification> notifications = new Stack<>();

    private final HBox header = new HBox();
    private final Label titleLabel = new Label();
    private final Button closeButton = new Button();
    private final Label contentLabel = new Label();
    private final Hyperlink link = new Hyperlink();
    private final Timeline timeline = new Timeline();

    public Notification() {
        super();
        initialize();
    }

    public static void create(String title, String content, Duration timeout, String linkText, EventHandler<ActionEvent> linkEvent) {
        final var notification = new Notification();

        if (!notifications.isEmpty()) {
            notifications.pop().timeline.playFrom("close");
        }

        notification.titleLabel.setText(title);
        notification.contentLabel.setText(content);

        if (Objects.nonNull(linkText) && !linkText.isEmpty()) {
            notification.link.setText(linkText);
            notification.link.setOnAction(linkEvent);
            notification.getChildren().add(notification.link);
        }

        notification.timeline.getKeyFrames().add(new KeyFrame(Duration.ZERO, new KeyValue(notification.translateXProperty(), -332, Interpolator.EASE_OUT), new KeyValue(notification.opacityProperty(), 0.0D, Interpolator.EASE_OUT)));
        notification.timeline.getKeyFrames().add(new KeyFrame(Duration.millis(250), new KeyValue(notification.translateXProperty(), 0.0D, Interpolator.EASE_IN), new KeyValue(notification.opacityProperty(), 1.0D, Interpolator.EASE_IN)));
        notification.timeline.getKeyFrames().add(new KeyFrame(timeout, "close", new KeyValue(notification.translateXProperty(), 0.0D, Interpolator.EASE_IN), new KeyValue(notification.opacityProperty(), 1.0D, Interpolator.EASE_IN)));
        notification.timeline.getKeyFrames().add(new KeyFrame(timeout.add(Duration.millis(200)), e -> App.controller.root.getChildren().remove(notification), new KeyValue(notification.translateXProperty(), -332, Interpolator.EASE_OUT), new KeyValue(notification.opacityProperty(), 0.0D, Interpolator.EASE_OUT)));
        notification.timeline.play();

        notifications.push(notification);

        App.controller.root.getChildren().add(notification);
    }

    public static void create(String title, String content) {
        create(title, content, Duration.seconds(5), null, null);
    }

    public static void closeShowingNotification() {
        if (!notifications.isEmpty()) {
            notifications.pop().timeline.playFrom("close");
        }
    }

    private void initialize() {
        final var icon = new Region();

        icon.getStyleClass().add("close-icon");

        setMaxWidth(VBox.USE_PREF_SIZE);
        setMaxHeight(VBox.USE_PREF_SIZE);

        getStyleClass().add("notification");
        titleLabel.getStyleClass().add("title");
        closeButton.getStyleClass().add("icon-round-button");
        contentLabel.getStyleClass().add("content");

        closeButton.setGraphic(icon);

        StackPane.setAlignment(this, Pos.BOTTOM_LEFT);
        StackPane.setMargin(this, new Insets(32.0D));

        HBox.setHgrow(titleLabel, Priority.ALWAYS);
        titleLabel.setMaxWidth(Double.MAX_VALUE);

        VBox.setVgrow(contentLabel, Priority.ALWAYS);
        contentLabel.setMaxHeight(Double.MAX_VALUE);
        contentLabel.setWrapText(true);

        closeButton.setMinSize(Button.USE_PREF_SIZE, Button.USE_PREF_SIZE);
        closeButton.setOnAction(e -> timeline.playFrom("close"));

        setId("notification");

        header.setFillHeight(false);

        header.getChildren().addAll(titleLabel, closeButton);
        getChildren().addAll(header, contentLabel);
    }
}
