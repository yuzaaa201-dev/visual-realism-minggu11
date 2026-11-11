package com.grafkom.viewing3d;

import org.lwjgl.BufferUtils;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL21.GL_SRGB8_ALPHA8;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL30.glGenerateMipmap;
import static org.lwjgl.stb.STBImage.*;

/** Loads an image resource through STBImage and configures repeat/mipmap filtering. */
final class Texture2D implements AutoCloseable {
    private final int id;
    private boolean mipmapped = true;

    Texture2D(String resourcePath) {
        id = glGenTextures();
        byte[] imageBytes = readBytes(resourcePath);
        ByteBuffer encoded = BufferUtils.createByteBuffer(imageBytes.length);
        encoded.put(imageBytes).flip();
        stbi_set_flip_vertically_on_load(true);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer width = stack.mallocInt(1);
            IntBuffer height = stack.mallocInt(1);
            IntBuffer channels = stack.mallocInt(1);
            ByteBuffer pixels = stbi_load_from_memory(encoded, width, height, channels, 4);
            if (pixels == null) {
                glDeleteTextures(id);
                throw new IllegalStateException("STBImage could not decode " + resourcePath + ": "
                        + stbi_failure_reason());
            }
            try {
                glBindTexture(GL_TEXTURE_2D, id);
                glTexImage2D(GL_TEXTURE_2D, 0, GL_SRGB8_ALPHA8, width.get(0), height.get(0), 0,
                        GL_RGBA, GL_UNSIGNED_BYTE, pixels);
                glGenerateMipmap(GL_TEXTURE_2D);
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
                setFiltering(true);
            } finally {
                stbi_image_free(pixels);
            }
        } finally {
            MemoryUtil.memFree(encoded);
        }
    }

    void bind(int textureUnit) {
        glActiveTexture(GL_TEXTURE0 + textureUnit);
        glBindTexture(GL_TEXTURE_2D, id);
    }

    void toggleFiltering() {
        setFiltering(!mipmapped);
    }

    String filteringName() {
        return mipmapped ? "BILINEAR + MIPMAP" : "NEAREST";
    }

    private void setFiltering(boolean useMipmaps) {
        mipmapped = useMipmaps;
        glBindTexture(GL_TEXTURE_2D, id);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER,
                useMipmaps ? GL_LINEAR_MIPMAP_LINEAR : GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER,
                useMipmaps ? GL_LINEAR : GL_NEAREST);
    }

    private static byte[] readBytes(String path) {
        try (InputStream input = Texture2D.class.getResourceAsStream(path)) {
            if (input == null) throw new IllegalArgumentException("Texture resource is missing: " + path);
            return input.readAllBytes();
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read texture resource: " + path, exception);
        }
    }

    @Override public void close() { glDeleteTextures(id); }
}
