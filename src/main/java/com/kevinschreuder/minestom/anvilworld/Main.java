package com.kevinschreuder.minestom.anvilworld;

import net.minestom.server.MinecraftServer;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.storage.StorageLocation;
import net.minestom.server.storage.systems.FileStorageSystem;

import java.io.File;

public final class Main
{
    public static void main(String[] args)
    {
        final File file = new File(args[0]);

        MinecraftServer.init();
        MinecraftServer.getStorageManager().defineDefaultStorageSystem(FileStorageSystem::new);
        StorageLocation storageLocation = MinecraftServer.getStorageManager().getLocation("minestom_world");

        WorldConverter converter = new WorldConverter(file, storageLocation);
        InstanceContainer instanceContainer = converter.convert();

        MinecraftServer.getSchedulerManager().shutdown();
        System.exit(0);
    }
}
