package plc.project;

import java.io.PrintWriter;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public final class Generator implements Ast.Visitor<Void> {

    private final PrintWriter writer;
    private int indent = 0;

    public Generator(PrintWriter writer) {
        this.writer = writer;
    }

    private void print(Object... objects) {
        for (Object object : objects) {
            if (object instanceof Ast) {
                visit((Ast) object);
            } else {
                writer.write(object.toString());
            }
        }
    }

    private void newline(int indent) {
        writer.println();
        for (int i = 0; i < indent; i++) {
            writer.write("    ");
        }
    }

    @Override
    public Void visit(Ast.Source ast) {
        print("public class Main {");

        if (!ast.getGlobals().isEmpty()) {
            newline(0);
        }
        indent++;

        for (int index = 0; index < ast.getGlobals().size(); ++index) {
            newline(indent);
            visit(ast.getGlobals().get(index));
        }
        newline(0);

        newline(indent);
        print("public static void main(String[] args) {");
        indent++;

        newline(indent);
        print("System.exit(new Main().main());");
        indent--;

        newline(indent);
        print("}");

        for (int index = 0; index < ast.getFunctions().size(); ++index) {
            newline(0);
            newline(indent);
            visit(ast.getFunctions().get(index));
        }
        newline(0);

        indent--;
        newline(indent);
        print("}");

        return null;
    }

    @Override
    public Void visit(Ast.Global ast) {
        if (!ast.getMutable()) {
            print("final ");
        }

        print(ast.getVariable().getType().getJvmName());

        if (ast.getValue().isPresent() && ast.getValue().get() instanceof Ast.Expression.PlcList) {
            print("[]");
        }
        print(" " + ast.getName());

        if (ast.getValue().isPresent()) {
            print(" = ");
            visit(ast.getValue().get());
        }
        print(";");

        return null;
    }

    @Override
    public Void visit(Ast.Function ast) {
        print(ast.getFunction().getReturnType().getJvmName() + " " + ast.getName() + "(");

        for (int index = 0; index < ast.getParameters().size(); ++index) {
            print(Environment.getType(ast.getParameterTypeNames().get(index)).getJvmName() + " ");
            print(ast.getParameters().get(index));

            if (index != ast.getParameters().size() - 1) {
                print(", ");
            }
        }

        print(") {");
        if (ast.getStatements().isEmpty()) {
            print("}");
        }
        else {
            indent++;
            for (int index = 0; index < ast.getStatements().size(); ++index) {
                newline(indent);
                visit(ast.getStatements().get(index));
            }

            indent--;
            newline(indent);
            print("}");
        }

        return null;
    }

    @Override
    public Void visit(Ast.Statement.Expression ast) {
        visit(ast.getExpression());
        print(";");

        return null;
    }

    @Override
    public Void visit(Ast.Statement.Declaration ast) {
        print(ast.getVariable().getType().getJvmName() + " " + ast.getName());

        if (ast.getValue().isPresent()) {
            print(" = ");
            visit(ast.getValue().get());
        }

        print(";");

        return null;
    }

    @Override
    public Void visit(Ast.Statement.Assignment ast) {
        visit(ast.getReceiver());
        print(" = ");
        visit(ast.getValue());
        print(";");

        return null;
    }

    @Override
    public Void visit(Ast.Statement.If ast) {
        print("if (");
        visit(ast.getCondition());
        print(") {");
        indent++;

        for (Ast.Statement current : ast.getThenStatements()) {
            newline(indent);
            visit(current);
        }

        indent--;
        newline(indent);
        print("}");

        if (!ast.getElseStatements().isEmpty()) {
            print(" else {");
            indent++;

            for (Ast.Statement current : ast.getElseStatements()) {
                newline(indent);
                visit(current);
            }

            indent--;
            newline(indent);
            print("}");
        }

        return null;
    }

    @Override
    public Void visit(Ast.Statement.Switch ast) {
        print("switch (");
        visit(ast.getCondition());
        print(") {");
        indent++;

        for (int index = 0; index < ast.getCases().size(); ++index) {
            newline(indent);
            visit(ast.getCases().get(index));

            if (index < ast.getCases().size() - 1) {
                newline(indent);
                print("    break;");
            }
        }

        indent--;
        newline(indent);
        print("}");

        return null;
    }

    @Override
    public Void visit(Ast.Statement.Case ast) {
        if (ast.getValue().isPresent()) {
            print("case ");
            visit(ast.getValue().get());
            print(":");
        }

        else {
            print("default:");
        }

        indent++;

        for (int index = 0; index < ast.getStatements().size(); ++index) {
            newline(indent);
            visit(ast.getStatements().get(index));
        }

        indent--;

        return null;
    }

    @Override
    public Void visit(Ast.Statement.While ast) {
        print("while (");
        visit(ast.getCondition());
        print(") {");
        if (ast.getStatements().size() != 0) {
            indent++;
            for (int index = 0; index < ast.getStatements().size(); index++) {
                newline(indent);
                visit(ast.getStatements().get(index));
            }
            indent--;
            newline(indent);
        }
        print("}");

        return null;
    }

    @Override
    public Void visit(Ast.Statement.Return ast) {
        writer.write("return ");
        visit(ast.getValue());
        writer.write(";");
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Literal ast) {
        if (ast.getLiteral() instanceof String) {
            print("\"" + ast.getLiteral() + "\"");
        }
        else if (ast.getLiteral() instanceof Character) {
            print("'" + ast.getLiteral() + "'");
        }
        else {
            print(ast.getLiteral().toString());
        }

        return null;
    }

    @Override
    public Void visit(Ast.Expression.Group ast) {
        writer.write("(");
        visit(ast.getExpression());
        writer.write(")");
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Binary ast) {
        if (ast.getOperator().equals("^")) {
            print("Math.pow(");
            visit(ast.getLeft());
            print(", ");
            visit(ast.getRight());
            print(")");
        }
        else {
            visit(ast.getLeft());
            print(" " + ast.getOperator() + " ");
            visit(ast.getRight());
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Access ast) {
        print(ast.getVariable().getJvmName());
        Optional<Ast.Expression> current = ast.getOffset();
        if (current.isPresent()) {
            Ast.Expression expression = current.get();
            writer.write("[");
            visit(expression);
            writer.write("]");
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Function ast) {
        print(ast.getFunction().getJvmName() + "(");
        for (int index = 0; index < ast.getArguments().size(); index++) {
            visit(ast.getArguments().get(index));
            if (index != ast.getArguments().size() - 1) {
                print(", ");
            }
        }
        print(")");

        return null;
    }

    @Override
    public Void visit(Ast.Expression.PlcList ast) {
        List<Ast.Expression> current = ast.getValues();
        writer.write("{");
        for (int i = 0; i < current.size() - 1; i++) {
            visit(current.get(i));
            writer.write(", ");
        }
        visit(current.get(current.size() - 1));
        writer.write("}");
        return null;
    }
}