package plc.project;

import java.util.List;
import java.util.ArrayList;

import static plc.project.Token.Type.*;

public final class Lexer {

    private final CharStream chars;

    public Lexer(String input) {
        chars = new CharStream(input);
    }

    public List<Token> lex() {
        List<Token> tokenList = new ArrayList<>();
        while (chars.has(0)) {
            if (match("[\\s]") || match("[\b\n\t\r]")) {
                chars.skip();
                continue;
            }
            Token newToken = lexToken();
            tokenList.add(newToken);
        }
        return tokenList;
    }

    public Token lexToken() {
        Token newToken;
        if (peek("[A-Za-z@]")) {
            newToken = lexIdentifier();
        }
        else if (peek("(-|[0-9])")) {
            newToken = lexNumber();
        }
        else if (peek("\\'")) {
            newToken = lexCharacter();
        }
        else if (peek("\\\"")) {
            newToken = lexString();
        }
        else {
            newToken = lexOperator();
        }
        return newToken;
    }

    public Token lexIdentifier() {
        Token result;

        if (!match("@") && !match("[A-Za-z]")) {
            throw new ParseException("Error: Must start with a double quote.", chars.index);
        }

        while (peek("[A-Za-z0-9_-]")) {
            chars.advance();
        }

        result = chars.emit(Token.Type.IDENTIFIER);
        return result;
    }

    public Token lexNumber() {
        boolean current = false;
        if (match("0")) {
            current = true;
        }
        else if (match("-")) {
            if (!peek("[0-9]")) {
                return chars.emit(OPERATOR);
            }
            return lexNumber();
        }
        while (!current && match("[0-9]"));
        if (peek("\\.")) {
            if (!chars.has(1) || !String.valueOf(chars.get(1)).matches("[0-9]")) {
                return chars.emit(INTEGER);
            }
            chars.advance();
            while (match("[0-9]"));
            return chars.emit(DECIMAL);
        }
        else {
            return chars.emit(INTEGER);
        }
    }

    public Token lexCharacter() {
        if (peek("\'"))
            match("\'");

        if (peek("\\\\"))
            lexEscape();
        else if (peek("[^\'\\n\\r\\\\]"))
            match("[^\'\\n\\r\\\\]");
        else {
            throw new ParseException("ERROR", chars.index);
        }
        if (peek("\'")) {
            match("\'");
            return chars.emit(Token.Type.CHARACTER);
        }
        throw new ParseException("ERROR", chars.index);
    }

    public Token lexString() {
        Token result;

        if (!match("\"")) {
            throw new ParseException("Error: String must start with a double quote.", chars.index);
        }

        while (!peek("\"") && chars.has(0)) {

            if (peek("\r") || peek("\n")) {
                throw new ParseException("Error: Opening and closing quotes must be on the same line.", chars.index);
            }

            else if (peek("\\\\")) {
                lexEscape();
            }

            else {
                match("[^\"]");
            }
        }

        if (!match("\"")) {
            throw new ParseException("Error: String must end with a double quote.", chars.index);
        }

        result = chars.emit(Token.Type.STRING);
        return result;
    }

    public void lexEscape() {
        chars.advance();

        if (peek("[bnrt'\"\\\\]")) {
            chars.advance();
        }

        else {
            throw new ParseException("Error: Invalid escape.", chars.index);
        }
    }

    public Token lexOperator() {
        if (match("!")) {
            match("=");
        }
        else if (match("&")) {
            match("&");
        }
        else if (match("|")) {
            match("|");
        }
        else if (match("=")) {
            match("=");
        }
        else {
            chars.advance();
        }
        return chars.emit(OPERATOR);
    }

    public boolean peek(String... patterns) {

        for (int i = 0; i < patterns.length; i++){

            if ( !chars.has(i) || !String.valueOf(chars.get(i)).matches(patterns[i]) ) {
                return false;
            }
        }
        return true;
    }

    public boolean match(String... patterns) {

        boolean peek = peek(patterns);

        if (peek) {
            for (int i = 0; i < patterns.length; i++){
                chars.advance();
            }
        }
        return peek;
    }

    public static final class CharStream {

        private final String input;
        private int index = 0;
        private int length = 0;

        public CharStream(String input) {
            this.input = input;
        }

        public boolean has(int offset) {
            return index + offset < input.length();
        }

        public char get(int offset) {
            return input.charAt(index + offset);
        }

        public void advance() {
            index++;
            length++;
        }

        public void skip() {
            length = 0;
        }

        public Token emit(Token.Type type) {
            int start = index - length;
            skip();
            return new Token(type, input.substring(start, index), start);
        }

    }
}