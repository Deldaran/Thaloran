#ifdef GL_ES
precision highp float;
#endif

uniform sampler2D u_texture;
uniform sampler2D u_meteo;
uniform vec2 u_ecran;
uniform float u_rayon;
uniform float u_ecrasement;
uniform vec3 u_est;
uniform vec3 u_nord;
uniform vec3 u_centre;
uniform float u_pixel;
uniform float u_mpp;
uniform float u_art;
uniform vec2 u_decalage;
uniform vec2 u_rotation;

vec2 versMonde(vec2 o) {
    return vec2(u_rotation.x * o.x + u_rotation.y * o.y, -u_rotation.y * o.x + u_rotation.x * o.y);
}
uniform float u_temps;
uniform float u_loin;
uniform float u_local;
uniform float u_nuage;
uniform float u_pluie;
uniform float u_orage;
uniform float u_neige;
uniform float u_brouillard;
uniform float u_brume;
uniform vec2 u_vent;
uniform float u_lucioles;
uniform float u_moucherons;
uniform float u_pollen;
uniform float u_papillons;
uniform vec3 u_soleil;

varying vec4 v_color;
varying vec2 v_texCoords;

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

float bruit(vec2 p) {
    vec2 i = floor(p), f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    float a = hasard(i), b = hasard(i + vec2(1.0, 0.0)), c = hasard(i + vec2(0.0, 1.0)), d = hasard(i + vec2(1.0, 1.0));
    return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
}

float fbm(vec2 p) {
    return bruit(p) * 0.55 + bruit(p * 2.03 + 17.0) * 0.3 + bruit(p * 4.01 + 41.0) * 0.15;
}

float bruit3(vec3 p) {
    vec3 i = floor(p), f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    float a = mix(hasard3(i), hasard3(i + vec3(1.0, 0.0, 0.0)), f.x);
    float b = mix(hasard3(i + vec3(0.0, 1.0, 0.0)), hasard3(i + vec3(1.0, 1.0, 0.0)), f.x);
    float c = mix(hasard3(i + vec3(0.0, 0.0, 1.0)), hasard3(i + vec3(1.0, 0.0, 1.0)), f.x);
    float d = mix(hasard3(i + vec3(0.0, 1.0, 1.0)), hasard3(i + vec3(1.0, 1.0, 1.0)), f.x);
    return mix(mix(a, b, f.y), mix(c, d, f.y), f.z);
}

float fbm3(vec3 p) {
    return bruit3(p) * 0.55 + bruit3(p * 2.03 + 17.0) * 0.3 + bruit3(p * 4.01 + 41.0) * 0.15;
}

float bayer2(vec2 a) {
    a = floor(a);
    return fract(a.x / 2.0 + a.y * a.y * 0.75);
}

float bayer4(vec2 a) {
    return bayer2(0.5 * a) * 0.25 + bayer2(a);
}

void dessus(inout vec4 c, vec3 rgb, float a) {
    c.rgb = rgb * a + c.rgb * (1.0 - a);
    c.a = a + c.a * (1.0 - a);
}

vec4 eclairage(float e) {
    vec3 doree = vec3(1.0, 0.55, 0.25), crepuscule = vec3(0.16, 0.12, 0.32), nuit = vec3(0.02, 0.03, 0.1);
    if (e > 0.2) return vec4(0.0);
    if (e > 0.0) return vec4(doree, 0.18 * (1.0 - e / 0.2));
    if (e > -0.12) {
        float k = -e / 0.12;
        return vec4(mix(doree, crepuscule, k), mix(0.18, 0.5, k));
    }
    float k = clamp((-e - 0.12) / 0.12, 0.0, 1.0);
    return vec4(mix(crepuscule, nuit, k), mix(0.5, 0.66, k));
}

vec4 meteoEn(vec3 dir) {
    float lat = asin(clamp(dir.z, -1.0, 1.0));
    float lon = atan(dir.y, dir.x);
    return texture2D(u_meteo, vec2((lon + PI) / (2.0 * PI), 0.5 - lat / PI));
}

void main() {
    vec4 c = vec4(0.0);

    vec2 sc = (floor(gl_FragCoord.xy / u_pixel) + 0.5) * u_pixel;
    vec2 q = (sc - u_ecran * 0.5) / u_rayon;
    q.y /= u_ecrasement;
    float r2 = dot(q, q);
    if (u_loin > 0.0 || u_local < 1.0) {
        if (r2 < 1.0) {
            vec3 dir = normalize(u_est * q.x + u_nord * q.y + u_centre * sqrt(1.0 - r2));
            vec4 m = meteoEn(dir);
            vec4 mo = meteoEn(normalize(dir + (u_est - u_nord) * 0.012));
            float detail = fbm3(dir * 14.0 + vec3(u_temps * 0.004, 0.0, -u_temps * 0.003));
            float nuage = smoothstep(0.2, 0.55, m.r + (detail - 0.5) * 0.7);
            float ombre = smoothstep(0.2, 0.55, mo.r + (detail - 0.5) * 0.7);
            float lointain = 1.0 - u_local;
            dessus(c, vec3(0.03, 0.04, 0.08), ombre * 0.3 * lointain * (1.0 - nuage * u_loin));
            if (u_loin > 0.0) {
                float bord = pow(r2, 4.0);
                dessus(c, vec3(0.5, 0.65, 0.95), bord * 0.3 * u_loin);
                vec3 blanc = mix(vec3(0.93, 0.94, 0.96), vec3(0.5, 0.53, 0.6), clamp(m.g * 1.5, 0.0, 1.0));
                blanc *= 0.85 + 0.15 * detail;
                float neigeux = m.a < 0.5 ? 0.0 : 1.0;
                blanc = mix(blanc, blanc * vec3(0.95, 0.97, 1.0), neigeux);
                dessus(c, blanc, nuage * 0.88 * u_loin);
            }
        } else if (u_loin > 0.0) {
            float r = sqrt(r2);
            float halo = 1.0 - clamp((r - 1.0) / 0.05, 0.0, 1.0);
            dessus(c, vec3(0.45, 0.6, 0.95), halo * halo * 0.55 * u_loin);
        }
    }

    if (u_local > 0.0) {
        vec2 w = u_decalage + versMonde((gl_FragCoord.xy - u_ecran * 0.5) * u_mpp / vec2(1.0, u_ecrasement));
        vec2 a = floor(w / u_art);

        float passage = fbm((w - u_vent * u_temps) / 260.0);
        float ombre = smoothstep(0.3, 0.8, passage + u_nuage * 0.45 - 0.25) * u_nuage;
        float sombre = clamp(0.2 * u_nuage + 0.2 * u_pluie + 0.25 * u_orage + ombre * 0.3, 0.0, 0.7);
        dessus(c, vec3(0.05, 0.06, 0.1), sombre * u_local);

        float f1 = fbm(w / 22.0 + u_vent * u_temps * 0.02);
        float f2 = fbm(w / 12.0 - u_vent * u_temps * 0.03 + 31.0);
        float brume = u_brouillard * (0.2 + 0.9 * f1) + u_brume * smoothstep(0.55, 0.85, f2) * 0.35;
        brume = clamp(brume, 0.0, 0.7) * u_local;
        float tramee = floor(brume * 16.0 + bayer4(a)) / 16.0;
        dessus(c, vec3(0.72, 0.75, 0.79), tramee);

        if (u_pluie > 0.0) {
            vec2 cs = vec2(16.0, 10.0);
            vec2 cellule = floor(a / cs);
            float phase = hasard(cellule + vec2(3.0, 3.0));
            float cycle = u_temps / 0.45 + phase;
            float n = floor(cycle), age = fract(cycle);
            if (hasard(cellule + vec2(mod289(n * 7.0), 13.0)) < u_pluie * 0.6) {
                vec2 pos = floor(vec2(hasard(cellule + vec2(mod289(n), 1.0)), hasard(cellule + vec2(2.0, mod289(n)))) * (cs - 4.0)) + 2.0;
                vec2 d = a - cellule * cs - pos;
                float rr = floor(age * 3.0);
                bool goutte = (d.y == 0.0 && abs(d.x) == rr + 1.0) || (rr == 0.0 && d.x == 0.0 && d.y == 1.0) || (rr == 1.0 && abs(d.x) == 1.0 && d.y == 1.0);
                if (goutte) dessus(c, vec3(0.75, 0.8, 0.9), (1.0 - age) * 0.7 * u_local);
            }
            for (int k = 0; k < 3; k++) {
                float fk = float(k);
                float vitesse = 140.0 + 50.0 * fk;
                float longueur = 5.0 + 3.0 * fk;
                vec2 s = a;
                s.y += u_temps * vitesse;
                s.x += floor(s.y * u_vent.x / 40.0);
                vec2 taille = vec2(7.0 + 3.0 * fk, 34.0);
                vec2 cel = floor(s / taille);
                vec2 loc = s - cel * taille;
                if (hasard(cel + vec2(17.0 * fk, 3.0)) < u_pluie * (0.3 + 0.15 * fk)) {
                    float dx = floor(hasard(cel + vec2(5.0, 91.0)) * taille.x);
                    float y0 = floor(hasard(cel + vec2(41.0, 7.0)) * (taille.y - longueur));
                    if (floor(loc.x) == dx && loc.y >= y0 && loc.y < y0 + longueur) dessus(c, vec3(0.62, 0.68, 0.78), (0.35 + 0.12 * fk) * u_local);
                }
            }
        }

        if (u_neige > 0.0) {
            for (int k = 0; k < 3; k++) {
                float fk = float(k);
                vec2 taille = vec2(14.0 - 2.0 * fk);
                vec2 s = a;
                s.y += u_temps * (10.0 + 6.0 * fk);
                s.x += floor(sin(u_temps * 1.3 + s.y * 0.05 + fk) * 2.0 + u_vent.x * u_temps * 0.3);
                vec2 cel = floor(s / taille);
                vec2 loc = floor(s - cel * taille);
                if (hasard(cel + vec2(11.0 * fk, 7.0)) < u_neige * 0.7) {
                    vec2 p = floor(vec2(hasard(cel + vec2(1.0, 5.0)), hasard(cel + vec2(9.0, 2.0))) * (taille - 2.0));
                    vec2 d = loc - p;
                    float cote = k == 2 ? 2.0 : 1.0;
                    if (d.x >= 0.0 && d.y >= 0.0 && d.x < cote && d.y < cote) dessus(c, vec3(0.95, 0.96, 1.0), 0.9 * u_local);
                }
            }
        }

    }

    if (r2 < 1.0) {
        vec3 sol = normalize(u_est * q.x + u_nord * q.y + u_centre * sqrt(1.0 - r2));
        vec4 lumiere = eclairage(dot(sol, u_soleil));
        dessus(c, lumiere.rgb, lumiere.a);
    }

    if (u_local > 0.0) {
        vec2 w = u_decalage + versMonde((gl_FragCoord.xy - u_ecran * 0.5) * u_mpp);
        vec2 a = floor(w / u_art);
        float t = u_temps;
        if (u_lucioles > 0.0) {
            vec2 g = w / 3.0;
            vec2 cel = floor(g);
            float h = hasard(cel + vec2(7.0, 77.0));
            if (h < u_lucioles * 0.3) {
                vec2 centre = (cel + 0.5 + 0.3 * vec2(sin(t * 0.7 + h * 40.0), sin(t * 0.53 + h * 90.0))) * 3.0;
                vec2 d = (w - centre) / u_art;
                float pulse = 0.25 + 0.75 * max(0.0, sin(t * 2.2 + h * 60.0));
                float halo = exp(-dot(d, d) / 12.0) * 0.6 * pulse * u_local;
                c.rgb += vec3(0.7, 0.95, 0.3) * halo;
                c.a += halo * 0.2;
                if (a == floor(centre / u_art)) dessus(c, vec3(1.0, 1.0, 0.7), 0.95 * pulse * u_local);
            }
        }
        if (u_moucherons > 0.0) {
            vec2 cel = floor(w / 5.0);
            float h = hasard(cel + vec2(23.0, 5.0));
            if (h < u_moucherons * 0.35) {
                vec2 centre = (cel + 0.5) * 5.0 + 0.4 * vec2(sin(t * 0.6 + h * 9.0), cos(t * 0.5 + h * 7.0));
                for (int i = 0; i < 8; i++) {
                    float fi = float(i);
                    vec2 p = centre + 0.35 * vec2(sin(t * (5.0 + fi) * 1.3 + fi * 2.1 + h * 20.0), cos(t * (4.3 + fi) * 1.1 + fi * 1.7));
                    if (a == floor(p / u_art)) dessus(c, vec3(0.08, 0.08, 0.06), 0.85 * u_local);
                }
            }
        }
        if (u_pollen > 0.0) {
            vec2 gw = w - u_vent * t * 0.4;
            vec2 cel = floor(gw / 2.0);
            float h = hasard(cel + vec2(61.0, 19.0));
            if (h < u_pollen * 0.25) {
                vec2 p = (cel + 0.5 + 0.35 * vec2(sin(t * 0.4 + h * 30.0), sin(t * 0.6 + h * 50.0))) * 2.0;
                vec2 d = (gw - p) / u_art;
                float halo = exp(-dot(d, d) / 3.0) * 0.3 * u_local;
                c.rgb += vec3(1.0, 1.0, 0.85) * halo;
                c.a += halo * 0.3;
                if (floor(gw / u_art) == floor(p / u_art)) dessus(c, vec3(1.0, 0.98, 0.85), 0.7 * u_local);
            }
        }
        if (u_papillons > 0.0) {
            vec2 cel = floor(w / 7.0);
            float h = hasard(cel + vec2(31.0, 83.0));
            if (h < u_papillons * 0.25) {
                vec2 centre = (cel + 0.5 + 0.3 * vec2(sin(t * 0.45 + h * 10.0) + 0.5 * sin(t * 1.7 + h * 3.0), sin(t * 0.37 + h * 20.0) + 0.4 * sin(t * 2.3))) * 7.0;
                vec2 rel = a - floor(centre / u_art);
                float aile = sin(t * 18.0 + h * 30.0) > 0.0 ? 2.0 : 1.0;
                vec3 couleur = h < u_papillons * 0.08 ? vec3(0.9, 0.5, 0.15) : h < u_papillons * 0.16 ? vec3(0.95, 0.95, 0.9) : vec3(0.35, 0.55, 0.95);
                if (rel == vec2(0.0)) dessus(c, vec3(0.15, 0.1, 0.08), 0.95 * u_local);
                else if (rel.y == 0.0 && abs(rel.x) <= aile) dessus(c, couleur, 0.95 * u_local);
                else if (rel.y == 1.0 && abs(rel.x) == 1.0 && aile == 2.0) dessus(c, couleur, 0.95 * u_local);
            }
        }

        if (u_orage > 0.0) {
            float eclair = step(0.97, hasard(vec2(mod289(floor(u_temps * 6.0)), 3.0))) * u_orage;
            dessus(c, vec3(0.9, 0.92, 1.0), eclair * 0.45 * u_local);
        }
    }

    gl_FragColor = c * texture2D(u_texture, v_texCoords).a * v_color.a;
}
