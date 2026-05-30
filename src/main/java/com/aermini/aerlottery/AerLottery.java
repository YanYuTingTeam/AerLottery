package com.aermini.aerlottery;

import com.aermini.aerlottery.command.AerLotteryCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class AerLottery extends JavaPlugin {
    private static AerLottery instance;
    private final ConcurrentHashMap<UUID, Integer> playerLotteryCount = new ConcurrentHashMap<>();

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        this.getCommand("aerlottery").setExecutor(new AerLotteryCommand());
        getLogger().info("AerLottery 已启用");
    }

    @Override
    public void onDisable() {
        getLogger().info("AerLottery 已禁用");
    }

    public static AerLottery getInstance() {
        return instance;
    }

    public void incrementLotteryCount(UUID uuid) {
        playerLotteryCount.put(uuid, playerLotteryCount.getOrDefault(uuid, 0) + 1);
    }

    public void decrementLotteryCount(UUID uuid) {
        playerLotteryCount.computeIfPresent(uuid, (k, v) -> (v > 0) ? v - 1 : null);
    }

    public int getLotteryCount(UUID uuid) {
        return playerLotteryCount.getOrDefault(uuid, 0);
    }
}