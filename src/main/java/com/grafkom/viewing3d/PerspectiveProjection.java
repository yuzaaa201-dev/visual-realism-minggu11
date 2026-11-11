package com.grafkom.viewing3d;

/** Perspective frustum and clip-space projection, including off-axis stereo. */
public final class PerspectiveProjection {
    private float fovDegree;
    private float aspectRatio;
    private final float zNear;
    private final float zFar;

    public PerspectiveProjection(float fovDegree, float aspectRatio, float zNear, float zFar) {
        this.fovDegree = clamp(fovDegree, 28.0f, 82.0f);
        this.aspectRatio = Math.max(0.1f, aspectRatio);
        this.zNear = Math.max(0.01f, zNear);
        this.zFar = Math.max(this.zNear + 1.0f, zFar);
    }

    public float[] calculateProjectionMatrix() {
        return calculateProjectionMatrix(0.0f, 4.8f);
    }

    /**
     * Builds a right-handed asymmetric perspective matrix. eyeOffset shifts the
     * frustum so both stereo eyes converge on the same zero-parallax plane.
     */
    public float[] calculateProjectionMatrix(float eyeOffset, float convergenceDistance) {
        float f = 1.0f / (float) Math.tan(Math.toRadians(fovDegree) / 2.0);
        float[] projection = new float[16];
        projection[0] = f / aspectRatio;
        projection[5] = f;
        projection[10] = -(zFar + zNear) / (zFar - zNear);
        projection[14] = -(2.0f * zFar * zNear) / (zFar - zNear);
        projection[11] = -1.0f;
        projection[15] = 0.0f;

        if (Math.abs(eyeOffset) > 1.0e-6f) {
            float convergence = Math.max(zNear + 0.1f, convergenceDistance);
            projection[8] = -f * eyeOffset / (aspectRatio * convergence);
        }
        return projection;
    }

    public ProjectionPoint project(Vec3 viewPoint, int width, int height,
                                   float eyeOffset, float convergenceDistance) {
        setAspectRatio((float) width / Math.max(1, height));
        float depth = -viewPoint.z;
        if (depth < zNear || depth > zFar) {
            return null;
        }

        float[] projection = calculateProjectionMatrix(eyeOffset, convergenceDistance);
        float clipX = projection[0] * viewPoint.x + projection[8] * viewPoint.z;
        float clipY = projection[5] * viewPoint.y;
        float clipW = -viewPoint.z;
        if (clipW <= 1.0e-5f) {
            return null;
        }

        float normalizedX = clipX / clipW;
        float normalizedY = clipY / clipW;
        return new ProjectionPoint(
                width * (0.5f + normalizedX * 0.5f),
                height * (0.5f - normalizedY * 0.5f),
                depth);
    }

    public void setFovDegree(float fovDegree) {
        this.fovDegree = clamp(fovDegree, 28.0f, 82.0f);
    }

    public void setAspectRatio(float aspectRatio) {
        this.aspectRatio = Math.max(0.1f, aspectRatio);
    }

    public float getFovDegree() {
        return fovDegree;
    }

    public float getZNear() {
        return zNear;
    }

    public float getZFar() {
        return zFar;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    public static final class ProjectionPoint {
        public final float x;
        public final float y;
        public final float depth;

        ProjectionPoint(float x, float y, float depth) {
            this.x = x;
            this.y = y;
            this.depth = depth;
        }
    }
}
