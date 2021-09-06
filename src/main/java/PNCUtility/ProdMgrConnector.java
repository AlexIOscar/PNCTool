package PNCUtility;

import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.util.List;

public class ProdMgrConnector {

    String sPort;
    String sServer;
    String sDataBase;
    String sUserName;
    String sPassword;
    String connectionUrl;

    public ProdMgrConnector(String sPort, String sServer, String sDataBase, String sUserName, String sPassword) {

        this.sPort = sPort;
        this.sServer = sServer;
        this.sDataBase = sDataBase;
        this.sUserName = sUserName;
        this.sPassword = sPassword;

        connectionUrl = String.format("jdbc:sqlserver://%s:%s;databaseName=%s;user=%s;password=%s",
                this.sServer, this.sPort, this.sDataBase, this.sUserName, this.sPassword);
    }

    public int[] getAmountsArr(String order, int party, List<String> markNames, List<String> posNames) {
        String query = "SELECT CASE WHEN sprGroupKontrAgentov.[NUMBERGROUPKONTRAGENTOV] != 10 then RIGHT(YEAR" +
                "(HistoryZakazov.[CURDATE]), 1) + CAST (sprGroupKontrAgentov.[NUMBERGROUPKONTRAGENTOV] as VARCHAR) + " +
                "CASE WHEN Len(HistoryZakazov.[NUMZAKAZA]) < 2 THEN '0' + \n" +
                "CAST (HistoryZakazov.[NUMZAKAZA] AS VARCHAR) ELSE \n" +
                "CAST (HistoryZakazov.[NUMZAKAZA] AS VARCHAR) END ELSE \n" +
                "HistoryZakazov.[NUMZAKAZA] END AS NUMBER_ORDER, HistoryObjectsOfDogSpec.NUMB_POS_SP AS NUMBER_PARTY," +
                " HistoryMarok.NUMBERMARKI, sprNameMKonstr.NAMEMKONSTR AS NAME_OF_MARK,\n" +
                "HistoryMarok.KOLVOT * HistoryObjectsOfDogSpec.KOLVO AS MARKS_AMOUNT_BY_PARTY, HistoryPosition" +
                ".[POSITION], \n" +
                "sprNameProfAndMet.NAMEPROFANDM + ' ' + sprProfAndMetizov.SECHENIESORTAMENT AS NAME_MP, " +
                "(HistoryPosition.KOLVOT) * HistoryMarok.KOLVOT * HistoryObjectsOfDogSpec" +
                ".KOLVO AS POS_AMOUNT_BY_PARTY FROM HistoryPosition, HistoryMarok, sprNameMKonstr, sprProfAndMetizov," +
                " sprNameProfAndMet, HistoryZakazov, sprGroupKontrAgentov, HistoryObjectsOfDogSpec\n" +
                "WHERE HistoryMarok.IDMKONSTR = sprNameMKonstr.IDMKONSTR\n" +
                "AND HistoryPosition.IDMAROK = HistoryMarok.IDMAROK\n" +
                "AND sprProfAndMetizov.IDPRANDMETIZOV = HistoryPosition.IDPRANDMETIZOV\n" +
                "AND sprProfAndMetizov.IDNAMEPANDM = sprNameProfAndMet.IDNAMEPANDM\n" +
                "AND HistoryZakazov.IDZAKAZA = HistoryMarok.IDZAKAZA\n" +
                "AND sprGroupKontrAgentov.IDGROUPKONTRAGENTOV = HistoryZakazov.IDGROUPKONTRAGENTOV\n" +
                "AND HistoryObjectsOfDogSpec.ID_ORDER = HistoryZakazov.IDZAKAZA AND (CASE WHEN sprGroupKontrAgentov" +
                ".[NUMBERGROUPKONTRAGENTOV] != 10 then \n" +
                "  RIGHT(YEAR(HistoryZakazov.[CURDATE]), 1) + \n" +
                "  CAST (sprGroupKontrAgentov.[NUMBERGROUPKONTRAGENTOV] as VARCHAR) + \n" +
                "  CASE WHEN Len(HistoryZakazov.[NUMZAKAZA]) < 2 THEN '0' + \n" +
                "  CAST (HistoryZakazov.[NUMZAKAZA] AS VARCHAR) ELSE \n" +
                "  CAST (HistoryZakazov.[NUMZAKAZA] AS VARCHAR) END ELSE \n" +
                "  HistoryZakazov.[NUMZAKAZA] END) = '%1%'" +
                "AND HistoryObjectsOfDogSpec.NUMB_POS_SP = '%2%'" +
                "ORDER BY 1, 2, 3, 6";

        String query4 = "SELECT DISTINCT ListOfStringsByShiftTask.NUMBER_ORDER, HistoryObjectsOfDogSpec" +
                ".NUMB_POS_SP AS NUMB_PARTY, jrnShiftTaskRegistry.ID_SHIFT_TASK, " +
                "jrnShiftTaskRegistry.NUMBER_OF_TASK, jrnShiftTaskRegistry.DATE_OF_TASK, sprDevices.SHORT_NAME + " +
                "' :: ' + sprTechnologicalOperation.SHORT_NAME AS NAME_DEVICE, sprDevices.NAME_DEVICE + ' :: ' + " +
                "sprTechnologicalOperation.NAME_TECHNOLOGICAL_OPERATION AS FULL_NAME_DEVICE, " +
                "ListOfStringsByShiftTask.ID_POSITION AS IDPOSITION, \n" +
                "ListOfStringsByShiftTask.SHEET_NUMBER AS NUMLISTA, \n" +
                "ListOfStringsByShiftTask.MARK_NUMBER, \n" +
                "ListOfStringsByShiftTask.POS_NUMBER AS [POSITION], LEFT(ListOfStringsByShiftTask.NAME_MP, 2) + '" +
                " ' + ListOfStringsByShiftTask.SECTION_MP + ' ' + ListOfStringsByShiftTask.NAMESTEEL AS NAME_MP, " +
                "ListOfStringsByShiftTask.MASSA,  ROUND((ListOfStringsByShiftTask.TASK_POS_AMOUNT_TAK +  " +
                "ListOfStringsByShiftTask.TASK_POS_AMOUNT_NAOB) * ListOfStringsByShiftTask.MASSA, 3) AS " +
                "FULL_MASSA, ListOfStringsByShiftTask.TASK_POS_AMOUNT_TAK, ListOfStringsByShiftTask" +
                ".TASK_POS_AMOUNT_NAOB, sprDevices.NAME_DEVICE, sprTechnologicalOperation" +
                ".NAME_TECHNOLOGICAL_OPERATION, HistoryPosition.[LENGTH] AS LEN_OF_POS, HistoryPosition.WIDTH, " +
                "ListOfStringsByShiftTask.NAME_MP + ' ' + ListOfStringsByShiftTask.SECTION_MP + ' ' + CASE WHEN " +
                "HistoryPosition.TYPEIZM = 1 THEN ListOfStringsByShiftTask.NAMESTEEL ELSE ISNULL((SELECT " +
                "sprMarokSteel.NAMESTEEL FROM sprMarokSteel, jrnCuttingsCards WHERE sprMarokSteel.IDMARKSTEEL = " +
                "ISNULL(jrnCuttingsCards.ID_STEEL_OF_SHEET, -1) AND jrnCuttingsCards.ID_CUTTING_CARD = " +
                "ListOfStringsByShiftTask.ID_CUTTING_CARD), '') END AS FULL_NAME_MP, CASE WHEN ISNULL" +
                "(ListOfStringsByShiftTask.IS_HAVE_MARKING, 0) = 1 THEN 'X' ELSE '' END AS IS_HAVE_MARKING, " +
                "HistoryObjectsOfDogSpec.KOLVO * HistoryMarok.KOLVOT AS MARKS_AMOUNT,\n" +
                "ListOfStringsByShiftTask.MARK_NAME,\n" +
                "ListOfStringsByShiftTask.CARD_NUMBER, ListOfStringsByShiftTask.CALC_TIME_PROC AS TIME_OF_CARD, " +
                "CASE WHEN ISNULL((SELECT TOP(1) ISNULL(jrnCuttingsCards.SHEETS_AMOUNT, 0) FROM " +
                "jrnCuttingsCards WHERE jrnCuttingsCards.ID_CUTTING_CARD = ListOfStringsByShiftTask" +
                ".ID_CUTTING_CARD), 0) <> 0 THEN ISNULL((SELECT TOP(1) ISNULL(jrnCuttingsCards.SHEETS_AMOUNT, 0) " +
                "FROM jrnCuttingsCards\n" +
                "  WHERE jrnCuttingsCards.ID_CUTTING_CARD = ListOfStringsByShiftTask.ID_CUTTING_CARD), 0) * \n" +
                "  ListOfStringsByShiftTask.CALC_TIME_PROC ELSE\n" +
                "  ListOfStringsByShiftTask.CALC_TIME_PROC \n" +
                "END AS FULL_TIME_OF_CARD, CAST(HistoryObjectsOfDogSpec.NUMB_POS_SP AS VARCHAR(50)) + ' партия' " +
                "AS SHOR_PARTY_NUMB, HistoryPosition.TECH_DESCRIPTION FROM jrnShiftTaskRegistry, sprWorkCenters, " +
                "sprDevices, " +
                "sprTechOpersOnWorkCenter, sprTechnologicalOperation, HistoryObjectsOfDogSpec, HistoryZakazov, " +
                "ListOfStringsByShiftTask,\n" +
                "HistoryPosition, HistoryMarok WHERE jrnShiftTaskRegistry.ID_WORK_CENTER = sprWorkCenters" +
                ".ID_WORK_CENTER AND sprWorkCenters.ID_DEVICE = sprDevices.ID_DEVICE \n" +
                "AND sprTechOpersOnWorkCenter.ID_WORK_CENTER = sprWorkCenters.ID_WORK_CENTER \n" +
                "AND sprTechnologicalOperation.ID_TECHNOLOGICAL_OPERATION = sprTechOpersOnWorkCenter.ID_TECHNOLOGICAL_OPERATION \n" +
                "AND sprWorkCenters.TYPE_OF_WORK_CENTER = 0 \n" +
                "AND sprTechOpersOnWorkCenter.ID_OPERATION = jrnShiftTaskRegistry.ID_OPERATION \n" +
                "AND HistoryZakazov.IDZAKAZA = HistoryObjectsOfDogSpec.ID_ORDER \n" +
                "AND jrnShiftTaskRegistry.ID_SHIFT_TASK = ListOfStringsByShiftTask.ID_SHIFT_TASK \n" +
                "AND ListOfStringsByShiftTask.ID_MOUNTING_GROUP = HistoryObjectsOfDogSpec.ID_HISTORY_OBJECTS\n" +
                "AND HistoryPosition.IDPOSITION = ListOfStringsByShiftTask.ID_POSITION\n" +
                "AND HistoryMarok.IDMAROK = HistoryPosition.IDMAROK\n" +
                "AND jrnShiftTaskRegistry.ID_OPERATION IN (88, 134, 128, 125)\n" +
                "AND ListOfStringsByShiftTask.NUMBER_ORDER = '%1%' " +
                "AND HistoryObjectsOfDogSpec.NUMB_POS_SP = '%2%'" +
                "ORDER BY 2, 7, 4, 10, 11";

        int[] outArr = new int[markNames.size()];

        try (Connection DBConnection = DriverManager.getConnection(connectionUrl)) {
            Statement stStatement = DBConnection.createStatement();

            query = query.replaceAll("%1%", order);
            query = query.replaceAll("%2%", String.valueOf(party));

            ResultSet rs = stStatement.executeQuery(query);

            while (rs.next()) {
                //System.out.println(rs.getString(3) + "|" + rs.getInt(6) + "|" + rs.getInt(8));
                int innerIndex = 0;
                for (String markName : markNames) {
                    if (rs.getString(3).equals(markName)) {
                        if (String.valueOf(rs.getInt(6)).equals(posNames.get(innerIndex))) {
                            outArr[innerIndex] = rs.getInt(8);
                            break;
                        }
                    }
                    innerIndex++;
                }
            }

        } catch (SQLException throwables) {
            String errMsg = throwables.getMessage();
            String msg = "Ошибка в работе с сервером ProdMgr:";
            JOptionPane.showMessageDialog(null, msg + "\n" + errMsg);
            throwables.printStackTrace();
        }
        return outArr;
    }

    public int[] getAmountsArrDummy(String order, List<String> markNames, List<String> posNames) {
        int[] outPosAmounts = new int[markNames.size()];
        return new int[]{12, 13, 15, 45, 67};
    }
}
