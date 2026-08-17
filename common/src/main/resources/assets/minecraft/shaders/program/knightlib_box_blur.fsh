#version 150

uniform sampler2D DiffuseSampler;

in vec2 texCoord;
in vec2 oneTexel;

uniform vec2 BlurDir;
uniform float Radius;
uniform float RadiusMultiplier;

out vec4 fragColor;

// Based off the vanilla's square blur used on certain screens (1.21.1+)
void main() {
    vec4 blurred = vec4(0.0);
    float actualRadius = round(Radius * RadiusMultiplier);

    for (float radius = -actualRadius; radius <= actualRadius; radius += 1.0) {
        blurred += texture(DiffuseSampler, texCoord + oneTexel * radius * BlurDir);
    }

    fragColor = vec4(blurred.rgb / (actualRadius * 2.0 + 1.0), 1.0);
}
