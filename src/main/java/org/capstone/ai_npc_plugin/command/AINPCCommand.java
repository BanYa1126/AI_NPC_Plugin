package org.capstone.ai_npc_plugin.command;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.capstone.ai_npc_plugin.AI_NPC_Plugin;
import org.capstone.ai_npc_plugin.gui.PromptEditorManager;

import java.util.*;

public class AINPCCommand implements CommandExecutor {

    private final AI_NPC_Plugin plugin;
    private final PromptEditorManager manager;
    public AINPCCommand(AI_NPC_Plugin plugin, PromptEditorManager manager) {
        this.plugin  = plugin;
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage(ChatColor.RED + "플레이어만 사용할 수 있습니다.");
            return true;
        }

        if (args.length < 1) {
            p.sendMessage("§e사용법: /ainpc <prompt_set|prompt_fix|create|remove|reset|chatlog|disengage>");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "prompt_set": {
                // GUI로만 열기
                manager.openPromptSelectGUI(p);
                p.sendMessage(ChatColor.GREEN + "프롬프트 파일 선택 GUI를 열었습니다.");
                return true;
            }

            case "create": {
                if (manager.getAllData().isEmpty()) {
                    p.sendMessage(ChatColor.YELLOW
                            + "먼저 /ainpc prompt_set 으로 프롬프트를 설정하세요.");
                    return true;
                }

                Location loc = p.getLocation()
                        .add(p.getLocation().getDirection().normalize().multiply(2));
                Villager npc = p.getWorld().spawn(loc, Villager.class);
                npc.setCustomName("AI 주민");
                npc.setCustomNameVisible(true);
                npc.setAI(false);
                npc.setInvulnerable(true);
                npc.setPersistent(true);
                npc.setProfession(Villager.Profession.NONE);

                manager.openDataSelectGUI(p, npc);
                return true;
            }

            case "prompt_fix": {
                // GUI로만 열기
                manager.openPromptFixGUI(p);
                p.sendMessage(ChatColor.GREEN + "프롬프트 파일 수정 GUI를 열었습니다.");
                return true;
            }

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

            case "disengage": {
                org.capstone.ai_npc_plugin.listener.NpcInteractListener.clearInteraction(p);
                p.sendMessage(ChatColor.GRAY + "NPC와의 대화 연결이 해제되었습니다.");
                return true;
            }

            default:
                sender.sendMessage("알 수 없는 명령어입니다.");
                return true;
        }
    }
}