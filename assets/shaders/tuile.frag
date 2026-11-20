#ifdef GL_ES
precision highp float;
#endif

uniform sampler2D u_donnees;
uniform sampler2D u_couverture;
uniform sampler2D u_palette;
uniform vec2 u_centre;
uniform float u_rayon;
uniform vec2 u_camera;
uniform float u_metresPixel;
uniform float u_plaines;
uniform float u_biomes;
uniform float u_niveau;
uniform float u_fondu;
uniform float u_densite;
uniform float u_plat;
uniform float u_temps;
uniform float u_maree;
uniform float u_mode;
uniform float u_referenceMetres;
uniform vec2 u_rotation;
uniform float u_mouille;
uniform float u_neigeSol;
uniform sampler2D u_traces;
uniform vec2 u_traceCamera;
uniform sampler2D u_rues;
uniform vec2 u_rueCamera;
uniform sampler2D u_rueSol;
uniform float u_rueTexel;
uniform float u_rueBase;
uniform float u_rueActif;
uniform vec3 u_microFrac;
uniform vec3 u_microBase;
uniform vec3 u_microBase2;
uniform vec3 u_est;
uniform vec3 u_nord;
uniform sampler2D u_champs;
uniform vec2 u_champDecalage;
uniform float u_annee;


varying vec2 v_texture;
varying float v_hauteur;
varying vec3 v_direction;

const float TEXELS = 256.0;
const float LARGEUR = 258.0;
const vec2 LUMIERE = vec2(-0.70710678, 0.70710678);
const float RELIEF = 6.0;
const float TAU = 6.2831853;
const float HOULE = 0.15;
const float DEPASSE = 0.15;
const float VITESSE_RIVIERE = 1.5;
const float ESTUAIRE = 2.5;
const float TOUFFE = 9.0;
const float VIDE_HERBE = 0.12;
const float SEUIL_HERBE = 0.8;
const float REFERENCE_HERBE = 0.0468;
const float FINESSE_MIN = 0.35;
const float TEXEL_TRACE = 0.09;
const float TAILLE_RUE = 1024.0;
const float PIERRE_RUE = 4.0;
const float CELLULE_MICRO = 12.0;
const float AMPLITUDE_MICRO = 4.0;
const float SILLON = 0.7;
const float PI = 3.14159265;

float mod289(float x) {
    return x - floor(x / 289.0) * 289.0;
}

float permute(float x) {
    return mod289((x * 34.0 + 1.0) * x);
}

float hasard(vec2 n) {
    return permute(permute(mod289(n.x)) + mod289(n.y)) / 288.0;
}

float hasard3(vec3 n) {
    return permute(permute(permute(mod289(n.x)) + mod289(n.y)) + mod289(n.z)) / 288.0;
}

float bruitMicro(vec3 c, vec3 base) {
    vec3 i = floor(c);
    vec3 f = c - i;
    vec3 a = vec3(mod289(base.x + i.x), mod289(base.y + i.y), mod289(base.z + i.z));
    vec3 b = vec3(mod289(a.x + 1.0), mod289(a.y + 1.0), mod289(a.z + 1.0));
    vec3 u = f * f * f * (f * (f * 6.0 - 15.0) + 10.0);
    float x00 = mix(hasard3(vec3(a.x, a.y, a.z)), hasard3(vec3(b.x, a.y, a.z)), u.x);
    float x10 = mix(hasard3(vec3(a.x, b.y, a.z)), hasard3(vec3(b.x, b.y, a.z)), u.x);
    float x01 = mix(hasard3(vec3(a.x, a.y, b.z)), hasard3(vec3(b.x, a.y, b.z)), u.x);
    float x11 = mix(hasard3(vec3(a.x, b.y, b.z)), hasard3(vec3(b.x, b.y, b.z)), u.x);
    return mix(mix(x00, x10, u.y), mix(x01, x11, u.y), u.z);
}

float micro(vec2 decalage) {
    vec3 c = u_microFrac + (u_est * decalage.x + u_nord * decalage.y) / CELLULE_MICRO;
    return 0.72 * bruitMicro(c, u_microBase) + 0.28 * bruitMicro(c * 3.0, u_microBase2);
}


float poids(float x) {
    return x < 1.0 ? x : pow(0.5, x - 1.0);
}

float grain(vec2 n) {
    float somme = 0.0;
    float carres = 0.0;
    for (int k = 0; k < 6; k++) {
        float o = float(k);
        float w = poids(o + u_fondu);
        somme += w * (hasard(floor(n / exp2(o)) + mod289((u_niveau - o) * 53.0)) - 0.5);
        carres += w * w;
    }
    return 0.5 + 0.616 * somme / sqrt(carres);
}

float grainSphere(vec3 p) {
    float somme = 0.0;
    float carres = 0.0;
    for (int k = 0; k < 6; k++) {
        float o = float(k);
        float w = poids(o + u_fondu);
        somme += w * (hasard3(floor(p / exp2(o)) + mod289((u_niveau - o) * 53.0)) - 0.5);
        carres += w * w;
    }
    return 0.5 + 0.616 * somme / sqrt(carres);
}
vec2 worley(vec2 p, float t) {
    vec2 c = floor(p);
    vec2 f = p - c;
    float f1 = 8.0;
    float f2 = 8.0;
    for (int j = -1; j <= 1; j++) {
        for (int i = -1; i <= 1; i++) {
            vec2 o = vec2(float(i), float(j));
            vec2 h = vec2(hasard(c + o), hasard(c + o + vec2(57.0, 113.0)));
            vec2 r = o + 0.5 + 0.4 * sin(t + TAU * h) - f;
            float d = dot(r, r);
            if (d < f1) {
                f2 = f1;
                f1 = d;
            } else if (d < f2) {
                f2 = d;
            }
        }
    }
    return sqrt(vec2(f1, f2));
}


float brin(vec2 n, float ecrase, float arrache) {
    float devant = 1e9;
    float hauteur = -1.0;
    float finesse = max(REFERENCE_HERBE / u_metresPixel, FINESSE_MIN);
    float touffe = TOUFFE * finesse;
    vec2 c0 = floor(n / touffe);
    for (int j = 0; j <= 2; j++) {
        for (int i = -1; i <= 1; i++) {
            vec2 c = c0 + vec2(float(i), -float(j));
            if (hasard(c + vec2(9.0, 41.0)) < VIDE_HERBE) continue;
            vec2 centre = floor(c * touffe + vec2(hasard(c + vec2(17.0, 31.0)), hasard(c + vec2(71.0, 5.0))) * touffe);
            float vent = 0.7 * sin(u_temps * 1.7 + centre.x * 0.13 + centre.y * 0.07);
            for (int b = 0; b < 7; b++) {
                float o = float(b) - 3.0;
                float h = hasard(c + vec2(3.0 + float(b) * 11.0, 97.0));
                if (hasard(c + vec2(59.0 + float(b) * 7.0, 13.0)) < arrache) continue;
                vec2 racine = centre + vec2(floor(o * 1.5 * finesse), floor(h * 3.0 * finesse));
                float longueur = (4.0 + floor(h * 5.0) + 2.0 * (3.0 - abs(o))) * finesse;
                longueur = max(1.0, floor(longueur * (1.0 - 0.8 * ecrase)));
                float k = n.y - racine.y;
                if (k < 0.0 || k >= longueur) continue;
                float penche = o * 0.7 + (h - 0.5) * 0.8 + vent;
                float x = racine.x + floor(penche * finesse * k / longueur + 0.5);
                if (abs(n.x - x) > 0.5) continue;
                if (racine.y < devant) {
                    devant = racine.y;
                    hauteur = k / max(longueur - 1.0, 1.0);
                }
            }
        }
    }
    return hauteur;
}

vec2 coin(vec2 uv) {
    return (uv * TEXELS + 1.0) / LARGEUR;
}

float altitudeEn(vec2 uv) {
    return texture2D(u_donnees, coin(uv)).r;
}

vec3 solRue(vec2 uv) {
    vec2 c = uv * TAILLE_RUE - 0.5;
    vec2 i = floor(c);
    vec2 f = c - i;
    float somme = 0.0;
    float sommeH = 0.0;
    float rigole = 0.0;
    for (int k = 0; k < 4; k++) {
        vec2 o = vec2(mod(float(k), 2.0), floor(float(k) / 2.0));
        vec4 t = texture2D(u_rueSol, (i + o + 0.5) / TAILLE_RUE);
        vec2 b = mix(1.0 - f, f, o);
        float w = b.x * b.y * t.b;
        float h = (floor(t.r * 255.0 + 0.5) * 256.0 + floor(t.g * 255.0 + 0.5)) / 64.0 - 512.0;
        somme += w;
        sommeH += w * h;
        rigole += b.x * b.y * t.a;
    }
    return vec3(somme > 0.0 ? sommeH / somme + u_rueBase : 0.0, somme, rigole);
}

// Lot J1 : la terre labourée, en sillons (tr : la distance en travers des sillons, en mètres).
vec2 labour(float tr, vec2 n, float g, float fin) {
    float s = fract(tr / SILLON);
    float i = fin > 0.5 ? (s < 0.3 ? 4.0 : (s < 0.62 ? 2.9 : 1.7)) : 2.9;
    i += (hasard(floor(n / 2.0) + vec2(41.0, 3.0)) - 0.5) * 0.9 + (g - 0.5) * 0.6;
    return vec2(2.0, i);
}

// Lot J1 : les tiges vues en 3/4 : de petits traits verticaux à l'écran, de 3 à 5 pixels.
float tiges(vec2 n) {
    float k = floor((n.y + hasard(vec2(n.x, 5.0)) * 4.0) / 4.0);
    return hasard(vec2(n.x, k) + vec2(17.0, 29.0));
}

// Lot J1 : un champ de blé au fil de l'année : labour, semis, pousse, blé vert, épis, mûr, moisson et gerbes, chaume, terre d'hiver.
vec2 cereale(float an, float le, float tr, vec2 n, vec2 p, float g, float fin) {
    float s = fract(tr / SILLON);
    float t = tiges(n);
    if (an < 0.06) return labour(tr, n, g, fin);
    if (an < 0.12) {
        if (s < 0.35 && hasard(floor(vec2(le, tr) / 0.12) + vec2(7.0, 70.0)) < 0.08) return vec2(22.0, 3.0);
        return labour(tr, n, g, fin);
    }
    if (an < 0.25) {
        float c = (an - 0.12) / 0.13;
        if (abs(s - 0.2) < 0.05 + 0.32 * c && hasard(floor(vec2(le, tr) / 0.1) + vec2(3.0, 33.0)) < 0.45 + 0.55 * c) return vec2(22.0, 2.4 + c * 2.0 + (t - 0.5) * 1.4);
        return labour(tr, n, g, fin);
    }
    float onde = sin(dot(p, vec2(0.31, 0.17)) - u_temps * 1.4) * 0.45 * fin;
    float creux = s > 0.82 && fin > 0.5 ? -0.9 : 0.0;
    float relief = (t - 0.5) * 1.6 * fin + onde + creux + (g - 0.5) * 0.5;
    if (an < 0.36) return vec2(22.0, 3.3 + (an - 0.25) * 8.0 + relief);
    if (an < 0.47) {
        float d = (an - 0.36) / 0.11;
        bool mur = hasard(vec2(floor(n.x), floor(n.y / 3.0)) + vec2(77.0, 1.0)) < d;
        return vec2(mur ? 18.0 : 22.0, (mur ? 4.6 : 5.2) + relief);
    }
    // La moisson : les faucheurs avancent le long des rangs, trois rangs à la fois.
    float coupe = clamp((an - 0.47) / 0.09, 0.0, 1.0);
    if (fract(le / 40.0 + hasard(vec2(floor(tr / (SILLON * 3.0)), 3.0))) >= coupe) return vec2(18.0, 5.0 + relief);
    if (an < 0.68) {
        vec2 c = vec2(le / 4.0, tr / 3.5);
        vec2 f = (fract(c) - 0.5) * vec2(4.0, 3.5);
        if (hasard(floor(c) + vec2(13.0, 2.0)) < 0.7 && length(f * vec2(1.0, 0.8)) < 0.48) return vec2(18.0, f.y > 0.1 ? 6.2 : (f.y > -0.2 ? 4.8 : 2.8));
    }
    float hiver = clamp((an - 0.72) / 0.12, 0.0, 1.0);
    float pointe = hasard(floor(n / vec2(1.0, 2.0)) + vec2(5.0, 61.0)) < 0.18 * fin ? 1.4 : 0.0;
    // Le chaume : des rangs de tiges coupées, la terre visible entre eux ; l'hiver, la terre nue.
    if (fin > 0.5 && s > 0.62) return vec2(2.0, 3.0 + (g - 0.5) * 0.6 - hiver * 0.8);
    float i = 3.0 + (fin > 0.5 && s < 0.2 ? 0.6 : 0.0) + pointe * 1.4 + (g - 0.5) * 0.6 - hiver * 1.6;
    return vec2(hiver > 0.6 ? 9.0 : 18.0, hiver > 0.6 ? i + 1.7 : i);
}

// Lot J1 : un potager : des rangs de touffes qui grossissent, puis la récolte et la terre nue de l'hiver.
vec2 legumes(float an, float le, float tr, vec2 n, float g, float fin) {
    if (an < 0.08 || an > 0.74) return labour(tr, n, g, fin);
    float c = clamp((an - 0.08) / 0.3, 0.0, 1.0);
    float recolte = clamp((an - 0.6) / 0.12, 0.0, 1.0);
    vec2 cel = vec2(le / 0.6, tr / 0.9);
    vec2 f = (fract(cel) - 0.5) * vec2(0.6, 0.9);
    float rayon = (0.08 + 0.24 * c) * (0.75 + 0.5 * hasard(floor(cel) + vec2(4.0, 8.0)));
    if (hasard(floor(cel) + vec2(31.0, 17.0)) >= recolte && length(f) < rayon) {
        float tache = hasard(floor(n / 2.0) + vec2(3.0, 3.0));
        return vec2(hasard(floor(cel) + vec2(1.0, 1.0)) < 0.3 ? 22.0 : 0.0, 3.4 + c * 1.4 + (tache - 0.5) * 1.6 * fin + (f.y > 0.0 ? 0.6 : -0.3) * fin);
    }
    return vec2(2.0, 2.6 + (hasard(floor(n / 2.0) + vec2(41.0, 3.0)) - 0.5) * 0.9 + (g - 0.5) * 0.6);
}

vec3 materiau(float altitude, float temperature, float pluie, float eau, vec4 couverture, float g, float pente, float niveau) {
    float jeu = (couverture.r - 0.5) * 2.0 + (g - 0.5) * 0.5;
    float t = temperature + jeu * 2.5;
    float p = pluie * (1.0 + jeu * 0.15);
    float metres = altitude * 1000.0;
    if (metres < niveau) {
        if (t < -9.0) return vec3(7.0, 4.5, 1.2);
        float mer = 6.6 - log2(1.0 + niveau - metres) * 0.75;
        float estuaire = eau * (1.0 - smoothstep(0.0, ESTUAIRE, niveau - metres));
        return vec3(13.0, mix(mer, 5.0, estuaire), mix(0.5, 0.6, estuaire));

    }
    if (eau > 0.5 + (g - 0.5) * 0.2) return vec3(13.0, 5.0, 0.6);
    if (u_biomes < 0.5) {
        if (altitude - u_plaines > 1.2) return vec3(temperature < -5.0 ? 6.0 : 5.0, 4.0, 1.5);
        return vec3(altitude - u_plaines > 0.4 ? 2.0 : 0.0, 4.0, 1.5);
    }
    if (metres < 2.0 + couverture.g * 4.0 + (g - 0.5) * 1.5 && t > -2.0) return vec3(3.0, 4.0, 1.8);
    if (altitude - u_plaines > 1.2 + jeu * 0.05) return vec3(t < -5.0 ? 6.0 : 5.0, 3.8, 2.2);
    if (pente > 0.8 + jeu * 0.1) return vec3(5.0, 3.5, 2.2);
    float ligne;
    float base = 3.6;
    if (t < -8.0) ligne = 7.0;
    else if (t < 0.0) ligne = 11.0;
    else if (t < 8.0) ligne = p < 200.0 ? 11.0 : 10.0;
    else if (t < 20.0) ligne = p < 250.0 ? 3.0 : (p < 550.0 ? 0.0 : 9.0);
    else if (p < 250.0) ligne = 12.0;
    else if (p < 600.0) ligne = 1.0;
    else {
        ligne = 0.0;
        base = 2.6;
    }
    bool herbe = ligne == 0.0 || ligne == 1.0 || ligne == 10.0;
    if (herbe && couverture.g + (g - 0.5) * 0.12 > 0.74) return vec3(2.0, 3.4, 2.0);
    return vec3(ligne, base, 2.2);
}

void main() {
    if (v_hauteur <= 0.0) {
        discard;
    }
    vec2 q = (gl_FragCoord.xy - u_centre) / u_rayon;
    float distance2 = dot(q, q);
    if (distance2 >= 1.0) {
        discard;
    }
    vec2 n = floor(gl_FragCoord.xy) + u_camera;
    vec4 d = texture2D(u_donnees, coin(v_texture));
    vec4 couverture = texture2D(u_couverture, coin(v_texture));
    float g = grainSphere(normalize(v_direction) * u_densite);
    if (u_plat > 0.0) g = mix(g, grain(n), u_plat);


    vec2 pasU = vec2(1.0 / TEXELS, 0.0);
    vec2 pasV = vec2(0.0, 1.0 / TEXELS);
    float hu = (altitudeEn(v_texture + pasU) - altitudeEn(v_texture - pasU)) * 0.5 * TEXELS;
    float hv = (altitudeEn(v_texture + pasV) - altitudeEn(v_texture - pasV)) * 0.5 * TEXELS;
    vec2 dx = dFdx(v_texture);
    vec2 dy = dFdy(v_texture);
    vec2 pente = vec2(hu * dx.x + hv * dx.y, hu * dy.x + hv * dy.y) * 1000.0 / u_metresPixel;
    float bosse = 0.0;
    float rocheux = smoothstep(0.5, 1.1, d.r - u_plaines);
    if (rocheux > 0.0 && d.a < 0.5 && u_metresPixel < 4.0 && u_plat > 0.99) {
        vec2 ici = (gl_FragCoord.xy - u_centre) * u_metresPixel;
        float amplitude = AMPLITUDE_MICRO * rocheux;
        float h0 = micro(ici);
        bosse = amplitude * h0;
        vec2 gradient = vec2(micro(ici + vec2(0.3, 0.0)) - h0, micro(ici + vec2(0.0, 0.3)) - h0) * amplitude / 0.3;
        pente += gradient * clamp((4.0 - u_metresPixel) / 2.0, 0.0, 1.0);
    }
    vec2 uvRue = vec2(-1.0);
    vec3 aplani = vec3(0.0);
    vec4 rue = vec4(0.0);
    bool surEau = d.a > 0.5 || d.r * 1000.0 < u_maree;
    if (u_rueActif > 0.5) {
        vec2 o = (gl_FragCoord.xy - u_centre) * u_metresPixel;
        uvRue = (u_rueCamera + vec2(u_rotation.x * o.x + u_rotation.y * o.y, -u_rotation.y * o.x + u_rotation.x * o.y)) / (u_rueTexel * TAILLE_RUE) + 0.5;
        if (uvRue.x > 0.0 && uvRue.y > 0.0 && uvRue.x < 1.0 && uvRue.y < 1.0) {
            aplani = solRue(uvRue);
            rue = texture2D(u_rues, uvRue);
            if (surEau && rue.a > 0.02) aplani.y = 0.0;
        } else uvRue = vec2(-1.0);
    }
    if (u_mode > 0.5) {
        float sol = max(d.r * 1000.0, 0.0) + bosse;
        if (aplani.y > 0.0) sol = mix(sol, aplani.x, aplani.y);
        float code = floor((sol - u_referenceMetres) * 256.0 + 8388608.5);
        code = clamp(code, 0.0, 16777215.0);
        float r = floor(code / 65536.0);
        float v = floor((code - r * 65536.0) / 256.0);
        float b = code - r * 65536.0 - v * 256.0;
        gl_FragColor = vec4(r / 255.0, v / 255.0, b / 255.0, 1.0);
        return;
    }

        float detail = clamp((2.0 - u_metresPixel) / 1.4, 0.0, 1.0);
    float vagues = clamp((12.0 - u_metresPixel) / 8.0, 0.0, 1.0);
    vec2 p = (n + 0.5) * u_metresPixel;
    float metres = d.r * 1000.0;
    float niveau = u_maree;
    if (vagues > 0.0) {
        float phase = worley(p / 40.0, 0.0).x * 4.0;
        niveau += HOULE * vagues * (0.5 + 0.5 * sin(u_temps * TAU / 6.0 + phase));
    }

    vec3 m = materiau(d.r, d.g, d.b, d.a, couverture, g, length(pente), niveau);
    if (m.x == 13.0 || m.x == 14.0) rue = vec4(0.0);
    float frange = (hasard(floor(n / 2.0) + vec2(3.0, 41.0)) - 0.5) * 0.35;
    bool pave = max(rue.g, rue.b) + frange > 0.5;
    bool battue = !pave && rue.r + frange > 0.5;
    if (pave) m = vec3(PIERRE_RUE, 3.6, 0.5);
    else if (battue) m = vec3(3.0, 2.3, 0.6);
    float lumiere = d.r < 0.0 || m.x == 13.0 ? 0.0 : clamp(-dot(pente, LUMIERE) * RELIEF, -2.5, 2.5) * (1.0 - aplani.y);
    float indice = m.y + (g - 0.5) * m.z * 2.0 + lumiere;
    // Lot J1 : les champs. La texture dit la sorte, le sens des sillons et la distance au bord ; l'année dit l'étape.
    float fauche = 0.0;
    if (uvRue.x > 0.0 && !pave && !battue && m.x != 13.0 && m.x != 14.0 && m.x != 6.0 && m.x != 5.0) {
        vec2 bouge = (vec2(hasard(floor(n / 2.0) + vec2(5.0, 19.0)), hasard(floor(n / 2.0) + vec2(23.0, 7.0))) - 0.5) * 0.8 / TAILLE_RUE;
        vec4 champ = texture2D(u_champs, uvRue + bouge);
        float bord = champ.a * 25.5;
        if (bord > 0.3) {
            float code = floor(champ.g * 255.0 + 0.5);
            float sorte = floor(code / 32.0);
            float variante = (code - sorte * 32.0) / 32.0;
            float angle = champ.b * PI;
            vec2 ec = (gl_FragCoord.xy - u_centre) * u_metresPixel;
            vec2 pos = u_rueCamera + vec2(u_rotation.x * ec.x + u_rotation.y * ec.y, -u_rotation.y * ec.x + u_rotation.x * ec.y) + u_champDecalage;
            vec2 sens = vec2(cos(angle), sin(angle));
            float le = dot(pos, sens);
            float tr = dot(pos, vec2(-sens.y, sens.x));
            float an = fract(u_annee + (variante - 0.5) * 0.08);
            float fin = clamp((0.32 - u_metresPixel) / 0.12, 0.0, 1.0);
            vec2 r = vec2(-1.0);
            if (sorte < 0.5) r = cereale(an, le, tr, n, p, g, fin);
            else if (sorte < 1.5) r = legumes(an, le, tr, n, g, fin);
            else if (sorte < 2.5) {
                // Le pré : de l'herbe, fauchée au début de l'été ; le foin sèche en andains, puis l'herbe repousse.
                m = vec3(0.0, 3.8, 1.6);
                fauche = an > 0.33 && an < 0.6 ? (an < 0.48 ? 0.75 : 0.75 * (0.6 - an) / 0.12) : 0.0;
                if (an > 0.36 && an < 0.46 && fract(tr / 3.0) < 0.16) r = vec2(18.0, 4.4 + (tiges(n) - 0.5) * 1.4);
                else indice = m.y + (g - 0.5) * 1.2 + lumiere;
            } else if (sorte < 3.5) {
                m = vec3(0.0, 3.6, 1.6);
                indice = m.y + (g - 0.5) * 1.2 + lumiere;
            } else {
                // La friche : de grandes herbes sèches, où l'on devine encore les anciens sillons.
                m = vec3(1.0, 3.4, 1.8);
                indice = m.y + (g - 0.5) * 1.6 + lumiere + (fract(tr / SILLON) < 0.3 ? 0.4 : 0.0) * fin;
            }
            if (r.x >= 0.0) {
                if (bord < 0.55 && sorte < 1.5) r = vec2(2.0, 2.0 + (g - 0.5) * 0.6);
                m.x = r.x;
                indice = r.y + lumiere * 0.5;
            }
        }
    }
    if (pave) {
        vec2 w = worley(p / 0.32, 0.0);
        indice += w.y - w.x < 0.12 ? -1.8 : (hasard(floor(p / 0.32) + vec2(9.0, 4.0)) - 0.5) * 0.9 + 0.3;
    } else if (battue) {
        indice += (hasard(floor(n / 3.0) + vec2(11.0, 5.0)) - 0.5) * 0.6 - rue.r * 0.3;
    }
    if ((pave || battue) && aplani.z > 0.35) {
        bool fond = aplani.z > 0.75;
        m.x = 5.0;
        indice = fond ? 1.3 + (g - 0.5) * 0.6 : 3.3 + (hasard(floor(p / 0.25) + vec2(2.0, 8.0)) - 0.5) * 0.9;
        if (fond && u_mouille > 0.15) {
            m.x = 13.0;
            indice = 3.0 + 1.5 * step(0.85, fract((p.x + p.y) * 0.6 - u_temps * 1.5));
        }
    }
    vec3 trace = vec3(0.0);
    if (u_metresPixel < 0.3) {
        vec2 o = (gl_FragCoord.xy - u_centre) * u_metresPixel;
        vec2 uv = (u_traceCamera + vec2(u_rotation.x * o.x + u_rotation.y * o.y, -u_rotation.y * o.x + u_rotation.x * o.y)) / (TEXEL_TRACE * 512.0) + 0.5;
        if (uv.x > 0.0 && uv.y > 0.0 && uv.x < 1.0 && uv.y < 1.0) trace = texture2D(u_traces, uv).rgb;
    }
    float usure = trace.r * 1.2 + trace.b * 2.0 + trace.g * 0.5;
    float ecrase = max(clamp(usure * 1.5, 0.0, 1.0), fauche);
    float arrache = clamp((usure - 0.25) * 1.3, 0.0, 1.0);
    bool pre = m.x == 0.0 || m.x == 1.0 || m.x == 8.0 || m.x == 10.0 || m.x == 11.0;
    if (pre && u_metresPixel < SEUIL_HERBE && u_plat > 0.99) {
        float h = brin(n, ecrase, arrache);
        if (h >= 0.0) {
            indice = m.y + 0.2 + h * 2.4 + lumiere * 0.6 + (g - 0.5) * 0.6;
            if (h > 0.99 && hasard(n + vec2(131.0, 7.0)) < 0.025) {
                m.x = 15.0;
                indice = 6.0;
            }
        } else {
            indice = m.y - 1.4 + (g - 0.5) * 0.8 + lumiere * 0.4;
        }
    }

    if (pre && usure > 0.1) {
        indice -= min(usure, 1.0) * 0.7;
        if (usure + (g - 0.5) * 0.25 + (hasard(floor(n / 3.0) + vec2(5.0, 77.0)) - 0.5) * 0.15 > 0.8) {
            m.x = 2.0;
            indice = 3.0 + (g - 0.5) * 1.2 + lumiere * 0.5;
        }
    }

    vec2 courant = couverture.ba * 2.0 - 1.0;
    float estuaire = d.a * (1.0 - smoothstep(0.0, ESTUAIRE, max(niveau - metres, 0.0)));
    bool riviere = m.x == 13.0 && dot(courant, courant) > 0.09 && (metres >= niveau || estuaire > 0.3);

    if (riviere && vagues > 0.0) {
        float det = dx.x * dy.y - dy.x * dx.y;
        vec2 sens = normalize(vec2(dy.y * courant.x - dy.x * courant.y, -dx.y * courant.x + dx.x * courant.y) / det);
        vec2 travers = vec2(-sens.y, sens.x);
        float leLong = dot(p, sens) - u_temps * VITESSE_RIVIERE;
        float enTravers = dot(p, travers);
        vec2 w = worley(vec2(leLong / 50.0, enTravers / 8.0), u_temps * TAU / 8.0);
        float force = metres >= niveau ? 1.0 : estuaire;
        if (vagues > 0.5 && w.x < 0.25 * force) {
                        indice = m.y + 1.0 + (g - 0.5) * 0.4;
        }

    }
    if (m.x == 13.0 && !riviere && detail > 0.0) {

        float lent = u_temps * TAU / 20.0;
        vec2 q = p / 2.5 + 0.6 * vec2(sin(p.y * 0.31 + lent), cos(p.x * 0.27 - lent));
        vec2 w = worley(q, u_temps * TAU / 10.0);
        float caustique = 1.0 - smoothstep(0.03, 0.08, w.y - w.x);
        indice += 1.2 * caustique * detail;
    }
    if (m.x == 13.0 && metres < niveau && vagues > 0.0) {
        float profondeur = niveau - metres;
        float casse = worley(p / 7.0, 0.0).x;
        float bande = fract(profondeur / 0.25 + u_temps / 4.0);
        bool creste = u_metresPixel < 2.5 && bande < 0.1 + 0.12 * casse && profondeur < 0.4 + 0.8 * casse;
        bool bord = profondeur < 0.02 + 0.05 * casse;
        bool calme = d.a + (casse - 0.5) * 0.3 > 0.3;
        if (vagues > 0.5 && !calme && (creste || bord)) {
            m.x = 14.0;
            indice = 4.0 + (g - 0.5) * 2.0 + (bord ? 1.0 : 0.0);
        }

    }

    if ((m.x == 13.0 || m.x == 14.0) && metres < niveau && niveau - metres < DEPASSE && u_metresPixel < SEUIL_HERBE && u_plat > 0.99) {
        vec3 sol = materiau(d.r, d.g, d.b, d.a, couverture, g, length(pente), -1000.0);
        if (sol.x == 0.0 || sol.x == 1.0 || sol.x == 8.0 || sol.x == 10.0 || sol.x == 11.0) {
            float h = brin(n, 0.0, 0.0);
            if (h > (niveau - metres) / DEPASSE) {
                m = sol;
                indice = sol.y + 0.2 + h * 2.4 + (g - 0.5) * 0.6;
            }
        }
    }



    float eau = m.x == 13.0 || m.x == 14.0 ? 1.0 : 0.0;
    if (surEau && eau > 0.5 && aplani.y > 0.02) {
        if (aplani.y > 0.85) {
            vec2 w = worley(p / 0.32, 0.0);
            m.x = PIERRE_RUE;
            indice = 3.6 + (g - 0.5) * 0.5 + (w.y - w.x < 0.12 ? -1.8 : (hasard(floor(p / 0.32) + vec2(9.0, 4.0)) - 0.5) * 0.9 + 0.3);
        } else {
            vec2 b = vec2(p.x / 0.6 + floor(p.y / 0.3) * 0.5, p.y / 0.3);
            vec2 f = fract(b);
            m.x = 5.0;
            indice = f.x < 0.1 || f.y < 0.15 ? 1.6 : 3.0 + (hasard(floor(b) + vec2(21.0, 9.0)) - 0.5) * 1.0;
        }
        eau = 0.0;
    }
    float proche = clamp((3.0 - u_metresPixel) / 1.5, 0.0, 1.0) * u_plat;
    if (eau == 0.0 && proche > 0.0) {
        vec2 taches = worley(p / 4.0, 0.0);
        if (u_neigeSol > 0.0 && u_neigeSol * 1.4 - taches.x * 0.9 - (g - 0.5) * 0.3 > 0.35) {
            m.x = 6.0;
            indice = 4.2 + (g - 0.5) * 1.2 + lumiere * 0.8 - trace.g * 2.2 - trace.r * 0.8;
        } else if (u_mouille > 0.0) {
            vec2 creux = worley(p / 3.0 + 17.0 + 0.22 * vec2(sin(p.y * 1.3 + p.x * 0.4), cos(p.x * 1.1 - p.y * 0.5)), 0.0);
            float flaque = (u_mouille - 0.4) * (pre ? 0.22 : 0.4);
            if (u_mouille > 0.4 && length(pente) < 0.3 && creux.x < flaque) {
                m.x = 13.0;
                indice = (creux.x > flaque * 0.7 ? 2.0 : 3.6) + (g - 0.5) * 0.6;
            } else {
                indice -= 1.1 * u_mouille + trace.g * 1.4 * u_mouille;
                if (trace.r > 0.35 && u_mouille > 0.3) {
                    m.x = 2.0;
                    indice = 1.8 + (g - 0.5) * 0.8;
                }
            }
        } else if (!pre) {
            indice -= trace.g * 0.6;
        }
    }

    float colonne = clamp(floor(indice + 0.5), 0.0, 7.0);
    vec3 couleur = texture2D(u_palette, vec2((colonne + 0.5) / 8.0, (m.x + 0.5) / 24.0)).rgb;
    float ombre = mix(0.45, 1.0, sqrt(sqrt(1.0 - distance2)));

    vec3 sortie = couleur * ombre;
    sortie.b = (floor(sortie.b * 127.5) * 2.0 + eau) / 255.0;
    gl_FragColor = vec4(sortie, 1.0);

}
