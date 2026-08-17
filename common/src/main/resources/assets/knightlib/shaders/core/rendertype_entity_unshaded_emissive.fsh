#version 150

#moj_import <fog.glsl>

uniform sampler2D Sampler0;

uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;

in float vertexDistance;
in vec4 vertexColor;
in vec4 overlayColor;
in vec2 texCoord0;

out vec4 fragColor;

// Ignores ambient lightning
void main() {
    vec4 color = texture(Sampler0, texCoord0) * vertexColor * ColorModulator;
    color.rgb = mix(overlayColor.rgb, color.rgb, overlayColor.a);
    color *= linear_fog_fade(vertexDistance, FogStart, FogEnd);
    // Doesn't render the pixel below such alpha
    if (color.a < 0.0001) {
        discard;
    }

    fragColor = color;
}
