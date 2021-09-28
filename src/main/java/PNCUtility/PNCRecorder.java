package PNCUtility;

import java.io.UnsupportedEncodingException;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

public class PNCRecorder {

    public Connection con;

    //метод-деструктор
    public void delete(int pieceID) throws SQLException {
        try {
            con = DriverManager.getConnection("jdbc:firebirdsql://localhost:3050/PARTA" +
                            "?useUnicode=yes&localEncoding=ISO8859_1",
                    "SYSDBA", "masterkey");
            con.setAutoCommit(false);
            //dummy - одиночный вызов. целевой способ работы - вызов для списка

            deletePiece(pieceID);

            //коммит (в случае полного успеха всей транзакции для списка)
            con.commit();

        } catch (SQLException throwables) {
            throwables.printStackTrace();
            con.rollback();
        } finally {
            con.close();
        }
    }

    //метод-генератор
    public void generate(int prototypeID) throws SQLException {
        try {
            con = DriverManager.getConnection("jdbc:firebirdsql://localhost:3050/C:\\Pronc2\\pnc_db\\VBPARTA.GDB" +
                            "?useUnicode=yes&localEncoding=ISO8859_1",
                    "SYSDBA", "masterkey");
            con.setAutoCommit(false);
            //dummy - одиночный вызов. целевой способ работы - вызов для списка
            createPiece(prototypeID, "заказ8888", "gadget", "до ", " после",
                    42, false, 0, 0);

            //коммит (в случае полного успеха всей транзакции для списка)
            con.commit();

        } catch (SQLException throwables) {
            throwables.printStackTrace();
            con.rollback();
        } finally {
            con.close();
        }
    }

    //создание полной копии детали
    public void createPiece(int prototypeID, String numOrder, String prodName, String markPreAdd, String markPostAdd,
                            int amount, boolean safeMark, int preTruncate, int postTruncate) throws SQLException {

        int targetPieceID = createPieceRecord(prototypeID, numOrder, prodName, markPreAdd, markPostAdd, amount, safeMark,
                preTruncate, postTruncate);

        //создание записей об операциях
        createOperationsRecords(prototypeID, targetPieceID);

        //создание записей о контурах
        createContoursRecords(prototypeID, targetPieceID);

        //создание записей о COUPE
        String coupeListQuery = "SELECT COUPE_ID from COUPE where COUPE_PIECE_ID = %source%";
        String coupeCopyQuery = "INSERT INTO COUPE (COUPE_PIECE_ID, COUPE_TYPE, COUPE_POSITION, " +
                "COUPE_AME_AILE, COUPE_DISTX, COUPE_DISTY, COUPE_ANGLE, COUPE_CH1, COUPE_INT1) SELECT %targ%, " +
                "COUPE_TYPE, COUPE_POSITION, COUPE_AME_AILE, COUPE_DISTX, COUPE_DISTY, " +
                "COUPE_ANGLE, COUPE_CH1, COUPE_INT1 from COUPE where COUPE_ID = %item%";
        createAnyRecords(prototypeID, targetPieceID, coupeListQuery, coupeCopyQuery);

        //создание записей о гравировках - начало
        //запрос на получение записей о гравировках источника
        String scribeListQuery = "SELECT SCRIB_ID from SCRIB where SCRIB_PIECE_ID = %source%";
        //копирование записей источника для записи-приемника
        String scribeCopyQuery = "INSERT INTO SCRIB (SCRIB_PIECE_ID, SCRIB_FACE, SCRIB_MACRO, SCRIB_ORDRE, " +
                "SCRIB_ORI_DROITE, SCRIB_X_DEBUT, SCRIB_Y_DEBUT, SCRIB_NB_POINT, SCRIB_CH1, SCRIB_INT1, " +
                "SCRIB_REEL1, SCRIB_SPECIAL) SELECT %targ%, SCRIB_FACE, SCRIB_MACRO, SCRIB_ORDRE, " +
                "SCRIB_ORI_DROITE, SCRIB_X_DEBUT, " +
                "SCRIB_Y_DEBUT, SCRIB_NB_POINT, SCRIB_CH1, SCRIB_INT1, SCRIB_REEL1, SCRIB_SPECIAL from SCRIB " +
                "where SCRIB_ID = %item%";
        //отправляем запросы
        createAnyRecords(prototypeID, targetPieceID, scribeListQuery, scribeCopyQuery);

        //получаем лист идентификаторов гравировок для источника
        List<Integer> scribsListSource = getIds(prototypeID, "SELECT SCRIB_ID FROM SCRIB WHERE SCRIB_PIECE_ID = " +
                "%source%");
        //получаем лист идентификаторов гравировок для приемника
        List<Integer> scribsListTarget = getIds(targetPieceID, "SELECT SCRIB_ID FROM SCRIB WHERE SCRIB_PIECE_ID = " +
                "%source%");

        //создаем итератор по гравировкам приемника
        Iterator<Integer> targIDIter = scribsListTarget.iterator();

        //для каждой гравировки из листа источника создаем копии точек в лист приемника
        for (int scribRecordSourceID : scribsListSource) {
            String scribPointQuery = "SELECT SCRPT_ID from SCRPT where SCRPT_SCRIB_ID = %source%";
            String scribPtsCopyQuery = "INSERT INTO SCRPT (SCRPT_SCRIB_ID, SCRPT_ORDRE, SCRPT_DISTX, " +
                    "SCRPT_DISTY, SCRPT_RAYON, SCRPT_MARQUE, SCRPT_CH1, SCRPT_INT1, SCRPT_REEL1) SELECT %targ%, " +
                    "SCRPT_ORDRE, SCRPT_DISTX, SCRPT_DISTY, SCRPT_RAYON, SCRPT_MARQUE, SCRPT_CH1, SCRPT_INT1, " +
                    "SCRPT_REEL1 FROM SCRPT WHERE SCRPT_ID = %item%";
            int targID = 0;
            if (targIDIter.hasNext()) {
                targID = targIDIter.next();
            } else break;
            createAnyRecords(scribRecordSourceID, targID, scribPointQuery, scribPtsCopyQuery);
        }
        //создание записей о гравировках - конец

        //создание записей о фрезеровках - начало
        //запрос на получение записей о фрезеровках источника
        String millingListQuery = "SELECT FRAIS_ID from FRAIS where FRAIS_PIECE_ID = %source%";
        //копирование записей источника для записи-приемника
        String millingCopyQuery = "INSERT INTO FRAIS (FRAIS_PIECE_ID, FRAIS_FACE, FRAIS_ORDRE, FRAIS_ORI_DROITE, " +
                "FRAIS_NB_POINT, FRAIS_MARQUE, FRAIS_CH1, FRAIS_INT1, FRAIS_INT2, FRAIS_INT3, FRAIS_REEL1, " +
                "FRAIS_REEL2, FRAIS_REEL3, FRAIS_TYPE_MACRO, FRAIS_ZONE_MACRO, FRAIS_TECHNO, FRAIS_NUM_MACRO, " +
                "FRAIS_DIAM_FRAISE, FRAIS_DATA_MACRO) SELECT %targ%, FRAIS_FACE, FRAIS_ORDRE, FRAIS_ORI_DROITE, " +
                "FRAIS_NB_POINT, FRAIS_MARQUE, FRAIS_CH1, FRAIS_INT1, FRAIS_INT2, FRAIS_INT3, FRAIS_REEL1, " +
                "FRAIS_REEL2, FRAIS_REEL3, FRAIS_TYPE_MACRO, FRAIS_ZONE_MACRO, FRAIS_TECHNO, FRAIS_NUM_MACRO, " +
                "FRAIS_DIAM_FRAISE, FRAIS_DATA_MACRO from FRAIS where FRAIS_ID = %item%";
        //отправляем запросы базе
        createAnyRecords(prototypeID, targetPieceID, millingListQuery, millingCopyQuery);

        //получаем лист идентификаторов фрезеровок для источника
        List<Integer> millingsListSource = getIds(prototypeID, "SELECT FRAIS_ID FROM FRAIS WHERE FRAIS_PIECE_ID " +
                "= %source%");
        //получаем лист идентификаторов фрезеровок для приемника
        List<Integer> millingsListTarget = getIds(targetPieceID, "SELECT FRAIS_ID FROM FRAIS WHERE " +
                "FRAIS_PIECE_ID = %source%");

        //создаем итератор по фрезеровкам приемника
        Iterator<Integer> targMilIDIter = millingsListTarget.iterator();

        //для каждой фрезеровки из листа источника создаем копии точек в лист приемника
        for (int milRecordSourceID : millingsListSource) {
            String milPointQuery = "SELECT FRAPT_ID from FRAPT where FRAPT_FRAIS_ID = %source%";
            String milPtsCopyQuery = "INSERT INTO FRAPT (FRAPT_FRAIS_ID, FRAPT_ORDRE, FRAPT_N_PASSE, " +
                    "FRAPT_P_FINITION, FRAPT_MARQUE, FRAPT_ANGLE, FRAPT_X, FRAPT_Y, FRAPT_Z, FRAPT_R, FRAPT_CH1, " +
                    "FRAPT_INT1, FRAPT_INT2, FRAPT_INT3, FRAPT_REEL1, FRAPT_REEL2, FRAPT_REEL3) SELECT %targ%, " +
                    "FRAPT_ORDRE, FRAPT_N_PASSE, FRAPT_P_FINITION, FRAPT_MARQUE, FRAPT_ANGLE, FRAPT_X, FRAPT_Y, " +
                    "FRAPT_Z, FRAPT_R, FRAPT_CH1, FRAPT_INT1, FRAPT_INT2, FRAPT_INT3, FRAPT_REEL1, FRAPT_REEL2, " +
                    "FRAPT_REEL3 FROM FRAPT WHERE FRAPT_ID = %item%";
            int targID = 0;
            if (targMilIDIter.hasNext()) {
                targID = targMilIDIter.next();
            } else break;
            createAnyRecords(milRecordSourceID, targID, milPointQuery, milPtsCopyQuery);
        }
        //создание записей о фрезеровках - конец

        //создание записей о "разрезках" - начало
        //запрос на получение записей о разрезках источника
        String decouListQuery = "SELECT DECOU_ID from DECOU where DECOU_PIECE_ID = %source%";
        //копирование записей источника для записи-приемника
        String decouCopyQuery = "INSERT INTO DECOU (DECOU_PIECE_ID, DECOU_FACE, DECOU_ORDRE, DECOU_DEBFIN, " +
                "DECOU_ORI_DROITE, DECOU_NB_POINT, DECOU_GROUPE, DECOU_MARQUE, DECOU_CH1, DECOU_INT1, " +
                "DECOU_REEL1, DECOU_TYPE, DECOU_FREE1, DECOU_FREE2, DECOU_FREE3) SELECT %targ%, DECOU_FACE, " +
                "DECOU_ORDRE, DECOU_DEBFIN, DECOU_ORI_DROITE, DECOU_NB_POINT, DECOU_GROUPE, DECOU_MARQUE, " +
                "DECOU_CH1, DECOU_INT1, DECOU_REEL1, DECOU_TYPE, DECOU_FREE1, DECOU_FREE2, DECOU_FREE3 from DECOU" +
                " where DECOU_ID = %item%";
        //отправляем запросы базе
        createAnyRecords(prototypeID, targetPieceID, decouListQuery, decouCopyQuery);

        //получаем лист идентификаторов разрезок для источника
        List<Integer> decousListSource = getIds(prototypeID, "SELECT DECOU_ID FROM DECOU WHERE DECOU_PIECE_ID " +
                "= %source%");
        //получаем лист идентификаторов разрезок для приемника
        List<Integer> decousListTarget = getIds(targetPieceID, "SELECT DECOU_ID FROM DECOU WHERE " +
                "DECOU_PIECE_ID = %source%");

        //создаем итератор по разрезкам приемника
        Iterator<Integer> targDecIDIter = decousListTarget.iterator();

        //для каждой разрезки из листа источника создаем копии точек в лист приемника
        for (int decRecordSourceID : decousListSource) {
            String milPointQuery = "SELECT DECPT_ID from DECPT where DECPT_DECOU_ID = %source%";
            String milPtsCopyQuery = "INSERT INTO DECPT (DECPT_DECOU_ID, DECPT_ORDRE, DECPT_MARQUE, DECPT_ANGLE, " +
                    "DECPT_X, DECPT_Y, DECPT_R, DECPT_CH1, DECPT_INT1, DECPT_REEL1, DECPT_Z, DECPT_RX, DECPT_RY, " +
                    "DECPT_RZ, DECPT_TYPE, DECPT_FREE1, DECPT_FREE2, DECPT_FREE3, DECPT_FREE4, DECPT_FREE5, " +
                    "DECPT_FREE6) SELECT %targ%, DECPT_ORDRE, DECPT_MARQUE, DECPT_ANGLE, DECPT_X, DECPT_Y, " +
                    "DECPT_R, DECPT_CH1, DECPT_INT1, DECPT_REEL1, DECPT_Z, DECPT_RX, DECPT_RY, DECPT_RZ, " +
                    "DECPT_TYPE, DECPT_FREE1, DECPT_FREE2, DECPT_FREE3, DECPT_FREE4, DECPT_FREE5, DECPT_FREE6 " +
                    "FROM DECPT WHERE DECPT_ID = %item%";
            int targID = 0;
            if (targDecIDIter.hasNext()) {
                targID = targDecIDIter.next();
            } else break;
            createAnyRecords(decRecordSourceID, targID, milPointQuery, milPtsCopyQuery);
        }
        //создание записей о "разрезках" - конец
    }

    /**
     * Потенциально опасный для консистентности базы метод. Если записи, кодирующие различные конструктивные особенности
     * деталей, достаточно хорошо изучены, и могут удаляться без повреждения каких-либо структур, то запись о самой
     * детали вполне может фигурировать в малоисследованных областях базы, например, в таблицах учета выполенных
     * работ, в раскроях, и тому подобном. Удаление записи о детали без удаления ее из таких таблиц, и последовательным
     * каскадным удалением связанных записей, нарушит целостность базы. Для безопасности, рекомендуется выполнять
     * удаление из ProNC на этом этапе.
     * P.S. Однако, по всей видимости, в сложившихся условиях работы никаких "опасных" связей не образуется - все
     * таблицы, которые содержат какие-то связи (на уровне анализа имен атрибутов), пусты. То есть функционал ProNC,
     * задействующий их для хранения данных, не используется
     */
    //удаление детали (каскадно со всеми данными о конструктивных признаках во всех таблицах )
    private void deletePiece(int pieceID) throws SQLException {
        //получаем контейнер идентификаторов
        FieldPieceStruct fps = getPieceFieldStruct(pieceID);
        //двухуровневые удаления
        //удаляем точки фрезеровок
        for (int milPtId : fps.milPtsList) {
            deleteRecord("FRAPT", "FRAPT_ID", milPtId);
        }
        //...и сами фрезеровки
        for (int milId : fps.millingsList) {
            deleteRecord("FRAIS", "FRAIS_ID", milId);
        }
        //удаляем точки decou
        for (int decPtId : fps.decPtsList) {
            deleteRecord("DECPT", "DECPT_ID", decPtId);
        }
        //...и сами decou
        for (int decId : fps.decousList) {
            deleteRecord("DECOU", "DECOU_ID", decId);
        }
        //удаляем точки гравировки
        for (int scrPtId : fps.scribPtsList) {
            deleteRecord("SCRPT", "SCRPT_ID", scrPtId);
        }
        //... и сами гравировки
        for (int scribeID : fps.scribesList) {
            deleteRecord("SCRIB", "SCRIB_ID", scribeID);
        }
        //одноуровневые удаления
        //удаление обработок
        for (int usinaID : fps.usinaList) {
            deleteRecord("USINA", "USINA_ID", usinaID);
        }
        //удаление контуров
        for (int contID : fps.contoList) {
            deleteRecord("CONTO", "CONTO_ID", contID);
        }
        //удаление торцевых резов
        for (int coupeID : fps.coupeList) {
            deleteRecord("COUPE", "COUPE_ID", coupeID);
        }
        //и удаление записи самой детали
        deleteRecord("PIECE", "PIECE_ID", pieceID);
    }

    public FieldPieceStruct getPieceFieldStruct(int pieceID) throws SQLException {
        FieldPieceStruct fps = new FieldPieceStruct();

        //запись таблиц первого уровня
        fps.decousList = getIDs("DECOU", "PIECE_ID", pieceID);
        fps.millingsList = getIDs("FRAIS", "PIECE_ID", pieceID);
        fps.contoList = getIDs("CONTO", "PIECE_ID", pieceID);
        fps.scribesList = getIDs("SCRIB", "PIECE_ID", pieceID);
        fps.usinaList = getIDs("USINA", "PIECE_ID", pieceID);
        fps.coupeList = getIDs("COUPE", "PIECE_ID", pieceID);
        //запись таблиц второго уровня
        for (int milRecID : fps.millingsList) {
            fps.milPtsList.addAll(getIDs("FRAPT", "FRAIS_ID", milRecID));
        }
        for (int scrRecID : fps.scribesList) {
            fps.scribPtsList.addAll(getIDs("SCRPT", "SCRIB_ID", scrRecID));
        }
        for (int decRecID : fps.decousList) {
            fps.decPtsList.addAll(getIDs("DECPT", "DECOU_ID", decRecID));
        }
        return fps;
    }

    //создание записи о детали
    private int createPieceRecord(int sourceID, String numOrder, String prodName, String markPreAdd, String markPostAdd,
                                  int amount, boolean safeMark, int preTruncate, int postTruncate) throws SQLException {

        Statement stm = con.createStatement();
        int pieceID = -1;

        try {
            //дергаем генератор чтоб получить новый идентификатор
            String getFreePieceID = "SELECT GEN_ID(PIECE_ID_GEN, 1) FROM RDB$DATABASE";
            ResultSet rs = stm.executeQuery(getFreePieceID);
            rs.next();
            //идентификатор для новой детали
            pieceID = rs.getInt(1);

            //поле для даты и времени
            String datePattern = "dd.MM.yyyy";
            String timePattern = "HH:mm:ss";
            Date rawDate = new Date();
            String date = new SimpleDateFormat(datePattern).format(rawDate);
            String time = new SimpleDateFormat(timePattern).format(rawDate);

            String detNameQuery = "SELECT PIECE_MARQUAGE FROM piece where PIECE_ID = " + sourceID;
            ResultSet detNameRS = stm.executeQuery(detNameQuery);
            detNameRS.next();
            String detMark = "";
            try {
                if (safeMark) {
                    byte[] dName = detNameRS.getBytes(1);
                    //если не нулл, то выполняем чтение из рекордсета (иначе строка останется пустой)
                    if (!(dName == null)) {
                        detMark = new String(dName, "ISO8859_1");
                        if (detMark.length() <= postTruncate + preTruncate) {
                            detMark = "";
                        } else {
                            detMark = detMark.substring(preTruncate, detMark.length() - postTruncate);
                        }
                    }
                }

                markPreAdd = new String(markPreAdd.getBytes("cp1251"), "ISO8859_1");
                markPostAdd = new String(markPostAdd.getBytes("cp1251"), "ISO8859_1");
                numOrder = new String(numOrder.getBytes("cp1251"), "ISO8859_1");
                if(prodName != null){
                    prodName = new String(prodName.getBytes("cp1251"), "ISO8859_1");
                }
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            detMark = markPreAdd + detMark + markPostAdd;

            //Создание копии записи в таблице "детали"
            String createPieceQuery = "INSERT INTO piece " +
                    "(PIECE_ID, PIECE_PROFI_ID, PIECE_AFFAIRE, PIECE_PLAN, PIECE_REPERE, PIECE_SOUSENS, " +
                    "PIECE_MARQUAGE, " +
                    "PIECE_QAF, PIECE_QTF, PIECE_QTPLACE, PIECE_LONGUEUR, PIECE_POIDS, PIECE_SURFACE, " +
                    "PIECE_NBUSI, PIECE_REMARQUE, PIECE_ORIGINE, PIECE_MACHINE, PIECE_TRAITEMENT, PIECE_DATE, " +
                    "PIECE_HEURE, PIECE_EXPORTE, PIECE_TYPEPIECE, PIECE_LARGEURJARRET, PIECE_MARQUE, " +
                    "PIECE_SEL_EXPORT, PIECE_CUSTOM1, PIECE_CUSTOM2, PIECE_CUSTOM3, PIECE_CH1, PIECE_INT1, " +
                    "PIECE_DURET_ID, PIECE_USER_CREATE, PIECE_USER_MODIF, PIECE_PRS, " +
                    "PIECE_PRS_PENTE, " +
                    "PIECE_PRS_HT_PIEDS, PIECE_PRS_HT_TETE, PIECE_PRS_SUREP_F1, PIECE_PRS_SUREP_F3, " +
                    "PIECE_PRS_POS_X_SUREP_F1, PIECE_PRS_POS_X_SUREP_F3, PIECE_PRS_DATA1, PIECE_PRS_TETE_OK, " +
                    "PIECE_FINITION, PIECE_ERP_ID, PIECE_ERP_NOM, PIECE_DEST1, PIECE_DEST2, PIECE_ERP_CUST1, " +
                    "PIECE_ERP_CUST2, PIECE_ERP_CUST3, PIECE_ERP_CUST4) " +
                    "SELECT " + pieceID + ", PIECE_PROFI_ID, %1%, PIECE_PLAN, PIECE_REPERE, " +
                    " '" + numOrder + "', '" +
                    detMark + "', " + amount + ", 0, PIECE_QTPLACE, PIECE_LONGUEUR, PIECE_POIDS, " +
                    "PIECE_SURFACE, PIECE_NBUSI, PIECE_REMARQUE, 'PNCRecorder', PIECE_MACHINE, PIECE_TRAITEMENT, '"
                    + date + "', '" + time +
                    "', 0, PIECE_TYPEPIECE, PIECE_LARGEURJARRET, PIECE_MARQUE, " +
                    "PIECE_SEL_EXPORT, PIECE_CUSTOM1, PIECE_CUSTOM2, PIECE_CUSTOM3, PIECE_CH1, PIECE_INT1, " +
                    "PIECE_DURET_ID, PIECE_USER_CREATE, PIECE_USER_MODIF, PIECE_PRS, " +
                    "PIECE_PRS_PENTE, " +
                    "PIECE_PRS_HT_PIEDS, PIECE_PRS_HT_TETE, PIECE_PRS_SUREP_F1, PIECE_PRS_SUREP_F3, " +
                    "PIECE_PRS_POS_X_SUREP_F1, PIECE_PRS_POS_X_SUREP_F3, PIECE_PRS_DATA1, PIECE_PRS_TETE_OK, " +
                    "PIECE_FINITION, PIECE_ERP_ID, PIECE_ERP_NOM, PIECE_DEST1, PIECE_DEST2, PIECE_ERP_CUST1, " +
                    "PIECE_ERP_CUST2, PIECE_ERP_CUST3, PIECE_ERP_CUST4 " +
                    "FROM piece " +
                    "WHERE piece_id=" + sourceID;

            if (prodName == null) {
                createPieceQuery = createPieceQuery.replaceAll("%1%", "PIECE_AFFAIRE");
            } else {
                createPieceQuery = createPieceQuery.replaceAll("%1%", "'"  + prodName + "'");
            }

            stm.execute(createPieceQuery);

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return pieceID;
    }

    //операции
    private void createOperationsRecords(int sourceID, int targetID) throws SQLException {
        Statement stm = con.createStatement();
        String getOperationListQuery = "SELECT USINA_ID from USINA where USINA_PIECE_ID='" + sourceID + "'";
        ResultSet rs = stm.executeQuery(getOperationListQuery);
        List<Integer> opIDsList = new ArrayList<>();
        while (rs.next()) {
            opIDsList.add(rs.getInt(1));
        }
        for (Integer opID : opIDsList) {
            String copyOpQuery = "INSERT INTO USINA (USINA_PIECE_ID, USINA_FACE, USINA_USINAGE, USINA_DISTX, " +
                    "USINA_DISTY, USINA_DISTZ, USINA_DIAM1, USINA_DIAM2, USINA_LOBLONG, USINA_HOBLONG, " +
                    "USINA_AOBLONG, USINA_CH1, USINA_INT1, USINA_CHAINE_MARQ) SELECT '" + targetID +
                    "', USINA_FACE, USINA_USINAGE, USINA_DISTX, USINA_DISTY, USINA_DISTZ, USINA_DIAM1, " +
                    "USINA_DIAM2, USINA_LOBLONG, USINA_HOBLONG, USINA_AOBLONG, USINA_CH1, USINA_INT1, " +
                    "USINA_CHAINE_MARQ from usina where USINA_ID ='" + opID + "'";
            stm.execute(copyOpQuery);
        }
    }

    //контуры
    private void createContoursRecords(int sourceID, int targetID) throws SQLException {
        Statement stm = con.createStatement();
        String getContourListQuery = "SELECT CONTO_ID from CONTO where CONTO_PIECE_ID='" + sourceID + "'";
        ResultSet rs = stm.executeQuery(getContourListQuery);
        List<Integer> contIDsList = new ArrayList<>();
        while (rs.next()) {
            contIDsList.add(rs.getInt(1));
        }
        for (Integer contID : contIDsList) {
            String copyContQuery = "INSERT INTO CONTO (CONTO_PIECE_ID, CONTO_FACE, CONTO_NUMERO, " +
                    "CONTO_DISTX, CONTO_DISTY, CONTO_RAYON, CONTO_ANGLE, CONTO_CHANF, CONTO_TYPE, CONTO_GRUGEAGE, " +
                    "CONTO_CH1, CONTO_INT1) SELECT '" + targetID +
                    "', CONTO_FACE, CONTO_NUMERO, CONTO_DISTX, CONTO_DISTY, CONTO_RAYON, CONTO_ANGLE, CONTO_CHANF, " +
                    "CONTO_TYPE, CONTO_GRUGEAGE, CONTO_CH1, CONTO_INT1 from CONTO where CONTO_ID ='" + contID + "'";
            stm.execute(copyContQuery);
        }
    }

    //любые (добавлено позже, можно/нужно зарефакторить пару методов выше с использованием этого - унифицированного)
    private void createAnyRecords(int sourceID, int targetID, String getListQuery, String copyQuery) throws SQLException {
        Statement stm = con.createStatement();
        getListQuery = getListQuery.replaceAll("%source%", "'" + sourceID + "'");
        ResultSet rs = stm.executeQuery(getListQuery);
        List<Integer> attrIDsList = new ArrayList<>();
        while (rs.next()) {
            attrIDsList.add(rs.getInt(1));
        }
        for (Integer itemID : attrIDsList) {
            String localCopyQuery = copyQuery;
            localCopyQuery = localCopyQuery.replaceAll("%targ%", "'" + targetID + "'");
            localCopyQuery = localCopyQuery.replaceAll("%item%", "'" + itemID + "'");
            stm.execute(localCopyQuery);
        }
    }

    //устаревшее дерьмо
    //получение листа идентификаторов, по специально подготовленному запросу
    private List<Integer> getIds(int sourceID, String getIdsQuery) throws SQLException {
        Statement stm = con.createStatement();
        getIdsQuery = getIdsQuery.replaceAll("%source%", "'" + sourceID + "'");
        ResultSet rs = stm.executeQuery(getIdsQuery);
        List<Integer> attrIDsList = new ArrayList<>();
        while (rs.next()) {
            attrIDsList.add(rs.getInt(1));
        }
        return attrIDsList;
    }

    //это дерьмо получше
    //получение листа идентификаторов по имени таблицы и значению внешнего ключа
    private List<Integer> getIDs(String tableName, String fKName, int fKValue) throws SQLException {
        String getListQuery = "SELECT %0% FROM %1% WHERE %2% = '%3%'";
        getListQuery = getListQuery.replaceAll("%0%", tableName + "_ID");
        getListQuery = getListQuery.replaceAll("%1%", tableName);
        getListQuery = getListQuery.replaceAll("%2%", tableName + "_" + fKName);
        getListQuery = getListQuery.replaceAll("%3%", String.valueOf(fKValue));
        Statement stm = con.createStatement();
        ResultSet rs = stm.executeQuery(getListQuery);
        List<Integer> attrIDsList = new ArrayList<>();
        while (rs.next()) {
            attrIDsList.add(rs.getInt(1));
        }
        return attrIDsList;
    }

    //универсальный метод-деструктор записи
    private void deleteRecord(String tableName, String primaryKeyName, int primaryKeyVal) throws SQLException {
        String delQuery = "DELETE FROM %1% WHERE %2% = '%3%'";
        delQuery = delQuery.replaceAll("%1%", tableName);
        delQuery = delQuery.replaceAll("%2%", primaryKeyName);
        delQuery = delQuery.replaceAll("%3%", String.valueOf(primaryKeyVal));
        Statement stm = con.createStatement();
        stm.execute(delQuery);
    }

    private static class FieldPieceStruct {
        //таблицы первого уровня
        List<Integer> decousList;
        List<Integer> millingsList;
        List<Integer> contoList;
        List<Integer> scribesList;
        List<Integer> usinaList;
        List<Integer> coupeList;
        //таблицы второго уровня
        List<Integer> milPtsList = new ArrayList<>();
        List<Integer> scribPtsList = new ArrayList<>();
        List<Integer> decPtsList = new ArrayList<>();
    }
}
