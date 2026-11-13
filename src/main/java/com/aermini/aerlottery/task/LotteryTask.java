package com.aermini.aerlottery.task;

import com.aermini.aerlottery.AerLottery;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class LotteryTask extends BukkitRunnable {
    private final Player player;
    private int elapsedTicks = 0;
    private boolean hasPlayedFinalSound = false;
    private final boolean legacySound;
    public LotteryTask(Player player) {
        this.player = player;
        this.legacySound = useLegacySound();
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
                            playClickSound();
                        }
                    });
        }

        if (elapsedTicks >= (AerLottery.getInstance().getConfig().getInt("total_time") / 50)) {
            if (!hasPlayedFinalSound) {
                playAnvilSound();
                hasPlayedFinalSound = true;
            }
            this.cancel();
            AerLottery.getInstance().decrementLotteryCount(player.getUniqueId());
            giveReward();
            return;
        }
        elapsedTicks++;
    }

    private int getClientProtocolVersion() {
        int viaVersion = getViaProtocolVersion();
        if (viaVersion > 0) {
            return viaVersion;
        }
        int supportVersion = getProtocolSupportVersion();
        if (supportVersion > 0) {
            return supportVersion;
        }
        try {
            Object handle = player.getClass().getMethod("getHandle").invoke(player);
            Object connection = handle.getClass().getField("playerConnection").get(handle);
            Object manager = connection.getClass().getField("networkManager").get(connection);
            java.lang.reflect.Method getVer = manager.getClass().getMethod("getVersion");
            return (int) getVer.invoke(manager);
        } catch (Exception e) {
            return -1;
        }
    }

    private int getViaProtocolVersion() {
        String[] classNames = {"com.viaversion.viaversion.api.Via", "us.myles.ViaVersion.api.Via"};
        for (String className : classNames) {
            try {
                Class<?> viaClass = Class.forName(className);
                Object api = viaClass.getMethod("getAPI").invoke(null);
                if (api == null) {
                    continue;
                }
                Object version = api.getClass().getMethod("getPlayerVersion", Object.class).invoke(api, player);
                if (version instanceof Integer) {
                    return (Integer) version;
                }
            } catch (Throwable ignored) {
            }
        }
        return -1;
    }

    private int getProtocolSupportVersion() {
        try {
            Class<?> apiClass = Class.forName("protocolsupport.api.ProtocolSupportAPI");
            Object version = apiClass.getMethod("getProtocolVersion", Player.class).invoke(null, player);
            if (version == null) {
                return -1;
            }
            if (version instanceof Integer) {
                return (Integer) version;
            }
            Object id = version.getClass().getMethod("getId").invoke(version);
            if (id instanceof Integer) {
                return (Integer) id;
            }
        } catch (Throwable ignored) {
        }
        return -1;
    }

    private boolean useLegacySound() {
        int version = getClientProtocolVersion();
        if (version > 0) {
            // ViaRewind 3.1.x 会取消未映射的自定义音效名, 退回现代名避免静音
            return version < 107 && !isViaRewind31();
        }
        return Bukkit.getBukkitVersion().startsWith("1.8");
    }

    private boolean isViaRewind31() {
        Plugin viaRewind = Bukkit.getPluginManager().getPlugin("ViaRewind");
        return viaRewind != null && viaRewind.getDescription().getVersion().startsWith("3.1");
    }

    private void playClickSound() {
        if (legacySound) {
            player.playSound(player.getLocation(), "random.click", 1.0f, 1.0f);
        } else {
            player.playSound(player.getLocation(), "ui.button.click", 1.0f, 1.0f);
        }
    }

    private void playAnvilSound() {
        if (legacySound) {
            player.playSound(player.getLocation(), "random.anvil_use", 1.0f, 1.0f);
        } else {
            player.playSound(player.getLocation(), "block.anvil.use", 1.0f, 1.0f);
        }
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
            playAnvilSound();
            String rewardMsg = AerLottery.getInstance().getConfig().getString("reward_msg", "&a恭喜获得{reward}");
            rewardMsg = ChatColor.translateAlternateColorCodes('&', rewardMsg.replace("{reward}", rewardName));
            player.sendMessage(rewardMsg);
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        });
    }
}