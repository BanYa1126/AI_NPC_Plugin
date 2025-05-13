package org.capstone.ai_npc_plugin.listener;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.capstone.ai_npc_plugin.network.ModelSocketClient;
import org.capstone.ai_npc_plugin.network.PersistentModelClient;

public class ChatListener implements Listener {

    private final PersistentModelClient modelClient = new PersistentModelClient();

    public ChatListener() {
        modelClient.connect();  // 서버 시작 시 1회 연결
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();

        Bukkit.getScheduler().runTaskAsynchronously(
                Bukkit.getPluginManager().getPlugin("AI_NPC_Plugin"),
                () -> {
                    String response = modelClient.sendMessage(player.getName(), message);
                    Bukkit.getScheduler().runTask(Bukkit.getPluginManager().getPlugin("AI_NPC_Plugin"),
                            () -> player.sendMessage("§e[NPC]§f " + response));
                }
        );
    }

    public void shutdown() {
        modelClient.disconnect();
    }
}
