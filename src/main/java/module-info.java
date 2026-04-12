module com.hospismart.hospismartdesktop {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires java.sql;
    requires itextpdf;
    requires jbcrypt;

    exports com.hospismart.hospismartdesktop.main;
    exports com.hospismart.hospismartdesktop.controllers;
    exports com.hospismart.hospismartdesktop.models;
    exports com.hospismart.hospismartdesktop.tests;

    // Autoriser JavaFX à accéder aux fichiers FXML
    opens com.hospismart.hospismartdesktop.main to javafx.fxml;
    opens com.hospismart.hospismartdesktop.controllers to javafx.fxml;
    opens com.hospismart.hospismartdesktop.models to javafx.fxml;
    opens com.hospismart.hospismartdesktop.tests to javafx.fxml;
}