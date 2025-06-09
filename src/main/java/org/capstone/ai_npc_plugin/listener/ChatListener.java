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

/**
 * ChatListener
 *
 * 플레이어와 AI NPC 간 채팅 연동을 처리하는 Listener 클래스
 *
 * 주요 기능:
 * - 플레이어가 채팅 입력 시, 모델 서버에 메시지 전송
 * - 응답을 받아 플레이어에게 출력
 *
 * 지원 모드:
 * - chatmode 활성화 상태 (NpcInteractListener 에서 설정)
 * - 거리 기반으로 가장 가까운 AI NPC 와 자동 상호작용 가능
 *
 * 이벤트 처리:
 * - AsyncPlayerChatEvent : 플레이어 채팅 입력 감지
 */

public class ChatListener implements Listener {

    // 모델 서버와 지속 연결 클라이언트
    private final PersistentModelClient modelClient = new PersistentModelClient();

    // 생성자 - 서버 시작 시 모델 서버와 1회 연결
    public ChatListener() {
        modelClient.connect();
    }

    // AI NPC 여부 판별 (PersistentDataContainer 사용)
    private boolean isAINPC(Villager v) {
        return v.getPersistentDataContainer().has(
                new NamespacedKey(Bukkit.getPluginManager().getPlugin("AI_NPC_Plugin"), "ainpc"),
                PersistentDataType.STRING
        );
    }

    // 플레이어 채팅 이벤트 핸들러
    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {

        Player player = event.getPlayer();
        String message = event.getMessage();

        // 대화 모드가 활성화된 플레이어인지 확인
        if (!NpcInteractListener.isChatMode(player)) {
            return; // 일반 채팅은 무시
        }

        // 주 스레드에서 나머지 로직 실행 (게임 월드 접근 시 주 스레드 필요)
        Bukkit.getScheduler().runTask(
                Bukkit.getPluginManager().getPlugin("AI_NPC_Plugin"),
                () -> {

                    // 현재 플레이어가 상호작용 중인 NPC (있으면 해당 NPC 사용)
                    UUID npcId = NpcInteractListener.getInteractingNPC(player);

                    if (npcId == null) {
                        // 자동 거리 기반 검색 (10블록 이내 가장 가까운 AI NPC 탐색)
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
                            // 가장 가까운 NPC와 상호작용 설정
                            npcId = nearest.getUniqueId();
                            NpcInteractListener.setInteraction(player, nearest);

                            player.sendMessage("§7(가장 가까운 NPC '" + nearest.getCustomName() + "'와 대화 중)");
                        } else {
                            // NPC 없음 → 메시지 무시
                            return;
                        }
                    }

                    // 모델 서버 호출은 비동기 처리
                    Bukkit.getScheduler().runTaskAsynchronously(
                            Bukkit.getPluginManager().getPlugin("AI_NPC_Plugin"),
                            () -> {
                                // 플레이어 이름 + 메시지 전송 → 모델 응답 받기
                                String response = modelClient.sendMessage(player.getName(), message);

                                // 응답 메시지를 메인 스레드에서 플레이어에게 출력
                                Bukkit.getScheduler().runTask(
                                        Bukkit.getPluginManager().getPlugin("AI_NPC_Plugin"),
                                        () -> player.sendMessage("§e[NPC]§f " + response)
                                );
                            }
                    );
                }
        );
    }

    // 모델 서버와 연결 종료 메서드 (플러그인 종료 시 사용)
    public void shutdown() {
        modelClient.disconnect();
    }
}