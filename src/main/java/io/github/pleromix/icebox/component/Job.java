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
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;

import java.util.Objects;

public class Job extends AnchorPane {

    private final HBox header = new HBox();
    private final Region statusIcon = new Region();
    private final Label titleLabel = new Label();
    private final Button cancelButton = new Button();
    private final Button linkButton = new Button();
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
        create(titleWhileRunning, titleWhileFinished, titleWhileFailed, null, task);
    }

    public static void create(String titleWhileRunning, String titleWhileFinished, String titleWhileFailed, String linkPath, Task<?> task) {
        final var panelContent = (VBox) App.getController().jobsPanel.getContent();
        final var job = new Job();
        final var toggleButton = App.getController().jobsToggleButton;
        final var tooltip = new Tooltip();

        Tooltip.install(job, tooltip);

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
        job.cancelButton.setOnAction(event -> job.cancel.run());

        tooltip.setText(titleWhileRunning);

        task.addEventFilter(WorkerStateEvent.WORKER_STATE_SUCCEEDED, e -> {
            job.timeline.play();
            job.titleLabel.setText(titleWhileFinished);
            tooltip.setText(titleWhileFinished);

            if (Objects.nonNull(linkPath)) {
                job.titleLabel.setGraphic(job.linkButton);

                job.linkButton.setOnAction(event -> {
                    App.getApplication().getHostServices().showDocument(linkPath);
                });
            }
        });

        task.addEventFilter(WorkerStateEvent.WORKER_STATE_FAILED, e -> {
            job.statusIcon.getStyleClass().remove("succeeded");
            job.statusIcon.getStyleClass().add("failed");
            job.timeline.play();
            job.titleLabel.setText(titleWhileFailed);
            tooltip.setText(titleWhileFailed);
            Notification.create("Warning", "Something went wrong!");
        });

        Thread.ofPlatform().start(task);

        panelContent.getChildren().addFirst(job);

        if (!toggleButton.isSelected()) {
            toggleButton.fire();
        }
    }

    private void initialize() {
        final var cancelIcon = new Region();
        final var linkIcon = new Region();
        final var cancelTooltip = new Tooltip("Cancel job");
        final var linkTooltip = new Tooltip("Open file");

        setMinHeight(AnchorPane.USE_PREF_SIZE);
        setPrefHeight(42.0D);

        statusIcon.setMinWidth(Region.USE_PREF_SIZE);
        statusIcon.setMinHeight(Region.USE_PREF_SIZE);
        statusIcon.getStyleClass().addAll("status-icon", "succeeded");

        cancelIcon.getStyleClass().add("close-icon");
        linkIcon.getStyleClass().add("link-icon");

        cancelButton.setMinWidth(Button.USE_PREF_SIZE);
        cancelButton.setMinHeight(Button.USE_PREF_SIZE);

        linkButton.setMinWidth(Button.USE_PREF_SIZE);
        linkButton.setMinHeight(Button.USE_PREF_SIZE);

        HBox.setHgrow(titleLabel, Priority.ALWAYS);
        titleLabel.setMaxWidth(Double.MAX_VALUE);
        titleLabel.setContentDisplay(ContentDisplay.RIGHT);

        progressBar.setMaxWidth(Double.MAX_VALUE);

        getStyleClass().add("job");
        cancelButton.getStyleClass().add("icon-round-button");
        linkButton.getStyleClass().add("link-button");

        cancelButton.setGraphic(cancelIcon);
        cancelButton.setTooltip(cancelTooltip);

        linkIcon.setScaleX(0.0D);
        linkIcon.setScaleY(0.0D);

        linkButton.setGraphic(linkIcon);
        linkButton.setTooltip(linkTooltip);

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
        timeline.getKeyFrames().add(
                new KeyFrame(
                        Duration.millis(250),
                        new KeyValue(statusIcon.scaleXProperty(), 0.0D),
                        new KeyValue(statusIcon.scaleYProperty(), 0.0D),
                        new KeyValue(statusIcon.prefWidthProperty(), 0.0D),
                        new KeyValue(statusIcon.prefHeightProperty(), 0.0D),
                        new KeyValue(titleLabel.paddingProperty(), new Insets(0.0D), Interpolator.EASE_OUT),
                        new KeyValue(linkIcon.scaleXProperty(), 0.0D),
                        new KeyValue(linkIcon.scaleYProperty(), 0.0D)
                )
        );
        timeline.getKeyFrames().add(
                new KeyFrame(Duration.millis(350),
                        new KeyValue(statusIcon.scaleXProperty(), 1.0D),
                        new KeyValue(statusIcon.scaleYProperty(), 1.0D),
                        new KeyValue(statusIcon.prefWidthProperty(), 18.0D),
                        new KeyValue(statusIcon.prefHeightProperty(), 18.0D),
                        new KeyValue(titleLabel.paddingProperty(), new Insets(0.0D, 0.0D, 0.0D, 8.0D), Interpolator.EASE_OUT),
                        new KeyValue(linkIcon.scaleXProperty(), 1.0D),
                        new KeyValue(linkIcon.scaleYProperty(), 1.0D)
                )
        );
        timeline.getKeyFrames().add(new KeyFrame(Duration.millis(350), e -> cancelTooltip.setText("Remove"), new KeyValue(bottomAnchorProperty, 0.0D)));

        header.setFillHeight(false);
        header.setAlignment(Pos.CENTER);
        header.setSpacing(2.0D);

        header.getChildren().addAll(statusIcon, titleLabel, cancelButton);
        getChildren().addAll(header, progressBar);
    }
}
