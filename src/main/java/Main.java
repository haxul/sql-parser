import java.util.List;

public class Main {
    public static void main(String[] args) {
        SqlParser sqlParser =  SqlParser.parse(" select * from (select * from users group by id) group by id, , ;");
        List<String> whereClauses = sqlParser.getGroupByColumns();
    }
}
