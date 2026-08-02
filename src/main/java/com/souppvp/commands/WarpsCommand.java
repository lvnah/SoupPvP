package com.souppvp.commands;

import com.souppvp.Main;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;

public class WarpsCommand implements CommandExecutor {

    private final Main main;

    public WarpsCommand(Main main) {
        this.main = main;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player p = (Player) sender;

        if (cmd.getName().equalsIgnoreCase("queue1v1")) {
            main.getDuelManager().joinQueueUnranked(p);
        } else {
            openWarpsMenu(p);
        }
        return true;
    }

    public void openWarpsMenu(Player p) {
        Inventory inv = Bukkit.createInventory(null, 9, ChatColor.DARK_GRAY + "Warps List");

        inv.setItem(1, createWarpItem(Material.MUSHROOM_SOUP,
                ChatColor.LIGHT_PURPLE + "FFA",
                Arrays.asList(
                        ChatColor.GRAY + "Fight in FFA mode against all",
                        ChatColor.GRAY + "players with soup healing.",
                        "",
                        ChatColor.WHITE + "> " + ChatColor.LIGHT_PURPLE + "0 players"
                )));

        inv.setItem(3, createWarpItem(Material.STONE_SWORD,
                ChatColor.LIGHT_PURPLE + "Early HG",
                Arrays.asList(
                        ChatColor.GRAY + "Fight with a stone sword",
                        ChatColor.GRAY + "in the simulation of the",
                        ChatColor.GRAY + "first few minutes of a HG Games.",
                        "",
                        ChatColor.WHITE + "> " + ChatColor.LIGHT_PURPLE + "0 players"
                )));

        inv.setItem(5, createWarpItem(Material.INK_SACK, (short) 1,
                ChatColor.LIGHT_PURPLE + "1v1",
                Arrays.asList(
                        ChatColor.GRAY + "Queue up for Unranked or Ranked",
                        ChatColor.GRAY + "duels against other players.",
                        "",
                        ChatColor.WHITE + "> " + ChatColor.LIGHT_PURPLE + "0 players"
                )));

        inv.setItem(7, createWarpItem(Material.LAVA_BUCKET,
                ChatColor.LIGHT_PURPLE + "Challenge",
                Arrays.asList(
                        ChatColor.GRAY + "Train your mechanics and lava",
                        ChatColor.GRAY + "refills in lava challenge.",
                        "",
                        ChatColor.WHITE + "> " + ChatColor.LIGHT_PURPLE + "0 players"
                )));

        p.openInventory(inv);
    }

    public void open1v1SelectMenu(Player p) {
        Inventory inv = Bukkit.createInventory(null, 27, ChatColor.DARK_GRAY + "Mode 1v1");
        inv.setItem(12, main.getPlayerManager().createItem(Material.MUSHROOM_SOUP, main.getColorConfig().getSbValueColor() + "" + ChatColor.BOLD + "1v1 Unranked"));
        inv.setItem(14, main.getPlayerManager().createItem(Material.DIAMOND_SWORD, main.getColorConfig().getSbValueColor() + "" + ChatColor.BOLD + "1v1 Ranked"));
        p.openInventory(inv);
    }

    private ItemStack createWarpItem(Material mat, String name, List<String> lore) {
        return createWarpItem(mat, (short) 0, name, lore);
    }

    private ItemStack createWarpItem(Material mat, short data, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat, 1, data);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }
}