#ifdef GL_ES
precision highp float;
#endif

uniform sampler2D u_texture;
uniform sampler2D u_palette;
uniform vec2 u_taille;
uniform vec2 u_cadre;
uniform vec2 u_camera;
uniform float u_pas;
uniform float u_base;
uniform float u_temps;

varying vec4 v_color;
varying vec2 v_texCoords;

const int MARGE = 128;

float mod289(float x) {
    return x - floor(x / 289.0) * 289.0;
}

float permute(float x) {
    return mod289((x * 34.0 + 1.0) * x);
}

float hasard(vec2 n) {
    return permute(permute(mod289(n.x)) + mod289(n.y)) / 288.0;
}

vec4 lire(vec2 p) {
    if (p.y < 0.0 || p.y >= u_cadre.y) return vec4(0.0);
    return texture2D(u_texture, (p + 0.5) / u_taille);
}

float decalage(float a) {
    if (a < 1.0) return -float(MARGE) - 1.0;
    float rang = floor(a / 2.0) - 64.0;
    return clamp(floor(rang * u_pas + u_base + 0.5), -float(MARGE), float(MARGE));
}

vec3 couleur(float colonne, float ligne) {
    return texture2D(u_palette, vec2((clamp(floor(colonne + 0.5), 0.0, 7.0) + 0.5) / 8.0, (ligne + 0.5) / 16.0)).rgb;
}

void main() {
    vec2 f = floor(gl_FragCoord.xy);
    if (u_pas <= 0.0) {
        vec4 c = lire(f);
        gl_FragColor = c.a > 0.0 ? vec4(c.rgb, 1.0) : vec4(0.0);
        return;
    }
    float x = f.x + u_camera.x;
    float precedent = -float(MARGE) - 1.0;
    bool dehors = true;
    for (int k = -MARGE; k <= MARGE; k++) {
        float yp = f.y + float(k);
        vec4 c = lire(vec2(f.x, yp));
        float a = floor(c.a * 255.0 + 0.5);
        float s = decalage(a);
        float t = s + float(k);
        if (t >= 0.0) {
            if (t < 0.5) {
                gl_FragColor = vec4(c.rgb, 1.0);
                return;
            }
            if (dehors) {
                precedent = s;
                dehors = a < 1.0;
                continue;
            }
            float hauteur = max(s - max(precedent, -float(MARGE)), 1.0);
            float bas = clamp(t / hauteur, 0.0, 1.0);
            if (mod(a, 2.0) > 0.5) {
                float filet = hasard(vec2(x, floor((t - u_temps * 24.0) / 3.0)));
                bool ecume = bas > 0.8 || filet > 0.85;
                gl_FragColor = vec4(couleur(ecume ? 5.0 : 3.0 + filet * 2.5 - bas, ecume ? 14.0 : 13.0), 1.0);
                return;
            }
            float strate = floor((t + hasard(vec2(x, 7.0)) * 3.0) / 4.0);
            float bloc = hasard(vec2(floor((x + strate * 3.0) / 5.0), strate + 11.0));
            float indice = 2.6 + bloc * 1.8 - bas * 2.0;
            if (hasard(vec2(floor(x / 2.0), strate + 50.0)) < 0.12) indice -= 1.2;
            if (t < 1.5) indice = 5.5;
            gl_FragColor = vec4(couleur(indice, 5.0), 1.0);
            return;
        }
        precedent = s;
        dehors = a < 1.0;
    }
    gl_FragColor = vec4(0.0);
}
