package dev.xylonity.knightlib.api.client.animation.molang;

/**
 * A compiled Molang expression, where parsing happens once when the asset is loaded.
 *
 * Based off GeckoLib implementation
 * https://github.com/bernie-g/geckolib/blob/1.20.1/core/src/main/java/software/bernie/geckolib/core/molang/expressions/MolangValue.java
 */
@FunctionalInterface
public interface MolangExpression {

    float evaluate(MolangContext context);

    static MolangExpression parse(String source) {
        return MolangParser.parse(source);
    }

}
