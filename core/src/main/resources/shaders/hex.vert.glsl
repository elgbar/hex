attribute vec2 a_position;
attribute vec4 a_color;
uniform mat4 u_projTrans;
varying vec4 vColor;
void main() {
	vColor = a_color;
	gl_Position =  u_projTrans * vec4(a_position.xy, 0.0, 1.0);
}
