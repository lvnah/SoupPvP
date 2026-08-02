package com.souppvp.commands;

import com.souppvp.Main;
import com.souppvp.models.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AdminCommand implements CommandExecutor {

    private final Main main;

    public AdminCommand(Main main) {
        this.main = main;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player p = (Player) sender;
        String name = cmd.getName().toLowerCase();

        if (name.equals("setspawn")) {
            main.getDataManager().setSpawnMain(p.getLocation());
            main.getDataManager().saveData();
            p.sendMessage(main.getColorConfig().getPrefixTag() + ChatColor.GRAY + "Spawn principal defini !");
        } else if (name.equals("setffaspawn")) {
            main.getDataManager().setSpawnFFA(p.getLocation());
            main.getDataManager().saveData();
            p.sendMessage(main.getColorConfig().getPrefixTag() + ChatColor.GRAY + "Spawn FFA defini !");
        } else if (name.equals("setearlyhgspawn")) {
            main.getDataManager().setSpawnEarlyHG(p.getLocation());
            main.getDataManager().saveData();
            p.sendMessage(main.getColorConfig().getPrefixTag() + ChatColor.GRAY + "Spawn EarlyHG defini !");
        } else if (name.equals("setchallengespawn")) {
            main.getDataManager().setSpawnChallenge(p.getLocation());
            main.getDataManager().saveData();
            p.sendMessage(main.getColorConfig().getPrefixTag() + ChatColor.GRAY + "Spawn Challenge defini !");
        } else if (name.equals("set1v1spawn1")) {
            main.getDataManager().setSpawn1v1_1(p.getLocation());
            main.getDataManager().saveData();
            p.sendMessage(main.getColorConfig().getPrefix1v1() + ChatColor.GRAY + "Spawn 1v1 #1 defini !");
        } else if (name.equals("set1v1spawn2")) {
            main.getDataManager().setSpawn1v1_2(p.getLocation());
            main.getDataManager().saveData();
            p.sendMessage(main.getColorConfig().getPrefix1v1() + ChatColor.GRAY + "Spawn 1v1 #2 defini !");
        } else if (name.equals("settournoispawn")) {
            main.getDataManager().getTournoiSpawns().add(p.getLocation());
            main.getDataManager().saveData();
            p.sendMessage(main.getColorConfig().getPrefixTournoi() + ChatColor.GRAY + "Spawn tournoi #" + main.getDataManager().getTournoiSpawns().size() + " ajoute !");
        } else if (name.equals("setrank")) {
            if (!p.hasPermission("souppvp.admin") && !p.isOp()) {
                p.sendMessage(main.getColorConfig().getPrefixTag() + main.getColorConfig().color("&cTu n'as pas la permission."));
                return true;
            }
            if (args.length < 2) {
                p.sendMessage(main.getColorConfig().getPrefixTag() + ChatColor.GRAY + "Usage: /setrank <joueur> <admin|mod|famous|vip|default>");
                return true;
            }
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                p.sendMessage(main.getColorConfig().getPrefixTag() + main.getColorConfig().color("&cJoueur introuvable."));
                return true;
            }
            PlayerData data = main.getPlayerManager().getPlayerData(target);
            data.setRank(args[1].toLowerCase());
            main.getPlayerManager().updateAllPlayerNameTags();
            main.getSbManager().updateScoreboard(target);
            main.getDataManager().saveData();
            p.sendMessage(main.getColorConfig().getPrefixTag() + ChatColor.GRAY + "Grade de " + target.getName() + " mis a jour !");
        }
        return true;
    }
}