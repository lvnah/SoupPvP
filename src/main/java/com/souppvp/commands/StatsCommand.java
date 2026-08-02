package com.souppvp.commands;

import com.souppvp.Main;
import com.souppvp.models.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class StatsCommand implements CommandExecutor {

    private final Main main;

    public StatsCommand(Main main) {
        this.main = main;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player p = (Player) sender;

        if (cmd.getName().equalsIgnoreCase("help")) {
            sendHelpMenu(p);
            return true;
        }

        Player target = p;
        if (args.length > 0) {
            Player searched = Bukkit.getPlayer(args[0]);
            if (searched != null) {
                target = searched;
            } else {
                p.sendMessage(main.getColorConfig().getPrefixTag() + main.getColorConfig().color("&cJoueur introuvable."));
                return true;
            }
        }

        sendStats(p, target);
        return true;
    }

    private void sendHelpMenu(Player p) {
        p.sendMessage(" ");
        p.sendMessage(main.getColorConfig().getSbTitle() + main.getColorConfig().color(" &7&l===== COMMANDES DATSOUP ====="));
        p.sendMessage(main.getColorConfig().getSbValueColor() + "/warps " + ChatColor.GRAY + "- Ouvre le menu des warps");
        p.sendMessage(main.getColorConfig().getSbValueColor() + "/stats [joueur] " + ChatColor.GRAY + "- Voir tes statistiques");
        p.sendMessage(main.getColorConfig().getSbValueColor() + "/sb ou /scoreboard " + ChatColor.GRAY + "- Masquer/Afficher le scoreboard");

        if (p.hasPermission("souppvp.admin") || p.isOp()) {
            p.sendMessage(" ");
            p.sendMessage(main.getColorConfig().getSbTitle() + main.getColorConfig().color(" &7&l===== COMMANDES ADMIN ====="));
            p.sendMessage(main.getColorConfig().getSbValueColor() + "/setspawn " + ChatColor.GRAY + "- Definir le Spawn principal");
            p.sendMessage(main.getColorConfig().getSbValueColor() + "/setffaspawn " + ChatColor.GRAY + "- Definir le Spawn FFA");
            p.sendMessage(main.getColorConfig().getSbValueColor() + "/setearlyhgspawn " + ChatColor.GRAY + "- Definir le Spawn EarlyHG");
            p.sendMessage(main.getColorConfig().getSbValueColor() + "/setchallengespawn " + ChatColor.GRAY + "- Definir le Spawn Challenge");
            p.sendMessage(main.getColorConfig().getSbValueColor() + "/set1v1spawn1 " + ChatColor.GRAY + "- Definir le 1er spawn 1v1");
            p.sendMessage(main.getColorConfig().getSbValueColor() + "/set1v1spawn2 " + ChatColor.GRAY + "- Definir le 2eme spawn 1v1");
            p.sendMessage(main.getColorConfig().getSbValueColor() + "/settournoispawn " + ChatColor.GRAY + "- Ajouter un spawn de Tournoi");
            p.sendMessage(main.getColorConfig().getSbValueColor() + "/tournoi start " + ChatColor.GRAY + "- Lancer le tournoi (min 4 joueurs)");
            p.sendMessage(main.getColorConfig().getSbValueColor() + "/setrank <joueur> <rank> " + ChatColor.GRAY + "- Changer le grade");
        }
        p.sendMessage(" ");
    }

    private void sendStats(Player p, Player target) {
        PlayerData data = main.getPlayerManager().getPlayerData(target);
        double kd = (data.getDeaths() == 0) ? data.getKills() : (double) Math.round(((double) data.getKills() / data.getDeaths()) * 10.0) / 10.0;
        String rankColor = main.getColorConfig().getRankColorCode(data.getRank());

        p.sendMessage(" ");
        p.sendMessage(main.getColorConfig().getSbTitle() + main.getColorConfig().color(" &7&l===== STATISTIQUES (" + target.getName() + ") ====="));
        p.sendMessage(ChatColor.GRAY + "Rank: " + main.getColorConfig().color(rankColor) + data.getRank());
        p.sendMessage(ChatColor.GRAY + "Elo: " + main.getColorConfig().getSbValueColor() + data.getElo() + " (" + main.getColorConfig().getEloRankName(data.getElo()) + ChatColor.GRAY + ")");
        p.sendMessage(ChatColor.GRAY + "Kills: " + main.getColorConfig().getSbValueColor() + data.getKills());
        p.sendMessage(ChatColor.GRAY + "Deaths: " + main.getColorConfig().getSbValueColor() + data.getDeaths());
        p.sendMessage(ChatColor.GRAY + "Ratio: " + main.getColorConfig().getSbValueColor() + kd);
        p.sendMessage(ChatColor.GRAY + "Killstreak: " + main.getColorConfig().getSbValueColor() + data.getKillstreak());
        p.sendMessage(ChatColor.GRAY + "Tournament wins: " + main.getColorConfig().getSbValueColor() + data.getTournoiWins());
        p.sendMessage(" ");
    }
}