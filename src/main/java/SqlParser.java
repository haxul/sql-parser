import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


public class SqlParser implements SqlQueryAble {

    private final String sqlQuery;

    public static SqlParser create(String sqlQuery) {
        String trimmedSql = sqlQuery.trim();
        if (!trimmedSql.endsWith(";"))
            throw new SqlParserException("Incorrect end of sql query. ';' is not found ");
        return new SqlParser(trimmedSql);
    }

    private SqlParser(String sqlQuery) {
        this.sqlQuery = sqlQuery;
    }

    private String validSqlItem(String item, Predicate<String> predicate, String errorGuess) {
        String column = item.trim();
        if (predicate.test(column)) throw new SqlParserException("Empty field. " + errorGuess);
        return column.replaceAll("\\s+", " ");
    }

    @Override
    public List<String> getColumns() {
        String regex = "(?<=select)\\\\s+[A-Za-z0-9.,*\\\\s()_]+?(?=from)";
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(sqlQuery);
        if (!matcher.find()) throw new SqlParserException("Fields to select are not found");
        String[] columns = matcher.group().split(",");
        return Arrays.stream(columns)
                .map((item) -> validSqlItem(item, (col-> col.equals("")), "Perhaps cause is : ', ,'"))
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getSource() {
        String regex = "(?<=from)\\s+[A-Za-z0-9.,*\\s()_]+?" +
                "(?=(LEFT|RIGHT|FULL|JOIN|WHERE|OFFSET|LIMIT|;|GROUP\\s+BY|ORDER\\s+BY|\\(\\s*select.*\\)))";
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(sqlQuery);
        if (!matcher.find()) return new ArrayList<>();
        String[] sources = matcher.group().split(",");
        return Arrays.stream(sources)
                .map((item) -> validSqlItem(item, col -> col.equals(""), "Perhaps cause is : ', ,'"))
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getJoins() {
        String regex = "(?<=(join))[A-Za-z0-9.,*\\s_=]+?(?=(right|join|left|full|WHERE|OFFSET|LIMIT|;|GROUP\\s+BY|ORDER\\s+BY))";
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(sqlQuery);
        List<String> joins = new ArrayList<>();
        while (matcher.find()) {
            String found = matcher.group();
            joins.add(found);
        }
        String errorMessage = "Perhaps cause is : ' == '";
        String correctJoinRegex = "(?i)[A-Za-z0-9.\\s_]+\\s+on\\s+[A-Za-z0-9.\\s_]+=[A-Za-z0-9.\\s_]+";
        return joins.stream()
                .map((item) -> validSqlItem(item, join -> !join.matches(correctJoinRegex), errorMessage))
                .collect(Collectors.toList());
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
