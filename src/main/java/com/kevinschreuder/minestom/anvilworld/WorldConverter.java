package com.kevinschreuder.minestom.anvilworld;

import com.google.common.collect.Maps;
import com.kevinschreuder.minestom.anvilworld.generators.AnvilChunkGenerator;
import com.kevinschreuder.minestom.anvilworld.generators.VoidGenerator;
import net.minestom.server.MinecraftServer;
import net.minestom.server.instance.ChunkGenerator;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.storage.StorageLocation;
import net.minestom.server.timer.SchedulerManager;
import net.minestom.server.utils.time.TimeUnit;
import net.querz.mca.MCAFile;
import net.querz.mca.MCAUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.CountDownLatch;

public class WorldConverter
{
    private static final Logger LOGGER = LoggerFactory.getLogger(WorldConverter.class);

    private final File worldFolder;
    private final InstanceContainer instanceContainer;
    private final SchedulerManager schedulerManager;
    private final ChunkGenerator fallbackGenerator;

    private int currentMcaFileIndex = 0;
    private final Map<String, MCAFile> mcaFilesCache = Maps.newHashMap();

    /**
     * Construct a new WorldConverter object
     * Generates void at non generated chunks
     * @param worldFolder File instance of the anvil world folder
     * @param location Minestom output storageLocation
     */

    public WorldConverter(File worldFolder, StorageLocation location)
    {
        this(worldFolder, location, new VoidGenerator());
    }

    /**
     * Construct a new WorldConverter object
     * @param worldFolder File instance of the anvil world folder
     * @param location Minestom output storageLocation
     * @param fallbackGenerator Fallback generator for non generated chunks
     */

    public WorldConverter(File worldFolder, StorageLocation location, ChunkGenerator fallbackGenerator)
    {
        this.worldFolder = worldFolder;
        this.schedulerManager = MinecraftServer.getSchedulerManager();
        this.instanceContainer = MinecraftServer.getInstanceManager().createInstanceContainer(location);
        this.fallbackGenerator = fallbackGenerator;
    }

    /**
     * Start converting given anvil world directory into the Minestom world format
     * @return Minestom instanceContainer for use with the API
     */

    public InstanceContainer convert()
    {
        List<File> files = this.getRegionFiles();

        if (files.size() == 0)
        {
            LOGGER.error(String.format("Cannot find world folder: %s", this.worldFolder.getName()));

            /* Use fallback generator on instance when anvil world loading fails */
            this.instanceContainer.setChunkGenerator(this.fallbackGenerator);
            return this.instanceContainer;
        }

        /* Prepare the instanceContainer for loading */
        this.schedulerManager.buildTask(() -> this.instanceContainer.tick(0L)).repeat(1L, TimeUnit.SERVER_TICK).schedule();
        this.instanceContainer.setChunkGenerator(new AnvilChunkGenerator(this.mcaFilesCache));
        this.instanceContainer.enableAutoChunkLoad(true);

        for (File file: files)
        {
            this.currentMcaFileIndex++;

            try
            {
                MCAFile mcaFile = MCAUtil.read(file);

                this.mcaFilesCache.put(file.getName(), mcaFile);
                int[] coords = this.getRegionCoords(mcaFile);

                /* Wait (32 * 32) due to 32 * 32 chunks in mca region file */
                final CountDownLatch countDownLatch = new CountDownLatch(32 * 32);

                /* Process each chunk in the region */
                for (int x = (coords[0] * 32); x < (coords[0] * 32) + 32; x++)
                {
                    for (int z = (coords[1] * 32); z < (coords[1] * 32) + 32; z++)
                    {
                        final int fz = z;
                        final int fx = x;

                        /* Process multiple chunks in the region at once */
                        new Thread(() -> this.instanceContainer.loadChunk(fx, fz, chunk -> this.instanceContainer.saveChunkToStorage(chunk, () ->
                            {
                                /* Unload chunk after loading and saving */
                                this.instanceContainer.unloadChunk(chunk);

                                countDownLatch.countDown();
                            }
                        ))).start();
                    }
                }

                LOGGER.info(String.format("Converting Anvil world format: %s%%", (int) (((double) this.currentMcaFileIndex / (double) files.size()) * 100)));
                countDownLatch.await();

                /* Remove mcaFile region from cache when fully rendered */
                this.mcaFilesCache.remove(file.getName());
            }
            catch (IOException | ReflectiveOperationException | InterruptedException e)
            {
                e.printStackTrace();

                /* Remove failed mcaFile region from cache */
                this.mcaFilesCache.remove(file.getName());
            }
        }

        /* Set fallback generator when generation of anvil world is complete */
        this.instanceContainer.setChunkGenerator(this.fallbackGenerator);

        return this.instanceContainer;
    }

    /**
     * Get the region files from the base world folder
     * @return returns empty when no regions folder is available else it returns a list of all plain NBT region files
     */

    @SuppressWarnings("ConstantConditions")
    private List<File> getRegionFiles()
    {
        File regionsFolder = new File(this.worldFolder + "/region");

        if (!this.worldFolder.isDirectory() || !regionsFolder.exists() || !regionsFolder.isDirectory())
        {
            return new ArrayList<>();
        }

        List<File> files = new ArrayList<>();

        for (File file: regionsFolder.listFiles())
        {
            if (file.length() <= 0 || !file.getName().startsWith("r.") || !file.getName().endsWith(".mca"))
            {
                continue;
            }

            files.add(file);
        }

        return files;
    }

    /**
     * Get the start x and z coordinates of the region file
     * @param mcaFile MCAFile to read the start coordinates from
     * @return Int array of the X and Z coordinates
     * @throws ReflectiveOperationException Throws when reflective operation fails
     */

    private int[] getRegionCoords(MCAFile mcaFile) throws ReflectiveOperationException
    {
        Field regionX = mcaFile.getClass().getDeclaredField("regionX");
        Field regionZ = mcaFile.getClass().getDeclaredField("regionZ");

        regionX.setAccessible(true);
        regionZ.setAccessible(true);

        int rx = (int) regionX.get(mcaFile);
        int rz = (int) regionZ.get(mcaFile);

        return new int[]{ rx, rz };
    }
}
