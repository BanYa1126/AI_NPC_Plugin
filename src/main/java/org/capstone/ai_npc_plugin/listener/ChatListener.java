package org.capstone.ai_npc_plugin.listener;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.persistence.PersistentDataType;
import org.capstone.ai_npc_plugin.network.PersistentModelClient;

import java.util.UUID;

public class ChatListener implements Listener {

    private final PersistentModelClient modelClient = new PersistentModelClient();

    public ChatListener() {
        modelClient.connect(); // 서버 시작 시 1회 연결
    }

    private boolean isAINPC(Villager v) {
        return v.getPersistentDataContainer().has(
                new NamespacedKey(Bukkit.getPluginManager().getPlugin("AI_NPC_Plugin"), "ainpc"),
                PersistentDataType.STRING
        );
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();

        if (!NpcInteractListener.isChatMode(player)) {
            return; // 일반 모드에서는 무시
        }

        // ✅ 모든 로직을 메인 스레드에서 실행
        Bukkit.getScheduler().runTask(
                Bukkit.getPluginManager().getPlugin("AI_NPC_Plugin"),
                () -> {
                    UUID npcId = NpcInteractListener.getInteractingNPC(player);

                    if (npcId == null) {
                        Villager nearest = null;
                        double minDist = Double.MAX_VALUE;

                        for (Entity e : player.getNearbyEntities(10, 10, 10)) {
                            if (e instanceof Villager v && isAINPC(v)) {
                                double dist = e.getLocation().distance(player.getLocation());
                                if (dist < minDist) {
                                    nearest = v;
                                    minDist = dist;
                                }
                            }
                        }

                        if (nearest != null) {
                            npcId = nearest.getUniqueId();
                            NpcInteractListener.setInteraction(player, nearest);
                            player.sendMessage("§7(가장 가까운 NPC '" + nearest.getCustomName() + "'와 대화 중)");
                        } else {
                            return; // NPC가 없음 → 메시지 무시
                        }
                    }

                    // ✅ 모델 서버 호출은 다시 비동기로 수행
                    Bukkit.getScheduler().runTaskAsynchronously(
                            Bukkit.getPluginManager().getPlugin("AI_NPC_Plugin"),
                            () -> {
                                String response = modelClient.sendMessage(player.getName(), message);
                                Bukkit.getScheduler().runTask(
                                        Bukkit.getPluginManager().getPlugin("AI_NPC_Plugin"),
                                        () -> player.sendMessage("§e[NPC]§f " + response)
                                );
                            }
                    );
                }
        );
    }

    public void shutdown() {
        modelClient.disconnect();
    }
}
