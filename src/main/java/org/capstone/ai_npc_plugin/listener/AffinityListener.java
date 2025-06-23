package org.capstone.ai_npc_plugin.listener;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.capstone.ai_npc_plugin.AI_NPC_Plugin;
import java.util.UUID;

/**
 * AffinityListener
 * NPC 우호도(Affinity)를 조작하는 이벤트 처리 클래스
 * 주요 기능:
 * - Shift 우클릭 + 에메랄드 → 우호도 상승
 * - 플레이어가 NPC 공격 시 → 우호도 감소
 * 이벤트 처리:
 * - PlayerInteractEntityEvent : 에메랄드 주기
 * - EntityDamageByEntityEvent : 공격 시 우호도 감소
 * NPCStateManager 를 통해 affinityMap 을 업데이트
 */

public class AffinityListener implements Listener {

    // 플러그인 인스턴스
    private final Plugin plugin;

    // 생성자 - 플러그인 인스턴스 전달받아 초기화
    public AffinityListener(Plugin plugin) {
        this.plugin = plugin;
    }

    // (1) Shift 우클릭 + 에메랄드 → 우호도 상승 처리
    @EventHandler
    public void onPlayerGiveEmerald(PlayerInteractEntityEvent event) {

        // 우클릭 대상이 Villager인지 확인
        if (!(event.getRightClicked() instanceof Villager villager)) return;

        Player player = event.getPlayer();

        // AI NPC 여부 확인 (PersistentDataContainer에서 "ainpc" 태그 확인)
        NamespacedKey key = new NamespacedKey(plugin, "ainpc");
        if (!villager.getPersistentDataContainer().has(key, PersistentDataType.STRING)) return;

        // Shift 우클릭 조건 확인 (스니크 상태 + 메인 핸드 사용)
        if (!player.isSneaking()) return;
        if (event.getHand() != EquipmentSlot.HAND) return;

        // 손에 든 아이템이 에메랄드인지 확인
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() != Material.EMERALD) return;

        // 에메랄드 1개 소비
        item.setAmount(item.getAmount() - 1);

        // 현재 우호도 조회
        UUID npcId = villager.getUniqueId();
        // 우호도 +10 상승
        ((AI_NPC_Plugin) plugin).getNpcStateManager().addAffinity(npcId, 10);
        int newAffinity = ((AI_NPC_Plugin) plugin).getNpcStateManager().getAffinity(npcId);
        // 플레이어에게 알림
        player.sendMessage(ChatColor.GREEN + "[NPC] 우호도가 상승했습니다! 현재 점수: " + newAffinity);
    }

    // (2) 플레이어가 NPC를 공격할 경우 → 우호도 감소 처리
    @EventHandler
    public void onNpcDamaged(EntityDamageByEntityEvent event) {

        // 피해 대상이 Villager인지 확인
        if (!(event.getEntity() instanceof Villager villager)) return;
        // 공격자가 Player인지 확인
        if (!(event.getDamager() instanceof Player player)) return;
        // AI NPC 여부 확인
        NamespacedKey key = new NamespacedKey(plugin, "ainpc");
        if (!villager.getPersistentDataContainer().has(key, PersistentDataType.STRING)) return;

        // 현재 우호도 조회
        UUID npcId = villager.getUniqueId();
        // 우호도 -15 감소
        ((AI_NPC_Plugin) plugin).getNpcStateManager().addAffinity(npcId, -15);
        int newAffinity = ((AI_NPC_Plugin) plugin).getNpcStateManager().getAffinity(npcId);
        // 플레이어에게 알림
        player.sendMessage(ChatColor.RED + "[NPC] 우호도가 감소했습니다! 현재 점수: " + newAffinity);
    }
}