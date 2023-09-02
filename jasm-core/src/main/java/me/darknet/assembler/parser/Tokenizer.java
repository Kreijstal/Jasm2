package me.darknet.assembler.parser;

import me.darknet.assembler.util.Location;
import me.darknet.assembler.util.Range;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class Tokenizer {

    public static boolean isOperator(char c) {
        return c == '{' || c == '}' || c == ':' || c == ',';
    }

    public static boolean isNumber(char c) {
        return c >= '0' && c <= '9';
    }

    public static boolean isHex(char c) {
        return isNumber(c) || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
    }

    public static boolean isNumberSuffix(char c) {
        return c == 'f' || c == 'F' || c == 'l' || c == 'L' || c == 'd' || c == 'D';
    }

    public static boolean isExponent(char c) {
        return c == 'E' || c == 'e';
    }

    public static boolean isNumberContinuation(char c) {
        return c == '.' || isExponent(c);
    }

    private void handleComment(TokenizerContext ctx, char currentChar) {
        if (currentChar == '\n') {
            ctx.collectToken();
            ctx.enterComment();
            ctx.nextLine();
            ctx.next();
        }
    }

    private void handleString(TokenizerContext ctx, char currentChar) {
        switch (currentChar) {
            case '"' -> {
                ctx.collectToken();
                ctx.leaveString();
                ctx.next();
            }
            case '\\' -> {
                ctx.next();
                ctx.processEscape();
            }
            default -> ctx.forward();
        }
    }

    private void handleWhitespace(TokenizerContext ctx, char currentChar) {
        // always collect the token
        ctx.collectToken();
        if (currentChar == '\n') {
            ctx.nextLine();
        }
        ctx.next();
    }

    private void handleNormal(TokenizerContext ctx, char currentChar) {
        if (currentChar == '/' && ctx.peek() == '/') {
            ctx.next();
            ctx.next();
            ctx.enterComment();
        } else if (currentChar == '"') {
            ctx.next();
            ctx.enterString();
        } else if (isOperator(currentChar)) {
            ctx.collectToken();
            ctx.forward();
            ctx.collectToken();
        } else {
            ctx.forward();
        }
    }

    public List<Token> tokenize(String source, String input) {
        TokenizerContext ctx = new TokenizerContext();
        ctx.input = input;
        ctx.buffer = new StringBuffer();
        ctx.source = source;
        int length = input.length();
        while (ctx.index < length) {
            char c = input.charAt(ctx.index);
            if (ctx.isComment()) {
                handleComment(ctx, c);
            }
            if (ctx.isString()) {
                handleString(ctx, c);
            } else if (Character.isWhitespace(c)) {
                handleWhitespace(ctx, c);
            } else {
                handleNormal(ctx, c);
            }
        }

        ctx.collectToken();

        return ctx.tokens;
    }

    private static class TokenizerContext {

        private int line = 1;
        private int column = 1;
        private int index;
        private boolean inString;
        private boolean inComment;
        private StringBuffer buffer;
        private final List<Token> tokens = new ArrayList<>();

        private String input, source;

        public void forward() {
            buffer.append(input.charAt(index));
            next();
        }

        public void nextLine() {
            line++;
            column = 1;
        }

        public void next() {
            index++;
            column++;
        }

        public void enterComment() {
            inComment = true;
        }

        public void leaveComment() {
            inComment = false;
        }

        public void enterString() {
            inString = true;
        }

        public void leaveString() {
            inString = false;
        }

        public boolean isString() {
            return inString;
        }

        public boolean isComment() {
            return inComment;
        }

        public char peek() {
            return input.charAt(index + 1);
        }

        static final Pattern NUMBER_PATTERN = Pattern.compile("-?(?:(?:(?:(?:(?:\\d[\\d_]*\\.(?:\\d[\\d_]*)?([eE]-?\\d[\\d_]*)?)|(?:\\.(?:\\d[\\d_]*)(?:[eE]-?\\d[\\d_]*)?)|(?:(?:\\d[\\d_]*)(?:[eE]-?\\d[\\d_]*))|(?:0[xX][\\dA-Fa-f_]*(\\.[\\dA-Fa-f_]*)?[pP]-?\\d[\\d_]*))[fFdD]?)|(?:(?:(?:0[xX][\\dA-fa-f_]+)|(?:\\d[\\d_]*))[LlFfDd]?)))");
        boolean checkIfNumber(String content) {
            switch (content.toLowerCase()) { // floating point numbers
                case "nan", "infinity", "+infinity", "-infinity" -> {
                    return true;
                }
            }
            return NUMBER_PATTERN.matcher(content).matches();
        }

        public TokenType getType(String content) {
            if (content.length() == 1) {
                if (isOperator(content.charAt(0)))
                    return TokenType.OPERATOR;
            }
            if (inString)
                return TokenType.STRING;
            TokenType type = TokenType.IDENTIFIER;
            // check if all the characters in the token are digits (and the '-' sign)
            if (checkIfNumber(content))
                type = TokenType.NUMBER;
            return type;
        }

        public void collectToken() {
            if (buffer.isEmpty())
                return;
            String content = buffer.toString();
            Range range = new Range(index - content.length(), index);
            Location location = new Location(line, column, source);

            TokenType type = getType(content);

            tokens.add(new Token(range, location, type, content));

            buffer = new StringBuffer();
        }

        public void processEscape() {
            switch (input.charAt(index++)) {
                case 'n' -> buffer.append('\n');
                case 'r' -> buffer.append('\r');
                case 't' -> buffer.append('\t');
                case 'b' -> buffer.append('\b');
                case 'f' -> buffer.append('\f');
                case '"' -> buffer.append('"');
                case '\'' -> buffer.append('\'');
                case 'u' -> {
                    buffer.append((char) Integer.parseInt(input.substring(index, index + 4), 16));
                    index += 4;
                }
                default -> buffer.append('\\');
            }
        }

    }

}
