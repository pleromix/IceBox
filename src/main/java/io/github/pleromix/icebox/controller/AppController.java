package io.github.pleromix.icebox.controller;

import com.digidemic.unitof.UnitOf;
import io.github.pleromix.icebox.App;
import io.github.pleromix.icebox.component.*;
import io.github.pleromix.icebox.dto.Metadata;
import io.github.pleromix.icebox.dto.PageSize;
import io.github.pleromix.icebox.util.Config;
import io.github.pleromix.icebox.util.Utility;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
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
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import javafx.util.Pair;
import lombok.Getter;
import lombok.Setter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;

public class AppController implements Initializable {


    private static final Logger logger = LoggerFactory.getLogger(AppController.class);

    public static final float ONE_HALF_INCH_MARGIN = 0.5F * 2 * 72;
    public static final float ONE_INCH_MARGIN = 1.0F * 2 * 72;
    public static final float THREE_HALVES_INCH_MARGIN = 1.5F * 2 * 72;

    private final BooleanProperty taskIsRunningProperty = new SimpleBooleanProperty();

    private double jobsPanelCurrentDividerPosition;
    private PageSize defaultPageSize;

    @Getter
    @Setter
    private Metadata metadata;

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
    public ToggleButton jobsToggleButton;
    @FXML
    public Label pageCountLabel;
    @FXML
    public Label sizeLabel;
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
    public Button createPdfFileButton;
    @FXML
    public Button newPdfFileButton;
    @FXML
    public Button importFilesButton;
    @FXML
    public Button imageFileRemoveButton;
    @FXML
    public ChoiceBox<PageSize> pageSizeChoiceBox;
    @FXML
    public ChoiceBox<PageMargin> pageMarginChoiceBox;
    @FXML
    public ChoiceBox<PageOrientation> pageOrientationChoiceBox;
    @FXML
    public Button menuButton;
    @FXML
    public Slider viewSizeSlider;

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

        repositoryContent.getChildren().addListener((InvalidationListener) observable -> repositorySize.set(repositoryContent.getChildren().size()));
        createPdfFileButton.disableProperty().bind(taskIsRunningProperty.or(repositorySize.isEqualTo(0)));

        jobsPanelCurrentDividerPosition = mainSplitPane.getDividers().getFirst().getPosition();

        setMenu();

        viewSizeSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            Thumbnail.viewSizeProperty.set(Thumbnail.ViewSize.values()[newValue.intValue()]);
        });

        if (Config.getInstance().getShowWelcomePanel()) {
            Platform.runLater(() -> {
                new Timeline(new KeyFrame(Duration.millis(250), event -> {
                    Panel.open(Panel.WELCOME);
                })).play();
            });
        }
    }

    public void onNewPdfFile(ActionEvent actionEvent) {
        MessageBox.create("New PDF File", "Would you like to start over?", MessageBox.MessageType.Question, List.of(new Pair<>("Yes", e -> {
            final var jobsPanelContent = (VBox) jobsPanel.getContent();
            final var jobs = jobsPanelContent.getChildren().stream().filter(node -> node instanceof Job).map(node -> (Job) node).toList();

            for (Job job : jobs) {
                job.cancel.run();
            }

            Thumbnail.removeAll();
            pageSizeChoiceBox.getSelectionModel().select(defaultPageSize);
            pageMarginChoiceBox.getSelectionModel().select(PageMargin.None);
            pageOrientationChoiceBox.getSelectionModel().select(PageOrientation.Portrait);
            viewSizeSlider.setValue(1.0D);
            metadata = null;

            Notification.closeShowingNotification();
        }), new Pair<>("No", e -> {
        })));
    }

    public void onCreate(ActionEvent actionEvent) {
        Panel.open(Panel.CREATION);
    }

    public void createPdf(String fileName, org.apache.commons.lang3.tuple.Pair<Double, Integer> combination, boolean rotate90DegCcw, boolean rotate90DegCw, boolean flipHorizontal, boolean flipVertical, boolean isGrayscale) {
        final var fileChooser = new FileChooser();

        fileChooser.setTitle("Save the PDF file");
        fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("PDF (*.pdf)", "*.pdf"));
        fileChooser.setInitialFileName(fileName.concat(".pdf"));

        var file = fileChooser.showSaveDialog(Panel.getCurrentStage());

        final var task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                try (var document = new PDDocument()) {
                    var information = document.getDocumentInformation();
                    var files = getFiles();
                    var rectangle = pageSizeChoiceBox.getValue().getRectangle();
                    PDRectangle pageSize;

                    for (var file : files) {
                        var transformedImage = Utility.transformation(file, combination.getLeft(), combination.getRight(), rotate90DegCcw, rotate90DegCw, flipHorizontal, flipVertical, isGrayscale);
                        var image = PDImageXObject.createFromByteArray(document, transformedImage, null);

                        if (Objects.isNull(rectangle)) {
                            pageSize = pageOrientationChoiceBox.getValue() == PageOrientation.Landscape ? new PDRectangle(image.getHeight(), image.getWidth()) : new PDRectangle(image.getWidth(), image.getHeight());
                        } else {
                            pageSize = pageOrientationChoiceBox.getValue() == PageOrientation.Landscape ? new PDRectangle(rectangle.getHeight(), rectangle.getWidth()) : rectangle;
                        }

                        var page = new PDPage(pageSize);

                        document.addPage(page);

                        var imageWidth = (float) image.getWidth();
                        var imageHeight = (float) image.getHeight();

                        var pageWidth = page.getMediaBox().getWidth();
                        var pageHeight = page.getMediaBox().getHeight();

                        var widthScale = pageWidth / imageWidth;
                        var heightScale = pageHeight / imageHeight;
                        var scale = Math.min(widthScale, heightScale);

                        var newWidth = imageWidth * scale;
                        var newHeight = imageHeight * scale;

                        var alpha = Math.min(newWidth, newHeight);
                        var beta = Math.max(newWidth, newHeight);

                        var imageRatio = beta / alpha;

                        switch (pageMarginChoiceBox.getSelectionModel().getSelectedItem()) {
                            case Small -> {
                                // 0.5 inch:

                                if (newWidth >= newHeight) {
                                    newWidth -= ONE_HALF_INCH_MARGIN;
                                    newHeight -= ONE_HALF_INCH_MARGIN / imageRatio;
                                } else {
                                    newWidth -= ONE_HALF_INCH_MARGIN / imageRatio;
                                    newHeight -= ONE_HALF_INCH_MARGIN;
                                }
                            }
                            case Medium -> {
                                // 1 inch:

                                if (newWidth >= newHeight) {
                                    newWidth -= ONE_INCH_MARGIN;
                                    newHeight -= ONE_INCH_MARGIN / imageRatio;
                                } else {
                                    newWidth -= ONE_INCH_MARGIN / imageRatio;
                                    newHeight -= ONE_INCH_MARGIN;
                                }
                            }
                            case Large -> {
                                // 1.5 inch:

                                if (newWidth >= newHeight) {
                                    newWidth -= THREE_HALVES_INCH_MARGIN;
                                    newHeight -= THREE_HALVES_INCH_MARGIN / imageRatio;
                                } else {
                                    newWidth -= THREE_HALVES_INCH_MARGIN / imageRatio;
                                    newHeight -= THREE_HALVES_INCH_MARGIN;
                                }
                            }
                        }

                        var x = (pageWidth - newWidth) / 2;
                        var y = (pageHeight - newHeight) / 2;

                        try (var contentStream = new PDPageContentStream(document, page)) {
                            contentStream.drawImage(image, x, y, newWidth, newHeight);
                        }
                    }

                    if (Objects.nonNull(metadata)) {
                        information.setTitle(metadata.getTitle());
                        information.setAuthor(metadata.getAuthor());
                        information.setSubject(metadata.getSubject());
                        information.setProducer(metadata.getProducer());
                        information.setCreator(metadata.getCreator());
                        information.setKeywords(metadata.getKeywords());
                    }

                    document.save(file);
                } catch (IOException e) {
                    logger.error("Could not create document: {}", e.getMessage(), e);
                }

                return null;
            }

            @Override
            protected void running() {
                super.running();
                taskIsRunningProperty.set(true);
                disableTopControls(true);
                Panel.close();
            }

            @Override
            protected void succeeded() {
                super.succeeded();
                Notification.create("Done!", "PDF file is ready, follow the link:", Duration.minutes(1), "Open file", e -> {
                    App.getApplication().getHostServices().showDocument(file.getAbsolutePath());
                });
                disableTopControls(false);
            }

            @Override
            protected void done() {
                super.done();
                taskIsRunningProperty.set(false);
            }

            @Override
            protected void failed() {
                super.failed();
                getException().printStackTrace();
            }

            @Override
            protected void cancelled() {
                super.cancelled();
                disableTopControls(false);
            }
        };

        if (Objects.nonNull(file)) {
            Job.create(
                    "Creating PDF file",
                    "PDF file is ready",
                    "We couldn't create PDF file",
                    file.getAbsolutePath(),
                    task
            );
        }
    }

    private List<File> getFiles() {
        final var repositoryContent = (FlowPane) repository.getContent();
        return repositoryContent
                .getChildren()
                .stream()
                .filter(node -> node instanceof Thumbnail)
                .map(node -> ((Thumbnail) node))
                .map(thumbnail -> thumbnail.getImageInfo().getOriginalFile())
                .toList();
    }

    public void onImportFiles(ActionEvent actionEvent) {
        final var fileChooser = new FileChooser();
        final var repositoryContent = (FlowPane) repository.getContent();
        final var ownerWindows = Objects.isNull(Panel.getCurrentStage()) ? App.getPrimaryStage() : Panel.getCurrentStage();

        fileChooser.setTitle("Import Image Files");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("JPEG (*.jpeg)", "*.jpg", "*.jpeg"),
                new FileChooser.ExtensionFilter("JPEG 2000 (*.jp2)", "*.jp2"),
                new FileChooser.ExtensionFilter("Portable Network Graphics (*.png)", "*.png"),
                new FileChooser.ExtensionFilter("Bitmap (*.bmp)", "*.bmp"),
                new FileChooser.ExtensionFilter("WebP (*.webp)", "*.webp")
        );

        final var files = fileChooser.showOpenMultipleDialog(ownerWindows);

        if (Objects.nonNull(files) && !files.isEmpty()) {
            Job.create("Importing image files", "Image files are imported", "We couldn't import image files", new Task<Void>() {

                private final List<Thumbnail> thumbnails = new ArrayList<>();

                @Override
                protected Void call() throws Exception {
                    final var numberOfCurrentThumbnails = repositoryContent.getChildren().size();

                    for (var index = 0; index < files.size(); index++) {
                        if (isCancelled()) {
                            return null;
                        }

                        try {
                            thumbnails.add(Thumbnail.create(files.get(index), Utility.resize(files.get(index), Thumbnail.IMAGE_SIZE * 3), numberOfCurrentThumbnails + index + 1));
                            updateProgress(numberOfCurrentThumbnails + index + 1, files.size());
                        } catch (IOException e) {
                            logger.error("Could not create thumbnail: {}", e.getMessage(), e);
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

                @Override
                protected void failed() {
                    super.failed();
                    getException().printStackTrace();
                }
            });

            Panel.close();
        }
    }

    public void onJobs(ActionEvent actionEvent) {
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
        if (Config.getInstance().getShowImageInfo()) {
            fileNameTextField.setText(fileName);
            fileSizeTextField.setText(String.format("%.2f MB", new UnitOf.DataStorage().fromBytes(fileSize).toMegabytes()));
            filePathTextField.setText(filePath);
            filePageNumberTextField.setText(Integer.toString(filePage));
            fileDimensionTextField.setText(String.format("%d × %d pixels", fileWidth, fileHeight));
            imageInfoPanel.setVisible(true);
        }
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
        final var size = new UnitOf
                .DataStorage()
                .fromBytes(repositoryContent
                        .getChildren()
                        .stream()
                        .filter(node -> node instanceof Thumbnail)
                        .map(node -> ((Thumbnail) node).getImageInfo().getSize())
                        .mapToLong(Long::longValue).sum())
                .toMegabytes();
        pageCountLabel.setText(String.format("Pages: %d", repositoryContent.getChildren().size()));
        sizeLabel.setText(String.format("Size: %s%.2f MB", size > 0.0D ? "~" : "", size));
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
                    logger.error("Could not access class field: {}", e.getMessage(), e);
                }
            }
        }

        final var resizeToImageSizeOption = new PageSize("Resize to image size", null);

        pageSizeChoiceBox.getItems().add(resizeToImageSizeOption);
        pageSizeChoiceBox.setValue(defaultPageSize);

        pageOrientationChoiceBox.disableProperty().bind(Bindings.createBooleanBinding(() -> {
            if (pageSizeChoiceBox.getSelectionModel().getSelectedItem() == resizeToImageSizeOption) {
                pageOrientationChoiceBox.setValue(PageOrientation.Portrait);
                return true;
            }

            return false;
        }, pageSizeChoiceBox.valueProperty()));
    }

    private void setJobsPanelDummyLabel() {
        final var jobsPanelContent = (VBox) jobsPanel.getContent();
        final var dummyLabel = new Label("No job is in progress");

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

    private void setRepositoryDummyLabel() {
        final var parent = (StackPane) repository.getParent();
        final var repositoryContent = (FlowPane) repository.getContent();
        final var dummyLabel = new Label("No images have been added");

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

    public void disableTopControls(boolean disable) {
        newPdfFileButton.setDisable(disable);
        importFilesButton.setDisable(disable);
    }

    public void onMetadata(ActionEvent actionEvent) {
        Panel.open(Panel.METADATA);
    }

    public void setMenu() {
        final var contextMenu = new ContextMenu();
        final var settingsMenuItem = new MenuItem("Settings");
        final var aboutMenuItem = new MenuItem("About");
        final var exitMenuItem = new MenuItem("Exit");

        settingsMenuItem.setOnAction(event -> Panel.open(Panel.SETTINGS));
        aboutMenuItem.setOnAction(event -> Panel.open(Panel.ABOUT));
        exitMenuItem.setOnAction(event -> {
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
            } else {
                Platform.exit();
            }
        });

        contextMenu.getItems().addAll(
                settingsMenuItem,
                aboutMenuItem,
                exitMenuItem
        );
        contextMenu.getStyleClass().add("main-menu");

        menuButton.setOnContextMenuRequested(Event::consume);
        menuButton.setOnMouseClicked(event -> {
            var point2D = menuButton.localToScreen(event.getX(), event.getY());
            contextMenu.show(menuButton, point2D.getX(), point2D.getY());
        });
    }
}