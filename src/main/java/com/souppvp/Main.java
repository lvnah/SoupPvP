package com.souppvp;

import com.souppvp.commands.*;
import com.souppvp.config.ColorConfig;
import com.souppvp.data.DataManager;
import com.souppvp.listeners.*;
import com.souppvp.managers.*;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {

    private ColorConfig colorConfig;
    private DataManager dataManager;
    private PlayerManager playerManager;
    private ScoreboardManager sbManager;
    private DuelManager duelManager;
    private TournamentManager tournamentManager;
    private WarpsCommand warpsCommand;

    @Override
    public void onEnable() {
        // 1. Initialisation de la Config & Data
        this.colorConfig = new ColorConfig(this);
        this.dataManager = new DataManager(this);
        
        // 2. Initialisation des Managers
        this.playerManager = new PlayerManager(this);
        this.sbManager = new ScoreboardManager(this);
        this.duelManager = new DuelManager(this);
        this.tournamentManager = new TournamentManager(this);

        // 3. Charger les données du fichier data.yml
        this.dataManager.loadData();

        // 4. Enregistrement des Listeners
        getServer().getPluginManager().registerEvents(new PlayerJoinQuitListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerCombatListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerInteractListener(this), this);
        getServer().getPluginManager().registerEvents(new InventoryClickListener(this), this);

        // 5. Enregistrement des Commandes
        this.warpsCommand = new WarpsCommand(this);
        getCommand("warps").setExecutor(warpsCommand);
        getCommand("menu").setExecutor(warpsCommand);
        getCommand("queue1v1").setExecutor(warpsCommand);

        StatsCommand statsCmd = new StatsCommand(this);
        getCommand("stats").setExecutor(statsCmd);
        getCommand("help").setExecutor(statsCmd);

        ScoreboardCommand sbCmd = new ScoreboardCommand(this);
        getCommand("sb").setExecutor(sbCmd);
        getCommand("scoreboard").setExecutor(sbCmd);

        getCommand("tournoi").setExecutor(new TournoiCommand(this));

        AdminCommand adminCmd = new AdminCommand(this);
        getCommand("setspawn").setExecutor(adminCmd);
        getCommand("setffaspawn").setExecutor(adminCmd);
        getCommand("setearlyhgspawn").setExecutor(adminCmd);
        getCommand("setchallengespawn").setExecutor(adminCmd);
        getCommand("set1v1spawn1").setExecutor(adminCmd);
        getCommand("set1v1spawn2").setExecutor(adminCmd);
        getCommand("settournoispawn").setExecutor(adminCmd);
        getCommand("setrank").setExecutor(adminCmd);

        // Mettre à jour l'affichage de tous les joueurs en ligne après un /reload
        for (Player online : Bukkit.getOnlinePlayers()) {
            sbManager.updateScoreboard(online);
        }
        playerManager.updateAllPlayerNameTags();

        getLogger().info("Plugin DatSoup 1.8 propre et modularise active avec succes !");
    }

    @Override
    public void onDisable() {
        dataManager.saveData();
    }

    // Getters pour autoriser la communication inter-classes
    public ColorConfig getColorConfig() { return colorConfig; }
    public DataManager getDataManager() { return dataManager; }
    public PlayerManager getPlayerManager() { return playerManager; }
    public ScoreboardManager getSbManager() { return sbManager; }
    public DuelManager getDuelManager() { return duelManager; }
    public TournamentManager getTournamentManager() { return tournamentManager; }
    public WarpsCommand getWarpsCommand() { return warpsCommand; }
}