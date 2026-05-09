module com.hospismart.hospismartdesktop {
    // JavaFX Modules
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires javafx.swing;
    requires javafx.media;

    // Standard JDK Modules
    requires java.sql;
    requires java.desktop;
    requires java.net.http;
    requires jdk.jsobject;

    // External Libraries (Declared in pom.xml)
    requires jakarta.mail;                      // Eclipse Angus (Automatic-Module-Name)
    requires jakarta.activation;                 // Needed by Jakarta Mail for MIME handling
    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires org.kordamp.ikonli.javafx;
    requires org.apache.pdfbox;
    requires itextpdf;
    requires jbcrypt;
    requires googleauth;
    requires com.google.zxing;
    requires com.google.zxing.javase;
    requires org.apache.poi.ooxml;
    requires org.apache.poi.poi;
    requires com.google.gson;
    requires okhttp3;
    requires opencv;
    requires vosk;

    // Exporting packages for use by other modules/JavaFX
    exports com.hospismart.hospismartdesktop.main;
    exports com.hospismart.hospismartdesktop.controllers;
    exports com.hospismart.hospismartdesktop.models;
    exports com.hospismart.hospismartdesktop.services;
    exports com.hospismart.hospismartdesktop.util;
    exports com.hospismart.hospismartdesktop.utils;
    exports com.hospismart.hospismartdesktop.tests;

    // Opening packages for reflection (required for FXMLLoader and TableView)
    opens com.hospismart.hospismartdesktop.main to javafx.fxml;
    opens com.hospismart.hospismartdesktop.controllers to javafx.fxml;
    opens com.hospismart.hospismartdesktop.models to javafx.base, javafx.fxml;
    opens com.hospismart.hospismartdesktop.services to javafx.fxml, javafx.base;
    opens com.hospismart.hospismartdesktop.util to javafx.fxml;
    opens com.hospismart.hospismartdesktop.utils to javafx.fxml;
    opens com.hospismart.hospismartdesktop.tests to javafx.fxml;
}

