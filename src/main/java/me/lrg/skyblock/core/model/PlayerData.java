package me.lrg.skyblock.core.model;

import java.util.Objects;
import java.util.UUID;

/**
 * プレイヤーの基本データを保持するクラス。
 *
 * このクラスの役割:
 * - UUIDを持つ
 * - プレイヤー名を持つ
 * - Coinsを持つ
 *
 * 注意:
 * - SQLを書かない
 * - ゲームロジックを書かない
 * - イベント処理を書かない
 */
public class PlayerData {

    private final UUID uuid;
    private String name;
    private long coins;

    public PlayerData(UUID uuid, String name, long coins) {
        this.uuid = Objects.requireNonNull(uuid, "uuid");
        this.name = Objects.requireNonNull(name, "name");
        this.coins = coins;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    /**
     * プレイヤー名は変更される可能性があるため、更新できるようにする。
     */
    public void setName(String name) {
        this.name = Objects.requireNonNull(name, "name");
    }

    public long getCoins() {
        return coins;
    }

    public void setCoins(long coins) {
        this.coins = coins;
    }
}