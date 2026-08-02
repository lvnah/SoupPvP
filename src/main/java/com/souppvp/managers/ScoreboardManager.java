package com.souppvp.managers;

import com.souppvp.Main;
import com.souppvp.models.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

import java.util.ArrayList;
import java.util.List;

public class ScoreboardManager {

    private final Main main;
    private final List<Player> hiddenScoreboards = new ArrayList<Player>();

    public ScoreboardManager(Main main) {
        this.main = main;
    }

    public void updateScoreboard(Player p) {
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
        if (obj != null) obj.unregister();

        obj = pBoard.registerNewObjective("souptourn", "dummy");
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        obj.setDisplayName(main.getColorConfig().getSbTitle());

        PlayerData data = main.getPlayerManager().getPlayerData(p);
        String rankColor = main.getColorConfig().color(main.getColorConfig().getRankColorCode(data.getRank()));

        addSafeScore(obj, ChatColor.GRAY + "Rank: " + rankColor + safeString(data.getRank(), 8), 3);
        addSafeScore(obj, ChatColor.GRAY + "Elo: " + main.getColorConfig().getSbValueColor() + data.getElo(), 2);
        addSafeScore(obj, ChatColor.GRAY + "Wins: " + main.getColorConfig().getSbValueColor() + data.getTournoiWins(), 1);
    }

    private void addSafeScore(Objective obj, String text, int score) {
        if (text.length() > 16) text = text.substring(0, 16);
        obj.getScore(text).setScore(score);
    }

    private String safeString(String text, int maxLen) {
        if (text == null) return "";
        return text.length() > maxLen ? text.substring(0, maxLen) : text;
    }

    public void toggleScoreboard(Player p) {
        if (hiddenScoreboards.contains(p)) {
            hiddenScoreboards.remove(p);
            updateScoreboard(p);
            p.sendMessage(main.getColorConfig().getPrefixTag() + ChatColor.GRAY + "Scoreboard affiche !");
        } else {
            hiddenScoreboards.add(p);
            p.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
            p.sendMessage(main.getColorConfig().getPrefixTag() + main.getColorConfig().color("&cScoreboard masque !"));
        }
    }
}