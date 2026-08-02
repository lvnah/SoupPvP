package com.souppvp.commands;

import com.souppvp.Main;
import com.souppvp.models.PlayerData;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BuildCommand implements CommandExecutor {

    private final Main main;

    public BuildCommand(Main main) {
        this.main = main;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player p = (Player) sender;
        PlayerData data = main.getPlayerManager().getPlayerData(p);

        // Vérification si le joueur est Admin
        if (!data.getRank().equalsIgnoreCase("admin") && !p.hasPermission("souppvp.admin") && !p.isOp()) {
            p.sendMessage(main.getColorConfig().getPrefixTag() + main.getColorConfig().color("&cSeuls les Admin peuvent utiliser cette commande !"));
            return true;
        }

        if (args.length > 0) {
            if (args[0].equalsIgnoreCase("on")) {
                data.setCanBuild(true);
                p.sendMessage(main.getColorConfig().getPrefixTag() + ChatColor.GREEN + "Mode build ACTIVER ! Vous pouvez casser/poser des blocs.");
                return true;
            } else if (args[0].equalsIgnoreCase("off")) {
                data.setCanBuild(false);
                p.sendMessage(main.getColorConfig().getPrefixTag() + ChatColor.RED + "Mode build DESACTIVER ! Vous ne pouvez plus casser/poser de blocs.");
                return true;
            }
        }

        // Bascule automatique si pas d'argument (toggle)
        boolean newState = !data.canBuild();
        data.setCanBuild(newState);
        if (newState) {
            p.sendMessage(main.getColorConfig().getPrefixTag() + ChatColor.GREEN + "Mode build ACTIVER !");
        } else {
            p.sendMessage(main.getColorConfig().getPrefixTag() + ChatColor.RED + "Mode build DESACTIVER !");
        }

        return true;
    }
}