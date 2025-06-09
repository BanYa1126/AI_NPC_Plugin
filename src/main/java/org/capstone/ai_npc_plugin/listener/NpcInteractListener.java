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
import java.util.Set;
import java.util.HashSet;

/**
 * NpcInteractListener
 *
 * 플레이어와 AI NPC 간의 상호작용 상태를 관리하는 Listener 클래스
 *
 * 주요 기능:
 * - 우클릭 시 거래창 차단 및 상호작용 상태(대화모드) 설정
 * - chatmode 명령어와 연동하여 채팅 기반 상호작용 지원
 *
 * 내부 상태 관리:
 * - npcChatMode : chatmode on/off 여부 저장
 * - interactingMap : 현재 상호작용 중인 NPC UUID 저장
 *
 * 이벤트 처리:
 * - PlayerInteractEntityEvent : NPC 우클릭 감지
 */

public class NpcInteractListener implements Listener {
    // 플러그인 참조
    private final Plugin plugin;
    // 대화 모드 활성화된 플레이어 목록
    private static final Set<UUID> npcChatMode = new HashSet<>();

    // 플레이어 ↔ 상호작용 중인 NPC (Villager) UUID 매핑
    // Key: Player UUID, Value: Villager UUID
    private static final Map<UUID, UUID> interactingMap = new HashMap<>();
    // 플레이어의 chatMode 설정 (켜기/끄기)
    public static void setChatMode(Player player, boolean enabled) {
        if (enabled) {
            npcChatMode.add(player.getUniqueId());
        } else {
            npcChatMode.remove(player.getUniqueId());
        }
    }
    // 생성자
    public NpcInteractListener(Plugin plugin) {
        this.plugin = plugin;
    }
    // 현재 플레이어가 상호작용 중인 NPC UUID 반환
    public static UUID getInteractingNPC(Player player) {
        return interactingMap.get(player.getUniqueId());
    }
    // 플레이어 ↔ NPC 상호작용 상태 해제
    public static void clearInteraction(Player player) {
        interactingMap.remove(player.getUniqueId());
    }
    // 플레이어 ↔ NPC 상호작용 상태 설정
    public static void setInteraction(Player player, Villager villager) {
        interactingMap.put(player.getUniqueId(), villager.getUniqueId());
    }
    // 현재 플레이어가 chatMode 상태인지 여부 반환
    public static boolean isChatMode(Player player) {
        return npcChatMode.contains(player.getUniqueId());
    }
    // 플레이어가 Villager 우클릭 시 이벤트 처리
    @EventHandler
    public void onNpcRightClick(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        Entity entity = event.getRightClicked();
        // Villager만 처리
        if (!(entity instanceof Villager villager)) return;

        Bukkit.getLogger().info("[AINPC DEBUG] Villager 우클릭 감지됨");
        // AI NPC 여부 확인 (PersistentData: ainpc == "true")
        NamespacedKey key = new NamespacedKey(plugin, "ainpc");
        // AI NPC가 맞으면:
        // - 거래창 차단
        // - 상호작용 상태 설정
        if (villager.getPersistentDataContainer().has(key, PersistentDataType.STRING)) {
            event.setCancelled(true); // ✅ 거래창 차단
            setInteraction(player, villager);
            player.sendMessage(ChatColor.YELLOW + "💬 이제 이 NPC와 대화합니다.");
        } else { // 일반 Villager는 무시
            Bukkit.getLogger().info("[AINPC DEBUG] 해당 Villager는 AI NPC 아님");
            }
        }
    }
