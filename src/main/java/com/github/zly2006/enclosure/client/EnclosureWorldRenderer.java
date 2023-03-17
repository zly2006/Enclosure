package com.github.zly2006.enclosure.client;

import com.github.zly2006.enclosure.commands.Session;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Matrix3f;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;

public abstract class EnclosureWorldRenderer {
    private static final float DELTA = 0.01f;
    public static void register() {
        WorldRenderEvents.BEFORE_BLOCK_OUTLINE.register((context, w) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.options.hudHidden)
                return true;
            Session session = ClientMain.clientSession;
            if (session == null || session.getPos1() == null || session.getPos2() == null)
                return true;
            Vec3d cameraPos = context.camera().getPos();
            drawSessionOutline(context.matrixStack(), session, cameraPos, context.tickDelta(), context.consumers());
            return true;
        });
        WorldRenderEvents.AFTER_TRANSLUCENT.register((context) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.options.hudHidden)
                return;
            Session session = ClientMain.clientSession;
            if (session == null || session.getPos1() == null || session.getPos2() == null)
                return;
            Vec3d cameraPos = context.camera().getPos();
            RenderSystem.enableBlend();
            drawSessionFaces(context.matrixStack(), session, cameraPos, context.tickDelta());
            RenderSystem.disableBlend();
        });
    }

    private static void drawSessionOutline(@NotNull MatrixStack matrices, @NotNull Session session, @NotNull Vec3d cameraPos, float delta, VertexConsumerProvider provider) {
        VertexConsumer linesBuffer = provider.getBuffer(RenderLayer.getLines());

        float minX = (float) (Math.min(session.getPos1().getX(), session.getPos2().getX()) - cameraPos.getX());
        float minY = (float) (Math.min(session.getPos1().getY(), session.getPos2().getY()) - cameraPos.getY());
        float minZ = (float) (Math.min(session.getPos1().getZ(), session.getPos2().getZ()) - cameraPos.getZ());
        float maxX = (float) (Math.max(session.getPos1().getX(), session.getPos2().getX()) + 1 - cameraPos.getX());
        float maxY = (float) (Math.max(session.getPos1().getY(), session.getPos2().getY()) + 1 - cameraPos.getY());
        float maxZ = (float) (Math.max(session.getPos1().getZ(), session.getPos2().getZ()) + 1 - cameraPos.getZ());
        float red = 1;
        float green = 1;
        float blue = 1;
        float alpha = 1;
        Matrix4f matrix4f = matrices.peek().getPositionMatrix();
        Matrix3f matrix3f = matrices.peek().getNormalMatrix();
        // Render two points
        WorldRenderer.drawBox(matrices, linesBuffer,
                session.getPos1().getX() - cameraPos.getX(),
                session.getPos1().getY() - cameraPos.getY(),
                session.getPos1().getZ() - cameraPos.getZ(),
                session.getPos1().getX() + 1 - cameraPos.getX(),
                session.getPos1().getY() + 1 - cameraPos.getY(),
                session.getPos1().getZ() + 1 - cameraPos.getZ(),
                1, 0.25f, 0.25f, alpha);
        WorldRenderer.drawBox(matrices, linesBuffer,
                session.getPos2().getX() - cameraPos.getX(),
                session.getPos2().getY() - cameraPos.getY(),
                session.getPos2().getZ() - cameraPos.getZ(),
                session.getPos2().getX() + 1 - cameraPos.getX(),
                session.getPos2().getY() + 1 - cameraPos.getY(),
                session.getPos2().getZ() + 1 - cameraPos.getZ(),
                0.25f, 0.25f, 1, alpha);
        // Render the outline of the box
        renderLine(linesBuffer, matrix4f, matrix3f, minX, minY, minZ, 0, maxX, red, 0, 0, alpha);
        renderLine(linesBuffer, matrix4f, matrix3f, minX, minY, maxZ, 0, maxX, red, 0, 0, alpha);
        renderLine(linesBuffer, matrix4f, matrix3f, minX, maxY, maxZ, 0, maxX, red, 0, 0, alpha);
        renderLine(linesBuffer, matrix4f, matrix3f, minX, maxY, minZ, 0, maxX, red, 0, 0, alpha);
        renderLine(linesBuffer, matrix4f, matrix3f, minX, minY, minZ, 1, maxY, 0, green, 0, alpha);
        renderLine(linesBuffer, matrix4f, matrix3f, minX, minY, maxZ, 1, maxY, 0, green, 0, alpha);
        renderLine(linesBuffer, matrix4f, matrix3f, maxX, minY, maxZ, 1, maxY, 0, green, 0, alpha);
        renderLine(linesBuffer, matrix4f, matrix3f, maxX, minY, minZ, 1, maxY, 0, green, 0, alpha);
        renderLine(linesBuffer, matrix4f, matrix3f, minX, minY, minZ, 2, maxZ, 0, 0, blue, alpha);
        renderLine(linesBuffer, matrix4f, matrix3f, minX, maxY, minZ, 2, maxZ, 0, 0, blue, alpha);
        renderLine(linesBuffer, matrix4f, matrix3f, maxX, maxY, minZ, 2, maxZ, 0, 0, blue, alpha);
        renderLine(linesBuffer, matrix4f, matrix3f, maxX, minY, minZ, 2, maxZ, 0, 0, blue, alpha);
    }
    private static void drawSessionFaces(@NotNull MatrixStack matrices, @NotNull Session session, @NotNull Vec3d cameraPos, float delta) {
        float minX = (float) (Math.min(session.getPos1().getX(), session.getPos2().getX()) - cameraPos.getX() - DELTA);
        float minY = (float) (Math.min(session.getPos1().getY(), session.getPos2().getY()) - cameraPos.getY() - DELTA);
        float minZ = (float) (Math.min(session.getPos1().getZ(), session.getPos2().getZ()) - cameraPos.getZ() - DELTA);
        float maxX = (float) (Math.max(session.getPos1().getX(), session.getPos2().getX()) + 1 - cameraPos.getX() + DELTA);
        float maxY = (float) (Math.max(session.getPos1().getY(), session.getPos2().getY()) + 1 - cameraPos.getY() + DELTA);
        float maxZ = (float) (Math.max(session.getPos1().getZ(), session.getPos2().getZ()) + 1 - cameraPos.getZ() + DELTA);
        float red = 1;
        float green = 1;
        float blue = 1;
        float alpha = 0.15f;
        Matrix4f matrix4f = matrices.peek().getPositionMatrix();

        matrices.push();
        RenderSystem.disableCull();
        BufferBuilder bufferBuilder = Tessellator.getInstance().getBuffer();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        bufferBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        bufferBuilder.vertex(matrix4f, minX, minY, minZ).color(red, green, blue, alpha).next();
        bufferBuilder.vertex(matrix4f, minX, minY, maxZ).color(red, green, blue, alpha).next();
        bufferBuilder.vertex(matrix4f, minX, maxY, maxZ).color(red, green, blue, alpha).next();
        bufferBuilder.vertex(matrix4f, minX, maxY, minZ).color(red, green, blue, alpha).next();
        Tessellator.getInstance().draw();

        bufferBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        bufferBuilder.vertex(matrix4f, maxX, minY, minZ).color(red, green, blue, alpha).next();
        bufferBuilder.vertex(matrix4f, maxX, minY, maxZ).color(red, green, blue, alpha).next();
        bufferBuilder.vertex(matrix4f, maxX, maxY, maxZ).color(red, green, blue, alpha).next();
        bufferBuilder.vertex(matrix4f, maxX, maxY, minZ).color(red, green, blue, alpha).next();
        Tessellator.getInstance().draw();

        bufferBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        bufferBuilder.vertex(matrix4f, minX, minY, minZ).color(red, green, blue, alpha).next();
        bufferBuilder.vertex(matrix4f, minX, minY, maxZ).color(red, green, blue, alpha).next();
        bufferBuilder.vertex(matrix4f, maxX, minY, maxZ).color(red, green, blue, alpha).next();
        bufferBuilder.vertex(matrix4f, maxX, minY, minZ).color(red, green, blue, alpha).next();
        Tessellator.getInstance().draw();

        bufferBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        bufferBuilder.vertex(matrix4f, minX, maxY, minZ).color(red, green, blue, alpha).next();
        bufferBuilder.vertex(matrix4f, minX, maxY, maxZ).color(red, green, blue, alpha).next();
        bufferBuilder.vertex(matrix4f, maxX, maxY, maxZ).color(red, green, blue, alpha).next();
        bufferBuilder.vertex(matrix4f, maxX, maxY, minZ).color(red, green, blue, alpha).next();
        Tessellator.getInstance().draw();

        bufferBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        bufferBuilder.vertex(matrix4f, minX, minY, minZ).color(red, green, blue, alpha).next();
        bufferBuilder.vertex(matrix4f, minX, maxY, minZ).color(red, green, blue, alpha).next();
        bufferBuilder.vertex(matrix4f, maxX, maxY, minZ).color(red, green, blue, alpha).next();
        bufferBuilder.vertex(matrix4f, maxX, minY, minZ).color(red, green, blue, alpha).next();
        Tessellator.getInstance().draw();

        bufferBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        bufferBuilder.vertex(matrix4f, minX, minY, maxZ).color(red, green, blue, alpha).next();
        bufferBuilder.vertex(matrix4f, minX, maxY, maxZ).color(red, green, blue, alpha).next();
        bufferBuilder.vertex(matrix4f, maxX, maxY, maxZ).color(red, green, blue, alpha).next();
        bufferBuilder.vertex(matrix4f, maxX, minY, maxZ).color(red, green, blue, alpha).next();
        Tessellator.getInstance().draw();
        RenderSystem.enableCull();
        matrices.pop();
    }

    /**
     * 线条太大会浮点精度丢失，所以分段画
     * @param linesBuffer the buffer to render the line
     * @param matrix4f the matrix to render the line
     * @param x1 the x coordinate of the first point
     * @param y1 the y coordinate of the first point
     * @param z1 the z coordinate of the first point
     * @param way 0 = x, 1 = y, 2 = z
     * @param to the second point, must be greater than from
     * @param red 0-1
     * @param blue 0-1
     * @param green 0-1
     * @param alpha 0-1
     */
    private static void renderLine(VertexConsumer linesBuffer, Matrix4f matrix4f, Matrix3f matrix3f,
                                   float x1, float y1, float z1,
                                   int way, float to,
                                   float red, float green, float blue, float alpha) {
        float min, max;
        switch (way) {
            case 0 -> {
                min = x1;
                max = to;
                for (; min < max - 64; min += 64) {
                    linesBuffer.vertex(matrix4f, min, y1, z1).color(red, green, blue, alpha).normal(matrix3f, 1, 0, 0).next();
                    linesBuffer.vertex(matrix4f, min + 64, y1, z1).color(red, green, blue, alpha).normal(matrix3f, 1, 0, 0).next();
                }
                linesBuffer.vertex(matrix4f, min, y1, z1).color(red, green, blue, alpha).normal(matrix3f, 1, 0, 0).next();
                linesBuffer.vertex(matrix4f, max, y1, z1).color(red, green, blue, alpha).normal(matrix3f, 1, 0, 0).next();
            }
            case 1 -> {
                min = y1;
                max = to;
                for (; min < max - 64; min += 64) {
                    linesBuffer.vertex(matrix4f, x1, min, z1).color(red, green, blue, alpha).normal(matrix3f, 0, 1, 0).next();
                    linesBuffer.vertex(matrix4f, x1, min + 64, z1).color(red, green, blue, alpha).normal(matrix3f, 0, 1, 0).next();
                }
                linesBuffer.vertex(matrix4f, x1, min, z1).color(red, green, blue, alpha).normal(matrix3f, 0, 1, 0).next();
                linesBuffer.vertex(matrix4f, x1, max, z1).color(red, green, blue, alpha).normal(matrix3f, 0, 1, 0).next();
            }
            case 2 -> {
                min = z1;
                max = to;
                for (; min < max - 64; min += 64) {
                    linesBuffer.vertex(matrix4f, x1, y1, min).color(red, green, blue, alpha).normal(matrix3f, 0, 0, 1).next();
                    linesBuffer.vertex(matrix4f, x1, y1, min + 64).color(red, green, blue, alpha).normal(matrix3f, 0, 0, 1).next();
                }
                linesBuffer.vertex(matrix4f, x1, y1, min).color(red, green, blue, alpha).normal(matrix3f, 0, 0, 1).next();
                linesBuffer.vertex(matrix4f, x1, y1, max).color(red, green, blue, alpha).normal(matrix3f, 0, 0, 1).next();
            }
            default -> throw new IllegalStateException("Unexpected value: " + way);
        }
    }
}
