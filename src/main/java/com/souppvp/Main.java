package com.souppvp;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
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
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Main extends JavaPlugin implements Listener, CommandExecutor {

    private final String PREFIX = ChatColor.GRAY + "[" + ChatColor.RED + "SoupPvP" + ChatColor.GRAY + "] " + ChatColor.RESET;
    private final String PREFIX_1V1 = ChatColor.GRAY + "[" + ChatColor.YELLOW + "1v1" + ChatColor.GRAY + "] " + ChatColor.RESET;
    private final String PREFIX_TOURNOI = ChatColor.GRAY + "[" + ChatColor.DARK_RED + "Tournoi" + ChatColor.GRAY + "] " + ChatColor.RESET;

    private Location spawnMain;
    private Location spawnArene;
    private Location spawn1v1_1;
    private Location spawn1v1_2;
    private final List<Location> tournoiSpawns = new ArrayList<Location>();

    private final List<Player> queue1v1 = new ArrayList<Player>();
    private final Map<Player, Player> opponents = new HashMap<Player, Player>();

    private boolean tournoiEnCours = false;
    private final List<Player> tournoiParticipants = new ArrayList<Player>();

    // Stats & Data
    private final Map<UUID, Integer> kills = new HashMap<UUID, Integer>();
    private final Map<UUID, Integer> deaths = new HashMap<UUID, Integer>();
    private final Map<UUID, Integer> killstreaks = new HashMap<UUID, Integer>();
    private final Map<UUID, Integer> tournoiWins = new HashMap<UUID, Integer>();
    private final Map<UUID, String> playerRanks = new HashMap<UUID, String>();
    private final List<Player> hiddenScoreboards = new ArrayList<Player>();

    @Override
    public void onEnable() {
        loadDataFromConfig();
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("Plugin DatSoup 1.8 active avec succes !");
    }

    @Override
    public void onDisable() {
        saveDataToConfig();
    }

    private void loadDataFromConfig() {
        if (getConfig().contains("spawns.main")) spawnMain = (Location) getConfig().get("spawns.main");
        if (getConfig().contains("spawns.arene")) spawnArene = (Location) getConfig().get("spawns.arene");
        if (getConfig().contains("spawns.1v1_1")) spawn1v1_1 = (Location) getConfig().get("spawns.1v1_1");
        if (getConfig().contains("spawns.1v1_2")) spawn1v1_2 = (Location) getConfig().get("spawns.1v1_2");
        if (getConfig().contains("spawns.tournois")) {
            List<?> list = getConfig().getList("spawns.tournois");
            for (Object o : list) {
                if (o instanceof Location) tournoiSpawns.add((Location) o);
            }
        }

        if (getConfig().contains("players")) {
            for (String key : getConfig().getConfigurationSection("players").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    kills.put(uuid, getConfig().getInt("players." + key + ".kills", 0));
                    deaths.put(uuid, getConfig().getInt("players." + key + ".deaths", 0));
                    killstreaks.put(uuid, getConfig().getInt("players." + key + ".killstreak", 0));
                    tournoiWins.put(uuid, getConfig().getInt("players." + key + ".tournoiWins", 0));
                    playerRanks.put(uuid, getConfig().getString("players." + key + ".rank", "default"));
                } catch (Exception ignored) {}
            }
        }
    }

    private void saveDataToConfig() {
        if (spawnMain != null) getConfig().set("spawns.main", spawnMain);
        if (spawnArene != null) getConfig().set("spawns.arene", spawnArene);
        if (spawn1v1_1 != null) getConfig().set("spawns.1v1_1", spawn1v1_1);
        if (spawn1v1_2 != null) getConfig().set("spawns.1v1_2", spawn1v1_2);
        getConfig().set("spawns.tournois", tournoiSpawns);

        for (UUID uuid : playerRanks.keySet()) {
            getConfig().set("players." + uuid.toString() + ".kills", kills.getOrDefault(uuid, 0));
            getConfig().set("players." + uuid.toString() + ".deaths", deaths.getOrDefault(uuid, 0));
            getConfig().set("players." + uuid.toString() + ".killstreak", killstreaks.getOrDefault(uuid, 0));
            getConfig().set("players." + uuid.toString() + ".tournoiWins", tournoiWins.getOrDefault(uuid, 0));
            getConfig().set("players." + uuid.toString() + ".rank", playerRanks.getOrDefault(uuid, "default"));
        }
        saveConfig();
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

        if (name.equals("setspawn")) {
            spawnMain = p.getLocation();
            saveDataToConfig();
            p.sendMessage(PREFIX + ChatColor.GREEN + "Spawn principal defini !");
        } else if (name.equals("setarenespawn")) {
            spawnArene = p.getLocation();
            saveDataToConfig();
            p.sendMessage(PREFIX + ChatColor.GREEN + "Spawn de l'Arene FFA defini !");
        } else if (name.equals("set1v1spawn1")) {
            spawn1v1_1 = p.getLocation();
            saveDataToConfig();
            p.sendMessage(PREFIX_1V1 + ChatColor.GREEN + "Spawn 1v1 #1 defini !");
        } else if (name.equals("set1v1spawn2")) {
            spawn1v1_2 = p.getLocation();
            saveDataToConfig();
            p.sendMessage(PREFIX_1V1 + ChatColor.GREEN + "Spawn 1v1 #2 defini !");
        } else if (name.equals("settournoispawn")) {
            tournoiSpawns.add(p.getLocation());
            saveDataToConfig();
            p.sendMessage(PREFIX_TOURNOI + ChatColor.GREEN + "Spawn tournoi #" + tournoiSpawns.size() + " ajoute !");
        } else if (name.equals("menu")) {
            openMenu(p);
        } else if (name.equals("queue1v1")) {
            joinQueue1v1(p);
        } else if (name.equals("tournoi")) {
            if (args.length > 0 && args[0].equalsIgnoreCase("start")) {
                startTournoi(p);
            } else {
                p.sendMessage(PREFIX_TOURNOI + ChatColor.YELLOW + "Usage: /tournoi start");
            }
        } else if (name.equals("sb") || name.equals("scoreboard")) {
            toggleScoreboard(p);
        } else if (name.equals("setrank")) {
            if (!p.hasPermission("souppvp.admin") && !p.isOp()) {
                p.sendMessage(PREFIX + ChatColor.RED + "Tu n'as pas la permission.");
                return true;
            }
            if (args.length < 2) {
                p.sendMessage(PREFIX + ChatColor.YELLOW + "Usage: /setrank <joueur> <admin|mod|famous|vip|default>");
                return true;
            }
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                p.sendMessage(PREFIX + ChatColor.RED + "Joueur introuvable.");
                return true;
            }
            setPlayerRank(target, args[1].toLowerCase());
            saveDataToConfig();
            p.sendMessage(PREFIX + ChatColor.GREEN + "Grade de " + target.getName() + " mis a jour !");
        }
        return true;
    }

    private void sendHelpMenu(Player p) {
        p.sendMessage(" ");
        p.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "===== COMMANDES DATSOUP =====");
        p.sendMessage(ChatColor.YELLOW + "/menu " + ChatColor.GRAY + "- Ouvre le menu des modes de jeu");
        p.sendMessage(ChatColor.YELLOW + "/queue1v1 " + ChatColor.GRAY + "- Rejoindre la file d'attente 1v1");
        p.sendMessage(ChatColor.YELLOW + "/sb ou /scoreboard " + ChatColor.GRAY + "- Masquer/Afficher le scoreboard");
        
        if (p.hasPermission("souppvp.admin") || p.isOp()) {
            p.sendMessage(" ");
            p.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "===== COMMANDES ADMIN =====");
            p.sendMessage(ChatColor.RED + "/setspawn " + ChatColor.GRAY + "- Definir le Spawn principal");
            p.sendMessage(ChatColor.RED + "/setarenespawn " + ChatColor.GRAY + "- Definir le Spawn de l'Arene FFA");
            p.sendMessage(ChatColor.RED + "/set1v1spawn1 " + ChatColor.GRAY + "- Definir le 1er spawn 1v1");
            p.sendMessage(ChatColor.RED + "/set1v1spawn2 " + ChatColor.GRAY + "- Definir le 2eme spawn 1v1");
            p.sendMessage(ChatColor.RED + "/settournoispawn " + ChatColor.GRAY + "- Ajouter un spawn de Tournoi");
            p.sendMessage(ChatColor.RED + "/tournoi start " + ChatColor.GRAY + "- Lancer le tournoi (min 4 joueurs)");
            p.sendMessage(ChatColor.RED + "/setrank <joueur> <rank> " + ChatColor.GRAY + "- Changer le grade (admin, mod, famous, vip, default)");
        }
        p.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "=============================");
        p.sendMessage(" ");
    }

    private void setPlayerRank(Player p, String rank) {
        playerRanks.put(p.getUniqueId(), rank);
        updateAllPlayerNameTags();
        updateScoreboard(p);
    }

    private ChatColor getRankColor(String rank) {
        if (rank.equals("admin")) return ChatColor.RED;
        if (rank.equals("mod")) return ChatColor.DARK_PURPLE;
        if (rank.equals("famous")) return ChatColor.AQUA;
        if (rank.equals("vip")) return ChatColor.GOLD;
        return ChatColor.WHITE;
    }

    private void updateAllPlayerNameTags() {
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            Scoreboard sb = viewer.getScoreboard();
            if (sb == Bukkit.getScoreboardManager().getMainScoreboard()) {
                sb = Bukkit.getScoreboardManager().getNewScoreboard();
                viewer.setScoreboard(sb);
            }

            Team tAdmin = getOrCreateTeam(sb, "01Admin", ChatColor.RED);
            Team tMod = getOrCreateTeam(sb, "02Mod", ChatColor.DARK_PURPLE);
            Team tFamous = getOrCreateTeam(sb, "03Famous", ChatColor.AQUA);
            Team tVip = getOrCreateTeam(sb, "04Vip", ChatColor.GOLD);
            Team tDefault = getOrCreateTeam(sb, "05Default", ChatColor.WHITE);

            for (Player target : Bukkit.getOnlinePlayers()) {
                String r = playerRanks.getOrDefault(target.getUniqueId(), "default");
                ChatColor color = getRankColor(r);

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

                // Troncature du TabList si le nom + couleur dépasse 16 caractères
                String listName = color + target.getName();
                if (listName.length() > 16) {
                    listName = listName.substring(0, 16);
                }
                target.setPlayerListName(listName);
            }
        }
    }

    private Team getOrCreateTeam(Scoreboard sb, String name, ChatColor color) {
        Team t = sb.getTeam(name);
        if (t == null) {
            t = sb.registerNewTeam(name);
        }
        t.setPrefix(color.toString());
        return t;
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {
        Player p = e.getPlayer();
        String rank = playerRanks.getOrDefault(p.getUniqueId(), "default");
        ChatColor color = getRankColor(rank);

        e.setFormat(color + "%1$s" + ChatColor.RESET + ": %2$s");
    }

    @EventHandler
    public void onFoodChange(FoodLevelChangeEvent e) {
        e.setCancelled(true);
        if (e.getEntity() instanceof Player) {
            ((Player) e.getEntity()).setFoodLevel(20);
        }
    }

    // ----------------------------------------------------
    // SCOREBOARD SÉCURISÉ (FORCE '0' POUR KILLS SI NON DÉFINI)
    // ----------------------------------------------------
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
        obj.setDisplayName(ChatColor.RED + "" + ChatColor.BOLD + "SoupTournament");

        UUID uuid = p.getUniqueId();
        
        int k = kills.get(uuid) != null ? kills.get(uuid) : 0;
        int d = deaths.get(uuid) != null ? deaths.get(uuid) : 0;
        int ks = killstreaks.get(uuid) != null ? killstreaks.get(uuid) : 0;
        int wins = tournoiWins.get(uuid) != null ? tournoiWins.get(uuid) : 0;
        double kd = (d == 0) ? k : (double) Math.round(((double) k / d) * 10.0) / 10.0;

        String rank = playerRanks.getOrDefault(uuid, "default");
        ChatColor rankColor = getRankColor(rank);

        // Sécurité stricte 16 caractères max par ligne
        addSafeScore(obj, rankColor + safeString(p.getName(), 14), 11);
        addSafeScore(obj, ChatColor.GRAY + " ", 10);
        addSafeScore(obj, ChatColor.GRAY + "Kills", 9);
        addSafeScore(obj, ChatColor.RED + "" + k, 8);
        addSafeScore(obj, ChatColor.GRAY + "Death", 7);
        addSafeScore(obj, ChatColor.RED + "" + d, 6);
        addSafeScore(obj, ChatColor.GRAY + "Ratio", 5);
        addSafeScore(obj, ChatColor.RED + "" + kd, 4);
        addSafeScore(obj, ChatColor.GRAY + "KS", 3);
        addSafeScore(obj, ChatColor.RED + "" + ks, 2);
        addSafeScore(obj, ChatColor.GRAY + "Tournament win", 1);
        addSafeScore(obj, ChatColor.RED + "" + wins + " ", 0);
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
            p.sendMessage(PREFIX + ChatColor.GREEN + "Scoreboard affiche !");
        } else {
            hiddenScoreboards.add(p);
            p.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
            p.sendMessage(PREFIX + ChatColor.RED + "Scoreboard masque !");
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
        if (!playerRanks.containsKey(uuid)) playerRanks.put(uuid, "default");

        for (Player online : Bukkit.getOnlinePlayers()) {
            online.showPlayer(p);
            p.showPlayer(online);
        }

        teleportToSpawn(p);
        updateAllPlayerNameTags();
        updateScoreboard(p);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        e.setQuitMessage(null);

        Player p = e.getPlayer();
        queue1v1.remove(p);
        if (opponents.containsKey(p)) {
            Player winner = opponents.remove(p);
            opponents.remove(winner);
            resetVanish(winner);
            teleportToSpawn(winner);
            updateScoreboard(winner);
            winner.sendMessage(PREFIX_1V1 + ChatColor.GREEN + "L'adversaire s'est deconnecte.");
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
        
        ItemStack boussole = createItem(Material.COMPASS, ChatColor.GREEN + "" + ChatColor.BOLD + "Selection de Mode");
        p.getInventory().setItem(4, boussole);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if (p.getItemInHand() != null) {
            if (p.getItemInHand().getType() == Material.COMPASS && e.getAction().name().contains("RIGHT")) {
                e.setCancelled(true);
                openMenu(p);
            } else if (p.getItemInHand().getType() == Material.REDSTONE && e.getAction().name().contains("RIGHT")) {
                e.setCancelled(true);
                leaveQueue1v1(p);
            }
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent e) {
        if (e.getItemDrop().getItemStack().getType() == Material.REDSTONE) {
            e.setCancelled(true);
        }
    }

    private void openMenu(Player p) {
        Inventory inv = Bukkit.createInventory(null, 27, ChatColor.DARK_GRAY + "Menu Principal");
        
        inv.setItem(11, createItem(Material.IRON_SWORD, ChatColor.RED + "" + ChatColor.BOLD + "Arene FFA"));
        inv.setItem(13, createItem(Material.DIAMOND_SWORD, ChatColor.YELLOW + "" + ChatColor.BOLD + "Mode 1v1 Duel"));
        inv.setItem(15, createItem(Material.NETHER_STAR, ChatColor.GREEN + "" + ChatColor.BOLD + "Retour au Spawn"));

        p.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (e.getView() == null || e.getView().getTitle() == null) return;
        
        if (e.getCurrentItem() != null && e.getCurrentItem().getType() == Material.REDSTONE) {
            e.setCancelled(true);
            return;
        }

        if (!e.getView().getTitle().equals(ChatColor.DARK_GRAY + "Menu Principal")) return;
        
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player)) return;

        Player p = (Player) e.getWhoClicked();
        int slot = e.getRawSlot();

        if (slot == 11) {
            p.closeInventory();
            if (spawnArene != null) {
                p.teleport(spawnArene);
                giveKit(p);
                p.sendMessage(PREFIX + ChatColor.GREEN + "Teleporte dans l'Arene FFA !");
            }
        } else if (slot == 13) {
            p.closeInventory();
            joinQueue1v1(p);
        } else if (slot == 15) {
            p.closeInventory();
            teleportToSpawn(p);
        }
    }

    private void joinQueue1v1(Player p) {
        if (opponents.containsKey(p)) {
            p.sendMessage(PREFIX_1V1 + ChatColor.RED + "Tu es deja en duel !");
            return;
        }
        if (queue1v1.contains(p)) {
            p.sendMessage(PREFIX_1V1 + ChatColor.RED + "Tu es deja en recherche.");
            return;
        }
        if (spawn1v1_1 == null || spawn1v1_2 == null) {
            p.sendMessage(PREFIX_1V1 + ChatColor.RED + "Spawns 1v1 non me-configures !");
            return;
        }

        queue1v1.add(p);
        p.sendMessage(PREFIX_1V1 + ChatColor.GREEN + "File 1v1 rejointe...");

        p.getInventory().clear();
        p.getInventory().setArmorContents(null);
        ItemStack redstone = createItem(Material.REDSTONE, ChatColor.RED + "" + ChatColor.BOLD + "Quitter la recherche");
        p.getInventory().setItem(8, redstone);

        if (queue1v1.size() >= 2) {
            Player p1 = queue1v1.remove(0);
            Player p2 = queue1v1.remove(0);

            opponents.put(p1, p2);
            opponents.put(p2, p1);

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

            p1.sendMessage(PREFIX_1V1 + ChatColor.GREEN + "Duel contre " + p2.getName() + " !");
            p2.sendMessage(PREFIX_1V1 + ChatColor.GREEN + "Duel contre " + p1.getName() + " !");
        }
    }

    private void leaveQueue1v1(Player p) {
        if (queue1v1.contains(p)) {
            queue1v1.remove(p);
            teleportToSpawn(p);
            p.sendMessage(PREFIX_1V1 + ChatColor.RED + "Tu as quitte la file d'attente 1v1.");
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
        updateScoreboard(victim);

        if (victim.getKiller() != null) {
            Player killer = victim.getKiller();
            UUID kUuid = killer.getUniqueId();
            kills.put(kUuid, kills.getOrDefault(kUuid, 0) + 1);
            killstreaks.put(kUuid, killstreaks.getOrDefault(kUuid, 0) + 1);
            updateScoreboard(killer);
        }

        saveDataToConfig();

        if (opponents.containsKey(victim)) {
            final Player winner = opponents.remove(victim);
            opponents.remove(winner);

            resetVanish(victim);
            resetVanish(winner);

            winner.sendMessage(PREFIX_1V1 + ChatColor.GREEN + "Victoire contre " + victim.getName() + " !");
            victim.sendMessage(PREFIX_1V1 + ChatColor.RED + "Defaite !");

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
            Bukkit.broadcastMessage(PREFIX_TOURNOI + ChatColor.YELLOW + victim.getName() + ChatColor.RED + " elimine ! (" + tournoiParticipants.size() + " restants)");

            if (tournoiParticipants.size() == 1) {
                final Player winner = tournoiParticipants.get(0);
                UUID wUuid = winner.getUniqueId();
                tournoiWins.put(wUuid, tournoiWins.getOrDefault(wUuid, 0) + 1);

                Bukkit.broadcastMessage(PREFIX_TOURNOI + ChatColor.GOLD + "" + ChatColor.BOLD + "VICTOIRE DE " + winner.getName() + " !");
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

    private void resetVanish(Player p) {
        for (Player online : Bukkit.getOnlinePlayers()) {
            online.showPlayer(p);
            p.showPlayer(online);
        }
    }

    private void startTournoi(Player sender) {
        if (tournoiEnCours) return;
        if (Bukkit.getOnlinePlayers().size() < 4) {
            sender.sendMessage(PREFIX_TOURNOI + ChatColor.RED + "Il faut au moins 4 joueurs !");
            return;
        }

        tournoiEnCours = true;
        Bukkit.broadcastMessage(PREFIX_TOURNOI + ChatColor.YELLOW + "Debut du tournoi dans 30 secondes !");

        new BukkitRunnable() {
            int timer = 30;

            @Override
            public void run() {
                if (timer == 10 || (timer <= 5 && timer > 0)) {
                    Bukkit.broadcastMessage(PREFIX_TOURNOI + ChatColor.RED + "Lancement dans " + timer + "s !");
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
                    Bukkit.broadcastMessage(PREFIX_TOURNOI + ChatColor.GREEN + "LE TOURNOI COMMENCE !");
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
        meta.setDisplayName(ChatColor.YELLOW + "Epee de Combat");
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