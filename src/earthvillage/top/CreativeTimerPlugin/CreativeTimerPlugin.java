package earthvillage.top.CreativeTimerPlugin;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.*;
import org.bukkit.entity.minecart.HopperMinecart;
import org.bukkit.entity.minecart.StorageMinecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;


import java.io.File;
import java.io.IOException;
import java.util.*;

import static org.bukkit.Material.AIR;


public class CreativeTimerPlugin extends JavaPlugin implements Listener {
    private static CreativeTimerPlugin instance;
    private File playersFile;
    private FileConfiguration players;
    private BukkitTask timer;
    private Economy eco;
    private final List<String> tab = Arrays.asList("buy", "start", "cancel", "reload", "give", "set");
    //Config

    private double MONEY_PER_SEC;
    private boolean AUTO_CANCEL;
    private int MAX_TIME;


    //配置文件里的作弊开关
    private boolean 传递物品;
    private boolean 允许使用TNT;
    public static boolean 允许保留物品和模式状态;
    private boolean 允许打人;
    private boolean 要求移出生存模式物品;


    public void 读取安全配置项(){
        传递物品 = getConfig().getBoolean("传递物品",false);
        允许使用TNT = getConfig().getBoolean("允许使用TNT",false);
        允许保留物品和模式状态=getConfig().getBoolean("允许保留物品和模式状态",false);
        允许打人=getConfig().getBoolean("允许打人",false);
        要求移出生存模式物品=getConfig().getBoolean("要求移出生存模式物品",true);
    }
    @Override
    public void onEnable() {
        instance = this; // 将插件实例保存为单例
        if (!initEco()) {
            getServer().getConsoleSender().sendMessage(ChatColor.RED + "未检测到插件: EssentialsX和Vault, 插件卸载中（一定要三个一起用）");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        saveDefaultConfig();
        读取安全配置项();
        loadConfig();
        getServer().getPluginManager().registerEvents(this, this);
        playersFile = new File(getDataFolder(), "players.yml"); //玩家文件 用于持久化
        if (!playersFile.exists()) {
            saveResource("players.yml", false);
        }
        players = YamlConfiguration.loadConfiguration(playersFile);
        timer = new Timer().runTaskTimer(this, 0, 20);
        //用于支持热重载
        Bukkit.getOnlinePlayers().forEach(p -> loadPlayer(p.getPlayer()));
        getLogger().info("加载完成");
//播报配置文件
        System.out.println("======这是你的配置项======");
        getLogger().info("允许使用TNT: " + 允许使用TNT +"（推荐值：false）");
        getLogger().info("允许保留物品和模式状态: " + 允许保留物品和模式状态 +"（推荐值：false）");
        getLogger().info("允许打人: " + 允许打人 +"（推荐值：false）");
        getLogger().info("传递物品: " + 传递物品 +"（推荐值：false）");
        getLogger().info("要求移出生存模式物品: " + 要求移出生存模式物品 +"（推荐值：true）");

//当config.yml里有下面这样的默认推荐配置时
        //允许使用TNT: false
        //允许保留物品和模式状态: false
        //允许打人: false
        //传递物品: false
        //要求移出生存模式物品: true
        //
        //      if(){}里面的“[创造模式体验系统]保护程序已启用，创造模式玩家现在禁止作弊”应该会播报
        if ( !允许使用TNT && !允许保留物品和模式状态&& !允许打人 && !传递物品 && 要求移出生存模式物品) {
            this.getLogger().info("[创造模式体验系统]保护程序已启用，创造模式玩家现在禁止作弊");
        }else {
            //如果有一条不满足就播下面这条
            this.getLogger().info(ChatColor.RED+"[创造模式体验系统]保护程序没有彻底启用，创造模式玩家可能会出现作弊现象");
        }


    }
    public static CreativeTimerPlugin getInstance() {
        return instance;
    }
    @Override
    public void onDisable() {
        timer.cancel();
        //保存玩家信息 用于持久化
        保存玩家数据();
        读取安全配置项();
        loadConfig();
        // 获取服务器中的所有玩家，首先检查是否有在线玩家
        if (Bukkit.getServer().getOnlinePlayers().isEmpty()) {
            System.out.println("[创造模式体验系统]没有在线玩家");
        } else {
            for (Player player : Bukkit.getServer().getOnlinePlayers()) {
                System.out.println("[创造模式体验系统]正在获取是否有人保留了模式···");
                //   允许保留物品和模式状态是不是开着的
                if ( !允许保留物品和模式状态) {
                    //检查玩家是否处于创造模式且不是管理员
                    if (player.getGameMode() == GameMode.CREATIVE && !player.isOp() ){
                        // 将玩家设为生存模式
                        player.setGameMode(GameMode.SURVIVAL);
                        System.out.println("[创造模式体验系统]已恢复玩家为生存");
                    }

                } else {
                    this.getLogger().info(ChatColor.RED + "检测到你没有禁用“允许保留物品和模式状态”，他们将一直保留创造模式的状态，不建议");
                    //播报配置文件
                    System.out.println("======这是你的配置项======");
                    getLogger().info("允许使用TNT: " + 允许使用TNT +"（推荐值：false）");
                    getLogger().info("允许保留物品和模式状态: " + 允许保留物品和模式状态 +"（推荐值：false）");
                    getLogger().info("允许打人: " + 允许打人 +"（推荐值：false）");
                    getLogger().info("传递物品: " + 传递物品 +"（推荐值：false）");
                    getLogger().info("要求移出生存模式物品: " + 要求移出生存模式物品 +"（推荐值：true）");
                }
            }
        }//这段代码同样是防止有人逃脱监管卡进永久创造模式的


    }
    //保存玩家数据的代码包装成方法了
    public void 保存玩家数据() {
        CTPlayer.playerMap.forEach((k, v) -> {
            System.out.println("[创造模式体验系统]正在保存玩家信息···");
            int dur = v.duration;
            if (dur > 0) {//只有非0时间才会被记录，防止yml文件里一堆0秒数据……
                // 保存为嵌套结构
                players.set(k.toString() + ".time", dur);
                players.set(k.toString() + ".name", v.player.getName());
            } else {
                // 清除无效数据
                players.set(k.toString(), null);
            }
            System.out.println("[创造模式体验系统]保存玩家信息完毕···");
        });
        try {
            players.save(playersFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    //加载经济
    private boolean initEco() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        eco = rsp.getProvider();
        return eco != null;
    }

    //模块化 易于reload重载
    private void loadConfig() {
        reloadConfig();
        MONEY_PER_SEC = getConfig().getDouble("money-per-second", 10.0);
        MAX_TIME = getConfig().getInt("max-time", 300);
        AUTO_CANCEL = getConfig().getBoolean("auto-cancel", true);
        允许保留物品和模式状态 = getConfig().getBoolean("允许保留物品和模式状态", false);
    }
    private void loadPlayer(Player player) {
        String uuidStr = player.getUniqueId().toString();
        int time = 0;

        // 检查旧格式（UUID直接对应时间）
        if (players.isInt(uuidStr)) {
            // 读取旧数据并迁移到新格式
            time = players.getInt(uuidStr, 0);
            players.set(uuidStr + ".time", time);
            players.set(uuidStr + ".name", player.getName());
            players.set(uuidStr, null); // 删除旧条目
            try {
                players.save(playersFile); // 保存迁移后的数据
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            // 读取新格式的时间
            time = players.getInt(uuidStr + ".time", 0);
        }

        new CTPlayer(player, time);
    }

    //事件
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {//进服务器事件
        Player player = e.getPlayer();
        if (!CTPlayer.playerMap.containsKey(e.getPlayer().getUniqueId())) {
            loadPlayer(e.getPlayer());
        }
        //允许保留物品和模式状态: false时
        if (!允许保留物品和模式状态&&player.getGameMode()==GameMode.CREATIVE && !player.isOp()) {
            //无法调用隔壁cancel()方法(?)，包括切生存与清空物品栏双操作，这里重复写了代码
            player.getInventory().clear();
            player.setGameMode(GameMode.SURVIVAL);
            player.sendMessage("§4检测到你上次退出游戏时不是OP但是处于创造模式，已恢复生存模式");
        }
        else
            // 如果允许保留物品和模式状态为 true，并且玩家上次退出游戏时是创造模式体验计时状态
            if (允许保留物品和模式状态 && CTPlayer.playerMap.containsKey(player.getUniqueId())) {
                if (!player.isOp()&&player.getGameMode() == GameMode.CREATIVE) {
                    // 继续上次的创造模式体验计时并离开载具
                    CTPlayer.playerMap.get(player.getUniqueId()).start();
                    player.leaveVehicle();
                    player.sendMessage("§b检测到你上一次退出游戏时处于创造模式，同时管理员允许保留物品与计时状态，已恢复计时");
                }
            }
    }
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {//退出服务器事件
        // 当玩家退出服务器时触发此事件
        保存玩家数据();
        // 从CTPlayer的静态映射中获取当前退出玩家的CTPlayer实例
        CTPlayer gPlayer = CTPlayer.playerMap.get(e.getPlayer().getUniqueId());
        // 将玩家的游戏时长保存到players映射中，键为玩家UUID的字符串形式，值为游戏时长
        //players.set(e.getPlayer().getUniqueId().toString(), gPlayer.duration);
        // 获取退出事件的玩家对象
        Player player = e.getPlayer();
        // 检查是否不允许保留物品和模式状态，并且玩家当前处于计时状态且不是服务器管理员
        if (!允许保留物品和模式状态 && gPlayer.isTicking && !player.isOp()) {
            // 如果条件满足，调用gPlayer的cancel方法，该方法会取消计时并执行以下操作：
            // 1. 将玩家模式切换回生存模式
            // 2. 清空玩家物品栏
            //这里调用方法时将 player 强制转换为 CommandSender 类型
            清空背包但保留白名单物品(player);
            //gPlayer.cancel();
            // 向全服玩家广播消息，通知玩家退出并清空背包
            String playerName = player.getName();
            String message = playerName + "在创造模式中退出游戏，已变回生存模式并且清空了背包";
            Bukkit.broadcastMessage(ChatColor.YELLOW + message);
        } else if (允许保留物品和模式状态 && gPlayer.isTicking && !player.isOp()) {
            // 向全服玩家广播消息，通知玩家退出但没有清空背包
            String playerName = player.getName();
            String message = playerName + "在创造模式中退出游戏，但腐竹设置了“允许保留模式状态”，所以并没有恢复生存清空背包";
            Bukkit.broadcastMessage(ChatColor.RED + message);
        }
        // 从CTPlayer的静态映射中移除当前退出玩家的记录
        CTPlayer.playerMap.remove(e.getPlayer().getUniqueId());
    }




    // 注解EventHandler表示该方法是一个事件处理器，用于处理特定类型的事件
    @EventHandler
    public void onGameModeChange(PlayerGameModeChangeEvent 模式改变事件) {
        // 检查新游戏模式是否不是创造模式，并且AUTO_CANCEL变量为true
        if (!模式改变事件.getNewGameMode().equals(GameMode.CREATIVE) && AUTO_CANCEL) {
            // 从CTPlayer的静态映射中获取当前玩家的CTPlayer实例
            CTPlayer player = CTPlayer.playerMap.get(模式改变事件.getPlayer().getUniqueId());
            // 检查玩家是否正在计时，并且不允许保留物品和模式状态
            if (player.isTicking&&!允许保留物品和模式状态) {
                // 取消玩家的计时
                清空背包但保留白名单物品(模式改变事件.getPlayer());
                // 向玩家发送消息，通知其计时体验已取消
                模式改变事件.getPlayer().sendMessage("§6哎呀！你中途切换了游戏模式，计时体验取消了");
                // 调用保存玩家数据的方法
                保存玩家数据();
            }
        }
    }

    public void 清空背包但保留白名单物品(CommandSender sender){
        Player player = (Player) sender;
        // 加载插件配置文件
        FileConfiguration config = this.getConfig();

        // 创建一个Map来存储符合白名单的物品及其位置
        Map<Integer, ItemStack> whiteListItemsWithSlots = new HashMap<>();

        // 获取玩家背包中的所有物品
        ItemStack[] contents = player.getInventory().getContents();

        // 遍历背包中的物品
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item != null && isItemInWhiteList(item, config)) {
                // 如果物品符合白名单条件，保存到Map中，记录物品及其位置
                whiteListItemsWithSlots.put(i, item);
            }
        }

        // 清空玩家的物品
        CTPlayer.playerMap.get(player.getUniqueId()).cancel();

        // 将符合白名单的物品放回原来的位置
        for (Map.Entry<Integer, ItemStack> entry : whiteListItemsWithSlots.entrySet()) {
            int slot = entry.getKey();
            ItemStack whiteListItem = entry.getValue();

            // 将物品放回原来的位置
            player.getInventory().setItem(slot, whiteListItem);
        }
    }
    //命令
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        //用法不对 不工作
        if (args.length < 1 || args.length > 4) {
            sender.sendMessage("语法有误");
            sender.sendMessage(ChatColor.AQUA + "§l====用法====");
            sender.sendMessage("reload：重载插件配置文件");
            sender.sendMessage("start：开始体验创造模式并开始计时");
            sender.sendMessage("cancel：暂停体验，时间将储存");
            sender.sendMessage("buy：购买时间，后面接数字，单位:秒");
            return true;
        }
        if (args[0].equals("reload") && sender.hasPermission("ct.reload")) {
            //loadConfig();     //旧的重载方式
            onDisable();

            onEnable();

            sender.sendMessage(ChatColor.AQUA + "配置重载完成");
            Bukkit.broadcastMessage(ChatColor.YELLOW + "[创造模式体验系统]刚刚插件重载了一下");
            return true;
        }

//直接给时间或直接设置时长
        if (args[0].equals("set") || args[0].equals("give")) {
            if (!sender.isOp()) {
                sender.sendMessage(ChatColor.RED + "只有玩家或管理员才能使用这个命令");
                return true;
            }
            if (args.length != 3) {
                sender.sendMessage(ChatColor.RED + "用法: /ct " + args[0] + " <时长，单位:秒> <玩家名>");
                return true;
            }
            int time;
            try {
                time = Integer.parseInt(args[1]);
                if (time < 0) {
                    sender.sendMessage(ChatColor.RED + "时间不能是负的啊  (PД`q。)·。'゜");
                    return true;
                }
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "请输入有效的数字时间");
                return true;
            }

            Player targetPlayer = Bukkit.getPlayer(args[2]);
            if (targetPlayer == null || !targetPlayer.isOnline()) {
                sender.sendMessage(ChatColor.RED + "玩家 " + args[2] + " 不在线或不存在！");
                return true;
            }

            CTPlayer gPlayer = CTPlayer.playerMap.get(targetPlayer.getUniqueId());
// 判断是否超过最大持有时间
            if (gPlayer.duration + time > MAX_TIME) {
                sender.sendMessage(ChatColor.RED  + args[2] + " 不能持有这么多时间, 最大可持有时间: " + MAX_TIME);
                return true;
            }
            if (args[0].equals("set")) {
                gPlayer.setDuration(time);
                targetPlayer.sendMessage(ChatColor.AQUA + "系统设置了你的体验时间为 " + time + " 秒");
                sender.sendMessage(ChatColor.AQUA + "已为玩家 " + args[2] + " 设置了体验时间为 " + time + " 秒");
            } else if (args[0].equals("give")) {
                gPlayer.add(time);
                targetPlayer.sendMessage(ChatColor.AQUA + "系统为你增加了 " + time + " 秒体验时间，当前剩余时间: " + gPlayer.duration);
                sender.sendMessage(ChatColor.AQUA + "已为玩家 " + args[2] + " 增加了 " + time + " 秒体验时间，当前剩余时间："+ gPlayer.duration);
            }

            return true;
        }



        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "只有玩家才能使用这个命令");
            return true;
        }
        //上面这个判断指令执行者是玩家还是控制台
        Player player = (Player) sender;

        switch (args[0]) {
            case "start": {
                if (!player.isOp()) {  //先判断是不是OP，因为OP不需要这个指令
                    if (!要求移出生存模式物品 || player.getInventory().isEmpty() || isOnlyWhiteListItems(player)) {
                        // 如果玩家物品栏为空，或者不要求移出物品，或者玩家物品栏只包含白名单物品
                        CTPlayer.playerMap.get(player.getUniqueId()).start();  // 执行start方法
                        player.leaveVehicle();  // 让玩家离开车辆
                    } else {
                        // 玩家物品栏包含不在白名单内的东西，或者要求移出生存模式物品: true
                        player.sendMessage("§6请先将物品栏的个人物品移出后再执行该指令！（打开菜单等专用道具不会清除，可以保留）");
                        // 你也可以根据需要显示注释行
                        // player.sendMessage("§4但是在计时取消后会连同专用道具一同清空，所以尽量不要用纸菜单而是更方便的快捷键或指令");
                    }
                }else{
                    player.sendMessage("§a§l你不是管理员吗？你还需要这个吗？");
                }
                break;
            }

            case "cancel": {
                // 判断是否允许保留物品和模式状态
                // 如果不允许，则调用CTPlayer对象的cancel方法（并且还要忽略白名单物品）
                if (!允许保留物品和模式状态) {
                    //  调用方法
                    清空背包但保留白名单物品(sender);
                }
                else {
                    // 如果允许保留物品和模式状态，则调用CTPlayer对象的cancel2方法
                    CTPlayer.playerMap.get(player.getUniqueId()).cancel2();
                    // 向发送者发送消息，告知服主开启了允许保留物品栏和模式状态，背包没有清空
                    sender.sendMessage(ChatColor.AQUA + "§b服主开启了允许保留物品栏和模式状态，你的背包没有清空");
                }
                break;
            }
            case "buy": {
                if (args.length != 2) {
                    player.sendMessage(ChatColor.RED + "用法: /ct buy <时长，单位:秒>");
                    break;
                }
                int time;
                try {
                    time = Integer.parseInt(args[1]);
                    if (time < 0) {
                        player.sendMessage(ChatColor.RED + "时间不能是负的啊  (PД`q。)·。'゜");
                        break;
                    }
                } catch (NumberFormatException e) {
                    player.sendMessage(ChatColor.RED + "请输入有效的数字时间");
                    break;
                }
                CTPlayer gPlayer = CTPlayer.playerMap.get(player.getUniqueId());
                if (gPlayer.duration + time > MAX_TIME) {
                    player.sendMessage(ChatColor.RED + "你不能购买这么多的时间, 最大可持有时间: " + MAX_TIME);
                    break;
                }
                double cost = time * MONEY_PER_SEC;
                if (eco.getBalance(player) < cost) {
                    player.sendMessage(ChatColor.RED + "你没有足够的钱, 需要: " + cost + ", 当前: " + eco.getBalance(player));
                    break;
                }
                eco.withdrawPlayer(player, cost);
                gPlayer.add(time);
                player.sendMessage(ChatColor.AQUA + "购买成功, 当前剩余时间: " + gPlayer.duration);
                break;
            }
            case "give": {
                if (args.length != 3) {
                    sender.sendMessage(ChatColor.RED + "用法: /ct give <时长，单位:秒> <玩家名>");
                    break;
                }
                int time;
                try {
                    time = Integer.parseInt(args[1]);
                    if (time < 0) {
                        sender.sendMessage(ChatColor.RED + "时间不能是负的啊  (PД`q。)·。'゜");
                        break;
                    }
                } catch (NumberFormatException e) {
                    sender.sendMessage(ChatColor.RED + "请输入有效的数字时间");
                    break;
                }
                Player targetPlayer = Bukkit.getPlayer(args[2]);
                if (targetPlayer == null || !targetPlayer.isOnline()) {
                    sender.sendMessage(ChatColor.RED + "玩家 " + args[2] + " 不在线或不存在！");
                    break;
                }
                CTPlayer gPlayer = CTPlayer.playerMap.get(targetPlayer.getUniqueId());
                if (gPlayer.duration + time > MAX_TIME) {
                    sender.sendMessage(ChatColor.RED + "玩家 " + args[2] + " 不能增加这么多时间, 最大可持有时间: " + MAX_TIME);
                    break;
                }
                gPlayer.add(time);
                targetPlayer.sendMessage(ChatColor.AQUA + "系统为你增加了 " + time + " 秒体验时间，当前剩余时间: " + gPlayer.duration);
                sender.sendMessage(ChatColor.AQUA + "已为玩家 " + args[2] + " 增加了 " + time + " 秒体验时间");
                break;
            }
            case "set": {
                if (args.length != 3) {
                    sender.sendMessage(ChatColor.RED + "用法: /ct set <时长，单位:秒> <玩家名>");
                    break;
                }
                int time;
                try {
                    time = Integer.parseInt(args[1]);
                    if (time < 0) {
                        sender.sendMessage(ChatColor.RED + "时间不能是负的啊  (PД`q。)·。'゜");
                        break;
                    }
                } catch (NumberFormatException e) {
                    sender.sendMessage(ChatColor.RED + "请输入有效的数字时间");
                    break;
                }
                Player targetPlayer = Bukkit.getPlayer(args[2]);
                if (targetPlayer == null || !targetPlayer.isOnline()) {
                    sender.sendMessage(ChatColor.RED + "玩家 " + args[2] + " 不在线或不存在！");
                    break;
                }
                CTPlayer gPlayer = CTPlayer.playerMap.get(targetPlayer.getUniqueId());
                gPlayer.setDuration(time); // 设置玩家持有的时长
                targetPlayer.sendMessage(ChatColor.AQUA + "设置了你的体验时间为 " + time + " 秒");
                sender.sendMessage(ChatColor.AQUA + "已为玩家 " + args[2] + " 设置了体验时间为 " + time + " 秒");
                break;
            }
        }
        return true;
    }


    //物品栏白名单
    public boolean isOnlyWhiteListItems(Player player) {
        // 加载插件配置文件
        FileConfiguration config = this.getConfig();
        // 从配置文件中获取白名单的lore列表
        List<String> whitelist = config.getStringList("Lore白名单");

        // 遍历玩家背包中的所有物品
        for (ItemStack itemStack : player.getInventory().getContents()) {
            // 如果物品不为空
            if (itemStack != null) {
                // 获取物品的物品Meta
                ItemMeta itemMeta = itemStack.getItemMeta();
                if (itemMeta != null && itemMeta.hasLore()) {
                    // 获取物品的lore
                    List<String> itemLore = itemMeta.getLore();

                    // 如果lore不在白名单中，返回false
                    if (!whitelist.containsAll(itemLore)) {
                        return false;
                    }
                }
            }
        }
        // 只包含lore在白名单内的物品，返回 true
        return true;
    }
    //辅助方法，判断单个物品是否符合白名单
    public boolean isItemInWhiteList(ItemStack itemStack,FileConfiguration config) {
        // 获取物品的物品Meta
        if (itemStack != null && itemStack.hasItemMeta()) {
            ItemMeta itemMeta = itemStack.getItemMeta();

            // 如果物品Meta存在且有lore
            if (itemMeta != null && itemMeta.hasLore()) {
                List<String> itemLore = itemMeta.getLore();
                // 这里判断该物品的lore是否在白名单中
                return config.getStringList("Lore白名单").containsAll(itemLore);
            }
        }
        return false;
    }




    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length < 2) {
            return tab;
        }
        return Collections.emptyList();
    }



    //下面都是防作弊系统
    //保护程序，防止创造模式玩家乱打人(除了管理员 因为管理员永远是对的)
    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {//防打非创造玩家（实体伤害事件）
        if (event.getDamager() instanceof Player && event.getEntity() instanceof Player) {
            Player attacker = (Player)event.getDamager();
            Player victim = (Player)event.getEntity();

            if (attacker.getGameMode() == GameMode.CREATIVE && victim.getGameMode() != GameMode.CREATIVE  &&   !attacker.isOp()&&允许打人==false) {
                attacker.sendMessage("你不能攻击生存/冒险模式玩家");
                event.setCancelled(true);
            }
        }

    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {//防丢物品（玩家丢物品事件）
        Player player = event.getPlayer();
        ItemStack droppedItem = event.getItemDrop().getItemStack();
        //如果传递物品: false
        if (!传递物品) {
            if (!player.isOp() && player.getGameMode() == GameMode.CREATIVE && (droppedItem.getType() != AIR)) {
                // 阻止玩家丢出任何物品
                event.setCancelled(true);
                player.sendMessage("§c不能给别人刷物品哦");
            }
        }
    }
//互动事件

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        /* 阻止放置TNT，实际上可以搭配Banitem插件实现相同效果，未来这段代码会移除吧
         */
        Player player = event.getPlayer(); // 获取触发事件的玩家
        // 检查玩家是否处于创造模式且不是管理员
        if (!允许使用TNT && player.getGameMode() == GameMode.CREATIVE && !player.isOp()) {
            // 检查玩家手中是否拿着TNT或TNT矿车
            ItemStack item = player.getItemInHand();
            if (item != null && (item.getType() == Material.TNT || item.getType() == Material.TNT_MINECART)) {
                // 检查玩家是否尝试右键点击方块或空气
                Action action = event.getAction();
                if (action == Action.RIGHT_CLICK_BLOCK || action == Action.RIGHT_CLICK_AIR) {
                    event.setCancelled(true); // 取消事件，阻止放置TNT
                    player.sendMessage("§c干嘛？想炸图吗？我告诉你：没门（嘿嘿嘿）"); // 警告玩家
                }
            }
        }
    }

    // 监听容器打开事件
    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {

        /*打开容器事件，防止创造玩家偷藏物品*/

        // 获取触发事件的玩家
        Player player = (Player) event.getPlayer();
        // 获取玩家打开的物品栏所在位置的方块
        Block block = event.getView().getTopInventory().getLocation().getBlock();
        // 获取物品栏的持有者
        InventoryHolder holder = event.getInventory().getHolder();
        // 获取方块的材料类型
        Material blockType = block.getType();
        // 检查玩家是否处于创造模式且不是管理员并且传递物品关掉了
        if (!传递物品&&player.getGameMode() == GameMode.CREATIVE && !player.isOp()) {
            // 阻止骑乘驴或骡时打开物品栏
            if (player.getVehicle() != null && (player.getVehicle().getType() == EntityType.DONKEY || player.getVehicle().getType() == EntityType.MULE)) {
                // 取消事件，阻止玩家打开物品栏
                event.setCancelled(true);
                // 发送提示消息给玩家
                player.sendMessage("§6我知道你想干什么，不能在骑乘驴或骡时打开物品栏偷放东西！");
            }
            // 检查打开的是容器类方块
            if (block.getState() instanceof InventoryHolder ||holder instanceof ChestBoat|| holder instanceof StorageMinecart || holder instanceof HopperMinecart || blockType.toString().endsWith("_CHEST")) {
                // 取消事件，阻止物品被打开
                event.setCancelled(true  /*有条件成立时就取消事件，阻止物品被打开*/);
                // 发送提示消息给玩家
                player.sendMessage("§6我知道你想干什么，不许利用创造模式大量刷取物资"); // 发送提示消息给玩家
            }
        }
    }
    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {

        /**
         * 与实体互动事件
         * 一开始是阻止玩家在创造模式下使用物品展示框传递物品
         **/
        // 获取触发事件的玩家
        Player player = event.getPlayer();

        // 检查是否允许传递物品，玩家是否为管理员，以及玩家是否在创造模式，并且点击的实体是否为InventoryHolder（如箱子）
        if (!传递物品 && !player.isOp() && player.getGameMode() == org.bukkit.GameMode.CREATIVE && event.getRightClicked() instanceof InventoryHolder) {
            // 取消事件，阻止玩家与该实体交互
            event.setCancelled(true);
            // 向玩家发送消息，提示不允许在创造模式下大量刷取物资
            player.sendMessage("§6我知道你想干什么，不许利用创造模式大量刷取物资");
        }

        // 锁物品展示框
        // 检查玩家是否为管理员，玩家是否在创造模式，并且点击的实体是否为ItemFrame（物品展示框）
        // 也许可以尝试“实体黑名单”，这样还能阻止点击坐骑开箱？但是好像只有展示框有这样的特性

        if (!player.isOp() && player.getGameMode() == org.bukkit.GameMode.CREATIVE && event.getRightClicked() instanceof ItemFrame) {
            // 获取点击的物品展示框实体（注释掉的代码）
            // ItemFrame itemFrame = (ItemFrame) event.getRightClicked();
            // 检查玩家副手和主手是否持有物品，并且是否允许传递物品
            if (player.getInventory().getItemInOffHand().getType() != AIR ||
                    player.getInventory().getItemInMainHand().getType() != AIR && !传递物品) {
                // 取消事件，阻止玩家与该实体交互
                event.setCancelled(true);
                // 向玩家发送消息，提示不允许使用物品展示框传递物品
                player.sendMessage("§4不要用展示框给别人传递物品！不要给新人装备");
            }
        }

    }

/*
    @EventHandler

    */

    /*预留空位？
            广告位招租
                广告位招租
                    广告位招租
     */

}