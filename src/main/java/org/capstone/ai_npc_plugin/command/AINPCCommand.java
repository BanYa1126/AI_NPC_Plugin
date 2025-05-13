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
        if (args.length < 1) {
            sender.sendMessage("사용법: /ainpc create | prompt_fix | remove | reset | chatlog");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "create":
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(ChatColor.RED + "이 명령어는 플레이어만 사용할 수 있습니다.");
                    return true;
                }

                Location loc = player.getLocation().add(player.getLocation().getDirection().normalize().multiply(2));
                Villager npc = player.getWorld().spawn(loc, Villager.class);
                npc.setCustomName("AI 주민");
                npc.setCustomNameVisible(true);
                npc.setAI(false);
                npc.setInvulnerable(true);
                npc.setPersistent(true);
                npc.setProfession(Villager.Profession.LIBRARIAN);

                plugin.getNpcManager().openPromptSelectGUI(player, npc);
                return true;

            case "prompt_fix":
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(ChatColor.RED + "이 명령어는 플레이어만 사용할 수 있습니다.");
                    return true;
                }

                if (args.length < 2) {
                    player.sendMessage(ChatColor.YELLOW + "사용법: /ainpc prompt_fix <파일명>");
                    return true;
                }
                String fileName = args[1];  // JSON 파일명 (확장자 제외)
                if (manager.loadNpcData(fileName)) {
                    manager.openNpcEditGUI(player);
                    player.sendMessage(ChatColor.GREEN + "NPC 편집 GUI가 열렸습니다: " + fileName + ".json");
                } else {
                    player.sendMessage(ChatColor.RED + "해당 파일을 찾을 수 없습니다: " + fileName + ".json");
                }
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