package com.premium.client.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.premium.client.PremiumClient;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MappableRingBuffer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryUtil;

import java.util.OptionalDouble;
import java.util.OptionalInt;

/**
 * Draws filled shapes through walls using a custom pipeline (1.21.11 render API).
 * All inputs are WORLD coordinates (doubles). We subtract the camera in double precision
 * before casting to float, so it stays accurate even millions of blocks from spawn.
 */
public final class Renderer {
    private static final RenderPipeline PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
                    .withLocation(Identifier.fromNamespaceAndPath(PremiumClient.ID, "pipeline/esp_filled"))
                    .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                    .build());

    private static final ByteBufferBuilder ALLOCATOR = new ByteBufferBuilder(RenderType.SMALL_BUFFER_SIZE);
    private static final Vector4f COLOR_MODULATOR = new Vector4f(1f, 1f, 1f, 1f);
    private static final Vector3f MODEL_OFFSET = new Vector3f();
    private static final Matrix4f TEXTURE_MATRIX = new Matrix4f();

    private static BufferBuilder buffer;
    private static MappableRingBuffer vertexBuffer;
    private static Matrix4fc pose;
    private static Vec3 cam = Vec3.ZERO;
    private static boolean failed;

    public static Vec3 cam() { return cam; }

    // ------------------------------------------------------------ frame entry
    public static void render(WorldRenderContext context) {
        if (failed) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        try {
            cam = context.worldState().cameraRenderState.pos;
            pose = context.matrices().last().pose();
            buffer = null;
            PremiumClient.modules.render(mc, mc.getDeltaTracker().getGameTimeDeltaPartialTick(false));
            if (buffer != null) drawBuffer(mc);
        } catch (Throwable t) {
            failed = true;
            buffer = null;
            PremiumClient.LOGGER.error("Premium world renderer disabled after an error", t);
        }
    }

    // ------------------------------------------------------------ shapes
    private static BufferBuilder buf() {
        if (buffer == null)
            buffer = new BufferBuilder(ALLOCATOR, PIPELINE.getVertexFormatMode(), PIPELINE.getVertexFormat());
        return buffer;
    }

    private static void v(BufferBuilder b, float x, float y, float z, float r, float g, float bl, float a) {
        b.addVertex(pose, x, y, z).setColor(r, g, bl, a);
    }

    /** Solid box. */
    public static void box(double x1, double y1, double z1, double x2, double y2, double z2,
                           float r, float g, float bl, float a) {
        BufferBuilder b = buf();
        float minX = (float) (x1 - cam.x), minY = (float) (y1 - cam.y), minZ = (float) (z1 - cam.z);
        float maxX = (float) (x2 - cam.x), maxY = (float) (y2 - cam.y), maxZ = (float) (z2 - cam.z);

        v(b, minX, minY, maxZ, r, g, bl, a); v(b, maxX, minY, maxZ, r, g, bl, a); v(b, maxX, maxY, maxZ, r, g, bl, a); v(b, minX, maxY, maxZ, r, g, bl, a);
        v(b, maxX, minY, minZ, r, g, bl, a); v(b, minX, minY, minZ, r, g, bl, a); v(b, minX, maxY, minZ, r, g, bl, a); v(b, maxX, maxY, minZ, r, g, bl, a);
        v(b, minX, minY, minZ, r, g, bl, a); v(b, minX, minY, maxZ, r, g, bl, a); v(b, minX, maxY, maxZ, r, g, bl, a); v(b, minX, maxY, minZ, r, g, bl, a);
        v(b, maxX, minY, maxZ, r, g, bl, a); v(b, maxX, minY, minZ, r, g, bl, a); v(b, maxX, maxY, minZ, r, g, bl, a); v(b, maxX, maxY, maxZ, r, g, bl, a);
        v(b, minX, maxY, maxZ, r, g, bl, a); v(b, maxX, maxY, maxZ, r, g, bl, a); v(b, maxX, maxY, minZ, r, g, bl, a); v(b, minX, maxY, minZ, r, g, bl, a);
        v(b, minX, minY, minZ, r, g, bl, a); v(b, maxX, minY, minZ, r, g, bl, a); v(b, maxX, minY, maxZ, r, g, bl, a); v(b, minX, minY, maxZ, r, g, bl, a);
    }

    public static void box(AABB bb, float r, float g, float bl, float a) {
        box(bb.minX, bb.minY, bb.minZ, bb.maxX, bb.maxY, bb.maxZ, r, g, bl, a);
    }

    /** Wireframe box built from 12 thin solid boxes. half = half thickness. */
    public static void outline(AABB bb, double half, float r, float g, float bl, float a) {
        double[] xs = {bb.minX, bb.maxX}, ys = {bb.minY, bb.maxY}, zs = {bb.minZ, bb.maxZ};
        for (double y : ys) for (double z : zs) box(bb.minX, y - half, z - half, bb.maxX, y + half, z + half, r, g, bl, a);
        for (double x : xs) for (double z : zs) box(x - half, bb.minY, z - half, x + half, bb.maxY, z + half, r, g, bl, a);
        for (double x : xs) for (double y : ys) box(x - half, y - half, bb.minZ, x + half, y + half, bb.maxZ, r, g, bl, a);
    }

    /** A flat line (tracer) that always faces the camera. */
    public static void line(Vec3 from, Vec3 to, double width, float r, float g, float bl, float a) {
        BufferBuilder b = buf();
        double ax = from.x - cam.x, ay = from.y - cam.y, az = from.z - cam.z;
        double bx = to.x - cam.x, by = to.y - cam.y, bz = to.z - cam.z;
        Vec3 dir = new Vec3(bx - ax, by - ay, bz - az);
        Vec3 mid = new Vec3((ax + bx) / 2, (ay + by) / 2, (az + bz) / 2);
        Vec3 side = dir.cross(mid);
        double len = side.length();
        if (len < 1e-6) return;
        side = side.scale(width / 2.0 / len);

        float p1x = (float) (ax + side.x), p1y = (float) (ay + side.y), p1z = (float) (az + side.z);
        float p2x = (float) (ax - side.x), p2y = (float) (ay - side.y), p2z = (float) (az - side.z);
        float p3x = (float) (bx - side.x), p3y = (float) (by - side.y), p3z = (float) (bz - side.z);
        float p4x = (float) (bx + side.x), p4y = (float) (by + side.y), p4z = (float) (bz + side.z);

        // both windings so back-face culling can never hide it
        v(b, p1x, p1y, p1z, r, g, bl, a); v(b, p2x, p2y, p2z, r, g, bl, a); v(b, p3x, p3y, p3z, r, g, bl, a); v(b, p4x, p4y, p4z, r, g, bl, a);
        v(b, p4x, p4y, p4z, r, g, bl, a); v(b, p3x, p3y, p3z, r, g, bl, a); v(b, p2x, p2y, p2z, r, g, bl, a); v(b, p1x, p1y, p1z, r, g, bl, a);
    }

    // ------------------------------------------------------------ draw (from Fabric docs, 1.21.11)
    private static void drawBuffer(Minecraft client) {
        MeshData builtBuffer = buffer.buildOrThrow();
        MeshData.DrawState drawParameters = builtBuffer.drawState();
        VertexFormat format = drawParameters.format();

        GpuBuffer vertices = upload(drawParameters, format, builtBuffer);
        draw(client, PIPELINE, builtBuffer, drawParameters, vertices, format);

        vertexBuffer.rotate();
        buffer = null;
    }

    private static GpuBuffer upload(MeshData.DrawState drawParameters, VertexFormat format, MeshData builtBuffer) {
        int vertexBufferSize = drawParameters.vertexCount() * format.getVertexSize();

        if (vertexBuffer == null || vertexBuffer.size() < vertexBufferSize) {
            if (vertexBuffer != null) vertexBuffer.close();
            vertexBuffer = new MappableRingBuffer(() -> PremiumClient.ID + " esp vertex buffer",
                    GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_MAP_WRITE, vertexBufferSize);
        }

        CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
        try (GpuBuffer.MappedView mappedView = commandEncoder.mapBuffer(
                vertexBuffer.currentBuffer().slice(0, builtBuffer.vertexBuffer().remaining()), false, true)) {
            MemoryUtil.memCopy(builtBuffer.vertexBuffer(), mappedView.data());
        }
        return vertexBuffer.currentBuffer();
    }

    private static void draw(Minecraft client, RenderPipeline pipeline, MeshData builtBuffer,
                             MeshData.DrawState drawParameters, GpuBuffer vertices, VertexFormat format) {
        GpuBuffer indices;
        VertexFormat.IndexType indexType;

        if (pipeline.getVertexFormatMode() == VertexFormat.Mode.QUADS) {
            builtBuffer.sortQuads(ALLOCATOR, RenderSystem.getProjectionType().vertexSorting());
            indices = pipeline.getVertexFormat().uploadImmediateIndexBuffer(builtBuffer.indexBuffer());
            indexType = builtBuffer.drawState().indexType();
        } else {
            RenderSystem.AutoStorageIndexBuffer shapeIndexBuffer = RenderSystem.getSequentialBuffer(pipeline.getVertexFormatMode());
            indices = shapeIndexBuffer.getBuffer(drawParameters.indexCount());
            indexType = shapeIndexBuffer.type();
        }

        GpuBufferSlice dynamicTransforms = RenderSystem.getDynamicUniforms()
                .writeTransform(RenderSystem.getModelViewMatrix(), COLOR_MODULATOR, MODEL_OFFSET, TEXTURE_MATRIX);

        try (RenderPass renderPass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                () -> PremiumClient.ID + " esp pass",
                client.getMainRenderTarget().getColorTextureView(), OptionalInt.empty(),
                client.getMainRenderTarget().getDepthTextureView(), OptionalDouble.empty())) {
            renderPass.setPipeline(pipeline);
            RenderSystem.bindDefaultUniforms(renderPass);
            renderPass.setUniform("DynamicTransforms", dynamicTransforms);
            renderPass.setVertexBuffer(0, vertices);
            renderPass.setIndexBuffer(indices, indexType);
            renderPass.drawIndexed(0 / format.getVertexSize(), 0, drawParameters.indexCount(), 1);
        }
        builtBuffer.close();
    }

    public static void close() {
        ALLOCATOR.close();
        if (vertexBuffer != null) { vertexBuffer.close(); vertexBuffer = null; }
    }
}
