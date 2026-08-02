package com.souppvp.managers;

import com.souppvp.Main;
import com.souppvp.models.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class DuelManager {

    private final Main main;
    private final List<Player> queueUnranked = new ArrayList<Player>();
    private final List<Player> queueRanked = new ArrayList<Player>();
    private final Map<Player, Player> opponents = new HashMap<Player, Player>();
    private final List<Player> rankedMatch = new ArrayList<Player>();

    public DuelManager(Main main) {
        this.main = main;
    }

    public void joinQueueUnranked(Player p) {
        if (opponents.containsKey(p)) {
            p.sendMessage(main.getColorConfig().getPrefix1v1() + main.getColorConfig().color("&cTu es deja en duel !"));
            return;
        }
        if (queueUnranked.contains(p) || queueRanked.contains(p)) {
            p.sendMessage(main.getColorConfig().getPrefix1v1() + main.getColorConfig().color("&cTu es deja en recherche."));
            return;
        }
        if (main.getDataManager().getSpawn1v1_1() == null || main.getDataManager().getSpawn1v1_2() == null) {
            p.sendMessage(main.getColorConfig().getPrefix1v1() + main.getColorConfig().color("&cSpawns 1v1 non configures !"));
            return;
        }

        queueUnranked.add(p);
        p.sendMessage(main.getColorConfig().getPrefix1v1() + ChatColor.GRAY + "File " + main.getColorConfig().getSbValueColor() + "Unranked" + ChatColor.GRAY + " rejointe...");

        setupQueueItem(p);

        if (queueUnranked.size() >= 2) {
            Player p1 = queueUnranked.remove(0);
            Player p2 = queueUnranked.remove(0);
            startDuel(p1, p2, false);
        }
    }

    public void joinQueueRanked(Player p) {
        if (opponents.containsKey(p)) {
            p.sendMessage(main.getColorConfig().getPrefix1v1() + main.getColorConfig().color("&cTu es deja en duel !"));
            return;
        }
        if (queueUnranked.contains(p) || queueRanked.contains(p)) {
            p.sendMessage(main.getColorConfig().getPrefix1v1() + main.getColorConfig().color("&cTu es deja en recherche."));
            return;
        }
        if (main.getDataManager().getSpawn1v1_1() == null || main.getDataManager().getSpawn1v1_2() == null) {
            p.sendMessage(main.getColorConfig().getPrefix1v1() + main.getColorConfig().color("&cSpawns 1v1 non configures !"));
            return;
        }

        int pElo = main.getPlayerManager().getPlayerData(p).getElo();
        Player matchedPlayer = null;

        for (Player other : queueRanked) {
            int otherElo = main.getPlayerManager().getPlayerData(other).getElo();
            if (Math.abs(pElo - otherElo) <= 40) {
                matchedPlayer = other;
                break;
            }
        }

        if (matchedPlayer != null) {
            queueRanked.remove(matchedPlayer);
            startDuel(p, matchedPlayer, true);
        } else {
            queueRanked.add(p);
            p.sendMessage(main.getColorConfig().getPrefix1v1() + ChatColor.GRAY + "File " + main.getColorConfig().getSbValueColor() + "Ranked" + ChatColor.GRAY + " rejointe (Recherche ±40 ELO)...");
            setupQueueItem(p);
        }
    }

    private void setupQueueItem(Player p) {
        p.getInventory().clear();
        p.getInventory().setArmorContents(null);
        ItemStack redstone = main.getPlayerManager().createItem(Material.REDSTONE, main.getColorConfig().getSbValueColor() + "" + ChatColor.BOLD + "Quitter la recherche");
        p.getInventory().setItem(8, redstone);
    }

    public void startDuel(Player p1, Player p2, boolean isRanked) {
        opponents.put(p1, p2);
        opponents.put(p2, p1);

        if (isRanked) {
            rankedMatch.add(p1);
            rankedMatch.add(p2);
        }

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online != p1 && online != p2) {
                online.hidePlayer(p1);
                online.hidePlayer(p2);
            }
        }

        p1.showPlayer(p2);
        p2.showPlayer(p1);

        p1.teleport(main.getDataManager().getSpawn1v1_1());
        p2.teleport(main.getDataManager().getSpawn1v1_2());

        main.getPlayerManager().giveKit(p1);
        main.getPlayerManager().giveKit(p2);

        String modeName = isRanked ? main.getColorConfig().getSbValueColor() + "Ranked" : ChatColor.GRAY + "Unranked";
        p1.sendMessage(main.getColorConfig().getPrefix1v1() + ChatColor.GRAY + "Duel " + modeName + ChatColor.GRAY + " contre " + main.getColorConfig().getSbValueColor() + p2.getName() + ChatColor.GRAY + " !");
        p2.sendMessage(main.getColorConfig().getPrefix1v1() + ChatColor.GRAY + "Duel " + modeName + ChatColor.GRAY + " contre " + main.getColorConfig().getSbValueColor() + p1.getName() + ChatColor.GRAY + " !");
    }

    public void leaveQueues(Player p) {
        if (queueUnranked.contains(p) || queueRanked.contains(p)) {
            queueUnranked.remove(p);
            queueRanked.remove(p);
            main.getPlayerManager().teleportToSpawn(p);
            p.sendMessage(main.getColorConfig().getPrefix1v1() + main.getColorConfig().color("&cTu as quitte la file d'attente."));
        }
    }

    public void processRankedElo(Player winner, Player loser) {
        PlayerData wData = main.getPlayerManager().getPlayerData(winner);
        PlayerData lData = main.getPlayerManager().getPlayerData(loser);

        int wElo = wData.getElo();
        int lElo = lData.getElo();

        int gain, loss;
        if (wElo < 600) { gain = 25; loss = 10; }
        else if (wElo < 1000) { gain = 20; loss = 15; }
        else if (wElo < 1400) { gain = 15; loss = 15; }
        else { gain = 10; loss = 20; }

        wData.setElo(wElo + gain);
        lData.setElo(Math.max(0, lElo - loss));

        winner.sendMessage(main.getColorConfig().getPrefix1v1() + ChatColor.GRAY + "Victoire contre " + main.getColorConfig().getSbValueColor() + loser.getName() + ChatColor.GRAY + " (" + ChatColor.GREEN + "+" + gain + " Elo" + ChatColor.GRAY + ")");
        loser.sendMessage(main.getColorConfig().getPrefix1v1() + main.getColorConfig().color("&cDefaite ") + ChatColor.GRAY + "contre " + main.getColorConfig().getSbValueColor() + winner.getName() + ChatColor.GRAY + " (" + ChatColor.RED + "-" + loss + " Elo" + ChatColor.GRAY + ")");

        main.getSbManager().updateScoreboard(winner);
        main.getSbManager().updateScoreboard(loser);
        main.getDataManager().saveData();
    }

    public void resetVanish(Player p) {
        for (Player online : Bukkit.getOnlinePlayers()) {
            online.showPlayer(p);
            p.showPlayer(online);
        }
    }

    // Getters & Map checks
    public Map<Player, Player> getOpponents() { return opponents; }
    public List<Player> getRankedMatch() { return rankedMatch; }
    public List<Player> getQueueUnranked() { return queueUnranked; }
    public List<Player> getQueueRanked() { return queueRanked; }
}