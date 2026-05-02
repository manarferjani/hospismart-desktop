import com.hospismart.hospismartdesktop.utils.MyDbConnexion;
import java.sql.*;

public class FindUserEmail {
    public static void main(String[] args) {
        try {
            Connection cnx = MyDbConnexion.getInstance().getCnx();
            String req = "SELECT email, nom, prenom FROM user WHERE nom LIKE '%FERJANI%'";
            Statement st = cnx.createStatement();
            ResultSet rs = st.executeQuery(req);
            while (rs.next()) {
                System.out.println("Email: " + rs.getString("email") + " | Nom: " + rs.getString("nom") + " | Prenom: " + rs.getString("prenom"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
