#version 300 es
precision highp float;
in vec3 vPosition;
in vec3 vNormal;
uniform vec3 uAccent;
uniform float uVariant;
out vec4 fragColor;
void main(){
    vec3 n=normalize(vNormal),view=normalize(vec3(0.0,0.0,7.0)-vPosition);
    vec3 key=normalize(vec3(-3.0,5.0,4.0)-vPosition),rim=normalize(vec3(3.0,-1.0,1.0)-vPosition);
    float diffuse=max(dot(n,key),0.0),specular=pow(max(dot(n,normalize(key+view)),0.0),90.0);
    float fresnel=pow(1.0-max(dot(n,view),0.0),3.5),edge=pow(max(dot(n,rim),0.0),7.0);
    vec3 base=mix(vec3(0.035,0.075,0.11),uAccent*0.32,uVariant);
    vec3 color=base*(0.3+diffuse*0.7)+specular*vec3(0.73,0.88,0.94)+fresnel*uAccent*0.72+edge*vec3(0.23,0.20,0.46)*0.62;
    color=mix(color,vec3(0.022,0.036,0.063),clamp(-vPosition.z*0.06,0.0,0.55));
    fragColor=vec4(pow(color,vec3(0.86)),1.0);
}
