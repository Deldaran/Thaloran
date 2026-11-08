attribute vec2 a_position;
attribute vec2 a_texCoord0;
attribute float a_hauteur;
attribute vec3 a_direction;

varying vec2 v_texture;
varying float v_hauteur;
varying vec3 v_direction;

void main() {
    v_texture = a_texCoord0;
    v_hauteur = a_hauteur;
    v_direction = a_direction;
    gl_Position = vec4(a_position, 0.0, 1.0);
}
