package org.capstone.ai_npc_plugin.command;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.capstone.ai_npc_plugin.AI_NPC_Plugin;
import org.capstone.ai_npc_plugin.manager.PromptEditorManager;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;
import org.capstone.ai_npc_plugin.listener.NpcInteractListener;

/**
 * AINPCCommand
 *
 * 메인 명령어 /ainpc 를 처리하는 CommandExecutor 클래스
 *
 * 주요 기능:
 * - prompt_set : 프롬프트 선택 GUI 열기
 * - prompt_fix : 프롬프트 수정 GUI 열기
 * - create : AI 주민(NPC) 생성
 * - remove : 글로벌 NPC 리셋
 * - reset : 대화 로그 초기화
 * - chatlog : 현재 대화 로그 출력
 * - chatmode : 플레이어와 NPC 간 채팅 모드 설정 (on/off)
 * - disengage : NPC 상호작용 상태 해제
 *
 * 사용 위치: plugin.yml > commands > ainpc
 */

public class AINPCCommand implements CommandExecutor {

    // 플러그인 인스턴스
    private final AI_NPC_Plugin plugin;

    // 프롬프트 에디터 매니저 (프롬프트 관련 GUI/데이터 관리)
    private final PromptEditorManager manager;

    // 생성자 - 플러그인 인스턴스와 매니저를 받아 초기화
    public AINPCCommand(AI_NPC_Plugin plugin, PromptEditorManager manager) {
        this.plugin  = plugin;
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // 명령어 실행자가 플레이어인지 확인
        if (!(sender instanceof Player p)) {
            sender.sendMessage(ChatColor.RED + "플레이어만 사용할 수 있습니다.");
            return true;
        }

        // 인자가 비어있으면 사용법 출력
        if (args.length < 1) {
            p.sendMessage("§e사용법: /ainpc <prompt_set|prompt_fix|create|remove|reset|chatlog|disengage>");
            return true;
        }

        // 첫 번째 인자를 소문자로 변환하여 switch 문으로 처리
        switch (args[0].toLowerCase()) {

            // 프롬프트 선택 GUI 열기
            case "prompt_set": {
                manager.openPromptSelectGUI(p);
                p.sendMessage(ChatColor.GREEN + "프롬프트 파일 선택 GUI를 열었습니다.");
                return true;
            }

            // NPC 생성
            case "create": {
                // 먼저 프롬프트가 설정되어 있는지 확인
                if (manager.getAllData().isEmpty()) {
                    p.sendMessage(ChatColor.YELLOW
                            + "먼저 /ainpc prompt_set 으로 프롬프트를 설정하세요.");
                    return true;
                }

                // 플레이어 앞쪽 위치에 Villager NPC 생성
                Location loc = p.getLocation()
                        .add(p.getLocation().getDirection().normalize().multiply(2));

                Villager npc = p.getWorld().spawn(loc, Villager.class);

                // NPC 기본 설정
                npc.setCustomName("AI 주민");
                npc.setCustomNameVisible(true);
                npc.setAI(false); // 기본적으로 AI 비활성화 (움직이지 않음)
                npc.setInvulnerable(true);
                npc.setPersistent(true);
                npc.setProfession(Villager.Profession.NONE);

                // AI_NPC 식별 태그 부여
                npc.getPersistentDataContainer().set(
                        new NamespacedKey(plugin, "ainpc"),
                        PersistentDataType.STRING,
                        "true"
                );

                // 데이터 선택 GUI 열기
                manager.openDataSelectGUI(p, npc);
                return true;
            }

            // 프롬프트 수정 GUI 열기
            case "prompt_fix": {
                manager.openPromptFixGUI(p);
                p.sendMessage(ChatColor.GREEN + "프롬프트 파일 수정 GUI를 열었습니다.");
                return true;
            }

            // 글로벌 NPC 인스턴스 초기화 (NPC 제거)
            case "remove":
                AI_NPC_Plugin.globalNpc = new org.capstone.ai_npc_plugin.npc.AINPC();
                sender.sendMessage("NPC 제거 완료.");
                return true;

            // 글로벌 NPC 대화 로그 초기화
            case "reset":
                AI_NPC_Plugin.globalNpc.resetChatLog();
                sender.sendMessage("대화 로그 초기화 완료.");
                return true;

            // 글로벌 NPC 대화 로그 출력
            case "chatlog":
                sender.sendMessage("대화 로그:\n" + AI_NPC_Plugin.globalNpc.getChatLog());
                return true;

            // 대화 모드 on/off 설정
            case "chatmode": {
                // 다시 플레이어 확인
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("플레이어만 사용할 수 있습니다.");
                    return true;
                }

                // 인자가 부족하면 사용법 출력
                if (args.length < 2) {
                    player.sendMessage("§e사용법: /ainpc chatmode <on|off>");
                    return true;
                }

                // on/off 처리
                switch (args[1].toLowerCase()) {
                    case "on" -> {
                        NpcInteractListener.setChatMode(player, true);
                        player.sendMessage("§a[NPC] 대화 모드가 활성화되었습니다.");
                    }
                    case "off" -> {
                        NpcInteractListener.setChatMode(player, false);
                        player.sendMessage("§c[NPC] 대화 모드가 비활성화되었습니다.");
                    }
                    default -> {
                        player.sendMessage("§e사용법: /ainpc chatmode <on|off>");
                    }
                }
                return true;
            }

            // NPC 상호작용 상태 해제
            case "disengage": {
                if (sender instanceof Player player) {
                    NpcInteractListener.clearInteraction(player);
                    player.sendMessage("§7NPC 상호작용 상태가 해제되었습니다.");
                }
                return true;
            }

            // 알 수 없는 명령어 처리
            default:
                sender.sendMessage("알 수 없는 명령어입니다.");
                return true;
        }
    }
}