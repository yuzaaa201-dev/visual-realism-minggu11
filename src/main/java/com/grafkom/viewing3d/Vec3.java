package com.grafkom.viewing3d;

/** Small immutable vector used by the self-contained software renderer. */
final class Vec3 {
    final float x;
    final float y;
    final float z;

    Vec3(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    Vec3 add(Vec3 other) {
        return new Vec3(x + other.x, y + other.y, z + other.z);
    }

    Vec3 subtract(Vec3 other) {
        return new Vec3(x - other.x, y - other.y, z - other.z);
    }

    Vec3 scale(float amount) {
        return new Vec3(x * amount, y * amount, z * amount);
    }

    float dot(Vec3 other) {
        return x * other.x + y * other.y + z * other.z;
    }

    Vec3 cross(Vec3 other) {
        return new Vec3(
                y * other.z - z * other.y,
                z * other.x - x * other.z,
                x * other.y - y * other.x);
    }

    float length() {
        return (float) Math.sqrt(dot(this));
    }

    Vec3 normalized() {
        float length = length();
        if (length < 1.0e-6f) {
            return new Vec3(0, 0, 0);
        }
        return scale(1.0f / length);
    }

    Vec3 rotateX(float radians) {
        float c = (float) Math.cos(radians);
        float s = (float) Math.sin(radians);
        return new Vec3(x, y * c - z * s, y * s + z * c);
    }

    Vec3 rotateY(float radians) {
        float c = (float) Math.cos(radians);
        float s = (float) Math.sin(radians);
        return new Vec3(x * c + z * s, y, -x * s + z * c);
    }
}
