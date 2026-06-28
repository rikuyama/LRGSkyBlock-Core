package me.lrg.skyblock.core.model;

import java.util.UUID;

public class PlayerData {

    private final UUID uuid;
    private String name;

    private double coins;

    private int health;
    private int mana;

    public PlayerData(UUID uuid, String name) {

        this.uuid = uuid;
        this.name = name;

        this.coins = 0;

        this.health = 100;
        this.mana = 100;

    }

    public UUID getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getCoins() {
        return coins;
    }

    public void setCoins(double coins) {
        this.coins = coins;
    }

    public void addCoins(double amount) {
        this.coins += amount;
    }

    public void removeCoins(double amount) {
        this.coins -= amount;
    }

    public int getHealth() {
        return health;
    }

    public void setHealth(int health) {
        this.health = health;
    }

    public int getMana() {
        return mana;
    }

    public void setMana(int mana) {
        this.mana = mana;
    }

}