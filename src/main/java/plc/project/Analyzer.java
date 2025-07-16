package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public final class Analyzer implements Ast.Visitor<Void> {

    public Scope scope;
    private Ast.Function function;

    public Analyzer(Scope parent) {
        scope = new Scope(parent);
        scope.defineFunction("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL);
    }

    public Scope getScope() {
        return scope;
    }

    @Override
    public Void visit(Ast.Source ast) {
        ast.getGlobals().forEach(this::visit);
        ast.getFunctions().forEach(this::visit);
        if (!scope.lookupFunction("main", 0).getReturnType().equals(Environment.Type.INTEGER)) {
            throw new RuntimeException("error");
        }

        return null;
    }

    @Override
    public Void visit(Ast.Global ast) {
        if (ast.getValue().isPresent()) {
            if (ast.getValue().get() instanceof Ast.Expression.PlcList) {
                ((Ast.Expression.PlcList) ast.getValue().get()).setType(Environment.getType(ast.getTypeName()));
            }
            visit(ast.getValue().get());
            requireAssignable(Environment.getType(ast.getTypeName()), ast.getValue().get().getType());
        }
        scope.defineVariable(ast.getName(), ast.getName(), Environment.getType(ast.getTypeName()), ast.getMutable(), Environment.NIL);
        ast.setVariable(scope.lookupVariable(ast.getName()));

        return null;
    }

    @Override
    public Void visit(Ast.Function ast) {
        List<Environment.Type> current = new ArrayList<>();
        for (String index : ast.getParameterTypeNames()) {
            current.add(Environment.getType(index));
        }
        scope.defineFunction(ast.getName(), ast.getName(), current, Environment.getType(ast.getReturnTypeName().orElse("Nil")), args -> Environment.NIL);
        function = ast;
        function.setFunction(scope.lookupFunction(ast.getName(), ast.getParameters().size()));
        scope = new Scope(scope);

        for (int index = 0; index < ast.getParameters().size(); index++) {
            scope.defineVariable(ast.getParameters().get(index), ast.getParameters().get(index), Environment.getType(ast.getParameterTypeNames().get(index)), true, Environment.NIL);
        }
        ast.getStatements().forEach(this::visit);
        scope = scope.getParent();
        function = null;

        return null;
    }

    @Override
    public Void visit(Ast.Statement.Expression ast) {
        Ast.Expression current = ast.getExpression();
        visit(current);
        if (!current.getClass().equals(Ast.Expression.Function.class)) {
            throw new RuntimeException("Receiver must be an access expression");
        }
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Declaration ast) {
        Environment.Type current = null;

        if (ast.getTypeName().isPresent()) {
            current = Environment.getType(ast.getTypeName().get());
        }

        if (ast.getValue().isPresent()) {
            visit(ast.getValue().get());

            if (current == null) {
                current = ast.getValue().get().getType();
            }

            else if (!current.equals(ast.getValue().get().getType())) {
                throw new RuntimeException("ERROR");
            }
        }

        if (current == null) {
            throw new RuntimeException("ERROR");
        }

        Environment.Variable variable = scope.defineVariable(ast.getName(), ast.getName(), current, true, Environment.NIL);
        ast.setVariable(variable);

        return null;
    }

    @Override
    public Void visit(Ast.Statement.Assignment ast) {
        Ast.Expression receiver = ast.getReceiver();
        Ast.Expression value = ast.getValue();
        visit(receiver);
        if (!receiver.getClass().equals(Ast.Expression.Access.class)) {
            throw new RuntimeException("Receiver must be an access expression");
        }
        visit(value);
        requireAssignable(value.getType(), receiver.getType());
        return null;
    }

    @Override
    public Void visit(Ast.Statement.If ast) {
        Ast.Expression condition = ast.getCondition();
        visit(condition);
        requireAssignable(Environment.Type.BOOLEAN, ast.getCondition().getType());
        if (ast.getThenStatements().isEmpty()) {
            throw new RuntimeException("ThenStatements list cannot be empty");
        }
        try {
            scope = new Scope(scope);
            for (Ast.Statement stmt : ast.getThenStatements()) {
                visit(stmt);
            }
        } finally {
            scope = scope.getParent();
        }
        try {
            scope = new Scope(scope);
            for (Ast.Statement stmt : ast.getElseStatements()) {
                visit(stmt);
            }
        } finally {
            scope = scope.getParent();
        }
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Switch ast) {
        Ast.Expression condition = ast.getCondition();
        visit(condition);
        Environment.Type conditionType = condition.getType();
        List<Ast.Statement.Case> cases = ast.getCases();
        for (int i = 0; i < cases.size(); i++) {
            visit(cases.get(i));
            Optional<Ast.Expression> optional = cases.get(i).getValue();
            if (optional.isPresent()) {
                if (i == cases.size() - 1) {
                    throw new RuntimeException("Default case cannot contain a value");
                }
                Ast.Expression expression = optional.get();
                visit(expression);
                requireAssignable(expression.getType(), conditionType);
            }

        }
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Case ast) {
        Optional<Ast.Expression> optional = ast.getValue();
        if (optional.isPresent()) {
            Ast.Expression expression = optional.get();
            visit(expression);
        }
        try {
            scope = new Scope(scope);
            for (Ast.Statement stmt : ast.getStatements()) {
                visit(stmt);
            }
        } finally {
            scope = scope.getParent();
        }
        return null;
    }

    @Override
    public Void visit(Ast.Statement.While ast) {
        visit(ast.getCondition());
        requireAssignable(Environment.Type.BOOLEAN, ast.getCondition().getType());
        try {
            scope = new Scope(scope);
            for (Ast.Statement stmt : ast.getStatements()) {
                visit(stmt);
            }
        } finally {
            scope = scope.getParent();
        }
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Return ast) {
        visit(ast.getValue());

        if (this.function == null) {
            throw new IllegalStateException("ERROR");
        }

        Environment.Type daGoal = Environment.getType(this.function.getReturnTypeName().orElse("Nil"));
        Environment.Type current = ast.getValue().getType();

        if (!daGoal.equals(current)) {
            throw new RuntimeException("ERROR");
        }

        return null;
    }

    @Override
    public Void visit(Ast.Expression.Literal ast) {
        switch (ast.getLiteral()) {
            case BigInteger bigInteger -> {
                if (bigInteger.toByteArray().length > 4) {
                    throw new RuntimeException("error");
                }
                ast.setType(Environment.Type.INTEGER);
            }
            case BigDecimal bigDecimal -> {
                double current = bigDecimal.doubleValue();
                if (current == Double.POSITIVE_INFINITY || current == Double.NEGATIVE_INFINITY) {
                    throw new RuntimeException("error");
                }
                ast.setType(Environment.Type.DECIMAL);
            }
            case Boolean b -> ast.setType(Environment.Type.BOOLEAN);
            case Character c -> ast.setType(Environment.Type.CHARACTER);
            case String s -> ast.setType(Environment.Type.STRING);
            case null, default -> ast.setType(Environment.Type.NIL);
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Group ast) {
        visit(ast.getExpression());
        if (!ast.getExpression().getClass().equals(Ast.Expression.Binary.class)) {
            throw new RuntimeException("A group expression must be a binary expresssion");
        }
        ast.setType(ast.getExpression().getType());
        return null;
    }

    private void helperBinaryFunction(Ast.Expression.Binary ast) {
        if (ast.getRight().getType().equals(Environment.Type.INTEGER) && ast.getLeft().getType().equals(Environment.Type.INTEGER)) {
            ast.setType(Environment.Type.INTEGER);
        }
        else if (ast.getRight().getType().equals(Environment.Type.DECIMAL) && ast.getLeft().getType().equals(Environment.Type.DECIMAL)) {
            ast.setType(Environment.Type.DECIMAL);
        }
        else {
            throw new RuntimeException("error");
        }
    }

    @Override
    public Void visit(Ast.Expression.Binary ast) {
        switch (ast.getOperator()) {
            case "&&":
            case"||": {
                visit(ast.getLeft());
                visit(ast.getRight());
                if (ast.getLeft().getType().equals(Environment.Type.BOOLEAN) && ast.getRight().getType().equals(Environment.Type.BOOLEAN)) {
                    ast.setType(Environment.Type.BOOLEAN);
                }
                else {
                    throw new RuntimeException("error");
                }
                break;
            }

            case "<":
            case ">":
            case "==":
            case "!=": {
                visit(ast.getLeft());
                visit(ast.getRight());
                requireAssignable(Environment.Type.COMPARABLE, ast.getLeft().getType());
                requireAssignable(Environment.Type.COMPARABLE, ast.getRight().getType());
                if (ast.getLeft().getType().equals(ast.getRight().getType())) {
                    ast.setType(Environment.Type.BOOLEAN);
                }
                else {
                    throw new RuntimeException("error");
                }
                break;
            }

            case "-":
            case "*":
            case "/": {
                visit(ast.getLeft());
                visit(ast.getRight());
                helperBinaryFunction(ast);
                break;
            }

            case "+": {
                visit(ast.getLeft());
                visit(ast.getRight());
                if (ast.getLeft().getType().equals(Environment.Type.STRING) || ast.getRight().getType().equals(Environment.Type.STRING)) {
                    ast.setType(Environment.Type.STRING);
                }
                else helperBinaryFunction(ast);
                break;
            }

            case "^": {
                visit(ast.getLeft());
                visit(ast.getRight());
                if ((ast.getLeft().getType().equals(Environment.Type.INTEGER) || ast.getLeft().getType().equals(Environment.Type.DECIMAL)) && ast.getRight().getType().equals(Environment.Type.INTEGER)) {
                    ast.setType(ast.getLeft().getType());
                }
                else {
                    throw new RuntimeException("error");
                }
                break;
            }
        }

        return null;
    }

    @Override
    public Void visit(Ast.Expression.Access ast) {
        String name = ast.getName();
        Optional<Ast.Expression> optional = ast.getOffset();
        if (optional.isPresent()) {
            Ast.Expression expression = optional.get();
            visit(expression);
            if (!expression.getType().equals(Environment.Type.INTEGER)) {
                throw new RuntimeException("Offset value must be an integer");
            }
        }
        Environment.Variable variable = scope.lookupVariable(name);
        ast.setVariable(variable);
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Function ast) {
        String name = ast.getName();
        List<Ast.Expression> listArgs = ast.getArguments();
        Environment.Function function = scope.lookupFunction(name, listArgs.size());
        ast.setFunction(function);

        List<Environment.Type> listParameterTypes = function.getParameterTypes();
        for (int i = 0; i < listParameterTypes.size(); i++) {
            visit(listArgs.get(i));
            requireAssignable(listParameterTypes.get(i), listArgs.get(i).getType());
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expression.PlcList ast) {
        List<Ast.Expression> current = ast.getValues();
        for (Ast.Expression thisOne : current) {
            visit(thisOne);
            requireAssignable(ast.getType(), thisOne.getType());
        }

        return null;
    }

    public static void requireAssignable(Environment.Type target, Environment.Type type) {
        if (target.equals(Environment.Type.ANY)) {        }
        else if (target.equals(Environment.Type.COMPARABLE)) {
            if (!((type.equals(Environment.Type.INTEGER)) || (type.equals(Environment.Type.DECIMAL)) || (type.equals(Environment.Type.CHARACTER)) || (type.equals(Environment.Type.STRING)))) {
                throw new RuntimeException("Target type does not match the type being used/assigned");
            }
        }
        else {
            if (!target.equals(type)) {
                throw new RuntimeException("Target type does not match the type being used/assigned");
            }
        }
    }
}
