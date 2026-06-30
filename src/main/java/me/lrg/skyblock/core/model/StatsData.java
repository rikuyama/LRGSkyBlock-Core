package me.lrg.skyblock.core.model;

import java.util.Objects;
import java.util.UUID;

/**
 * プレイヤーのステータスデータを保持するModel。
 *
 * このクラスの役割:
 * - Healthを持つ
 * - Manaを持つ
 * - Strengthを持つ
 * - Defenseを持つ
 * - Speedを持つ
 *
 * 注意:
 * - SQLは書かない
 * - Bukkit APIは使わない
 * - ゲームロジックを詰め込みすぎない
 */
public class StatsData {

    private final UUID uuid;

    private double health;
    private double mana;
    private double strength;
    private double defense;
    private double speed;

    public StatsData(
            UUID uuid,
            double health,
            double mana,
            double strength,
            double defense,
            double speed
    ) {
        this.uuid = Objects.requireNonNull(uuid, "uuid");
        this.health = health;
        this.mana = mana;
        this.strength = strength;
        this.defense = defense;
        this.speed = speed;
    }

    public UUID getUuid() {
        return uuid;
    }

    public double getHealth() {
        return health;
    }

    public void setHealth(double health) {
        this.health = health;
    }

    public double getMana() {
        return mana;
    }

    public void setMana(double mana) {
        this.mana = mana;
    }

    public double getStrength() {
        return strength;
    }

    public void setStrength(double strength) {
        this.strength = strength;
    }

    public double getDefense() {
        return defense;
    }

    public void setDefense(double defense) {
        this.defense = defense;
    }

    public double getSpeed() {
        return speed;
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }
}