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

import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;


import java.io.File;
import java.io.IOException;
import java.util.*;

import static org.bukkit.Material.AIR;


public class CreativeTimerPlugin extends JavaPlugin implements Listener {
    private File playersFile;
    private FileConfiguration players;
    private BukkitTask timer;
    private Economy eco;
    private final List<String> tab = Arrays.asList("buy", "start", "cancel", "reload", "give", "set");
    //Config

    private double MONEY_PER_SEC;
    private boolean AUTO_CANCEL;
    private int MAX_TIME;


    //作弊开关
    private boolean 传递物品;
    private boolean 允许使用TNT;
    public static boolean 允许保留物品和模式状态;
    private boolean 允许打人;
    private boolean 要求移出生存模式物品;
    @Override
    public void onEnable() {
        if (!initEco()) {
            getServer().getConsoleSender().sendMessage(ChatColor.RED + "未检测到插件: EssentialsX和Vault, 插件卸载中（一定要三个一起用）");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        saveDefaultConfig();
        传递物品 = getConfig().getBoolean("允许使用展示框",false);
        允许使用TNT = getConfig().getBoolean("允许使用TNT",false);
        允许保留物品和模式状态=getConfig().getBoolean("允许保留物品和模式状态",false);
        允许打人=getConfig().getBoolean("允许打人",false);
        要求移出生存模式物品=getConfig().getBoolean("要求移出生存模式物品",true);
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

    @Override
    public void onDisable() {
        timer.cancel();
        //保存玩家信息 用于持久化（还是先检查一下playMap是不是空的）
        if (CTPlayer.playerMap.isEmpty()) {
            System.out.println("[创造模式体验系统]playerMap 为空，无法保存玩家信息（？这一段是AI写的，我也不知道用来干嘛？）");
        } else {
            CTPlayer.playerMap.forEach((k, v) -> {
                System.out.println("[创造模式体验系统]正在保存玩家信息···");
                int dur = v.duration;
                if (dur > 0) {
                    players.set(k.toString(), dur);
                } else {
                    //没有持续时间或者玩家名非法不记录 减少硬盘消耗
                    players.set(k.toString(), null);
                }
                System.out.println("[创造模式体验系统]保存玩家信息完毕···");
            });
        }
        try {
            players.save(playersFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
        传递物品 = getConfig().getBoolean("传递物品");
        允许使用TNT = getConfig().getBoolean("允许使用TNT");
        允许保留物品和模式状态=getConfig().getBoolean("允许保留物品和模式状态");
        允许打人=getConfig().getBoolean("允许打人");
        要求移出生存模式物品=getConfig().getBoolean("要求移出生存模式物品");
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
    //BOSS栏相关

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
        int time = players.getInt(player.getUniqueId().toString(), 0);
        new CTPlayer(player, time);
    }

    //事件
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        Player player = e.getPlayer();
        if (!CTPlayer.playerMap.containsKey(e.getPlayer().getUniqueId())) {
            loadPlayer(e.getPlayer());
        }
                //允许保留物品和模式状态: false时
        if (!允许保留物品和模式状态&&player.getGameMode()==GameMode.CREATIVE && !player.isOp()) {
            //调用隔壁cancel()方法(?)，包括切生存与清空物品栏双操作
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
    public void onPlayerQuit(PlayerQuitEvent e) {

        CTPlayer gPlayer = CTPlayer.playerMap.get(e.getPlayer().getUniqueId());
        players.set(e.getPlayer().getUniqueId().toString(), gPlayer.duration);

        Player player = e.getPlayer();
        //允许保留物品和模式状态: false时执行
        if (!允许保留物品和模式状态 && gPlayer.isTicking && !player.isOp()) {
            //调用隔壁cancel()方法，包括切生存与清空物品栏双操作
            gPlayer.cancel();
            // 向全服玩家发送玩家退出的消息
            String playerName = player.getName();
            String message = playerName + "在创造模式中退出游戏，已变回生存模式并且清空了背包";
            Bukkit.broadcastMessage(ChatColor.YELLOW + message);
        }
        CTPlayer.playerMap.remove(e.getPlayer().getUniqueId());
    }




    @EventHandler
    public void onGameModeChange(PlayerGameModeChangeEvent 模式改变事件) {
        if (!模式改变事件.getNewGameMode().equals(GameMode.CREATIVE) && AUTO_CANCEL) {
            CTPlayer player = CTPlayer.playerMap.get(模式改变事件.getPlayer().getUniqueId());
            if (player.isTicking&&!允许保留物品和模式状态) {
                player.cancel();
模式改变事件.getPlayer().sendMessage("§6哎呀！你中途切换了游戏模式，计时体验取消了");
            }
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
        if (args[0].equals("reload") && sender.hasPermission("gmt.reload")) {
            //loadConfig();     //旧的重载方式
            onDisable();
            sender.sendMessage(ChatColor.AQUA + "关闭插件···");
            sender.sendMessage(ChatColor.AQUA + "已关闭");
            onEnable();
            sender.sendMessage(ChatColor.AQUA + "启动插件···");
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
                sender.sendMessage(ChatColor.RED + "用法: /gmt " + args[0] + " <时长，单位:秒> <玩家名>");
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
                        //如果不要求事先清空物品栏或者玩家背包是空的或者只有白名单物品
                        // 玩家物品栏为空或者只含有白名单物品
                        CTPlayer.playerMap.get(player.getUniqueId()).start();
                        player.leaveVehicle(); // 让玩家离开车辆
                    } else {
                        // 玩家物品栏包含不在白名单内的东西或 要求移出生存模式物品: true
                        player.sendMessage("§6请先将物品栏的个人物品移出后再执行该指令！（你可以留下用来打开菜单的专用道具）");
                        player.sendMessage("§4但是在计时取消后会连同专用道具一同清空，所以尽量不要用纸菜单而是更方便的快捷键或指令");
                    }
                }else{
                    player.sendMessage("§a§l你不是管理员吗？你还需要这个吗？");
                }
                break;
            }

            case "cancel": {
                //在配置文件里不同的开关调用不同的处理方式
                if (!允许保留物品和模式状态) {
                    CTPlayer.playerMap.get(player.getUniqueId()).cancel();
                }  else {
                    CTPlayer.playerMap.get(player.getUniqueId()).cancel2();
                    sender.sendMessage(ChatColor.AQUA + "§b服主开启了允许保留物品栏和模式状态，你的背包没有清空");
                }
                break;
            }
            case "buy": {
                if (args.length != 2) {
                    player.sendMessage(ChatColor.RED + "用法: /gmt buy <时长，单位:秒>");
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
                    sender.sendMessage(ChatColor.RED + "用法: /gmt give <时长，单位:秒> <玩家名>");
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
                    sender.sendMessage(ChatColor.RED + "用法: /gmt set <时长，单位:秒> <玩家名>");
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
private boolean isOnlyWhiteListItems(Player player) {
    FileConfiguration config = this.getConfig(); // 加载插件配置文件
    List<String> whitelist = config.getStringList("whitelist");

    for (ItemStack itemStack : player.getInventory().getContents()) {
        if (itemStack != null) {
            Material material = itemStack.getType();
            if (!whitelist.contains(material.name())) {
                return false; // 存在不在白名单内的物品，返回 false
            }
        }
    }
    return true; // 只包含白名单内的物品，返回 true
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
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
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
    public void onPlayerDropItem(PlayerDropItemEvent event) {
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
    //阻止放置TNT
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        // 检查玩家是否处于创造模式
        if (!允许使用TNT &&player.getGameMode() == GameMode.CREATIVE && !player.isOp()) {

            // 检查玩家手中是否拿着TNT
            ItemStack item = player.getItemInHand();
            if (item != null && item.getType() == Material.TNT || item.getType() == Material.TNT_MINECART) {

                // 检查玩家是否尝试放出它们
                Action action = event.getAction();
                if (action == Action.RIGHT_CLICK_BLOCK || action == Action.RIGHT_CLICK_AIR) {
                    event.setCancelled(true); // 取消事件，阻止放置
                    player.sendMessage("§c干嘛？想炸图吗？我告诉你：没门（嘿嘿嘿）");
                }
            }
        }
    }

    // 监听容器打开事件
    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        Player player = (Player) event.getPlayer();
        Block block = event.getView().getTopInventory().getLocation().getBlock();
        InventoryHolder holder = event.getInventory().getHolder();
        Material blockType = block.getType();
        // 检查玩家是否处于创造模式且不是管理员并且传递物品关掉了
        if (!传递物品&&player.getGameMode() == GameMode.CREATIVE && !player.isOp()) {
            // 阻止骑乘驴或骡时打开物品栏
            if (player.getVehicle() != null && (player.getVehicle().getType() == EntityType.DONKEY || player.getVehicle().getType() == EntityType.MULE)) {
                event.setCancelled(true);
                player.sendMessage("§6我知道你想干什么，不能在骑乘驴或骡时打开物品栏偷放东西！");
            }
            // 检查打开的是容器类方块
            if (block.getState() instanceof InventoryHolder ||holder instanceof ChestBoat|| holder instanceof StorageMinecart || holder instanceof HopperMinecart || blockType.toString().endsWith("_CHEST")) {
                event.setCancelled(true  /*有条件成立时就取消事件，阻止物品被打开*/);
                player.sendMessage("§6我知道你想干什么，不许利用创造模式大量刷取物资"); // 发送提示消息给玩家
            }
        }
    }
    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();

        if (!传递物品 &&!player.isOp() && player.getGameMode() == org.bukkit.GameMode.CREATIVE && event.getRightClicked() instanceof InventoryHolder) {
            event.setCancelled(true);
            player.sendMessage("§6我知道你想干什么，不许利用创造模式大量刷取物资");
        }
        //锁物品展示框
        if (!player.isOp()&& player.getGameMode() == org.bukkit.GameMode.CREATIVE&&event.getRightClicked() instanceof ItemFrame) {
           // ItemFrame itemFrame = (ItemFrame) event.getRightClicked();
            if (player.getInventory().getItemInOffHand().getType() !=AIR ||
                    player.getInventory().getItemInMainHand().getType() !=AIR
                            &&!传递物品) {
                event.setCancelled(true);
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