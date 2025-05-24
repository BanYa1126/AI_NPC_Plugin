package org.capstone.ai_npc_plugin.listener;

import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class NpcInteractListener implements Listener {

    // 상호작용 상태 저장 (Player UUID → Villager UUID)
    private static final Map<UUID, UUID> interactingMap = new HashMap<>();

    public static UUID getInteractingNPC(Player player) {
        return interactingMap.get(player.getUniqueId());
    }

    public static void clearInteraction(Player player) {
        interactingMap.remove(player.getUniqueId());
    }

    public static void setInteraction(Player player, Villager villager) {
        interactingMap.put(player.getUniqueId(), villager.getUniqueId());
    }

    @EventHandler
    public void onNpcRightClick(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        Entity entity = event.getRightClicked();

        if (entity instanceof Villager villager && "AI 주민".equals(villager.getCustomName())) {
            interactingMap.put(player.getUniqueId(), villager.getUniqueId());
            player.sendMessage(ChatColor.YELLOW + "💬 이제 이 NPC와 대화합니다.");
        }
    }
}
