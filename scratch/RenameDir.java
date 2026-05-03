import java.io.File;

public class RenameDir {
    public static void main(String[] args) {
        File oldDir = new File("src/main/java/com/hospismart/hospismartdesktop/Services");
        File tempDir = new File("src/main/java/com/hospismart/hospismartdesktop/Services_tmp");
        File newDir = new File("src/main/java/com/hospismart/hospismartdesktop/services");

        if (oldDir.exists()) {
            boolean success = oldDir.renameTo(tempDir);
            if (success) {
                tempDir.renameTo(newDir);
                System.out.println("Renamed Services to services");
            } else {
                System.out.println("Failed to rename Services to Services_tmp");
            }
        } else {
            System.out.println("Services directory not found");
        }
    }
}
