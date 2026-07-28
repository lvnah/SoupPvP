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
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
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

    // Stats & Scoreboard
    private final Map<Player, Integer> kills = new HashMap<Player, Integer>();
    private final Map<Player, Integer> deaths = new HashMap<Player, Integer>();
    private final List<Player> hiddenScoreboards = new ArrayList<Player>();

    // Teams Bukkit pour les Couleurs
    private Scoreboard mainScoreboard;
    private Team teamAdmin, teamMod, teamFamous, teamVip, teamDefault;

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        setupTeams();
        getLogger().info("Plugin SoupPvP 1.8 active avec succes !");
    }

    private void setupTeams() {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        mainScoreboard = manager.getMainScoreboard();

        teamAdmin = getOrCreateTeam("01Admin", ChatColor.RED);
        teamMod = getOrCreateTeam("02Mod", ChatColor.DARK_PURPLE);
        teamFamous = getOrCreateTeam("03Famous", ChatColor.AQUA);
        teamVip = getOrCreateTeam("04Vip", ChatColor.GOLD);
        teamDefault = getOrCreateTeam("05Default", ChatColor.WHITE);
    }

    private Team getOrCreateTeam(String name, ChatColor color) {
        Team team = mainScoreboard.getTeam(name);
        if (team == null) {
            team = mainScoreboard.registerNewTeam(name);
        }
        team.setPrefix(color.toString());
        return team;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player p = (Player) sender;

        String name = cmd.getName().toLowerCase();
        if (name.equals("setspawn")) {
            spawnMain = p.getLocation();
            p.sendMessage(PREFIX + ChatColor.GREEN + "Spawn principal defini !");
        } else if (name.equals("setarenespawn")) {
            spawnArene = p.getLocation();
            p.sendMessage(PREFIX + ChatColor.GREEN + "Spawn de l'Arene FFA defini !");
        } else if (name.equals("set1v1spawn1")) {
            spawn1v1_1 = p.getLocation();
            p.sendMessage(PREFIX_1V1 + ChatColor.GREEN + "Spawn 1v1 #1 defini !");
        } else if (name.equals("set1v1spawn2")) {
            spawn1v1_2 = p.getLocation();
            p.sendMessage(PREFIX_1V1 + ChatColor.GREEN + "Spawn 1v1 #2 defini !");
        } else if (name.equals("settournoispawn")) {
            tournoiSpawns.add(p.getLocation());
            p.sendMessage(PREFIX_TOURNOI + ChatColor.GREEN + "Spawn tournoi #" + tournoiSpawns.size() + " ajoute !");
        } else if (name.equals("menu")) {
            openMenu(p);
        } else if (name.equals("queue1v1")) {
            joinQueue1v1(p);
        } else if (name.equals("tournoi")) {
            if (args.length > 0 && args[0].equalsIgnoreCase("start")) {
                startTournoi(p);
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
            p.sendMessage(PREFIX + ChatColor.GREEN + "Grade de " + target.getName() + " mis a jour !");
        }
        return true;
    }

    private void setPlayerRank(Player p, String rank) {
        teamAdmin.removeEntry(p.getName());
        teamMod.removeEntry(p.getName());
        teamFamous.removeEntry(p.getName());
        teamVip.removeEntry(p.getName());
        teamDefault.removeEntry(p.getName());

        if (rank.equals("admin")) {
            teamAdmin.addEntry(p.getName());
            p.setPlayerListName(ChatColor.RED + p.getName());
        } else if (rank.equals("mod")) {
            teamMod.addEntry(p.getName());
            p.setPlayerListName(ChatColor.DARK_PURPLE + p.getName());
        } else if (rank.equals("famous")) {
            teamFamous.addEntry(p.getName());
            p.setPlayerListName(ChatColor.AQUA + p.getName());
        } else if (rank.equals("vip")) {
            teamVip.addEntry(p.getName());
            p.setPlayerListName(ChatColor.GOLD + p.getName());
        } else {
            teamDefault.addEntry(p.getName());
            p.setPlayerListName(ChatColor.WHITE + p.getName());
        }
    }

    // ----------------------------------------------------
    // GESTION SCOREBOARD
    // ----------------------------------------------------
    private void updateScoreboard(Player p) {
        if (hiddenScoreboards.contains(p)) {
            p.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
            return;
        }

        Scoreboard sb = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective obj = sb.registerNewObjective("datsoup", "dummy");
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        obj.setDisplayName(ChatColor.WHITE + "" + ChatColor.BOLD + "------------------");

        int k = kills.getOrDefault(p, 0);
        int d = deaths.getOrDefault(p, 0);
        double kd = (d == 0) ? k : (double) Math.round(((double) k / d) * 10.0) / 10.0;

        obj.getScore(ChatColor.RED + "DatSoup").setScore(7);
        obj.getScore(ChatColor.WHITE + "" + ChatColor.BOLD + "------------------").setScore(6);
        obj.getScore(ChatColor.WHITE + p.getName()).setScore(5);
        obj.getScore(ChatColor.WHITE + "Kills: " + ChatColor.GREEN + k).setScore(4);
        obj.getScore(ChatColor.WHITE + "Deaths: " + ChatColor.RED + d).setScore(3);
        obj.getScore(ChatColor.WHITE + "Kd: " + ChatColor.YELLOW + kd).setScore(2);
        obj.getScore(ChatColor.WHITE + "" + ChatColor.BOLD + "------------------ ").setScore(1);

        p.setScoreboard(sb);
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

    // ----------------------------------------------------
    // INTERACTION / SOUPS / FILE 1V1
    // ----------------------------------------------------
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
        Player p = e.getPlayer();
        if (!kills.containsKey(p)) kills.put(p, 0);
        if (!deaths.containsKey(p)) deaths.put(p, 0);

        setPlayerRank(p, "default");

        for (Player online : Bukkit.getOnlinePlayers()) {
            online.showPlayer(p);
            p.showPlayer(online);
        }
        teleportToSpawn(p);
        updateScoreboard(p);
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

        // INVENTAIRE COMPLETEMENT VIDÉ (N'A QUE LA REDSTONE EN SLOT 9)
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
                e.setDamage(5.0); // 2.5 cœurs
            } else {
                e.setDamage(4.0); // 2 cœurs
            }
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        final Player victim = e.getEntity();
        e.getDrops().clear();

        deaths.put(victim, deaths.getOrDefault(victim, 0) + 1);
        updateScoreboard(victim);

        if (victim.getKiller() != null) {
            Player killer = victim.getKiller();
            kills.put(killer, kills.getOrDefault(killer, 0) + 1);
            updateScoreboard(killer);
        }

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
                Bukkit.broadcastMessage(PREFIX_TOURNOI + ChatColor.GOLD + "" + ChatColor.BOLD + "VICTOIRE DE " + winner.getName() + " !");
                tournoiEnCours = false;
                tournoiParticipants.clear();
                Bukkit.getScheduler().runTaskLater(this, new Runnable() {
                    @Override
                    public void run() {
                        teleportToSpawn(winner);
                        updateScoreboard(winner);
                    }
                }, 60L);
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
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
