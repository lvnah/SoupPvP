package com.souppvp.managers;

import com.souppvp.Main;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;

public class TournamentManager {

    private final Main main;
    private boolean tournoiEnCours = false;
    private final List<Player> tournoiParticipants = new ArrayList<Player>();

    public TournamentManager(Main main) {
        this.main = main;
    }

    public void startTournoi(Player sender) {
        if (tournoiEnCours) return;
        if (Bukkit.getOnlinePlayers().size() < 4) {
            sender.sendMessage(main.getColorConfig().getPrefixTournoi() + main.getColorConfig().color("&cIl faut au moins 4 joueurs !"));
            return;
        }

        tournoiEnCours = true;
        Bukkit.broadcastMessage(main.getColorConfig().getPrefixTournoi() + ChatColor.GRAY + "Debut du tournoi dans 30 secondes !");

        new BukkitRunnable() {
            int timer = 30;

            @Override
            public void run() {
                if (timer == 10 || (timer <= 5 && timer > 0)) {
                    Bukkit.broadcastMessage(main.getColorConfig().getPrefixTournoi() + main.getColorConfig().getSbValueColor() + "Lancement dans " + timer + "s !");
                }

                if (timer == 0) {
                    tournoiParticipants.clear();
                    int spawnIndex = 0;

                    for (Player p : Bukkit.getOnlinePlayers()) {
                        tournoiParticipants.add(p);
                        if (!main.getDataManager().getTournoiSpawns().isEmpty()) {
                            p.teleport(main.getDataManager().getTournoiSpawns().get(spawnIndex % main.getDataManager().getTournoiSpawns().size()));
                            spawnIndex++;
                        }
                        main.getPlayerManager().giveKit(p);
                    }
                    Bukkit.broadcastMessage(main.getColorConfig().getPrefixTournoi() + main.getColorConfig().getSbValueColor() + "LE TOURNOI COMMENCE !");
                    cancel();
                }
                timer--;
            }
        }.runTaskTimer(main, 0L, 20L);
    }

    public boolean isTournoiEnCours() { return tournoiEnCours; }
    public void setTournoiEnCours(boolean b) { this.tournoiEnCours = b; }
    public List<Player> getTournoiParticipants() { return tournoiParticipants; }
}