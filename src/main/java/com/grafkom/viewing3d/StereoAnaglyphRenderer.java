package com.grafkom.viewing3d;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

/** Two-pass off-axis stereo renderer with red/cyan channel masking. */
public final class StereoAnaglyphRenderer {
    @FunctionalInterface
    public interface EyeRenderCallback {
        void render(Graphics2D graphics, float eyeOffset, boolean leftEye);
    }

    private boolean enabled;
    private float ipd = 0.18f;
    private float convergenceDistance = 4.8f;
    private BufferedImage output;
    private BufferedImage leftPass;
    private BufferedImage rightPass;

    public BufferedImage render(int width, int height, EyeRenderCallback callback) {
        int safeWidth = Math.max(1, width);
        int safeHeight = Math.max(1, height);
        ensureBuffers(safeWidth, safeHeight);

        if (!enabled) {
            Graphics2D graphics = output.createGraphics();
            try {
                callback.render(graphics, 0.0f, false);
            } finally {
                graphics.dispose();
            }
            return output;
        }

        Graphics2D leftGraphics = leftPass.createGraphics();
        try {
            callback.render(leftGraphics, -ipd * 0.5f, true);
        } finally {
            leftGraphics.dispose();
        }
        Graphics2D rightGraphics = rightPass.createGraphics();
        try {
            callback.render(rightGraphics, ipd * 0.5f, false);
        } finally {
            rightGraphics.dispose();
        }

        int[] leftPixels = ((DataBufferInt) leftPass.getRaster().getDataBuffer()).getData();
        int[] rightPixels = ((DataBufferInt) rightPass.getRaster().getDataBuffer()).getData();
        int[] outputPixels = ((DataBufferInt) output.getRaster().getDataBuffer()).getData();
        for (int i = 0; i < outputPixels.length; i++) {
            int red = leftPixels[i] & 0x00FF0000;
            int cyan = rightPixels[i] & 0x0000FFFF;
            outputPixels[i] = 0xFF000000 | red | cyan;
        }
        return output;
    }

    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public boolean isEnabled() { return enabled; }

    public void setIpd(float ipd) {
        this.ipd = Math.max(0.0f, Math.min(0.40f, ipd));
    }

    public float getIpd() { return ipd; }

    public void setConvergenceDistance(float distance) {
        convergenceDistance = Math.max(1.0f, Math.min(20.0f, distance));
    }

    public float getConvergenceDistance() { return convergenceDistance; }

    private void ensureBuffers(int width, int height) {
        if (output != null && output.getWidth() == width && output.getHeight() == height) return;
        output = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        leftPass = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        rightPass = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    }
}
