package com.hospismart.hospismartdesktop.scratch;

import com.hospismart.hospismartdesktop.utils.MyDbConnexion;
import java.sql.*;

public class DbCheck {
    public static void main(String[] args) {
        try {
            Connection cnx = MyDbConnexion.getInstance().getCnx();
            DatabaseMetaData dbmd = cnx.getMetaData();
            ResultSet rs = dbmd.getColumns(null, null, "consultation", null);
            System.out.println("Columns in 'consultation':");
            while (rs.next()) {
                System.out.println("- " + rs.getString("COLUMN_NAME") + " (" + rs.getString("TYPE_NAME") + ")");
            }
            
            rs = dbmd.getIndexInfo(null, null, "consultation", true, false);
            System.out.println("\nUnique Indexes in 'consultation':");
            while (rs.next()) {
                System.out.println("- Key: " + rs.getString("INDEX_NAME") + " Column: " + rs.getString("COLUMN_NAME"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
