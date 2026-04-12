package com.hospismart.hospismartdesktop;

import com.hospismart.hospismartdesktop.utils.MyDbConnexion;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class TestDB {
    public static void main(String[] args) {
        try {
            Connection cnx = MyDbConnexion.getInstance().getCnx();
            Statement st = cnx.createStatement();
            ResultSet rs = st.executeQuery("DESCRIBE user");
            while (rs.next()) {
                System.out.println(rs.getString("Field") + " : " + rs.getString("Type"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
