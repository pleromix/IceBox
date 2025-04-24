package io.github.pleromix.icebox.component;

import io.github.pleromix.icebox.App;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.*;
import javafx.util.Duration;

public class Job extends AnchorPane {

    private final HBox header = new HBox();
    private final Region statusIcon = new Region();
    private final Label titleLabel = new Label();
    private final Button cancelButton = new Button();
    private final ProgressBar progressBar = new ProgressBar();
    private final Timeline timeline = new Timeline();
    private final DoubleProperty bottomAnchorProperty = new SimpleDoubleProperty(4.0D);

    public Runnable cancel;
    public Runnable clear;

    public Job() {
        super();
        initialize();
    }

    public static void create(String titleWhileRunning, String titleWhileFinished, String titleWhileFailed, Task<?> task) {
        final var panelContent = (VBox) App.controller.jobsPanel.getContent();
        final var job = new Job();
        final var toggleButton = App.controller.currentJobsToggleButton;

        job.cancel = () -> {
            task.cancel();
            job.clear.run();
        };

        job.clear = () -> {
            if (task.isDone()) {
                panelContent.getChildren().remove(job);
            }
        };

        task.setOnCancelled(event -> {
            panelContent.getChildren().remove(job);
        });

        job.titleLabel.setText(titleWhileRunning);
        job.progressBar.setProgress(Double.NEGATIVE_INFINITY);
        job.progressBar.progressProperty().bind(task.progressProperty());
        job.cancelButton.setOnAction(event -> {
            job.cancel.run();
        });

        task.addEventFilter(WorkerStateEvent.WORKER_STATE_SUCCEEDED, e -> {
            job.timeline.play();
            job.titleLabel.setText(titleWhileFinished);
        });

        task.addEventFilter(WorkerStateEvent.WORKER_STATE_FAILED, e -> {
            job.statusIcon.getStyleClass().remove("succeeded");
            job.statusIcon.getStyleClass().add("failed");
            job.timeline.play();
            job.titleLabel.setText(titleWhileFailed);
            Notification.create("Warning", "Something went wrong!");
        });

        Thread.ofPlatform().start(task);

        panelContent.getChildren().addFirst(job);

        if (!toggleButton.isSelected()) {
            toggleButton.fire();
        }
    }

    private void initialize() {
        final var icon = new Region();
        final var tooltip = new Tooltip("Cancel the job");

        setPrefHeight(42.0D);

        statusIcon.setMinWidth(Region.USE_PREF_SIZE);
        statusIcon.setMinHeight(Region.USE_PREF_SIZE);
        statusIcon.getStyleClass().addAll("status-icon", "succeeded");

        icon.getStyleClass().add("close-icon");

        cancelButton.setMinWidth(Button.USE_PREF_SIZE);
        cancelButton.setMinHeight(Button.USE_PREF_SIZE);

        HBox.setHgrow(titleLabel, Priority.ALWAYS);
        titleLabel.setMaxWidth(Double.MAX_VALUE);

        progressBar.setMaxWidth(Double.MAX_VALUE);

        getStyleClass().add("job");
        cancelButton.getStyleClass().add("icon-round-button");

        cancelButton.setGraphic(icon);
        cancelButton.setTooltip(tooltip);

        AnchorPane.setTopAnchor(header, 0.0D);
        AnchorPane.setRightAnchor(header, 0.0D);
        AnchorPane.setBottomAnchor(header, bottomAnchorProperty.doubleValue());
        AnchorPane.setLeftAnchor(header, 0.0D);

        bottomAnchorProperty.addListener((obs, oldVal, newVal) -> {
            AnchorPane.setBottomAnchor(header, newVal.doubleValue());
        });

        AnchorPane.setRightAnchor(progressBar, 0.0D);
        AnchorPane.setBottomAnchor(progressBar, 0.0D);
        AnchorPane.setLeftAnchor(progressBar, 0.0D);

        timeline.getKeyFrames().add(new KeyFrame(Duration.ZERO, new KeyValue(progressBar.opacityProperty(), 1.0D)));
        timeline.getKeyFrames().add(new KeyFrame(Duration.millis(150), new KeyValue(bottomAnchorProperty, bottomAnchorProperty.doubleValue())));
        timeline.getKeyFrames().add(new KeyFrame(Duration.millis(200), e -> getChildren().remove(progressBar), new KeyValue(progressBar.opacityProperty(), 0.0D)));
        timeline.getKeyFrames().add(new KeyFrame(Duration.millis(250), new KeyValue(statusIcon.scaleXProperty(), 0.0D), new KeyValue(statusIcon.scaleYProperty(), 0.0D), new KeyValue(statusIcon.prefWidthProperty(), 0.0D), new KeyValue(statusIcon.prefHeightProperty(), 0.0D), new KeyValue(titleLabel.paddingProperty(), new Insets(0.0D), Interpolator.EASE_OUT)));
        timeline.getKeyFrames().add(new KeyFrame(Duration.millis(350), new KeyValue(statusIcon.scaleXProperty(), 1.0D), new KeyValue(statusIcon.scaleYProperty(), 1.0D), new KeyValue(statusIcon.prefWidthProperty(), 18.0D), new KeyValue(statusIcon.prefHeightProperty(), 18.0D), new KeyValue(titleLabel.paddingProperty(), new Insets(0.0D, 0.0D, 0.0D, 8.0D), Interpolator.EASE_OUT)));
        timeline.getKeyFrames().add(new KeyFrame(Duration.millis(350), e -> tooltip.setText("Remove"), new KeyValue(bottomAnchorProperty, 0.0D)));

        header.setFillHeight(false);
        header.setAlignment(Pos.CENTER);

        header.getChildren().addAll(statusIcon, titleLabel, cancelButton);
        getChildren().addAll(header, progressBar);
    }
}
