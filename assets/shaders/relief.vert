attribute vec2 a_position;

uniform sampler2D u_hauteurs;
uniform vec2 u_taille;
uniform vec2 u_cadre;
uniform vec2 u_vue;
uniform float u_bande;
uniform float u_pas;
uniform float u_ecrase;
uniform float u_relief;
uniform float u_fraction;
uniform float u_metresPixel;
uniform float u_cosinus;
uniform float u_sinus;

varying vec2 v_plan;
varying float v_valide;
varying float v_pente;
varying float v_rangs;

float lire(vec2 p, out float valide) {
    vec4 c = texture2DLod(u_hauteurs, p / u_taille, 0.0);
    valide = c.a > 0.5 ? 1.0 : 0.0;
    vec3 o = floor(c.rgb * 255.0 + 0.5);
    return (o.r * 65536.0 + o.g * 256.0 + o.b - 8388608.0) / 256.0;
}

void main() {
    vec2 p = a_position + vec2(0.0, u_bande);
    float h = 0.0;
    float haut = 0.0;
    float valide = 1.0;
    float valideHaut = 1.0;
    if (u_relief > 0.0) {
        h = lire(p, valide);
        haut = lire(p + vec2(0.0, u_pas), valideHaut);
        if (valide < 0.5) h = 0.0;
        if (valideHaut < 0.5) haut = h;
    }
    float rangs = h * u_relief + u_fraction;
    v_plan = p;
    v_valide = valide;
    v_pente = (haut - h) / (u_pas * u_metresPixel);
    v_rangs = rangs;
    float x = p.x - u_cadre.x * 0.5 + u_vue.x * 0.5;
    float y = (p.y - u_cadre.y * 0.5) * u_ecrase + u_vue.y * 0.5 + 0.25 + rangs;
    float profondeur = ((p.y - u_cadre.y * 0.5) * u_metresPixel * u_cosinus - h * u_sinus) / (u_cadre.y * u_metresPixel + 20000.0);
    gl_Position = vec4(x / u_vue.x * 2.0 - 1.0, y / u_vue.y * 2.0 - 1.0, clamp(profondeur, -0.999, 0.999), 1.0);
}
