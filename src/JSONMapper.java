import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;

/**
 * Utility to map JDBC ResultSets dynamically to JSON format using metadata introspection.
 */
public class JSONMapper {

    /**
     * Converts a ResultSet into a JSON array string.
     */
    public static String mapResultSetToJson(ResultSet rs) throws SQLException {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        ResultSetMetaData md = rs.getMetaData();
        int numCols = md.getColumnCount();
        boolean firstRow = true;

        while (rs.next()) {
            if (!firstRow) sb.append(",");
            firstRow = false;
            sb.append(mapRowToString(rs, md, numCols));
        }

        sb.append("]");
        return sb.toString();
    }

    /**
     * Converts the current row of a ResultSet to a single JSON object string, or returns null if no rows exist.
     */
    public static String mapResultSetToSingleObject(ResultSet rs) throws SQLException {
        ResultSetMetaData md = rs.getMetaData();
        int numCols = md.getColumnCount();
        if (rs.next()) {
            return mapRowToString(rs, md, numCols);
        }
        return "{}";
    }

    private static String mapRowToString(ResultSet rs, ResultSetMetaData md, int numCols) throws SQLException {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        for (int i = 1; i <= numCols; i++) {
            String colName = md.getColumnLabel(i); // getColumnLabel handles column aliases correctly
            Object colVal = rs.getObject(i);
            
            sb.append("\"").append(escapeJson(colName)).append("\":");
            
            if (colVal == null) {
                sb.append("null");
            } else {
                int type = md.getColumnType(i);
                if (type == Types.VARCHAR || type == Types.CHAR || type == Types.LONGVARCHAR || type == Types.TIMESTAMP || type == Types.DATE) {
                    sb.append("\"").append(escapeJson(colVal.toString())).append("\"");
                } else if (type == Types.BOOLEAN || type == Types.BIT) {
                    sb.append(colVal.toString().toLowerCase());
                } else {
                    sb.append(colVal.toString());
                }
            }
            
            if (i < numCols) sb.append(",");
        }
        sb.append("}");
        return sb.toString();
    }

    /**
     * Maps the current row of a ResultSet directly to a JSON object.
     */
    public static String mapRowToJsonObject(ResultSet rs) throws SQLException {
        ResultSetMetaData md = rs.getMetaData();
        int numCols = md.getColumnCount();
        return mapRowToString(rs, md, numCols);
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
