module com.github.icebox {
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

    opens com.github.icebox to javafx.fxml;
    opens com.github.icebox.controller to javafx.fxml;

    exports com.github.icebox;
    exports com.github.icebox.controller;
    exports com.github.icebox.dto;
}