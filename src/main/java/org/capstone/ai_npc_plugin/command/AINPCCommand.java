package org.capstone.ai_npc_plugin.command;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.capstone.ai_npc_plugin.AI_NPC_Plugin;

import java.util.Arrays;

public class AINPCCommand implements CommandExecutor {

    private final AI_NPC_Plugin plugin;

    public AINPCCommand(AI_NPC_Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) {
            sender.sendMessage("사용법: /ainpc create | prompt_fix | remove | reset | chatlog");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "prompt_fix":
                if (args.length < 2) {
                    if (sender instanceof Player player) {
                        plugin.getNpcManager().openNpcEditGUI(player);
                        sender.sendMessage(ChatColor.GREEN + "NPC 프롬프트 수정 GUI가 열렸습니다.");
                    } else {
                        sender.sendMessage(ChatColor.RED + "이 명령어는 플레이어만 사용할 수 있습니다.");
                    }
                    return true;
                }
                String prompt = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                AI_NPC_Plugin.globalNpc.setPrompt(prompt);
                sender.sendMessage(ChatColor.YELLOW + "프롬프트가 수동으로 설정되었습니다.");
                sender.sendMessage(ChatColor.GRAY + "※ JSON 기반 GUI 편집은 /ainpc prompt_fix 로 실행하세요.");
                return true;

            case "remove":
                AI_NPC_Plugin.globalNpc = new org.capstone.ai_npc_plugin.npc.AINPC();
                sender.sendMessage("NPC 제거 완료.");
                return true;

            case "reset":
                AI_NPC_Plugin.globalNpc.resetChatLog();
                sender.sendMessage("대화 로그 초기화 완료.");
                return true;

            case "chatlog":
                sender.sendMessage("대화 로그:\n" + AI_NPC_Plugin.globalNpc.getChatLog());
                return true;

            default:
                sender.sendMessage("알 수 없는 명령어입니다.");
                return true;
        }
    }
}