package com.hospismart.hospismartdesktop.tests;

import com.hospismart.hospismartdesktop.utils.MyDbConnexion;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;

public class TestSchema {
    public static void main(String[] args) {
        try {
            Connection cnx = MyDbConnexion.getInstance().getCnx();
            Statement st = cnx.createStatement();
            ResultSet rs = st.executeQuery("SELECT * FROM reclamation LIMIT 1");
            ResultSetMetaData meta = rs.getMetaData();
            System.out.println("--- SCHEMA DE LA TABLE RECLAMATION ---");
            for (int i = 1; i <= meta.getColumnCount(); i++) {
                System.out.println(meta.getColumnName(i) + " - " + meta.getColumnTypeName(i));
            }
            System.out.println("--------------------------------------");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

