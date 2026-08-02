package com.souppvp.config;

import com.souppvp.Main;
import org.bukkit.ChatColor;

public class ColorConfig {

    private final Main main;
    private String prefixTag;
    private String prefix1v1;
    private String prefixTournoi;
    private String sbTitle;
    private String sbValueColor;

    public ColorConfig(Main main) {
        this.main = main;
        setupDefaults();
        loadValues();
    }

    public void setupDefaults() {
        main.getConfig().addDefault("colors.prefix-tag", "&8[&5SoupTournament&8] ");
        main.getConfig().addDefault("colors.prefix-1v1", "&8[&51v1&8] ");
        main.getConfig().addDefault("colors.prefix-tournoi", "&8[&5Tournoi&8] ");
        main.getConfig().addDefault("colors.scoreboard-title", "&5&lSoupTournament");
        main.getConfig().addDefault("colors.scoreboard-values", "&5");

        main.getConfig().addDefault("colors.rank-admin", "&c");
        main.getConfig().addDefault("colors.rank-mod", "&d");
        main.getConfig().addDefault("colors.rank-famous", "&b");
        main.getConfig().addDefault("colors.rank-vip", "&6");
        main.getConfig().addDefault("colors.rank-default", "&7");

        main.getConfig().addDefault("colors.elo-bronze", "&8Bronze");
        main.getConfig().addDefault("colors.elo-silver", "&7Silver");
        main.getConfig().addDefault("colors.elo-gold", "&6Gold");
        main.getConfig().addDefault("colors.elo-platine", "&bPlatine");
        main.getConfig().addDefault("colors.elo-diamant", "&9Diamant");
        main.getConfig().addDefault("colors.elo-supreme", "&4Supreme");

        main.getConfig().options().copyDefaults(true);
        main.saveConfig();
    }

    public void loadValues() {
        main.reloadConfig();
        prefixTag = color(main.getConfig().getString("colors.prefix-tag", "&8[&5SoupTournament&8] ")) + ChatColor.RESET;
        prefix1v1 = color(main.getConfig().getString("colors.prefix-1v1", "&8[&51v1&8] ")) + ChatColor.RESET;
        prefixTournoi = color(main.getConfig().getString("colors.prefix-tournoi", "&8[&5Tournoi&8] ")) + ChatColor.RESET;
        sbTitle = color(main.getConfig().getString("colors.scoreboard-title", "&5&lSoupTournament"));
        sbValueColor = color(main.getConfig().getString("colors.scoreboard-values", "&5"));
    }

    public String color(String text) {
        if (text == null) return "";
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    public String getRankColorCode(String rank) {
        return main.getConfig().getString("colors.rank-" + rank, "&7");
    }

    public String getEloRankName(int playerElo) {
        if (playerElo < 600) return color(main.getConfig().getString("colors.elo-bronze", "&8Bronze"));
        if (playerElo < 800) return color(main.getConfig().getString("colors.elo-silver", "&7Silver"));
        if (playerElo < 1000) return color(main.getConfig().getString("colors.elo-gold", "&6Gold"));
        if (playerElo < 1200) return color(main.getConfig().getString("colors.elo-platine", "&bPlatine"));
        if (playerElo < 1400) return color(main.getConfig().getString("colors.elo-diamant", "&9Diamant"));
        return color(main.getConfig().getString("colors.elo-supreme", "&4Supreme"));
    }

    public String getPrefixTag() { return prefixTag; }
    public String getPrefix1v1() { return prefix1v1; }
    public String getPrefixTournoi() { return prefixTournoi; }
    public String getSbTitle() { return sbTitle; }
    public String getSbValueColor() { return sbValueColor; }
}