package com.kevinschreuder.minestom.anvilworld.generators;

import net.minestom.server.instance.ChunkGenerator;
import net.minestom.server.instance.ChunkPopulator;
import net.minestom.server.instance.batch.ChunkBatch;
import net.minestom.server.world.biomes.Biome;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

public class VoidGenerator implements ChunkGenerator
{
    @Override
    public void generateChunkData(@NotNull ChunkBatch batch, int chunkX, int chunkZ)
    {
        // Do not generate any block
    }

    @Override
    public void fillBiomes(@NotNull Biome[] biomes, int chunkX, int chunkZ)
    {
        Arrays.fill(biomes, Biome.PLAINS);
    }

    @Override
    public @Nullable List<ChunkPopulator> getPopulators()
    {
        return null;
    }
}
