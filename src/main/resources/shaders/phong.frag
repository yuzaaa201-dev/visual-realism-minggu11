#version 330 core
out vec4 FragColor;

in vec3 FragPos;
in vec3 Normal;
in vec2 TexCoords;
in vec4 FragPosLightSpace;

uniform vec3 lightPos;
uniform vec3 viewPos;
uniform vec3 lightColor;
uniform vec3 objectColor;
uniform sampler2D textureSampler;
uniform sampler2D shadowMap;
uniform bool useTexture;
uniform bool useBlinnPhong;
uniform bool useShadows;

float shadowVisibility(vec3 normal, vec3 lightDir) {
    vec3 projected = FragPosLightSpace.xyz / FragPosLightSpace.w;
    projected = projected * 0.5 + 0.5;
    if (projected.z > 1.0 || projected.x < 0.0 || projected.x > 1.0
            || projected.y < 0.0 || projected.y > 1.0) return 1.0;

    float bias = max(0.035 * (1.0 - dot(normal, lightDir)), 0.0035);
    vec2 texel = 1.0 / vec2(textureSize(shadowMap, 0));
    float shadow = 0.0;
    for (int x = -1; x <= 1; ++x) {
        for (int y = -1; y <= 1; ++y) {
            float closestDepth = texture(shadowMap, projected.xy + vec2(x, y) * texel).r;
            shadow += projected.z - bias > closestDepth ? 1.0 : 0.0;
        }
    }
    return 1.0 - shadow / 9.0;
}

void main() {
    vec3 baseColor = useTexture ? texture(textureSampler, TexCoords).rgb : objectColor;
    vec3 normal = normalize(Normal);
    vec3 lightDir = normalize(lightPos - FragPos);
    vec3 viewDir = normalize(viewPos - FragPos);

    float ambientStrength = 0.30;
    vec3 ambient = ambientStrength * lightColor;
    float diffuseFactor = max(dot(normal, lightDir), 0.0);
    vec3 diffuse = diffuseFactor * lightColor;

    float specularFactor = 0.0;
    if (diffuseFactor > 0.0) {
        if (useBlinnPhong) {
            vec3 halfwayDir = normalize(lightDir + viewDir);
            specularFactor = pow(max(dot(normal, halfwayDir), 0.0), 32.0);
        } else {
            vec3 reflectDir = reflect(-lightDir, normal);
            specularFactor = pow(max(dot(viewDir, reflectDir), 0.0), 16.0);
        }
    }
    vec3 specular = 0.90 * specularFactor * lightColor;
    float visibility = useShadows ? shadowVisibility(normal, lightDir) : 1.0;
    vec3 color = ambient * baseColor + visibility * (diffuse * baseColor + specular);
    FragColor = vec4(color, 1.0);
}
