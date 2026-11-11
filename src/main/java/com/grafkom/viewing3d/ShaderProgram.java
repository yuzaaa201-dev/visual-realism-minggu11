package com.grafkom.viewing3d;

import org.joml.Matrix4f;
import org.joml.Vector3fc;
import org.lwjgl.system.MemoryStack;

import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL20.*;

/** Compiles a GLSL program from classpath resources and caches uniform locations. */
final class ShaderProgram implements AutoCloseable {
    private final int programId;
    private final Map<String, Integer> uniforms = new HashMap<>();

    ShaderProgram(String vertexResource, String fragmentResource) {
        int vertex = compile(GL_VERTEX_SHADER, readResource(vertexResource), vertexResource);
        int fragment = compile(GL_FRAGMENT_SHADER, readResource(fragmentResource), fragmentResource);
        programId = glCreateProgram();
        glAttachShader(programId, vertex);
        glAttachShader(programId, fragment);
        glLinkProgram(programId);
        if (glGetProgrami(programId, GL_LINK_STATUS) == GL_FALSE) {
            String log = glGetProgramInfoLog(programId);
            glDeleteProgram(programId);
            glDeleteShader(vertex);
            glDeleteShader(fragment);
            throw new IllegalStateException("GLSL link failed: " + log);
        }
        glDetachShader(programId, vertex);
        glDetachShader(programId, fragment);
        glDeleteShader(vertex);
        glDeleteShader(fragment);
    }

    void use() { glUseProgram(programId); }

    void setInt(String name, int value) { glUniform1i(location(name), value); }
    void setFloat(String name, float value) { glUniform1f(location(name), value); }
    void setBoolean(String name, boolean value) { setInt(name, value ? 1 : 0); }
    void setVector3(String name, Vector3fc value) {
        glUniform3f(location(name), value.x(), value.y(), value.z());
    }
    void setMatrix4(String name, Matrix4f value) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer buffer = stack.mallocFloat(16);
            value.get(buffer);
            glUniformMatrix4fv(location(name), false, buffer);
        }
    }

    private int location(String name) {
        return uniforms.computeIfAbsent(name, key -> {
            int result = glGetUniformLocation(programId, key);
            if (result < 0) throw new IllegalArgumentException("GLSL uniform not found: " + key);
            return result;
        });
    }

    private static int compile(int type, String source, String resource) {
        int shader = glCreateShader(type);
        glShaderSource(shader, source);
        glCompileShader(shader);
        if (glGetShaderi(shader, GL_COMPILE_STATUS) == GL_FALSE) {
            String log = glGetShaderInfoLog(shader);
            glDeleteShader(shader);
            throw new IllegalStateException("GLSL compile failed for " + resource + ":\n" + log);
        }
        return shader;
    }

    private static String readResource(String path) {
        try (InputStream input = ShaderProgram.class.getResourceAsStream(path)) {
            if (input == null) throw new IllegalArgumentException("Shader resource is missing: " + path);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read shader resource: " + path, exception);
        }
    }

    @Override public void close() { glDeleteProgram(programId); }
}
