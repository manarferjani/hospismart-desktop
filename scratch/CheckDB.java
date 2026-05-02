
import com.hospismart.hospismartdesktop.utils.MyDbConnexion;
import java.sql.*;

public class CheckDB {
    public static void main(String[] args) {
        try {
            Connection cnx = MyDbConnexion.getInstance().getCnx();
            String req = "SELECT * FROM rendez_vous";
            Statement st = cnx.createStatement();
            ResultSet rs = st.executeQuery(req);
            System.out.println("--- Rendez-vous ---");
            while (rs.next()) {
                System.out.println("ID: " + rs.getInt("id") + " | Date: " + rs.getTimestamp("datetime") + " | Statut: " + rs.getString("statut") + " | Medecin: " + rs.getInt("medecin_id"));
            }
            
            System.out.println("--- Table Consultation Details ---");
            req = "SELECT MAX(id) as max_id FROM consultation";
            rs = st.executeQuery(req);
            if (rs.next()) System.out.println("Max ID: " + rs.getInt("max_id"));

            req = "SELECT * FROM consultation WHERE id = 86";
            rs = st.executeQuery(req);
            if (rs.next()) System.out.println("Row with ID 86 found! patient_id: " + rs.getInt("patient_id") + " | rdv_id: " + rs.getInt("rendez_vous_id"));

            req = "SELECT * FROM consultation WHERE rendez_vous_id = 86";
            rs = st.executeQuery(req);
            if (rs.next()) System.out.println("Row with rdv_id 86 found! id: " + rs.getInt("id") + " | patient_id: " + rs.getInt("patient_id"));

            System.out.println("\n--- Indexes ---");
            DatabaseMetaData meta = cnx.getMetaData();
            ResultSet rsIndexes = meta.getIndexInfo(null, null, "consultation", false, false);
            while (rsIndexes.next()) {
                System.out.println("Index: " + rsIndexes.getString("INDEX_NAME") + " | Column: " + rsIndexes.getString("COLUMN_NAME"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
