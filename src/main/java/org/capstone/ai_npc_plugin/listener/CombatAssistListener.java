package org.capstone.ai_npc_plugin.listener;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.capstone.ai_npc_plugin.AI_NPC_Plugin;
import org.capstone.ai_npc_plugin.npc.AINPC;

import java.util.UUID;

/**
 * CombatAssistListener
 *
 * AI NPC 전투 지원 기능을 위한 Listener 클래스
 *
 * 주요 기능:
 * - 플레이어가 공격한 대상을 NPC 가 함께 공격하도록 타겟 등록
 *
 * 조건:
 * - NPC 우호도(affinity)가 ALLY 상태일 때만 전투 지원 가능
 * - assistMap 에 등록된 플레이어에 한해 동작
 *
 * 이벤트 처리:
 * - EntityDamageByEntityEvent : 플레이어 공격 감지
 *
 * 타겟 정보는 npcTargetMap 에 저장 → runCombatTask 에서 실제 전투 수행
 */

public class CombatAssistListener implements Listener {

    // 플러그인 인스턴스
    private final AI_NPC_Plugin plugin;

    // 생성자 - 플러그인 인스턴스 전달받아 초기화
    public CombatAssistListener(AI_NPC_Plugin plugin) {
        this.plugin = plugin;
    }

    // 플레이어가 엔티티 공격 시 발생하는 이벤트 핸들러
    @EventHandler
    public void onPlayerAttack(EntityDamageByEntityEvent event) {

        // 공격자가 플레이어인지 확인
        if (!(event.getDamager() instanceof Player player)) return;

        UUID playerId = player.getUniqueId();

        // 해당 플레이어가 assist 모드인지 확인 (플러그인의 assistMap 참조)
        UUID npcId = plugin.getAssistMap().get(playerId);
        if (npcId == null) return; // assist 모드 아님 → 무시

        // assist NPC 가져오기 (UUID로 엔티티 조회)
        Villager npc = (Villager) Bukkit.getEntity(npcId);
        if (npc == null || npc.isDead()) return;

        // 해당 NPC의 현재 우호도 조회
        UUID villagerUUID = npc.getUniqueId();
        int affinity = plugin.getNpcStateManager().getAffinity(villagerUUID);

        // 우호도 등급 계산
        AINPC.AffinityLevel level;
        if (affinity <= 20) level = AINPC.AffinityLevel.HOSTILE;
        else if (affinity <= 50) level = AINPC.AffinityLevel.NEUTRAL;
        else if (affinity <= 80) level = AINPC.AffinityLevel.FRIENDLY;
        else level = AINPC.AffinityLevel.ALLY;

        // 우호도가 ALLY (동료)일 때만 전투 지원 가능
        if (level != AINPC.AffinityLevel.ALLY) return;

        // 플레이어가 공격한 대상 (타겟)
        Entity target = event.getEntity();
        if (target.getUniqueId().equals(npcId)) return;
        if (!(target instanceof LivingEntity)) return;

        // 타겟 등록 (NPC → 타겟 UUID)
        // 이후 별도 스케줄링된 runCombatTask에서 이동 및 공격 로직을 처리
        plugin.getNpcTargetMap().put(npcId, target.getUniqueId());

        // 플레이어에게 안내 메시지 출력
        player.sendMessage(ChatColor.GOLD + "[NPC] 타겟 설정 완료: " + target.getName());
    }
}