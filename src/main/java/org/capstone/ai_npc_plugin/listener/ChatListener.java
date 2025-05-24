package org.capstone.ai_npc_plugin.listener;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import java.util.UUID;
import org.capstone.ai_npc_plugin.network.PersistentModelClient;
import org.capstone.ai_npc_plugin.listener.NpcInteractListener;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Villager;

public class ChatListener implements Listener {

    private final PersistentModelClient modelClient = new PersistentModelClient();

    public ChatListener() {
        modelClient.connect();  // 서버 시작 시 1회 연결
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();

        UUID npcId = NpcInteractListener.getInteractingNPC(player);

        if (npcId == null) {
            Villager nearest = null;
            double minDist = Double.MAX_VALUE;

            for (Entity e : player.getNearbyEntities(10, 10, 10)) {
                if (e instanceof Villager v && "AI 주민".equals(v.getCustomName())) {
                    double dist = e.getLocation().distance(player.getLocation());
                    if (dist < minDist) {
                        nearest = v;
                        minDist = dist;
                    }
                }
            }
            if (nearest != null) {
                npcId = nearest.getUniqueId();
                // 선택된 NPC를 상호작용 상태로 등록
                NpcInteractListener.setInteraction(player, nearest);
                player.sendMessage("§7(가장 가까운 NPC '" + nearest.getCustomName() + "'와 대화 중)");
            } else {
                return; // ❌ 주변에 NPC 없음 → 무시
            }
        }


        Bukkit.getScheduler().runTaskAsynchronously(
                Bukkit.getPluginManager().getPlugin("AI_NPC_Plugin"),
                () -> {
                    String response = modelClient.sendMessage(player.getName(), message);
                    Bukkit.getScheduler().runTask(Bukkit.getPluginManager().getPlugin("AI_NPC_Plugin"),
                            () -> player.sendMessage("§e[NPC]§f " + response));
                }
        );
    }

    public void shutdown() {
        modelClient.disconnect();
    }
}
