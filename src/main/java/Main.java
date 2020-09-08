import java.util.List;

public class Main {
    public static void main(String[] args) {
        SqlParser sqlParser =  SqlParser.parse(" select * from users left join  role on role.name = users.name order by    users.id, users.name   limit 1;");
        List<String> whereClauses = sqlParser.getSortColumns();
    }
}
