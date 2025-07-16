package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
public class Interpreter implements Ast.Visitor<Environment.PlcObject> {

    private Scope scope = new Scope(null);

    public Interpreter(Scope parent) {
        scope = new Scope(parent);
        scope.defineFunction("print", 1, args -> {
            System.out.println(args.get(0).getValue());
            return Environment.NIL;
        });
    }

    public Scope getScope() {
        return scope;
    }

    @Override
    public Environment.PlcObject visit(Ast.Source ast) {

        for (Ast.Global global : ast.getGlobals()) {
            visit(global);
        }
        for (Ast.Function function : ast.getFunctions()) {
            visit(function);
        }

        Environment.Function current = scope.lookupFunction("main", 0);

        if (current != null) {
            return current.invoke(List.of());
        }
        else {
            throw new RuntimeException("error");
        }
    }

    @Override
    public Environment.PlcObject visit(Ast.Global ast) {

        Environment.PlcObject current = Environment.NIL;

        if (ast.getValue().isPresent()) {
            current = visit(ast.getValue().get());
        }

        scope.defineVariable(ast.getName(), false, current);
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Function ast) {
        Scope first = scope;
        scope.defineFunction(ast.getName(), ast.getParameters().size(), args -> {
            Scope second = scope;
            scope = new Scope(first);
            for (int index = 0; index < ast.getParameters().size(); index++) {
                scope.defineVariable(ast.getParameters().get(index), true, args.get(index));
            }

            try {
                ast.getStatements().forEach(this::visit);
            }

            catch (Return returnValue) {
                return returnValue.value;
            }

            finally {
                scope = second;
            }

            return Environment.NIL;
        });
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Expression ast) {
        visit(ast.getExpression());
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Declaration ast) {

        if (ast.getValue().isPresent()) {
            Environment.PlcObject current = visit(ast.getValue().get());
            scope.defineVariable(ast.getName(), true, current);
        }

        else {
            scope.defineVariable(ast.getName(), true, Environment.NIL);
        }

        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Assignment ast) {

        if (!(ast.getReceiver() instanceof Ast.Expression.Access current)) {
            throw new RuntimeException("error");
        }

        String toChange = current.getName();
        Environment.Variable curVar = scope.lookupVariable(toChange);

        if (!curVar.getMutable()) {
            throw new RuntimeException("error" + toChange);
        }

        Environment.PlcObject toTrack = visit(ast.getValue());

        if (current.getOffset().isPresent()) {
            Object forList = curVar.getValue().getValue();

            if (forList instanceof List) {
                Environment.PlcObject updatedObject = visit(current.getOffset().get());
                List<Object> updatedList = (List<Object>) forList;

                if (updatedObject.getValue() instanceof BigInteger) {
                    int index = ((BigInteger) updatedObject.getValue()).intValueExact();

                    if (index >= updatedList.size() || index < 0) {
                        throw new RuntimeException("error" + index);
                    }
                    updatedList.set(index, toTrack.getValue());

                }

                else {
                    throw new RuntimeException("error" + updatedObject.getValue());
                }
            }

            else {
                throw new RuntimeException("error" + toChange);
            }
        }

        else {
            curVar.setValue(toTrack);
        }

        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.If ast) {
        Environment.PlcObject first = visit(ast.getCondition());
        requireType(Boolean.class, first);
        scope = new Scope(scope);
        if (first.getValue().equals(Boolean.TRUE)) {

            try {
                ast.getThenStatements().forEach(this::visit);
            }

            finally {
                scope = scope.getParent();
            }
        }

        else {

            try {
                ast.getElseStatements().forEach(this::visit);
            }

            finally {
                scope = scope.getParent();
            }
        }

        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Switch ast) {

        Environment.PlcObject current = visit(ast.getCondition());
        boolean forToggle = false;
        Scope ogScope = scope;
        scope = new Scope(scope);

        try {
            for (Ast.Statement.Case forVar : ast.getCases()) {

                if (forVar.getValue().isPresent()) {
                    Environment.PlcObject keepTrack = visit(forVar.getValue().get());
                    if (current.getValue().equals(keepTrack.getValue())) {
                        for (Ast.Statement statement : forVar.getStatements()) {
                            visit(statement);
                        }
                        return Environment.NIL;
                    }
                }

                else {
                    if (!forToggle) {
                        for (Ast.Statement statement : forVar.getStatements()) {
                            visit(statement);
                        }
                        return Environment.NIL;
                    }
                }
            }
        }

        finally {
            scope = ogScope;
        }

        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Case ast) {
        for (Ast.Statement current : ast.getStatements()) {
            visit(current);
        }

        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.While ast) {

        while (requireType(Boolean.class, visit(ast.getCondition()))) {
            for (Ast.Statement current : ast.getStatements()) {
                visit(current);
            }
        }

        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Return ast) {
        throw new Return(visit(ast.getValue()));
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Literal ast) {

        if (ast.getLiteral() == null)
            return Environment.NIL;

        else {
            return Environment.create(ast.getLiteral());
        }
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Group ast) {
        return visit(ast.getExpression());
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Binary ast) {
        String forInitials = ast.getOperator();
        switch (forInitials) {


            case "&&":
            case "||": {
                Environment.PlcObject forLeft = visit(ast.getLeft());
                requireType(Boolean.class, forLeft);

                if (forLeft.getValue().equals(Boolean.FALSE) && forInitials.equals("&&")) {
                    return forLeft;
                }
                if (forLeft.getValue().equals(Boolean.TRUE) && forInitials.equals("||")) {
                    return forLeft;
                }
                Environment.PlcObject forRight = visit(ast.getRight());
                requireType(Boolean.class, forRight);

                if (forInitials.equals("&&")) {
                    return Environment.create(Boolean.logicalAnd((boolean) forLeft.getValue(), (boolean) forRight.getValue()));
                }
                else {
                    return Environment.create(Boolean.logicalOr((boolean) forLeft.getValue(), (boolean) forRight.getValue()));
                }
            }


            case "+": {
                Environment.PlcObject forLeft = visit(ast.getLeft());
                Environment.PlcObject forRight = visit(ast.getRight());
                if (forLeft.getValue() instanceof String || forRight.getValue() instanceof String) {
                    return Environment.create("" + forLeft.getValue() + forRight.getValue());
                }
                else if (forLeft.getValue() instanceof BigInteger) {
                    requireType(BigInteger.class, forRight);
                    return Environment.create(((BigInteger) forLeft.getValue()).add((BigInteger) forRight.getValue()));
                }
                else if (forLeft.getValue() instanceof BigDecimal) {
                    requireType(BigDecimal.class, forRight);
                    return Environment.create(((BigDecimal) forLeft.getValue()).add((BigDecimal) forRight.getValue()));
                }
                else {
                    throw new RuntimeException("error");
                }
            }


            case "<":
            case ">": {
                Environment.PlcObject forLeft = visit(ast.getLeft());
                Environment.PlcObject forRight = visit(ast.getRight());
                requireType(forLeft.getValue().getClass(), forRight);
                Comparable leftSide = requireType(Comparable.class, forLeft);
                Comparable rightSide = requireType(Comparable.class, forRight);
                int compVal;
                compVal = leftSide.compareTo(rightSide);
                if ((compVal < 0 && forInitials.equals("<")) || (compVal > 0 && forInitials.equals(">"))) {
                    return Environment.create(Boolean.TRUE);
                }
                else {
                    return Environment.create(Boolean.FALSE);
                }
            }


            case "==": {
                Environment.PlcObject forLeft = visit(ast.getLeft());
                Environment.PlcObject forRight = visit(ast.getRight());
                return Environment.create(Objects.equals(forLeft.getValue(), forRight.getValue()));
            }


            case "!=": {
                Environment.PlcObject forLeft = visit(ast.getLeft());
                Environment.PlcObject forRight = visit(ast.getRight());
                return Environment.create(!Objects.equals(forLeft.getValue(), forRight.getValue()));
            }


            case "*":
            case "-": {
                Environment.PlcObject forLeft = visit(ast.getLeft());
                Environment.PlcObject forRight = visit(ast.getRight());
                if (forLeft.getValue() instanceof BigDecimal) {
                    requireType(BigDecimal.class, forRight);
                    if (forInitials.equals("*")) {
                        return Environment.create(((BigDecimal) forLeft.getValue()).multiply((BigDecimal) forRight.getValue()));
                    }
                    else {
                        return Environment.create(((BigDecimal) forLeft.getValue()).subtract((BigDecimal) forRight.getValue()));
                    }
                }
                else if (forLeft.getValue() instanceof BigInteger) {
                    requireType(BigInteger.class, forRight);
                    if (forInitials.equals("*")) {
                        return Environment.create(((BigInteger) forLeft.getValue()).multiply((BigInteger) forRight.getValue()));
                    }
                    else {
                        return Environment.create(((BigInteger) forLeft.getValue()).subtract((BigInteger) forRight.getValue()));
                    }
                }
                else {
                    throw new RuntimeException("error");
                }
            }


            case "^": {
                Environment.PlcObject forLeft = visit(ast.getLeft());
                Environment.PlcObject forRight = visit(ast.getRight());
                requireType(BigInteger.class, forRight);
                if (forLeft.getValue() instanceof BigDecimal) {
                    return Environment.create(((BigDecimal) forLeft.getValue()).pow(((BigInteger) forRight.getValue()).intValue(), MathContext.DECIMAL64));
                }
                else if (forLeft.getValue() instanceof BigInteger) {
                    return Environment.create(((BigInteger) forLeft.getValue()).pow(((BigInteger) forRight.getValue()).intValue()));
                }
                else {
                    throw new RuntimeException("error");
                }
            }


            case "/": {
                Environment.PlcObject forLeft = visit(ast.getLeft());
                Environment.PlcObject forRight = visit(ast.getRight());
                if (forRight.getValue().equals(BigDecimal.valueOf(0)) || forRight.getValue().equals(BigInteger.valueOf(0))) {
                    throw new RuntimeException("error");
                }
                if (forLeft.getValue() instanceof BigDecimal) {
                    requireType(BigDecimal.class, forRight);
                    return Environment.create(((BigDecimal) forLeft.getValue()).divide((BigDecimal) forRight.getValue(), RoundingMode.HALF_EVEN));
                }
                else if (forLeft.getValue() instanceof BigInteger) {
                    requireType(BigInteger.class, forRight);
                    return Environment.create(((BigInteger) forLeft.getValue()).divide((BigInteger) forRight.getValue()));
                }
                else {
                    throw new RuntimeException("error");
                }
            }


            default:
                throw new RuntimeException("error");
        }
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Access ast){

        if (ast.getOffset().isPresent()) {
            Environment.Variable current = scope.lookupVariable(ast.getName());
            Object forObj = current.getValue().getValue();

            if (forObj instanceof List<?> list) {
                Environment.PlcObject forVar = visit(ast.getOffset().get());

                if (forVar.getValue() instanceof BigInteger) {
                    int value = ((BigInteger) forVar.getValue()).intValueExact();

                    if (value >= list.size() || value < 0) {
                        throw new RuntimeException("error" + value);
                    }

                    Object currentList = list.get(value);
                    return Environment.create(currentList);
                }
                else {
                    throw new RuntimeException("error" + forVar.getValue());
                }
            }
            else {
                throw new RuntimeException("error" + ast.getName());
            }
        }
        else {
            return scope.lookupVariable(ast.getName()).getValue();
        }

    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Function ast) {

        Environment.Function current = scope.lookupFunction(ast.getName(), ast.getArguments().size());
        List<Environment.PlcObject> evaluatedArgs = ast.getArguments().stream().map(this::visit).collect(Collectors.toList());

        return current.invoke(evaluatedArgs);

    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.PlcList ast) {
        List<Object> current = ast.getValues().stream().map(this::visit).map(Environment.PlcObject::getValue).collect(Collectors.toList());
        return Environment.create(current);
    }

    private static <T> T requireType(Class<T> type, Environment.PlcObject object) {
        if (type.isInstance(object.getValue())) {
            return type.cast(object.getValue());
        } else {
            throw new RuntimeException("Expected type " + type.getName() + ", received " + object.getValue().getClass().getName() + ".");
        }
    }

    private static class Return extends RuntimeException {

        private final Environment.PlcObject value;

        private Return(Environment.PlcObject value) {
            this.value = value;
        }

    }
}
