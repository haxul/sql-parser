import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


public class SqlSelectParser implements SqlQueryAble {

    private final String sqlQuery;

    private SqlSelectParser(String sqlQuery) {
        this.sqlQuery = sqlQuery;
    }

    public static SqlSelectParser parse(String sqlQuery) {
        String trimmedSql = sqlQuery.trim();
        if (!trimmedSql.matches("(?i)^\\s*select.*")) throw new SqlParserException("'select' is not found");
        if (!trimmedSql.endsWith(";")) throw new SqlParserException("';' is not found in the end of query");
        return new SqlSelectParser(trimmedSql);
    }

    @Override
    public List<String> getGroupByColumns() {
        String regex = "(?<=group by)\\s+[A-Za-z0-9=.,*\\s_>'<!]+?(?=(order\\s+by|;|limit|offset|having))";
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        String groupByWithoutSpaceQuery = sqlQuery.replaceAll("group\\s+by", "group by");
        Matcher matcher = pattern.matcher(groupByWithoutSpaceQuery);
        if (!matcher.find()) return new ArrayList<>();
        return Arrays.stream(matcher.group().split(","))
                .map(item -> validateSqlItem(item, groupBy -> groupBy.equals(""), "Error is near 'group by'"))
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getSortColumns() {
        String regex = "(?<=order by)\\s+[A-Za-z0-9=.,*\\s_>'<!]+?(?=(;|limit|offset))";
        String orderByWithoutSpaceQuery = sqlQuery.replaceAll("order\\s+by", "order by");
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(orderByWithoutSpaceQuery);
        if (!matcher.find()) return new ArrayList<>();
        return Arrays.stream(matcher.group().split(","))
                .map(item -> validateSqlItem(item, orderBy -> orderBy.equals(""), "Error is near 'order by'"))
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getJoins() {
        String regex = "(?<=(join))[A-Za-z0-9.(),*\\s_=]+?(?=(right|join|left|full|WHERE|OFFSET|LIMIT|;|GROUP\\s+BY|ORDER\\s+BY))";
        Matcher matcher = Pattern.compile(regex, Pattern.CASE_INSENSITIVE).matcher(sqlQuery);
        List<String> joins = getMatchedList(matcher, new ArrayList<>());
        String errorMessage = "Perhaps cause is : ' join '";
        String correctJoinRegex = "(?i)[A-Za-z0-9.()\\s_]+\\s+on\\s+[A-Za-z0-9.\\s_()]+=[A-Za-z0-9.()\\s_]+";
        return joins.stream()
                .map((item) -> validateSqlItem(item, join -> !join.matches(correctJoinRegex), errorMessage))
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getColumns() {
        String regex = "(?<=select)\\s+[A-Za-z0-9.,*\\s()_]+?(?=from)";
        Matcher matcher = Pattern.compile(regex, Pattern.CASE_INSENSITIVE).matcher(sqlQuery);
        if (!matcher.find()) throw new SqlParserException("Fields to select are not found");
        String[] columns = matcher.group().split(",");
        return Arrays.stream(columns)
                .map((item) -> validateSqlItem(item, (col -> col.equals("")), "Perhaps cause is : ', ,'"))
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getSource() {
        String regex = "(?<=from)\\s+[A-Za-z0-9.,*\\s()_]+?" +
                "(?=(LEFT|RIGHT|FULL|JOIN|WHERE|OFFSET|LIMIT|;|GROUP\\s+BY|ORDER\\s+BY|\\(\\s*select.*\\)))";
        Matcher matcher = Pattern.compile(regex, Pattern.CASE_INSENSITIVE).matcher(sqlQuery);
        if (!matcher.find()) return new ArrayList<>();
        String[] sources = matcher.group().split(",");
        return Arrays.stream(sources)
                .map((item) -> validateSqlItem(item, col -> col.equals(""), "Perhaps cause is : ', ,'"))
                .collect(Collectors.toList());
    }

    private List<String> findClausesByType(ClauseType clauseType) {
        String sqlWithoutSubQueries = removeRawSubqueries(sqlQuery, getRawSubqueries());
        String regex = "(?<=(" + clauseType.toString() + "))\\s+[A-Za-z0-9=.,*\\s_>()'<!]+?(?=(group\\s+by|;|limit|offset|order\\s+by))";
        Matcher matcher = Pattern.compile(regex, Pattern.CASE_INSENSITIVE).matcher(sqlWithoutSubQueries);
        if (!matcher.find()) return new ArrayList<>();
        String correctClauseReg = "^\\s*[A-Za-z0-9=.(),*\\s_']+\\s*(=|!=|=>|<=|<>|>|<|\\s+is\\s+|is\\s+not\\s+)\\s*[A-Za-z0-9=.,()*\\s_']+\\s*$";
        return Arrays.stream(matcher.group().split("(?i)(and|or)"))
                .map(item -> validateSqlItem(item, (clause) -> !clause.matches(correctClauseReg), "Perhaps cause is near '" + clauseType.toString() + "'"))
                .collect(Collectors.toList());
    }

    @Override
    public Integer getLimit() {
        String regex = "(?<=limit)\\s+[A-Za-z0-9=.,*\\s_>'<!]+?(?=(;|offset))";
        Matcher matcher = Pattern.compile(regex, Pattern.CASE_INSENSITIVE).matcher(sqlQuery);
        if (!matcher.find()) return null;
        String limitValue = validateSqlItem(matcher.group(), limit -> !limit.matches("\\d+"), "Error is near 'limit'");
        return Integer.parseInt(limitValue);
    }

    @Override
    public Integer getOffset() {
        String regex = "(?<=offset)\\s+[A-Za-z0-9=.,*\\s_>'<!]+?(?=(;|limit))";
        Matcher matcher = Pattern.compile(regex, Pattern.CASE_INSENSITIVE).matcher(sqlQuery);
        if (!matcher.find()) return null;
        String offsetValue = validateSqlItem(matcher.group(), offset -> !offset.matches("\\d+"), "Error is near 'offset'");
        return Integer.parseInt(offsetValue);
    }

    @Override
    public List<String> getSubQueries() {
        return getRawSubqueries()
                .stream()
                .map(subquery -> subquery.replaceAll("(\\(\\s*|\\s*\\))", ""))
                .map(subquery -> subquery.replaceAll("\\s+", " "))
                .collect(Collectors.toList());
    }

    private List<String> getRawSubqueries() {
        String regex = "\\(\\s*select\\s+[A-Za-z0-9=.,*\\s_>'<!]+\\)";
        Matcher matcher = Pattern.compile(regex, Pattern.CASE_INSENSITIVE).matcher(sqlQuery);
        List<String> subqueries = getMatchedList(matcher, new ArrayList<>());
        return new ArrayList<>(subqueries);
    }

    private String validateSqlItem(String item, Predicate<String> predicate, String errorMessage) {
        var column = item.trim();
        if (predicate.test(column)) throw new SqlParserException(errorMessage);
        return column.replaceAll("\\s+", " ");
    }

    private List<String> getMatchedList(Matcher matcher, List<String> list) {
        if (matcher.find()) {
            list.add(matcher.group());
            return getMatchedList(matcher, list);
        } else return list;
    }

    private String removeRawSubqueries(String sqlQuery, List<String> subqueries) {
        if (subqueries.isEmpty()) return sqlQuery;
        var subquery = subqueries.remove(subqueries.size() - 1);
        return removeRawSubqueries(sqlQuery.replace(subquery, ""), subqueries);
    }

    @Override
    public List<String> getWhereClauses() {
        return findClausesByType(ClauseType.WHERE);
    }

    public List<String> getHavingClauses() {
        return findClausesByType(ClauseType.HAVING);
    }
}
