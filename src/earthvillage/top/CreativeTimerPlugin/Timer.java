package earthvillage.top.CreativeTimerPlugin;

import org.bukkit.scheduler.BukkitRunnable;

public class Timer extends BukkitRunnable {
    @Override
    public void run() {
        /*
        lambda语法 java8新功能 逻辑上等价于:
        for (Map.Entry<UUID, GMTPlayer> entry : GMTPlayer.playerMap.entrySet()) {
            GMTPlayer player = entry.getValue();
            if (player.isTicking) {
                player.tick();
            }
        }
         */
        CTPlayer.playerMap.forEach((k, v) -> {
            if (v.isTicking) {
                v.tick();
            }
        });
    }
}
