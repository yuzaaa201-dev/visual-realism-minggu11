package com.grafkom.viewing3d;

import static org.lwjgl.opengl.GL11.*;

/** Centralizes the visible-surface controls: depth testing and back-face culling. */
final class HSRManager {
    private HSRManager() { }

    static void setupDepthTest(boolean enabled) {
        if (enabled) {
            glEnable(GL_DEPTH_TEST);
            glDepthFunc(GL_LESS);
        } else {
            glDisable(GL_DEPTH_TEST);
        }
    }

    static void setupBackFaceCulling(boolean enabled) {
        if (enabled) {
            glEnable(GL_CULL_FACE);
            glCullFace(GL_BACK);
            glFrontFace(GL_CCW);
        } else {
            glDisable(GL_CULL_FACE);
        }
    }
}
