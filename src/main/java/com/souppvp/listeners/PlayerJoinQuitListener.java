package com.souppvp.listeners;

import com.souppvp.Main;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public class PlayerJoinQuitListener implements Listener {

    private final Main main;

    public PlayerJoinQuitListener(Main main) {
        this.main = main;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        e.setJoinMessage(null);
        Player p = e.getPlayer();

        main.getPlayerManager().getPlayerData(p); // Initialise la data

        for (Player online : Bukkit.getOnlinePlayers()) {
            online.showPlayer(p);
            p.showPlayer(online);
        }

        for (int i = 0; i < 100; i++) p.sendMessage(" ");
        p.sendMessage(ChatColor.GRAY + "Bienvenue sur " + main.getColorConfig().getSbTitle());

        main.getPlayerManager().teleportToSpawn(p);
        main.getPlayerManager().updateAllPlayerNameTags();
        main.getSbManager().updateScoreboard(p);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        e.setQuitMessage(null);
        Player p = e.getPlayer();

        main.getDuelManager().getQueueUnranked().remove(p);
        main.getDuelManager().getQueueRanked().remove(p);

        if (main.getDuelManager().getOpponents().containsKey(p)) {
            Player winner = main.getDuelManager().getOpponents().remove(p);
            main.getDuelManager().getOpponents().remove(winner);
            boolean isRanked = main.getDuelManager().getRankedMatch().contains(p);
            main.getDuelManager().getRankedMatch().remove(p);
            main.getDuelManager().getRankedMatch().remove(winner);

            if (isRanked) {
                main.getDuelManager().processRankedElo(winner, p);
            }

            main.getDuelManager().resetVanish(winner);
            main.getPlayerManager().teleportToSpawn(winner);
            main.getSbManager().updateScoreboard(winner);
            winner.sendMessage(main.getColorConfig().getPrefix1v1() + ChatColor.GRAY + "L'adversaire s'est deconnecte.");
        }
        main.getDataManager().saveData();
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        final Player p = e.getPlayer();
        Bukkit.getScheduler().runTaskLater(main, new Runnable() {
            @Override
            public void run() {
                main.getPlayerManager().teleportToSpawn(p);
                main.getSbManager().updateScoreboard(p);
            }
        }, 1L);
    }
}