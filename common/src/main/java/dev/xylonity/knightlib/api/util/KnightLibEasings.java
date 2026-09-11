package dev.xylonity.knightlib.api.util;

import net.minecraft.util.Mth;

/**
 * Common easing function predicates
 */
public enum KnightLibEasings {

    LINEAR(0) {
        @Override
        public float apply(float progress) {
            return progress;
        }

    },
    SMOOTHSTEP(1) {
        @Override
        public float apply(float progress) {
            return progress * progress * (3f - 2f * progress);
        }

    },
    SMOOTHERSTEP(2) {
        @Override
        public float apply(float progress) {
            return progress * progress * progress * (progress * (progress * 6f - 15f) + 10f);
        }

    },
    STEP(3) {
        @Override
        public float apply(float progress) {
            return progress >= 1f ? 1f : 0f;
        }

    },
    EASE_IN_QUAD(4) {
        @Override
        public float apply(float progress) {
            return progress * progress;
        }

    },
    EASE_OUT_QUAD(5) {
        @Override
        public float apply(float progress) {
            float time = 1f - progress;
            return 1f - time * time;
        }

    },
    EASE_IN_OUT_QUAD(6) {
        @Override
        public float apply(float progress) {
            if (progress < 0.5f) {
                return 2f * progress * progress;
            }

            return 1f - (float) Math.pow(-2f * progress + 2f, 2f) / 2f;
        }

    },
    EASE_IN_CUBIC(7) {
        @Override
        public float apply(float progress) {
            return progress * progress * progress;
        }

    },
    EASE_OUT_CUBIC(8) {
        @Override
        public float apply(float progress) {
            float time = 1f - progress;
            return 1f - time * time * time;
        }

    },
    EASE_IN_OUT_CUBIC(9) {
        @Override
        public float apply(float progress) {
            if (progress < 0.5f) {
                return 4.0f * progress * progress * progress;
            }

            return 1f - (float) Math.pow(-2f * progress + 2f, 3f) / 2f;
        }

    },
    EASE_IN_QUART(10) {
        @Override
        public float apply(float progress) {
            return progress * progress * progress * progress;
        }

    },
    EASE_OUT_QUART(11) {
        @Override
        public float apply(float progress) {
            float time = 1f - progress;
            return 1f - time * time * time * time;
        }

    },
    EASE_IN_OUT_QUART(12) {
        @Override
        public float apply(float progress) {
            if (progress < 0.5f) {
                return 8.0f * progress * progress * progress * progress;
            }

            return 1f - (float) Math.pow(-2f * progress + 2f, 4.0f) / 2f;
        }

    },
    EASE_IN_QUINT(13) {
        @Override
        public float apply(float progress) {
            return progress * progress * progress * progress * progress;
        }

    },
    EASE_OUT_QUINT(14) {
        @Override
        public float apply(float progress) {
            float time = 1f - progress;
            return 1f - time * time * time * time * time;
        }

    },
    EASE_IN_OUT_QUINT(15) {
        @Override
        public float apply(float progress) {
            if (progress < 0.5f) {
                return 16f * progress * progress * progress * progress * progress;
            }

            return 1f - (float) Math.pow(-2f * progress + 2f, 5f) / 2f;
        }

    },
    EASE_IN_SINE(16) {
        @Override
        public float apply(float progress) {
            return 1f - (float) Math.cos((progress * Math.PI) / 2.0);
        }

    },
    EASE_OUT_SINE(17) {
        @Override
        public float apply(float progress) {
            return (float) Math.sin((progress * Math.PI) / 2.0);
        }

    },
    EASE_IN_OUT_SINE(18) {
        @Override
        public float apply(float progress) {
            return (float) (-(Math.cos(Math.PI * progress) - 1.0) / 2.0);
        }

    },
    EASE_IN_EXPO(19) {
        @Override
        public float apply(float progress) {
            if (progress == 0f) {
                return 0f;
            }

            return (float) Math.pow(2.0, 10.0 * progress - 10.0);
        }

    },

    EASE_OUT_EXPO(20) {
        @Override
        public float apply(float progress) {
            if (progress == 1f) {
                return 1f;
            }

            return 1f - (float) Math.pow(2.0, -10.0 * progress);
        }

    },
    EASE_IN_OUT_EXPO(21) {
        @Override
        public float apply(float progress) {
            if (progress == 0f) {
                return 0f;
            }

            if (progress == 1f) {
                return 1f;
            }

            if (progress < 0.5f) {
                return (float) Math.pow(2.0, 20.0 * progress - 10.0) / 2f;
            }

            return (2f - (float) Math.pow(2.0, -20.0 * progress + 10.0)) / 2f;
        }

    },
    EASE_IN_CIRC(22) {
        @Override
        public float apply(float progress) {
            return 1f - (float) Math.sqrt(1.0 - progress * progress);
        }

    },
    EASE_OUT_CIRC(23) {
        @Override
        public float apply(float progress) {
            float time = progress - 1f;
            return (float) Math.sqrt(1.0 - time * time);
        }

    },
    EASE_IN_OUT_CIRC(24) {
        @Override
        public float apply(float progress) {
            if (progress < 0.5f) {
                return (1f - (float) Math.sqrt(1.0 - Math.pow(2f * progress, 2f))) / 2f;
            }

            return ((float) Math.sqrt(1.0 - Math.pow(-2f * progress + 2f, 2f)) + 1f) / 2f;
        }

    },
    EASE_IN_BACK(25) {
        @Override
        public float apply(float progress) {
            final float c1 = 1.70158f;
            final float c3 = c1 + 1f;

            return c3 * progress * progress * progress - c1 * progress * progress;
        }

    },
    EASE_OUT_BACK(26) {
        @Override
        public float apply(float progress) {
            final float c1 = 1.70158f;
            final float c3 = c1 + 1f;

            float time = progress - 1f;
            return 1f + c3 * time * time * time + c1 * time * time;
        }

    },
    EASE_IN_OUT_BACK(27) {
        @Override
        public float apply(float progress) {
            final float c1 = 1.70158f;
            final float c2 = c1 * 1.525f;

            if (progress < 0.5f) {
                float t = 2f * progress;
                return (t * t * ((c2 + 1f) * t - c2)) / 2f;
            }

            float time = 2f * progress - 2f;
            return (time * time * ((c2 + 1f) * time + c2) + 2f) / 2f;
        }

    },
    EASE_IN_ELASTIC(28) {
        @Override
        public float apply(float progress) {
            if (progress == 0f) {
                return 0f;
            }

            if (progress == 1f) {
                return 1f;
            }

            final double c4 = (2.0 * Math.PI) / 3.0;
            return (float) (-Math.pow(2.0, 10.0 * progress - 10.0) * Math.sin((progress * 10.0 - 10.75) * c4));
        }

    },
    EASE_OUT_ELASTIC(29) {
        @Override
        public float apply(float progress) {
            if (progress == 0f) {
                return 0f;
            }

            if (progress == 1f) {
                return 1f;
            }

            final double c4 = (2.0 * Math.PI) / 3.0;
            return (float) (Math.pow(2.0, -10.0 * progress) * Math.sin((progress * 10.0 - 0.75) * c4) + 1.0);
        }

    },
    EASE_IN_OUT_ELASTIC(30) {
        @Override
        public float apply(float progress) {
            if (progress == 0f) {
                return 0f;
            }

            if (progress == 1f) {
                return 1f;
            }

            final double c5 = (2.0 * Math.PI) / 4.5;
            if (progress < 0.5f) {
                return (float) (-(Math.pow(2.0, 20.0 * progress - 10.0) * Math.sin((20.0 * progress - 11.125) * c5)) / 2.0);
            }

            return (float) ((Math.pow(2.0, -20.0 * progress + 10.0) * Math.sin((20.0 * progress - 11.125) * c5)) / 2.0 + 1.0);
        }

    },
    EASE_OUT_BOUNCE(31) {
        @Override
        public float apply(float progress) {
            final float n1 = 7.5625f;
            final float d1 = 2.75f;

            if (progress < 1f / d1) {
                return n1 * progress * progress;
            }

            if (progress < 2f / d1) {
                float t = progress - 1.5f / d1;
                return n1 * t * t + 0.75f;
            }

            if (progress < 2.5f / d1) {
                float t = progress - 2.25f / d1;
                return n1 * t * t + 0.9375f;
            }

            float time = progress - 2.625f / d1;
            return n1 * time * time + 0.984375f;
        }

    },
    EASE_IN_BOUNCE(32) {
        @Override
        public float apply(float progress) {
            return 1f - EASE_OUT_BOUNCE.apply(1f - progress);
        }

    },
    EASE_IN_OUT_BOUNCE(33) {
        @Override
        public float apply(float progress) {
            if (progress < 0.5f) {
                return (1f - EASE_OUT_BOUNCE.apply(1f - 2f * progress)) / 2f;
            }

            return (1f + EASE_OUT_BOUNCE.apply(2f * progress - 1f)) / 2f;
        }
    },
    CLAMPED_SMOOTHSTEP(34) {
        @Override
        public float apply(float progress) {
            float time = Mth.clamp(progress, 0f, 1f);
            return time * time * (3f - 2f * time);
        }

    };

    private final int id;

    KnightLibEasings(int id) {
        this.id = id;
    }

    public int id() {
        return id;
    }

    public float apply(float progress, float argument) {
        if (this == STEP) {
            return stepped(progress, Float.isFinite(argument) ? Math.round(argument) : 2);
        }
        if (!Float.isFinite(argument)) {
            return apply(progress);
        }

        return switch (this) {
            case EASE_IN_BACK -> {
                yield backIn(progress, argument * 1.70158f);
            }
            case EASE_OUT_BACK -> {
                yield 1f - backIn(1f - progress, argument * 1.70158f);
            }
            case EASE_IN_OUT_BACK -> {
                yield progress < 0.5f
                    ? backIn(progress * 2f, argument * 1.70158f) / 2f
                    : 1f - backIn((1f - progress) * 2f, argument * 1.70158f) / 2f;
            }
            case EASE_IN_ELASTIC -> {
                yield elasticIn(progress, argument);
            }
            case EASE_OUT_ELASTIC -> {
                yield 1f - elasticIn(1f - progress, argument);
            }
            case EASE_IN_OUT_ELASTIC -> {
                yield progress < 0.5f
                    ? elasticIn(progress * 2f, argument) / 2f
                    : 1f - elasticIn((1f - progress) * 2f, argument) / 2f;
            }
            case EASE_IN_BOUNCE -> {
                yield 1f - adjustableBounce(1f - progress, argument);
            }
            case EASE_OUT_BOUNCE -> {
                yield adjustableBounce(progress, argument);
            }
            case EASE_IN_OUT_BOUNCE -> {
                yield progress < 0.5f
                    ? (1f - adjustableBounce(1f - progress * 2f, argument)) / 2f
                    : (1f + adjustableBounce(progress * 2f - 1f, argument)) / 2f;
            }
            default -> {
                yield apply(progress);
            }

        };

    }

    private static float backIn(float progress, float overshoot) {
        return (overshoot + 1f) * progress * progress * progress - overshoot * progress * progress;
    }

    private static float stepped(float progress, int requestedSteps) {
        final int steps = Math.max(2, requestedSteps);
        final float time = Mth.clamp(progress, 0f, 1f);
        final int index = Mth.clamp((int) Math.ceil(time * steps) - 1, 0, steps - 1);
        return index / (float) steps;
    }

    private static float elasticIn(float progress, float elasticity) {
        final double cos = Math.cos(progress * Math.PI / 2.0);
        return (float) (1.0 - cos * cos * cos * Math.cos(progress * elasticity * Math.PI));
    }

    private static float adjustableBounce(float progress, float bounciness) {
        final double one = 121.0 / 16.0 * progress * progress;
        final double two = 121.0 / 4.0 * bounciness * Math.pow(progress - 6.0 / 11.0, 2) + 1.0 - bounciness;
        final double three = 121.0 * bounciness * bounciness * Math.pow(progress - 9.0 / 11.0, 2) + 1.0 - bounciness * bounciness;
        final double four = 484.0 * bounciness * bounciness * bounciness * Math.pow(progress - 10.5 / 11.0, 2) + 1.0 - bounciness * bounciness * bounciness;
        return (float) Math.min(Math.min(one, two), Math.min(three, four));
    }

    public static KnightLibEasings byId(int id) {
        for (final KnightLibEasings easing : values()) {
            if (easing.id == id) {
                return easing;
            }

        }

        return SMOOTHSTEP;
    }

    public abstract float apply(float progress);

}