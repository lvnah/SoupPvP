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
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class Main extends JavaPlugin implements Listener, CommandExecutor {

    private final String PREFIX = ChatColor.GRAY + "[" + ChatColor.RED + "SoupPvP" + ChatColor.GRAY + "] " + ChatColor.RESET;
    private final String PREFIX_1V1 = ChatColor.GRAY + "[" + ChatColor.YELLOW + "1v1" + ChatColor.GRAY + "] " + ChatColor.RESET;
    private final String PREFIX_TOURNOI = ChatColor.GRAY + "[" + ChatColor.DARK_RED + "Tournoi" + ChatColor.GRAY + "] " + ChatColor.RESET;

    private Location spawnMain;
    private Location spawnArene;
    private Location spawn1v1_1;
    private Location spawn1v1_2;
    private final List<Location> tournoiSpawns = new ArrayList<>();

    // Duels 1v1
    private final List<Player> queue1v1 = new ArrayList<>();
    private final Map<Player, Player> opponents = new HashMap<>();

    // Tournois
    private boolean tournoiEnCours = false;
    private final List<Player> tournoiParticipants = new ArrayList<>();

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("Plugin SoupPvP 1.8 active avec succes !");
    }

    // ----------------------------------------------------
    // GESTION DES COMMANDES
    // ----------------------------------------------------
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player p = (Player) sender;

        switch (cmd.getName().toLowerCase()) {
            case "setspawn":
                spawnMain = p.getLocation();
                p.sendMessage(PREFIX + ChatColor.GREEN + "Spawn principal défini !");
                break;

            case "setarenespawn":
                spawnArene = p.getLocation();
                p.sendMessage(PREFIX + ChatColor.GREEN + "Spawn de l'Arène FFA défini !");
                break;

            case "set1v1spawn1":
                spawn1v1_1 = p.getLocation();
                p.sendMessage(PREFIX_1V1 + ChatColor.GREEN + "Spawn 1v1 #1 défini !");
                break;

            case "set1v1spawn2":
                spawn1v1_2 = p.getLocation();
                p.sendMessage(PREFIX_1V1 + ChatColor.GREEN + "Spawn 1v1 #2 défini !");
                break;

            case "settournoispawn":
                tournoiSpawns.add(p.getLocation());
                p.sendMessage(PREFIX_TOURNOI + ChatColor.GREEN + "Spawn tournoi #" + tournoiSpawns.size() + " ajouté !");
                break;

            case "menu":
                openMenu(p);
                break;

            case "queue1v1":
                joinQueue1v1(p);
                break;

            case "tournoi":
                if (args.length > 0 && args[0].equalsIgnoreCase("start")) {
                    startTournoi(p);
                }
                break;
        }
        return true;
    }

    // ----------------------------------------------------
    // ÉVÉNEMENTS DU SERVEUR
    // ----------------------------------------------------

    // 1. Instant Soup
    @EventHandler
    public void onSoup(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if ((e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK)
                && p.getItemInHand().getType() == Material.MUSHROOM_SOUP) {
            
            if (p.getHealth() < p.getMaxHealth()) {
                e.setCancelled(true);
                p.setHealth(Math.min(p.getHealth() + 6.0, p.getMaxHealth())); // 6 HP = 3 cœurs
                p.getItemInHand().setType(Material.BOWL);
            }
        }
    }

    // 2. Connexion / Respawn & Boussole
    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        for (Player online : Bukkit.getOnlinePlayers()) {
            online.showPlayer(p);
            p.showPlayer(online);
        }
        teleportToSpawn(p);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        Bukkit.getScheduler().runTaskLater(this, () -> teleportToSpawn(e.getPlayer()), 1L);
    }

    private void teleportToSpawn(Player p) {
        if (spawnMain != null) p.teleport(spawnMain);
        p.getInventory().clear();
        p.getInventory().setArmorContents(null);
        
        ItemStack boussole = createItem(Material.COMPASS, ChatColor.GREEN + "" + ChatColor.BOLD + "Sélection de Mode");
        p.getInventory().setItem(4, boussole);
    }

    // 3. Clic Boussole & Menu GUI
    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if (p.getItemInHand().getType() == Material.COMPASS && e.getAction().name().contains("RIGHT")) {
            e.setCancelled(true);
            openMenu(p);
        }
    }

    private void openMenu(Player p) {
        Inventory inv = Bukkit.createInventory(null, 27, ChatColor.DARK_GRAY + "Menu Principal");
        
        inv.setItem(11, createItem(Material.IRON_SWORD, ChatColor.RED + "" + ChatColor.BOLD + "Arène FFA"));
        inv.setItem(13, createItem(Material.DIAMOND_SWORD, ChatColor.YELLOW + "" + ChatColor.BOLD + "Mode 1v1 Duel"));
        inv.setItem(15, createItem(Material.NETHER_STAR, ChatColor.GREEN + "" + ChatColor.BOLD + "Retour au Spawn"));

        p.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!e.getView().getTitle().equals(ChatColor.DARK_GRAY + "Menu Principal")) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player)) return;

        Player p = (Player) e.getWhoClicked();
        int slot = e.getRawSlot();

        if (slot == 11) { // Arène FFA
            p.closeInventory();
            if (spawnArene != null) {
                p.teleport(spawnArene);
                giveKit(p);
                p.sendMessage(PREFIX + ChatColor.GREEN + "Téléporté dans l'Arène FFA !");
            }
        } else if (slot == 13) { // 1v1
            p.closeInventory();
            joinQueue1v1(p);
        } else if (slot == 15) { // Hub
            p.closeInventory();
            teleportToSpawn(p);
        }
    }

    // ----------------------------------------------------
    // SYSTÈME DE DUEL 1v1 (SANS SE VOIR)
    // ----------------------------------------------------
    private void joinQueue1v1(Player p) {
        if (opponents.containsKey(p)) {
            p.sendMessage(PREFIX_1V1 + ChatColor.RED + "Tu es déjà en duel !");
            return;
        }
        if (queue1v1.contains(p)) {
            p.sendMessage(PREFIX_1V1 + ChatColor.RED + "Tu es déjà en recherche.");
            return;
        }
        if (spawn1v1_1 == null || spawn1v1_2 == null) {
            p.sendMessage(PREFIX_1V1 + ChatColor.RED + "Spawns 1v1 non configurés !");
            return;
        }

        queue1v1.add(p);
        p.sendMessage(PREFIX_1V1 + ChatColor.GREEN + "File 1v1 rejointe...");

        if (queue1v1.size() >= 2) {
            Player p1 = queue1v1.remove(0);
            Player p2 = queue1v1.remove(0);

            opponents.put(p1, p2);
            opponents.put(p2, p1);

            // Masquer les duellistes pour tous les autres joueurs
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

    // ----------------------------------------------------
    // DÉGÂTS & SÉCURITÉ DUELS (2 HP / 3 HP CRITIQUE)
    // ----------------------------------------------------
    @EventHandler
    public void onDamage(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player) || !(e.getEntity() instanceof Player)) return;

        Player attacker = (Player) e.getDamager();
        Player victim = (Player) e.getEntity();

        // Isolation 1v1
        if (opponents.containsKey(attacker)) {
            if (opponents.get(attacker) != victim) {
                e.setCancelled(true);
                return;
            }
        }

        // Dégâts ajustés pour l'épée en pierre
        if (attacker.getItemInHand().getType() == Material.STONE_SWORD) {
            if (!attacker.isOnGround() && attacker.getFallDistance() > 0) {
                e.setDamage(3.0); // 3 HP (Critique)
            } else {
                e.setDamage(2.0); // 2 HP (Normal)
            }
        }
    }

    // ----------------------------------------------------
    // FIN DE DUEL & TOURNOI SUR MORT
    // ----------------------------------------------------
    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        Player victim = e.getEntity();
        e.getDrops().clear();

        // Gestion Fin 1v1
        if (opponents.containsKey(victim)) {
            Player winner = opponents.remove(victim);
            opponents.remove(winner);

            resetVanish(victim);
            resetVanish(winner);

            winner.sendMessage(PREFIX_1V1 + ChatColor.GREEN + "Victoire contre " + victim.getName() + " !");
            victim.sendMessage(PREFIX_1V1 + ChatColor.RED + "Défaite !");

            Bukkit.getScheduler().runTaskLater(this, () -> teleportToSpawn(winner), 40L);
        }

        // Gestion Élimination Tournoi
        if (tournoiEnCours && tournoiParticipants.contains(victim)) {
            tournoiParticipants.remove(victim);
            Bukkit.broadcastMessage(PREFIX_TOURNOI + ChatColor.YELLOW + victim.getName() + ChatColor.RED + " éliminé ! (" + tournoiParticipants.size() + " restants)");

            if (tournoiParticipants.size() == 1) {
                Player winner = tournoiParticipants.get(0);
                Bukkit.broadcastMessage(PREFIX_TOURNOI + ChatColor.GOLD + "" + ChatColor.BOLD + "VICTOIRE DE " + winner.getName() + " !");
                tournoiEnCours = false;
                tournoiParticipants.clear();
                Bukkit.getScheduler().runTaskLater(this, () -> teleportToSpawn(winner), 60L);
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
            winner.sendMessage(PREFIX_1V1 + ChatColor.GREEN + "L'adversaire s'est déconnecté.");
        }
    }

    private void resetVanish(Player p) {
        for (Player online : Bukkit.getOnlinePlayers()) {
            online.showPlayer(p);
            p.showPlayer(online);
        }
    }

    // ----------------------------------------------------
    // SYSTEME DE TOURNOI AUTOMATIQUE
    // ----------------------------------------------------
    private void startTournoi(Player sender) {
        if (tournoiEnCours) return;
        if (Bukkit.getOnlinePlayers().size() < 4) {
            sender.sendMessage(PREFIX_TOURNOI + ChatColor.RED + "Il faut au moins 4 joueurs !");
            return;
        }

        tournoiEnCours = true;
        Bukkit.broadcastMessage(PREFIX_TOURNOI + ChatColor.YELLOW + "Début du tournoi dans 30 secondes !");

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

    // ----------------------------------------------------
    // OUTILS (KIT & ITEMS)
    // ----------------------------------------------------
    private void giveKit(Player p) {
        p.getInventory().clear();
        p.getInventory().setArmorContents(null);

        // Épée en pierre Unbreakable
        ItemStack sword = new ItemStack(Material.STONE_SWORD);
        ItemMeta meta = sword.getItemMeta();
        meta.setDisplayName(ChatColor.YELLOW + "Épée de Combat");
        meta.spigot().setUnbreakable(true); // Méthode Native Spigot 1.8
        sword.setItemMeta(meta);

        p.getInventory().setItem(0, sword);

        // 35 Soupes
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
