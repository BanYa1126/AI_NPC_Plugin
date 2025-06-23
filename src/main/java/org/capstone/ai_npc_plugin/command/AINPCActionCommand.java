package org.capstone.ai_npc_plugin.command;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.capstone.ai_npc_plugin.AI_NPC_Plugin;
import org.capstone.ai_npc_plugin.listener.NpcInteractListener;
import org.capstone.ai_npc_plugin.npc.AINPC;

import java.util.UUID;

/**
 * AINPCActionCommand
 *
 * AI NPC 행동 요청 명령어 /ainpc_action 을 처리하는 CommandExecutor 클래스
 *
 * 주요 기능:
 * - follow : NPC가 플레이어를 따라오게 요청
 * - wait : NPC 대기 명령 (follow/assist 해제)
 * - assist : NPC 전투 지원 모드 활성화
 *
 * 우호도(Affinity)에 따라 요청의 수락 여부가 달라짐
 *
 * 사용 위치: plugin.yml > commands > ainpc_action
 */

public class AINPCActionCommand implements CommandExecutor {

    private final AI_NPC_Plugin plugin;

    // 생성자 → 메인 플러그인의 인스턴스를 받아 저장 (FollowMap, AssistMap 등 사용 목적)
    public AINPCActionCommand(AI_NPC_Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // 1. 커맨드 실행자가 플레이어인지 확인
        if(!(sender instanceof Player player)) {
            sender.sendMessage("플레이어만 사용 가능");
            return true;
        }

        // 2. 인자가 비어있으면 사용법 안내
        if (args.length < 1) {
            sender.sendMessage("§e사용법: /ainpc_action <follow|wait|assist>");
            return true;
        }

        // 3. 현재 플레이어의 UUID 저장
        UUID playerId = player.getUniqueId();

        // 4. 현재 플레이어가 "대화 중인 NPC"를 가져오기 (NpcInteractListener에서 저장된 것)
        UUID npcId = NpcInteractListener.getInteractingNPC(player);

        if (npcId == null) {
            // → 대화 중인 NPC가 없으면 안내 후 종료
            player.sendMessage("§c대화 중인 NPC가 없습니다 (우클릭 or chatmode 사용).");
            return true;
        }

        // 5. UUID로 실제 Villager 엔티티 가져오기
        Villager npc = (Villager) Bukkit.getEntity(npcId);
        if (npc == null) {
            // → 존재하지 않으면 안내 후 종료
            player.sendMessage("§cNPC가 존재하지 않습니다.");
            return true;
        }

        // 6. 해당 NPC의 현재 우호도(affinity) 조회
        int affinity = plugin.getNpcStateManager().getAffinity(npcId);

        // 7. affinity 점수를 AINPC.AffinityLevel enum으로 변환
        AINPC.AffinityLevel level;
        if (affinity <= 20) level = AINPC.AffinityLevel.HOSTILE;
        else if (affinity <= 50) level = AINPC.AffinityLevel.NEUTRAL;
        else if (affinity <= 80) level = AINPC.AffinityLevel.FRIENDLY;
        else level = AINPC.AffinityLevel.ALLY;

        // 8. 플레이어가 입력한 행동 (follow, wait, assist) 파싱
        String action = args[0].toLowerCase();

        // 9. 행동 분기 (switch 문 사용)
        switch (action) {

            // ========================
            // FOLLOW : 따라오기 명령
            // ========================
            case "follow" -> {
                if (level == AINPC.AffinityLevel.HOSTILE || level == AINPC.AffinityLevel.NEUTRAL) {
                    // HOSTILE or NEUTRAL → 무조건 거절
                    player.sendMessage(ChatColor.RED + "[NPC] 당신을 따라가고 싶지 않아요.");
                } else if (level == AINPC.AffinityLevel.FRIENDLY) {
                    // FRIENDLY → 60% 확률적 수락
                    boolean accepted = Math.random() < 0.6;
                    if (accepted) {
                        plugin.getFollowMap().put(playerId, npcId);
                        player.sendMessage(ChatColor.GREEN + "[NPC] 알겠어요, 따라갈게요!");
                    } else {
                        player.sendMessage(ChatColor.YELLOW + "[NPC] 미안하지만 지금은 못 따라가겠어요.");
                    }
                } else if (level == AINPC.AffinityLevel.ALLY) {
                    // ALLY → 무조건 수락
                    plugin.getFollowMap().put(playerId, npcId);
                    player.sendMessage(ChatColor.AQUA + "[NPC] 언제든지 따라가겠습니다!");
                }
            }

            // ========================
            // WAIT : 대기 명령
            // ========================
            case "wait" -> {
                Villager target = getTargetedVillager(player); // 대상 NPC
                if (target == null) {
                    player.sendMessage(ChatColor.RED + "⚠️ 바라보는 NPC가 없습니다.");
                    return true;
                }
                plugin.getWaitMap().put(player.getUniqueId(), target.getUniqueId());
                plugin.getFollowMap().remove(player.getUniqueId()); // 따라오기 해제
                player.sendMessage(ChatColor.YELLOW + "[NPC] 해당 위치에서 대기합니다.");
            }


            // ========================
            // ASSIST : 전투 지원 모드
            // ========================
            case "assist" -> {
                if (level != AINPC.AffinityLevel.ALLY) {
                    // → ALLY 상태가 아니면 거절
                    player.sendMessage(ChatColor.RED + "[NPC] 전투는 함께할 정도로 친하지 않아요.");
                } else {
                    // → ALLY 상태이면 전투 지원 모드 활성화 (AssistMap 등록)
                    plugin.getAssistMap().put(playerId, npcId);
                    player.sendMessage(ChatColor.AQUA + "[NPC] 전투 지원 모드 활성화.");
                }
            }

            // ========================
            // DEFAULT : 잘못된 명령어 입력 시 안내
            // ========================
            default -> player.sendMessage("§e사용법: /ainpc_action <follow|wait|assist>");
        }

        return true;
    }

    // [현재 사용 X] → handleRequest() 메서드 (예전 구조 잔존, 호출되지 않음)
    private void handleRequest(CommandSender sender, AINPC.AffinityLevel level, String request) {
        switch (level) {
            case HOSTILE -> sender.sendMessage(ChatColor.DARK_RED + "[NPC] 싫어! 절대 안 해. 꺼져!");
            case NEUTRAL -> sender.sendMessage(ChatColor.YELLOW + "[NPC] 죄송하지만 그럴 수 없습니다.");
            case FRIENDLY -> {
                boolean accepted = Math.random() < 0.6; // 현재 확률 60%
                if (accepted) {
                    sender.sendMessage(ChatColor.GREEN + "[NPC] 알겠어요! " + request + " 시작할게요.");
                } else {
                    sender.sendMessage(ChatColor.YELLOW + "[NPC] 미안하지만 이번엔 안 될 것 같아요.");
                }
            }
            case ALLY -> sender.sendMessage(ChatColor.AQUA + "[NPC] 물론이죠! " + request + " 진행하겠습니다.");
        }
    }
}