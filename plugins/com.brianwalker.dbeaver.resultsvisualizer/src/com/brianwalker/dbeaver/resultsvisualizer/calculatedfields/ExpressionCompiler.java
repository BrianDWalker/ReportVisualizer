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

/**
 * Recursive-descent compiler for the deliberately small calculated-field grammar.
 *
 * <p>Grammar (loosest to tightest precedence):
 * <pre>
 * expression -> or
 * or         -> and ('OR' and)*
 * and        -> not ('AND' not)*
 * not        -> 'NOT' not | comparison
 * comparison -> additive (('=' | '&lt;&gt;' | '!=' | '&gt;' | '&lt;' | '&gt;=' | '&lt;=') additive)?
 * additive   -> term (('+' | '-') term)*
 * term       -> unary (('*' | '/') unary)*
 * unary      -> ('+' | '-')? primary
 * primary    -> '(' expression ')' | field | number | function
 * </pre>
 * Boolean results (comparisons, AND/OR/NOT) are represented as {@code 1.0}/{@code 0.0}, with
 * {@code null} used for SQL-style "unknown" propagation: AND/OR/NOT return {@code null} if
 * either operand is {@code null}, a deliberate simplification of full three-valued logic that
 * keeps the grammar small while still failing safe (a formula that can't be evaluated renders
 * as blank rather than a wrong number).
 */
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

    /** Single, non-chained comparison. Returns {@code null} ("unknown") if either side is null. */
    private record ComparisonNode(String operator, Node left, Node right) implements Node {
        @Override public Double evaluate(List<Object> values) {
            Double a = left.evaluate(values);
            Double b = right.evaluate(values);
            if (a == null || b == null) return null;
            int comparison = Double.compare(a, b);
            boolean result = switch (operator) {
                case "=" -> comparison == 0;
                case "<>", "!=" -> comparison != 0;
                case ">" -> comparison > 0;
                case "<" -> comparison < 0;
                case ">=" -> comparison >= 0;
                case "<=" -> comparison <= 0;
                default -> throw new IllegalStateException("Unsupported comparison operator.");
            };
            return result ? 1.0 : 0.0;
        }
    }

    private record AndNode(Node left, Node right) implements Node {
        @Override public Double evaluate(List<Object> values) {
            Double a = left.evaluate(values);
            Double b = right.evaluate(values);
            if (a == null || b == null) return null;
            return (a != 0 && b != 0) ? 1.0 : 0.0;
        }
    }

    private record OrNode(Node left, Node right) implements Node {
        @Override public Double evaluate(List<Object> values) {
            Double a = left.evaluate(values);
            Double b = right.evaluate(values);
            if (a == null || b == null) return null;
            return (a != 0 || b != 0) ? 1.0 : 0.0;
        }
    }

    private record NotNode(Node operand) implements Node {
        @Override public Double evaluate(List<Object> values) {
            Double value = operand.evaluate(values);
            if (value == null) return null;
            return value == 0 ? 1.0 : 0.0;
        }
    }

    /** Returns the first non-null argument, or {@code null} if all arguments are null. */
    private record CoalesceNode(List<Node> arguments) implements Node {
        @Override public Double evaluate(List<Object> values) {
            for (Node argument : arguments) {
                Double value = argument.evaluate(values);
                if (value != null) return value;
            }
            return null;
        }
    }

    /** Returns {@code null} if the two arguments are equal, otherwise returns the first argument. */
    private record NullIfNode(Node left, Node right) implements Node {
        @Override public Double evaluate(List<Object> values) {
            Double a = left.evaluate(values);
            if (a == null) return null;
            Double b = right.evaluate(values);
            return b != null && a.doubleValue() == b.doubleValue() ? null : a;
        }
    }

    /** Ternary conditional; the condition is treated as false only when exactly zero. */
    private record IfNode(Node condition, Node whenTrue, Node whenFalse) implements Node {
        @Override public Double evaluate(List<Object> values) {
            Double conditionValue = condition.evaluate(values);
            if (conditionValue == null) return null;
            return conditionValue != 0 ? whenTrue.evaluate(values) : whenFalse.evaluate(values);
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
                case "LOG" -> {
                    double value = evaluated.get(0);
                    if (value <= 0) yield null;
                    if (evaluated.size() == 1) yield Math.log(value);
                    double base = evaluated.get(1);
                    if (base <= 0 || base == 1) yield null;
                    yield Math.log(value) / Math.log(base);
                }
                case "EXP" -> Math.exp(evaluated.get(0));
                case "MOD" -> evaluated.get(1) == 0 ? null : evaluated.get(0) % evaluated.get(1);
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
        private static final List<String> COMPARISON_OPERATORS =
                List.of("<>", "!=", ">=", "<=", ">", "<", "=");

        private final String input;
        private final Map<String, Integer> fields;
        private int position;

        Parser(String input, Map<String, Integer> fields) {
            this.input = input;
            this.fields = fields;
        }

        Node parseExpression() throws CalculatedFieldException {
            return parseOr();
        }

        Node parseOr() throws CalculatedFieldException {
            Node value = parseAnd();
            while (true) {
                skipWhitespace();
                if (matchKeyword("OR")) value = new OrNode(value, parseAnd());
                else return value;
            }
        }

        Node parseAnd() throws CalculatedFieldException {
            Node value = parseNot();
            while (true) {
                skipWhitespace();
                if (matchKeyword("AND")) value = new AndNode(value, parseNot());
                else return value;
            }
        }

        Node parseNot() throws CalculatedFieldException {
            skipWhitespace();
            if (matchKeyword("NOT")) return new NotNode(parseNot());
            return parseComparison();
        }

        Node parseComparison() throws CalculatedFieldException {
            Node value = parseAdditive();
            skipWhitespace();
            String operator = matchComparisonOperator();
            if (operator == null) return value;
            return new ComparisonNode(operator, value, parseAdditive());
        }

        Node parseAdditive() throws CalculatedFieldException {
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
            skipWhitespace();
            if (!match('(')) fail("Expected '(' after " + name);
            List<Node> arguments = new ArrayList<>();
            skipWhitespace();
            if (!atEnd() && current() != ')') {
                while (true) {
                    arguments.add(parseExpression());
                    skipWhitespace();
                    if (!match(',')) break;
                    skipWhitespace();
                }
            }
            skipWhitespace();
            if (!match(')')) fail("Missing closing ')' for " + name);
            return buildFunctionNode(name, arguments);
        }

        private Node buildFunctionNode(String name, List<Node> arguments) throws CalculatedFieldException {
            return switch (name) {
                case "COALESCE" -> {
                    if (arguments.isEmpty()) fail("COALESCE expects at least 1 argument(s)");
                    yield new CoalesceNode(List.copyOf(arguments));
                }
                case "NULLIF" -> {
                    requireArgumentCount(name, arguments, 2, 2);
                    yield new NullIfNode(arguments.get(0), arguments.get(1));
                }
                case "IF" -> {
                    requireArgumentCount(name, arguments, 3, 3);
                    yield new IfNode(arguments.get(0), arguments.get(1), arguments.get(2));
                }
                case "ABS", "CEIL", "FLOOR", "SQRT", "EXP" -> {
                    requireArgumentCount(name, arguments, 1, 1);
                    yield new FunctionNode(name, List.copyOf(arguments));
                }
                case "ROUND", "LOG" -> {
                    requireArgumentCount(name, arguments, 1, 2);
                    yield new FunctionNode(name, List.copyOf(arguments));
                }
                case "POWER", "MIN", "MAX", "MOD" -> {
                    requireArgumentCount(name, arguments, 2, 2);
                    yield new FunctionNode(name, List.copyOf(arguments));
                }
                default -> {
                    fail("Unknown function: " + name);
                    yield null;
                }
            };
        }

        private void requireArgumentCount(String name, List<Node> arguments, int minimum, int maximum)
                throws CalculatedFieldException {
            if (arguments.size() < minimum || arguments.size() > maximum) {
                fail(name + " expects " + (minimum == maximum
                        ? minimum : minimum + " or " + maximum) + " argument(s)");
            }
        }

        void skipWhitespace() { while (!atEnd() && Character.isWhitespace(current())) position++; }

        boolean match(char expected) {
            if (atEnd() || current() != expected) return false;
            position++;
            return true;
        }

        /**
         * Matches a case-insensitive whole-word keyword (e.g. {@code AND}/{@code OR}/{@code NOT})
         * at the current position without consuming input on failure. The caller is responsible
         * for calling {@link #skipWhitespace()} first, consistent with {@link #match(char)}.
         */
        boolean matchKeyword(String keyword) {
            int length = keyword.length();
            if (position + length > input.length()) return false;
            if (!input.regionMatches(true, position, keyword, 0, length)) return false;
            if (position + length < input.length()) {
                char next = input.charAt(position + length);
                if (Character.isLetterOrDigit(next) || next == '_') return false;
            }
            position += length;
            return true;
        }

        /** Matches the longest applicable comparison operator, or returns {@code null}. */
        String matchComparisonOperator() {
            for (String operator : COMPARISON_OPERATORS) {
                if (startsWith(operator)) {
                    position += operator.length();
                    return operator;
                }
            }
            return null;
        }

        boolean startsWith(String token) {
            return input.regionMatches(position, token, 0, token.length());
        }

        boolean atEnd() { return position >= input.length(); }
        char current() { return input.charAt(position); }
        void fail(String message) throws CalculatedFieldException {
            throw new CalculatedFieldException(message + " at position " + (position + 1) + ".");
        }
    }
}
