package com.grafkom.viewing3d;

/** Orbital camera with a manually calculated right-handed LookAt matrix. */
public final class Camera3D {
    private Vec3 target = new Vec3(0, 0, 0);
    private Vec3 worldUp = new Vec3(0, 1, 0);
    private float radius;
    private float azimuthDegrees;
    private float elevationDegrees;

    public Camera3D(float radius, float azimuthDegrees, float elevationDegrees) {
        this.radius = clamp(radius, 3.2f, 12.0f);
        setAngles(azimuthDegrees, elevationDegrees);
    }

    public float[] calculateLookAtMatrix() {
        return calculateLookAtMatrix(0.0f);
    }

    /** Returns the column-major OpenGL-style view matrix for a stereo eye offset. */
    public float[] calculateLookAtMatrix(float eyeOffset) {
        Vec3 eye = getEyePosition();
        Vec3 n = eye.subtract(target).normalized();
        Vec3 u = worldUp.cross(n).normalized();
        if (u.length() < 1.0e-5f) {
            u = new Vec3(1, 0, 0);
        }
        Vec3 v = n.cross(u).normalized();
        Vec3 shiftedEye = eye.add(u.scale(eyeOffset));

        float[] matrix = new float[16];
        matrix[0] = u.x;
        matrix[4] = u.y;
        matrix[8] = u.z;
        matrix[12] = -u.dot(shiftedEye);

        matrix[1] = v.x;
        matrix[5] = v.y;
        matrix[9] = v.z;
        matrix[13] = -v.dot(shiftedEye);

        matrix[2] = n.x;
        matrix[6] = n.y;
        matrix[10] = n.z;
        matrix[14] = -n.dot(shiftedEye);

        matrix[15] = 1.0f;
        return matrix;
    }

    public Vec3 transformToView(Vec3 point, float[] viewMatrix) {
        float x = point.x;
        float y = point.y;
        float z = point.z;
        return new Vec3(
                viewMatrix[0] * x + viewMatrix[4] * y + viewMatrix[8] * z + viewMatrix[12],
                viewMatrix[1] * x + viewMatrix[5] * y + viewMatrix[9] * z + viewMatrix[13],
                viewMatrix[2] * x + viewMatrix[6] * y + viewMatrix[10] * z + viewMatrix[14]);
    }

    public Vec3 getEyePosition() {
        double azimuth = Math.toRadians(azimuthDegrees);
        double elevation = Math.toRadians(elevationDegrees);
        float horizontal = (float) (radius * Math.cos(elevation));
        return target.add(new Vec3(
                horizontal * (float) Math.sin(azimuth),
                radius * (float) Math.sin(elevation),
                horizontal * (float) Math.cos(azimuth)));
    }

    public void orbit(float deltaX, float deltaY) {
        setAngles(azimuthDegrees + deltaX * 0.42f, elevationDegrees - deltaY * 0.42f);
    }

    public void zoom(float wheelRotation) {
        radius = clamp(radius * (float) Math.pow(1.12, wheelRotation), 3.2f, 12.0f);
    }

    public void setAngles(float azimuth, float elevation) {
        azimuthDegrees = wrapDegrees(azimuth);
        elevationDegrees = clamp(elevation, -78.0f, 78.0f);
    }

    public void setPreset(float azimuth, float elevation) {
        setAngles(azimuth, elevation);
    }

    public void reset() {
        radius = 6.6f;
        target = new Vec3(0, 0, 0);
        worldUp = new Vec3(0, 1, 0);
        setAngles(38.0f, 27.0f);
    }

    public float getRadius() {
        return radius;
    }

    public float getAzimuthDegrees() {
        return azimuthDegrees;
    }

    public float getElevationDegrees() {
        return elevationDegrees;
    }

    private static float wrapDegrees(float value) {
        float wrapped = value % 360.0f;
        return wrapped < 0 ? wrapped + 360.0f : wrapped;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
