package com.souppvp.listeners;

import com.souppvp.Main;
import com.souppvp.models.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;

public class PlayerCombatListener implements Listener {

    private final Main main;

    public PlayerCombatListener(Main main) {
        this.main = main;
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {
        Player p = e.getPlayer();
        PlayerData data = main.getPlayerManager().getPlayerData(p);
        String rankColor = main.getColorConfig().color(main.getColorConfig().getRankColorCode(data.getRank()));
        String eloRank = main.getColorConfig().getEloRankName(data.getElo());

        e.setFormat(eloRank + ChatColor.RESET + " <" + rankColor + "%1$s" + ChatColor.RESET + "> %2$s");
    }

    @EventHandler
    public void onFoodChange(FoodLevelChangeEvent e) {
        e.setCancelled(true);
        if (e.getEntity() instanceof Player) ((Player) e.getEntity()).setFoodLevel(20);
    }

    @EventHandler
    public void onItemDamage(PlayerItemDamageEvent e) {
        if (e.getItem() != null && e.getItem().getType() == Material.STONE_SWORD) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onSoup(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if ((e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK)
                && p.getItemInHand() != null && p.getItemInHand().getType() == Material.MUSHROOM_SOUP) {
            
            if (p.getHealth() < p.getMaxHealth()) {
                e.setCancelled(true);
                p.setHealth(Math.min(p.getHealth() + 6.0, p.getMaxHealth()));
                p.getItemInHand().setType(Material.BOWL);
            }
        }
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player) || !(e.getEntity() instanceof Player)) return;

        Player attacker = (Player) e.getDamager();
        Player victim = (Player) e.getEntity();

        if (main.getDuelManager().getOpponents().containsKey(attacker)) {
            if (main.getDuelManager().getOpponents().get(attacker) != victim) {
                e.setCancelled(true);
                return;
            }
        }

        if (attacker.getItemInHand() != null && attacker.getItemInHand().getType() == Material.STONE_SWORD) {
            if (!attacker.isOnGround() && attacker.getFallDistance() > 0) {
                e.setDamage(5.0);
            } else {
                e.setDamage(4.0);
            }
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        final Player victim = e.getEntity();
        e.setDeathMessage(null);
        e.getDrops().clear();

        PlayerData vData = main.getPlayerManager().getPlayerData(victim);
        vData.addDeath();

        if (victim.getKiller() != null) {
            Player killer = victim.getKiller();
            PlayerData kData = main.getPlayerManager().getPlayerData(killer);
            kData.addKill();
            main.getSbManager().updateScoreboard(killer);
        }

        main.getSbManager().updateScoreboard(victim);
        main.getDataManager().saveData();

        if (main.getDuelManager().getOpponents().containsKey(victim)) {
            final Player winner = main.getDuelManager().getOpponents().remove(victim);
            main.getDuelManager().getOpponents().remove(winner);

            boolean isRanked = main.getDuelManager().getRankedMatch().contains(victim);
            main.getDuelManager().getRankedMatch().remove(victim);
            main.getDuelManager().getRankedMatch().remove(winner);

            main.getDuelManager().resetVanish(victim);
            main.getDuelManager().resetVanish(winner);

            if (isRanked) {
                main.getDuelManager().processRankedElo(winner, victim);
            } else {
                winner.sendMessage(main.getColorConfig().getPrefix1v1() + ChatColor.GRAY + "Victoire contre " + main.getColorConfig().getSbValueColor() + victim.getName() + ChatColor.GRAY + " !");
                victim.sendMessage(main.getColorConfig().getPrefix1v1() + main.getColorConfig().color("&cDefaite !"));
            }

            Bukkit.getScheduler().runTaskLater(main, new Runnable() {
                @Override
                public void run() {
                    main.getPlayerManager().teleportToSpawn(winner);
                    main.getSbManager().updateScoreboard(winner);
                }
            }, 40L);
        }

        if (main.getTournamentManager().isTournoiEnCours() && main.getTournamentManager().getTournoiParticipants().contains(victim)) {
            main.getTournamentManager().getTournoiParticipants().remove(victim);
            Bukkit.broadcastMessage(main.getColorConfig().getPrefixTournoi() + ChatColor.GRAY + victim.getName() + main.getColorConfig().color("&c elimine ! (") + main.getTournamentManager().getTournoiParticipants().size() + " restants)");

            if (main.getTournamentManager().getTournoiParticipants().size() == 1) {
                final Player winner = main.getTournamentManager().getTournoiParticipants().get(0);
                main.getPlayerManager().getPlayerData(winner).addTournoiWin();

                Bukkit.broadcastMessage(main.getColorConfig().getPrefixTournoi() + main.getColorConfig().getSbValueColor() + "" + ChatColor.BOLD + "VICTOIRE DE " + winner.getName() + " !");
                main.getTournamentManager().setTournoiEnCours(false);
                main.getTournamentManager().getTournoiParticipants().clear();
                main.getDataManager().saveData();

                Bukkit.getScheduler().runTaskLater(main, new Runnable() {
                    @Override
                    public void run() {
                        main.getPlayerManager().teleportToSpawn(winner);
                        main.getSbManager().updateScoreboard(winner);
                    }
                }, 60L);
            }
        }

        Bukkit.getScheduler().runTaskLater(main, new Runnable() {
            @Override
            public void run() {
                victim.spigot().respawn();
            }
        }, 2L);
    }
}