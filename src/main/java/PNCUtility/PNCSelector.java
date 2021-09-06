package PNCUtility;

import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class PNCSelector {

    public Connection con;

    //лайк-флаг - заготовка для возможности создания запросов типа like
    public ResultSet selectRecords(String constrName, String orderName, String markName, boolean likeFlag) throws SQLException {
        StringBuilder selectQuery = new StringBuilder();
        //имя конструкции, оно же заказчик
        selectQuery.append("SELECT PIECE_ID, PIECE_AFFAIRE, PIECE_REPERE, PIECE_PLAN, PIECE_MARQUAGE, PIECE_SOUSENS, " +
                "PIECE_QAF FROM PIECE");
        boolean emptySet = true;

        if (!constrName.equals("*")) {
            selectQuery.append(" WHERE");
            emptySet = false;
            selectQuery.append(" UPPER(PIECE_AFFAIRE) %4% '%1%'");
        }

        //заказ, он же группа
        if (!orderName.equals("*")) {
            if (emptySet) {
                selectQuery.append(" WHERE");
                emptySet = false;
            } else {
                selectQuery.append(" AND");
            }
            selectQuery.append(" UPPER(PIECE_SOUSENS) %4% '%2%'");
        }

        //марка, оно же номер
        if (!markName.equals("*")) {
            if (emptySet) {
                selectQuery.append(" WHERE");
            } else {
                selectQuery.append(" AND");
            }
            selectQuery.append(" UPPER(PIECE_REPERE) %4% '%3%'");
        }

        if(likeFlag){
            orderName = "%" + orderName + "%";
            constrName = "%" + constrName + "%";
            markName = "%" + markName + "%";
        }

        String formattedQuery = selectQuery.toString().replaceAll("%1%", constrName);
        formattedQuery = formattedQuery.replaceAll("%2%", orderName);
        formattedQuery = formattedQuery.replaceAll("%3%", markName);
        formattedQuery = formattedQuery.replaceAll("%4%", likeFlag?"LIKE":"=");
        try {
            formattedQuery = new String(formattedQuery.getBytes("cp1251"), "ISO8859_1");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        Statement stm = con.createStatement();
        return stm.executeQuery(formattedQuery);
    }
}
