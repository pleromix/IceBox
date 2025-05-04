package io.github.pleromix.icebox.component;

import io.github.pleromix.icebox.App;
import io.github.pleromix.icebox.dto.ImageInfo;
import io.github.pleromix.icebox.util.Utility;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.css.PseudoClass;
import javafx.event.Event;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import lombok.Getter;

import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.Objects;
import java.util.TreeSet;

public class Thumbnail extends StackPane {

    public enum ViewSize {
        SMALL, NORMAL, LARGE
    }

    private static final TreeSet<Integer> selectedItems = new TreeSet<>(Comparator.naturalOrder());
    public static final double IMAGE_SIZE = 70.0D;
    public static final ObjectProperty<ViewSize> viewSizeProperty = new SimpleObjectProperty<>(ViewSize.NORMAL);

    private static Thumbnail draggedThumbnail;
    private static boolean isOutsideOfRoot;

    private final ImageView imageView = new ImageView();
    private final PseudoClass selected = PseudoClass.getPseudoClass("selected");
    private final PseudoClass dragged = PseudoClass.getPseudoClass("dragged");

    private Tooltip tooltip;

    @Getter
    private boolean isSelected;
    @Getter
    private ImageInfo imageInfo;

    public Thumbnail() {
        super();
        initialize();
    }

    private static void removeSelected() {
        final var repositoryContent = (FlowPane) App.getController().repository.getContent();
        final var thumbnails = repositoryContent.getChildren().stream().map(node -> (Thumbnail) node).filter(Thumbnail::isSelected).toList();

        for (var thumbnail : thumbnails) {
            repositoryContent.getChildren().remove(thumbnail);
        }

        reorderPages();

        App.getController().closeImageInfo();
        App.getController().updateDetails();
    }

    public static void removeAll() {
        selectAll();
        removeSelected();
    }

    public static Thumbnail create(File originalFile, Image thumbnailImage, int page) throws IOException {
        final var thumbnail = new Thumbnail();
        final var size = Utility.fileAsMat(originalFile).size();

        thumbnail.imageView.setImage(thumbnailImage);
        thumbnail.imageInfo = new ImageInfo(originalFile.getName(), originalFile.length(), originalFile, thumbnailImage, page, (int) Math.round(size.width), (int) Math.round(size.height));
        thumbnail.tooltip = new Tooltip(String.format("Page: %d", thumbnail.imageInfo.getPage()));
        Tooltip.install(thumbnail, thumbnail.tooltip);

        thumbnail.setSize(viewSizeProperty.getValue(), thumbnailImage);

        viewSizeProperty.addListener((observable, oldValue, newValue) -> thumbnail.setSize(newValue, thumbnailImage));

        return thumbnail;
    }

    private static void selectRange(int x, int y) {
        final var repositoryContent = (FlowPane) App.getController().repository.getContent();

        if (y == -1) {
            repositoryContent.getChildren().subList(x, repositoryContent.getChildren().size()).stream().map(node -> (Thumbnail) node).forEach(Thumbnail::select);
        } else {
            repositoryContent.getChildren().subList(x, y + 1).stream().map(node -> (Thumbnail) node).forEach(Thumbnail::select);
        }

        App.getController().closeImageInfo();
    }

    private static void reorderPages() {
        final var repositoryContent = (FlowPane) App.getController().repository.getContent();
        repositoryContent.getChildren().stream().map(node -> (Thumbnail) node).forEach(thumbnail -> {
            final var page = repositoryContent.getChildren().indexOf(thumbnail) + 1;
            thumbnail.imageInfo.setPage(page);
            thumbnail.tooltip.setText(String.format("Page: %d", page));
        });
    }

    public static void selectAll() {
        selectRange(0, -1);
    }

    private void initialize() {
        final var repositoryContent = (FlowPane) App.getController().repository.getContent();
        final var children = repositoryContent.getChildren();
        final var contextMenu = new ContextMenu();
        final var removeIcon = new Region();
        final var selectAllIcon = new Region();
        final var removeMenuItem = new MenuItem("Remove");
        final var selectAllMenuItem = new MenuItem("Select All");

        addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {

            var point = App.getController().repository.screenToLocal(event.getScreenX(), event.getScreenY());
            var bounds = App.getController().repository.getBoundsInParent();

            if (point.getX() <= bounds.getWidth() / 2) {
                StackPane.setAlignment(App.getController().imageInfoPanel, Pos.BOTTOM_RIGHT);
            } else if (point.getX() > bounds.getWidth() / 2) {
                StackPane.setAlignment(App.getController().imageInfoPanel, Pos.BOTTOM_LEFT);
            }

            final var index = children.indexOf(this);

            selectedItems.add(index);

            if (event.getButton() == MouseButton.PRIMARY) {
                if (event.isShiftDown()) {
                    if (selectedItems.size() >= 2) {
                        selectRange(selectedItems.getFirst(), selectedItems.getLast());
                        App.getController().closeImageInfo();
                        return;
                    }
                } else if (isSelected) {
                    deselect();
                    selectedItems.remove(index);
                    return;
                }

                select();
                imageInfo.setPage(index + 1);

                if (!event.isControlDown()) {
                    App.getController().setImageInfo(imageInfo.getName(), imageInfo.getSize(), imageInfo.getOriginalFile().getAbsolutePath(), imageInfo.getPage(), imageInfo.getWidth(), imageInfo.getHeight());
                } else {
                    App.getController().closeImageInfo();
                }
            }
        });

        addEventFilter(MouseDragEvent.DRAG_DETECTED, event -> {
            deselectAll();
            draggedThumbnail = this;
            startFullDrag();
            App.getController().root.setCursor(Cursor.CLOSED_HAND);
            pseudoClassStateChanged(dragged, true);
            event.consume();
        });

        addEventFilter(MouseDragEvent.MOUSE_DRAG_ENTERED, event -> {
            if (Objects.nonNull(draggedThumbnail) && draggedThumbnail != this) {
                final var index = children.indexOf(this);
                children.remove(draggedThumbnail);
                children.add(index, draggedThumbnail);
            }
        });

        Runnable mouseDragReleasedAction = () -> {
            App.getController().root.setCursor(Cursor.DEFAULT);
            reorderPages();
            pseudoClassStateChanged(dragged, false);

            if (Objects.nonNull(draggedThumbnail)) {
                draggedThumbnail = null;
            }
        };

        App.getController().root.addEventFilter(MouseDragEvent.MOUSE_DRAG_RELEASED, e -> mouseDragReleasedAction.run());
        App.getController().root.addEventFilter(MouseEvent.MOUSE_ENTERED, e -> isOutsideOfRoot = false);
        App.getController().root.addEventFilter(MouseEvent.MOUSE_EXITED, e -> isOutsideOfRoot = true);

        App.getPrimaryStage().addEventFilter(MouseEvent.MOUSE_RELEASED, event -> {
            if (isOutsideOfRoot) {
                mouseDragReleasedAction.run();
            }
        });

        App.getController().repository.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
            if (event.getTarget() != this && event.getButton() != MouseButton.SECONDARY && !(event.isControlDown() || event.isShiftDown())) {
                deselect();
                selectedItems.clear();
            }
        });

        App.getController().repository.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.DELETE) {
                removeSelected();
            }
        });

        removeMenuItem.setOnAction(event -> removeSelected());
        selectAllMenuItem.setOnAction(event -> selectAll());
        removeIcon.getStyleClass().addAll("icon", "remove-icon");
        selectAllIcon.getStyleClass().addAll("icon", "select-icon");
        removeMenuItem.setGraphic(removeIcon);
        selectAllMenuItem.setGraphic(selectAllIcon);
        contextMenu.getItems().addAll(removeMenuItem, selectAllMenuItem);

        addEventFilter(ContextMenuEvent.CONTEXT_MENU_REQUESTED, Event::consume);

        addEventFilter(MouseEvent.MOUSE_CLICKED, mouseEvent -> {
            if (isSelected) {
                if (mouseEvent.getButton() == MouseButton.SECONDARY) {
                    var point2D = localToScreen(mouseEvent.getX(), mouseEvent.getY());
                    contextMenu.show(this, point2D.getX(), point2D.getY());
                }
            } else {
                contextMenu.hide();
            }
        });

        App.getController().imageFileRemoveButton.setOnAction(event -> removeMenuItem.fire());

        imageView.setFitWidth(IMAGE_SIZE);
        imageView.setFitHeight(IMAGE_SIZE);
        imageView.setPreserveRatio(true);
        imageView.setMouseTransparent(true);
        imageView.setSmooth(true);

        StackPane.setMargin(imageView, new Insets(8.0D));

        getStyleClass().add("thumbnail");
        getChildren().add(imageView);
    }

    private void setSize(ViewSize newValue, Image thumbnailImage) {
        switch (newValue) {
            case SMALL:
                calculateSize(1.25D, thumbnailImage);
                break;
            case NORMAL:
                calculateSize(2.0D, thumbnailImage);
                break;
            case LARGE:
                calculateSize(3.0D, thumbnailImage);
                break;
        }
    }

    public void select() {
        isSelected = true;
        pseudoClassStateChanged(selected, true);
    }

    public void deselect() {
        isSelected = false;
        pseudoClassStateChanged(selected, false);
        App.getController().closeImageInfo();
    }

    public void deselectAll() {
        final var repositoryContent = (FlowPane) App.getController().repository.getContent();
        repositoryContent.getChildren().stream().map(node -> (Thumbnail) node).forEach(Thumbnail::deselect);
    }

    private void calculateSize(double factor, Image thumbnailImage) {
        if (Math.max(thumbnailImage.getWidth(), thumbnailImage.getHeight()) < IMAGE_SIZE) {
            imageView.setFitWidth(thumbnailImage.getWidth());
            imageView.setFitHeight(thumbnailImage.getHeight());
        } else {
            imageView.setFitWidth(IMAGE_SIZE * factor);
            imageView.setFitHeight(IMAGE_SIZE * factor);
        }

        setMinSize(IMAGE_SIZE * factor + 8.0D, IMAGE_SIZE * factor + 8.0D);
        setPrefSize(IMAGE_SIZE * factor + 8.0D, IMAGE_SIZE * factor + 8.0D);
        setMaxSize(IMAGE_SIZE * factor + 8.0D, IMAGE_SIZE * factor + 8.0D);
    }
}
