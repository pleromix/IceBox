package io.github.pleromix.icebox.component;

import io.github.pleromix.icebox.App;
import io.github.pleromix.icebox.dto.ImageInfo;
import io.github.pleromix.icebox.util.Utility;
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

    private static final TreeSet<Integer> selectedItems = new TreeSet<>(Comparator.naturalOrder());

    private static Thumbnail draggedThumbnail;

    private final ImageView imageView = new ImageView();
    private final PseudoClass selected = PseudoClass.getPseudoClass("selected");

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
        final var repositoryContent = (FlowPane) App.controller.repository.getContent();
        final var thumbnails = repositoryContent.getChildren().stream().map(node -> (Thumbnail) node).filter(Thumbnail::isSelected).toList();

        for (var thumbnail : thumbnails) {
            repositoryContent.getChildren().remove(thumbnail);
        }

        reorderPages();

        App.controller.closeImageInfo();
        App.controller.updateDetails();
    }

    public static void removeAll() {
        selectAll();
        removeSelected();
    }

    public static Thumbnail create(File originalFile, File thumbnailFile, int page) throws IOException {
        final var thumbnail = new Thumbnail();
        final var size = Utility.getFileAsImage(originalFile).size();

        thumbnail.imageView.setImage(new Image(thumbnailFile.toURI().toString()));

        thumbnail.imageInfo = new ImageInfo(originalFile.getName(), originalFile.length(), originalFile, thumbnailFile, page, (int) Math.round(size.width), (int) Math.round(size.height));
        thumbnail.tooltip = new Tooltip(String.format("Page: %d", thumbnail.imageInfo.getPage()));
        Tooltip.install(thumbnail, thumbnail.tooltip);

        return thumbnail;
    }

    private static void selectRange(int x, int y) {
        final var repositoryContent = (FlowPane) App.controller.repository.getContent();

        if (y == -1) {
            repositoryContent.getChildren().subList(x, repositoryContent.getChildren().size()).stream().map(node -> (Thumbnail) node).forEach(Thumbnail::select);
        } else {
            repositoryContent.getChildren().subList(x, y + 1).stream().map(node -> (Thumbnail) node).forEach(Thumbnail::select);
        }
    }

    private static void reorderPages() {
        final var repositoryContent = (FlowPane) App.controller.repository.getContent();
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
        final var repositoryContent = (FlowPane) App.controller.repository.getContent();
        final var children = repositoryContent.getChildren();
        final var contextMenu = new ContextMenu();
        final var icon = new Region();
        final var removeMenuItem = new MenuItem("Remove");

        addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {

            var point = App.controller.repository.screenToLocal(event.getScreenX(), event.getScreenY());
            var bounds = App.controller.repository.getBoundsInParent();

            if (point.getX() <= bounds.getWidth() / 2) {
                StackPane.setAlignment(App.controller.imageInfoPanel, Pos.TOP_RIGHT);
            } else if (point.getX() > bounds.getWidth() / 2) {
                StackPane.setAlignment(App.controller.imageInfoPanel, Pos.TOP_LEFT);
            }

            final var index = children.indexOf(this);

            selectedItems.add(index);

            if (event.getButton() == MouseButton.PRIMARY) {
                if (event.isShiftDown()) {
                    if (selectedItems.size() >= 2) {
                        selectRange(selectedItems.getFirst(), selectedItems.getLast());
                        App.controller.closeImageInfo();
                        return;
                    }
                } else if (isSelected) {
                    unSelect();
                    selectedItems.remove(index);
                    return;
                }

                select();
                imageInfo.setPage(index + 1);

                if (!event.isControlDown()) {
                    App.controller.setImageInfo(imageInfo.getName(), imageInfo.getSize(), imageInfo.getOriginalFile().getAbsolutePath(), imageInfo.getPage(), imageInfo.getWidth(), imageInfo.getHeight());
                } else {
                    App.controller.closeImageInfo();
                }
            }
        });

        addEventFilter(MouseDragEvent.DRAG_DETECTED, event -> {
            draggedThumbnail = this;
            startFullDrag();
            setOpacity(0.75D);
            App.controller.root.setCursor(Cursor.CLOSED_HAND);
            event.consume();
        });

        addEventFilter(MouseDragEvent.MOUSE_DRAG_ENTERED, event -> {
            if (Objects.nonNull(draggedThumbnail) && draggedThumbnail != this) {
                final var index = children.indexOf(this);
                children.remove(draggedThumbnail);
                children.add(index, draggedThumbnail);
            }
        });

        App.controller.root.addEventFilter(MouseDragEvent.MOUSE_DRAG_RELEASED, event -> {
            App.controller.root.setCursor(Cursor.DEFAULT);
            draggedThumbnail.setOpacity(1.0D);
            reorderPages();
            draggedThumbnail = null;
        });

        App.controller.repository.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
            if (event.getTarget() != this && event.getButton() != MouseButton.SECONDARY && !(event.isControlDown() || event.isShiftDown())) {
                unSelect();
                selectedItems.clear();
            }
        });

        App.controller.repository.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.DELETE) {
                removeSelected();
            }
        });

        removeMenuItem.setOnAction(event -> removeSelected());

        icon.getStyleClass().addAll("icon", "remove-icon");

        removeMenuItem.setGraphic(icon);

        contextMenu.getItems().add(removeMenuItem);

        addEventFilter(ContextMenuEvent.CONTEXT_MENU_REQUESTED, Event::consume);
        addEventFilter(MouseEvent.MOUSE_CLICKED, mouseEvent -> {
            if (isSelected) {
                if (mouseEvent.getButton() == MouseButton.SECONDARY) {
                    var point2D = localToScreen(mouseEvent.getX(), mouseEvent.getY());
                    contextMenu.show(this, point2D.getX(), point2D.getY());
                } else {
                    contextMenu.hide();
                }
            }
        });

        App.controller.imageFileRemoveButton.setOnAction(event -> removeMenuItem.fire());

        imageView.setFitWidth(120.0D);
        imageView.setFitHeight(120.0D);
        imageView.setPreserveRatio(true);
        imageView.setMouseTransparent(true);

        StackPane.setMargin(imageView, new Insets(8.0D));

        getStyleClass().add("thumbnail");
        getChildren().add(imageView);
    }

    public void select() {
        isSelected = true;
        pseudoClassStateChanged(selected, true);
    }

    public void unSelect() {
        isSelected = false;
        pseudoClassStateChanged(selected, false);
        App.controller.closeImageInfo();
    }
}
