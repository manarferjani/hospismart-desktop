module com.hospismart.hospismartdesktop {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires java.sql;
    requires java.desktop;

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
    exports com.hospismart.hospismartdesktop.services;
    exports com.hospismart.hospismartdesktop.util;
    exports com.hospismart.hospismartdesktop.utils;
    exports com.hospismart.hospismartdesktop.tests;

    opens com.hospismart.hospismartdesktop.main        to javafx.fxml;
    opens com.hospismart.hospismartdesktop.controllers to javafx.fxml;
    opens com.hospismart.hospismartdesktop.models      to javafx.base, javafx.fxml;
    opens com.hospismart.hospismartdesktop.services    to javafx.fxml;
    opens com.hospismart.hospismartdesktop.util        to javafx.fxml;
    opens com.hospismart.hospismartdesktop.utils       to javafx.fxml;
    opens com.hospismart.hospismartdesktop.tests       to javafx.fxml;
}