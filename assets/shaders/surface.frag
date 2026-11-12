#ifdef GL_ES
precision highp float;
#endif

uniform sampler2D u_texture;
uniform sampler2D u_traces;
uniform vec2 u_taille;
uniform vec2 u_centre;
uniform vec2 u_traceCamera;
uniform vec2 u_rotation;
uniform float u_metresPixel;
uniform float u_actif;
uniform float u_mouille;
uniform float u_neigeSol;

varying vec4 v_color;
varying vec2 v_texCoords;

const float TEXEL = 0.09;
const float FENETRE = 512.0;
const float PROFONDEUR = 0.12;
const vec2 LUMIERE = vec2(-0.70710678, 0.70710678);

float mod289(float x) {
    return x - floor(x / 289.0) * 289.0;
}

float permute(float x) {
    return mod289((x * 34.0 + 1.0) * x);
}

float hasard(vec2 n) {
    return permute(permute(mod289(n.x)) + mod289(n.y)) / 288.0;
}

float bruit(vec2 p) {
    vec2 i = floor(p);
    vec2 f = p - i;
    vec2 u = f * f * (3.0 - 2.0 * f);
    float a = hasard(i), b = hasard(i + vec2(1.0, 0.0)), c = hasard(i + vec2(0.0, 1.0)), d = hasard(i + vec2(1.0, 1.0));
    return mix(mix(a, b, u.x), mix(c, d, u.x), u.y);
}

float bayer2(vec2 a) {
    a = floor(a);
    return fract(a.x / 2.0 + a.y * a.y * 0.75);
}

float bayer4(vec2 a) {
    return bayer2(0.5 * a) * 0.25 + bayer2(a);
}

vec3 lireTrace(vec2 m) {
    vec2 uv = m / (TEXEL * FENETRE) + 0.5;
    if (uv.x <= 0.0 || uv.y <= 0.0 || uv.x >= 1.0 || uv.y >= 1.0) return vec3(0.0);
    return texture2D(u_traces, uv).rgb;
}

float hauteur(vec2 m) {
    vec2 tordu = m + 0.07 * vec2(bruit(m / 0.21) - 0.5, bruit(m / 0.21 + 37.0) - 0.5);
    vec3 t = lireTrace(tordu);
    float creux = t.b + t.g * 0.35;
    float autour = 0.0;
    autour += lireTrace(tordu + vec2(0.24, 0.0)).b;
    autour += lireTrace(tordu - vec2(0.24, 0.0)).b;
    autour += lireTrace(tordu + vec2(0.0, 0.24)).b;
    autour += lireTrace(tordu - vec2(0.0, 0.24)).b;
    float bourrelet = max(autour * 0.25 - t.b, 0.0) * 0.7;
    float grain = (bruit(m / 0.06) - 0.5) * 0.08 * min(t.b * 3.0, 1.0);
    return (bourrelet - creux + grain) * PROFONDEUR;
}

void main() {
    vec2 f = floor(gl_FragCoord.xy);
    vec4 c = texture2D(u_texture, (f + 0.5) / u_taille);
    bool eau = mod(floor(c.b * 255.0 + 0.5), 2.0) > 0.5;
    if (u_actif <= 0.0 || c.a <= 0.0 || eau) {
        gl_FragColor = c;
        return;
    }
    vec2 o = (gl_FragCoord.xy - u_centre) * u_metresPixel;
    vec2 m = u_traceCamera + vec2(u_rotation.x * o.x + u_rotation.y * o.y, -u_rotation.y * o.x + u_rotation.x * o.y);
    float ici = lireTrace(m).b;
    float h0 = hauteur(m);
    if (ici < 0.01 && abs(h0) < 0.0005) {
        gl_FragColor = c;
        return;
    }
    float pas = u_metresPixel;
    vec2 gradient = vec2(hauteur(m + vec2(pas, 0.0)) - hauteur(m - vec2(pas, 0.0)), hauteur(m + vec2(0.0, pas)) - hauteur(m - vec2(0.0, pas))) / (2.0 * pas);
    float lumiere = clamp(dot(gradient, -LUMIERE) * 7.0, -1.0, 1.0);
    float palier = floor(lumiere * 3.0 + bayer4(f)) / 3.0;

    float clarte = dot(c.rgb, vec3(0.3, 0.5, 0.2));
    float neige = u_neigeSol * smoothstep(0.5, 0.7, clarte);
    float fond = clamp(-h0 / PROFONDEUR, 0.0, 1.0);
    vec3 rgb = c.rgb;

    vec3 ombre = mix(vec3(0.62, 0.6, 0.66), vec3(0.55, 0.64, 0.86), neige);
    vec3 eclat = mix(vec3(1.22, 1.18, 1.1), vec3(1.08, 1.08, 1.1), neige);
    rgb *= palier < 0.0 ? mix(vec3(1.0), ombre, -palier) : mix(vec3(1.0), eclat, palier);

    float tasse = floor(fond * 4.0 + bayer4(f + 2.0)) / 4.0;
    rgb *= 1.0 - 0.18 * tasse * (1.0 - neige);
    rgb = mix(rgb, rgb * vec3(0.72, 0.8, 1.0), 0.7 * tasse * neige);

    float boue = smoothstep(0.35, 0.8, u_mouille) * (1.0 - neige);
    if (boue > 0.0) {
        rgb = mix(rgb, rgb * vec3(0.62, 0.55, 0.5), boue * tasse * 0.8);
        float flaque = fond * boue;
        if (flaque > 0.55 + bayer4(f) * 0.25) {
            rgb = mix(rgb, vec3(0.2, 0.22, 0.24), 0.6);
            if (hasard(floor(m / 0.09)) > 0.93) rgb = vec3(0.62, 0.66, 0.7);
        }
    }

    vec3 sortie = clamp(rgb, 0.0, 1.0);
    sortie.b = floor(sortie.b * 127.5) * 2.0 / 255.0;
    gl_FragColor = vec4(sortie, c.a);
}
