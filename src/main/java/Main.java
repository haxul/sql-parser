import java.util.List;

public class Main {
    public static void main(String[] args) {
        SqlParser sqlParser =  SqlParser.create(" select * from  (select  * from users where id > 10 and id < 15) where hello = 'som123' and id > 10 or id < 5 ;;");
        List<String> whereClauses = sqlParser.getWhereClauses();
    }
}
