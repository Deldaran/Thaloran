#ifdef GL_ES
precision highp float;
#endif

uniform sampler2D u_texture;
uniform sampler2D u_palette;
uniform vec2 u_taille;
uniform vec2 u_camera;
uniform float u_temps;

varying vec2 v_plan;
varying float v_valide;
varying float v_pente;
varying float v_rangs;

const float DOUCE = 0.58;
const float FORTE = 1.15;
const float RAIDE = 2.6;
const float CASCADE = 2.0;

float mod289(float x) {
    return x - floor(x / 289.0) * 289.0;
}

float permute(float x) {
    return mod289((x * 34.0 + 1.0) * x);
}

float hasard(vec2 n) {
    return permute(permute(mod289(n.x)) + mod289(n.y)) / 288.0;
}

vec3 couleur(float colonne, float ligne) {
    return texture2D(u_palette, vec2((clamp(floor(colonne + 0.5), 0.0, 7.0) + 0.5) / 8.0, (ligne + 0.5) / 16.0)).rgb;
}

void main() {
    if (v_valide < 0.999) discard;
    vec2 f = floor(v_plan);
    vec4 c = texture2D(u_texture, (f + 0.5) / u_taille);
    if (c.a <= 0.0) discard;
    bool eau = mod(floor(c.b * 255.0 + 0.5), 2.0) > 0.5;
    float x = mod(f.x + u_camera.x, 289.0);
    if (eau && v_pente > CASCADE) {
        float filet = hasard(vec2(x, floor((v_rangs - u_temps * 24.0) / 3.0)));
        bool ecume = filet > 0.85;
        gl_FragColor = vec4(couleur(ecume ? 5.0 : 3.0 + filet * 2.5, ecume ? 14.0 : 13.0), 1.0);
        return;
    }
    if (!eau && v_pente > RAIDE) {
        float rang = floor(v_rangs);
        float strate = floor((rang + hasard(vec2(x, 7.0)) * 3.0) / 4.0);
        float bloc = hasard(vec2(floor((x + strate * 3.0) / 5.0), mod289(strate + 11.0)));
        float indice = 1.8 + bloc * 1.8;
        if (hasard(vec2(floor(x / 2.0), mod289(strate + 50.0))) < 0.12) indice -= 1.2;
        gl_FragColor = vec4(couleur(indice, 5.0), 1.0);
        return;
    }
    float teinte = v_pente > FORTE ? 0.8 : v_pente > DOUCE ? 0.9 : 1.0;
    gl_FragColor = vec4(c.rgb * teinte, 1.0);
}
