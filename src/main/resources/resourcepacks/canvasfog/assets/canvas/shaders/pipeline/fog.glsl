#include frex:shaders/api/fog.glsl
#include canvas:shaders/pipeline/options.glsl
#include frex:shaders/api/player.glsl
#include canvas:fog_config

/******************************************************
  canvas:shaders/pipeline/fog.glsl
******************************************************/

vec4 p_fogInner(vec4 diffuseColor) {
#if _CV_FOG_CONFIG == _CV_FOG_CONFIG_NONE
    if (frx_effectBlindness != 1) {
        return diffuseColor;
    }
#endif
    float fogFactor;
    if (frx_fogStart == -512.0) { // Exponential
        fogFactor = exp(-frx_fogEnd*frx_distance);
//        return vec4(mix(frx_fogColor.rgb, diffuseColor.rgb, fogValue * fogColor.a), inColor.a);
    } else if (frx_fogStart == -1024.0) { // Exponential ^2
        fogFactor = exp(-frx_fogEnd*pow(frx_distance, 2.0));
//        return vec4(mix(fogColor.rgb, inColor.rgb, fogValue * fogColor.a), inColor.a);
    } else {
        fogFactor = 1.0 - smoothstep(frx_fogStart, frx_fogEnd, frx_distance);
    }

    return vec4(mix(frx_fogColor.rgb, diffuseColor.rgb, fogFactor * frx_fogColor.a), diffuseColor.a);
}

vec4 p_fog(vec4 diffuseColor) {
    return frx_fogEnabled == 1 ? p_fogInner(diffuseColor) : diffuseColor;
}