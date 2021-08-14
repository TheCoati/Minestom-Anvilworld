## Minestom World Converter

Easy to use world converter to convert anvil worlds to Minestom.<br>
This tool can either be used as an API or Standalone using the CLI.

### CLI

- Download the latest release from the releases pages.
- Execute `java -jar wolrd-converter.jar <WORLD_DIRECTORY>`
- Your Minestom compatible world will output in the `minestom_world` folder.


### Dependency

**Maven**
```
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>com.github.Minestom</groupId>
        <artifactId>Minestom</artifactId>
        <version>-SNAPSHOT</version>
        <exclusions>
            <exclusion>
                <groupId>org.jboss.shrinkwrap.resolver</groupId>
                <artifactId>shrinkwrap-resolver-depchain</artifactId>
            </exclusion>
        </exclusions>
    </dependency>
    <dependency>
        <groupId>com.github.KSchreuder</groupId>
        <artifactId>Minestom-Anvilworld</artifactId>
        <version>VERSION</version>
    </dependency>
</dependencies>
```

**Gradle**
```
repositories {
    mavenCentral()
    maven { url 'https://jitpack.io' }
    maven { url 'https://repo.spongepowered.org/maven' }
}

dependencies {
    implementation 'com.github.Minestom:Minestom:-SNAPSHOT'
    implementation 'com.github.KSchreuder:Minestom-Anvilworld:VERSION'
}
```

### API Example

```
final File file = new File("world");

MinecraftServer minecraftServer = MinecraftServer.init();
MinecraftServer.getStorageManager().defineDefaultStorageSystem(FileStorageSystem::new);
StorageLocation storageLocation = MinecraftServer.getStorageManager().getLocation("minestom_world");

WorldConverter converter = new WorldConverter(file, storageLocation);
InstanceContainer instanceContainer = converter.convert();

GlobalEventHandler globalEventHandler = MinecraftServer.getGlobalEventHandler();
globalEventHandler.addListener(PlayerLoginEvent.class, event -> {
    event.setSpawningInstance(instanceContainer);

    Player player = event.getPlayer();
    player.setRespawnPoint(new Position(0, 42, 0));
    player.setGameMode(GameMode.CREATIVE);
});

minecraftServer.start("0.0.0.0", 25565);
```