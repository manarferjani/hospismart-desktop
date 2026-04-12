module com.hospismart.hospismartdesktop {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires java.sql;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires eu.hansolo.tilesfx;
    requires com.almasb.fxgl.all;

    exports com.hospismart.hospismartdesktop.main;
    exports com.hospismart.hospismartdesktop.controllers;
    exports com.hospismart.hospismartdesktop.models;
    exports com.hospismart.hospismartdesktop.tests;

    // Autoriser JavaFX à accéder aux fichiers FXML
    opens com.hospismart.hospismartdesktop.main to javafx.fxml;
    opens com.hospismart.hospismartdesktop.controllers to javafx.fxml;
    opens com.hospismart.hospismartdesktop.models to javafx.fxml, javafx.base;
    opens com.hospismart.hospismartdesktop.tests to javafx.fxml;
}
