package com.aermini.aerlottery.command;

import com.aermini.aerlottery.AerLottery;
import com.aermini.aerlottery.task.LotteryTask;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AerLotteryCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "只有玩家可以执行此命令");
            return true;
        }

        Player player = (Player) sender;
        if (args.length == 0) return false;
        if (args[0].equalsIgnoreCase("start")) {
            if (args.length < 2) return false;
            String key = args[1];
            if (!key.equalsIgnoreCase("YYT2025SERVER")) return false;
            new LotteryTask(player).runTaskTimerAsynchronously(AerLottery.getInstance(), 0L, 1L);
            return true;

        } else if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("aerlottery.reload")) return true;
            AerLottery.getInstance().reloadConfig();
            player.sendMessage(ChatColor.GREEN + "配置已重载");
            return true;
        }

        return false;
    }
}