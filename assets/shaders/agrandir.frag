#ifdef GL_ES
precision highp float;
#endif

uniform sampler2D u_texture;
uniform vec2 u_taille;
uniform float u_echelle;
uniform float u_ecrase;

varying vec4 v_color;
varying vec2 v_texCoords;

void main() {
    vec2 p = v_texCoords * u_taille - 0.5;
    vec2 base = floor(p);
    vec2 f = clamp((fract(p) - 0.5) * vec2(u_echelle, u_echelle * (u_ecrase > 0.0 ? u_ecrase : 1.0)) + 0.5, 0.0, 1.0);
    gl_FragColor = v_color * texture2D(u_texture, (base + f + 0.5) / u_taille);
}
