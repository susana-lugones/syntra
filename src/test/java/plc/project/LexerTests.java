package plc.project;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class LexerTests {

    @ParameterizedTest
    @MethodSource
    void testIdentifier(String test, String input, boolean success) {
        test(input, Token.Type.IDENTIFIER, success);
    }

    private static Stream<Arguments> testIdentifier() {
        return Stream.of(
                Arguments.of("Alphabetic", "getName", true),
                Arguments.of("Alphanumeric", "thelegend27", true),
                Arguments.of("Leading Hyphen", "-five", false),
                Arguments.of("Leading Digit", "1fish2fish3fishbluefish", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testInteger(String test, String input, boolean success) {
        test(input, Token.Type.INTEGER, success);
    }

    private static Stream<Arguments> testInteger() {
        return Stream.of(
                Arguments.of("Single Digit", "1", true),
                Arguments.of("Multiple Digits", "12345", true),
                Arguments.of("Negative", "-1", true),
                Arguments.of("Leading Zero", "01", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testDecimal(String test, String input, boolean success) {
        test(input, Token.Type.DECIMAL, success);
    }

    private static Stream<Arguments> testDecimal() {
        return Stream.of(
                Arguments.of("Multiple Digits", "123.456", true),
                Arguments.of("Negative Decimal", "-1.0", true),
                Arguments.of("Trailing Decimal", "1.", false),
                Arguments.of("Leading Decimal", ".5", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testCharacter(String test, String input, boolean success) {
        test(input, Token.Type.CHARACTER, success);
    }

    private static Stream<Arguments> testCharacter() {
        return Stream.of(
                Arguments.of("Alphabetic", "\'c\'", true),
                Arguments.of("Newline Escape", "\'\\n\'", true),
                Arguments.of("Empty", "\'\'", false),
                Arguments.of("Multiple", "\'abc\'", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testString(String test, String input, boolean success) {
        test(input, Token.Type.STRING, success);
    }

    private static Stream<Arguments> testString() {
        return Stream.of(
                Arguments.of("Empty", "\"\"", true),
                Arguments.of("Alphabetic", "\"abc\"", true),
                Arguments.of("Newline Escape", "\"Hello,\\nWorld\"", true),
                Arguments.of("Unterminated", "\"unterminated", false),
                Arguments.of("Invalid Escape", "\"invalid\\escape\"", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testOperator(String test, String input, boolean success) {
        test(input, Arrays.asList(new Token(Token.Type.OPERATOR, input, 0)), success);
    }

    private static Stream<Arguments> testOperator() {
        return Stream.of(
                Arguments.of("Character", "(", true),
                Arguments.of("Comparison", "!=", true),
                Arguments.of("Space", " ", false),
                Arguments.of("Tab", "\t", false),
                Arguments.of("Hyphen", "-", true)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testExamples(String test, String input, List<Token> expected) {
        test(input, expected, true);
    }

    private static Stream<Arguments> testExamples() {
        return Stream.of(
                Arguments.of("Example 1", "LET x = 5;", Arrays.asList(
                        new Token(Token.Type.IDENTIFIER, "LET", 0),
                        new Token(Token.Type.IDENTIFIER, "x", 4),
                        new Token(Token.Type.OPERATOR, "=", 6),
                        new Token(Token.Type.INTEGER, "5", 8),
                        new Token(Token.Type.OPERATOR, ";", 9)
                )),
                Arguments.of("Example 2", "print(\"Hello, World!\");", Arrays.asList(
                        new Token(Token.Type.IDENTIFIER, "print", 0),
                        new Token(Token.Type.OPERATOR, "(", 5),
                        new Token(Token.Type.STRING, "\"Hello, World!\"", 6),
                        new Token(Token.Type.OPERATOR, ")", 21),
                        new Token(Token.Type.OPERATOR, ";", 22)
                )),
                Arguments.of("Binary", "x + 1 == y / 2.0 - 3", Arrays.asList(
                        new Token(Token.Type.IDENTIFIER, "x", 0),
                        new Token(Token.Type.OPERATOR, "+", 2),
                        new Token(Token.Type.INTEGER, "1", 4),
                        new Token(Token.Type.OPERATOR, "==", 6),
                        new Token(Token.Type.IDENTIFIER, "y", 9),
                        new Token(Token.Type.OPERATOR, "/", 11),
                        new Token(Token.Type.DECIMAL, "2.0", 13),
                        new Token(Token.Type.OPERATOR, "-", 17),
                        new Token(Token.Type.INTEGER, "3", 19)
                ))
        );
    }

    @ParameterizedTest
    @MethodSource("mixedTokensProvider")
    void testMixedTokens(String description, String input, List<Token> expectedTokens) {
        Lexer lexer = new Lexer(input);
        try {
            List<Token> actualTokens = lexer.lex();
            Assertions.assertEquals(expectedTokens, actualTokens,
                    String.format("Test Failed [%s]: Expected tokens do not match actual tokens.", description));
        } catch (ParseException e) {
            Assertions.fail("Unexpected ParseException for input: " + input);
        }
    }

    private static Stream<Arguments> mixedTokensProvider() {
        return Stream.of(
                Arguments.of(
                        "All Types",
                        "abc 123 456.789 'c' \"string\" %",
                        Arrays.asList(
                                new Token(Token.Type.IDENTIFIER, "abc", 0),
                                new Token(Token.Type.INTEGER, "123", 4),
                                new Token(Token.Type.DECIMAL, "456.789", 8),
                                new Token(Token.Type.CHARACTER, "'c'", 16),
                                new Token(Token.Type.STRING, "\"string\"", 20),
                                new Token(Token.Type.OPERATOR, "%", 29)
                        )
                )
        );
    }


    @Test
    void testException() {
        ParseException exception = Assertions.assertThrows(ParseException.class,
                () -> new Lexer("\"unterminated").lex());
        Assertions.assertEquals(13, exception.getIndex());

    }

    @Test
    void testEmptyCharacterException() {
        ParseException exception = Assertions.assertThrows(ParseException.class,
                () -> new Lexer("''").lex());
        Assertions.assertEquals(1, exception.getIndex());
    }

    @Test
    void testUnterminatedCharacterException() {
        ParseException exception = Assertions.assertThrows(ParseException.class,
                () -> new Lexer("'c").lex());
        Assertions.assertEquals(2, exception.getIndex());
    }

    private static void test(String input, Token.Type expected, boolean success) {
        try {
            if (success) {
                Assertions.assertEquals(new Token(expected, input, 0), new Lexer(input).lexToken());
            } else {
                Assertions.assertNotEquals(new Token(expected, input, 0), new Lexer(input).lexToken());
            }
        } catch (ParseException e) {
            Assertions.assertFalse(success, e.getMessage());
        }
    }

    private static void test(String input, List<Token> expected, boolean success) {
        try {
            if (success) {
                Assertions.assertEquals(expected, new Lexer(input).lex());
            } else {
                Assertions.assertNotEquals(expected, new Lexer(input).lex());
            }
        } catch (ParseException e) {
            Assertions.assertFalse(success, e.getMessage());
        }
    }
}
