package com.souppvp.listeners;

import com.souppvp.Main;
import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.scheduler.BukkitRunnable;

public class PlayerInteractListener implements Listener {

    private final Main main;

    public PlayerInteractListener(Main main) {
        this.main = main;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if (p.getItemInHand() != null) {
            if (p.getItemInHand().getType() == Material.COMPASS && e.getAction().name().contains("RIGHT")) {
                e.setCancelled(true);
                main.getWarpsCommand().openWarpsMenu(p);
            } else if (p.getItemInHand().getType() == Material.REDSTONE && e.getAction().name().contains("RIGHT")) {
                e.setCancelled(true);
                main.getDuelManager().leaveQueues(p);
            }
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent e) {
        Material type = e.getItemDrop().getItemStack().getType();
        if (type == Material.REDSTONE || type == Material.COMPASS) {
            e.setCancelled(true);
            return;
        }

        final Item droppedItem = e.getItemDrop();
        new BukkitRunnable() {
            @Override
            public void run() {
                if (droppedItem != null && droppedItem.isValid() && !droppedItem.isDead()) {
                    droppedItem.getWorld().playEffect(droppedItem.getLocation(), Effect.SMOKE, 4);
                    droppedItem.remove();
                }
            }
        }.runTaskLater(main, 100L);
    }
}