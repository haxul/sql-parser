import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SqlSelectParserTest {

    @Test
    public void testSelect() {
        assertEquals("*", SqlSelectParser.parse("  selEct     *  fRom testtable ; ").getColumns().get(0));
        assertArrayEquals(new String[]{"author.name", "count(book.id)", "sum(book.cost)"}, SqlSelectParser.parse(" SELECT author.name, count(book.id), sum(book.cost) FRoM author ;").getColumns().toArray());
        assertArrayEquals(new String[]{"author.name as name", "*"}, SqlSelectParser.parse(" select    author.name    as   name, * from test ;").getColumns().toArray());
        assertThrows(SqlParserException.class, () -> SqlSelectParser.parse(" select  'test string '  ;").getColumns());
        assertThrows(SqlParserException.class, () -> SqlSelectParser.parse(" 'test string ' from  ;").getColumns());
        assertThrows(SqlParserException.class, () -> SqlSelectParser.parse(" select field.name, , * from testtable;").getColumns());
    }

    @Test
    public void testFrom() {
        assertArrayEquals(new String[]{"author", "testtable"}, SqlSelectParser.parse(" SELECT * FRoM author, testtable ;").getSource().toArray());
        assertArrayEquals(new String[]{"author", "testtable"}, SqlSelectParser.parse(" SELECT * FRoM author   , testtable     LEFT   JOIN ...;").getSource().toArray());
        assertArrayEquals(new String[]{"author", "testtable"}, SqlSelectParser.parse(" SELECT * FRoM author   , testtable     GROUp   by ...;").getSource().toArray());
        assertArrayEquals(new String[]{"author", "testtable"}, SqlSelectParser.parse(" SELECT * FRoM author   , testtable     WHere ...;").getSource().toArray());
        assertArrayEquals(new String[]{"author", "testtable"}, SqlSelectParser.parse(" SELECT * FRoM author   , testtable     Offset ...;").getSource().toArray());
        assertArrayEquals(new String[]{"author", "testtable"}, SqlSelectParser.parse(" SELECT * FRoM author   , testtable     limit ...;").getSource().toArray());
        assertArrayEquals(new String[]{"author", "testtable"}, SqlSelectParser.parse(" SELECT * FRoM author   , testtable     (select ...) ...;").getSource().toArray());
        assertThrows(SqlParserException.class, () -> SqlSelectParser.parse(" SELEct * from testtable, , test;").getSource());
    }

    @Test
    public void testJoins() {
        assertArrayEquals(
                new String[]{"table2 on table2.id = table.id", "table3 on table3.id = table.id"},
                SqlSelectParser.parse(" seleCt * from table " +
                        "left join table2 on table2.id   = table.id " +
                        "full join table3   on table3.id =    table.id ; ").getJoins().toArray()
        );

        assertArrayEquals(
                new String[]{"table2 on table2.id = table.id", "table3 on table3.id = table.id"},
                SqlSelectParser.parse(" seleCt * from table " +
                        "left join table2 on table2.id   = table.id " +
                        "full join table3   on table3.id =    table.id WHERE ... ; ").getJoins().toArray()
        );

        assertArrayEquals(
                new String[]{"table2 on table2.id = table.id", "table3 on table3.id = table.id"},
                SqlSelectParser.parse(" seleCt * from table " +
                        "left join table2 on table2.id   = table.id " +
                        "full join table3   on table3.id =    table.id offset ... ; ").getJoins().toArray()
        );

        assertArrayEquals(
                new String[]{"table2 on table2.id = table.id", "table3 on table3.id = table.id"},
                SqlSelectParser.parse(" seleCt * from table " +
                        "left join table2 on table2.id   = table.id " +
                        "full join table3   on table3.id =    table.id order  by ... ; ").getJoins().toArray()
        );

        assertThrows(SqlParserException.class, () -> SqlSelectParser.parse(" seleCt * from table full join table3   oN table3.id == table.id order  by ... ; ").getJoins());
    }

    @Test
    public void testClauses() {
        assertArrayEquals(new String[]{"r.id = 10"}, SqlSelectParser.parse("select * from users left join role r on    users.name =     r.name where r.id     = 10  ;").getWhereClauses().toArray());
        assertArrayEquals(new String[]{"id => 10"}, SqlSelectParser.parse("select * from users where id => 10  group by users.id ;").getWhereClauses().toArray());
        assertArrayEquals(new String[]{"hello = 'som123'", "id > 10", "id < 5"}, SqlSelectParser.parse("select * from  (select  * from users where id > 10 and id < 15) where hello = 'som123' aNd id > 10 or id < 5 ;").getWhereClauses().toArray());
        assertThrows(SqlParserException.class, () -> SqlSelectParser.parse("select * from testtable where name =!! 'hello';").getWhereClauses());
        assertThrows(SqlParserException.class, () -> SqlSelectParser.parse("select * from testtable where name isnot null;").getWhereClauses());
        assertArrayEquals(new String[]{"name is null"}, SqlSelectParser.parse(" select * from test where name is null;").getWhereClauses().toArray());
        assertArrayEquals(new String[]{"name is not null"}, SqlSelectParser.parse(" select * from test where name is not   null;").getWhereClauses().toArray());
    }

    @Test
    public void testGroupBy() {
        assertArrayEquals(new String[]{"users.id", "users.name"}, SqlSelectParser.parse("select * from users left join  role on role.name = users.name group by    users.id, users.name   limit 1;").getGroupByColumns().toArray());
        assertArrayEquals(new String[]{"id"}, SqlSelectParser.parse("select * from (select * from users group   by id) group by id;").getGroupByColumns().toArray());
        assertArrayEquals(new String[]{"id"}, SqlSelectParser.parse("select * from (select * from users group    by id) group by id having;").getGroupByColumns().toArray());
        assertThrows(SqlParserException.class, () -> SqlSelectParser.parse("select * from (select * from users group by id) group by id, , ;").getGroupByColumns());
    }

    @Test
    public void testOrderBy() {
        assertArrayEquals(new String[]{"users.id", "users.name"}, SqlSelectParser.parse("select * from users left join  role on role.name = users.name order by    users.id, users.name   limit 1;").getSortColumns().toArray());
        assertArrayEquals(new String[]{"id"}, SqlSelectParser.parse("select * from (select * from users group   by id) order by id;").getSortColumns().toArray());
        assertArrayEquals(new String[]{"id"}, SqlSelectParser.parse("select * from (select * from users group    by id) order   by id limit;").getSortColumns().toArray());
        assertThrows(SqlParserException.class, () -> SqlSelectParser.parse("select * from (select * from users group by id) order by id, , ;").getSortColumns());
    }

    @Test
    public void testSubqueries() {
        assertArrayEquals(new String[]{"select * from users group by id order by id", "select * from roles"}, SqlSelectParser.parse(
                "select * from (   select *   from users    group by id    order by id) " +
                "left join (select * from roles) on role.name = users.name order by users.id, users.name   limit 1;").getSubQueries().toArray()
        );
        assertArrayEquals(new String[]{"select * from users group by id order by id"}, SqlSelectParser.parse(
                "select * from (  select * from   users group by id order by  id   ) order by id, name limit 1;").getSubQueries().toArray()
        );
    }

    @Test
    public void testLimit() {
        assertEquals(1, SqlSelectParser.parse("select * from (select * from users  order by id, name limit 1 ;").getLimit());
        assertThrows(SqlParserException.class, () -> SqlSelectParser.parse("select * from (select * from users  order by id, name limit 1 where id > 1;").getLimit());
        assertEquals(1, SqlSelectParser.parse("select * from (select * from users  order by id, name limit 1 offset 1;").getLimit());
        assertThrows(SqlParserException.class, () -> SqlSelectParser.parse("select * from (select * from users  order by id, name limit 1a;").getLimit());
        assertThrows(SqlParserException.class, () -> SqlSelectParser.parse("select * from (select * from users  order by id, name limit 1.1;").getLimit());
    }

    @Test
    public void testOffset() {
        assertEquals(1, SqlSelectParser.parse("select * from (select * from users  order by id, name Offset 1 ;").getOffset());
        assertThrows(SqlParserException.class, () -> SqlSelectParser.parse("select * from (select * from users  order by id, name offSet 1 where id > 1;").getOffset());
        assertEquals(1, SqlSelectParser.parse("select * from (select * from users  order by id, name limit 1 offset 1;").getOffset());
        assertThrows(SqlParserException.class, () -> SqlSelectParser.parse("select * from (select * from users  order by id, name offset 1a;").getOffset());
        assertThrows(SqlParserException.class, () -> SqlSelectParser.parse("select * from (select * from users  order by id, name offset 1.1;").getOffset());
    }

    @Test
    public void testSql(){
        String sql = "SELECT author.name, count(book.id), sum(book.cost) " +
                "FROM author " +
                "LEFT JOIN book ON (author.id = book.author_id)" +
                "GROUP BY author.name " +
                "HAVING COUNT(*) > 1 AND SUM(book.cost) > 500" +
                "LIMIT 10;";

        SqlSelectParser parsed = SqlSelectParser.parse(sql);
        assertArrayEquals(new String[] {"author.name", "count(book.id)", "sum(book.cost)"}, parsed.getColumns().toArray());
        assertArrayEquals(new String[] {"book ON (author.id = book.author_id)"}, parsed.getJoins().toArray());
        assertArrayEquals(new String[] {"author.name"}, parsed.getGroupByColumns().toArray());
        assertEquals(10, parsed.getLimit());
        assertArrayEquals(new String[] {"COUNT(*) > 1", "SUM(book.cost) > 500"}, parsed.getHavingClauses().toArray());
    }
}
