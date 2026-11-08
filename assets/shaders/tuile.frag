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
uniform float u_palier;


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
const float TOUFFE = 5.0;
const float VIDE_HERBE = 0.12;
const float SEUIL_HERBE = 0.8;
const float PALIER = 10.0;

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


float brin(vec2 n) {
    float devant = 1e9;
    float hauteur = -1.0;
    vec2 c0 = floor(n / TOUFFE);
    for (int j = 0; j <= 2; j++) {
        for (int i = -1; i <= 1; i++) {
            vec2 c = c0 + vec2(float(i), -float(j));
            if (hasard(c + vec2(9.0, 41.0)) < VIDE_HERBE) continue;
            vec2 centre = floor(c * TOUFFE + vec2(hasard(c + vec2(17.0, 31.0)), hasard(c + vec2(71.0, 5.0))) * TOUFFE);
            float vent = 0.7 * sin(u_temps * 1.7 + centre.x * 0.13 + centre.y * 0.07);
            for (int b = 0; b < 5; b++) {
                float o = float(b) - 2.0;
                float h = hasard(c + vec2(3.0 + float(b) * 11.0, 97.0));
                vec2 racine = centre + vec2(o, floor(h * 2.0));
                float longueur = 2.0 + floor(h * 3.0) + 2.0 - abs(o);
                float k = n.y - racine.y;
                if (k < 0.0 || k >= longueur) continue;
                float penche = o * 0.7 + (h - 0.5) * 0.8 + vent;
                float x = racine.x + floor(penche * k / longueur + 0.5);
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
    float lumiere = d.r < 0.0 || m.x == 13.0 ? 0.0 : clamp(-dot(pente, LUMIERE) * RELIEF, -2.5, 2.5);
    float indice = m.y + (g - 0.5) * m.z * 2.0 + lumiere;
    bool pre = m.x == 0.0 || m.x == 1.0 || m.x == 8.0 || m.x == 10.0 || m.x == 11.0;
    if (pre && u_metresPixel < SEUIL_HERBE && u_plat > 0.99) {
        float h = brin(n);
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
            float h = brin(n);
            if (h > (niveau - metres) / DEPASSE) {
                m = sol;
                indice = sol.y + 0.2 + h * 2.4 + (g - 0.5) * 0.6;
            }
        }
    }



    float colonne = clamp(floor(indice + 0.5), 0.0, 7.0);
    vec3 couleur = texture2D(u_palette, vec2((colonne + 0.5) / 8.0, (m.x + 0.5) / 16.0)).rgb;
    float ombre = mix(0.45, 1.0, sqrt(sqrt(1.0 - distance2)));

    float palier = max(floor(metres / PALIER), 0.0);
    float rang = clamp(palier - u_palier, -63.0, 63.0) + 64.0;
    float eau = m.x == 13.0 || m.x == 14.0 ? 1.0 : 0.0;
    gl_FragColor = vec4(couleur * ombre, (rang * 2.0 + eau) / 255.0);

}
