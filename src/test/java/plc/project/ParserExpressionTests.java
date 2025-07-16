package plc.project;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

final class ParserExpressionTests {
    @Test
    void testingCodes(){
        List<Token> input = Arrays.asList(
                new Token(Token.Type.IDENTIFIER, "VAR", 0),
                new Token(Token.Type.IDENTIFIER, "i", 4),
                new Token(Token.Type.OPERATOR, "=", 6),
                new Token(Token.Type.INTEGER, "-1", 8),
                new Token(Token.Type.OPERATOR, ";", 10),

                new Token(Token.Type.IDENTIFIER, "VAL", 12),
                new Token(Token.Type.IDENTIFIER, "inc", 16),
                new Token(Token.Type.OPERATOR, "=", 20),
                new Token(Token.Type.INTEGER, "2", 22),
                new Token(Token.Type.OPERATOR, ";", 23),

                new Token(Token.Type.IDENTIFIER, "FUN", 25),
                new Token(Token.Type.IDENTIFIER, "foo", 29),
                new Token(Token.Type.OPERATOR, "(", 32),
                new Token(Token.Type.OPERATOR, ")", 33),
                new Token(Token.Type.IDENTIFIER, "DO", 35),

                new Token(Token.Type.IDENTIFIER, "WHILE", 42),
                new Token(Token.Type.IDENTIFIER, "i", 48),
                new Token(Token.Type.OPERATOR, "!=", 50),
                new Token(Token.Type.INTEGER, "1", 53),
                new Token(Token.Type.IDENTIFIER, "DO", 55),

                new Token(Token.Type.IDENTIFIER, "IF", 66),
                new Token(Token.Type.IDENTIFIER, "i", 69),
                new Token(Token.Type.OPERATOR, ">", 71),
                new Token(Token.Type.INTEGER, "0", 73),
                new Token(Token.Type.IDENTIFIER, "DO", 75),

                new Token(Token.Type.IDENTIFIER, "print", 90),
                new Token(Token.Type.OPERATOR, "(", 95),
                new Token(Token.Type.STRING, "\"bar\"", 96),
                new Token(Token.Type.OPERATOR, ")", 101),
                new Token(Token.Type.OPERATOR, ";", 102),

                new Token(Token.Type.IDENTIFIER, "END", 112),

                new Token(Token.Type.IDENTIFIER, "i",124),
                new Token(Token.Type.OPERATOR, "=", 126),
                new Token(Token.Type.IDENTIFIER, "i", 128),
                new Token(Token.Type.OPERATOR, "+", 130),
                new Token(Token.Type.IDENTIFIER, "inc", 132),
                new Token(Token.Type.OPERATOR, ";", 135),

                new Token(Token.Type.IDENTIFIER, "END", 141),

                new Token(Token.Type.IDENTIFIER, "END", 145)
                );

        Ast.Source expected = new Ast.Source(
                Arrays.asList(
                        new Ast.Global("i", true, Optional.of(new Ast.Expression.Literal(BigInteger.valueOf(-1)))),
                        new Ast.Global("inc", false, Optional.of(new Ast.Expression.Literal(BigInteger.valueOf(2))))
                ),

                Arrays.asList(
                        new Ast.Function("foo",Arrays.asList(),
                        Arrays.asList(new Ast.Statement.While(new Ast.Expression.Binary("!=",
                        new Ast.Expression.Access(Optional.empty(), "i"),
                        new Ast.Expression.Literal(BigInteger.ONE)),
                        Arrays.asList(new Ast.Statement.If(new Ast.Expression.Binary(">",
                        new Ast.Expression.Access(Optional.empty(), "i"), new Ast.Expression.Literal(BigInteger.ZERO)),
                        Arrays.asList(new Ast.Statement.Expression(new Ast.Expression.Function("print",
                        Arrays.asList(new Ast.Expression.Literal("bar"))))),Arrays.asList()),new Ast.Statement.Assignment(
                        new Ast.Expression.Access(Optional.empty(),"i"),new Ast.Expression.Binary("+",
                        new Ast.Expression.Access(Optional.empty(), "i"),
                        new Ast.Expression.Access(Optional.empty(), "inc")
        ))))))));
    }

    private static void testParseException(List<Token> tokens, Function<Parser, ?> function) {
        Parser parser = new Parser(tokens);
        Assertions.assertThrows(ParseException.class, () -> function.apply(parser));
    }

    @ParameterizedTest
    @MethodSource
    void testScenarioParseException(String test, List<Token> tokens) {
        testParseException(tokens, Parser::parseExpression);
    }

    private static Stream<Arguments> testScenarioParseException() {
        return Stream.of(
                Arguments.of("Missing Closing Parenthesis",
                        Arrays.asList(
                                new Token(Token.Type.OPERATOR, "(", 0),
                                new Token(Token.Type.IDENTIFIER, "expr", 1)
                        )
                ),

                Arguments.of("Trailing Comma",
                        Arrays.asList(
                                new Token(Token.Type.IDENTIFIER, "name", 0),
                                new Token(Token.Type.OPERATOR, "(", 4),
                                new Token(Token.Type.IDENTIFIER, "expr", 5),
                                new Token(Token.Type.OPERATOR, ",", 9),
                                new Token(Token.Type.OPERATOR, ")", 10)
                        )
                ),

                Arguments.of("Invalid Expression",
                        Arrays.asList(
                                new Token(Token.Type.OPERATOR, "?", 0)
                        )
                ),

                Arguments.of("Invalid Closing Parenthesis",
                        Arrays.asList(
                                new Token(Token.Type.OPERATOR, "(", 0),
                                new Token(Token.Type.IDENTIFIER, "expr", 1),
                                new Token(Token.Type.OPERATOR, "]", 5)
                        )
                ),

                Arguments.of("Integer",
                        Arrays.asList(
                                new Token(Token.Type.OPERATOR, "1", 0)
                        )
                ),

                Arguments.of("Variable",
                        Arrays.asList(
                                new Token(Token.Type.OPERATOR, "expr", 0)
                        )
                )
        );
    }

    private static Stream<Arguments> testExpressionStatement() {
        return Stream.of(
                Arguments.of("Function Expression",
                        Arrays.asList(
                                new Token(Token.Type.IDENTIFIER, "name", 0),
                                new Token(Token.Type.OPERATOR, "(", 4),
                                new Token(Token.Type.OPERATOR, ")", 5),
                                new Token(Token.Type.OPERATOR, ";", 6)
                        ),
                        new Ast.Statement.Expression(new Ast.Expression.Function("name", Arrays.asList()))
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testAssignmentStatement(String test, List<Token> tokens, Ast.Statement.Assignment expected) {
        test(tokens, expected, Parser::parseStatement);
    }

    private static Stream<Arguments> testAssignmentStatement() {
        return Stream.of(
                Arguments.of("Assignment",
                        Arrays.asList(
                                new Token(Token.Type.IDENTIFIER, "name", 0),
                                new Token(Token.Type.OPERATOR, "=", 5),
                                new Token(Token.Type.IDENTIFIER, "value", 7),
                                new Token(Token.Type.OPERATOR, ";", 12)
                        ),
                        new Ast.Statement.Assignment(
                                new Ast.Expression.Access(Optional.empty(), "name"),
                                new Ast.Expression.Access(Optional.empty(), "value")
                        )
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testLiteralExpression(String test, List<Token> tokens, Ast.Expression.Literal expected) {
        test(tokens, expected, Parser::parseExpression);
    }

    private static Stream<Arguments> testLiteralExpression() {
        return Stream.of(
                Arguments.of("Nil Literal",
                        Arrays.asList(new Token(Token.Type.IDENTIFIER, "NIL", 0)),
                        new Ast.Expression.Literal(null)
                ),
                Arguments.of("Boolean Literal",
                        Arrays.asList(new Token(Token.Type.IDENTIFIER, "TRUE", 0)),
                        new Ast.Expression.Literal(Boolean.TRUE)
                ),
                Arguments.of("Integer Literal",
                        Arrays.asList(new Token(Token.Type.INTEGER, "1", 0)),
                        new Ast.Expression.Literal(new BigInteger("1"))
                ),
                Arguments.of("Decimal Literal",
                        Arrays.asList(new Token(Token.Type.DECIMAL, "2.0", 0)),
                        new Ast.Expression.Literal(new BigDecimal("2.0"))
                ),
                Arguments.of("Character Literal",
                        Arrays.asList(new Token(Token.Type.CHARACTER, "'c'", 0)),
                        new Ast.Expression.Literal('c')
                ),
                Arguments.of("String Literal",
                        Arrays.asList(new Token(Token.Type.STRING, "\"string\"", 0)),
                        new Ast.Expression.Literal("string")
                ),
                Arguments.of("Escape Character",
                        Arrays.asList(new Token(Token.Type.STRING, "\"Hello,\\nWorld!\"", 0)),
                        new Ast.Expression.Literal("Hello,\nWorld!")
                ),
                Arguments.of("Char Escape '\\b'",
                        Arrays.asList(new Token(Token.Type.CHARACTER, "'\\b'", 0)),
                        new Ast.Expression.Literal('\b')
                ),
                Arguments.of("Char Escape '\\n'",
                        Arrays.asList(new Token(Token.Type.CHARACTER, "'\\n'", 0)),
                        new Ast.Expression.Literal('\n')
                ),
                Arguments.of("Char Escape '\\r'",
                        Arrays.asList(new Token(Token.Type.CHARACTER, "'\\r'", 0)),
                        new Ast.Expression.Literal('\r')
                ),
                Arguments.of("Char Escape '\\t'",
                        Arrays.asList(new Token(Token.Type.CHARACTER, "'\\t'", 0)),
                        new Ast.Expression.Literal('\t')
                ),
                Arguments.of("Char Escape '\\'\"",
                        Arrays.asList(new Token(Token.Type.CHARACTER, "'\\\"'", 0)),
                        new Ast.Expression.Literal('\"')
                ),
                Arguments.of("Char Escape '\\''",
                        Arrays.asList(new Token(Token.Type.CHARACTER, "'\\''", 0)),
                        new Ast.Expression.Literal('\'')
                ),
                Arguments.of("Char Escape '\\\\'",
                        Arrays.asList(new Token(Token.Type.CHARACTER, "'\\\\'", 0)),
                        new Ast.Expression.Literal('\\')
                ),
                Arguments.of("String Escape \"\\b\"",
                        Arrays.asList(new Token(Token.Type.STRING, "\"\\b\"", 0)),
                        new Ast.Expression.Literal("\b")
                ),
                Arguments.of("String Escape \"\\n\"",
                        Arrays.asList(new Token(Token.Type.STRING, "\"\\n\"", 0)),
                        new Ast.Expression.Literal("\n")
                ),
                Arguments.of("String Escape \"\\r\"",
                        Arrays.asList(new Token(Token.Type.STRING, "\"\\r\"", 0)),
                        new Ast.Expression.Literal("\r")
                ),
                Arguments.of("String Escape \"\\t\"",
                        Arrays.asList(new Token(Token.Type.STRING, "\"\\t\"", 0)),
                        new Ast.Expression.Literal("\t")
                ),
                Arguments.of("String Escape \"\\'\"",
                        Arrays.asList(new Token(Token.Type.STRING, "\"\\'\"", 0)),
                        new Ast.Expression.Literal("\'")
                ),
                Arguments.of("String Escape \"\\\"\"",
                        Arrays.asList(new Token(Token.Type.STRING, "\"\\\"\"", 0)),
                        new Ast.Expression.Literal("\"")
                ),
                Arguments.of("String Escape \"\\\\\"",
                        Arrays.asList(new Token(Token.Type.STRING, "\"\\\\\"", 0)),
                        new Ast.Expression.Literal("\\")
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testGroupExpression(String test, List<Token> tokens, Ast.Expression.Group expected) {
        test(tokens, expected, Parser::parseExpression);
    }

    private static Stream<Arguments> testGroupExpression() {
        return Stream.of(
                Arguments.of("Grouped Variable",
                        Arrays.asList(
                                new Token(Token.Type.OPERATOR, "(", 0),
                                new Token(Token.Type.IDENTIFIER, "expr", 1),
                                new Token(Token.Type.OPERATOR, ")", 5)
                        ),
                        new Ast.Expression.Group(new Ast.Expression.Access(Optional.empty(), "expr"))
                ),
                Arguments.of("Grouped Binary",
                        Arrays.asList(
                                new Token(Token.Type.OPERATOR, "(", 0),
                                new Token(Token.Type.IDENTIFIER, "expr1", 1),
                                new Token(Token.Type.OPERATOR, "+", 7),
                                new Token(Token.Type.IDENTIFIER, "expr2", 9),
                                new Token(Token.Type.OPERATOR, ")", 14)
                        ),
                        new Ast.Expression.Group(new Ast.Expression.Binary("+",
                                new Ast.Expression.Access(Optional.empty(), "expr1"),
                                new Ast.Expression.Access(Optional.empty(), "expr2")
                        ))
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testBinaryExpression(String test, List<Token> tokens, Ast.Expression.Binary expected) {
        test(tokens, expected, Parser::parseExpression);
    }

    private static Stream<Arguments> testBinaryExpression() {
        return Stream.of(
                Arguments.of("Binary And",
                        Arrays.asList(
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.OPERATOR, "&&", 6),
                                new Token(Token.Type.IDENTIFIER, "expr2", 10)
                        ),
                        new Ast.Expression.Binary("&&",
                                new Ast.Expression.Access(Optional.empty(), "expr1"),
                                new Ast.Expression.Access(Optional.empty(), "expr2")
                        )
                ),
                Arguments.of("Binary Or",
                        Arrays.asList(
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.OPERATOR, "||", 6),
                                new Token(Token.Type.IDENTIFIER, "expr2", 10)
                        ),
                        new Ast.Expression.Binary("||",
                                new Ast.Expression.Access(Optional.empty(), "expr1"),
                                new Ast.Expression.Access(Optional.empty(), "expr2")
                        )
                ),

                Arguments.of("Binary Equality",
                        Arrays.asList(
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.OPERATOR, "==", 6),
                                new Token(Token.Type.IDENTIFIER, "expr2", 9)
                        ),
                        new Ast.Expression.Binary("==",
                                new Ast.Expression.Access(Optional.empty(), "expr1"),
                                new Ast.Expression.Access(Optional.empty(), "expr2")
                        )
                ),
                Arguments.of("Binary Less Than",
                        Arrays.asList(
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.OPERATOR, "<", 6),
                                new Token(Token.Type.IDENTIFIER, "expr2", 8)
                        ),
                        new Ast.Expression.Binary("<",
                                new Ast.Expression.Access(Optional.empty(), "expr1"),
                                new Ast.Expression.Access(Optional.empty(), "expr2")
                        )
                ),
                Arguments.of("Binary Greater Than",
                        Arrays.asList(
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.OPERATOR, ">", 6),
                                new Token(Token.Type.IDENTIFIER, "expr2", 8)
                        ),
                        new Ast.Expression.Binary(">",
                                new Ast.Expression.Access(Optional.empty(), "expr1"),
                                new Ast.Expression.Access(Optional.empty(), "expr2")
                        )
                ),
                Arguments.of("Binary Not Equal",
                        Arrays.asList(
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.OPERATOR, "!=", 6),
                                new Token(Token.Type.IDENTIFIER, "expr2", 9)
                        ),
                        new Ast.Expression.Binary("!=",
                                new Ast.Expression.Access(Optional.empty(), "expr1"),
                                new Ast.Expression.Access(Optional.empty(), "expr2")
                        )
                ),

                Arguments.of("Binary Addition",
                        Arrays.asList(
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.OPERATOR, "+", 6),
                                new Token(Token.Type.IDENTIFIER, "expr2", 8)
                        ),
                        new Ast.Expression.Binary("+",
                                new Ast.Expression.Access(Optional.empty(), "expr1"),
                                new Ast.Expression.Access(Optional.empty(), "expr2")
                        )
                ),
                Arguments.of("Binary Subtraction",
                        Arrays.asList(
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.OPERATOR, "-", 6),
                                new Token(Token.Type.IDENTIFIER, "expr2", 8)
                        ),
                        new Ast.Expression.Binary("-",
                                new Ast.Expression.Access(Optional.empty(), "expr1"),
                                new Ast.Expression.Access(Optional.empty(), "expr2")
                        )
                ),

                Arguments.of("Binary Multiplication",
                        Arrays.asList(
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.OPERATOR, "*", 6),
                                new Token(Token.Type.IDENTIFIER, "expr2", 8)
                        ),
                        new Ast.Expression.Binary("*",
                                new Ast.Expression.Access(Optional.empty(), "expr1"),
                                new Ast.Expression.Access(Optional.empty(), "expr2")
                        )
                ),
                Arguments.of("Binary Division",
                        Arrays.asList(
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.OPERATOR, "/", 6),
                                new Token(Token.Type.IDENTIFIER, "expr2", 8)
                        ),
                        new Ast.Expression.Binary("/",
                                new Ast.Expression.Access(Optional.empty(), "expr1"),
                                new Ast.Expression.Access(Optional.empty(), "expr2")
                        )
                ),
                Arguments.of("Binary Exponent",
                        Arrays.asList(
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.OPERATOR, "^", 6),
                                new Token(Token.Type.IDENTIFIER, "expr2", 8)
                        ),
                        new Ast.Expression.Binary("^",
                                new Ast.Expression.Access(Optional.empty(), "expr1"),
                                new Ast.Expression.Access(Optional.empty(), "expr2")
                        )
                ),

                Arguments.of("Triple And",
                        Arrays.asList(
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.OPERATOR, "&&", 6),
                                new Token(Token.Type.IDENTIFIER, "expr2", 10),
                                new Token(Token.Type.OPERATOR, "&&", 16),
                                new Token(Token.Type.IDENTIFIER, "expr3", 20)
                        ),
                        new Ast.Expression.Binary("&&",
                                new Ast.Expression.Binary("&&",
                                        new Ast.Expression.Access(Optional.empty(), "expr1"),
                                        new Ast.Expression.Access(Optional.empty(), "expr2")
                                ),
                                new Ast.Expression.Access(Optional.empty(), "expr3")
                        )
                ),
                Arguments.of("Triple Or",
                        Arrays.asList(
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.OPERATOR, "||", 6),
                                new Token(Token.Type.IDENTIFIER, "expr2", 10),
                                new Token(Token.Type.OPERATOR, "||", 16),
                                new Token(Token.Type.IDENTIFIER, "expr3", 20)
                        ),
                        new Ast.Expression.Binary("||",
                                new Ast.Expression.Binary("||",
                                        new Ast.Expression.Access(Optional.empty(), "expr1"),
                                        new Ast.Expression.Access(Optional.empty(), "expr2")
                                ),
                                new Ast.Expression.Access(Optional.empty(), "expr3")
                        )
                ),

                Arguments.of("Triple Less Than",
                        Arrays.asList(
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.OPERATOR, "<", 6),
                                new Token(Token.Type.IDENTIFIER, "expr2", 8),
                                new Token(Token.Type.OPERATOR, "<", 14),
                                new Token(Token.Type.IDENTIFIER, "expr3", 16)
                        ),
                        new Ast.Expression.Binary("<",
                                new Ast.Expression.Binary("<",
                                        new Ast.Expression.Access(Optional.empty(), "expr1"),
                                        new Ast.Expression.Access(Optional.empty(), "expr2")
                                ),
                                new Ast.Expression.Access(Optional.empty(), "expr3")
                        )
                ),
                Arguments.of("Triple Greater Than",
                        Arrays.asList(
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.OPERATOR, ">", 6),
                                new Token(Token.Type.IDENTIFIER, "expr2", 8),
                                new Token(Token.Type.OPERATOR, ">", 14),
                                new Token(Token.Type.IDENTIFIER, "expr3", 16)
                        ),
                        new Ast.Expression.Binary(">",
                                new Ast.Expression.Binary(">",
                                        new Ast.Expression.Access(Optional.empty(), "expr1"),
                                        new Ast.Expression.Access(Optional.empty(), "expr2")
                                ),
                                new Ast.Expression.Access(Optional.empty(), "expr3")
                        )
                ),

                Arguments.of("Triple Addition",
                        Arrays.asList(
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.OPERATOR, "+", 6),
                                new Token(Token.Type.IDENTIFIER, "expr2", 8),
                                new Token(Token.Type.OPERATOR, "+", 14),
                                new Token(Token.Type.IDENTIFIER, "expr3", 16)
                        ),
                        new Ast.Expression.Binary("+",
                                new Ast.Expression.Binary("+",
                                        new Ast.Expression.Access(Optional.empty(), "expr1"),
                                        new Ast.Expression.Access(Optional.empty(), "expr2")
                                ),
                                new Ast.Expression.Access(Optional.empty(), "expr3")
                        )
                ),
                Arguments.of("Triple Subtraction",
                        Arrays.asList(
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.OPERATOR, "-", 6),
                                new Token(Token.Type.IDENTIFIER, "expr2", 8),
                                new Token(Token.Type.OPERATOR, "-", 14),
                                new Token(Token.Type.IDENTIFIER, "expr3", 16)
                        ),
                        new Ast.Expression.Binary("-",
                                new Ast.Expression.Binary("-",
                                        new Ast.Expression.Access(Optional.empty(), "expr1"),
                                        new Ast.Expression.Access(Optional.empty(), "expr2")
                                ),
                                new Ast.Expression.Access(Optional.empty(), "expr3")
                        )
                ),

                Arguments.of("Triple Multiplication",
                        Arrays.asList(
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.OPERATOR, "*", 6),
                                new Token(Token.Type.IDENTIFIER, "expr2", 8),
                                new Token(Token.Type.OPERATOR, "*", 14),
                                new Token(Token.Type.IDENTIFIER, "expr3", 16)
                        ),
                        new Ast.Expression.Binary("*",
                                new Ast.Expression.Binary("*",
                                        new Ast.Expression.Access(Optional.empty(), "expr1"),
                                        new Ast.Expression.Access(Optional.empty(), "expr2")
                                ),
                                new Ast.Expression.Access(Optional.empty(), "expr3")
                        )
                ),
                Arguments.of("Triple Division",
                        Arrays.asList(
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.OPERATOR, "/", 6),
                                new Token(Token.Type.IDENTIFIER, "expr2", 8),
                                new Token(Token.Type.OPERATOR, "/", 14),
                                new Token(Token.Type.IDENTIFIER, "expr3", 16)
                        ),
                        new Ast.Expression.Binary("/",
                                new Ast.Expression.Binary("/",
                                        new Ast.Expression.Access(Optional.empty(), "expr1"),
                                        new Ast.Expression.Access(Optional.empty(), "expr2")
                                ),
                                new Ast.Expression.Access(Optional.empty(), "expr3")
                        )
                ),
                Arguments.of("Triple Exponent",
                        Arrays.asList(
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.OPERATOR, "^", 6),
                                new Token(Token.Type.IDENTIFIER, "expr2", 8),
                                new Token(Token.Type.OPERATOR, "^", 14),
                                new Token(Token.Type.IDENTIFIER, "expr3", 16)
                        ),
                        new Ast.Expression.Binary("^",
                                new Ast.Expression.Binary("^",
                                        new Ast.Expression.Access(Optional.empty(), "expr1"),
                                        new Ast.Expression.Access(Optional.empty(), "expr2")
                                ),
                                new Ast.Expression.Access(Optional.empty(), "expr3")
                        )
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testAccessExpression(String test, List<Token> tokens, Ast.Expression.Access expected) {
        test(tokens, expected, Parser::parseExpression);
    }

    private static Stream<Arguments> testAccessExpression() {
        return Stream.of(
                Arguments.of("Variable",
                        Arrays.asList(new Token(Token.Type.IDENTIFIER, "name", 0)),
                        new Ast.Expression.Access(Optional.empty(), "name")
                ),
                Arguments.of("List Index Access",
                        Arrays.asList(
                                new Token(Token.Type.IDENTIFIER, "list", 0),
                                new Token(Token.Type.OPERATOR, "[", 4),
                                new Token(Token.Type.IDENTIFIER, "expr", 5),
                                new Token(Token.Type.OPERATOR, "]", 9)
                        ),
                        new Ast.Expression.Access(Optional.of(new Ast.Expression.Access(Optional.empty(), "expr")), "list")
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testFunctionExpression(String test, List<Token> tokens, Ast.Expression.Function expected) {
        test(tokens, expected, Parser::parseExpression);
    }

    private static Stream<Arguments> testFunctionExpression() {
        return Stream.of(
                Arguments.of("Zero Arguments",
                        Arrays.asList(
                                new Token(Token.Type.IDENTIFIER, "name", 0),
                                new Token(Token.Type.OPERATOR, "(", 4),
                                new Token(Token.Type.OPERATOR, ")", 5)
                        ),
                        new Ast.Expression.Function("name", Arrays.asList())
                ),
                Arguments.of("Multiple Arguments",
                        Arrays.asList(
                                new Token(Token.Type.IDENTIFIER, "name", 0),
                                new Token(Token.Type.OPERATOR, "(", 4),
                                new Token(Token.Type.IDENTIFIER, "expr1", 5),
                                new Token(Token.Type.OPERATOR, ",", 10),
                                new Token(Token.Type.IDENTIFIER, "expr2", 12),
                                new Token(Token.Type.OPERATOR, ",", 17),
                                new Token(Token.Type.IDENTIFIER, "expr3", 19),
                                new Token(Token.Type.OPERATOR, ")", 24)
                        ),
                        new Ast.Expression.Function("name", Arrays.asList(
                                new Ast.Expression.Access(Optional.empty(), "expr1"),
                                new Ast.Expression.Access(Optional.empty(), "expr2"),
                                new Ast.Expression.Access(Optional.empty(), "expr3")
                        ))
                )
        );
    }

    private static <T extends Ast> void test(List<Token> tokens, T expected, Function<Parser, T> function) {
        Parser parser = new Parser(tokens);
        if (expected != null) {
            Assertions.assertEquals(expected, function.apply(parser));
        } else {
            Assertions.assertThrows(ParseException.class, () -> function.apply(parser));
        }
    }
}