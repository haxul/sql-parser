public class Main {
    public static void main(String[] args) {
        SqlParser sqlParser =  SqlParser.create(" seleCt * from table full join table3   on table3.id = = table.id order  by ... ;");
        sqlParser.getJoins();
    }
}
