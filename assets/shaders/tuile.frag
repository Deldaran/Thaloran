#ifdef GL_ES
precision highp float;
#endif

uniform sampler2D u_carte;
uniform vec2 u_centre;
uniform float u_rayon;

varying vec2 v_texture;
varying float v_hauteur;

void main() {
    if (v_hauteur <= 0.0) {
        discard;
    }
    vec2 q = (gl_FragCoord.xy - u_centre) / u_rayon;
    float distance2 = dot(q, q);
    if (distance2 >= 1.0) {
        discard;
    }
    float ombre = mix(0.45, 1.0, sqrt(sqrt(1.0 - distance2)));
    gl_FragColor = vec4(texture2D(u_carte, v_texture).rgb * ombre, 1.0);
}
