package org.capstone.ai_npc_plugin.listener;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.persistence.PersistentDataType;
import org.capstone.ai_npc_plugin.AI_NPC_Plugin;
import org.capstone.ai_npc_plugin.network.PersistentModelClient;

import java.util.UUID;

public class ChatListener implements Listener {

    private final PersistentModelClient modelClient;
    private final AI_NPC_Plugin plugin;

    public ChatListener(AI_NPC_Plugin plugin) {
        this.plugin = plugin;
        this.modelClient = plugin.getPersistentModelClient();  // ✅ 여기 고정!
    }

    private boolean isAINPC(Villager v) {
        return v.getPersistentDataContainer().has(
                new NamespacedKey(plugin, "ainpc"),
                PersistentDataType.STRING
        );
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {

        Player player = event.getPlayer();

        if (!NpcInteractListener.isChatMode(player)) {
            return;
        }

        String msg = event.getMessage();

        Bukkit.getScheduler().runTask(plugin, () -> {

            UUID npcId = NpcInteractListener.getInteractingNPC(player);
            Villager npc = null;

            if (npcId != null) {
                npc = (Villager) Bukkit.getEntity(npcId);
                if (npc == null || npc.isDead()) {
                    NpcInteractListener.clearInteraction(player);
                    player.sendMessage(ChatColor.RED + "NPC와의 상호작용이 해제되었습니다.");
                    return;
                }
            } else {
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
                    npc = nearest;
                    npcId = nearest.getUniqueId();
                    NpcInteractListener.setInteraction(player, nearest);

                    player.sendMessage(ChatColor.GRAY + "(가장 가까운 NPC '" + nearest.getCustomName() + "'와 대화 중)");
                } else {
                    player.sendMessage(ChatColor.GRAY + "주변에 대화할 NPC가 없습니다.");
                    return;
                }
            }

            String npcCode = npc.getPersistentDataContainer().get(
                    new NamespacedKey(plugin, "npc_code"),
                    PersistentDataType.STRING
            );

            if (npcCode == null) {
                player.sendMessage(ChatColor.RED + "해당 NPC에 npc_code가 설정되어 있지 않습니다.");
                return;
            }

            String finalNpcCode = npcCode;
            String npcName = npc.getCustomName() != null ? npc.getCustomName() : "AI_NPC";

            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                String response = modelClient.sendMessage(player.getName(), finalNpcCode, msg);

                Bukkit.getScheduler().runTask(plugin, () -> {
                    player.sendMessage("§e[NPC: " + npcName + "]§f " + response);
                });
            });
        });
    }

    public void shutdown() {
        if (modelClient != null && modelClient.isConnected()) {
            modelClient.disconnect();
        }
    }
}