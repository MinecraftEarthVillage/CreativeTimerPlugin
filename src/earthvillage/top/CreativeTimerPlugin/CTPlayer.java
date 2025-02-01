package earthvillage.top.CreativeTimerPlugin;


import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CTPlayer {
    private UUID playerUUID;

    // 其他属性和方法

    public void setDuration(int duration) {
        this.duration = duration;
    }
    public static Map<UUID, CTPlayer> playerMap = new HashMap<>();
    GameMode def = GameMode.SURVIVAL;
    Player player;
    int duration;
    boolean isTicking;
    public CTPlayer(Player player, int time) {
        this.player = player;
        this.duration = time;
        playerMap.put(player.getUniqueId(), this);
    }
    public void add(int time) {
        this.duration += time;
    }


    //BOSS栏显示倒计时
    HashMap<UUID, BossBar> bossBars = new HashMap<>();
    public void sendActionBarMessage(Player player, String message) {
        // 检查玩家是否已经有BOSS栏，如果没有，则创建新的BOSS栏并添加到Map中
        if (!bossBars.containsKey(player.getUniqueId())) {
            BossBar bossBar = Bukkit.createBossBar("", BarColor.GREEN, BarStyle.SOLID);
            bossBar.addPlayer(player);
            bossBars.put(player.getUniqueId(), bossBar);
        }

        BossBar bossBar = bossBars.get(player.getUniqueId());

        // 更新BOSS栏消息
        bossBar.setTitle(ChatColor.BOLD+ message);
        // 倒计时归零后移除BOSS栏
        if (duration <= 0) {
            bossBar.removeAll();
            bossBars.remove(player.getUniqueId());
        }
    }


    public void tick() {
        duration--;
        // BOSS栏显示倒计时
        sendActionBarMessage(player, ChatColor.GREEN + "§l创造模式体验 剩余时间: " + duration);

        //如果时间=0
        if (duration == 0 && !CreativeTimerPlugin.允许保留物品和模式状态) {
            player.sendMessage("§l§6[创造模式体验系统]§r" + ChatColor.RED + "您的创造模式时间已经结束");
            // 获取插件实例并调用清空背包但保留白名单物品方法
            CreativeTimerPlugin plugin = (CreativeTimerPlugin) Bukkit.getPluginManager().getPlugin("CreativeTimerPlugin");
            if (plugin != null) {
                // 调用主类的清空背包方法，传入玩家对象
                plugin.清空背包但保留白名单物品(player);
            }
          //  cancel(); //以前这个直接调用cancel的丢了
        } else if (duration == 0&& CreativeTimerPlugin.允许保留物品和模式状态) {
            cancel2();
        }

    }
    public void start() {
        if (isTicking) {
            player.sendMessage("§l§6[创造模式体验系统]§r" + ChatColor.RED + "计时已经启用了");
            return;
        }


        if (duration == 0) {
            player.sendMessage("§l§6[创造模式体验系统]§r" + ChatColor.RED + "你没有创造模式体验时间，请先购买");
        } else {
            isTicking = true;
            def = player.getGameMode();
            player.sendMessage("§l§6[创造模式体验系统]§r" + ChatColor.YELLOW + "§l已开启创造计时");
        //    player.sendMessage("§l§6[创造模式体验系统]§r" + ChatColor.RED + "注意：为防止刷物品，计时结束将§l§n清空背包§r，这就是为什么要提前保存生存模式重要物品的原因");
        //    player.sendMessage("§l§6[创造模式体验系统]§r" + ChatColor.RED + "注意：禁止利用创造模式权限进行任何大规模破坏性活动！一经发现，永久封禁！！！");
            player.setGameMode(GameMode.CREATIVE);
        }
    }
    public void cancel() {
        //这个是给 允许保留背包和模式状态：false的时候用的
        removeBossBar(player);
        if (!isTicking) {   //在没有计时的时候输入指令会怎么样
            player.sendMessage("§l§6[创造模式体验系统]§r" + ChatColor.RED + "不在计时体验状态");
        } else  {
            isTicking = false;
            player.sendMessage("§l§6[创造模式体验系统]§r" + ChatColor.AQUA + "已关闭创造计时并清空背包");
            player.getInventory().clear();
            player.setGameMode(GameMode.SURVIVAL);
        }
    }
    public void cancel2(){
        //这个是给 允许保留背包和模式状态：true 的时候用的
        removeBossBar(player);
        if (!isTicking) {   //在没有计时的时候输入指令会怎么样
            player.sendMessage("§l§6[创造模式体验系统]§r" + ChatColor.RED + "不在计时体验状态");
        } else  {
            isTicking = false;
            player.sendMessage("§l§6[创造模式体验系统]§r" + ChatColor.AQUA + "已关闭创造计时");
            player.setGameMode(GameMode.SURVIVAL);
        }
    }
    public void removeBossBar(Player player) {
        if (bossBars.containsKey(player.getUniqueId())) {
            BossBar bossBar = bossBars.get(player.getUniqueId());
            bossBar.removeAll();
            bossBars.remove(player.getUniqueId());
        }
    }
}