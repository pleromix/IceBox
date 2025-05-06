module io.github.pleromix.icebox {
    requires java.desktop;
    requires javafx.controls;
    requires javafx.fxml;
    requires jdk.jshell;
    requires org.apache.commons.io;
    requires org.apache.commons.lang3;
    requires opencv;
    requires UnitOf;
    requires org.apache.pdfbox;
    requires static lombok;
    requires java.sql;
    requires java.management;

    opens io.github.pleromix.icebox to javafx.fxml;
    opens io.github.pleromix.icebox.controller to javafx.fxml;

    exports io.github.pleromix.icebox;
    exports io.github.pleromix.icebox.controller;
    exports io.github.pleromix.icebox.dto;
    exports io.github.pleromix.icebox.component;
}