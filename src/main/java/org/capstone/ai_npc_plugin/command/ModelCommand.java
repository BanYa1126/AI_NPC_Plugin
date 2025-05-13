package org.capstone.ai_npc_plugin.command;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.capstone.ai_npc_plugin.AI_NPC_Plugin;
import org.capstone.ai_npc_plugin.controller.ModelController;
import org.capstone.ai_npc_plugin.npc.PromptData;

public class ModelCommand implements CommandExecutor {

    private final AI_NPC_Plugin plugin;
    private final ModelController controller = new ModelController();

    public ModelCommand(AI_NPC_Plugin plugin) {
        this.plugin = plugin;
    }

    public static void shutdownModel() {
        ModelController.stopModel();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) {
            sender.sendMessage("사용법: /model on | off | status | reload");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "on":
                controller.startModel();
                sender.sendMessage(ChatColor.GREEN + "모델 서버를 실행했습니다.");
                return true;

            case "off":
                controller.stopModel();
                sender.sendMessage(ChatColor.RED + "모델 서버를 종료했습니다.");
                return true;

            case "status":
                boolean running = ModelController.isModelRunning();
                sender.sendMessage("모델 상태: " + (running ? ChatColor.GREEN + "실행 중" : ChatColor.RED + "정지됨"));
                return true;

            case "reload":
                boolean loaded = plugin.getNpcManager().loadPromptDataByName("npc"); // npc.json 재로드
                PromptData data = plugin.getNpcManager().getCurrentData();

                if (data != null) {
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

            default:
                sender.sendMessage(ChatColor.YELLOW + "사용법: /model on | off | status | reload");
                return true;
        }
    }
}