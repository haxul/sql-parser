public class Main {
    public static void main(String[] args) {
        SqlParser sqlParser =  SqlParser.create(" select field.name, , * from testtable");
        sqlParser.getColumns();
    }
}
