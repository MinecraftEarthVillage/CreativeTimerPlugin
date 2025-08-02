package earthvillage.top.CreativeTimerPlugin;

import org.bukkit.scheduler.BukkitRunnable;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public class Timer extends BukkitRunnable {
    @Override
    public void run() {
        // 使用迭代器避免ConcurrentModificationException
        Iterator<Map.Entry<UUID, CTPlayer>> it = CTPlayer.playerMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, CTPlayer> entry = it.next();
            if (entry.getValue().isTicking) {
                entry.getValue().tick();
            }
        }
    }
}
