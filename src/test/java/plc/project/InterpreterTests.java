package plc.project;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

final class InterpreterTests {

    @ParameterizedTest
    @MethodSource
    void testSource(String test, Ast.Source ast, Object expected) {
        test(ast, expected, new Scope(null));
    }

    private static Stream<Arguments> testSource() {
        return Stream.of(
                Arguments.of("Main", new Ast.Source(
                        Arrays.asList(),
                        Arrays.asList(new Ast.Function("main", Arrays.asList(), Arrays.asList(
                                new Ast.Statement.Return(new Ast.Expression.Literal(BigInteger.ZERO)))
                        ))
                ), BigInteger.ZERO),
                Arguments.of("Globals & No Return", new Ast.Source(
                        Arrays.asList(
                                new Ast.Global("x", true, Optional.of(new Ast.Expression.Literal(BigInteger.ONE))),
                                new Ast.Global("y", true, Optional.of(new Ast.Expression.Literal(BigInteger.TEN)))
                        ),
                        Arrays.asList(new Ast.Function("main", Arrays.asList(), Arrays.asList(
                                new Ast.Statement.Expression(new Ast.Expression.Binary("+",
                                        new Ast.Expression.Access(Optional.empty(), "x"),
                                        new Ast.Expression.Access(Optional.empty(), "y")                                ))
                        )))
                ), Environment.NIL.getValue())
        );
    }

    @ParameterizedTest
    @MethodSource
    void testGlobal(String test, Ast.Global ast, Object expected) {
        Scope scope = test(ast, Environment.NIL.getValue(), new Scope(null));
        Assertions.assertEquals(expected, scope.lookupVariable(ast.getName()).getValue().getValue());
    }

    private static Stream<Arguments> testGlobal() {
        return Stream.of(
                Arguments.of("Mutable", new Ast.Global("name", true, Optional.empty()), Environment.NIL.getValue()),
                Arguments.of("Immutable", new Ast.Global("name", false, Optional.of(new Ast.Expression.Literal(BigInteger.ONE))), BigInteger.ONE)
        );
    }

    @Test
    void testList() {
        List<Object> expected = Arrays.asList(BigInteger.ONE, BigInteger.valueOf(5), BigInteger.TEN);

        List<Ast.Expression> values = Arrays.asList(new Ast.Expression.Literal(BigInteger.ONE),
                new Ast.Expression.Literal(BigInteger.valueOf(5)),
                new Ast.Expression.Literal(BigInteger.TEN));

        Optional<Ast.Expression> value = Optional.of(new Ast.Expression.PlcList(values));
        Ast.Global ast = new Ast.Global("list", true, value);

        Scope scope = test(ast, Environment.NIL.getValue(), new Scope(null));
        Assertions.assertEquals(expected, scope.lookupVariable(ast.getName()).getValue().getValue());
    }

    @ParameterizedTest
    @MethodSource
    void testFunction(String test, Ast.Function ast, List<Environment.PlcObject> args, Object expected) {
        Scope scope = test(ast, Environment.NIL.getValue(), new Scope(null));
        Assertions.assertEquals(expected, scope.lookupFunction(ast.getName(), args.size()).invoke(args).getValue());
    }

    private static Stream<Arguments> testFunction() {
        return Stream.of(
                Arguments.of("Main",
                        new Ast.Function("main", Arrays.asList(), Arrays.asList(
                                new Ast.Statement.Return(new Ast.Expression.Literal(BigInteger.ZERO)))
                        ),
                        Arrays.asList(),
                        BigInteger.ZERO
                ),
                Arguments.of("Arguments",
                        new Ast.Function("main", Arrays.asList("x"), Arrays.asList(
                                new Ast.Statement.Return(new Ast.Expression.Binary("*",
                                        new Ast.Expression.Access(Optional.empty(), "x"),
                                        new Ast.Expression.Access(Optional.empty(), "x")
                                ))
                        )),
                        Arrays.asList(Environment.create(BigInteger.TEN)),
                        BigInteger.valueOf(100)
                )
        );
    }

    @Test
    void testExpressionStatement() {
        PrintStream sysout = System.out;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        System.setOut(new PrintStream(out));
        try {
            test(new Ast.Statement.Expression(
                    new Ast.Expression.Function("print", Arrays.asList(new Ast.Expression.Literal("Hello, World!")))
            ), Environment.NIL.getValue(), new Scope(null));
            Assertions.assertEquals("Hello, World!" + System.lineSeparator(), out.toString());
        } finally {
            System.setOut(sysout);
        }
    }

    @ParameterizedTest
    @MethodSource
    void testDeclarationStatement(String test, Ast.Statement.Declaration ast, Object expected) {
        Scope scope = test(ast, Environment.NIL.getValue(), new Scope(null));
        Assertions.assertEquals(expected, scope.lookupVariable(ast.getName()).getValue().getValue());
    }

    private static Stream<Arguments> testDeclarationStatement() {
        return Stream.of(
                Arguments.of("Declaration",
                        new Ast.Statement.Declaration("name", Optional.empty()),
                        Environment.NIL.getValue()
                ),
                Arguments.of("Initialization",
                        new Ast.Statement.Declaration("name", Optional.of(new Ast.Expression.Literal(BigInteger.ONE))),
                        BigInteger.ONE
                )
        );
    }

    @Test
    void testVariableAssignmentStatement() {
        Scope scope = new Scope(null);
        scope.defineVariable("variable", true, Environment.create("variable"));
        test(new Ast.Statement.Assignment(
                new Ast.Expression.Access(Optional.empty(),"variable"),
                new Ast.Expression.Literal(BigInteger.ONE)
        ), Environment.NIL.getValue(), scope);
        Assertions.assertEquals(BigInteger.ONE, scope.lookupVariable("variable").getValue().getValue());
    }

    @Test
    void testListAssignmentStatement() {
        List<Object> expected = Arrays.asList(BigInteger.ONE, BigInteger.valueOf(5), BigInteger.valueOf(3));
        List<Object> list = Arrays.asList(BigInteger.ONE, BigInteger.valueOf(5), BigInteger.TEN);

        Scope scope = new Scope(null);
        scope.defineVariable("list", true, Environment.create(list));
        test(new Ast.Statement.Assignment(
                new Ast.Expression.Access(Optional.of(new Ast.Expression.Literal(BigInteger.valueOf(2))), "list"),
                new Ast.Expression.Literal(BigInteger.valueOf(3))
        ), Environment.NIL.getValue(), scope);

        Assertions.assertEquals(expected, scope.lookupVariable("list").getValue().getValue());
    }

    @ParameterizedTest
    @MethodSource
    void testIfStatement(String test, Ast.Statement.If ast, Object expected) {
        Scope scope = new Scope(null);
        scope.defineVariable("num", true, Environment.NIL);
        test(ast, Environment.NIL.getValue(), scope);
        Assertions.assertEquals(expected, scope.lookupVariable("num").getValue().getValue());
    }

    private static Stream<Arguments> testIfStatement() {
        return Stream.of(
                Arguments.of("True Condition",
                        new Ast.Statement.If(
                                new Ast.Expression.Literal(true),
                                Arrays.asList(new Ast.Statement.Assignment(new Ast.Expression.Access(Optional.empty(),"num"), new Ast.Expression.Literal(BigInteger.ONE))),
                                Arrays.asList()
                        ),
                        BigInteger.ONE
                ),
                Arguments.of("False Condition",
                        new Ast.Statement.If(
                                new Ast.Expression.Literal(false),
                                Arrays.asList(),
                                Arrays.asList(new Ast.Statement.Assignment(new Ast.Expression.Access(Optional.empty(),"num"), new Ast.Expression.Literal(BigInteger.TEN)))
                        ),
                        BigInteger.TEN
                )
        );
    }

    @Test
    void testSwitchStatement() {
        Scope scope = new Scope(null);
        scope.defineVariable("letter", true, Environment.create(new Character('y')));

        List<Ast.Statement> statements = Arrays.asList(
                new Ast.Statement.Expression(new Ast.Expression.Function("print", Arrays.asList(new Ast.Expression.Literal("yes")))),
                new Ast.Statement.Assignment(new Ast.Expression.Access(Optional.empty(), "letter"),
                        new Ast.Expression.Literal(new Character('n')))
        );

        List<Ast.Statement.Case> cases = Arrays.asList(
                new Ast.Statement.Case(Optional.of(new Ast.Expression.Literal(new Character('y'))), statements),
                new Ast.Statement.Case(Optional.empty(), Arrays.asList(new Ast.Statement.Expression(new Ast.Expression.Function("print", Arrays.asList(new Ast.Expression.Literal("no"))))))
        );

        Ast.Statement.Switch ast = new Ast.Statement.Switch(new Ast.Expression.Access(Optional.empty(), "letter"), cases);

        PrintStream sysout = System.out;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        System.setOut(new PrintStream(out));
        try {
            test(ast, Environment.NIL.getValue(), scope);
            Assertions.assertEquals("yes" + System.lineSeparator(), out.toString());
        } finally {
            System.setOut(sysout);
        }

        Assertions.assertEquals(new Character('n'), scope.lookupVariable("letter").getValue().getValue());
    }

    @ParameterizedTest
    @MethodSource
    void testLiteralExpression(String test, Ast ast, Object expected) {
        test(ast, expected, new Scope(null));
    }

    private static Stream<Arguments> testLiteralExpression() {
        return Stream.of(
                Arguments.of("Nil", new Ast.Expression.Literal(null), Environment.NIL.getValue()),
                Arguments.of("Boolean", new Ast.Expression.Literal(true), true),
                Arguments.of("Integer", new Ast.Expression.Literal(BigInteger.ONE), BigInteger.ONE),
                Arguments.of("Decimal", new Ast.Expression.Literal(BigDecimal.ONE), BigDecimal.ONE),
                Arguments.of("Character", new Ast.Expression.Literal('c'), 'c'),
                Arguments.of("String", new Ast.Expression.Literal("string"), "string")
        );
    }

    @ParameterizedTest
    @MethodSource
    void testGroupExpression(String test, Ast ast, Object expected) {
        test(ast, expected, new Scope(null));
    }

    private static Stream<Arguments> testGroupExpression() {
        return Stream.of(
                Arguments.of("Literal", new Ast.Expression.Group(new Ast.Expression.Literal(BigInteger.ONE)), BigInteger.ONE),
                Arguments.of("Binary",
                        new Ast.Expression.Group(new Ast.Expression.Binary("+",
                                new Ast.Expression.Literal(BigInteger.ONE),
                                new Ast.Expression.Literal(BigInteger.TEN)
                        )),
                        BigInteger.valueOf(11)
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testBinaryExpression(String test, Ast ast, Object expected) {
        test(ast, expected, new Scope(null));
    }

    private static Stream<Arguments> testBinaryExpression() {
        return Stream.of(
                Arguments.of("And",
                        new Ast.Expression.Binary("&&",
                                new Ast.Expression.Literal(true),
                                new Ast.Expression.Literal(false)
                        ),
                        false
                ),
                Arguments.of("Or (Short Circuit)",
                        new Ast.Expression.Binary("||",
                                new Ast.Expression.Literal(true),
                                new Ast.Expression.Access(Optional.empty(), "undefined")
                        ),
                        true
                ),
                Arguments.of("Less Than",
                        new Ast.Expression.Binary("<",
                                new Ast.Expression.Literal(BigInteger.ONE),
                                new Ast.Expression.Literal(BigInteger.TEN)
                        ),
                        true
                ),
                Arguments.of("Equal",
                        new Ast.Expression.Binary("==",
                                new Ast.Expression.Literal(BigInteger.ONE),
                                new Ast.Expression.Literal(BigInteger.TEN)
                        ),
                        false
                ),
                Arguments.of("Concatenation",
                        new Ast.Expression.Binary("+",
                                new Ast.Expression.Literal("a"),
                                new Ast.Expression.Literal("b")
                        ),
                        "ab"
                ),
                Arguments.of("Addition",
                        new Ast.Expression.Binary("+",
                                new Ast.Expression.Literal(BigInteger.ONE),
                                new Ast.Expression.Literal(BigInteger.TEN)
                        ),
                        BigInteger.valueOf(11)
                ),
                Arguments.of("Division",
                        new Ast.Expression.Binary("/",
                                new Ast.Expression.Literal(new BigDecimal("1.2")),
                                new Ast.Expression.Literal(new BigDecimal("3.4"))
                        ),
                        new BigDecimal("0.4")
                ),
                Arguments.of("BigInteger LHS",
                        new Ast.Expression.Binary("^",
                                new Ast.Expression.Literal(BigInteger.valueOf(8)),
                                new Ast.Expression.Literal(BigInteger.valueOf(3))
                        ),
                        BigInteger.valueOf(512)
                ),
                Arguments.of("Huge LHS",
                        new Ast.Expression.Binary("^",
                                new Ast.Expression.Literal(new BigInteger("2147483648")),
                                new Ast.Expression.Literal(BigInteger.TEN)
                        ),
                        new BigInteger("2085924839766513752338888384931203236916703635113918720651407820138886450957656787131798913024")
                ),
                Arguments.of("Huge RHS",
                        new Ast.Expression.Binary("^",
                                new Ast.Expression.Literal(BigInteger.TWO),
                                new Ast.Expression.Literal(BigInteger.valueOf(1000000))
                        ),
                        new BigInteger("2").pow(1000000)
                ),
                Arguments.of("Negative LHS",
                        new Ast.Expression.Binary("^",
                                new Ast.Expression.Literal(BigInteger.valueOf(-8)),
                                new Ast.Expression.Literal(BigInteger.valueOf(3))
                        ),
                        BigInteger.valueOf(-512)
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testAccessExpression(String test, Ast ast, Object expected) {
        Scope scope = new Scope(null);
        scope.defineVariable("variable", true, Environment.create("variable"));
        test(ast, expected, scope);
    }

    private static Stream<Arguments> testAccessExpression() {
        return Stream.of(
                Arguments.of("Variable",
                        new Ast.Expression.Access(Optional.empty(), "variable"),
                        "variable"
                )
        );
    }

    @Test
    void testListAccessExpression() {
        List<Object> list = Arrays.asList(BigInteger.ONE, BigInteger.valueOf(5), BigInteger.TEN);

        Scope scope = new Scope(null);
        scope.defineVariable("list", true, Environment.create(list));
        test(new Ast.Expression.Access(Optional.of(new Ast.Expression.Literal(BigInteger.valueOf(1))), "list"), BigInteger.valueOf(5), scope);
    }

    @ParameterizedTest
    @MethodSource
    void testFunctionExpression(String test, Ast ast, Object expected) {
        Scope scope = new Scope(null);
        scope.defineFunction("function", 0, args -> Environment.create("function"));
        test(ast, expected, scope);
    }

    private static Stream<Arguments> testFunctionExpression() {
        return Stream.of(
                Arguments.of("Function",
                        new Ast.Expression.Function("function", Arrays.asList()),
                        "function"
                ),
                Arguments.of("Print",
                        new Ast.Expression.Function("print", Arrays.asList(new Ast.Expression.Literal("Hello, World!"))),
                        Environment.NIL.getValue()
                )
        );
    }

    @Test
    void testPlcList() {
        List<Object> expected = Arrays.asList(BigInteger.ONE, BigInteger.valueOf(5), BigInteger.TEN);

        List<Ast.Expression> values = Arrays.asList(new Ast.Expression.Literal(BigInteger.ONE),
                new Ast.Expression.Literal(BigInteger.valueOf(5)),
                new Ast.Expression.Literal(BigInteger.TEN));

        Ast ast = new Ast.Expression.PlcList(values);

        test(ast, expected, new Scope(null));
    }

    private static Scope test(Ast ast, Object expected, Scope scope) {
        Interpreter interpreter = new Interpreter(scope);
        if (expected != null) {
            Assertions.assertEquals(expected, interpreter.visit(ast).getValue());
        } else {
            Assertions.assertThrows(RuntimeException.class, () -> interpreter.visit(ast));
        }
        return interpreter.getScope();
    }
}
