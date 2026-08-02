package com.souppvp;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Main extends JavaPlugin implements Listener, CommandExecutor {

    // Couleurs globales chargées depuis config.yml
    private String prefixTag;
    private String prefix1v1;
    private String prefixTournoi;
    private String sbTitle;
    private String sbValueColor;

    // Fichier de données séparé data.yml
    private File dataFile;
    private FileConfiguration dataConfig;

    private Location spawnMain;
    private Location spawnFFA;
    private Location spawnEarlyHG;
    private Location spawnChallenge;
    private Location spawn1v1_1;
    private Location spawn1v1_2;
    private final List<Location> tournoiSpawns = new ArrayList<Location>();

    // Queues 1v1
    private final List<Player> queueUnranked = new ArrayList<Player>();
    private final List<Player> queueRanked = new ArrayList<Player>();
    private final Map<Player, Player> opponents = new HashMap<Player, Player>();
    private final List<Player> rankedMatch = new ArrayList<Player>();

    private boolean tournoiEnCours = false;
    private final List<Player> tournoiParticipants = new ArrayList<Player>();

    // Stats & Data
    private final Map<UUID, Integer> kills = new HashMap<UUID, Integer>();
    private final Map<UUID, Integer> deaths = new HashMap<UUID, Integer>();
    private final Map<UUID, Integer> killstreaks = new HashMap<UUID, Integer>();
    private final Map<UUID, Integer> tournoiWins = new HashMap<UUID, Integer>();
    private final Map<UUID, Integer> elo = new HashMap<UUID, Integer>();
    private final Map<UUID, String> playerRanks = new HashMap<UUID, String>();
    private final List<Player> hiddenScoreboards = new ArrayList<Player>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        setupDataFile();
        loadDataFromConfig();
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("Plugin DatSoup 1.8 active avec succes !");
    }

    @Override
    public void onDisable() {
        saveDataToConfig();
    }

    private void setupDataFile() {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }
        dataFile = new File(getDataFolder(), "data.yml");
        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
    }

    private String color(String text) {
        if (text == null) return "";
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    private void loadDataFromConfig() {
        reloadConfig();

        // 1. Chargement de la Charte Graphique depuis config.yml
        prefixTag = color(getConfig().getString("colors.prefix-tag", "&8[&5SoupTournament&8] ")) + ChatColor.RESET;
        prefix1v1 = color(getConfig().getString("colors.prefix-1v1", "&8[&51v1&8] ")) + ChatColor.RESET;
        prefixTournoi = color(getConfig().getString("colors.prefix-tournoi", "&8[&5Tournoi&8] ")) + ChatColor.RESET;
        sbTitle = color(getConfig().getString("colors.scoreboard-title", "&5&lSoupTournament"));
        sbValueColor = color(getConfig().getString("colors.scoreboard-values", "&5"));

        // Migration rétrocompatible si l'ancien config.yml contenait des spawns ou players
        if (getConfig().contains("spawns") && !dataConfig.contains("spawns")) {
            dataConfig.set("spawns", getConfig().get("spawns"));
            getConfig().set("spawns", null);
        }
        if (getConfig().contains("players") && !dataConfig.contains("players")) {
            dataConfig.set("players", getConfig().get("players"));
            getConfig().set("players", null);
        }
        saveConfig();
        try { dataConfig.save(dataFile); } catch (IOException ignored) {}

        // 2. Chargement des données depuis data.yml
        if (dataConfig.contains("spawns.main")) spawnMain = (Location) dataConfig.get("spawns.main");
        if (dataConfig.contains("spawns.ffa")) spawnFFA = (Location) dataConfig.get("spawns.ffa");
        if (dataConfig.contains("spawns.earlyhg")) spawnEarlyHG = (Location) dataConfig.get("spawns.earlyhg");
        if (dataConfig.contains("spawns.challenge")) spawnChallenge = (Location) dataConfig.get("spawns.challenge");
        if (dataConfig.contains("spawns.1v1_1")) spawn1v1_1 = (Location) dataConfig.get("spawns.1v1_1");
        if (dataConfig.contains("spawns.1v1_2")) spawn1v1_2 = (Location) dataConfig.get("spawns.1v1_2");
        if (dataConfig.contains("spawns.tournois")) {
            List<?> list = dataConfig.getList("spawns.tournois");
            for (Object o : list) {
                if (o instanceof Location) tournoiSpawns.add((Location) o);
            }
        }

        if (dataConfig.contains("players")) {
            for (String key : dataConfig.getConfigurationSection("players").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    kills.put(uuid, dataConfig.getInt("players." + key + ".kills", 0));
                    deaths.put(uuid, dataConfig.getInt("players." + key + ".deaths", 0));
                    killstreaks.put(uuid, dataConfig.getInt("players." + key + ".killstreak", 0));
                    tournoiWins.put(uuid, dataConfig.getInt("players." + key + ".tournoiWins", 0));
                    elo.put(uuid, dataConfig.getInt("players." + key + ".elo", 500));
                    playerRanks.put(uuid, dataConfig.getString("players." + key + ".rank", "default"));
                } catch (Exception ignored) {}
            }
        }

        // Mettre à jour l'affichage de tous les joueurs en ligne après un /reload
        for (Player online : Bukkit.getOnlinePlayers()) {
            updateScoreboard(online);
        }
        updateAllPlayerNameTags();
    }

    private void saveDataToConfig() {
        if (spawnMain != null) dataConfig.set("spawns.main", spawnMain);
        if (spawnFFA != null) dataConfig.set("spawns.ffa", spawnFFA);
        if (spawnEarlyHG != null) dataConfig.set("spawns.earlyhg", spawnEarlyHG);
        if (spawnChallenge != null) dataConfig.set("spawns.challenge", spawnChallenge);
        if (spawn1v1_1 != null) dataConfig.set("spawns.1v1_1", spawn1v1_1);
        if (spawn1v1_2 != null) dataConfig.set("spawns.1v1_2", spawn1v1_2);
        dataConfig.set("spawns.tournois", tournoiSpawns);

        for (UUID uuid : playerRanks.keySet()) {
            dataConfig.set("players." + uuid.toString() + ".kills", kills.getOrDefault(uuid, 0));
            dataConfig.set("players." + uuid.toString() + ".deaths", deaths.getOrDefault(uuid, 0));
            dataConfig.set("players." + uuid.toString() + ".killstreak", killstreaks.getOrDefault(uuid, 0));
            dataConfig.set("players." + uuid.toString() + ".tournoiWins", tournoiWins.getOrDefault(uuid, 0));
            dataConfig.set("players." + uuid.toString() + ".elo", elo.getOrDefault(uuid, 500));
            dataConfig.set("players." + uuid.toString() + ".rank", playerRanks.getOrDefault(uuid, "default"));
        }

        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player p = (Player) sender;

        String name = cmd.getName().toLowerCase();

        if (name.equals("help")) {
            sendHelpMenu(p);
            return true;
        }

        if (name.equals("stats")) {
            Player target = p;
            if (args.length > 0) {
                Player searched = Bukkit.getPlayer(args[0]);
                if (searched != null) {
                    target = searched;
                } else {
                    p.sendMessage(prefixTag + color("&cJoueur introuvable."));
                    return true;
                }
            }
            sendStats(p, target);
            return true;
        }

        if (name.equals("setspawn")) {
            spawnMain = p.getLocation();
            saveDataToConfig();
            p.sendMessage(prefixTag + ChatColor.GRAY + "Spawn principal defini !");
        } else if (name.equals("setffaspawn")) {
            spawnFFA = p.getLocation();
            saveDataToConfig();
            p.sendMessage(prefixTag + ChatColor.GRAY + "Spawn FFA defini !");
        } else if (name.equals("setearlyhgspawn")) {
            spawnEarlyHG = p.getLocation();
            saveDataToConfig();
            p.sendMessage(prefixTag + ChatColor.GRAY + "Spawn EarlyHG defini !");
        } else if (name.equals("setchallengespawn")) {
            spawnChallenge = p.getLocation();
            saveDataToConfig();
            p.sendMessage(prefixTag + ChatColor.GRAY + "Spawn Challenge defini !");
        } else if (name.equals("set1v1spawn1")) {
            spawn1v1_1 = p.getLocation();
            saveDataToConfig();
            p.sendMessage(prefix1v1 + ChatColor.GRAY + "Spawn 1v1 #1 defini !");
        } else if (name.equals("set1v1spawn2")) {
            spawn1v1_2 = p.getLocation();
            saveDataToConfig();
            p.sendMessage(prefix1v1 + ChatColor.GRAY + "Spawn 1v1 #2 defini !");
        } else if (name.equals("settournoispawn")) {
            tournoiSpawns.add(p.getLocation());
            saveDataToConfig();
            p.sendMessage(prefixTournoi + ChatColor.GRAY + "Spawn tournoi #" + tournoiSpawns.size() + " ajoute !");
        } else if (name.equals("menu") || name.equals("warps")) {
            openWarpsMenu(p);
        } else if (name.equals("queue1v1")) {
            joinQueueUnranked(p);
        } else if (name.equals("tournoi")) {
            if (args.length > 0 && args[0].equalsIgnoreCase("start")) {
                startTournoi(p);
            } else {
                p.sendMessage(prefixTournoi + ChatColor.GRAY + "Usage: /tournoi start");
            }
        } else if (name.equals("sb") || name.equals("scoreboard")) {
            toggleScoreboard(p);
        } else if (name.equals("setrank")) {
            if (!p.hasPermission("souppvp.admin") && !p.isOp()) {
                p.sendMessage(prefixTag + color("&cTu n'as pas la permission."));
                return true;
            }
            if (args.length < 2) {
                p.sendMessage(prefixTag + ChatColor.GRAY + "Usage: /setrank <joueur> <admin|mod|famous|vip|default>");
                return true;
            }
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                p.sendMessage(prefixTag + color("&cJoueur introuvable."));
                return true;
            }
            setPlayerRank(target, args[1].toLowerCase());
            saveDataToConfig();
            p.sendMessage(prefixTag + ChatColor.GRAY + "Grade de " + target.getName() + " mis a jour !");
        }
        return true;
    }

    private void sendHelpMenu(Player p) {
        p.sendMessage(" ");
        p.sendMessage(sbTitle + color(" &7&l===== COMMANDES DATSOUP ====="));
        p.sendMessage(sbValueColor + "/warps " + ChatColor.GRAY + "- Ouvre le menu des warps");
        p.sendMessage(sbValueColor + "/stats [joueur] " + ChatColor.GRAY + "- Voir tes statistiques");
        p.sendMessage(sbValueColor + "/sb ou /scoreboard " + ChatColor.GRAY + "- Masquer/Afficher le scoreboard");
        
        if (p.hasPermission("souppvp.admin") || p.isOp()) {
            p.sendMessage(" ");
            p.sendMessage(sbTitle + color(" &7&l===== COMMANDES ADMIN ====="));
            p.sendMessage(sbValueColor + "/setspawn " + ChatColor.GRAY + "- Definir le Spawn principal");
            p.sendMessage(sbValueColor + "/setffaspawn " + ChatColor.GRAY + "- Definir le Spawn FFA");
            p.sendMessage(sbValueColor + "/setearlyhgspawn " + ChatColor.GRAY + "- Definir le Spawn EarlyHG");
            p.sendMessage(sbValueColor + "/setchallengespawn " + ChatColor.GRAY + "- Definir le Spawn Challenge");
            p.sendMessage(sbValueColor + "/set1v1spawn1 " + ChatColor.GRAY + "- Definir le 1er spawn 1v1");
            p.sendMessage(sbValueColor + "/set1v1spawn2 " + ChatColor.GRAY + "- Definir le 2eme spawn 1v1");
            p.sendMessage(sbValueColor + "/settournoispawn " + ChatColor.GRAY + "- Ajouter un spawn de Tournoi");
            p.sendMessage(sbValueColor + "/tournoi start " + ChatColor.GRAY + "- Lancer le tournoi (min 4 joueurs)");
            p.sendMessage(sbValueColor + "/setrank <joueur> <rank> " + ChatColor.GRAY + "- Changer le grade");
        }
        p.sendMessage(" ");
    }

    private void sendStats(Player p, Player target) {
        UUID uuid = target.getUniqueId();
        int k = kills.getOrDefault(uuid, 0);
        int d = deaths.getOrDefault(uuid, 0);
        int ks = killstreaks.getOrDefault(uuid, 0);
        int wins = tournoiWins.getOrDefault(uuid, 0);
        int pElo = elo.getOrDefault(uuid, 500);
        double kd = (d == 0) ? k : (double) Math.round(((double) k / d) * 10.0) / 10.0;
        String rank = playerRanks.getOrDefault(uuid, "default");
        String rankColor = getRankColorCode(rank);

        p.sendMessage(" ");
        p.sendMessage(sbTitle + color(" &7&l===== STATISTIQUES (" + target.getName() + ") ====="));
        p.sendMessage(ChatColor.GRAY + "Rank: " + color(rankColor) + rank);
        p.sendMessage(ChatColor.GRAY + "Elo: " + sbValueColor + pElo + " (" + getEloRankName(pElo) + ChatColor.GRAY + ")");
        p.sendMessage(ChatColor.GRAY + "Kills: " + sbValueColor + k);
        p.sendMessage(ChatColor.GRAY + "Deaths: " + sbValueColor + d);
        p.sendMessage(ChatColor.GRAY + "Ratio: " + sbValueColor + kd);
        p.sendMessage(ChatColor.GRAY + "Killstreak: " + sbValueColor + ks);
        p.sendMessage(ChatColor.GRAY + "Tournament wins: " + sbValueColor + wins);
        p.sendMessage(" ");
    }

    private String getEloRankName(int playerElo) {
        if (playerElo < 600) return color(getConfig().getString("colors.elo-bronze", "&8Bronze"));
        if (playerElo < 800) return color(getConfig().getString("colors.elo-silver", "&7Silver"));
        if (playerElo < 1000) return color(getConfig().getString("colors.elo-gold", "&6Gold"));
        if (playerElo < 1200) return color(getConfig().getString("colors.elo-platine", "&bPlatine"));
        if (playerElo < 1400) return color(getConfig().getString("colors.elo-diamant", "&9Diamant"));
        return color(getConfig().getString("colors.elo-supreme", "&4Supreme"));
    }

    private void setPlayerRank(Player p, String rank) {
        playerRanks.put(p.getUniqueId(), rank);
        updateAllPlayerNameTags();
        updateScoreboard(p);
    }

    private String getRankColorCode(String rank) {
        return getConfig().getString("colors.rank-" + rank, "&7");
    }

    private void updateAllPlayerNameTags() {
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            Scoreboard sb = viewer.getScoreboard();
            if (sb == Bukkit.getScoreboardManager().getMainScoreboard()) {
                sb = Bukkit.getScoreboardManager().getNewScoreboard();
                viewer.setScoreboard(sb);
            }

            Team tAdmin = getOrCreateTeam(sb, "01Admin", color(getRankColorCode("admin")));
            Team tMod = getOrCreateTeam(sb, "02Mod", color(getRankColorCode("mod")));
            Team tFamous = getOrCreateTeam(sb, "03Famous", color(getRankColorCode("famous")));
            Team tVip = getOrCreateTeam(sb, "04Vip", color(getRankColorCode("vip")));
            Team tDefault = getOrCreateTeam(sb, "05Default", color(getRankColorCode("default")));

            for (Player target : Bukkit.getOnlinePlayers()) {
                String r = playerRanks.getOrDefault(target.getUniqueId(), "default");
                String colorCode = color(getRankColorCode(r));

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
                if (listName.length() > 16) {
                    listName = listName.substring(0, 16);
                }
                target.setPlayerListName(listName);
            }
        }
    }

    private Team getOrCreateTeam(Scoreboard sb, String name, String prefixColor) {
        Team t = sb.getTeam(name);
        if (t == null) {
            t = sb.registerNewTeam(name);
        }
        t.setPrefix(prefixColor);
        return t;
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {
        Player p = e.getPlayer();
        String rank = playerRanks.getOrDefault(p.getUniqueId(), "default");
        String rankColor = color(getRankColorCode(rank));

        int pElo = elo.getOrDefault(p.getUniqueId(), 500);
        String eloRank = getEloRankName(pElo);

        e.setFormat(eloRank + ChatColor.RESET + " <" + rankColor + "%1$s" + ChatColor.RESET + "> %2$s");
    }

    @EventHandler
    public void onFoodChange(FoodLevelChangeEvent e) {
        e.setCancelled(true);
        if (e.getEntity() instanceof Player) {
            ((Player) e.getEntity()).setFoodLevel(20);
        }
    }

    private void updateScoreboard(Player p) {
        if (hiddenScoreboards.contains(p)) {
            p.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
            return;
        }

        Scoreboard pBoard = p.getScoreboard();
        if (pBoard == null || pBoard == Bukkit.getScoreboardManager().getMainScoreboard()) {
            pBoard = Bukkit.getScoreboardManager().getNewScoreboard();
            p.setScoreboard(pBoard);
        }

        Objective obj = pBoard.getObjective("souptourn");
        if (obj != null) {
            obj.unregister();
        }
        
        obj = pBoard.registerNewObjective("souptourn", "dummy");
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        obj.setDisplayName(sbTitle);

        UUID uuid = p.getUniqueId();
        int wins = tournoiWins.getOrDefault(uuid, 0);
        int pElo = elo.getOrDefault(uuid, 500);
        String rank = playerRanks.getOrDefault(uuid, "default");
        String rankColor = color(getRankColorCode(rank));

        addSafeScore(obj, ChatColor.GRAY + "Rank: " + rankColor + safeString(rank, 8), 3);
        addSafeScore(obj, ChatColor.GRAY + "Elo: " + sbValueColor + pElo, 2);
        addSafeScore(obj, ChatColor.GRAY + "Wins: " + sbValueColor + wins, 1);
    }

    private void addSafeScore(Objective obj, String text, int score) {
        if (text.length() > 16) {
            text = text.substring(0, 16);
        }
        obj.getScore(text).setScore(score);
    }

    private String safeString(String text, int maxLen) {
        if (text == null) return "";
        return text.length() > maxLen ? text.substring(0, maxLen) : text;
    }

    private void toggleScoreboard(Player p) {
        if (hiddenScoreboards.contains(p)) {
            hiddenScoreboards.remove(p);
            updateScoreboard(p);
            p.sendMessage(prefixTag + ChatColor.GRAY + "Scoreboard affiche !");
        } else {
            hiddenScoreboards.add(p);
            p.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
            p.sendMessage(prefixTag + color("&cScoreboard masque !"));
        }
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
                && p.getItemInHand() != null
                && p.getItemInHand().getType() == Material.MUSHROOM_SOUP) {
            
            if (p.getHealth() < p.getMaxHealth()) {
                e.setCancelled(true);
                p.setHealth(Math.min(p.getHealth() + 6.0, p.getMaxHealth()));
                p.getItemInHand().setType(Material.BOWL);
            }
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        e.setJoinMessage(null);

        Player p = e.getPlayer();
        UUID uuid = p.getUniqueId();

        if (!kills.containsKey(uuid)) kills.put(uuid, 0);
        if (!deaths.containsKey(uuid)) deaths.put(uuid, 0);
        if (!killstreaks.containsKey(uuid)) killstreaks.put(uuid, 0);
        if (!tournoiWins.containsKey(uuid)) tournoiWins.put(uuid, 0);
        if (!elo.containsKey(uuid)) elo.put(uuid, 500);
        if (!playerRanks.containsKey(uuid)) playerRanks.put(uuid, "default");

        for (Player online : Bukkit.getOnlinePlayers()) {
            online.showPlayer(p);
            p.showPlayer(online);
        }

        for (int i = 0; i < 100; i++) {
            p.sendMessage(" ");
        }
        p.sendMessage(ChatColor.GRAY + "Bienvenue sur " + sbTitle);

        teleportToSpawn(p);
        updateAllPlayerNameTags();
        updateScoreboard(p);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        e.setQuitMessage(null);

        Player p = e.getPlayer();
        queueUnranked.remove(p);
        queueRanked.remove(p);

        if (opponents.containsKey(p)) {
            Player winner = opponents.remove(p);
            opponents.remove(winner);
            boolean isRanked = rankedMatch.contains(p);
            rankedMatch.remove(p);
            rankedMatch.remove(winner);

            if (isRanked) {
                processRankedElo(winner, p);
            }

            resetVanish(winner);
            teleportToSpawn(winner);
            updateScoreboard(winner);
            winner.sendMessage(prefix1v1 + ChatColor.GRAY + "L'adversaire s'est deconnecte.");
        }
        saveDataToConfig();
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        final Player p = e.getPlayer();
        Bukkit.getScheduler().runTaskLater(this, new Runnable() {
            @Override
            public void run() {
                teleportToSpawn(p);
                updateScoreboard(p);
            }
        }, 1L);
    }

    private void teleportToSpawn(Player p) {
        if (spawnMain != null) p.teleport(spawnMain);
        p.setHealth(p.getMaxHealth());
        p.setFoodLevel(20);
        p.getInventory().clear();
        p.getInventory().setArmorContents(null);
        
        ItemStack boussole = createItem(Material.COMPASS, sbValueColor + "Warps");
        p.getInventory().setItem(0, boussole);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if (p.getItemInHand() != null) {
            if (p.getItemInHand().getType() == Material.COMPASS && e.getAction().name().contains("RIGHT")) {
                e.setCancelled(true);
                openWarpsMenu(p);
            } else if (p.getItemInHand().getType() == Material.REDSTONE && e.getAction().name().contains("RIGHT")) {
                e.setCancelled(true);
                leaveQueues(p);
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
        }.runTaskLater(this, 100L);
    }

    private void openWarpsMenu(Player p) {
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

    private void open1v1SelectMenu(Player p) {
        Inventory inv = Bukkit.createInventory(null, 27, ChatColor.DARK_GRAY + "Mode 1v1");
        inv.setItem(12, createItem(Material.MUSHROOM_SOUP, sbValueColor + "" + ChatColor.BOLD + "1v1 Unranked"));
        inv.setItem(14, createItem(Material.DIAMOND_SWORD, sbValueColor + "" + ChatColor.BOLD + "1v1 Ranked"));
        p.openInventory(inv);
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

            if (slot == 1) {
                p.closeInventory();
                if (spawnFFA != null) {
                    p.teleport(spawnFFA);
                    giveKit(p);
                    p.sendMessage(prefixTag + ChatColor.GRAY + "Teleporte au FFA !");
                } else {
                    p.sendMessage(prefixTag + color("&cSpawn FFA non defini !"));
                }
            } else if (slot == 3) {
                p.closeInventory();
                if (spawnEarlyHG != null) {
                    p.teleport(spawnEarlyHG);
                    giveKit(p);
                    p.sendMessage(prefixTag + ChatColor.GRAY + "Teleporte a EarlyHG !");
                } else {
                    p.sendMessage(prefixTag + color("&cSpawn EarlyHG non defini !"));
                }
            } else if (slot == 5) {
                p.closeInventory();
                open1v1SelectMenu(p);
            } else if (slot == 7) {
                p.closeInventory();
                if (spawnChallenge != null) {
                    p.teleport(spawnChallenge);
                    giveKit(p);
                    p.sendMessage(prefixTag + ChatColor.GRAY + "Teleporte au Challenge !");
                } else {
                    p.sendMessage(prefixTag + color("&cSpawn Challenge non defini !"));
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
                joinQueueUnranked(p);
            } else if (slot == 14) {
                p.closeInventory();
                joinQueueRanked(p);
            }
        }
    }

    private void joinQueueUnranked(Player p) {
        if (opponents.containsKey(p)) {
            p.sendMessage(prefix1v1 + color("&cTu es deja en duel !"));
            return;
        }
        if (queueUnranked.contains(p) || queueRanked.contains(p)) {
            p.sendMessage(prefix1v1 + color("&cTu es deja en recherche."));
            return;
        }
        if (spawn1v1_1 == null || spawn1v1_2 == null) {
            p.sendMessage(prefix1v1 + color("&cSpawns 1v1 non configures !"));
            return;
        }

        queueUnranked.add(p);
        p.sendMessage(prefix1v1 + ChatColor.GRAY + "File " + sbValueColor + "Unranked" + ChatColor.GRAY + " rejointe...");

        setupQueueItem(p);

        if (queueUnranked.size() >= 2) {
            Player p1 = queueUnranked.remove(0);
            Player p2 = queueUnranked.remove(0);
            startDuel(p1, p2, false);
        }
    }

    private void joinQueueRanked(Player p) {
        if (opponents.containsKey(p)) {
            p.sendMessage(prefix1v1 + color("&cTu es deja en duel !"));
            return;
        }
        if (queueUnranked.contains(p) || queueRanked.contains(p)) {
            p.sendMessage(prefix1v1 + color("&cTu es deja en recherche."));
            return;
        }
        if (spawn1v1_1 == null || spawn1v1_2 == null) {
            p.sendMessage(prefix1v1 + color("&cSpawns 1v1 non configures !"));
            return;
        }

        int pElo = elo.getOrDefault(p.getUniqueId(), 500);
        Player matchedPlayer = null;

        for (Player other : queueRanked) {
            int otherElo = elo.getOrDefault(other.getUniqueId(), 500);
            if (Math.abs(pElo - otherElo) <= 40) {
                matchedPlayer = other;
                break;
            }
        }

        if (matchedPlayer != null) {
            queueRanked.remove(matchedPlayer);
            startDuel(p, matchedPlayer, true);
        } else {
            queueRanked.add(p);
            p.sendMessage(prefix1v1 + ChatColor.GRAY + "File " + sbValueColor + "Ranked" + ChatColor.GRAY + " rejointe (Recherche ±40 ELO)...");
            setupQueueItem(p);
        }
    }

    private void setupQueueItem(Player p) {
        p.getInventory().clear();
        p.getInventory().setArmorContents(null);
        ItemStack redstone = createItem(Material.REDSTONE, sbValueColor + "" + ChatColor.BOLD + "Quitter la recherche");
        p.getInventory().setItem(8, redstone);
    }

    private void startDuel(Player p1, Player p2, boolean isRanked) {
        opponents.put(p1, p2);
        opponents.put(p2, p1);

        if (isRanked) {
            rankedMatch.add(p1);
            rankedMatch.add(p2);
        }

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online != p1 && online != p2) {
                online.hidePlayer(p1);
                online.hidePlayer(p2);
            }
        }

        p1.showPlayer(p2);
        p2.showPlayer(p1);

        p1.teleport(spawn1v1_1);
        p2.teleport(spawn1v1_2);

        giveKit(p1);
        giveKit(p2);

        String modeName = isRanked ? sbValueColor + "Ranked" : ChatColor.GRAY + "Unranked";
        p1.sendMessage(prefix1v1 + ChatColor.GRAY + "Duel " + modeName + ChatColor.GRAY + " contre " + sbValueColor + p2.getName() + ChatColor.GRAY + " !");
        p2.sendMessage(prefix1v1 + ChatColor.GRAY + "Duel " + modeName + ChatColor.GRAY + " contre " + sbValueColor + p1.getName() + ChatColor.GRAY + " !");
    }

    private void leaveQueues(Player p) {
        if (queueUnranked.contains(p) || queueRanked.contains(p)) {
            queueUnranked.remove(p);
            queueRanked.remove(p);
            teleportToSpawn(p);
            p.sendMessage(prefix1v1 + color("&cTu as quitte la file d'attente."));
        }
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player) || !(e.getEntity() instanceof Player)) return;

        Player attacker = (Player) e.getDamager();
        Player victim = (Player) e.getEntity();

        if (opponents.containsKey(attacker)) {
            if (opponents.get(attacker) != victim) {
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

        UUID vUuid = victim.getUniqueId();
        deaths.put(vUuid, deaths.getOrDefault(vUuid, 0) + 1);
        killstreaks.put(vUuid, 0);

        if (victim.getKiller() != null) {
            Player killer = victim.getKiller();
            UUID kUuid = killer.getUniqueId();
            kills.put(kUuid, kills.getOrDefault(kUuid, 0) + 1);
            killstreaks.put(kUuid, killstreaks.getOrDefault(kUuid, 0) + 1);
            updateScoreboard(killer);
        }

        updateScoreboard(victim);
        saveDataToConfig();

        if (opponents.containsKey(victim)) {
            final Player winner = opponents.remove(victim);
            opponents.remove(winner);

            boolean isRanked = rankedMatch.contains(victim);
            rankedMatch.remove(victim);
            rankedMatch.remove(winner);

            resetVanish(victim);
            resetVanish(winner);

            if (isRanked) {
                processRankedElo(winner, victim);
            } else {
                winner.sendMessage(prefix1v1 + ChatColor.GRAY + "Victoire contre " + sbValueColor + victim.getName() + ChatColor.GRAY + " !");
                victim.sendMessage(prefix1v1 + color("&cDefaite !"));
            }

            Bukkit.getScheduler().runTaskLater(this, new Runnable() {
                @Override
                public void run() {
                    teleportToSpawn(winner);
                    updateScoreboard(winner);
                }
            }, 40L);
        }

        if (tournoiEnCours && tournoiParticipants.contains(victim)) {
            tournoiParticipants.remove(victim);
            Bukkit.broadcastMessage(prefixTournoi + ChatColor.GRAY + victim.getName() + color("&c elimine ! (") + tournoiParticipants.size() + " restants)");

            if (tournoiParticipants.size() == 1) {
                final Player winner = tournoiParticipants.get(0);
                UUID wUuid = winner.getUniqueId();
                tournoiWins.put(wUuid, tournoiWins.getOrDefault(wUuid, 0) + 1);

                Bukkit.broadcastMessage(prefixTournoi + sbValueColor + "" + ChatColor.BOLD + "VICTOIRE DE " + winner.getName() + " !");
                tournoiEnCours = false;
                tournoiParticipants.clear();
                saveDataToConfig();

                Bukkit.getScheduler().runTaskLater(this, new Runnable() {
                    @Override
                    public void run() {
                        teleportToSpawn(winner);
                        updateScoreboard(winner);
                    }
                }, 60L);
            }
        }

        Bukkit.getScheduler().runTaskLater(this, new Runnable() {
            @Override
            public void run() {
                victim.spigot().respawn();
            }
        }, 2L);
    }

    private void processRankedElo(Player winner, Player loser) {
        UUID wUuid = winner.getUniqueId();
        UUID lUuid = loser.getUniqueId();

        int wElo = elo.getOrDefault(wUuid, 500);
        int lElo = elo.getOrDefault(lUuid, 500);

        int gain;
        int loss;

        if (wElo < 600) {
            gain = 25;
            loss = 10;
        } else if (wElo < 1000) {
            gain = 20;
            loss = 15;
        } else if (wElo < 1400) {
            gain = 15;
            loss = 15;
        } else {
            gain = 10;
            loss = 20;
        }

        elo.put(wUuid, wElo + gain);
        elo.put(lUuid, Math.max(0, lElo - loss));

        winner.sendMessage(prefix1v1 + ChatColor.GRAY + "Victoire contre " + sbValueColor + loser.getName() + ChatColor.GRAY + " (" + ChatColor.GREEN + "+" + gain + " Elo" + ChatColor.GRAY + ")");
        loser.sendMessage(prefix1v1 + color("&cDefaite ") + ChatColor.GRAY + "contre " + sbValueColor + winner.getName() + ChatColor.GRAY + " (" + ChatColor.RED + "-" + loss + " Elo" + ChatColor.GRAY + ")");

        updateScoreboard(winner);
        updateScoreboard(loser);
        saveDataToConfig();
    }

    private void resetVanish(Player p) {
        for (Player online : Bukkit.getOnlinePlayers()) {
            online.showPlayer(p);
            p.showPlayer(online);
        }
    }

    private void startTournoi(Player sender) {
        if (tournoiEnCours) return;
        if (Bukkit.getOnlinePlayers().size() < 4) {
            sender.sendMessage(prefixTournoi + color("&cIl faut au moins 4 joueurs !"));
            return;
        }

        tournoiEnCours = true;
        Bukkit.broadcastMessage(prefixTournoi + ChatColor.GRAY + "Debut du tournoi dans 30 secondes !");

        new BukkitRunnable() {
            int timer = 30;

            @Override
            public void run() {
                if (timer == 10 || (timer <= 5 && timer > 0)) {
                    Bukkit.broadcastMessage(prefixTournoi + sbValueColor + "Lancement dans " + timer + "s !");
                }

                if (timer == 0) {
                    tournoiParticipants.clear();
                    int spawnIndex = 0;

                    for (Player p : Bukkit.getOnlinePlayers()) {
                        tournoiParticipants.add(p);
                        if (!tournoiSpawns.isEmpty()) {
                            p.teleport(tournoiSpawns.get(spawnIndex % tournoiSpawns.size()));
                            spawnIndex++;
                        }
                        giveKit(p);
                    }
                    Bukkit.broadcastMessage(prefixTournoi + sbValueColor + "LE TOURNOI COMMENCE !");
                    cancel();
                }
                timer--;
            }
        }.runTaskTimer(this, 0L, 20L);
    }

    private void giveKit(Player p) {
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

    private ItemStack createItem(Material mat, String name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        item.setItemMeta(meta);
        return item;
    }
}