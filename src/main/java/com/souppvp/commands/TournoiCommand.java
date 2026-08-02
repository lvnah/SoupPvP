package com.souppvp.commands;

import com.souppvp.Main;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TournoiCommand implements CommandExecutor {

    private final Main main;

    public TournoiCommand(Main main) {
        this.main = main;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player p = (Player) sender;

        if (args.length > 0 && args[0].equalsIgnoreCase("start")) {
            main.getTournamentManager().startTournoi(p);
        } else {
            p.sendMessage(main.getColorConfig().getPrefixTournoi() + ChatColor.GRAY + "Usage: /tournoi start");
        }
        return true;
    }
}