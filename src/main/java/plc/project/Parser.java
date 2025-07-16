package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Vector;

public final class Parser {
    private void equalCheck(Object pattern) throws ParseException {
        if (!match(pattern)) {
            try {
                throw new ParseException("error " + pattern.toString(), tokens.get(0).getIndex());
            }
            catch (IndexOutOfBoundsException e) {
                throw new ParseException("error " + pattern, tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
            }
        }
    }
    private void currentC(Object pattern) {
        if (match(",")) {
            if (peek(pattern) || match(",")) {
                throw new ParseException("error", tokens.get(0).getIndex());
            }
        }
        else {
            if (!peek(pattern)) {
                throw new ParseException("error " + pattern.toString(), tokens.get(0).getIndex());
            }
        }
    }

    private final TokenStream tokens;

    public Parser(List<Token> tokens) {
        this.tokens = new TokenStream(tokens);
    }

    public Ast.Source parseSource() throws ParseException {
        List<Ast.Global> first = new ArrayList<>();
        List<Ast.Function> second = new ArrayList<>();

        while (peek("LIST") || peek("VAR") || peek("VAL")) {
            first.add(parseGlobal());
        }

        while (peek("FUN")) {
            second.add(parseFunction());
        }

        if (tokens.has(0)) {
            throw new ParseException("error", tokens.get(0).getIndex());
        }

        return new Ast.Source(first, second);
    }

    public Ast.Global parseGlobal() throws ParseException {
        Ast.Global current;

        if (peek("LIST")) {
            current = parseList();
        }
        else if (peek("VAR")) {
            current = parseMutable();
        }
        else  {
            current = parseImmutable();
        }
        equalCheck(";");

        return current;
    }

    public Ast.Global parseList() throws ParseException {
        Ast.Expression.PlcList count;
        List<Ast.Expression> secondCount = new ArrayList<>();
        String first, second;

        match("LIST");

        equalCheck(Token.Type.IDENTIFIER);
        first = tokens.get(-1).getLiteral();
        equalCheck(":");
        equalCheck(Token.Type.IDENTIFIER);
        second = tokens.get(-1).getLiteral();

        equalCheck("=");
        equalCheck("[");

        do {
            secondCount.add(parseExpression());
            currentC("]");
        }

        while (!match("]"));
        count = new Ast.Expression.PlcList(secondCount);

        return new Ast.Global(first, second, true, Optional.of(count));
    }

    public Ast.Global parseMutable() throws ParseException {
        Optional<Ast.Expression> count = Optional.empty();
        String first, second;

        match("VAR");

        equalCheck(Token.Type.IDENTIFIER);
        first = tokens.get(-1).getLiteral();
        equalCheck(":");
        equalCheck(Token.Type.IDENTIFIER);
        second = tokens.get(-1).getLiteral();

        if (match("=")) {
            count = Optional.of(parseExpression());
        }

        return new Ast.Global(first, second, true, count);
    }

    public Ast.Global parseImmutable() throws ParseException {
        String first, second;
        Optional<Ast.Expression> count;

        match("VAL");

        equalCheck(Token.Type.IDENTIFIER);
        first = tokens.get(-1).getLiteral();
        equalCheck(":");
        equalCheck(Token.Type.IDENTIFIER);
        second = tokens.get(-1).getLiteral();
        equalCheck("=");
        count = Optional.of(parseExpression());
        return new Ast.Global(first, second,false, count);
    }

    public Ast.Function parseFunction() throws ParseException {
        List<String> toIdentify = new ArrayList<>();
        List<String> current = new ArrayList<>();
        Optional<String> count = Optional.of("Any");
        List<Ast.Statement> total;
        String first;

        match("FUN");

        equalCheck(Token.Type.IDENTIFIER);
        first = tokens.get(-1).getLiteral();
        equalCheck("(");

        while (!match(")")) {
            equalCheck(Token.Type.IDENTIFIER);
            toIdentify.add(tokens.get(-1).getLiteral());
            equalCheck(":");
            equalCheck(Token.Type.IDENTIFIER);
            current.add(tokens.get(-1).getLiteral());
            currentC(")");
        }
        if (match(":")) {
            equalCheck(Token.Type.IDENTIFIER);
            count = Optional.of(tokens.get(-1).getLiteral());
        }
        equalCheck("DO");
        total = parseBlock();
        equalCheck("END");

        return new Ast.Function(first, toIdentify, current, count, total);
    }

    public List<Ast.Statement> parseBlock() throws ParseException {
        List<Ast.Statement> current = new ArrayList<>();
        while (!peek("END") && !peek("CASE") && !peek("DEFAULT") && !peek("ELSE")) {
            current.add(parseStatement());
        }

        return current;
    }

    public Ast.Statement parseStatement() throws ParseException {
        if (match("LET")) {
            return parseDeclarationStatement();
        }
        else if (match("WHILE")) {
            return parseWhileStatement();
        }
        else if (match("IF")) {
            return parseIfStatement();
        }
        else if (match("SWITCH")) {
            return parseSwitchStatement();
        }
        else if (match("RETURN")) {
            return parseReturnStatement();
        }
        else {
            Ast.Expression current = parseExpression();
            if (current instanceof Ast.Expression.Access && match("=")) {
                Ast.Expression forRight = parseExpression();
                equalCheck(";");
                return new Ast.Statement.Assignment(current, forRight);
            }
            else  {
                equalCheck(";");
                return new Ast.Statement.Expression(current);
            }
        }
    }

    public Ast.Statement.Declaration parseDeclarationStatement() throws ParseException {
        Optional<Ast.Expression> forRight = Optional.empty();
        Optional<String> second = Optional.empty();
        String first;
        equalCheck(Token.Type.IDENTIFIER);
        first = tokens.get(-1).getLiteral();
        if (match(":")) {
            equalCheck(Token.Type.IDENTIFIER);
            second = Optional.of(tokens.get(-1).getLiteral());
        }
        if (match("=")) {
            forRight = Optional.of(parseExpression());
        }
        equalCheck(";");
        return new Ast.Statement.Declaration(first, second, forRight);
    }

    public Ast.Statement.If parseIfStatement() throws ParseException {
        Ast.Expression count = parseExpression();
        List<Ast.Statement> first;
        List<Ast.Statement> second = new ArrayList<>();
        equalCheck("DO");
        first = parseBlock();
        if (match("ELSE")) {
            second = parseBlock();
        }
        equalCheck("END");

        return new Ast.Statement.If(count, first, second);
    }

    public Ast.Statement.Switch parseSwitchStatement() throws ParseException {
        List<Ast.Statement.Case> first = new ArrayList<>();
        Ast.Expression second = parseExpression();
        while (match("CASE")) {
            first.add(parseCaseStatement());
        }
        equalCheck("DEFAULT");
        first.add(parseCaseStatement());
        equalCheck("END");
        return new Ast.Statement.Switch(second, first);
    }

    public Ast.Statement.Case parseCaseStatement() throws ParseException {
        Optional<Ast.Expression> first = Optional.empty();
        List<Ast.Statement> second;
        if (!tokens.get(-1).getLiteral().equals("DEFAULT")) {
            first = Optional.of(parseExpression());
            equalCheck(":");
        }
        second = parseBlock();
        return new Ast.Statement.Case(first, second);
    }

    public Ast.Statement.While parseWhileStatement() throws ParseException {
        List<Ast.Statement> first;
        Ast.Expression second = parseExpression();
        equalCheck("DO");
        first = parseBlock();
        equalCheck("END");
        return new Ast.Statement.While(second, first);
    }

    public Ast.Statement.Return parseReturnStatement() throws ParseException {
        Ast.Expression count = parseExpression();
        equalCheck(";");
        return new Ast.Statement.Return(count);
    }

    public Ast.Expression parseExpression() throws ParseException {
        return parseLogicalExpression();
    }

    public Ast.Expression parseLogicalExpression() throws ParseException {
        Ast.Expression first, second;
        first = parseComparisonExpression();
        while (match("&&") || match("||")) {
            String current = tokens.get(-1).getLiteral();
            second = parseComparisonExpression();
            first = new Ast.Expression.Binary(current, first, second);
        }
        return first;
    }

    public Ast.Expression parseComparisonExpression() throws ParseException {
        Ast.Expression first, second;
        first = parseAdditiveExpression();
        while (match("==") || match("!=") || match(">") || match("<")) {
            String current = tokens.get(-1).getLiteral();
            second = parseAdditiveExpression();
            first = new Ast.Expression.Binary(current, first, second);
        }
        return first;
    }

    public Ast.Expression parseAdditiveExpression() throws ParseException {
        Ast.Expression first, second;
        first = parseMultiplicativeExpression();
        while (match("-") || match("+")) {
            String current = tokens.get(-1).getLiteral();
            second = parseMultiplicativeExpression();
            first = new Ast.Expression.Binary(current, first, second);
        }
        return first;
    }

    public Ast.Expression parseMultiplicativeExpression() throws ParseException {
        Ast.Expression first, second;
        first = parsePrimaryExpression();
        while (match("^") || match("*") || match("/")) {
            String current = tokens.get(-1).getLiteral();
            second = parsePrimaryExpression();
            first = new Ast.Expression.Binary(current, first, second);
        }
        return first;
    }

    private String shortCutNames(String current) {
        current = current.replace("\\b", "\b");
        current = current.replace("\\n", "\n");
        current = current.replace("\\r", "\r");
        current = current.replace("\\t", "\t");
        current = current.replace("\\\"", "\"");
        current = current.replace("\\'", "'");
        current = current.replace("\\\\", "\\");
        return current;
    }
    public Ast.Expression parsePrimaryExpression() throws ParseException {
        if (match("NIL")) {
            return new Ast.Expression.Literal(null);
        }
        else if (match("TRUE")) {
            return new Ast.Expression.Literal(Boolean.TRUE);
        }
        else if (match("FALSE")) {
            return new Ast.Expression.Literal(Boolean.FALSE);
        }
        else if (match(Token.Type.INTEGER)) {
            return new Ast.Expression.Literal(new BigInteger(tokens.get(-1).getLiteral()));
        }
        else if (match(Token.Type.DECIMAL)) {
            return new Ast.Expression.Literal(new BigDecimal(tokens.get(-1).getLiteral()));
        }
        else if (match(Token.Type.STRING)) {
            String currentS = tokens.get(-1).getLiteral();
            currentS = currentS.substring(1, currentS.length() - 1);
            currentS = shortCutNames(currentS);
            return new Ast.Expression.Literal(currentS);
        }
        else if (match(Token.Type.CHARACTER)) {
            String currentC = tokens.get(-1).getLiteral();
            currentC = currentC.substring(1, currentC.length() - 1);
            currentC = shortCutNames(currentC);
            return new Ast.Expression.Literal(currentC.charAt(0));
        }
        else if (match("(")) {
            Ast.Expression current = parseExpression();
            equalCheck(")");
            return new Ast.Expression.Group(current);
        }
        else if (match(Token.Type.IDENTIFIER)) {
            String currentI = tokens.get(-1).getLiteral();
            if (match("[")) {
                Ast.Expression counter = parseExpression();
                equalCheck("]");
                return new Ast.Expression.Access(Optional.of(counter), currentI);
            }
            else if (match("(")) {
                Vector<Ast.Expression> currentL = new Vector<>();
                while (!match(")")) {
                    currentL.add(parseExpression());
                    currentC(")");
                }
                return new Ast.Expression.Function(currentI, currentL);
            }
            else {
                return new Ast.Expression.Access(Optional.empty(), currentI);
            }
        }
        try {
            throw new ParseException("error", tokens.get(0).getIndex());
        }
        catch (IndexOutOfBoundsException e) {
            throw new ParseException("error", tokens.get(0).getIndex());
        }
    }

    private boolean peek(Object... patterns) {
        for (int i = 0; i < patterns.length; i++) {

            if (!tokens.has(i)) {
                return false;
            }

            else if (patterns[i] instanceof Token.Type) {
                if (patterns[i] != tokens.get(i).getType()) {
                    return false;
                }
            }

            else if (patterns[i] instanceof String) {
                if (!patterns[i].equals(tokens.get(i).getLiteral())) {
                    return false;
                }
            }

            else {
                throw new AssertionError("Invalid pattern object: " + patterns[i].getClass());
            }
        }

        return true;
    }

    private boolean match(Object... patterns) {
        boolean peek = peek(patterns);

        if (peek) {
            for(int i = 0; i < patterns.length; i++){
                tokens.advance();
            }
        }

        return peek;
    }

    private static final class TokenStream {

        private final List<Token> tokens;
        private int index = 0;

        private TokenStream(List<Token> tokens) {
            this.tokens = tokens;
        }

        public boolean has(int offset) {
            return index + offset < tokens.size();
        }

        public Token get(int offset) {
            return tokens.get(index + offset);
        }

        public void advance() {
            index++;
        }
    }
}
