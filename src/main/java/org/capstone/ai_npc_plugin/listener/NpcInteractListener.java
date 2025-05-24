package org.capstone.ai_npc_plugin.listener;

import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.Bukkit;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class NpcInteractListener implements Listener {

    private final Plugin plugin;
    // 상호작용 상태 저장 (Player UUID → Villager UUID)
    private static final Map<UUID, UUID> interactingMap = new HashMap<>();

    public NpcInteractListener(Plugin plugin) {
        this.plugin = plugin;
    }

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

        if (!(entity instanceof Villager villager)) return;

        Bukkit.getLogger().info("[AINPC DEBUG] Villager 우클릭 감지됨");

        NamespacedKey key = new NamespacedKey(plugin, "ainpc");

        if (villager.getPersistentDataContainer().has(key, PersistentDataType.STRING)) {
            event.setCancelled(true); // ✅ 거래창 차단
            setInteraction(player, villager);
            player.sendMessage(ChatColor.YELLOW + "💬 이제 이 NPC와 대화합니다.");
        } else {
            Bukkit.getLogger().info("[AINPC DEBUG] 해당 Villager는 AI NPC 아님");
            }
        }
    }
