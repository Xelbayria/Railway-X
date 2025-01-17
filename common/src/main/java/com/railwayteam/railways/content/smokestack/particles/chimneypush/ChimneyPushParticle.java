package com.railwayteam.railways.content.smokestack.particles.chimneypush;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Quaternion;
import com.mojang.math.Vector3f;
import com.railwayteam.railways.content.smokestack.particles.CustomAnimatedTextureSheetParticle;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class ChimneyPushParticle extends CustomAnimatedTextureSheetParticle {
    protected final boolean leadOnly;
    protected final RandomSource random;
    protected final boolean small;
    protected ChimneyPushParticle(ClientLevel level, double x, double y, double z, RandomSource random, boolean leadOnly, boolean small) {
        super(level, x, y, z, 0.0, 0, 0.0);
        this.small = small;
        this.xd = 0;
        this.yd = 0;
        this.zd = 0;
        this.quadSize = 1.0f;
        this.friction = 0.99f;
        this.random = random;
        this.leadOnly = leadOnly;
        setLifetime((int) (random.nextIntBetweenInclusive(15, 25) * Math.min(getAnimationFactor()*3, 1.0)));
    }

    protected double getAnimationFactor() {
        return leadOnly ? (small ? 3/9. : 3/13.) : 1;
    }

    @Override
    protected int frameHeightFactor() {
        return small ? 2 : 1;
    }

    @Override
    protected double getAnimationProgress() {
        int age = Math.min(this.age, this.lifetime - 1);
        double lifeFactor = age / (double) this.lifetime;
        return Mth.clamp(lifeFactor, 0, 1) * getAnimationFactor();
    }

    @Override
    public void render(@NotNull VertexConsumer buffer, @NotNull Camera renderInfo, float partialTicks) {
        Vec3 vec3 = renderInfo.getPosition();
        float quadSize = this.getQuadSize(partialTicks);
        float x = (float)(Mth.lerp(partialTicks, this.xo, this.x) - vec3.x());
        float y = (float)(Mth.lerp(partialTicks, this.yo, this.y) - vec3.y()) + (quadSize/2);
        float z = (float)(Mth.lerp(partialTicks, this.zo, this.z) - vec3.z());
        Quaternion facing;
        if (this.roll == 0.0F) {
            facing = Vector3f.YP.rotationDegrees(-renderInfo.getYRot());//renderInfo.rotation();
        } else {
            facing = new Quaternion(Vector3f.YP.rotationDegrees(-renderInfo.getYRot()));
            float i = Mth.lerp(partialTicks, this.oRoll, this.roll);
            facing.mul(Vector3f.ZP.rotation(i));
        }

        Vector3f[] vertices = new Vector3f[]{
            new Vector3f(-1.0F, -1.0F, 0.0F), new Vector3f(-1.0F, 1.0F, 0.0F), new Vector3f(1.0F, 1.0F, 0.0F), new Vector3f(1.0F, -1.0F, 0.0F)
        };

        for(int i = 0; i < 4; ++i) {
            Vector3f vertex = vertices[i];
            vertex.transform(facing);
            vertex.mul(quadSize);
            if (small)
                vertex.mul(1, 2, 1);
            vertex.add(x, y, z);
        }

        float u0 = this.getU0();
        float u1 = this.getU1();
        float v0 = this.getV0();
        float v1 = this.getV1();
        int packedLight = this.getLightColor(partialTicks);
        buffer.vertex(vertices[0].x(), vertices[0].y(), vertices[0].z())
            .uv(u1, v1)
            .color(this.rCol, this.gCol, this.bCol, this.alpha)
            .uv2(packedLight)
            .endVertex();
        buffer.vertex(vertices[1].x(), vertices[1].y(), vertices[1].z())
            .uv(u1, v0)
            .color(this.rCol, this.gCol, this.bCol, this.alpha)
            .uv2(packedLight)
            .endVertex();
        buffer.vertex(vertices[2].x(), vertices[2].y(), vertices[2].z())
            .uv(u0, v0)
            .color(this.rCol, this.gCol, this.bCol, this.alpha)
            .uv2(packedLight)
            .endVertex();
        buffer.vertex(vertices[3].x(), vertices[3].y(), vertices[3].z())
            .uv(u0, v1)
            .color(this.rCol, this.gCol, this.bCol, this.alpha)
            .uv2(packedLight)
            .endVertex();
    }

    public void setOldPos() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
    }

    @Override
    public void tick() {
        setOldPos();
        if (this.age++ >= this.lifetime) {
            this.remove();
        }
    }

    @Override
    public @NotNull ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
    }

    @Environment(EnvType.CLIENT)
    public static class Factory<T extends ChimneyPushParticleData<T>> implements ParticleProvider<T> {
        private final SpriteSet spriteSet;
        public Factory(SpriteSet spriteSet) {
            this.spriteSet = spriteSet;
        }

        @Nullable
        @Override
        public Particle createParticle(@NotNull T type, @NotNull ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            ChimneyPushParticle particle = new ChimneyPushParticle(level, x, y, z, level.getRandom(), type.leadOnly, type instanceof ChimneyPushParticleData.Small);
            int textureCount = 2;
            int idx = 0;
            if (Mth.equal(type.red, -1) && Mth.equal(type.green, -1) && Mth.equal(type.blue, -1)) {
                idx = 1;
            }
            particle.setSprite(spriteSet.get(idx, textureCount - 1));
            particle.age = level.getRandom().nextInt(5);
            if (idx == 0) {
                particle.rCol = type.red;
                particle.gCol = type.green;
                particle.bCol = type.blue;
            }
            particle.quadSize = type.getQuadSize();
            return particle;
        }
    }
}
