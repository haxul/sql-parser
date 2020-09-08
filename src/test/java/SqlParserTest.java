import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SqlParserTest {


    @Test
    public void testSelect() {
        assertEquals("*", SqlParser.create("  selEct     *  fRom testtable ; ").getColumns().get(0));
        assertArrayEquals(new String[]{"author.name", "count(book.id)", "sum(book.cost)"}, SqlParser.create(" SELECT author.name, count(book.id), sum(book.cost) FRoM author ;").getColumns().toArray());
        assertArrayEquals(new String[]{"author.name as name", "*"}, SqlParser.create(" select    author.name    as   name, * from test ;").getColumns().toArray());
        assertThrows(SqlParserException.class, () -> SqlParser.create(" select  'test string '  ;").getColumns());
        assertThrows(SqlParserException.class, () -> SqlParser.create(" 'test string ' from  ;").getColumns());
        assertThrows(SqlParserException.class, () -> SqlParser.create(" select field.name, , * from testtable;").getColumns());
    }


    @Test
    public void testFrom() {
        assertArrayEquals(new String[]{"author", "testtable"}, SqlParser.create(" SELECT * FRoM author, testtable ;").getSource().toArray());
        assertArrayEquals(new String[]{"author", "testtable"}, SqlParser.create(" SELECT * FRoM author   , testtable     LEFT   JOIN ...;").getSource().toArray());
        assertArrayEquals(new String[]{"author", "testtable"}, SqlParser.create(" SELECT * FRoM author   , testtable     GROUp   by ...;").getSource().toArray());
        assertArrayEquals(new String[]{"author", "testtable"}, SqlParser.create(" SELECT * FRoM author   , testtable     WHere ...;").getSource().toArray());
        assertArrayEquals(new String[]{"author", "testtable"}, SqlParser.create(" SELECT * FRoM author   , testtable     Offset ...;").getSource().toArray());
        assertArrayEquals(new String[]{"author", "testtable"}, SqlParser.create(" SELECT * FRoM author   , testtable     limit ...;").getSource().toArray());
        assertArrayEquals(new String[]{"author", "testtable"}, SqlParser.create(" SELECT * FRoM author   , testtable     (select ...) ...;").getSource().toArray());
        assertThrows(SqlParserException.class, () -> SqlParser.create(" SELEct * from testtable, , test;").getSource());
    }

    @Test
    public void testJoins() {
        assertArrayEquals(
                new String[]{"table2 on table2.id = table.id", "table3 on table3.id = table.id" },
                SqlParser.create(" seleCt * from table " +
                        "left join table2 on table2.id   = table.id " +
                        "full join table3   on table3.id =    table.id ; ").getJoins().toArray()
        );

        assertArrayEquals(
                new String[]{"table2 on table2.id = table.id", "table3 on table3.id = table.id" },
                SqlParser.create(" seleCt * from table " +
                        "left join table2 on table2.id   = table.id " +
                        "full join table3   on table3.id =    table.id WHERE ... ; ").getJoins().toArray()
        );

        assertArrayEquals(
                new String[]{"table2 on table2.id = table.id", "table3 on table3.id = table.id" },
                SqlParser.create(" seleCt * from table " +
                        "left join table2 on table2.id   = table.id " +
                        "full join table3   on table3.id =    table.id offset ... ; ").getJoins().toArray()
        );

        assertArrayEquals(
                new String[]{"table2 on table2.id = table.id", "table3 on table3.id = table.id" },
                SqlParser.create(" seleCt * from table " +
                        "left join table2 on table2.id   = table.id " +
                        "full join table3   on table3.id =    table.id order  by ... ; ").getJoins().toArray()
        );

        assertThrows(SqlParserException.class, () -> SqlParser.create(" seleCt * from table full join table3   oN table3.id == table.id order  by ... ; ").getJoins());
    }

    @Test
    public void testClauses() {
        assertArrayEquals(new String[] {"r.id = 10"}, SqlParser.create("select * from users left join role r on    users.name =     r.name where r.id     = 10  ;").getWhereClauses().toArray());
        assertArrayEquals(new String[] {"id => 10"}, SqlParser.create("select * from users where id => 10  group by users.id ;").getWhereClauses().toArray());
        assertArrayEquals(new String[] {"hello = 'som123'",  "id > 10", "id < 5"}, SqlParser.create("select * from  (select  * from users where id > 10 and id < 15) where hello = 'som123' and id > 10 or id < 5 ;").getWhereClauses().toArray());
        assertThrows(SqlParserException.class, () -> SqlParser.create("select * from testtable where name =!! 'hello';").getWhereClauses());
        assertThrows(SqlParserException.class, () -> SqlParser.create("select * from testtable where name isnot null;").getWhereClauses());
        assertArrayEquals(new String[] {"name is null"}, SqlParser.create(" select * from test where name is null;").getWhereClauses().toArray());
        assertArrayEquals(new String[] {"name is not null"}, SqlParser.create(" select * from test where name is not   null;").getWhereClauses().toArray());
    }
}
