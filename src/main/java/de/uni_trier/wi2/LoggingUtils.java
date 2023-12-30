package de.uni_trier.wi2;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Arrays;

public class LoggingUtils {
    public static int MAX_LOGGED_STRING_LENGTH = 50000;

    public static String maxSubstring(Object obj){
        return maxSubstring(obj.toString());
    }

    public static String maxSubstring(Object[] objArr){
        return maxSubstring(Arrays.toString(objArr));
    }

    public static String maxSubstring(String str){
        return (str.length() > MAX_LOGGED_STRING_LENGTH ? str.substring(0, MAX_LOGGED_STRING_LENGTH) + "..." : str);
    }

    public static String stringOf(final ResultSet resultSet) throws SQLException {
        if (false) {
            ResultSetMetaData rsMetaData = resultSet.getMetaData();
            int colCount = rsMetaData.getColumnCount();

            String out = "";
            for (int i = 1; i < colCount; i++) {
                out = out + rsMetaData.getColumnName(i) + " | ";
            }
            out = out + rsMetaData.getColumnName(colCount);

            while (resultSet.next()) {
                String row = "";
                for (int i = 1; i <= colCount; i++) {
                    row += resultSet.getString(i) + " | ";
                }

                out = out + "\n" + row;

            }

            return resultSet.toString();
        }
        return resultSet.toString();
    }
}
