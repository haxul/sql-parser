import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


public class SqlParser implements QueryAble {

    private final String sqlQuery;

    public static SqlParser create(String sqlQuery) {
        String trimmedSql = sqlQuery.trim();
        if (!trimmedSql.endsWith(";"))
            throw new SqlParserException("Incorrect finish of sql query. ';' is not found ");
        return new SqlParser(trimmedSql);
    }

    private SqlParser(String sqlQuery) {
        this.sqlQuery = sqlQuery;
    }

    private String prettifySqlItem(String item) {
        String column = item.trim();
        if (column.equals("")) throw new SqlParserException("Empty field. Error is between ', ,'");
        return column.replaceAll("\\s+", " ");
    }

    @Override
    public List<String> getColumns() {
        Pattern pattern = Pattern.compile("(?i)(?<=select)\\s+[A-Za-z0-9.,*\\s()_]+(?=from)");
        Matcher matcher = pattern.matcher(sqlQuery);
        if (!matcher.find()) throw new SqlParserException("Fields to select are not found");
        String[] columns = matcher.group().split(",");
        return Arrays.stream(columns)
                .map(this::prettifySqlItem)
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getSource() {
        String regex = "(?i)(?<=from)\\s+[A-Za-z0-9.,*\\s()_]+" +
                "(?=(LEFT|RIGHT|FULL|JOIN|WHERE|OFFSET|LIMIT|;|GROUP BY|ORDER BY|\\(\\s*select.*\\)))";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(sqlQuery);
        if (!matcher.find()) return new ArrayList<>();
        String[] sources = matcher.group().split(",");
        return Arrays.stream(sources)
                .map(this::prettifySqlItem)
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getJoins() {
        return null;
    }

    @Override
    public List<String> getWhereClauses() {
        return null;
    }

    @Override
    public List<String> getGroupByColumns() {
        return null;
    }

    @Override
    public List<String> getSortColumns() {
        return null;
    }

    @Override
    public List<Integer> getLimit() {
        return null;
    }

    @Override
    public List<Integer> getOffset() {
        return null;
    }

    @Override
    public List<String> getSubQueries() {
        return null;
    }
}
