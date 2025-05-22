package io.github.pleromix.icebox.component;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

import java.util.Objects;

public class ToggleSwitch extends Pane {

    private final ObjectProperty<EventHandler<MouseEvent>> onToggleProperty = new SimpleObjectProperty<>();
    private final BooleanProperty selected = new SimpleBooleanProperty(false);
    private final Label onLabel = new Label("");
    private final Label offLabel = new Label("");
    private final Circle thumb = new Circle(6.0D);

    public ToggleSwitch() {
        super();
        initialize();
    }

    private void initialize() {
        onLabel.setAlignment(Pos.CENTER_LEFT);
        offLabel.setAlignment(Pos.CENTER_RIGHT);

        onLabel.getStyleClass().add("toggle-switch-on");
        offLabel.getStyleClass().add("toggle-switch-off");

        onLabel.setPrefSize(24.0D, 16.0D);
        offLabel.setPrefSize(24.0D, 16.0D);

        thumb.setStroke(Color.TRANSPARENT);
        thumb.setFill(Color.valueOf("#fff"));
        thumb.setCenterY(8.0D);

        onLabel.setMouseTransparent(true);
        offLabel.setMouseTransparent(true);
        thumb.setMouseTransparent(true);

        setCursor(Cursor.HAND);

        if (isSelected()) {
            thumb.setCenterX(16.0D);
            onLabel.setOpacity(1.0D);
            offLabel.setOpacity(0.0D);
        } else {
            thumb.setCenterX(8.0D);
            onLabel.setOpacity(0.0D);
            offLabel.setOpacity(1.0D);
        }

        getChildren().addAll(offLabel, onLabel, thumb);

        selected.addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                new Timeline(
                        new KeyFrame(
                                Duration.ZERO,
                                new KeyValue(
                                        thumb.centerXProperty(),
                                        8.0D
                                ),
                                new KeyValue(
                                        onLabel.opacityProperty(),
                                        0.0D
                                ),
                                new KeyValue(
                                        offLabel.opacityProperty(),
                                        1.0D
                                )
                        ),
                        new KeyFrame(
                                Duration.millis(100.0D),
                                new KeyValue(
                                        thumb.centerXProperty(),
                                        16.0D
                                ),
                                new KeyValue(
                                        onLabel.opacityProperty(),
                                        1.0D
                                ),
                                new KeyValue(
                                        offLabel.opacityProperty(),
                                        0.0D
                                )
                        )
                ).play();
            } else {
                new Timeline(
                        new KeyFrame(
                                Duration.ZERO,
                                new KeyValue(
                                        thumb.centerXProperty(),
                                        16.0D
                                ),
                                new KeyValue(
                                        onLabel.opacityProperty(),
                                        1.0D
                                ),
                                new KeyValue(
                                        offLabel.opacityProperty(),
                                        0.0D
                                )
                        ),
                        new KeyFrame(
                                Duration.millis(100.0D),
                                new KeyValue(
                                        thumb.centerXProperty(),
                                        8.0D
                                ),
                                new KeyValue(
                                        onLabel.opacityProperty(),
                                        0.0D
                                ),
                                new KeyValue(
                                        offLabel.opacityProperty(),
                                        1.0D
                                )
                        )
                ).play();
            }
        });

        EventHandler<MouseEvent> mouseClickedEventHandler = event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                selected.set(!selected.get());

                if (Objects.nonNull(onToggleProperty.get())) {
                    onToggleProperty.get().handle(event);
                }
            }
        };

        addEventFilter(MouseEvent.MOUSE_CLICKED, mouseClickedEventHandler);

        Platform.runLater(() -> {
            if (Objects.nonNull(getParent()) && getParent() instanceof Label) {
                getParent().addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
                    if (!(event.getTarget() instanceof ToggleSwitch)) {
                        mouseClickedEventHandler.handle(event);
                    }
                });
            }
        });
    }

    public BooleanProperty selectedProperty() {
        return selected;
    }

    public void setSelected(boolean selected) {
        selectedProperty().set(selected);
    }

    public boolean isSelected() {
        return selectedProperty().get();
    }

    public ObjectProperty<EventHandler<MouseEvent>> onToggleProperty() {
        return onToggleProperty;
    }

    public void setOnToggle(EventHandler<MouseEvent> onToggle) {
        onToggleProperty().set(onToggle);
    }

    public EventHandler<MouseEvent> getOnToggle() {
        return onToggleProperty().get();
    }
}
