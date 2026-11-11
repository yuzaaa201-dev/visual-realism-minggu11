package com.grafkom.viewing3d;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE1;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER_SRGB;
import static org.lwjgl.system.MemoryUtil.NULL;

/** Interactive LWJGL 3 / OpenGL 3.3 visual realism lab. */
public final class VisualRealismSuite {
    private static final int START_WIDTH = 1360;
    private static final int START_HEIGHT = 850;
    private static final Vector3f LIGHT_POSITION = new Vector3f(-3.5f, 6.0f, 4.0f);
    private static final Vector3f LIGHT_COLOR = new Vector3f(1.0f, 0.96f, 0.88f);

    private long window;
    private int framebufferWidth = START_WIDTH;
    private int framebufferHeight = START_HEIGHT;
    private boolean depthTest = true;
    private boolean backFaceCulling = true;
    private boolean flatShading;
    private boolean blinnPhong;
    private boolean textures = true;
    private boolean shadows = true;
    private boolean animated;
    private boolean dragging;
    private boolean firstCursor = true;
    private double previousMouseX;
    private double previousMouseY;
    private float yaw = 42.0f;
    private float pitch = 24.0f;
    private float cameraRadius = 6.3f;
    private long titleUpdateTime;
    private int renderedFrames;
    private float fps;

    private ShaderProgram lightingShader;
    private ShaderProgram depthShader;
    private GpuMesh sphereSmooth;
    private GpuMesh sphereFlat;
    private GpuMesh cube;
    private GpuMesh ground;
    private Texture2D brickTexture;
    private ShadowMapFBO shadowMap;

    private VisualRealismSuite() { }

    public static void main(String[] args) {
        new VisualRealismSuite().run();
    }

    private void run() {
        try {
            initialize();
            renderLoop();
        } catch (RuntimeException exception) {
            exception.printStackTrace(System.err);
            throw exception;
        } finally {
            cleanup();
        }
    }

    private void initialize() {
        GLFWErrorCallback.createPrint(System.err).set();
        if (!glfwInit()) throw new IllegalStateException("GLFW initialization failed.");
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        glfwWindowHint(GLFW_SRGB_CAPABLE, GLFW_TRUE);

        window = glfwCreateWindow(START_WIDTH, START_HEIGHT,
                "3D Visual Realism Suite", NULL, NULL);
        if (window == NULL) throw new IllegalStateException("Could not create an OpenGL 3.3 window.");
        glfwMakeContextCurrent(window);
        GL.createCapabilities();
        glfwSwapInterval(1);
        glfwSetFramebufferSizeCallback(window, (handle, width, height) -> {
            framebufferWidth = Math.max(1, width);
            framebufferHeight = Math.max(1, height);
            glViewport(0, 0, framebufferWidth, framebufferHeight);
        });
        glfwSetKeyCallback(window, (handle, key, scanCode, action, modifiers) -> handleKey(key, action));
        glfwSetMouseButtonCallback(window, (handle, button, action, modifiers) -> {
            if (button == GLFW_MOUSE_BUTTON_LEFT) {
                dragging = action == GLFW_PRESS;
                firstCursor = true;
            }
        });
        glfwSetCursorPosCallback(window, (handle, x, y) -> handleMouse(x, y));
        glfwSetScrollCallback(window, (handle, xOffset, yOffset) -> {
            cameraRadius = clamp(cameraRadius * (float) Math.exp(-yOffset * 0.10f), 3.0f, 13.0f);
        });

        glViewport(0, 0, START_WIDTH, START_HEIGHT);
        glEnable(GL_FRAMEBUFFER_SRGB);
        HSRManager.setupDepthTest(true);
        HSRManager.setupBackFaceCulling(true);
        glClearColor(0.035f, 0.050f, 0.075f, 1.0f);

        lightingShader = new ShaderProgram("/shaders/phong.vert", "/shaders/phong.frag");
        depthShader = new ShaderProgram("/shaders/depth.vert", "/shaders/depth.frag");
        sphereSmooth = GpuMesh.sphereSmooth(48, 32);
        sphereFlat = GpuMesh.sphereFlat(48, 32);
        cube = GpuMesh.cube();
        ground = GpuMesh.ground(8.0f);
        brickTexture = new Texture2D("/textures/brick.png");
        shadowMap = new ShadowMapFBO();

        titleUpdateTime = System.nanoTime();
        glfwShowWindow(window);
        updateWindowTitle();
    }

    private void renderLoop() {
        long lastFrame = System.nanoTime();
        float rotation = 0.42f;
        while (!glfwWindowShouldClose(window)) {
            long now = System.nanoTime();
            float delta = Math.min((now - lastFrame) / 1_000_000_000.0f, 0.05f);
            lastFrame = now;
            if (animated) rotation += delta * 0.42f;

            renderFrame(rotation);
            glfwSwapBuffers(window);
            glfwPollEvents();
            updateFps(now);
        }
    }

    private void renderFrame(float rotation) {
        Vector3f cameraPosition = cameraPosition();
        Matrix4f view = new Matrix4f().lookAt(cameraPosition, new Vector3f(0, 0, 0),
                new Vector3f(0, 1, 0));
        Matrix4f projection = new Matrix4f().perspective((float) Math.toRadians(45.0),
                (float) framebufferWidth / framebufferHeight, 0.1f, 60.0f);
        Matrix4f lightSpace = new Matrix4f()
                .ortho(-6.0f, 6.0f, -6.0f, 6.0f, 1.0f, 18.0f)
                .mul(new Matrix4f().lookAt(LIGHT_POSITION, new Vector3f(0, 0, 0),
                        new Vector3f(0, 1, 0)));

        Matrix4f floorModel = new Matrix4f();
        Matrix4f sphereModel = new Matrix4f().translate(0.05f, 0.40f, 0.0f)
                .rotateY(rotation).scale(1.05f);
        Matrix4f cubeModel = new Matrix4f().translate(-1.75f, -0.14f, 0.25f)
                .rotateY(-rotation * 0.7f).rotateX(0.18f).scale(1.02f);

        shadowMap.bindForWriting();
        HSRManager.setupDepthTest(true);
        HSRManager.setupBackFaceCulling(backFaceCulling);
        glEnable(GL_POLYGON_OFFSET_FILL);
        glPolygonOffset(2.0f, 4.0f);
        glClear(GL_DEPTH_BUFFER_BIT);
        depthShader.use();
        depthShader.setMatrix4("lightSpaceMatrix", lightSpace);
        drawDepth(ground, floorModel);
        drawDepth(cube, cubeModel);
        drawDepth(flatShading ? sphereFlat : sphereSmooth, sphereModel);
        glDisable(GL_POLYGON_OFFSET_FILL);
        shadowMap.unbind(framebufferWidth, framebufferHeight);

        HSRManager.setupDepthTest(depthTest);
        HSRManager.setupBackFaceCulling(backFaceCulling);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        lightingShader.use();
        lightingShader.setMatrix4("view", view);
        lightingShader.setMatrix4("projection", projection);
        lightingShader.setMatrix4("lightSpaceMatrix", lightSpace);
        lightingShader.setVector3("lightPos", LIGHT_POSITION);
        lightingShader.setVector3("viewPos", cameraPosition);
        lightingShader.setVector3("lightColor", LIGHT_COLOR);
        lightingShader.setInt("textureSampler", 0);
        lightingShader.setInt("shadowMap", 1);
        lightingShader.setBoolean("useTexture", textures);
        lightingShader.setBoolean("useBlinnPhong", blinnPhong);
        lightingShader.setBoolean("useShadows", shadows);
        brickTexture.bind(0);
        shadowMap.bindForReading(1);
        drawLit(ground, floorModel, new Vector3f(0.62f, 0.67f, 0.76f));
        drawLit(flatShading ? sphereFlat : sphereSmooth, sphereModel,
                new Vector3f(0.98f, 0.78f, 0.60f));
        drawLit(cube, cubeModel, new Vector3f(0.96f, 0.64f, 0.44f));
    }

    private void drawDepth(GpuMesh mesh, Matrix4f model) {
        depthShader.setMatrix4("model", model);
        mesh.draw();
    }

    private void drawLit(GpuMesh mesh, Matrix4f model, Vector3f color) {
        lightingShader.setMatrix4("model", model);
        lightingShader.setVector3("objectColor", color);
        mesh.draw();
    }

    private void handleKey(int key, int action) {
        if (action != GLFW_PRESS) return;
        switch (key) {
            case GLFW_KEY_ESCAPE: glfwSetWindowShouldClose(window, true); break;
            case GLFW_KEY_B: blinnPhong = !blinnPhong; break;
            case GLFW_KEY_F: flatShading = !flatShading; break;
            case GLFW_KEY_Z: depthTest = !depthTest; break;
            case GLFW_KEY_C: backFaceCulling = !backFaceCulling; break;
            case GLFW_KEY_T: textures = !textures; break;
            case GLFW_KEY_M: brickTexture.toggleFiltering(); break;
            case GLFW_KEY_S: shadows = !shadows; break;
            case GLFW_KEY_A: animated = !animated; break;
            case GLFW_KEY_R: resetCamera(); break;
            default: return;
        }
        updateWindowTitle();
    }

    private void handleMouse(double x, double y) {
        if (!dragging) return;
        if (firstCursor) {
            previousMouseX = x;
            previousMouseY = y;
            firstCursor = false;
            return;
        }
        yaw += (float) (x - previousMouseX) * 0.28f;
        pitch = clamp(pitch - (float) (y - previousMouseY) * 0.28f, -70.0f, 78.0f);
        previousMouseX = x;
        previousMouseY = y;
    }

    private Vector3f cameraPosition() {
        float yawRadians = (float) Math.toRadians(yaw);
        float pitchRadians = (float) Math.toRadians(pitch);
        float horizontal = cameraRadius * (float) Math.cos(pitchRadians);
        return new Vector3f(horizontal * (float) Math.sin(yawRadians),
                cameraRadius * (float) Math.sin(pitchRadians),
                horizontal * (float) Math.cos(yawRadians));
    }

    private void resetCamera() {
        yaw = 42.0f;
        pitch = 24.0f;
        cameraRadius = 6.3f;
    }

    private void updateFps(long now) {
        renderedFrames++;
        if (now - titleUpdateTime >= 1_000_000_000L) {
            fps = renderedFrames * 1_000_000_000.0f / Math.max(1L, now - titleUpdateTime);
            renderedFrames = 0;
            titleUpdateTime = now;
            updateWindowTitle();
        }
    }

    private void updateWindowTitle() {
        if (window == NULL) return;
        String title = String.format("3D Visual Realism | %s | %s | A:%s Z:%s C:%s T:%s M:%s S:%s | %.0f FPS",
                blinnPhong ? "Blinn-Phong" : "Phong",
                flatShading ? "Flat" : "Smooth",
                animated ? "ON" : "OFF",
                depthTest ? "ON" : "OFF", backFaceCulling ? "ON" : "OFF",
                textures ? "ON" : "OFF", brickTexture == null ? "MIPMAP" : brickTexture.filteringName(),
                shadows ? "ON" : "OFF", fps);
        glfwSetWindowTitle(window, title);
    }

    private void cleanup() {
        if (window != NULL) {
            if (shadowMap != null) shadowMap.close();
            if (brickTexture != null) brickTexture.close();
            if (ground != null) ground.close();
            if (cube != null) cube.close();
            if (sphereFlat != null) sphereFlat.close();
            if (sphereSmooth != null) sphereSmooth.close();
            if (depthShader != null) depthShader.close();
            if (lightingShader != null) lightingShader.close();
            glfwDestroyWindow(window);
            window = NULL;
        }
        glfwTerminate();
        GLFWErrorCallback callback = glfwSetErrorCallback(null);
        if (callback != null) callback.free();
    }

    private static float clamp(float value, float minimum, float maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
