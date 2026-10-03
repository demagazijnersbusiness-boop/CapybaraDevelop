package net.capybarasmp.scoreboard;

import java.math.BigInteger;
import java.util.HashSet;
import java.util.Set;

public final class PlayerData {
    public int kills;
    public int deaths;
    public long playtimeSeconds;
    public int lives;
    public BigInteger money;
    public String rank = "";
    public boolean admin;

    // Emerald currency (earned by being AFK)
    public long emerald;
    public long lastSpawnerBuy;

    // personal scoreboard settings (/scoreboard)
    public boolean boardHidden;
    public final Set<String> hiddenLines = new HashSet<>();

    public PlayerData(int lives, BigInteger money) {
        this.lives = lives;
        this.money = money;
    }
}
