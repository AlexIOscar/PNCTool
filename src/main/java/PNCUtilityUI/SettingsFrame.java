package PNCUtilityUI;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.PlainDocument;
import java.awt.*;
import java.awt.event.ActionListener;
import java.io.*;

public class SettingsFrame extends JFrame {

    private JPanel rootPanel;
    private JTextField addressField;
    private JTextField aliasField;
    private JButton setDBPathButton;
    private JCheckBox aliasCheckBox;
    private JButton saveButton;
    private JTextField maxRecordsField;
    private JCheckBox ahrCheckBox;
    private JTextField maxInjectionSizeField;
    private JTextField serverNameOrIPField;
    private JLabel dBFileLable;
    private JLabel aliasLable;
    private JLabel maxFetching;
    private JLabel maxWithoutConfLable;
    private JLabel serverNameLbl;
    private JTextField portFld;
    private JLabel PortLbl;
    private JTabbedPane tabbedPane1;
    private JPanel CommonTunes;
    private JPanel AddTunes;
    private JLabel userNameLbl;
    private JTextField userNameFld;
    private JLabel srvNamePMgrLbl;
    private JLabel aliasPMgrLbl;
    private JTextField srvNamePmgrFld;
    private JTextField aliasPMGrFld;
    private JLabel portPMgrLbl;
    private JTextField portPMgrFld;
    private JTextField passFld;
    private JTextField regexFld;
    private JLabel passLbl;
    private JLabel regexLbl;
    private JButton saveButton2;
    private JPanel AdvTunes;

    RecorderSettings rs;

    private final JFileChooser dirChooser = new JFileChooser();

    public SettingsFrame(JFrame root, RecorderSettings rs) throws HeadlessException {
        //основные настройки
        setContentPane(rootPanel);
        setVisible(true);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setSize(700, 450);
        setLocationRelativeTo(root);
        setTitle("Settings");

        //связываем объект настроек, переданный в конструкторе, с встроенным в настоящий класс
        this.rs = rs;

        //настраиваем поля ввода чисел
        PlainDocument doc = (PlainDocument) maxRecordsField.getDocument();
        doc.setDocumentFilter(new DigitFilter());

        PlainDocument doc2 = (PlainDocument) maxInjectionSizeField.getDocument();
        doc2.setDocumentFilter(new DigitFilter());

        //десериализуем объект настроек из файла значения
        String addr = System.getProperty("user.home");
        File f = new File(addr + "\\PNCT\\" + "Settings.data");
        if (f.exists()) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f))) {
                this.rs = (RecorderSettings) ois.readObject();
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, "Не удалось загрузить настройки.\nВведите новые, либо " +
                        "оставьте предлагаемые значения");
            }
        } else {
            JOptionPane.showMessageDialog(null, "Не удалось загрузить настройки.\nВведите новые, либо " +
                    "оставьте предлагаемые значения");
        }

        ImageIcon ii = new ImageIcon(PNCRecorderGUI.class.getResource("FormLogo.png"));
        setIconImage(ii.getImage());

        //извлечение значений в текстовые поля
        maxRecordsField.setText(String.valueOf(rs.maxDrawingRecords));
        maxInjectionSizeField.setText(String.valueOf(rs.maxInjectionWithoutConf));
        addressField.setText(rs.DBFilePath.replace("\\\\", "\\"));
        serverNameOrIPField.setText(rs.DBNameOrIP);
        aliasField.setText(rs.DBAliasName);
        portFld.setText(rs.DBPort);
        aliasCheckBox.setSelected(rs.useAlias);
        ahrCheckBox.setSelected(rs.allowHeaderReposition);

        portPMgrFld.setText(rs.prodMgrPort);
        srvNamePmgrFld.setText(rs.prodMgrNameOrIP);
        aliasPMGrFld.setText(rs.prodMgrAliasName);
        userNameFld.setText(rs.prodMgrLogin);
        passFld.setText(rs.prodMgrPass);
        regexFld.setText(rs.regExpTruncate);


        setDBPathButton.addActionListener(e -> {
            dirChooser.setDialogTitle("Выбор БД VBPARTA");
            // Определение режима - только каталог
            dirChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            dirChooser.setMultiSelectionEnabled(false);
            dirChooser.setFileFilter(new FileNameExtensionFilter("Piece DB file", "GDB"));
            int result = dirChooser.showOpenDialog(SettingsFrame.this);
            if (result == JFileChooser.APPROVE_OPTION) {
                addressField.setText(dirChooser.getSelectedFile().getAbsolutePath());
            }
        });

        ActionListener al = e -> {
            rs.maxDrawingRecords = Integer.parseInt(maxRecordsField.getText());
            rs.DBFilePath = addressField.getText().replace("\\", "\\\\");
            rs.DBAliasName = aliasField.getText();
            rs.useAlias = aliasCheckBox.isSelected();
            rs.allowHeaderReposition = ahrCheckBox.isSelected();
            rs.maxInjectionWithoutConf = Integer.parseInt(maxInjectionSizeField.getText());
            rs.DBNameOrIP = serverNameOrIPField.getText();
            rs.DBPort = portFld.getText();

            rs.prodMgrPass = passFld.getText();
            rs.prodMgrLogin = userNameFld.getText();
            rs.regExpTruncate = regexFld.getText();
            rs.prodMgrAliasName = aliasPMGrFld.getText();
            rs.prodMgrNameOrIP = srvNamePmgrFld.getText();
            rs.prodMgrPort = portPMgrFld.getText();


            if (!f.exists()) {
                if(!f.getParentFile().exists()){
                    if (!f.getParentFile().mkdir()){
                        JOptionPane.showMessageDialog(null, "Не удалось создать каталог сохранения конфигурации");
                        return;
                    }
                }
            }

            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(f))) {
                oos.writeObject(rs);
            } catch (IOException ioException) {
                ioException.printStackTrace();
                JOptionPane.showMessageDialog(null, "Не удалось сохранить настройки");
            }
            this.dispose();
        };

        saveButton.addActionListener(al);
        saveButton2.addActionListener(al);
    }
}
