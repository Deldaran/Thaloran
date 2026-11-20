#ifdef GL_ES
precision highp float;
#endif

uniform sampler2D u_texture;
uniform float u_masque;
uniform sampler2D u_palette;
uniform vec2 u_taille;
uniform vec2 u_quad;
uniform float u_ancre;
uniform float u_metresPixel;
uniform float u_temps;
uniform float u_vent;
uniform float u_neige;

varying vec4 v_color;
varying vec2 v_texCoords;

const vec3 LUMIERE = vec3(-0.62, 0.68, 0.38);
const float FEUILLE = 0.1;
const float BRIN = 0.035;
const float ESPACE = 0.5;
const float FEUILLU = 0.0, SOMBRE = 1.0, CONIFERE = 2.0, MORT = 3.0, BUISSON = 4.0, ROCHER = 5.0;
const float OMBRES = 10.0, LUMIERES = 0.0, ECORCE = 9.0, PIERRE = 5.0, NEIGE = 6.0;

float mod289(float x) {
    return x - floor(x / 289.0) * 289.0;
}

float permute(float x) {
    return mod289((x * 34.0 + 1.0) * x);
}

float hasard(vec2 n) {
    return permute(permute(mod289(n.x)) + mod289(n.y)) / 288.0;
}

float bayer2(vec2 a) {
    a = floor(a);
    return fract(a.x / 2.0 + a.y * a.y * 0.75);
}

float bayer4(vec2 a) {
    return bayer2(0.5 * a) * 0.25 + bayer2(a);
}

vec4 cellule(vec2 p, float graine) {
    vec2 i = floor(p);
    vec2 f = p - i;
    float meilleur = 9.0;
    vec2 centre = vec2(0.0), id = vec2(0.0);
    for (int y = -1; y <= 1; y++) {
        for (int x = -1; x <= 1; x++) {
            vec2 c = i + vec2(float(x), float(y));
            vec2 o = vec2(hasard(c + vec2(graine, 7.0)), hasard(c + vec2(13.0, graine))) * 0.8 + 0.1;
            vec2 d = vec2(float(x), float(y)) + o - f;
            float l = dot(d, d);
            if (l < meilleur) {
                meilleur = l;
                centre = d;
                id = c;
            }
        }
    }
    return vec4(-centre, id);
}

vec3 couleur(float rangee, float indice, vec2 a) {
    float colonne = clamp(floor(indice + (bayer4(a) - 0.5) * 0.6 + 0.5), 0.0, 7.0);
    return texture2D(u_palette, vec2((colonne + 0.5) / 8.0, (rangee + 0.5) / 24.0)).rgb;
}

vec3 rampe(float indice, vec2 a) {
    float r = clamp(floor(indice + (bayer4(a) - 0.5) * 0.7 + 0.5), 0.0, 9.0);
    if (r < 7.0) return texture2D(u_palette, vec2((r + 0.5) / 8.0, (OMBRES + 0.5) / 24.0)).rgb;
    return texture2D(u_palette, vec2((r - 2.0 + 0.5) / 8.0, (LUMIERES + 0.5) / 24.0)).rgb;
}

vec4 chene(vec2 p, vec2 centre, float rayon, float graine, vec2 a, float amas, float teinte) {
    vec4 t = cellule(p / FEUILLE, graine);
    float bosse = length(t.xy);
    float meilleur = -9.0;
    vec2 normale = vec2(0.0);
    float bord = 0.0;
    for (int k = 0; k < 16; k++) {
        float fk = float(k);
        if (fk >= amas) break;
        vec2 o = vec2(hasard(vec2(graine, fk * 5.0 + 1.0)), hasard(vec2(graine, fk * 5.0 + 2.0))) * 2.0 - 1.0;
        vec2 c = centre + o * vec2(rayon * 0.95, rayon * 0.6);
        float r = rayon * (0.3 + 0.18 * hasard(vec2(graine, fk * 5.0 + 3.0))) * (1.2 - 0.45 * length(o));
        vec2 d = (p - c) / r;
        float l = length(d) + bosse * FEUILLE * 1.6 / r;
        if (l >= 1.0) continue;
        float z = sqrt(1.0 - l * l) * r - c.y * 0.12 + hasard(vec2(fk, graine + 5.0)) * 0.05;
        if (z > meilleur) {
            meilleur = z;
            normale = d / max(length(d), 0.001) * min(l, 1.0);
            bord = l;
        }
    }
    if (meilleur < -8.0) return vec4(0.0);
    vec3 n = normalize(vec3(normale, sqrt(max(1.0 - dot(normale, normale), 0.0))));
    vec3 lum = normalize(LUMIERE);
    float eclaire = dot(n, lum);
    float vers = dot(normalize(normale + 0.0001), normalize(lum.xy));
    float indice = 2.8 + eclaire * 4.2 + teinte - bosse * 1.2 + (hasard(t.zw + vec2(graine, 3.0)) - 0.5) * 0.9;
    if (bord > 0.72 && vers > 0.3) indice += 1.6;
    if (bord > 0.8 && vers < -0.2) indice -= 2.2;
    vec3 c = rampe(indice, a);
    if (u_neige > 0.0 && n.y > 0.45 - u_neige * 0.3) c = couleur(NEIGE, 3.0 + eclaire * 2.0, a);
    return vec4(c, 1.0);
}

vec4 tronc(vec2 p, float hauteur, float largeur, float graine, vec2 a) {
    if (p.y < -0.05 || p.y > hauteur) return vec4(0.0);
    float t = p.y / hauteur;
    float axe = sin(t * 4.0 + graine) * 0.2 * t + sin(t * 11.0 + graine * 3.0) * 0.04;
    float demi = largeur * 0.5 * (1.0 - 0.35 * t) + max(0.0, 0.3 - p.y) * 0.7 * largeur;
    float x = (p.x - axe) / demi;
    if (abs(x) > 1.0) return vec4(0.0);
    float stries = hasard(vec2(floor((p.x - axe) / 0.05), floor(p.y / 0.3) + graine));
    float indice = 2.6 - x * 2.4 + (stries - 0.5) * 1.6 - (1.0 - t) * 0.4;
    if (x < -0.75) indice += 1.2;
    vec3 c = couleur(ECORCE, indice, a);
    if (hasard(floor(p / 0.06) + vec2(graine, 11.0)) < 0.25 * (1.0 - t) && x > -0.3) c = rampe(2.0 + (stries - 0.5) * 2.0, a);
    return vec4(c, 1.0);
}

float segment(vec2 p, vec2 a, vec2 b) {
    vec2 pa = p - a, ba = b - a;
    float h = clamp(dot(pa, ba) / dot(ba, ba), 0.0, 1.0);
    return length(pa - ba * h);
}

vec4 sapin(vec2 p, float hauteur, float rayon, float graine, vec2 a) {
    float base = hauteur * 0.2;
    if (p.y > hauteur + 0.6 || p.y < base - 1.8) return vec4(0.0);
    float brin = max(BRIN, u_metresPixel);
    float colonne = floor(p.x / brin);
    float kmax = floor((hauteur - p.y + 1.0) / ESPACE);
    for (int j = 7; j >= 0; j--) {
        float k = kmax - float(j);
        if (k < 0.0) continue;
        float yk = hauteur - k * ESPACE + (hasard(vec2(graine, k)) - 0.5) * 0.25;
        if (yk < base) continue;
        float u = clamp((hauteur - yk) / (hauteur - base), 0.0, 1.0);
        float cote = p.x < 0.0 ? -1.0 : 1.0;
        float hb = hasard(vec2(k + cote * 97.0, graine + 3.0));
        float h2 = hasard(vec2(k + cote * 53.0, graine + 19.0));
        float h3 = hasard(vec2(k + cote * 31.0, graine + 47.0));
        if (h3 < 0.06 && u > 0.25) continue;
        float longueur = rayon * (0.1 + 0.9 * pow(u, 0.7)) * (0.6 + 0.6 * hb);
        float x = abs(p.x) / longueur;
        if (x > 1.0) continue;
        float montee = (h2 - 0.45) * 0.7 * (1.0 - 0.5 * u);
        float chute = (0.15 + 0.55 * h3) * (0.5 + 0.7 * u);
        float releve = hasard(vec2(k + cote * 11.0, graine + 71.0)) * 0.3;
        float dessus = yk + longueur * (montee * x - chute * x * x + releve * smoothstep(0.6, 1.0, x));
        dessus += sin(x * 5.0 + hb * 6.28) * 0.04 * longueur + (hasard(vec2(colonne, k + 41.0)) - 0.5) * 0.05;
        vec2 pixel = floor(p / brin);
        float grain = hasard(pixel + vec2(graine, k * 7.0));
        dessus += (grain - 0.5) * brin * 1.6;
        float hc = hasard(vec2(colonne, k + graine * 0.37));
        float frange = (0.3 + 0.4 * hc) * ESPACE;
        float bas = dessus - ESPACE * (0.6 + 0.5 * hb) * (1.0 - 0.45 * x) - frange * (0.3 + 0.7 * x);
        if (p.y > dessus || p.y < bas) continue;
        if (hasard(vec2(colonne + 7.0, k + graine)) < 0.1 && p.y < bas + frange * 0.6) continue;
        float v = (dessus - p.y) / (dessus - bas);
        if (grain < 0.07 && v > 0.35) continue;
        float trait = hasard(vec2(floor((p.x + p.y * cote * 0.9) / brin), floor(p.y / (brin * 3.0)) + k * 13.0));
        float indice = 6.8 - v * 6.0 - (cote > 0.0 ? 1.4 : -0.3) * (0.3 + 0.7 * x) - u * 0.6 + (hc - 0.5) * 0.6 + montee * 1.5 + (trait - 0.5) * 1.3 + (grain - 0.5) * 0.7;
        if (dessus - p.y < u_metresPixel * 1.5 && cote < 0.0) indice += 1.6;
        if (v > 0.8) indice -= 1.0;
        vec3 c = rampe(indice, a);
        if (u_neige > 0.0 && v < 0.2 + u_neige * 0.3) c = couleur(NEIGE, 3.0 + (1.0 - v) * 2.5 - (cote > 0.0 ? 1.0 : 0.0), a);
        return vec4(c, 1.0);
    }
    return vec4(0.0);
}

vec4 branches(vec2 p, vec2 depart, float rayon, float graine, float amas, vec2 a) {
    for (int k = 0; k < 9; k++) {
        float fk = float(k);
        if (fk >= amas) break;
        vec2 o = vec2(hasard(vec2(graine, fk * 5.0 + 1.0)), hasard(vec2(graine, fk * 5.0 + 2.0))) * 2.0 - 1.0;
        vec2 fin = depart + vec2(0.0, rayon * 0.5) + o * vec2(rayon * 0.95, rayon * 0.6);
        vec2 coude = mix(depart, fin, 0.5) + vec2(hasard(vec2(fk, graine)) - 0.5, hasard(vec2(graine, fk + 9.0)) - 0.3) * 0.5;
        float d = min(segment(p, depart, coude), segment(p, coude, fin));
        float t = clamp(length(p - depart) / max(length(fin - depart), 0.01), 0.0, 1.0);
        if (d < 0.15 * (1.0 - 0.6 * t)) return vec4(couleur(ECORCE, 1.4 - (p.x - coude.x) * 2.0 + (hasard(floor(p / 0.05)) - 0.5), a), 1.0);
    }
    return vec4(0.0);
}

vec4 mort(vec2 p, float hauteur, float graine, vec2 a) {
    vec4 c = tronc(p, hauteur, 0.3, graine, a);
    if (c.a > 0.0) return c;
    for (int k = 0; k < 5; k++) {
        float fk = float(k);
        float y = hauteur * (0.45 + 0.11 * fk);
        float cote = mod(fk, 2.0) * 2.0 - 1.0;
        vec2 debut = vec2(0.0, y);
        vec2 fin = debut + vec2(cote * (0.6 + hasard(vec2(graine, fk)) * 0.9), 0.5 + hasard(vec2(fk, graine)) * 0.9);
        float d = segment(p, debut, fin);
        if (d < 0.06 + 0.04 * (1.0 - fk / 5.0)) return vec4(couleur(ECORCE, 2.5 + (p.y - y) * 1.5, a), 1.0);
    }
    return vec4(0.0);
}

vec4 rocher(vec2 p, float rayon, float graine, vec2 a) {
    vec2 d = (p - vec2(0.0, rayon * 0.55)) / vec2(rayon * 1.2, rayon * 0.75);
    float l = dot(d, d);
    float bosse = hasard(floor(p / 0.35) + vec2(graine, 3.0)) * 0.12;
    if (l > 1.0 - bosse) return vec4(0.0);
    vec3 n = normalize(vec3(d, sqrt(max(1.0 - l, 0.0))));
    n = normalize(floor(n * 2.5 + 0.5) / 2.5);
    vec4 t = cellule(p / 0.3, graine);
    float fissure = length(t.xy) > 0.42 ? 1.0 : 0.0;
    float indice = 3.0 + dot(n, normalize(LUMIERE)) * 3.0 - fissure * 1.5;
    vec3 c = couleur(PIERRE, indice, a);
    if (u_neige > 0.0 && n.y > 0.4) c = couleur(NEIGE, 5.0, a);
    return vec4(c, 1.0);
}

float ombre(vec2 p, float rayon) {
    vec2 d = (p - vec2(rayon * 0.35, -0.05)) / vec2(rayon * 1.1, rayon * 0.32);
    return dot(d, d) < 1.0 ? 1.0 : 0.0;
}

void main() {
    vec2 q = floor(vec2(v_texCoords.x, 1.0 - v_texCoords.y) * u_taille);
    vec2 p = (q + 0.5) * u_metresPixel - vec2(u_quad.x * 0.5, u_ancre);
    float graine = floor(v_color.r * 255.0 + 0.5) + 1.0;
    float code = floor(v_color.g * 255.0 + 0.5);
    float sorte = floor(code / 4.0);
    graine += mod(code, 4.0) * 256.0;
    float phase = v_color.b * 6.2831853;
    float taille = 0.8 + 0.45 * hasard(vec2(graine, 91.0));
    float balance = (sin(u_temps * (1.1 + u_vent * 0.06) + phase) + 0.35 * sin(u_temps * 2.9 + phase * 2.0)) * (0.04 + u_vent * 0.012);
    vec2 v = p;
    v.x -= balance * max(p.y, 0.0) * 0.18;
    vec4 c = vec4(0.0);
    float rayon = 1.0;
    if (sorte == FEUILLU || sorte == SOMBRE) {
        float hauteur = (2.0 + hasard(vec2(graine, 17.0)) * 1.2) * taille;
        rayon = (2.2 + hasard(vec2(graine, 23.0)) * 1.0) * taille * (sorte == SOMBRE ? 1.15 : 1.0);
        vec2 depart = vec2(sin(1.0 + graine) * 0.2, hauteur);
        c = chene(v, depart + vec2(0.0, rayon * 0.5), rayon, graine, q, 15.0, sorte == SOMBRE ? -0.8 : 0.0);
        if (c.a == 0.0) c = branches(v, depart, rayon, graine, 9.0, q);
        if (c.a == 0.0) c = tronc(v, hauteur + 0.3, 0.6 * taille, graine, q);
    } else if (sorte == CONIFERE) {
        float hauteur = (7.0 + hasard(vec2(graine, 17.0)) * 4.0) * taille;
        rayon = (1.7 + hasard(vec2(graine, 23.0)) * 0.6) * taille;
        c = sapin(v, hauteur, rayon, graine, q);
        if (c.a == 0.0) c = tronc(v, hauteur, 0.32 * taille, graine, q);
    } else if (sorte == MORT) {
        rayon = 1.2 * taille;
        c = mort(v, (3.5 + hasard(vec2(graine, 17.0)) * 2.0) * taille, graine, q);
    } else if (sorte == BUISSON) {
        rayon = (0.7 + hasard(vec2(graine, 23.0)) * 0.5) * taille;
        c = chene(v, vec2(0.0, rayon * 0.55), rayon, graine, q, 6.0, -0.4);
    } else {
        rayon = (0.4 + hasard(vec2(graine, 23.0)) * 0.6) * taille;
        c = rocher(p, rayon, graine, q);
    }
    if (c.a == 0.0) {
        if (u_masque < 0.5 && ombre(p, rayon) > 0.0 && bayer4(q) < 0.5) {
            gl_FragColor = vec4(0.0, 0.0, 0.0, 0.28);
            return;
        }
        discard;
    }
    gl_FragColor = vec4(c.rgb, texture2D(u_texture, v_texCoords).a);
}
