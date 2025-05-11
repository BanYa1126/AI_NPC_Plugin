package org.capstone.ai_npc_plugin.listener;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.capstone.ai_npc_plugin.network.ModelSocketClient;

public class ChatListener implements Listener {

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String playerMessage = event.getMessage();

        // 비동기 스레드로 모델에 요청
        Bukkit.getScheduler().runTaskAsynchronously(
                Bukkit.getPluginManager().getPlugin("AI_NPC_Plugin"),
                () -> {
                    String npcResponse = ModelSocketClient.getNPCResponse(player.getName(), playerMessage);

                    // 응답을 메인 스레드에서 출력
                    Bukkit.getScheduler().runTask(
                            Bukkit.getPluginManager().getPlugin("AI_NPC_Plugin"),
                            () -> player.sendMessage("§e[NPC]§f " + npcResponse)
                    );
                }
        );
    }
}
