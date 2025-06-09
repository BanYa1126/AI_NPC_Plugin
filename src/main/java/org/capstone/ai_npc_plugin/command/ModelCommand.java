package org.capstone.ai_npc_plugin.command;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.capstone.ai_npc_plugin.AI_NPC_Plugin;
import org.capstone.ai_npc_plugin.controller.ModelController;
import org.capstone.ai_npc_plugin.npc.PromptData;

/**
 * ModelCommand
 *
 * 모델 서버 제어 명령어 /model 을 처리하는 CommandExecutor 클래스
 *
 * 주요 기능:
 * - on : 모델 서버 실행
 * - off : 모델 서버 종료
 * - status : 모델 서버 실행 여부 확인
 * - reload : 현재 NPC 프롬프트 다시 로드
 *
 * 사용 위치: plugin.yml > commands > model
 */

public class ModelCommand implements CommandExecutor {

    // 플러그인 인스턴스
    private final AI_NPC_Plugin plugin;

    // 모델 컨트롤러 (모델 서버 관리용)
    private final ModelController controller = new ModelController();

    // 생성자 - 플러그인 인스턴스를 받아 초기화
    public ModelCommand(AI_NPC_Plugin plugin) {
        this.plugin = plugin;
    }

    // 정적 메서드 - 플러그인 종료 시 모델 서버 종료에 사용
    public static void shutdownModel() {
        ModelController.stopModel();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        // 인자가 부족할 경우 사용법 출력
        if (args.length < 1) {
            sender.sendMessage("사용법: /model on | off | status | reload");
            return true;
        }

        // 첫 번째 인자를 소문자로 변환하여 switch 문으로 처리
        switch (args[0].toLowerCase()) {

            // 모델 서버 실행 명령
            case "on":
                controller.startModel();
                sender.sendMessage(ChatColor.GREEN + "모델 서버를 실행했습니다.");
                return true;

            // 모델 서버 종료 명령
            case "off":
                controller.stopModel();
                sender.sendMessage(ChatColor.RED + "모델 서버를 종료했습니다.");
                return true;

            // 모델 서버 상태 확인 명령
            case "status":
                boolean running = ModelController.isModelRunning();
                sender.sendMessage("모델 상태: " + (running ? ChatColor.GREEN + "실행 중" : ChatColor.RED + "정지됨"));
                return true;

            // 모델 프롬프트 재로드 명령
            case "reload":
                // npc.json 파일을 재로드
                boolean loaded = plugin.getNpcManager().loadPromptDataByName("npc");
                PromptData data = plugin.getNpcManager().getCurrentData();

                if (data != null) {
                    // 글로벌 NPC 데이터 갱신
                    AI_NPC_Plugin.globalNpc.setName(data.name);
                    String prompt = String.format("이름: %s\n직업: %s\n성격: %s\n배경: %s",
                            data.name,
                            data.job,
                            String.join(", ", data.personality),
                            data.background
                    );
                    AI_NPC_Plugin.globalNpc.setPrompt(prompt);

                    sender.sendMessage(ChatColor.GREEN + "모델 프롬프트가 성공적으로 갱신되었습니다.");
                } else {
                    sender.sendMessage(ChatColor.RED + "프롬프트 JSON을 불러올 수 없습니다.");
                }
                return true;

            // 잘못된 명령어 입력 시 사용법 출력
            default:
                sender.sendMessage(ChatColor.YELLOW + "사용법: /model on | off | status | reload");
                return true;
        }
    }
}