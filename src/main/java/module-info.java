module com.hospismart.hospismartdesktop {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires java.sql;
    requires java.desktop;
    requires java.net.http;
    requires java.desktop;
    requires jakarta.mail;
    requires java.net.http;
    requires java.net.http;
    requires jdk.jsobject;
    requires com.google.gson;
    requires itextpdf;
    requires java.desktop;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires eu.hansolo.tilesfx;
    requires com.almasb.fxgl.all;
    requires java.desktop;
    requires java.net.http;
    requires openhtmltopdf.core;
    requires openhtmltopdf.pdfbox;
    requires jakarta.mail;
    // PDF generation (Apache PDFBox)
    requires org.apache.pdfbox;
    // Excel generation
    requires org.apache.poi.ooxml;
    requires org.apache.poi.poi;

    // Exportation des packages qui contiennent des classes
    exports com.hospismart.hospismartdesktop.main;
    exports com.hospismart.hospismartdesktop.controllers;
    exports com.hospismart.hospismartdesktop.models;
    exports com.hospismart.hospismartdesktop.services;
    exports com.hospismart.hospismartdesktop.util;
    exports com.hospismart.hospismartdesktop.utils;
    exports com.hospismart.hospismartdesktop.services;
    exports com.hospismart.hospismartdesktop.utils;
    exports com.hospismart.hospismartdesktop.tests;

    opens com.hospismart.hospismartdesktop.main        to javafx.fxml;
    // Ouverture des packages pour la réflexion (nécessaire pour FXMLLoader)
    opens com.hospismart.hospismartdesktop.main to javafx.fxml;
    // Autoriser JavaFX à accéder aux fichiers FXML
    opens com.hospismart.hospismartdesktop.main        to javafx.fxml;
    opens com.hospismart.hospismartdesktop.controllers to javafx.fxml;
    opens com.hospismart.hospismartdesktop.models      to javafx.base, javafx.fxml;
    opens com.hospismart.hospismartdesktop.services    to javafx.fxml;
    opens com.hospismart.hospismartdesktop.util        to javafx.fxml;
    opens com.hospismart.hospismartdesktop.utils       to javafx.fxml;
    opens com.hospismart.hospismartdesktop.tests       to javafx.fxml;
    opens com.hospismart.hospismartdesktop.models to javafx.fxml, javafx.base;
    opens com.hospismart.hospismartdesktop.tests to javafx.fxml;


    opens com.hospismart.hospismartdesktop.models      to javafx.fxml;
    opens com.hospismart.hospismartdesktop.services    to javafx.fxml;
    opens com.hospismart.hospismartdesktop.utils       to javafx.fxml;
    opens com.hospismart.hospismartdesktop.tests       to javafx.fxml;
}
