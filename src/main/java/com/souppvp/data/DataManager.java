package com.souppvp.data;

import com.souppvp.Main;
import com.souppvp.models.PlayerData;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DataManager {

    private final Main main;
    private File dataFile;
    private FileConfiguration dataConfig;

    private Location spawnMain;
    private Location spawnFFA;
    private Location spawnEarlyHG;
    private Location spawnChallenge;
    private Location spawn1v1_1;
    private Location spawn1v1_2;
    private final List<Location> tournoiSpawns = new ArrayList<Location>();

    public DataManager(Main main) {
        this.main = main;
        setupDataFile();
    }

    public void setupDataFile() {
        if (!main.getDataFolder().exists()) main.getDataFolder().mkdirs();
        dataFile = new File(main.getDataFolder(), "data.yml");
        if (!dataFile.exists()) {
            try { dataFile.createNewFile(); } catch (IOException e) { e.printStackTrace(); }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
    }

    public void loadData() {
        // Migration rétrocompatible
        if (main.getConfig().contains("spawns") && !dataConfig.contains("spawns")) {
            dataConfig.set("spawns", main.getConfig().get("spawns"));
            main.getConfig().set("spawns", null);
        }
        if (main.getConfig().contains("players") && !dataConfig.contains("players")) {
            dataConfig.set("players", main.getConfig().get("players"));
            main.getConfig().set("players", null);
        }
        main.saveConfig();
        saveFile();

        if (dataConfig.contains("spawns.main")) spawnMain = (Location) dataConfig.get("spawns.main");
        if (dataConfig.contains("spawns.ffa")) spawnFFA = (Location) dataConfig.get("spawns.ffa");
        if (dataConfig.contains("spawns.earlyhg")) spawnEarlyHG = (Location) dataConfig.get("spawns.earlyhg");
        if (dataConfig.contains("spawns.challenge")) spawnChallenge = (Location) dataConfig.get("spawns.challenge");
        if (dataConfig.contains("spawns.1v1_1")) spawn1v1_1 = (Location) dataConfig.get("spawns.1v1_1");
        if (dataConfig.contains("spawns.1v1_2")) spawn1v1_2 = (Location) dataConfig.get("spawns.1v1_2");
        if (dataConfig.contains("spawns.tournois")) {
            List<?> list = dataConfig.getList("spawns.tournois");
            for (Object o : list) {
                if (o instanceof Location) tournoiSpawns.add((Location) o);
            }
        }

        if (dataConfig.contains("players")) {
            for (String key : dataConfig.getConfigurationSection("players").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    int k = dataConfig.getInt("players." + key + ".kills", 0);
                    int d = dataConfig.getInt("players." + key + ".deaths", 0);
                    int ks = dataConfig.getInt("players." + key + ".killstreak", 0);
                    int tw = dataConfig.getInt("players." + key + ".tournoiWins", 0);
                    int elo = dataConfig.getInt("players." + key + ".elo", 500);
                    String rank = dataConfig.getString("players." + key + ".rank", "default");
                    
                    main.getPlayerManager().addPlayerData(new PlayerData(uuid, k, d, ks, tw, elo, rank));
                } catch (Exception ignored) {}
            }
        }
    }

    public void saveData() {
        if (spawnMain != null) dataConfig.set("spawns.main", spawnMain);
        if (spawnFFA != null) dataConfig.set("spawns.ffa", spawnFFA);
        if (spawnEarlyHG != null) dataConfig.set("spawns.earlyhg", spawnEarlyHG);
        if (spawnChallenge != null) dataConfig.set("spawns.challenge", spawnChallenge);
        if (spawn1v1_1 != null) dataConfig.set("spawns.1v1_1", spawn1v1_1);
        if (spawn1v1_2 != null) dataConfig.set("spawns.1v1_2", spawn1v1_2);
        dataConfig.set("spawns.tournois", tournoiSpawns);

        for (PlayerData data : main.getPlayerManager().getAllData()) {
            String key = data.getUuid().toString();
            dataConfig.set("players." + key + ".kills", data.getKills());
            dataConfig.set("players." + key + ".deaths", data.getDeaths());
            dataConfig.set("players." + key + ".killstreak", data.getKillstreak());
            dataConfig.set("players." + key + ".tournoiWins", data.getTournoiWins());
            dataConfig.set("players." + key + ".elo", data.getElo());
            dataConfig.set("players." + key + ".rank", data.getRank());
        }
        saveFile();
    }

    private void saveFile() {
        try { dataConfig.save(dataFile); } catch (IOException e) { e.printStackTrace(); }
    }

    // Getters / Setters pour les spawns
    public Location getSpawnMain() { return spawnMain; }
    public void setSpawnMain(Location loc) { this.spawnMain = loc; }
    public Location getSpawnFFA() { return spawnFFA; }
    public void setSpawnFFA(Location loc) { this.spawnFFA = loc; }
    public Location getSpawnEarlyHG() { return spawnEarlyHG; }
    public void setSpawnEarlyHG(Location loc) { this.spawnEarlyHG = loc; }
    public Location getSpawnChallenge() { return spawnChallenge; }
    public void setSpawnChallenge(Location loc) { this.spawnChallenge = loc; }
    public Location getSpawn1v1_1() { return spawn1v1_1; }
    public void setSpawn1v1_1(Location loc) { this.spawn1v1_1 = loc; }
    public Location getSpawn1v1_2() { return spawn1v1_2; }
    public void setSpawn1v1_2(Location loc) { this.spawn1v1_2 = loc; }
    public List<Location> getTournoiSpawns() { return tournoiSpawns; }
}