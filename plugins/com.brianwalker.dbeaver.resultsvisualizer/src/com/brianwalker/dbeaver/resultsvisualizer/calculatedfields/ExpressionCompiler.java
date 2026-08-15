/* Copyright (c) 2026 Brian Walker. SPDX-License-Identifier: EPL-2.0 */
package com.brianwalker.dbeaver.resultsvisualizer.calculatedfields;

import com.brianwalker.dbeaver.resultsvisualizer.model.ResultColumn;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.temporal.TemporalAccessor;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Recursive-descent compiler for the deliberately small calculated-field grammar. */
public final class ExpressionCompiler {
    private ExpressionCompiler() {}

    public static CompiledExpression compile(String expression, List<ResultColumn> columns)
            throws CalculatedFieldException {
        if (expression == null || expression.isBlank()) {
            throw new CalculatedFieldException("Expression is required.");
        }
        Map<String, Integer> fields = new LinkedHashMap<>();
        for (int index = 0; index < columns.size(); index++) {
            ResultColumn column = columns.get(index);
            fields.putIfAbsent(normalize(column.name()), index);
            fields.putIfAbsent(normalize(column.label()), index);
            fields.putIfAbsent(normalize(column.displayName()), index);
        }
        Parser parser = new Parser(expression, fields);
        Node root = parser.parseExpression();
        parser.skipWhitespace();
        if (!parser.atEnd()) parser.fail("Unexpected '" + parser.current() + "'");
        return values -> {
            Double value = root.evaluate(values);
            return value == null ? null : finite(value);
        };
    }

    private static String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private interface Node { Double evaluate(List<Object> values); }

    private record ConstantNode(double value) implements Node {
        @Override public Double evaluate(List<Object> values) { return value; }
    }

    private record FieldNode(int index) implements Node {
        @Override public Double evaluate(List<Object> values) {
            return index >= values.size() ? null : number(values.get(index));
        }
    }

    private record UnaryNode(char operator, Node operand) implements Node {
        @Override public Double evaluate(List<Object> values) {
            Double value = operand.evaluate(values);
            if (value == null) return null;
            return operator == '-' ? -value : value;
        }
    }

    private record BinaryNode(char operator, Node left, Node right) implements Node {
        @Override public Double evaluate(List<Object> values) {
            Double a = left.evaluate(values);
            Double b = right.evaluate(values);
            if (a == null || b == null || (operator == '/' && b == 0)) return null;
            return finite(switch (operator) {
                case '+' -> a + b;
                case '-' -> a - b;
                case '*' -> a * b;
                case '/' -> a / b;
                default -> throw new IllegalStateException("Unsupported arithmetic operator.");
            });
        }
    }

    private record FunctionNode(String name, List<Node> arguments) implements Node {
        @Override public Double evaluate(List<Object> values) {
            List<Double> evaluated = new ArrayList<>(arguments.size());
            for (Node argument : arguments) {
                Double value = argument.evaluate(values);
                if (value == null) return null;
                evaluated.add(value);
            }
            Double result = switch (name) {
                case "ABS" -> Math.abs(evaluated.get(0));
                case "ROUND" -> {
                    int scale = evaluated.size() == 1 ? 0 : (int) Math.round(evaluated.get(1));
                    if (scale < -10 || scale > 10) yield null;
                    yield BigDecimal.valueOf(evaluated.get(0))
                            .setScale(scale, RoundingMode.HALF_UP).doubleValue();
                }
                case "CEIL" -> Math.ceil(evaluated.get(0));
                case "FLOOR" -> Math.floor(evaluated.get(0));
                case "SQRT" -> evaluated.get(0) < 0 ? null : Math.sqrt(evaluated.get(0));
                case "POWER" -> Math.pow(evaluated.get(0), evaluated.get(1));
                case "MIN" -> Math.min(evaluated.get(0), evaluated.get(1));
                case "MAX" -> Math.max(evaluated.get(0), evaluated.get(1));
                default -> null;
            };
            return result == null ? null : finite(result);
        }
    }

    private static Double number(Object value) {
        if (value == null || value instanceof Boolean || value instanceof TemporalAccessor) return null;
        if (value instanceof Number numeric) return finite(numeric.doubleValue());
        try {
            return finite(new BigDecimal(value.toString().trim()).doubleValue());
        } catch (NumberFormatException error) {
            return null;
        }
    }

    private static Double finite(double value) { return Double.isFinite(value) ? value : null; }

    private static final class Parser {
        private final String input;
        private final Map<String, Integer> fields;
        private int position;

        Parser(String input, Map<String, Integer> fields) {
            this.input = input;
            this.fields = fields;
        }

        Node parseExpression() throws CalculatedFieldException {
            Node value = parseTerm();
            while (true) {
                skipWhitespace();
                if (match('+')) value = new BinaryNode('+', value, parseTerm());
                else if (match('-')) value = new BinaryNode('-', value, parseTerm());
                else return value;
            }
        }

        Node parseTerm() throws CalculatedFieldException {
            Node value = parseUnary();
            while (true) {
                skipWhitespace();
                if (match('*')) value = new BinaryNode('*', value, parseUnary());
                else if (match('/')) value = new BinaryNode('/', value, parseUnary());
                else return value;
            }
        }

        Node parseUnary() throws CalculatedFieldException {
            skipWhitespace();
            if (match('+')) return new UnaryNode('+', parseUnary());
            if (match('-')) return new UnaryNode('-', parseUnary());
            return parsePrimary();
        }

        Node parsePrimary() throws CalculatedFieldException {
            skipWhitespace();
            if (atEnd()) fail("Expression is incomplete");
            if (match('(')) {
                Node value = parseExpression();
                skipWhitespace();
                if (!match(')')) fail("Missing closing ')'");
                return value;
            }
            if (current() == '[') return parseField();
            if (Character.isDigit(current()) || current() == '.') return parseNumber();
            if (Character.isLetter(current())) return parseFunction();
            fail("Expected a number, field reference, function, or '('");
            return null;
        }

        Node parseField() throws CalculatedFieldException {
            position++;
            int start = position;
            while (!atEnd() && current() != ']') position++;
            if (atEnd()) fail("Missing closing ']' for field reference");
            String name = input.substring(start, position).trim();
            position++;
            if (name.isEmpty()) fail("Field reference cannot be empty");
            Integer index = fields.get(normalize(name));
            if (index == null) throw new CalculatedFieldException("Unknown field: [" + name + "]");
            return new FieldNode(index);
        }

        Node parseNumber() throws CalculatedFieldException {
            int start = position;
            boolean dot = false;
            while (!atEnd()) {
                char value = current();
                if (Character.isDigit(value)) position++;
                else if (value == '.' && !dot) { dot = true; position++; }
                else break;
            }
            try {
                return new ConstantNode(new BigDecimal(input.substring(start, position)).doubleValue());
            } catch (NumberFormatException error) {
                fail("Invalid numeric constant");
                return null;
            }
        }

        Node parseFunction() throws CalculatedFieldException {
            int start = position;
            while (!atEnd() && Character.isLetter(current())) position++;
            String name = input.substring(start, position).toUpperCase(Locale.ROOT);
            int minimumArguments;
            int maximumArguments;
            switch (name) {
                case "ABS", "CEIL", "FLOOR", "SQRT" -> {
                    minimumArguments = 1;
                    maximumArguments = 1;
                }
                case "ROUND" -> {
                    minimumArguments = 1;
                    maximumArguments = 2;
                }
                case "POWER", "MIN", "MAX" -> {
                    minimumArguments = 2;
                    maximumArguments = 2;
                }
                default -> {
                    fail("Unknown function: " + name);
                    return null;
                }
            }
            skipWhitespace();
            if (!match('(')) fail("Expected '(' after " + name);
            List<Node> arguments = new ArrayList<>();
            skipWhitespace();
            if (!atEnd() && current() != ')') {
                while (true) {
                    arguments.add(parseExpression());
                    skipWhitespace();
                    if (!match(',')) break;
                }
            }
            skipWhitespace();
            if (!match(')')) fail("Missing closing ')' for " + name);
            if (arguments.size() < minimumArguments || arguments.size() > maximumArguments) {
                fail(name + " expects " + (minimumArguments == maximumArguments
                        ? minimumArguments : minimumArguments + " or " + maximumArguments)
                        + " argument(s)");
            }
            return new FunctionNode(name, List.copyOf(arguments));
        }

        void skipWhitespace() { while (!atEnd() && Character.isWhitespace(current())) position++; }
        boolean match(char expected) {
            if (atEnd() || current() != expected) return false;
            position++;
            return true;
        }
        boolean atEnd() { return position >= input.length(); }
        char current() { return input.charAt(position); }
        void fail(String message) throws CalculatedFieldException {
            throw new CalculatedFieldException(message + " at position " + (position + 1) + ".");
        }
    }
}
