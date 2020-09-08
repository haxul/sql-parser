import java.util.List;

public interface SqlQueryAble {
    List<String> getColumns();
    List<String> getSource();
    List<String> getJoins();
    List<String> getWhereClauses();
    List<String> getGroupByColumns();
    List<String> getSortColumns();
    Integer getLimit();
    Integer getOffset();
    List<String> getSubQueries();
}
