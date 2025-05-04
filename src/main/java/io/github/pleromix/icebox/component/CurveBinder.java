package io.github.pleromix.icebox.component;

import io.github.pleromix.icebox.util.Utility;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.*;
import javafx.util.Duration;
import org.apache.commons.lang3.tuple.Pair;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

public class CurveBinder extends Group {

    public static final double CIRCLE_STROKE_WIDTH = 2.0D;
    public static final double CIRCLE_RADIUS = 5.0D;
    public static final Paint STROKE_COLOR = Paint.valueOf("#999");
    public static final Color CIRCLE_FILL_COLOR = Color.WHITE;
    public static final Paint STROKE_COLOR_SELECTED = Paint.valueOf("#27fa");
    public static final double TOGGLE_BUTTON_WIDTH = 100.0D;
    public static final Duration CURVE_ANIMATION_DURATION = Duration.millis(100.0D);
    public static final double LINE_STROKE_WIDTH = 2.0D;

    private final GridPane grid = new GridPane();
    private final ToggleGroup leftToggleGroup = new ToggleGroup();
    private final ToggleGroup rightToggleGroup = new ToggleGroup();
    private final CubicCurve curve = new CubicCurve();
    private final Polyline arrow = new Polyline();
    private final int leftMax;
    private final int rightMax;

    private ToggleButton leftSelectedToggleButton;
    private ToggleButton rightSelectedToggleButton;

    public CurveBinder() {
        this(
                List.of(
                        "Original",
                        "High",
                        "Medium",
                        "Low"
                ),
                List.of(
                        "Original",
                        "Enhanced",
                        "Standard",
                        "Basic"
                )
        );
    }

    public CurveBinder(List<String> leftSideControls, List<String> rightSideControls) {
        super();
        leftMax = leftSideControls.size();
        rightMax = rightSideControls.size();
        initialize(leftSideControls, rightSideControls);
    }

    private void initialize(List<String> leftSideControls, List<String> rightSideControls) {
        final var rowCount = Math.max(leftSideControls.size(), rightSideControls.size());

        IntStream.range(0, rowCount + 1).forEach(i -> {
            final var rowConstraints = new RowConstraints();
            grid.getRowConstraints().add(rowConstraints);
        });

        IntStream.range(0, 2).forEach(i -> {
            final var columnConstraints = new ColumnConstraints();
            columnConstraints.setPercentWidth(ColumnConstraints.CONSTRAIN_TO_PREF);
            grid.getColumnConstraints().add(columnConstraints);
        });

        IntStream.range(0, leftSideControls.size()).forEach(i -> {
            final var toggleButton = new ToggleButton();
            final var circle = new Circle();

            circle.setRadius(CIRCLE_RADIUS);
            circle.setStrokeWidth(CIRCLE_STROKE_WIDTH);
            circle.setStroke(STROKE_COLOR);
            circle.setFill(Color.WHITE);
            circle.setStrokeType(StrokeType.CENTERED);

            toggleButton.setPrefWidth(TOGGLE_BUTTON_WIDTH);
            toggleButton.setText(leftSideControls.get(i));
            toggleButton.setToggleGroup(leftToggleGroup);
            toggleButton.setContentDisplay(ContentDisplay.RIGHT);
            toggleButton.setGraphic(circle);
            toggleButton.setAlignment(Pos.CENTER_RIGHT);
            toggleButton.setMaxWidth(Double.MAX_VALUE);
            toggleButton.setUserData(leftMax - i);

            toggleButton.selectedProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue) {
                    curve.setStartX(toggleButton.getBoundsInParent().getMaxX());
                    curve.setControlX1(toggleButton.getBoundsInParent().getMaxX() + grid.getHgap() * 50.0D / 100.0D);
                    circle.setStroke(STROKE_COLOR_SELECTED);

                    new Timeline(
                            new KeyFrame(
                                    CURVE_ANIMATION_DURATION,
                                    new KeyValue(
                                            curve.startYProperty(),
                                            toggleButton.getBoundsInParent().getCenterY()
                                    ),
                                    new KeyValue(
                                            curve.controlY1Property(),
                                            toggleButton.getBoundsInParent().getCenterY()
                                    )
                            )
                    ).play();
                } else {
                    circle.setStroke(STROKE_COLOR);
                }
            });

            GridPane.setHgrow(toggleButton, Priority.ALWAYS);

            grid.add(toggleButton, 0, i + 1);

            if (i == 1) {
                leftSelectedToggleButton = toggleButton;
            }
        });

        IntStream.range(0, rightSideControls.size()).forEach(i -> {
            final var toggleButton = new ToggleButton();
            final var circle = new Circle();

            circle.setRadius(CIRCLE_RADIUS);
            circle.setStrokeWidth(CIRCLE_STROKE_WIDTH);
            circle.setStroke(STROKE_COLOR);
            circle.setFill(CIRCLE_FILL_COLOR);
            circle.setStrokeType(StrokeType.CENTERED);

            toggleButton.setPrefWidth(TOGGLE_BUTTON_WIDTH);
            toggleButton.setText(rightSideControls.get(i));
            toggleButton.setToggleGroup(rightToggleGroup);
            toggleButton.setContentDisplay(ContentDisplay.LEFT);
            toggleButton.setGraphic(circle);
            toggleButton.setAlignment(Pos.CENTER_LEFT);
            toggleButton.setMaxWidth(Double.MAX_VALUE);
            toggleButton.setUserData(rightMax - i);

            toggleButton.selectedProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue) {
                    curve.setEndX(toggleButton.getBoundsInParent().getMinX());
                    curve.setControlX2(toggleButton.getBoundsInParent().getMinX() - grid.getHgap() * 50.0D / 100.0D);
                    arrow.setLayoutX(toggleButton.getBoundsInParent().getMinX() - 10.0D);
                    circle.setStroke(STROKE_COLOR_SELECTED);

                    new Timeline(
                            new KeyFrame(
                                    CURVE_ANIMATION_DURATION,
                                    new KeyValue(
                                            curve.endYProperty(),
                                            toggleButton.getBoundsInParent().getCenterY()
                                    ),
                                    new KeyValue(
                                            curve.controlY2Property(),
                                            toggleButton.getBoundsInParent().getCenterY()
                                    ),
                                    new KeyValue(
                                            arrow.layoutYProperty(),
                                            toggleButton.getBoundsInParent().getCenterY() - 5.0D
                                    )
                            )
                    ).play();
                } else {
                    circle.setStroke(STROKE_COLOR);
                }
            });

            GridPane.setHgrow(toggleButton, Priority.ALWAYS);

            grid.add(toggleButton, 1, i + 1);

            if (i == 2) {
                rightSelectedToggleButton = toggleButton;
            }
        });

        grid.setHgap(128.0D);
        grid.setVgap(8.0D);

        curve.setFill(Color.TRANSPARENT);
        curve.setStroke(STROKE_COLOR_SELECTED);
        curve.setStrokeWidth(LINE_STROKE_WIDTH);
        curve.setStrokeLineCap(StrokeLineCap.BUTT);

        arrow.setFill(Color.TRANSPARENT);
        arrow.setStroke(STROKE_COLOR_SELECTED);
        arrow.setStrokeWidth(LINE_STROKE_WIDTH);
        arrow.setStrokeLineCap(StrokeLineCap.BUTT);

        arrow.getPoints().addAll(0.0D, 0.0D, 10.0D, 5.0D, 0.0D, 10.0D);

        final var leftTitleLabel = new Label("Resolution");
        final var rightTitleLabel = new Label("Quality");

        grid.add(leftTitleLabel, 0, 0);
        grid.add(rightTitleLabel, 1, 0);

        GridPane.setHalignment(leftTitleLabel, HPos.CENTER);
        GridPane.setHalignment(rightTitleLabel, HPos.CENTER);

        leftToggleGroup.selectedToggleProperty().addListener((observable, oldValue, newValue) -> {
            if (Objects.isNull(newValue)) {
                leftToggleGroup.selectToggle(oldValue);
            }
        });

        rightToggleGroup.selectedToggleProperty().addListener((observable, oldValue, newValue) -> {
            if (Objects.isNull(newValue)) {
                rightToggleGroup.selectToggle(oldValue);
            }
        });

        getChildren().addAll(grid, curve, arrow);

        Platform.runLater(() -> {
            curve.setStartY(leftSelectedToggleButton.getBoundsInParent().getCenterY());
            curve.setControlY1(leftSelectedToggleButton.getBoundsInParent().getCenterY());
            curve.setEndY(rightSelectedToggleButton.getBoundsInParent().getCenterY());
            curve.setControlY2(rightSelectedToggleButton.getBoundsInParent().getCenterY());
            arrow.setLayoutY(rightSelectedToggleButton.getBoundsInParent().getCenterY() - 5.0D);
            leftToggleGroup.selectToggle(leftSelectedToggleButton);
            rightToggleGroup.selectToggle(rightSelectedToggleButton);
        });
    }

    public Pair<Double, Integer> getValue() {
        final var leftValue = (int) leftToggleGroup.getSelectedToggle().getUserData();
        final var rightValue = (int) rightToggleGroup.getSelectedToggle().getUserData();
        return Pair.of(
                BigDecimal.valueOf(Math.pow(Utility.normalize(leftValue, 0.0D, leftMax), 0.6D)).setScale(2, RoundingMode.HALF_UP).doubleValue(),
                (int) Math.floor(Math.pow(Utility.normalize(rightValue, 0.0D, rightMax), 0.6D) * 100.0D)
        );
    }
}
