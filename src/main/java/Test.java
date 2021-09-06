import PNCUtilityUI.PNCRecorderGUI;
import javax.swing.*;
import java.sql.SQLException;

public class Test {
    public static void main(String[] args) {

        try {
            Class.forName("org.firebirdsql.jdbc.FBDriver");
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, e.getMessage());
        }

        PNCRecorderGUI pncWindow = new PNCRecorderGUI();
        //System.out.println("java.library.path: " + System.getProperty("java.library.path"));
    }
}
