package dev.xylonity.knightlib.api.client.animation.molang;

/**
 * A compiled molang expression. Parsed once at load time and evaluated per frame against a {@link MolangContext}
 */
@FunctionalInterface
public interface MolangExpression {

    float evaluate(MolangContext context);

    static MolangExpression parse(String source) {
        return MolangParser.parse(source);
    }

}
