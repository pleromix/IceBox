package io.github.pleromix.icebox.util;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import org.apache.commons.io.FileUtils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Properties;

public final class Config {

    private static final String APP_ID = "io.github.pleromix.icebox";
    private static final Path CONFIG_PATH = Paths.get(FileUtils.getTempDirectory().getPath(), APP_ID + ".properties");

    private static Config instance;

    private final Properties defaultProperties = new Properties();
    private final Properties appProperties = new Properties(defaultProperties);

    public final BooleanProperty showWelcomePanelProperty = new SimpleBooleanProperty();
    public final BooleanProperty showImageInfoProperty = new SimpleBooleanProperty();
    public final BooleanProperty askBeforeExitingApplicationProperty = new SimpleBooleanProperty();

    private Config() {
        initialize();
    }

    public static Config getInstance() {
        if (Objects.isNull(instance)) {
            synchronized (Config.class) {
                if (Objects.isNull(instance)) {
                    instance = new Config();
                }
            }
        }

        return instance;
    }

    private void initialize() {
        try (final var inputStream = Config.class.getResourceAsStream("/io/github/pleromix/icebox/asset/default.properties")) {

            if (!Files.exists(CONFIG_PATH)) {
                CONFIG_PATH.toFile().createNewFile();
            }

            defaultProperties.load(inputStream);
            appProperties.load(Files.newBufferedReader(CONFIG_PATH));

            showWelcomePanelProperty.addListener((observable, oldValue, newValue) -> setShowWelcomePanel(newValue));
            showImageInfoProperty.addListener((observable, oldValue, newValue) -> setShowImageInfo(newValue));
            askBeforeExitingApplicationProperty.addListener((observable, oldValue, newValue) -> setAskBeforeExitingApplication(newValue));

            showWelcomePanelProperty.set(getShowWelcomePanel());
            showImageInfoProperty.set(getShowImageInfo());
            askBeforeExitingApplicationProperty.set(getAskBeforeExitingApplication());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void save() {
        try (final var outputStream = new FileOutputStream(CONFIG_PATH.toFile())) {
            appProperties.store(outputStream, "User configurations");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Boolean getShowWelcomePanel() {
        return Boolean.parseBoolean(appProperties.getProperty("show-welcome-panel"));
    }

    public void setShowWelcomePanel(Boolean showWelcomePanel) {
        appProperties.setProperty("show-welcome-panel", showWelcomePanel.toString());
        save();
    }

    public Boolean getShowImageInfo() {
        return Boolean.valueOf(appProperties.getProperty("show-image-info"));
    }

    public void setShowImageInfo(Boolean showImageInfo) {
        appProperties.setProperty("show-image-info", showImageInfo.toString());
        save();
    }

    public Boolean getAskBeforeExitingApplication() {
        return Boolean.valueOf(appProperties.getProperty("ask-before-exiting-application"));
    }

    public void setAskBeforeExitingApplication(Boolean askBeforeExitingApplication) {
        appProperties.setProperty("ask-before-exiting-application", askBeforeExitingApplication.toString());
        save();
    }
}
