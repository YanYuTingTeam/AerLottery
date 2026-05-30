package com.aermini.aerlottery.task;

import com.aermini.aerlottery.AerLottery;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class LotteryTask extends BukkitRunnable {
    private final Player player;
    private int elapsedTicks = 0;
    private boolean hasPlayedFinalSound = false;
    public LotteryTask(Player player) {
        this.player = player;
        AerLottery.getInstance().incrementLotteryCount(player.getUniqueId());
    }

    @Override
    public void run() {
        if (player == null || !player.isOnline()) {
            this.cancel();
            AerLottery.getInstance().decrementLotteryCount(player.getUniqueId());
            return;
        }
        long elapsedMillis = elapsedTicks * 50L;
        ConfigurationSection waitSection = AerLottery.getInstance().getConfig().getConfigurationSection("wait");
        if (waitSection != null) {
            waitSection.getKeys(false).stream()
                    .map(Integer::parseInt)
                    .sorted()
                    .forEach(step -> {
                        long time = waitSection.getLong(step + ".time");
                        if (elapsedMillis >= time && elapsedMillis - 50 < time) {
                            List<String> messages = waitSection.getStringList(step + ".message");
                            for (String msg : messages) {
                                player.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
                            }
                            player.playSound(player.getLocation(), Sound.valueOf("CLICK"), 1.0f, 1.0f);
                        }
                    });
        }

        if (elapsedTicks >= (AerLottery.getInstance().getConfig().getInt("total_time") / 50)) {
            if (!hasPlayedFinalSound) {
                player.playSound(player.getLocation(), Sound.valueOf("ANVIL_USE"), 1.0f, 1.0f);
                hasPlayedFinalSound = true;
            }
            this.cancel();
            AerLottery.getInstance().decrementLotteryCount(player.getUniqueId());
            giveReward();
            return;
        }
        elapsedTicks++;
    }

    private void giveReward() {
        List<String> rewards = AerLottery.getInstance().getConfig().getStringList("rewards");
        if (rewards.isEmpty()) {
            player.sendMessage(ChatColor.RED + "配置文件中没有设置任何奖励");
            return;
        }

        String chosenRewardString = rewards.get(ThreadLocalRandom.current().nextInt(rewards.size()));
        String[] parts = chosenRewardString.split("\\|");

        if (parts.length != 3) {
            AerLottery.getInstance().getLogger().severe("奖励格式错误 -> " + chosenRewardString);
            player.sendMessage(ChatColor.RED + "奖励配置错误");
            return;
        }

        String rewardName = ChatColor.translateAlternateColorCodes('&', parts[0]);
        String command = parts[1].replace("%player%", player.getName());

        Bukkit.getScheduler().runTask(AerLottery.getInstance(), () -> {
            player.playSound(player.getLocation(), Sound.valueOf("ANVIL_USE"), 1.0f, 1.0f);
            String rewardMsg = AerLottery.getInstance().getConfig().getString("reward_msg", "&a恭喜获得{reward}");
            rewardMsg = ChatColor.translateAlternateColorCodes('&', rewardMsg.replace("{reward}", rewardName));
            player.sendMessage(rewardMsg);
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        });
    }
}