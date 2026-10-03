package net.capybarasmp.scoreboard;

import java.math.BigInteger;

public final class PlayerData {
    public int kills;
    public int deaths;
    public long playtimeSeconds;
    public int lives;
    public BigInteger money;
    public String rank = "";
    public boolean admin;

    public PlayerData(int lives, BigInteger money) {
        this.lives = lives;
        this.money = money;
    }
}
