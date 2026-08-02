package com.souppvp.managers;

import com.souppvp.Main;
import com.souppvp.models.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.*;

public class PlayerManager {

    private final Main main;
    private final Map<UUID, PlayerData> playersData = new HashMap<UUID, PlayerData>();

    public PlayerManager(Main main) {
        this.main = main;
    }

    public PlayerData getPlayerData(Player p) {
        UUID uuid = p.getUniqueId();
        if (!playersData.containsKey(uuid)) {
            playersData.put(uuid, new PlayerData(uuid, 0, 0, 0, 0, 500, "default"));
        }
        return playersData.get(uuid);
    }

    public void addPlayerData(PlayerData data) {
        playersData.put(data.getUuid(), data);
    }

    public Collection<PlayerData> getAllData() {
        return playersData.values();
    }

    public void teleportToSpawn(Player p) {
        if (main.getDataManager().getSpawnMain() != null) {
            p.teleport(main.getDataManager().getSpawnMain());
        }
        p.setHealth(p.getMaxHealth());
        p.setFoodLevel(20);
        p.getInventory().clear();
        p.getInventory().setArmorContents(null);

        ItemStack boussole = createItem(Material.COMPASS, main.getColorConfig().getSbValueColor() + "Warps");
        p.getInventory().setItem(0, boussole);
    }

    public void giveKit(Player p) {
        p.getInventory().clear();
        p.getInventory().setArmorContents(null);
        p.setHealth(p.getMaxHealth());
        p.setFoodLevel(20);

        ItemStack sword = new ItemStack(Material.STONE_SWORD);
        ItemMeta meta = sword.getItemMeta();
        meta.spigot().setUnbreakable(true);
        sword.setItemMeta(meta);

        p.getInventory().setItem(0, sword);

        ItemStack soup = new ItemStack(Material.MUSHROOM_SOUP);
        for (int i = 1; i < 36; i++) {
            p.getInventory().setItem(i, soup);
        }
    }

    public void updateAllPlayerNameTags() {
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            Scoreboard sb = viewer.getScoreboard();
            if (sb == Bukkit.getScoreboardManager().getMainScoreboard()) {
                sb = Bukkit.getScoreboardManager().getNewScoreboard();
                viewer.setScoreboard(sb);
            }

            Team tAdmin = getOrCreateTeam(sb, "01Admin", main.getColorConfig().color(main.getColorConfig().getRankColorCode("admin")));
            Team tMod = getOrCreateTeam(sb, "02Mod", main.getColorConfig().color(main.getColorConfig().getRankColorCode("mod")));
            Team tFamous = getOrCreateTeam(sb, "03Famous", main.getColorConfig().color(main.getColorConfig().getRankColorCode("famous")));
            Team tVip = getOrCreateTeam(sb, "04Vip", main.getColorConfig().color(main.getColorConfig().getRankColorCode("vip")));
            Team tDefault = getOrCreateTeam(sb, "05Default", main.getColorConfig().color(main.getColorConfig().getRankColorCode("default")));

            for (Player target : Bukkit.getOnlinePlayers()) {
                PlayerData data = getPlayerData(target);
                String r = data.getRank();
                String colorCode = main.getColorConfig().color(main.getColorConfig().getRankColorCode(r));

                tAdmin.removeEntry(target.getName());
                tMod.removeEntry(target.getName());
                tFamous.removeEntry(target.getName());
                tVip.removeEntry(target.getName());
                tDefault.removeEntry(target.getName());

                if (r.equals("admin")) tAdmin.addEntry(target.getName());
                else if (r.equals("mod")) tMod.addEntry(target.getName());
                else if (r.equals("famous")) tFamous.addEntry(target.getName());
                else if (r.equals("vip")) tVip.addEntry(target.getName());
                else tDefault.addEntry(target.getName());

                String listName = colorCode + target.getName();
                if (listName.length() > 16) listName = listName.substring(0, 16);
                target.setPlayerListName(listName);
            }
        }
    }

    private Team getOrCreateTeam(Scoreboard sb, String name, String prefixColor) {
        Team t = sb.getTeam(name);
        if (t == null) t = sb.registerNewTeam(name);
        t.setPrefix(prefixColor);
        return t;
    }

    public ItemStack createItem(Material mat, String name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        item.setItemMeta(meta);
        return item;
    }
}