package com.kevinschreuder.minestom.anvilworld.generators;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import net.minestom.server.MinecraftServer;
import net.minestom.server.instance.ChunkGenerator;
import net.minestom.server.instance.ChunkPopulator;
import net.minestom.server.instance.batch.ChunkBatch;
import net.minestom.server.instance.block.Block;
import net.minestom.server.world.biomes.Biome;
import net.minestom.server.world.biomes.BiomeManager;
import net.querz.mca.Chunk;
import net.querz.mca.MCAFile;
import net.querz.mca.MCAUtil;
import net.querz.nbt.tag.CompoundTag;
import net.querz.nbt.tag.Tag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class AnvilChunkGenerator implements ChunkGenerator
{
    private static final Logger LOGGER = LoggerFactory.getLogger(AnvilChunkGenerator.class);

    private static final Map<String, Block> BLOCKS = Maps.newHashMap();
    private static final BiomeManager BIOME_MANAGER = MinecraftServer.getBiomeManager();

    static
    {
        for (Block block : Block.values())
        {
            BLOCKS.put(block.getName(), block);
        }
    }

    private final Map<String, MCAFile> mcaFiles;

    public AnvilChunkGenerator(Map<String, MCAFile> mcaFiles)
    {
        this.mcaFiles = mcaFiles;
    }

    @Override
    public void generateChunkData(@NotNull ChunkBatch batch, int chunkX, int chunkZ)
    {
        Chunk chunk = this.getChunk(chunkX, chunkZ);

        if (chunk == null)
        {
            return;
        }

        for (int x = 0; x < 16; x++)
        {
            for (int y = 0; y < 256; y++)
            {
                for (int z = 0; z < 16; z++)
                {
                    CompoundTag blockState;

                    try
                    {
                        /* Get the block state at the give coordinates */
                        blockState = chunk.getBlockStateAt(x, y, z);

                        if (blockState == null)
                        {
                            continue;
                        }
                    }
                    catch (NullPointerException ignore)
                    {
                        continue;
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                        continue;
                    }

                    /* Get Minestom block object from the NBT data */
                    String blockName = blockState.get("Name").valueToString().replaceAll("\"", "");
                    Block block = BLOCKS.get(blockName);

                    if (block == null)
                    {
                        LOGGER.error(String.format("Block %s does not exists (skipping)", blockName));
                        continue;
                    }

                    /* Check for extra block properties */
                    CompoundTag propertiesTag = blockState.getCompoundTag("Properties");

                    if (propertiesTag == null)
                    {
                        batch.setBlock(x, y, z, block);
                        continue;
                    }

                    List<String> properties = Lists.newArrayList();

                    /* Add all block properties in required format */
                    for (Map.Entry<String, Tag<?>> entry : propertiesTag)
                    {
                        String value = entry.getValue().valueToString().replaceAll("\"", "");
                        properties.add(String.format("%s=%s", entry.getKey(), value));
                    }

                    Collections.sort(properties);
                    batch.setBlockStateId(x, y, z, block.withProperties(properties.toArray(new String[0])));
                }
            }
        }
    }

    @Override
    public void fillBiomes(@NotNull Biome[] biomes, int chunkX, int chunkZ)
    {
        Arrays.fill(biomes, Biome.PLAINS); // Default

        Chunk chunk = this.getChunk(chunkX, chunkZ);

        if (chunk == null)
        {
            return;
        }

        int[] biomesArray = chunk.getBiomes();

        for (int i = 0; i < biomesArray.length; i++)
        {
            final Biome biome = BIOME_MANAGER.getById(biomesArray[i]);

            if (biome == null)
            {
                continue;
            }

            biomes[i] = biome;
        }
    }

    @Override
    public @Nullable List<ChunkPopulator> getPopulators()
    {
        return null;
    }

    /**
     * Get MCA chunk from the X and Z coordinates
     * @param chunkX chunk X position
     * @param chunkZ chunk Z position
     * @return Chunk object from the MCA utils
     */

    private Chunk getChunk(int chunkX, int chunkZ)
    {
        String regionName = MCAUtil.createNameFromChunkLocation(chunkX, chunkZ);

        if (!this.mcaFiles.containsKey(regionName))
        {
            LOGGER.error(String.format("Region %s is not loaded", regionName));
            return null;
        }

        MCAFile mcaFile = this.mcaFiles.get(regionName);
        return mcaFile.getChunk(chunkX, chunkZ);
    }
}
