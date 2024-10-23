package com.github.zly2006.enclosure.client

import com.github.zly2006.enclosure.command.ClientSession
import com.mojang.blaze3d.systems.RenderSystem
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gl.ShaderProgramKeys
import net.minecraft.client.render.*
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.util.math.Vec3d
import org.joml.Matrix4f
import kotlin.math.max
import kotlin.math.min

object EnclosureWorldRenderer {
    private const val DELTA = 0.001f
    fun register() {
        WorldRenderEvents.BEFORE_BLOCK_OUTLINE.register a@{ context: WorldRenderContext, _ ->
            val client = MinecraftClient.getInstance()
            if (client.options.hudHidden) return@a true
            val session = ClientMain.clientSession ?: return@a true
            val cameraPos = context.camera().pos
            drawSessionOutline(context.matrixStack()!!, session, cameraPos, context.consumers())
            true
        }
        WorldRenderEvents.AFTER_TRANSLUCENT.register a@{ context: WorldRenderContext ->
            val client = MinecraftClient.getInstance()
            if (client.options.hudHidden) return@a
            val session = ClientMain.clientSession ?: return@a
            val cameraPos = context.camera().pos
            RenderSystem.enableBlend()
            drawSessionFaces(context.matrixStack()!!, session, cameraPos)
            RenderSystem.disableBlend()
        }
    }

    private fun drawSessionOutline(
        matrices: MatrixStack,
        session: ClientSession,
        cameraPos: Vec3d,
        provider: VertexConsumerProvider?
    ) {
        val linesBuffer = provider!!.getBuffer(RenderLayer.getLines())
        val minX = (min(session.pos1.x, session.pos2.x) - cameraPos.getX()).toFloat()
        val minY = (min(session.pos1.y, session.pos2.y) - cameraPos.getY()).toFloat()
        val minZ = (min(session.pos1.z, session.pos2.z) - cameraPos.getZ()).toFloat()
        val maxX = (max(session.pos1.x, session.pos2.x) + 1 - cameraPos.getX()).toFloat()
        val maxY = (max(session.pos1.y, session.pos2.y) + 1 - cameraPos.getY()).toFloat()
        val maxZ = (max(session.pos1.z, session.pos2.z) + 1 - cameraPos.getZ()).toFloat()
        val red = 1f
        val green = 1f
        val blue = 1f
        val alpha = 1f
        val matrix4f = matrices.peek().positionMatrix
        val matrix3f = matrices.peek()
        // Render two points
        VertexRendering.drawBox(
            matrices, linesBuffer,
            session.pos1.x - cameraPos.x,
            session.pos1.y - cameraPos.y,
            session.pos1.z - cameraPos.z,
            session.pos1.x + 1 - cameraPos.x,
            session.pos1.y + 1 - cameraPos.y,
            session.pos1.z + 1 - cameraPos.z,
            1f, 0.25f, 0.25f, alpha
        )
        VertexRendering.drawBox(
            matrices, linesBuffer,
            session.pos2.x - cameraPos.getX(),
            session.pos2.y - cameraPos.getY(),
            session.pos2.z - cameraPos.getZ(),
            session.pos2.x + 1 - cameraPos.getX(),
            session.pos2.y + 1 - cameraPos.getY(),
            session.pos2.z + 1 - cameraPos.getZ(),
            0.25f, 0.25f, 1f, alpha
        )
        // Render the outline of the box
        renderLine(linesBuffer, matrix4f, matrix3f, minX, minY, minZ, 0, maxX, red, 0f, 0f, alpha)
        renderLine(linesBuffer, matrix4f, matrix3f, minX, minY, maxZ, 0, maxX, red, 0f, 0f, alpha)
        renderLine(linesBuffer, matrix4f, matrix3f, minX, maxY, maxZ, 0, maxX, red, 0f, 0f, alpha)
        renderLine(linesBuffer, matrix4f, matrix3f, minX, maxY, minZ, 0, maxX, red, 0f, 0f, alpha)
        renderLine(linesBuffer, matrix4f, matrix3f, minX, minY, minZ, 1, maxY, 0f, green, 0f, alpha)
        renderLine(linesBuffer, matrix4f, matrix3f, minX, minY, maxZ, 1, maxY, 0f, green, 0f, alpha)
        renderLine(linesBuffer, matrix4f, matrix3f, maxX, minY, maxZ, 1, maxY, 0f, green, 0f, alpha)
        renderLine(linesBuffer, matrix4f, matrix3f, maxX, minY, minZ, 1, maxY, 0f, green, 0f, alpha)
        renderLine(linesBuffer, matrix4f, matrix3f, minX, minY, minZ, 2, maxZ, 0f, 0f, blue, alpha)
        renderLine(linesBuffer, matrix4f, matrix3f, minX, maxY, minZ, 2, maxZ, 0f, 0f, blue, alpha)
        renderLine(linesBuffer, matrix4f, matrix3f, maxX, maxY, minZ, 2, maxZ, 0f, 0f, blue, alpha)
        renderLine(linesBuffer, matrix4f, matrix3f, maxX, minY, minZ, 2, maxZ, 0f, 0f, blue, alpha)
    }

    fun drawSessionFaces(matrices: MatrixStack, session: ClientSession, cameraPos: Vec3d) {
        val minX = (min(session.pos1.x, session.pos2.x) - cameraPos.getX() - DELTA).toFloat()
        val minY = (min(session.pos1.y, session.pos2.y) - cameraPos.getY() - DELTA).toFloat()
        val minZ = (min(session.pos1.z, session.pos2.z) - cameraPos.getZ() - DELTA).toFloat()
        val maxX = (max(session.pos1.x, session.pos2.x) + 1 - cameraPos.getX() + DELTA).toFloat()
        val maxY = (max(session.pos1.y, session.pos2.y) + 1 - cameraPos.getY() + DELTA).toFloat()
        val maxZ = (max(session.pos1.z, session.pos2.z) + 1 - cameraPos.getZ() + DELTA).toFloat()
        val red = 1f
        val green = 1f
        val blue = 1f
        val alpha = 0.15f
        val matrix4f = matrices.peek().positionMatrix
        matrices.push()
        RenderSystem.disableCull()
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR)
        fun drawFace(x1: Float, y1: Float, z1: Float, x2: Float, y2: Float, z2: Float, x3: Float, y3: Float, z3: Float, x4: Float, y4: Float, z4: Float, red: Float, green: Float, blue: Float, alpha: Float) {
            val bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR)
            bufferBuilder.vertex(matrix4f, x1, y1, z1).color(red, green, blue, alpha)
            bufferBuilder.vertex(matrix4f, x2, y2, z2).color(red, green, blue, alpha)
            bufferBuilder.vertex(matrix4f, x3, y3, z3).color(red, green, blue, alpha)
            bufferBuilder.vertex(matrix4f, x4, y4, z4).color(red, green, blue, alpha)
            BufferRenderer.drawWithGlobalProgram(bufferBuilder.end())
        }
        drawFace(minX, minY, minZ, minX, minY, maxZ, minX, maxY, maxZ, minX, maxY, minZ, red, green, blue, alpha)
        drawFace(maxX, minY, minZ, maxX, minY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ, red, green, blue, alpha)
        drawFace(minX, minY, minZ, minX, minY, maxZ, maxX, minY, maxZ, maxX, minY, minZ, red, green, blue, alpha)
        drawFace(minX, maxY, minZ, minX, maxY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ, red, green, blue, alpha)
        drawFace(minX, minY, minZ, minX, maxY, minZ, maxX, maxY, minZ, maxX, minY, minZ, red, green, blue, alpha)
        drawFace(minX, minY, maxZ, minX, maxY, maxZ, maxX, maxY, maxZ, maxX, minY, maxZ, red, green, blue, alpha)
        RenderSystem.enableCull()
        matrices.pop()
    }

    /**
     * 线条太大会浮点精度丢失，所以分段画
     * @param linesBuffer the buffer to render the line
     * @param matrix4f the matrix to render the line
     * @param x1 the x coordinate of the first point
     * @param y1 the y coordinate of the first point
     * @param z1 the z coordinate of the first point
     * @param way 0 = x, 1 = y, 2 = z
     * @param to the second point, it must be greater than from
     * @param red 0-1
     * @param blue 0-1
     * @param green 0-1
     * @param alpha 0-1
     */
    private fun renderLine(
        linesBuffer: VertexConsumer, matrix4f: Matrix4f, matrix3f: MatrixStack.Entry,
        x1: Float, y1: Float, z1: Float,
        way: Int, to: Float,
        red: Float, green: Float, blue: Float, alpha: Float
    ) {
        if (x1 * x1 + y1 * y1 + z1 * z1 > 256 * 56) return
        var min: Float
        val max: Float
        when (way) {
            0 -> {
                min = x1
                max = to
                while (min < max - 64) {
                    linesBuffer.vertex(matrix4f, min, y1, z1).color(red, green, blue, alpha)
                        .normal(matrix3f, 1f, 0f, 0f)
                    linesBuffer.vertex(matrix4f, min + 64, y1, z1).color(red, green, blue, alpha)
                        .normal(matrix3f, 1f, 0f, 0f)
                    min += 64f
                }
                linesBuffer.vertex(matrix4f, min, y1, z1).color(red, green, blue, alpha).normal(matrix3f, 1f, 0f, 0f)
                    
                linesBuffer.vertex(matrix4f, max, y1, z1).color(red, green, blue, alpha).normal(matrix3f, 1f, 0f, 0f)
                    
            }

            1 -> {
                min = y1
                max = to
                while (min < max - 64) {
                    linesBuffer.vertex(matrix4f, x1, min, z1).color(red, green, blue, alpha)
                        .normal(matrix3f, 0f, 1f, 0f)
                    linesBuffer.vertex(matrix4f, x1, min + 64, z1).color(red, green, blue, alpha)
                        .normal(matrix3f, 0f, 1f, 0f)
                    min += 64f
                }
                linesBuffer.vertex(matrix4f, x1, min, z1).color(red, green, blue, alpha).normal(matrix3f, 0f, 1f, 0f)
                    
                linesBuffer.vertex(matrix4f, x1, max, z1).color(red, green, blue, alpha).normal(matrix3f, 0f, 1f, 0f)
                    
            }

            2 -> {
                min = z1
                max = to
                while (min < max - 64) {
                    linesBuffer.vertex(matrix4f, x1, y1, min).color(red, green, blue, alpha)
                        .normal(matrix3f, 0f, 0f, 1f)
                    linesBuffer.vertex(matrix4f, x1, y1, min + 64).color(red, green, blue, alpha)
                        .normal(matrix3f, 0f, 0f, 1f)
                    min += 64f
                }
                linesBuffer.vertex(matrix4f, x1, y1, min).color(red, green, blue, alpha).normal(matrix3f, 0f, 0f, 1f)
                    
                linesBuffer.vertex(matrix4f, x1, y1, max).color(red, green, blue, alpha).normal(matrix3f, 0f, 0f, 1f)
                    
            }

            else -> throw IllegalStateException("Unexpected value: $way")
        }
    }
}
