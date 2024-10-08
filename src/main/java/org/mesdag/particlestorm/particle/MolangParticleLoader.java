package org.mesdag.particlestorm.particle;

import com.google.gson.JsonParseException;
import com.mojang.serialization.JsonOps;
import net.minecraft.Util;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.mesdag.particlestorm.data.DefinedParticleEffect;
import org.mesdag.particlestorm.data.component.IEmitterComponent;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@OnlyIn(Dist.CLIENT)
public class MolangParticleLoader implements PreparableReloadListener {
    public final Hashtable<ResourceLocation, DefinedParticleEffect> ID_2_EFFECT = new Hashtable<>();
    public final Hashtable<ResourceLocation, ParticleDetail> ID_2_PARTICLE = new Hashtable<>();
    public final Hashtable<ResourceLocation, EmitterDetail> ID_2_EMITTER = new Hashtable<>();
    private static final FileToIdConverter PARTICLE_LISTER = FileToIdConverter.json("particle_definitions");

    @Override
    public @NotNull CompletableFuture<Void> reload(PreparationBarrier preparationBarrier, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller preparationsProfiler, @NotNull ProfilerFiller reloadProfiler, @NotNull Executor backgroundExecutor, @NotNull Executor gameExecutor) {
        return CompletableFuture.supplyAsync(() -> PARTICLE_LISTER.listMatchingResources(resourceManager), backgroundExecutor).thenCompose(map -> {
            List<CompletableFuture<DefinedParticleEffect>> list = new ArrayList<>(map.size());
            map.forEach((file, resource) -> {
                ResourceLocation id = PARTICLE_LISTER.fileToId(file);
                list.add(CompletableFuture.supplyAsync(() -> {
                    try (Reader reader = resource.openAsReader()) {
                        return DefinedParticleEffect.CODEC.parse(JsonOps.INSTANCE, GsonHelper.parse(reader).get("particle_effect")).getOrThrow(JsonParseException::new);
                    } catch (IOException exception) {
                        throw new IllegalStateException("Failed to load definition for particle " + id, exception);
                    }
                }, backgroundExecutor));
            });
            return Util.sequence(list);
        }).thenCompose(preparationBarrier::wait).thenAcceptAsync(effects -> {
            ID_2_EFFECT.clear();
            ID_2_PARTICLE.clear();
            ID_2_EMITTER.clear();
            effects.forEach(effect -> {
                ResourceLocation id = effect.description.identifier();
                ID_2_EFFECT.put(id, effect);
                ID_2_PARTICLE.put(id, new ParticleDetail(effect));
                ID_2_EMITTER.put(id, new EmitterDetail(
                        new MolangParticleOption(effect.description.identifier()),
                        effect.components.values().stream()
                                .filter(c -> c instanceof IEmitterComponent)
                                .map(c -> (IEmitterComponent) c).toList(),
                        effect.events
                ));
            });
        }, gameExecutor);
    }
}
