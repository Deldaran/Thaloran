#ifdef GL_ES
precision highp float;
#endif

uniform sampler2D u_texture;
uniform vec2 u_taille;
uniform float u_echelle;
uniform vec2 u_case;
uniform float u_temps;
uniform float u_vent;
uniform vec2 u_ventDir;
uniform float u_neige;

uniform float u_masque;
uniform vec4 u_silhouette;

varying vec4 v_color;
varying vec2 v_texCoords;

vec4 lire(vec2 p) {
    vec2 base = floor(p);
    vec2 f = clamp((fract(p) - 0.5) * u_echelle + 0.5, 0.0, 1.0);
    return texture2D(u_texture, (base + f + 0.5) / u_taille);
}

vec4 finir(vec4 c) {
    if (u_masque > 0.5 && c.a < 0.5) discard;
    if (u_silhouette.a > 0.0) {
        if (c.a < 0.5) discard;
        return u_silhouette;
    }
    return c;
}

bool cape(vec4 c) {
    return abs(c.a * 255.0 - 254.0) < 0.5;
}

void main() {
    vec2 p = v_texCoords * u_taille - 0.5;
    float force = v_color.g;
    vec2 cellule = floor(v_texCoords * u_taille / u_case);
    if (v_color.b > 0.5) {
        vec2 local = v_texCoords * u_taille - cellule * u_case;
        if (v_color.b > 0.9) {
            float brin = floor(fract(sin(floor(local.x) * 12.9898) * 43758.5453) * 4.0);
            if (local.y > u_case.y - 7.0 - brin) discard;
        }
        float bas = clamp((local.y / u_case.y - 0.3) / 0.6, 0.0, 1.0);
        float phase = v_color.r * 6.2831853;
        float onde = sin(u_temps * (2.2 + u_vent * 0.1) + phase - local.y * 0.12) + 0.4 * sin(u_temps * 5.3 + phase * 3.0 - local.y * 0.3);
        float souffle = clamp(u_vent / 15.0, 0.0, 1.0);
        float pousse = bas * clamp(0.4 + souffle * 1.4 + (0.6 + souffle * 0.8) * onde, -1.5, 3.0);
        vec2 sens = length(u_ventDir) > 0.01 ? u_ventDir : vec2(1.0, 0.0);
        vec2 decalage = floor(vec2(sens.x, -sens.y * 0.6) * pousse + 0.5);
        vec2 q = vec2(clamp(p.x - decalage.x, cellule.x * u_case.x, (cellule.x + 1.0) * u_case.x - 1.0), clamp(p.y - decalage.y, cellule.y * u_case.y, (cellule.y + 1.0) * u_case.y - 1.0));
        vec4 c1 = lire(q);
        vec4 c0 = lire(p);
        vec4 c = cape(c1) ? c1 : cape(c0) ? lire(p + vec2(0.0, u_taille.y * 0.5)) : c0;
        if (c.a > 0.9) c.a = 1.0;
        gl_FragColor = finir(vec4(c.rgb, c.a * v_color.a));
        return;
    }
    if (force > 0.0) {
        float y = v_texCoords.y * u_taille.y - cellule.y * u_case.y;
        float h = 1.0 - y / u_case.y;
        float phase = v_color.r * 6.2831853;
        float onde = sin(u_temps * (1.1 + u_vent * 0.06) + phase) + 0.35 * sin(u_temps * 2.9 + phase * 2.0);
        float decalage = floor((0.5 + u_vent * 0.18) * force * h * h * onde + 0.5);
        p.x = clamp(p.x - decalage, cellule.x * u_case.x, (cellule.x + 1.0) * u_case.x - 1.0);
    }
    vec2 base = floor(p);
    vec4 c = lire(p);
    if (u_neige > 0.0 && force > 0.0 && c.a > 0.5) {
        float a1 = texture2D(u_texture, (base + vec2(0.5, -0.5)) / u_taille).a;
        float a2 = texture2D(u_texture, (base + vec2(0.5, -1.5)) / u_taille).a;
        float a3 = texture2D(u_texture, (base + vec2(0.5, -2.5)) / u_taille).a;
        float couche = a1 < 0.5 ? 1.0 : a2 < 0.5 ? 0.85 : a3 < 0.5 && u_neige > 0.5 ? 0.6 : 0.0;
        c.rgb = mix(c.rgb, vec3(0.9, 0.93, 0.98), couche * clamp(u_neige * 1.5, 0.0, 1.0));
    }
    gl_FragColor = finir(vec4(c.rgb, c.a * v_color.a));
}
