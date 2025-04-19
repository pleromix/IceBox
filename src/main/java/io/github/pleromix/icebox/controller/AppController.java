package io.github.pleromix.icebox.controller;

import com.digidemic.unitof.UnitOf;
import io.github.pleromix.icebox.App;
import io.github.pleromix.icebox.component.*;
import io.github.pleromix.icebox.dto.PageSize;
import io.github.pleromix.icebox.util.Utility;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.ListChangeListener;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import javafx.util.Pair;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.io.IOException;
import java.net.URL;
import java.util.*;

public class AppController implements Initializable {
    @FXML
    public StackPane root;
    @FXML
    public SplitPane mainSplitPane;
    @FXML
    public ScrollPane jobsPanel;
    @FXML
    public ScrollPane repository;
    @FXML
    public GridPane imageInfoPanel;
    @FXML
    public ToggleButton currentJobsToggleButton;
    @FXML
    public Label pageCountLabel;
    @FXML
    public Label documentSizeLabel;
    @FXML
    public TextField pdfFileNameTextField;
    @FXML
    public TextField fileNameTextField;
    @FXML
    public TextField fileSizeTextField;
    @FXML
    public TextField filePathTextField;
    @FXML
    public TextField filePageNumberTextField;
    @FXML
    public TextField fileDimensionTextField;
    @FXML
    public Button createButton;
    @FXML
    public Button imageFileRemoveButton;
    @FXML
    public ChoiceBox<PageSize> pageSizeChoiceBox;
    @FXML
    public ChoiceBox<PageMargin> pageMarginChoiceBox;
    @FXML
    public ChoiceBox<PageOrientation> pageOrientationChoiceBox;

    private double jobsPanelCurrentDividerPosition;
    private PageSize defaultPageSize;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        final var repositoryContent = (FlowPane) repository.getContent();
        final var repositorySize = new SimpleIntegerProperty();

        setJobsPanelDummyLabel();
        setRepositoryDummyLabel();

        repository.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            if (event.isControlDown() && event.getCode() == KeyCode.A) {
                Thumbnail.selectAll();
                closeImageInfo();
            }
        });

        imageInfoPanel.setVisible(false);

        initPageSizeChoiceBox();
        initPageMarginChoiceBox();
        initPageOrientationChoiceBox();
        initPdfFileNameTextFieldContextMenu();

        repositoryContent.getChildren().addListener((InvalidationListener) observable -> repositorySize.set(repositoryContent.getChildren().size()));
        createButton.disableProperty().bind(pdfFileNameTextField.textProperty().isEmpty().or(repositorySize.isEqualTo(0)));

        jobsPanelCurrentDividerPosition = mainSplitPane.getDividers().getFirst().getPosition();
    }

    public void onNewPdfFile(ActionEvent actionEvent) {
        MessageBox.create("New PDF File", "Do you want to start a new session?", MessageBox.MessageType.Question, List.of(new Pair<>("Yes", e -> {
            final var jobsPanelContent = (VBox) jobsPanel.getContent();
            final var jobs = jobsPanelContent.getChildren().stream().filter(node -> node instanceof Job).map(node -> (Job) node).toList();

            for (Job job : jobs) {
                job.cancel.run();
            }

            Thumbnail.removeAll();
            pageSizeChoiceBox.getSelectionModel().select(defaultPageSize);
            pageMarginChoiceBox.getSelectionModel().select(PageMargin.None);
            pageOrientationChoiceBox.getSelectionModel().select(PageOrientation.Portrait);
            pdfFileNameTextField.clear();
        }), new Pair<>("No", e -> {
        })));
    }

    public void onGuid(ActionEvent actionEvent) {
        pdfFileNameTextField.setText(UUID.randomUUID().toString());
    }

    public void onCreate(ActionEvent actionEvent) {
        final var fileChooser = new FileChooser();

        fileChooser.setTitle("Save the PDF file");
        fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("PDF (*.pdf)", "*.pdf"));

        fileChooser.initialFileNameProperty().bind(pdfFileNameTextField.textProperty());

        final var file = fileChooser.showSaveDialog(root.getScene().getWindow());
        final var task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                try (var document = new PDDocument()) {
                    var files = ((FlowPane) repository.getContent()).getChildren().stream().filter(node -> node instanceof Thumbnail).map(node -> ((Thumbnail) node).getImageInfo().getOriginalFile()).toList();
                    var rectangle = pageSizeChoiceBox.getValue().getRectangle();
                    PDRectangle pageSize;

                    for (var file : files) {
                        var image = PDImageXObject.createFromFile(file.getAbsolutePath(), document);

                        if (Objects.isNull(rectangle)) {
                            pageSize = pageOrientationChoiceBox.getValue() == PageOrientation.Landscape ? new PDRectangle(image.getHeight(), image.getWidth()) : new PDRectangle(image.getWidth(), image.getHeight());
                        } else {
                            pageSize = pageOrientationChoiceBox.getValue() == PageOrientation.Landscape ? new PDRectangle(rectangle.getHeight(), rectangle.getWidth()) : rectangle;
                        }

                        var page = new PDPage(pageSize);

                        document.addPage(page);

                        // Get image dimensions
                        float imageWidth = image.getWidth();
                        float imageHeight = image.getHeight();

                        // Get page dimensions
                        float pageWidth = page.getMediaBox().getWidth();
                        float pageHeight = page.getMediaBox().getHeight();

                        // Calculate scaling factor to fit within the page while keeping aspect ratio
                        float widthScale = pageWidth / imageWidth;
                        float heightScale = pageHeight / imageHeight;
                        float scale = Math.min(widthScale, heightScale); // Maintain aspect ratio

                        // Calculate new image dimensions
                        float newWidth = imageWidth * scale;
                        float newHeight = imageHeight * scale;

                        switch (pageMarginChoiceBox.getSelectionModel().getSelectedItem()) {
                            case Small -> {
                                newWidth -= 50;
                                newHeight -= 50;
                            }
                            case Medium -> {
                                newWidth -= 100;
                                newHeight -= 100;
                            }
                            case Large -> {
                                newWidth -= 150;
                                newHeight -= 150;
                            }
                        }

                        // Calculate position to center the resized image
                        float x = (pageWidth - newWidth) / 2;
                        float y = (pageHeight - newHeight) / 2;

                        try (var contentStream = new PDPageContentStream(document, page)) {
                            contentStream.drawImage(image, x, y, newWidth, newHeight);
                        }
                    }

                    document.save(file);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                return null;
            }

            @Override
            protected void succeeded() {
                super.succeeded();
                Notification.create("Done!", "Your PDF file is ready, follow the link:", Duration.minutes(1), "Open File", e -> {
                    App.application.getHostServices().showDocument(file.getAbsolutePath());
                });
            }
        };

        if (Objects.nonNull(file)) {
            Job.create("Creating PDF file...", "Your PDF file is ready!", "We couldn't create the PDF file!", task);
        }
    }

    public void onImportFiles(ActionEvent actionEvent) {
        final var fileChooser = new FileChooser();
        final var repositoryContent = (FlowPane) repository.getContent();

        fileChooser.setTitle("Import Image Files");
        fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Image Files (png, jpg, jpeg, gif)", "*.png", "*.jpg", "*.jpeg", "*.gif"));

        final var files = fileChooser.showOpenMultipleDialog(root.getScene().getWindow());

        if (Objects.nonNull(files) && !files.isEmpty()) {
            Job.create("Importing image files...", "Your files are imported.", "We're not able to import files!", new Task<Void>() {

                private final List<Thumbnail> thumbnails = new ArrayList<>();

                @Override
                protected Void call() throws Exception {
                    final var numberOfCurrentThumbnails = repositoryContent.getChildren().size();

                    for (var index = 0; index < files.size(); index++) {
                        if (isCancelled()) {
                            return null;
                        }

                        try {
                            thumbnails.add(Thumbnail.create(files.get(index), Utility.resize(files.get(index), 120.0D), numberOfCurrentThumbnails + index + 1));
                            updateProgress(numberOfCurrentThumbnails + index + 1, files.size());
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }

                    return null;
                }

                @Override
                protected void succeeded() {
                    super.succeeded();
                    repositoryContent.getChildren().addAll(thumbnails);
                    updateDetails();
                }
            });
        }
    }

    public void onCurrentJobs(ActionEvent actionEvent) {
        final var toggleButton = (ToggleButton) actionEvent.getSource();

        if (toggleButton.isSelected()) {
            mainSplitPane.getItems().addFirst(jobsPanel);
            mainSplitPane.getDividers().getFirst().setPosition(jobsPanelCurrentDividerPosition);
        } else {
            jobsPanelCurrentDividerPosition = mainSplitPane.getDividers().getFirst().getPosition();
            mainSplitPane.getItems().remove(jobsPanel);
        }
    }

    public void setImageInfo(String fileName, long fileSize, String filePath, int filePage, int fileWidth, int fileHeight) {
        fileNameTextField.setText(fileName);
        fileSizeTextField.setText(String.format("%.2f MB", new UnitOf.DataStorage().fromBytes(fileSize).toMegabytes()));
        filePathTextField.setText(filePath);
        filePageNumberTextField.setText(Integer.toString(filePage));
        fileDimensionTextField.setText(String.format("%d × %d", fileWidth, fileHeight));
        imageInfoPanel.setVisible(true);
    }

    public void closeImageInfo() {
        fileNameTextField.clear();
        fileSizeTextField.clear();
        filePathTextField.clear();
        filePageNumberTextField.clear();
        fileDimensionTextField.clear();
        imageInfoPanel.setVisible(false);
    }

    public void updateDetails() {
        final var repositoryContent = (FlowPane) repository.getContent();
        pageCountLabel.setText(String.format("Pages: %d", repositoryContent.getChildren().size()));
        documentSizeLabel.setText(String.format("Size: %.2f MB", new UnitOf.DataStorage().fromBytes(repositoryContent.getChildren().stream().filter(node -> node instanceof Thumbnail).map(node -> ((Thumbnail) node).getImageInfo().getSize()).mapToLong(Long::longValue).sum()).toMegabytes()));
    }

    private void initPdfFileNameTextFieldContextMenu() {
        final var contextMenu = new ContextMenu();
        final var cutMenuItem = new MenuItem("Cut");
        final var copyMenuItem = new MenuItem("Copy");
        final var pasteMenuItem = new MenuItem("Paste");
        final var uuidMenuItem = new MenuItem("Get a GUID");

        cutMenuItem.setOnAction(event -> pdfFileNameTextField.cut());
        copyMenuItem.setOnAction(event -> pdfFileNameTextField.copy());
        pasteMenuItem.setOnAction(event -> pdfFileNameTextField.paste());
        uuidMenuItem.setOnAction(event -> pdfFileNameTextField.setText(UUID.randomUUID().toString()));

        contextMenu.getItems().addAll(cutMenuItem, copyMenuItem, pasteMenuItem, new SeparatorMenuItem(), uuidMenuItem);

        pdfFileNameTextField.setContextMenu(contextMenu);
    }

    private void initPageOrientationChoiceBox() {
        pageOrientationChoiceBox.getItems().addAll(PageOrientation.values());
        pageOrientationChoiceBox.setValue(PageOrientation.Portrait);
    }

    private void initPageMarginChoiceBox() {
        pageMarginChoiceBox.getItems().addAll(PageMargin.values());
        pageMarginChoiceBox.setValue(PageMargin.None);
    }

    private void initPageSizeChoiceBox() {
        for (var field : PDRectangle.class.getDeclaredFields()) {
            if (field.getType() == PDRectangle.class) {
                try {
                    final var rectangle = (PDRectangle) field.get(null);
                    final var pageSize = new PageSize(field.getName(), rectangle);
                    pageSizeChoiceBox.getItems().add(pageSize);

                    if (pageSize.getName().equalsIgnoreCase("A4")) {
                        defaultPageSize = pageSize;
                    }

                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        final var resizeToImageSizeOption = new PageSize("Resize to image size", null);

        pageSizeChoiceBox.getItems().add(resizeToImageSizeOption);
        pageSizeChoiceBox.setValue(defaultPageSize);

        pageSizeChoiceBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == resizeToImageSizeOption) {
                pageOrientationChoiceBox.getSelectionModel().select(PageOrientation.Portrait);
                pageOrientationChoiceBox.setDisable(true);
            } else {
                pageOrientationChoiceBox.setDisable(false);
            }
        });
    }

    private void setJobsPanelDummyLabel() {
        final var jobsPanelContent = (VBox) jobsPanel.getContent();
        final var dummyLabel = new Label("No job is currently in progress");

        VBox.setVgrow(dummyLabel, Priority.ALWAYS);
        dummyLabel.setMaxWidth(Double.MAX_VALUE);
        dummyLabel.setMaxHeight(Double.MAX_VALUE);
        dummyLabel.setAlignment(Pos.CENTER);
        dummyLabel.getStyleClass().add("dummy-label");

        jobsPanelContent.getChildren().add(dummyLabel);

        jobsPanelContent.getChildren().addListener((ListChangeListener<Node>) c -> {
            while (c.next()) {
                if (c.wasAdded() && c.getAddedSubList().getFirst() != dummyLabel) {
                    Platform.runLater(() -> {
                        jobsPanelContent.getChildren().remove(dummyLabel);
                    });
                } else if (c.wasRemoved() && jobsPanelContent.getChildren().isEmpty()) {
                    Platform.runLater(() -> {
                        jobsPanelContent.getChildren().add(dummyLabel);
                    });
                }
            }
        });
    }

    private void setJobsPanelContextMenu() {
        final var jobsPanelContent = (VBox) jobsPanel.getContent();
        final var contextMenu = new ContextMenu();
        final var cancelAllJobsMenuItem = new MenuItem("Cancel All Jobs");
        final var clearMenuItem = new MenuItem("Clear");

        cancelAllJobsMenuItem.setOnAction((ActionEvent actionEvent) -> {
            jobsPanelContent.getChildren().stream().filter(node -> node instanceof VBox).map(node -> (Job) node).forEach(job -> {
                Platform.runLater(() -> job.cancel.run());
            });
        });

        clearMenuItem.setOnAction((ActionEvent actionEvent) -> {
            jobsPanelContent.getChildren().stream().filter(node -> node instanceof VBox).map(node -> (Job) node).forEach(job -> {
                Platform.runLater(() -> job.clear.run());
            });
        });

        cancelAllJobsMenuItem.setDisable(true);
        contextMenu.getItems().addAll(cancelAllJobsMenuItem, clearMenuItem);

        jobsPanelContent.addEventFilter(ContextMenuEvent.CONTEXT_MENU_REQUESTED, Event::consume);
        jobsPanelContent.addEventFilter(MouseEvent.MOUSE_CLICKED, mouseEvent -> {
            if (mouseEvent.getButton() == MouseButton.SECONDARY) {
                var point2D = jobsPanelContent.localToScreen(mouseEvent.getX(), mouseEvent.getY());
                contextMenu.show(jobsPanelContent, point2D.getX(), point2D.getY());
            } else {
                contextMenu.hide();
            }
        });
    }

    private void setRepositoryDummyLabel() {
        final var parent = (StackPane) repository.getParent();
        final var repositoryContent = (FlowPane) repository.getContent();
        final var dummyLabel = new Label("No image has been imported yet!");

        dummyLabel.getStyleClass().add("dummy-label");

        parent.getChildren().add(dummyLabel);

        repositoryContent.getChildren().addListener((InvalidationListener) observable -> {
            if (repositoryContent.getChildren().isEmpty()) {
                parent.getChildren().add(dummyLabel);
            } else {
                parent.getChildren().remove(dummyLabel);
            }
        });
    }

    public void onAboutMe(ActionEvent actionEvent) {
        Panel.open(Panel.ABOUT_ME);
    }
}