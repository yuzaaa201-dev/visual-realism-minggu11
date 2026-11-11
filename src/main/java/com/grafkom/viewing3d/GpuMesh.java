package com.grafkom.viewing3d;

import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

/** Interleaved position/normal/UV mesh stored in OpenGL vertex and index buffers. */
final class GpuMesh implements AutoCloseable {
    private static final int FLOATS_PER_VERTEX = 8;
    private final int vao;
    private final int vbo;
    private final int ebo;
    private final int indexCount;

    private GpuMesh(float[] vertices, int[] indices) {
        vao = glGenVertexArrays();
        vbo = glGenBuffers();
        ebo = glGenBuffers();
        indexCount = indices.length;

        FloatBuffer vertexBuffer = BufferUtils.createFloatBuffer(vertices.length);
        vertexBuffer.put(vertices).flip();
        IntBuffer indexBuffer = BufferUtils.createIntBuffer(indices.length);
        indexBuffer.put(indices).flip();

        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, vertexBuffer, GL_STATIC_DRAW);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indexBuffer, GL_STATIC_DRAW);
        int stride = FLOATS_PER_VERTEX * Float.BYTES;
        glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0L);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(1, 3, GL_FLOAT, false, stride, 3L * Float.BYTES);
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(2, 2, GL_FLOAT, false, stride, 6L * Float.BYTES);
        glEnableVertexAttribArray(2);
        glBindVertexArray(0);
    }

    void draw() {
        glBindVertexArray(vao);
        glDrawElements(GL_TRIANGLES, indexCount, GL_UNSIGNED_INT, 0L);
        glBindVertexArray(0);
    }

    static GpuMesh sphereSmooth(int sectors, int stacks) {
        Builder builder = new Builder();
        for (int stack = 0; stack <= stacks; stack++) {
            float v = (float) stack / stacks;
            float theta = (float) (Math.PI * v);
            float y = (float) Math.cos(theta);
            float ring = (float) Math.sin(theta);
            for (int sector = 0; sector <= sectors; sector++) {
                float u = (float) sector / sectors;
                float phi = (float) (Math.PI * 2.0 * u);
                Vector3f normal = new Vector3f(ring * (float) Math.cos(phi), y,
                        ring * (float) Math.sin(phi));
                builder.vertex(normal.x, normal.y, normal.z, normal.x, normal.y, normal.z, u, v);
            }
        }
        for (int stack = 0; stack < stacks; stack++) {
            for (int sector = 0; sector < sectors; sector++) {
                int topLeft = stack * (sectors + 1) + sector;
                int bottomLeft = topLeft + sectors + 1;
                builder.triangle(topLeft, bottomLeft + 1, bottomLeft);
                builder.triangle(topLeft, topLeft + 1, bottomLeft + 1);
            }
        }
        return builder.build();
    }

    /** Duplicates each triangle's vertices and assigns its geometric face normal. */
    static GpuMesh sphereFlat(int sectors, int stacks) {
        Builder builder = new Builder();
        Vector3f[][] points = new Vector3f[stacks + 1][sectors + 1];
        for (int stack = 0; stack <= stacks; stack++) {
            float v = (float) stack / stacks;
            float theta = (float) (Math.PI * v);
            for (int sector = 0; sector <= sectors; sector++) {
                float u = (float) sector / sectors;
                float phi = (float) (Math.PI * 2.0 * u);
                points[stack][sector] = new Vector3f(
                        (float) Math.sin(theta) * (float) Math.cos(phi),
                        (float) Math.cos(theta),
                        (float) Math.sin(theta) * (float) Math.sin(phi));
            }
        }
        for (int stack = 0; stack < stacks; stack++) {
            for (int sector = 0; sector < sectors; sector++) {
                Vector3f p00 = points[stack][sector];
                Vector3f p01 = points[stack][sector + 1];
                Vector3f p10 = points[stack + 1][sector];
                Vector3f p11 = points[stack + 1][sector + 1];
                addFlatTriangle(builder, p00, p11, p10,
                        (float) sector / sectors, (float) stack / stacks,
                        (float) (sector + 1) / sectors, (float) (stack + 1) / stacks,
                        (float) sector / sectors, (float) (stack + 1) / stacks);
                addFlatTriangle(builder, p00, p01, p11,
                        (float) sector / sectors, (float) stack / stacks,
                        (float) (sector + 1) / sectors, (float) stack / stacks,
                        (float) (sector + 1) / sectors, (float) (stack + 1) / stacks);
            }
        }
        return builder.build();
    }

    static GpuMesh cube() {
        Builder builder = new Builder();
        face(builder, new Vector3f(0, 0, 1),
                p(-0.5f, -0.5f, 0.5f), p(0.5f, -0.5f, 0.5f),
                p(0.5f, 0.5f, 0.5f), p(-0.5f, 0.5f, 0.5f));
        face(builder, new Vector3f(0, 0, -1),
                p(0.5f, -0.5f, -0.5f), p(-0.5f, -0.5f, -0.5f),
                p(-0.5f, 0.5f, -0.5f), p(0.5f, 0.5f, -0.5f));
        face(builder, new Vector3f(1, 0, 0),
                p(0.5f, -0.5f, 0.5f), p(0.5f, -0.5f, -0.5f),
                p(0.5f, 0.5f, -0.5f), p(0.5f, 0.5f, 0.5f));
        face(builder, new Vector3f(-1, 0, 0),
                p(-0.5f, -0.5f, -0.5f), p(-0.5f, -0.5f, 0.5f),
                p(-0.5f, 0.5f, 0.5f), p(-0.5f, 0.5f, -0.5f));
        face(builder, new Vector3f(0, 1, 0),
                p(-0.5f, 0.5f, 0.5f), p(0.5f, 0.5f, 0.5f),
                p(0.5f, 0.5f, -0.5f), p(-0.5f, 0.5f, -0.5f));
        face(builder, new Vector3f(0, -1, 0),
                p(-0.5f, -0.5f, -0.5f), p(0.5f, -0.5f, -0.5f),
                p(0.5f, -0.5f, 0.5f), p(-0.5f, -0.5f, 0.5f));
        return builder.build();
    }

    static GpuMesh ground(float halfExtent) {
        Builder builder = new Builder();
        Vector3f up = new Vector3f(0, 1, 0);
        int a = builder.vertex(-halfExtent, -0.65f, -halfExtent, up.x, up.y, up.z, 0, 0);
        int b = builder.vertex(-halfExtent, -0.65f, halfExtent, up.x, up.y, up.z, 0, 8);
        int c = builder.vertex(halfExtent, -0.65f, halfExtent, up.x, up.y, up.z, 8, 8);
        int d = builder.vertex(halfExtent, -0.65f, -halfExtent, up.x, up.y, up.z, 8, 0);
        builder.triangle(a, b, c);
        builder.triangle(a, c, d);
        return builder.build();
    }

    private static Vector3f p(float x, float y, float z) { return new Vector3f(x, y, z); }

    private static void face(Builder builder, Vector3f normal,
                             Vector3f a, Vector3f b, Vector3f c, Vector3f d) {
        int ia = builder.vertex(a.x, a.y, a.z, normal.x, normal.y, normal.z, 0, 0);
        int ib = builder.vertex(b.x, b.y, b.z, normal.x, normal.y, normal.z, 1, 0);
        int ic = builder.vertex(c.x, c.y, c.z, normal.x, normal.y, normal.z, 1, 1);
        int id = builder.vertex(d.x, d.y, d.z, normal.x, normal.y, normal.z, 0, 1);
        builder.triangle(ia, ib, ic);
        builder.triangle(ia, ic, id);
    }

    private static void addFlatTriangle(Builder builder, Vector3fc a, Vector3fc b, Vector3fc c,
                                        float u0, float v0, float u1, float v1,
                                        float u2, float v2) {
        Vector3f normal = new Vector3f(b).sub(a).cross(new Vector3f(c).sub(a)).normalize();
        int ia = builder.vertex(a.x(), a.y(), a.z(), normal.x, normal.y, normal.z, u0, v0);
        int ib = builder.vertex(b.x(), b.y(), b.z(), normal.x, normal.y, normal.z, u1, v1);
        int ic = builder.vertex(c.x(), c.y(), c.z(), normal.x, normal.y, normal.z, u2, v2);
        builder.triangle(ia, ib, ic);
    }

    @Override public void close() {
        glDeleteBuffers(vbo);
        glDeleteBuffers(ebo);
        glDeleteVertexArrays(vao);
    }

    private static final class Builder {
        private final List<Float> vertices = new ArrayList<>();
        private final List<Integer> indices = new ArrayList<>();

        int vertex(float x, float y, float z, float nx, float ny, float nz, float u, float v) {
            int index = vertices.size() / FLOATS_PER_VERTEX;
            vertices.add(x); vertices.add(y); vertices.add(z);
            vertices.add(nx); vertices.add(ny); vertices.add(nz);
            vertices.add(u); vertices.add(v);
            return index;
        }

        void triangle(int a, int b, int c) {
            indices.add(a); indices.add(b); indices.add(c);
        }

        GpuMesh build() {
            float[] vertexArray = new float[vertices.size()];
            for (int i = 0; i < vertexArray.length; i++) vertexArray[i] = vertices.get(i);
            int[] indexArray = new int[indices.size()];
            for (int i = 0; i < indexArray.length; i++) indexArray[i] = indices.get(i);
            return new GpuMesh(vertexArray, indexArray);
        }
    }
}
