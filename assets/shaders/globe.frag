#ifdef GL_ES
precision highp float;
#endif

uniform sampler2D u_carte;
uniform mat3 u_repere;
uniform vec2 u_centre;
uniform float u_rayon;
uniform float u_opacite;

const float PI = 3.14159265;
const float FACES = 6.0;

vec2 versFace(float a, float b) {
    return vec2(atan(a), atan(b)) * (4.0 / PI);
}

vec2 coordonneesCarte(vec3 d) {
    vec3 p = abs(d);
    float face;
    vec2 uv;
    if (p.x >= p.y && p.x >= p.z) {
        face = d.x > 0.0 ? 0.0 : 1.0;
        uv = versFace(d.y / p.x, d.z / p.x);
    } else if (p.y >= p.z) {
        face = d.y > 0.0 ? 2.0 : 3.0;
        uv = versFace(d.x / p.y, d.z / p.y);
    } else {
        face = d.z > 0.0 ? 4.0 : 5.0;
        uv = versFace(d.x / p.z, d.y / p.z);
    }
    uv = uv * 0.5 + 0.5;
    return vec2((face + uv.x) / FACES, uv.y);
}

void main() {
    vec2 q = (gl_FragCoord.xy - u_centre) / u_rayon;
    float distance2 = dot(q, q);
    if (distance2 >= 1.0) {
        if (u_opacite < 1.0) {
            discard;
        }
        gl_FragColor = vec4(0.0, 0.0, 0.0, 1.0);
        return;
    }
    float hauteur = sqrt(1.0 - distance2);
    vec3 direction = u_repere * vec3(q, hauteur);
    vec4 texel = texture2D(u_carte, coordonneesCarte(direction));
    vec3 couleur = texel.rgb;
    float ombre = mix(0.45, 1.0, sqrt(hauteur));
    float bord = clamp((1.0 - sqrt(distance2)) * u_rayon, 0.0, 1.0);
    if (u_opacite < 1.0) {
        gl_FragColor = vec4(couleur, u_opacite * texel.a * bord);
        return;
    }
    gl_FragColor = vec4(couleur * ombre * bord, 1.0);
}
