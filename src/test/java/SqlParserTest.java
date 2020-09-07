import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class SqlParserTest {


    @Test
    public void testSelect() {
        SqlParser sqlParser = SqlParser.create("  selEct     *  fRom testtable ; ");
        assertEquals("*", sqlParser.getColumns().get(0));

        String[] expected = {"author.name", "count(book.id)", "sum(book.cost)"};
        SqlParser parser = SqlParser.create(" SELECT author.name, count(book.id), sum(book.cost) FRoM author ;");
        assertArrayEquals(expected, parser.getColumns().toArray());

        SqlParser pars = SqlParser.create(" select    author.name    as   name, * from test ;");
        String[] required = new String[] {"author.name as name", "*"} ;

        assertArrayEquals(required, pars.getColumns().toArray());

        SqlParser anotherParser = SqlParser.create(" select  'test string '  ;");
        assertThrows(SqlParserException.class, anotherParser::getColumns);

        SqlParser testParser = SqlParser.create(" 'test string ' from  ;");
        assertThrows(SqlParserException.class, testParser::getColumns);

        SqlParser Parser = SqlParser.create(" select field.name, , * from testtable;");
        assertThrows(SqlParserException.class, Parser::getColumns);
    }


    @Test
    public void testFrom() {
        SqlParser parser = SqlParser.create(" SELECT author.name, count(book.id), sum(book.cost), * FRoM author  (  Select  )");
        parser.getSource();
    }
}
