package PNCUtilityUI;

import PNCUtility.*;

import javax.swing.*;
import javax.swing.table.*;
import javax.swing.text.PlainDocument;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.io.*;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class PNCRecorderGUI extends JFrame {

    public PNCRecorder pncr = new PNCRecorder();
    public PNCSelector pncs = new PNCSelector();

    private RecorderSettings settings;

    private JPanel rootPanel;
    private JButton selectButton;

    private JTextField productNameField;
    private JTextField markNameField;
    private JTextField orderNameField;

    private JTable table1;
    private CustomTableModel tableModel;
    private JTableHeader jht;

    private JButton injectButton;
    private JButton clearButton;
    private JTextField preMarkField;
    private JTextField postMarkField;
    private JTextField injOrderField;
    private JCheckBox smCheckBox;
    private JLabel totalFetched;
    private JLabel prodLable;
    private JLabel markLable;
    private JLabel ordLable;
    private JLabel asOrdCopyLable;
    private JLabel preMarkLable;
    private JLabel postMarkLable;
    private JTextField preTruncField;
    private JTextField postTruncField;
    private JCheckBox likeFlCheckBox;
    private JButton bindButton;
    private JTextField partyField;
    private JLabel partyLbl;
    private JLabel totalCollatedLbl;
    private JLabel asProdLbl;
    private JTextField injProdFld;
    private JCheckBox useInjProdChkBx;

    //слушатель событий мыши на заголове таблицы
    TableMouseListener tml;

    //конструируем
    public PNCRecorderGUI() throws HeadlessException {
        //базовые настройки фрейма
        setContentPane(rootPanel);
        setVisible(true);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(1000, 700);
        setLocationRelativeTo(null);
        setTitle("PNC recorder utility");

        //создаем слушатель главного окна
        this.addWindowListener(new MainFrameListener());

        //иконка окна
        ImageIcon ii = new ImageIcon(PNCRecorderGUI.class.getResource("FormLogo.png"));
        setIconImage(ii.getImage());

        //локализация диалоговых окон
        UIManager.put("OptionPane.yesButtonText", "Да");
        UIManager.put("OptionPane.noButtonText", "Нет");
        UIManager.put("OptionPane.cancelButtonText", "Отмена");

        //создаем меню
        JMenuBar mb = new JMenuBar();
        setJMenuBar(mb);

        JMenu toolsMenu = new JMenu("Tools");
        mb.add(toolsMenu);

        JMenu helpMenu = new JMenu("Help");
        mb.add(helpMenu);

        JMenu aboutMenu = new JMenu("About");
        mb.add(aboutMenu);

        JMenuItem setDBaddrMI = new JMenuItem("Settings");
        toolsMenu.add(setDBaddrMI);
        toolsMenu.addSeparator();

        JMenuItem quitMI = new JMenuItem("Exit");
        toolsMenu.add(quitMI);

        JMenuItem howToUseMI = new JMenuItem("Help me");
        helpMenu.add(howToUseMI);

        JMenuItem aboutMI = new JMenuItem("About me");
        aboutMenu.add(aboutMI);

        //заполняем элементы интерфейса значениями по-умолчанию
        productNameField.setText("*");
        orderNameField.setText("*");
        markNameField.setText("*");

        //настраиваем поля ввода
        PlainDocument preT = (PlainDocument) preTruncField.getDocument();
        preT.setDocumentFilter(new DigitFilter());
        PlainDocument postT = (PlainDocument) postTruncField.getDocument();
        postT.setDocumentFilter(new DigitFilter());
        PlainDocument numParty = (PlainDocument) partyField.getDocument();
        numParty.setDocumentFilter(new DigitFilter());

        //слушатели мыши на полях
        productNameField.addMouseListener(new InputFieldsMouseListener());
        orderNameField.addMouseListener(new InputFieldsMouseListener());
        markNameField.addMouseListener(new InputFieldsMouseListener());

        //слушатели работы меню
        quitMI.addActionListener(e -> this.dispose());

        setDBaddrMI.addActionListener(e -> {
            SettingsFrame sf = new SettingsFrame(this, settings);
        });

        howToUseMI.addActionListener(e -> {
            JOptionPane.showMessageDialog(null, "Помощи нет :(", "Help", JOptionPane.PLAIN_MESSAGE);
        });

        aboutMI.addActionListener(e -> {
            JOptionPane.showMessageDialog(null, "Developed by IT Department\nof " +
                    "Omsk Electric-Mechanical Plant, aug.2021", "About the program", JOptionPane.PLAIN_MESSAGE);
        });

        //кнопка отбора значений
        selectButton.addActionListener(e -> {
            //формируем строку для коннекта
            String selectConnStr = getConnectionString();

            //извлекаем атрибуты фильтрации
            String productName = productNameField.getText().toUpperCase();
            String orderName = orderNameField.getText().toUpperCase();
            String markName = markNameField.getText().toUpperCase();

            tableModel.setRowCount(0);
            tableModel.startCounts.clear();

            //сбрасываем флаг в tml
            tml.firstPress[0] = true;

            try {
                pncs.con = DriverManager.getConnection(selectConnStr,
                        "SYSDBA", "masterkey");

                ResultSet rs = pncs.selectRecords(productName, orderName, markName, !likeFlCheckBox.isSelected());
                Object[] row = new Object[8];
                int counter = settings.maxDrawingRecords;
                int fetchedCounter = 0;
                while (rs.next()) {
                    if (counter == 0) break;
                    try {
                        row[0] = rs.getInt(1);
                        row[1] = Boolean.FALSE;
                        row[2] = new String(rs.getBytes(2), "cp1251");
                        row[3] = new String(rs.getBytes(3), "cp1251");
                        row[4] = new String(rs.getBytes(4), "cp1251");
                        row[6] = new String(rs.getBytes(6), "cp1251");
                        row[7] = rs.getInt(7);

                        //маркировка может быть null
                        if (!(rs.getBytes(5) == null)) {
                            row[5] = new String(rs.getBytes(5), "cp1251");
                        } else {
                            row[5] = "";
                        }
                        fetchedCounter++;
                        tableModel.startCounts.add((Integer) row[7]);

                    } catch (UnsupportedEncodingException unsupportedEncodingException) {
                        unsupportedEncodingException.printStackTrace();
                    }
                    tableModel.addRow(row);
                    counter--;
                }
                totalFetched.setText("Извлечено записей: " + fetchedCounter);
                totalCollatedLbl.setText("");

            } catch (SQLException throwables) {
                throwables.printStackTrace();
                JOptionPane.showMessageDialog(null, "DB Connection problems:" + "\n" +
                        throwables.getMessage());
            } finally {
                try {
                    if (pncs.con != null) {
                        pncs.con.close();
                    }
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                    JOptionPane.showMessageDialog(null, "DB Connection problems");
                }
            }
        });

        //кнопка очистить
        clearButton.addActionListener(e -> {
            tableModel.setRowCount(0);
            totalFetched.setText("");
            totalCollatedLbl.setText("");
            tableModel.startCounts.clear();
        });

        //кнопка выполнения записи
        injectButton.addActionListener(e -> {
            //получаем значения для формирования инъектируемых записей
            String preMark = preMarkField.getText().toUpperCase();
            String postMark = postMarkField.getText().toUpperCase();
            String injOrder = injOrderField.getText().toUpperCase();
            //учесть наличие чекбокса
            String injProd = null;
            if (useInjProdChkBx.isSelected()) {
                injProd = injProdFld.getText().toUpperCase();
            }
            boolean safeMark = smCheckBox.isSelected();

            //формируем список иденитификаторов записей, подлежащих копированию
            List<Integer> listID = new ArrayList<>();
            List<Integer> amounts = new ArrayList<>();
            int rows = tableModel.getRowCount();
            for (int i = 0; i < rows; i++) {
                if ((boolean) tableModel.getValueAt(i, 1)) {
                    listID.add((int) tableModel.getValueAt(i, 0));
                    amounts.add((int) tableModel.getValueAt(i, 7));
                }
            }

            String message = "Количество единовременно копируемых деталей (" + listID.size() + ") превыешает\n " +
                    "максимальное, не требующее подтверждения. Выполнить запись?";

            if (listID.size() > settings.maxInjectionWithoutConf) {
                int result = JOptionPane.showConfirmDialog(null, message, "Превышение объема разовой записи",
                        JOptionPane.YES_NO_OPTION);
                if (result == JOptionPane.NO_OPTION || result == JOptionPane.CLOSED_OPTION) return;
            }

            //размеры усечения маркировки
            int preTrunc = 0;
            int postTrunc = 0;
            if (!preTruncField.getText().equals("")) preTrunc = Integer.parseInt(preTruncField.getText());
            if (!postTruncField.getText().equals("")) postTrunc = Integer.parseInt(postTruncField.getText());

            //формируем строку для коннекта
            String selectConnStr = getConnectionString();

            try {
                pncr.con = DriverManager.getConnection(selectConnStr,
                        "SYSDBA", "masterkey");
                pncr.con.setAutoCommit(false);

                int index = 0;
                for (int pieceID : listID) {
                    pncr.createPiece(pieceID, injOrder, injProd, preMark, postMark, amounts.get(index), safeMark,
                            preTrunc, postTrunc);
                    index++;
                }

                pncr.con.commit();

            } catch (SQLException throwables) {
                try {
                    pncr.con.rollback();
                } catch (SQLException sqlException) {
                    sqlException.printStackTrace();
                    JOptionPane.showMessageDialog(null, "DB Connection problems");
                }
                throwables.printStackTrace();

                String errStr = "DB Connection problems. ";

                JOptionPane.showMessageDialog(null, errStr + throwables.getMessage());
            } finally {
                try {
                    if (pncr.con != null) {
                        pncr.con.close();
                    }
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                    JOptionPane.showMessageDialog(null, "DB Connection problems");
                }
            }
        });

        //кнопка связывания с победой
        bindButton.addActionListener(e -> {
            ProdMgrConnector pnc = new ProdMgrConnector(settings.prodMgrPort, settings.prodMgrNameOrIP,
                    settings.prodMgrAliasName, settings.prodMgrLogin, settings.prodMgrPass);


            //формируем списки имен марок и позиций для записей, на которых установлен флаг копирования
            List<String> marksNames = new ArrayList<>();
            List<String> posNames = new ArrayList<>();
            List<Integer> checked = new ArrayList<>();
            int rows = tableModel.getRowCount();
            for (int i = 0; i < rows; i++) {
                if ((boolean) tableModel.getValueAt(i, 1)) {
                    marksNames.add((String) tableModel.getValueAt(i, 3));
                    posNames.add(((String) tableModel.getValueAt(i, 4)).replaceAll("ПОЗ. ", ""));
                    checked.add(i);
                }
            }

            if (checked.size() == 0) return;
            if (partyField.getText().equals("")) return;

            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            int[] amounts = pnc.getAmountsArr(injOrderField.getText(), Integer.parseInt(partyField.getText()),
                    marksNames,
                    posNames);
            int index = 0;
            int totalCollated = amounts.length;
            for (int amount : amounts) {
                //если вернулся ноль, то значит соответствие не обнаружено (в системе, откуда делаем запрос, не м.б.
                // ноль деталей), такую запись пропускаем
                if (amount == 0) {
                    index++;
                    totalCollated--;
                    continue;
                }
                tableModel.setValueAt(amount, checked.get(index), 7);
                index++;
            }

            totalCollatedLbl.setText("Сопоставлено: " + totalCollated);
            setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        });

        useInjProdChkBx.addActionListener(e -> setInjProdCompsProps());
    }

    private void setInjProdCompsProps() {
        if (useInjProdChkBx.isSelected()) {
            injProdFld.setEnabled(true);
            asProdLbl.setEnabled(true);
        } else {
            injProdFld.setEnabled(false);
            asProdLbl.setEnabled(false);
        }
    }

    //TODO метка для удобства
    private void createUIComponents() {

        Object[] colsHeader = new Object[8];
        colsHeader[0] = "PieceID";
        colsHeader[1] = "setInjFlag";
        colsHeader[2] = "Изделие";
        colsHeader[3] = "Марка";
        colsHeader[4] = "Позиция";
        colsHeader[5] = "Маркировка";
        colsHeader[6] = "Заказ";
        colsHeader[7] = "Кол-во";

        //назначаем типы данных
        tableModel = new CustomTableModel() {
            @Override
            public Class<?> getColumnClass(int column) {
                switch (column) {
                    case 1:
                        return Boolean.class;
                    case 7:
                        return Integer.class;
                    default:
                        return String.class;
                }
            }

            @Override
            public boolean isCellEditable(int i, int i1) {
                switch (i1) {
                    case 1:
                    case 7:
                        return true;
                    default:
                        return false;
                }
            }
        };

        //назначаем модели данных идентификаторы столбцов
        tableModel.setColumnIdentifiers(colsHeader);
        //загружаем "настройки" десериализацией файла настроек
        deserializeSettings();

        //заздаем таблицу на основе модели данных
        table1 = new JTable(tableModel);
        table1.addMouseListener(new TableMouseListener());

        //создаем объект заголовка таблицы
        jht = table1.getTableHeader();
        jht.setReorderingAllowed(settings.allowHeaderReposition);

        //добавляем установку/сброс всех чекбоксов
        //слушатель событий мыши на заголовке таблицы
        tml = new TableMouseListener();
        jht.addMouseListener(tml);
        TableColumnModel tcm = table1.getColumnModel();

        //готовим значок для столбца-выделителя
        Icon selIcon = new ImageIcon(PNCRecorderGUI.class.getResource("check (1).png"));
        JLabel selLable = new JLabel(selIcon);
        TableCellRenderer renderer = new JComponentTableCellRenderer();

        //назначаем размеры для столбца чекбоксов выбора
        tcm.getColumn(1).setMinWidth(25);
        tcm.getColumn(1).setMaxWidth(30);
        tcm.getColumn(1).setPreferredWidth(30);

        //назначаем размеры для столбца количества
        tcm.getColumn(7).setMinWidth(25);
        tcm.getColumn(7).setMaxWidth(100);
        tcm.getColumn(7).setPreferredWidth(50);

        //
        DefaultTableCellRenderer dtcr = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (!tableModel.startCounts.get(row).equals(tableModel.getValueAt(row, column))) {
                    super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column).setBackground(new Color(188, 193, 114));
                } else {
                    super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column).setBackground(Color.WHITE);
                }
                return l;
            }
        };

        dtcr.setHorizontalAlignment(SwingConstants.CENTER);
        tcm.getColumn(7).setCellRenderer(dtcr);


        //нулевой размер для столбца идентификаторов
        tcm.getColumn(0).setMinWidth(0);
        tcm.getColumn(0).setMaxWidth(0);
        tcm.getColumn(0).setResizable(false);


        tcm.getColumn(1).setHeaderRenderer(renderer);
        tcm.getColumn(1).setHeaderValue(selLable);

        //размер заголовка
        jht.setPreferredSize(new Dimension(30, 30));

        //конструируем кнопку связывания
        bindButton = new JButton(new ImageIcon(PNCRecorderGUI.class.getResource("link small (yg).png")));

        clearButton = new JButton(new ImageIcon(PNCRecorderGUI.class.getResource("archeology.png")));
    }

    //TODO end of section

    private void deserializeSettings() {
        String addr = System.getProperty("user.home");
        File f = new File(addr + "\\PNCT\\" + "Settings.data");
        if (f.exists()) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f))) {
                settings = (RecorderSettings) ois.readObject();
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
                initSettings();
                JOptionPane.showMessageDialog(null, "Не удалось загрузить настройки,\nбудут применены значения " +
                        "по-умолчанию");
            }
        } else {
            initSettings();
        }
    }

    private void initSettings() {
        settings = new RecorderSettings(1000, "PARTA", "",
                "localhost", "3050", true, true, 50);
        settings.regExpTruncate = "ПОЗ. |П. ";
        settings.prodMgrPort = "1433";
        settings.prodMgrNameOrIP = "OEMZ-POBEDA.oemz.ru";
        settings.prodMgrAliasName = "test_ProdMgrDB";
        settings.prodMgrLogin = "1";
        settings.prodMgrPass = "1";
    }

    private String getConnectionString() {
        String selectConnStr = "jdbc:firebirdsql://%2%:%3%/%1%" +
                "?useUnicode=yes&localEncoding=ISO8859_1";
        if (settings.useAlias) {
            selectConnStr = selectConnStr.replaceAll("%1%", settings.DBAliasName);
        } else {
            selectConnStr = selectConnStr.replaceAll("%1%", settings.DBFilePath);
        }
        selectConnStr = selectConnStr.replaceAll("%2%", settings.DBNameOrIP);
        selectConnStr = selectConnStr.replaceAll("%3%", settings.DBPort);
        return selectConnStr;
    }

    //рендерер JComponent
    static class JComponentTableCellRenderer implements TableCellRenderer {
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            return (JComponent) value;
        }
    }

    //слушатель событий мыши таблицы
    class TableMouseListener implements MouseListener {
        final boolean[] firstPress = {false};

        @Override
        public void mouseClicked(MouseEvent e) {

        }

        @Override
        public void mousePressed(MouseEvent e) {

            if (e.getSource() instanceof JTableHeader) {
                if (tableModel.getRowCount() == 0) return;
                //находим позицию столбца в представлении по позиции в модели
                int pos = table1.convertColumnIndexToView(1);
                //определяем, произошел ли клик на участке заголовка в найденной позиции
                if (jht.columnAtPoint(e.getPoint()) == pos) {
                    int rows = tableModel.getRowCount();
                    for (int i = 0; i < rows; i++) {
                        if (firstPress[0]) {
                            tableModel.setValueAt(Boolean.TRUE, i, 1);
                        } else {
                            tableModel.setValueAt(Boolean.FALSE, i, 1);
                        }
                    }
                    firstPress[0] = !firstPress[0];
                }
            }

            if (e.getSource() instanceof JTable) {
                if (e.getButton() == MouseEvent.BUTTON3) {
                    JTable source = (JTable) e.getSource();
                    int row = source.rowAtPoint(e.getPoint());
                    int column = source.columnAtPoint(e.getPoint());
                    source.requestFocus();
                    source.setRowSelectionInterval(row, row);
                    source.changeSelection(row, column, false, false);
                }
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if (e.getSource() instanceof JTable) {
                if (e.getButton() == MouseEvent.BUTTON3) {
                    JTable source = (JTable) e.getSource();
                    int row = source.rowAtPoint(e.getPoint());
                    int column = source.columnAtPoint(e.getPoint());
                    Object valueInCell = table1.getValueAt(row, column);
                    if (valueInCell instanceof String) {
                        JPopupMenu popup = new JPopupMenu();
                        JMenuItem menuItemCopy = new JMenuItem();
                        menuItemCopy.setText("Копировать");
                        popup.add(menuItemCopy);
                        popup.show(e.getComponent(), e.getX(), e.getY());

                        menuItemCopy.addActionListener(e1 -> {
                            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                            StringSelection stringSelection = new StringSelection((String) valueInCell);
                            clipboard.setContents(stringSelection, null);
                        });
                    }
                }
            }
        }

        @Override
        public void mouseEntered(MouseEvent e) {

        }

        @Override
        public void mouseExited(MouseEvent e) {

        }
    }

    static class InputFieldsMouseListener implements MouseListener {

        @Override
        public void mouseClicked(MouseEvent e) {

        }

        @Override
        public void mousePressed(MouseEvent e) {

        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if (e.getButton() == MouseEvent.BUTTON3) {
                JPopupMenu popup = new JPopupMenu();
                JMenuItem menuItemPaste = new JMenuItem();
                menuItemPaste.setText("Вставить");
                popup.add(menuItemPaste);
                popup.show(e.getComponent(), e.getX(), e.getY());

                menuItemPaste.addActionListener(e1 -> {
                    Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                    Transferable t = clipboard.getContents(null);
                    if (t == null) return;
                    JTextField jtf = (JTextField) e.getComponent();
                    try {
                        int last = jtf.getSelectionEnd();
                        int first = jtf.getSelectionStart();
                        String firstStr = jtf.getText().substring(0, first);
                        String secStr = jtf.getText().substring(last);
                        jtf.setText(firstStr + t.getTransferData(DataFlavor.stringFlavor) + secStr);
                    } catch (UnsupportedFlavorException | IOException unsupportedFlavorException) {
                        unsupportedFlavorException.printStackTrace();
                    }
                });
            }
        }


        @Override
        public void mouseEntered(MouseEvent e) {

        }

        @Override
        public void mouseExited(MouseEvent e) {

        }
    }

    //слушатель событий главного окна
    class MainFrameListener implements WindowListener {

        @Override
        public void windowOpened(WindowEvent e) {
            String addr = System.getProperty("user.home");
            File colPos = new File(addr + "\\PNCT\\" + "ColConfig.data");
            if (!colPos.exists()) {
                return;
            }

            //иконка на заголовок столбца "выделить"
            Icon selIcon = new ImageIcon(PNCRecorderGUI.class.getResource("check (1).png"));
            JLabel selLable = new JLabel(selIcon);

            ArrayList<Integer> posList = new ArrayList<>();
            ArrayList<Integer> sizeList = new ArrayList<>();

            try (ObjectInputStream ois =
                         new ObjectInputStream(new FileInputStream(colPos))) {
                posList = (ArrayList) ois.readObject();
                sizeList = (ArrayList) ois.readObject();
                likeFlCheckBox.setSelected((Boolean) ois.readObject());
                useInjProdChkBx.setSelected((Boolean) ois.readObject());
            } catch (IOException | ClassNotFoundException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(null, "Не удалось загрузить настройки");
            }

            //удаляем из листа ноль, где бы он ни был, и ставим его в нулевую позицию
            posList.removeIf(x -> x == 0);
            posList.add(0, 0);

            Object[] colsHeader = new Object[7];
            colsHeader[0] = "PieceID";
            colsHeader[1] = selLable;
            colsHeader[2] = "Изделие";
            colsHeader[3] = "Марка";
            colsHeader[4] = "Позиция";
            colsHeader[5] = "Маркировка";
            colsHeader[6] = "Заказ";

            for (int i = 0; i < 7; i++) {
                setColPosition(posList.get(i), i, colsHeader, sizeList.get(i));
            }

            setInjProdCompsProps();
        }

        @Override
        public void windowClosing(WindowEvent e) {
            saveTableTunes();
        }

        @Override
        public void windowClosed(WindowEvent e) {
            saveTableTunes();
        }

        @Override
        public void windowIconified(WindowEvent e) {

        }

        @Override
        public void windowDeiconified(WindowEvent e) {

        }

        @Override
        public void windowActivated(WindowEvent e) {
            jht.setReorderingAllowed(settings.allowHeaderReposition);
        }

        //метод помещающий столбец модели в на позицию в отображении
        private void setColPosition(int modelPos, int columnPosMoveTo, Object[] colsHeader, int size) {
            //если позиция в модели нулевая - ничего не делаем (ID всегда на месте)
            if (modelPos == 0) return;

            TableColumnModel tcm = table1.getColumnModel();
            DefaultTableCellRenderer dtcr = (DefaultTableCellRenderer) jht.getDefaultRenderer();
            if (colsHeader[modelPos] instanceof JLabel) {
                tcm.getColumn(columnPosMoveTo).setHeaderRenderer(new JComponentTableCellRenderer());
                tcm.getColumn(columnPosMoveTo).setMaxWidth(30);
            } else {
                tcm.getColumn(columnPosMoveTo).setHeaderRenderer(dtcr);
                tcm.getColumn(columnPosMoveTo).setMaxWidth(1000);
                tcm.getColumn(columnPosMoveTo).setPreferredWidth(size);
            }
            tcm.getColumn(columnPosMoveTo).setModelIndex(modelPos);
            tcm.getColumn(columnPosMoveTo).setHeaderValue(colsHeader[modelPos]);
        }

        private void saveTableTunes() {
            ArrayList<Integer> posList = new ArrayList<>();
            ArrayList<Integer> sizeList = new ArrayList<>();
            Enumeration<TableColumn> en = table1.getColumnModel().getColumns();

            while (en.hasMoreElements()) {
                TableColumn tc = en.nextElement();
                posList.add(tc.getModelIndex());
                sizeList.add(tc.getWidth());
            }

            FileOutputStream fos = null;
            ObjectOutputStream oos = null;
            String addr = System.getProperty("user.home");
            File f = new File(addr + "\\PNCT\\" + "ColConfig.data");
            if (!f.exists()) {
                if (!f.getParentFile().exists()) {
                    if (!f.getParentFile().mkdir()) {
                        JOptionPane.showMessageDialog(null, "Не удалось создать каталог хранения конфигурации");
                        return;
                    }
                }
            }

            try {
                fos = new FileOutputStream(f);
                oos = new ObjectOutputStream(fos);

                oos.writeObject(posList);
                oos.writeObject(sizeList);
                oos.writeObject(likeFlCheckBox.isSelected());
                oos.writeObject(useInjProdChkBx.isSelected());

                fos.close();
                oos.close();
            } catch (IOException ioException) {
                ioException.printStackTrace();
                JOptionPane.showMessageDialog(null, "Не удалось сохранить настройки");
            } finally {
                if (oos != null) {
                    try {
                        oos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (fos != null) {
                    try {
                        fos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        @Override
        public void windowDeactivated(WindowEvent e) {

        }
    }

    static class CustomTableModel extends DefaultTableModel {

        public List<Integer> startCounts = new ArrayList<>();

        public CustomTableModel() {
            super();
        }
    }
}
