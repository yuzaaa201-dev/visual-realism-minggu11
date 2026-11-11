package com.grafkom.viewing3d;

import java.awt.Color;

/** Polygon mesh used by the visualizer's animated color cube. */
public final class Mesh3D {
    private final Vec3[] vertices;
    private final Face[] faces;

    private Mesh3D(Vec3[] vertices, Face[] faces) {
        this.vertices = vertices;
        this.faces = faces;
    }

    public static Mesh3D createColorCube(float halfSize) {
        float s = halfSize;
        Vec3[] vertices = {
            new Vec3(-s, -s, -s), new Vec3(s, -s, -s),
            new Vec3(s, s, -s), new Vec3(-s, s, -s),
            new Vec3(-s, -s, s), new Vec3(s, -s, s),
            new Vec3(s, s, s), new Vec3(-s, s, s)
        };
        Face[] faces = {
            new Face(new int[]{4, 5, 6, 7}, new Color(41, 199, 210)),
            new Face(new int[]{0, 3, 2, 1}, new Color(222, 80, 118)),
            new Face(new int[]{0, 4, 7, 3}, new Color(125, 101, 238)),
            new Face(new int[]{1, 2, 6, 5}, new Color(247, 157, 76)),
            new Face(new int[]{3, 7, 6, 2}, new Color(122, 211, 136)),
            new Face(new int[]{0, 1, 5, 4}, new Color(69, 127, 225))
        };
        return new Mesh3D(vertices, faces);
    }

    Vec3[] vertices() { return vertices; }
    Face[] faces() { return faces; }

    static final class Face {
        final int[] indices;
        final Color color;

        Face(int[] indices, Color color) {
            this.indices = indices;
            this.color = color;
        }
    }
}
