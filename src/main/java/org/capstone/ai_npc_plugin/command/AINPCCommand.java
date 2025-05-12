package org.capstone.ai_npc_plugin.command;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.capstone.ai_npc_plugin.AI_NPC_Plugin;

import java.util.*;

public class AINPCCommand implements CommandExecutor {

    private final AI_NPC_Plugin plugin;
    private final Set<UUID> nameInputWaiting = new HashSet<>();

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
                    nameInputWaiting.add(player.getUniqueId());
                    player.sendMessage(ChatColor.YELLOW + "수정할 NPC의 이름을 채팅으로 입력해주세요.");
                    return true;
                }

                String targetName = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                if (plugin.getNpcManager().loadPromptDataByName(targetName)) {
                    plugin.getNpcManager().openNpcEditGUI(player);
                    player.sendMessage(ChatColor.GREEN + "NPC 프롬프트 GUI가 열렸습니다.");
                } else {
                    player.sendMessage(ChatColor.RED + "해당 이름의 NPC를 찾을 수 없습니다.");
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
    public Set<UUID> getNameInputWaiting() {
        return nameInputWaiting;
    }
}