package com.souppvp.listeners;

import com.souppvp.Main;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class InventoryClickListener implements Listener {

    private final Main main;

    public InventoryClickListener(Main main) {
        this.main = main;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (e.getCurrentItem() != null) {
            Material type = e.getCurrentItem().getType();
            if (type == Material.COMPASS || type == Material.REDSTONE) {
                e.setCancelled(true);
                return;
            }
        }

        if (e.getView() == null || e.getView().getTitle() == null) return;
        String title = e.getView().getTitle();

        if (title.equals(ChatColor.DARK_GRAY + "Warps List")) {
            e.setCancelled(true);
            if (!(e.getWhoClicked() instanceof Player)) return;
            Player p = (Player) e.getWhoClicked();
            int slot = e.getRawSlot();

            if (slot == 1) { // FFA
                p.closeInventory();
                if (main.getDataManager().getSpawnFFA() != null) {
                    p.teleport(main.getDataManager().getSpawnFFA());
                    main.getPlayerManager().giveKit(p);
                    p.sendMessage(main.getColorConfig().getPrefixTag() + ChatColor.GRAY + "Teleporte au FFA !");
                } else {
                    p.sendMessage(main.getColorConfig().getPrefixTag() + main.getColorConfig().color("&cSpawn FFA non defini !"));
                }
            } else if (slot == 3) { // EarlyHG
                p.closeInventory();
                if (main.getDataManager().getSpawnEarlyHG() != null) {
                    p.teleport(main.getDataManager().getSpawnEarlyHG());
                    main.getPlayerManager().giveKit(p);
                    p.sendMessage(main.getColorConfig().getPrefixTag() + ChatColor.GRAY + "Teleporte a EarlyHG !");
                } else {
                    p.sendMessage(main.getColorConfig().getPrefixTag() + main.getColorConfig().color("&cSpawn EarlyHG non defini !"));
                }
            } else if (slot == 5) { // 1v1
                p.closeInventory();
                main.getWarpsCommand().open1v1SelectMenu(p);
            } else if (slot == 7) { // Challenge
                p.closeInventory();
                if (main.getDataManager().getSpawnChallenge() != null) {
                    p.teleport(main.getDataManager().getSpawnChallenge());
                    main.getPlayerManager().giveKit(p);
                    p.sendMessage(main.getColorConfig().getPrefixTag() + ChatColor.GRAY + "Teleporte au Challenge !");
                } else {
                    p.sendMessage(main.getColorConfig().getPrefixTag() + main.getColorConfig().color("&cSpawn Challenge non defini !"));
                }
            }
            return;
        }

        if (title.equals(ChatColor.DARK_GRAY + "Mode 1v1")) {
            e.setCancelled(true);
            if (!(e.getWhoClicked() instanceof Player)) return;
            Player p = (Player) e.getWhoClicked();
            int slot = e.getRawSlot();

            if (slot == 12) {
                p.closeInventory();
                main.getDuelManager().joinQueueUnranked(p);
            } else if (slot == 14) {
                p.closeInventory();
                main.getDuelManager().joinQueueRanked(p);
            }
        }
    }
}