package com.souppvp.models;

import java.util.UUID;

public class PlayerData {

    private final UUID uuid;
    private int kills;
    private int deaths;
    private int killstreak;
    private int tournoiWins;
    private int elo;
    private String rank;

    public PlayerData(UUID uuid, int kills, int deaths, int killstreak, int tournoiWins, int elo, String rank) {
        this.uuid = uuid;
        this.kills = kills;
        this.deaths = deaths;
        this.killstreak = killstreak;
        this.tournoiWins = tournoiWins;
        this.elo = elo;
        this.rank = rank;
    }

    public UUID getUuid() { return uuid; }
    public int getKills() { return kills; }
    public void addKill() { this.kills++; this.killstreak++; }
    
    public int getDeaths() { return deaths; }
    public void addDeath() { this.deaths++; this.killstreak = 0; }

    public int getKillstreak() { return killstreak; }
    public int getTournoiWins() { return tournoiWins; }
    public void addTournoiWin() { this.tournoiWins++; }

    public int getElo() { return elo; }
    public void setElo(int elo) { this.elo = elo; }

    public String getRank() { return rank; }
    public void setRank(String rank) { this.rank = rank; }
}