import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SqlParserTest {


    @Test
    public void testSelect() {
        assertEquals("*", SqlParser.parse("  selEct     *  fRom testtable ; ").getColumns().get(0));
        assertArrayEquals(new String[]{"author.name", "count(book.id)", "sum(book.cost)"}, SqlParser.parse(" SELECT author.name, count(book.id), sum(book.cost) FRoM author ;").getColumns().toArray());
        assertArrayEquals(new String[]{"author.name as name", "*"}, SqlParser.parse(" select    author.name    as   name, * from test ;").getColumns().toArray());
        assertThrows(SqlParserException.class, () -> SqlParser.parse(" select  'test string '  ;").getColumns());
        assertThrows(SqlParserException.class, () -> SqlParser.parse(" 'test string ' from  ;").getColumns());
        assertThrows(SqlParserException.class, () -> SqlParser.parse(" select field.name, , * from testtable;").getColumns());
    }


    @Test
    public void testFrom() {
        assertArrayEquals(new String[]{"author", "testtable"}, SqlParser.parse(" SELECT * FRoM author, testtable ;").getSource().toArray());
        assertArrayEquals(new String[]{"author", "testtable"}, SqlParser.parse(" SELECT * FRoM author   , testtable     LEFT   JOIN ...;").getSource().toArray());
        assertArrayEquals(new String[]{"author", "testtable"}, SqlParser.parse(" SELECT * FRoM author   , testtable     GROUp   by ...;").getSource().toArray());
        assertArrayEquals(new String[]{"author", "testtable"}, SqlParser.parse(" SELECT * FRoM author   , testtable     WHere ...;").getSource().toArray());
        assertArrayEquals(new String[]{"author", "testtable"}, SqlParser.parse(" SELECT * FRoM author   , testtable     Offset ...;").getSource().toArray());
        assertArrayEquals(new String[]{"author", "testtable"}, SqlParser.parse(" SELECT * FRoM author   , testtable     limit ...;").getSource().toArray());
        assertArrayEquals(new String[]{"author", "testtable"}, SqlParser.parse(" SELECT * FRoM author   , testtable     (select ...) ...;").getSource().toArray());
        assertThrows(SqlParserException.class, () -> SqlParser.parse(" SELEct * from testtable, , test;").getSource());
    }

    @Test
    public void testJoins() {
        assertArrayEquals(
                new String[]{"table2 on table2.id = table.id", "table3 on table3.id = table.id"},
                SqlParser.parse(" seleCt * from table " +
                        "left join table2 on table2.id   = table.id " +
                        "full join table3   on table3.id =    table.id ; ").getJoins().toArray()
        );

        assertArrayEquals(
                new String[]{"table2 on table2.id = table.id", "table3 on table3.id = table.id"},
                SqlParser.parse(" seleCt * from table " +
                        "left join table2 on table2.id   = table.id " +
                        "full join table3   on table3.id =    table.id WHERE ... ; ").getJoins().toArray()
        );

        assertArrayEquals(
                new String[]{"table2 on table2.id = table.id", "table3 on table3.id = table.id"},
                SqlParser.parse(" seleCt * from table " +
                        "left join table2 on table2.id   = table.id " +
                        "full join table3   on table3.id =    table.id offset ... ; ").getJoins().toArray()
        );

        assertArrayEquals(
                new String[]{"table2 on table2.id = table.id", "table3 on table3.id = table.id"},
                SqlParser.parse(" seleCt * from table " +
                        "left join table2 on table2.id   = table.id " +
                        "full join table3   on table3.id =    table.id order  by ... ; ").getJoins().toArray()
        );

        assertThrows(SqlParserException.class, () -> SqlParser.parse(" seleCt * from table full join table3   oN table3.id == table.id order  by ... ; ").getJoins());
    }

    @Test
    public void testClauses() {
        assertArrayEquals(new String[]{"r.id = 10"}, SqlParser.parse("select * from users left join role r on    users.name =     r.name where r.id     = 10  ;").getWhereClauses().toArray());
        assertArrayEquals(new String[]{"id => 10"}, SqlParser.parse("select * from users where id => 10  group by users.id ;").getWhereClauses().toArray());
        assertArrayEquals(new String[]{"hello = 'som123'", "id > 10", "id < 5"}, SqlParser.parse("select * from  (select  * from users where id > 10 and id < 15) where hello = 'som123' aNd id > 10 or id < 5 ;").getWhereClauses().toArray());
        assertThrows(SqlParserException.class, () -> SqlParser.parse("select * from testtable where name =!! 'hello';").getWhereClauses());
        assertThrows(SqlParserException.class, () -> SqlParser.parse("select * from testtable where name isnot null;").getWhereClauses());
        assertArrayEquals(new String[]{"name is null"}, SqlParser.parse(" select * from test where name is null;").getWhereClauses().toArray());
        assertArrayEquals(new String[]{"name is not null"}, SqlParser.parse(" select * from test where name is not   null;").getWhereClauses().toArray());
    }


    @Test
    public void testGroupBy() {
        assertArrayEquals(new String[]{"users.id", "users.name"}, SqlParser.parse("select * from users left join  role on role.name = users.name group by    users.id, users.name   limit 1;").getGroupByColumns().toArray());
        assertArrayEquals(new String[]{"id"}, SqlParser.parse("select * from (select * from users group   by id) group by id;").getGroupByColumns().toArray());
        assertArrayEquals(new String[]{"id"}, SqlParser.parse("select * from (select * from users group    by id) group by id having;").getGroupByColumns().toArray());
        assertThrows(SqlParserException.class, () -> SqlParser.parse("select * from (select * from users group by id) group by id, , ;").getGroupByColumns());
    }

    @Test
    public void testOrderBy() {
        assertArrayEquals(new String[]{"users.id", "users.name"}, SqlParser.parse("select * from users left join  role on role.name = users.name order by    users.id, users.name   limit 1;").getSortColumns().toArray());
        assertArrayEquals(new String[]{"id"}, SqlParser.parse("select * from (select * from users group   by id) order by id;").getSortColumns().toArray());
        assertArrayEquals(new String[]{"id"}, SqlParser.parse("select * from (select * from users group    by id) order   by id limit;").getSortColumns().toArray());
        assertThrows(SqlParserException.class, () -> SqlParser.parse("select * from (select * from users group by id) order by id, , ;").getSortColumns());
    }


    @Test
    public void testSubqueries() {
        assertArrayEquals(new String[]{"select * from users group by id order by id", "select * from roles"}, SqlParser.parse(
                "select * from (  select *    from    users    group   by id order by  id) " +
                "left join (select * from roles) on role.name = users.name order by users.id, users.name   limit 1;").getSubQueries().toArray()
        );
        assertArrayEquals(new String[]{"select * from users group by id order by id"}, SqlParser.parse(
                "select * from (select * from users group by id order by  id) order by id, name limit 1;").getSubQueries().toArray()
        );
    }

    @Test
    public void testLimit() {
        assertEquals(1, SqlParser.parse("select * from (select * from users  order by id, name limit 1 ;").getLimit());
        assertThrows(SqlParserException.class, () -> SqlParser.parse("select * from (select * from users  order by id, name limit 1 where id > 1;").getLimit());
        assertEquals(1, SqlParser.parse("select * from (select * from users  order by id, name limit 1 offset 1;").getLimit());
        assertThrows(SqlParserException.class, () -> SqlParser.parse("select * from (select * from users  order by id, name limit 1a;").getLimit());
        assertThrows(SqlParserException.class, () -> SqlParser.parse("select * from (select * from users  order by id, name limit 1.1;").getLimit());
    }

    @Test
    public void testOffset() {
        assertEquals(1, SqlParser.parse("select * from (select * from users  order by id, name Offset 1 ;").getOffset());
        assertThrows(SqlParserException.class, () -> SqlParser.parse("select * from (select * from users  order by id, name offSet 1 where id > 1;").getOffset());
        assertEquals(1, SqlParser.parse("select * from (select * from users  order by id, name limit 1 offset 1;").getOffset());
        assertThrows(SqlParserException.class, () -> SqlParser.parse("select * from (select * from users  order by id, name offset 1a;").getOffset());
        assertThrows(SqlParserException.class, () -> SqlParser.parse("select * from (select * from users  order by id, name offset 1.1;").getOffset());
    }


}
