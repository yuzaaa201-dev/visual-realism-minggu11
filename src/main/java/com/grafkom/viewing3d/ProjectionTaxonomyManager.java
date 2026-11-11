package com.grafkom.viewing3d;

/** Interactive parallel and perspective projection taxonomy. */
public final class ProjectionTaxonomyManager {
    public enum ProjectionType {
        ORTHOGRAPHIC(1, "Orthographic", "Parallel projection"),
        ISOMETRIC(2, "Isometric", "Parallel axonometric"),
        OBLIQUE_CAVALIER(3, "Oblique Cavalier", "Full-depth oblique"),
        OBLIQUE_CABINET(4, "Oblique Cabinet", "Half-depth oblique"),
        PERSPECTIVE_1POINT(5, "Perspective 1-Point", "One vanishing point"),
        PERSPECTIVE_2POINT(6, "Perspective 2-Point", "Two vanishing points"),
        PERSPECTIVE_3POINT(7, "Perspective 3-Point", "Three vanishing points");

        private final int key;
        private final String label;
        private final String description;

        ProjectionType(int key, String label, String description) {
            this.key = key;
            this.label = label;
            this.description = description;
        }

        public int getKey() { return key; }
        public String getLabel() { return label; }
        public String getDescription() { return description; }

        public static ProjectionType fromKey(int key) {
            for (ProjectionType type : values()) {
                if (type.key == key) return type;
            }
            return null;
        }
    }

    private ProjectionTaxonomyManager() { }

    /** Applies a useful camera orientation for each demonstrative projection. */
    public static void applyProjection(ProjectionType type, Camera3D camera) {
        switch (type) {
            case ORTHOGRAPHIC:
                camera.setPreset(38.0f, 27.0f);
                break;
            case ISOMETRIC:
                camera.setPreset(45.0f, 35.264f);
                break;
            case OBLIQUE_CAVALIER:
            case OBLIQUE_CABINET:
                camera.setPreset(8.0f, 7.0f);
                break;
            case PERSPECTIVE_1POINT:
                camera.setPreset(7.0f, 4.0f);
                break;
            case PERSPECTIVE_2POINT:
                camera.setPreset(45.0f, 3.0f);
                break;
            case PERSPECTIVE_3POINT:
                camera.setPreset(42.0f, 24.0f);
                break;
            default:
                throw new IllegalArgumentException("Unsupported projection: " + type);
        }
    }

    public static PerspectiveProjection.ProjectionPoint project(
            ProjectionType type, Vec3 viewPoint, PerspectiveProjection perspective,
            int width, int height, float eyeOffset, float convergenceDistance) {
        if (width <= 0 || height <= 0) return null;
        if (isPerspective(type)) {
            return perspective.project(viewPoint, width, height, eyeOffset, convergenceDistance);
        }

        float x = viewPoint.x;
        float y = viewPoint.y;
        if (type == ProjectionType.OBLIQUE_CAVALIER || type == ProjectionType.OBLIQUE_CABINET) {
            float depthScale = type == ProjectionType.OBLIQUE_CAVALIER ? 1.0f : 0.5f;
            float depth = -viewPoint.z * depthScale;
            float obliqueAngle = (float) Math.toRadians(45.0);
            x += depth * (float) Math.cos(obliqueAngle);
            y += depth * (float) Math.sin(obliqueAngle);
        }

        float scale = Math.min(width, height) * 0.235f;
        return new PerspectiveProjection.ProjectionPoint(
                width * 0.5f + x * scale,
                height * 0.5f - y * scale,
                -viewPoint.z);
    }

    public static boolean isPerspective(ProjectionType type) {
        return type == ProjectionType.PERSPECTIVE_1POINT
                || type == ProjectionType.PERSPECTIVE_2POINT
                || type == ProjectionType.PERSPECTIVE_3POINT;
    }
}
