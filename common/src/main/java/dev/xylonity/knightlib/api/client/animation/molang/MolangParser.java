package dev.xylonity.knightlib.api.client.animation.molang;

import dev.xylonity.knightlib.KnightLib;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Small Molang expression compiler used by Bedrock animation structure.
 *
 * Based off GeckoLib implementation
 * https://github.com/bernie-g/geckolib/blob/1.20.1/core/src/main/java/software/bernie/geckolib/core/molang/MolangParser.java
 */
public final class MolangParser {

    private static final Set<String> WARNED = ConcurrentHashMap.newKeySet();

    private final String source;
    private int position;

    private MolangParser(String source) {
        this.source = source;
    }

    /**
     * Compiles an expression
     */
    public static MolangExpression parse(String source) {
        String trimmed = source.trim().toLowerCase(Locale.ROOT);
        if (trimmed.endsWith(";")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }

        if (trimmed.contains(";") || trimmed.contains("=") && !trimmed.contains("==") && !trimmed.contains(">=") && !trimmed.contains("<=") && !trimmed.contains("!=")) {
            throw new IllegalArgumentException("Multi-statement molang is not supported: " + source);
        }

        final MolangParser parser = new MolangParser(trimmed);
        final MolangExpression expression = parser.parseTernary();
        parser.skipWhitespace();

        if (parser.position < parser.source.length()) {
            throw new IllegalArgumentException("Unexpected '" + parser.source.charAt(parser.position) + "' at " + parser.position + " in molang: " + source);
        }

        return expression;
    }

    private MolangExpression parseTernary() {
        final MolangExpression condition = parseOr();
        if (!eat("?")) {
            return condition;
        }

        final MolangExpression whenTrue = parseTernary();
        expect(":");
        final MolangExpression whenFalse = parseTernary();
        return context -> condition.evaluate(context) != 0f ? whenTrue.evaluate(context) : whenFalse.evaluate(context);
    }

    private MolangExpression parseOr() {
        MolangExpression left = parseAnd();
        while (eat("||")) {
            final MolangExpression a = left;
            final MolangExpression b = parseAnd();
            left = context -> a.evaluate(context) != 0f || b.evaluate(context) != 0f ? 1f : 0f;
        }

        return left;
    }

    private MolangExpression parseAnd() {
        MolangExpression left = parseComparison();
        while (eat("&&")) {
            final MolangExpression a = left;
            final MolangExpression b = parseComparison();
            left = context -> a.evaluate(context) != 0f && b.evaluate(context) != 0f ? 1f : 0f;
        }

        return left;
    }

    private MolangExpression parseComparison() {
        final MolangExpression left = parseAdditive();

        String operator = null;
        for (final String candidate : new String[] {"<=", ">=", "==", "!=", "<", ">"}) {
            if (eat(candidate)) {
                operator = candidate;
                break;
            }

        }

        if (operator == null) {
            return left;
        }

        final MolangExpression right = parseAdditive();
        return switch (operator) {
            case "<" -> context -> left.evaluate(context) < right.evaluate(context) ? 1f : 0f;
            case "<=" -> context -> left.evaluate(context) <= right.evaluate(context) ? 1f : 0f;
            case ">" -> context -> left.evaluate(context) > right.evaluate(context) ? 1f : 0f;
            case ">=" -> context -> left.evaluate(context) >= right.evaluate(context) ? 1f : 0f;
            case "==" -> context -> left.evaluate(context) == right.evaluate(context) ? 1f : 0f;
            default -> context -> left.evaluate(context) != right.evaluate(context) ? 1f : 0f;
        };

    }

    private MolangExpression parseAdditive() {
        MolangExpression left = parseMultiplicative();
        while (true) {
            if (eat("+")) {
                final MolangExpression a = left;
                final MolangExpression b = parseMultiplicative();
                left = context -> a.evaluate(context) + b.evaluate(context);
            }
            else if (eatMinus()) {
                final MolangExpression a = left;
                final MolangExpression b = parseMultiplicative();
                left = context -> a.evaluate(context) - b.evaluate(context);
            }
            else {
                return left;
            }

        }

    }

    private MolangExpression parseMultiplicative() {
        MolangExpression left = parseUnary();
        while (true) {
            if (eat("*")) {
                final MolangExpression a = left;
                final MolangExpression b = parseUnary();
                left = context -> a.evaluate(context) * b.evaluate(context);
            }
            else if (eat("/")) {
                final MolangExpression a = left;
                final MolangExpression b = parseUnary();
                left = context -> {
                    final float divisor = b.evaluate(context);
                    return divisor == 0f ? 0f : a.evaluate(context) / divisor;
                };

            }
            else if (eat("%")) {
                final MolangExpression a = left;
                final MolangExpression b = parseUnary();
                left = context -> {
                    final float divisor = b.evaluate(context);
                    return divisor == 0f ? 0f : a.evaluate(context) % divisor;
                };

            }
            else {
                return left;
            }

        }

    }

    private MolangExpression parseUnary() {
        if (eat("!")) {
            final MolangExpression inner = parseUnary();
            return context -> inner.evaluate(context) == 0f ? 1f : 0f;
        }

        if (eatMinus()) {
            final MolangExpression inner = parseUnary();
            return context -> -inner.evaluate(context);
        }

        return parsePrimary();
    }

    private MolangExpression parsePrimary() {
        skipWhitespace();
        if (position >= source.length()) {
            throw new IllegalArgumentException("Unexpected end of molang: " + source);
        }

        final char first = source.charAt(position);
        if (first == '(') {
            position++;
            final MolangExpression inner = parseTernary();
            expect(")");
            return inner;
        }

        if (Character.isDigit(first) || first == '.') {
            return parseNumber();
        }

        if (Character.isLetter(first) || first == '_') {
            return parseIdentifier();
        }

        throw new IllegalArgumentException("Unexpected '" + first + "' at " + position + " in molang: " + source);
    }

    private MolangExpression parseNumber() {
        final int start = position;
        while (position < source.length() && (Character.isDigit(source.charAt(position)) || source.charAt(position) == '.')) {
            position++;
        }

        if (position < source.length() && source.charAt(position) == 'e') {
            int exponent = position + 1;
            if (exponent < source.length() && (source.charAt(exponent) == '+' || source.charAt(exponent) == '-')) {
                exponent++;
            }

            if (exponent < source.length() && Character.isDigit(source.charAt(exponent))) {
                position = exponent;
                while (position < source.length() && Character.isDigit(source.charAt(position))) {
                    position++;
                }

            }

        }

        final float value = Float.parseFloat(source.substring(start, position));
        return context -> value;
    }

    private MolangExpression parseIdentifier() {
        final int start = position;
        while (position < source.length() && (Character.isLetterOrDigit(source.charAt(position)) || source.charAt(position) == '_' || source.charAt(position) == '.')) {
            position++;
        }

        final String name = source.substring(start, position);

        if (name.equals("true")) {
            return context -> 1f;
        }
        if (name.equals("false")) {
            return context -> 0f;
        }
        if (name.equals("math.pi")) {
            return context -> Mth.PI;
        }

        if (name.startsWith("query.") || name.startsWith("q.")) {
            final String query = name.substring(name.indexOf('.') + 1);
            return context -> context.query(query);
        }

        if (name.startsWith("math.")) {
            return parseFunction(name.substring(5));
        }

        // Unsupported identifiers evaluate as 0 instead of breaking the whole animation file
        if (WARNED.add(name)) {
            KnightLib.LOGGER.warn("[KnightLib] Unsupported molang identifier '{}' in: {}", name, source);
        }

        skipWhitespace();
        if (position < source.length() && source.charAt(position) == '(') {
            parseFunctionArguments();
        }

        return context -> 0f;
    }

    private void parseFunctionArguments() {
        expect("(");
        skipWhitespace();
        if (eat(")")) {
            return;
        }

        parseTernary();
        while (eat(",")) {
            parseTernary();
        }

        expect(")");
    }

    private MolangExpression parseFunction(String function) {
        expect("(");

        final List<MolangExpression> arguments = new ArrayList<>();
        skipWhitespace();

        if (!eat(")")) {
            arguments.add(parseTernary());
            while (eat(",")) {
                arguments.add(parseTernary());
            }

            expect(")");
        }

        return buildFunction(function, arguments);
    }

    private MolangExpression buildFunction(String function, List<MolangExpression> args) {
        return switch (function) {
            case "sin" -> unary(function, args, value -> Mth.sin(value * Mth.DEG_TO_RAD));
            case "cos" -> unary(function, args, value -> Mth.cos(value * Mth.DEG_TO_RAD));
            case "asin" -> unary(function, args, value -> (float) Math.toDegrees(Math.asin(Mth.clamp(value, -1f, 1f))));
            case "acos" -> unary(function, args, value -> (float) Math.toDegrees(Math.acos(Mth.clamp(value, -1f, 1f))));
            case "atan" -> unary(function, args, value -> (float) Math.toDegrees(Math.atan(value)));
            case "atan2" -> binary(function, args, (a, b) -> (float) Math.toDegrees(Math.atan2(a, b)));
            case "abs" -> unary(function, args, Math::abs);
            case "sign" -> unary(function, args, Math::signum);
            case "sqrt" -> unary(function, args, value -> (float) Math.sqrt(value));
            case "exp" -> unary(function, args, value -> (float) Math.exp(value));
            case "ln" -> unary(function, args, value -> (float) Math.log(value));
            case "floor" -> unary(function, args, value -> (float) Math.floor(value));
            case "ceil" -> unary(function, args, value -> (float) Math.ceil(value));
            case "round" -> unary(function, args, value -> (float) Math.round(value));
            case "trunc" -> unary(function, args, value -> (float) (int) value);
            case "mod" -> binary(function, args, (a, b) -> b == 0f ? 0f : a % b);
            case "pow" -> binary(function, args, (a, b) -> (float) Math.pow(a, b));
            case "min" -> binary(function, args, Math::min);
            case "max" -> binary(function, args, Math::max);
            case "random" -> binary(function, args, (a, b) -> a + (float) Math.random() * (b - a));
            case "clamp" -> {
                requireArgs(function, args, 3);

                final MolangExpression value = args.get(0);
                final MolangExpression min = args.get(1);
                final MolangExpression max = args.get(2);

                yield context -> Mth.clamp(value.evaluate(context), min.evaluate(context), max.evaluate(context));
            }
            case "lerp" -> {
                requireArgs(function, args, 3);

                final MolangExpression from = args.get(0);
                final MolangExpression to = args.get(1);
                final MolangExpression alpha = args.get(2);

                yield context -> Mth.lerp(alpha.evaluate(context), from.evaluate(context), to.evaluate(context));
            }
            case "lerprotate" -> {
                requireArgs(function, args, 3);

                final MolangExpression from = args.get(0);
                final MolangExpression to = args.get(1);
                final MolangExpression alpha = args.get(2);

                yield context -> Mth.rotLerp(alpha.evaluate(context), from.evaluate(context), to.evaluate(context));
            }
            case "min_angle" -> unary(function, args, Mth::wrapDegrees);
            case "hermite_blend" -> unary(function, args, value -> value * value * (3f - 2f * value));
            default -> {
                if (WARNED.add("math." + function)) {
                    KnightLib.LOGGER.warn("Unsupported molang function 'math.{}' in: {}", function, source);
                }

                yield context -> 0f;
            }

        };

    }

    private MolangExpression unary(String function, List<MolangExpression> args, FloatFunction operation) {
        requireArgs(function, args, 1);

        final MolangExpression inner = args.get(0);
        return context -> operation.apply(inner.evaluate(context));
    }

    private MolangExpression binary(String function, List<MolangExpression> args, FloatBiFunction operation) {
        requireArgs(function, args, 2);

        final MolangExpression a = args.get(0);
        final MolangExpression b = args.get(1);

        return context -> operation.apply(a.evaluate(context), b.evaluate(context));
    }

    private void requireArgs(String function, List<MolangExpression> args, int expected) {
        if (args.size() != expected) {
            throw new IllegalArgumentException("math." + function + " expects " + expected + " arguments, got " + args.size() + " in: " + source);
        }

    }

    private void skipWhitespace() {
        while (position < source.length() && Character.isWhitespace(source.charAt(position))) {
            position++;
        }

    }

    private boolean eat(String token) {
        skipWhitespace();
        if (source.startsWith(token, position)) {
            // Not splitting '<=' into '<' or '==' into '='
            if ((token.equals("<") || token.equals(">")) && position + 1 < source.length() && source.charAt(position + 1) == '=') {
                return false;
            }

            position += token.length();

            return true;
        }

        return false;
    }

    private boolean eatMinus() {
        return eat("-");
    }

    private void expect(String token) {
        if (!eat(token)) {
            throw new IllegalArgumentException("Expected '" + token + "' at " + position + " in molang: " + source);
        }

    }

    private interface FloatFunction {
        float apply(float value);
    }

    private interface FloatBiFunction {
        float apply(float a, float b);
    }

}